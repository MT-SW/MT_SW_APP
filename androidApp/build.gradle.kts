/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.android.build.api.dsl.ApplicationExtension
import org.meshtastic.buildlogic.DEFAULT_LOCALE_TAG
import org.meshtastic.buildlogic.GenerateLocaleConfigTask
import org.meshtastic.buildlogic.LocaleQualifiersValueSource
import org.meshtastic.buildlogic.configProperties
import org.meshtastic.buildlogic.resolveVersionInfo
import org.meshtastic.buildlogic.resourceLocaleFilters
import java.util.Properties

val versionInfo = resolveVersionInfo()

plugins {
    alias(libs.plugins.meshtastic.android.application)
    alias(libs.plugins.meshtastic.android.application.flavors)
    alias(libs.plugins.meshtastic.android.application.compose)
    alias(libs.plugins.meshtastic.kotlinx.serialization)
    alias(libs.plugins.meshtastic.koin)
    alias(libs.plugins.meshtastic.android.secrets)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.meshtastic.aboutlibraries)
    // Version-less on purpose: mokkery is embedded in the convention-plugin jar (build-logic
    // `implementation`), so a versioned alias(libs.plugins.mokkery) request is rejected by Gradle.
    id("dev.mokkery")
    alias(libs.plugins.devtools.ksp)
}

// Isolated-Projects-safe root access — never reach through `rootProject`.
val keystorePropertiesFile = isolated.rootProject.projectDirectory.file("keystore.properties").asFile
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

// The values-* directories Crowdin writes are the one list of shipped locales.
val appResourcesDir = isolated.rootProject.projectDirectory.dir("core/resources/src/commonMain/composeResources")

configure<ApplicationExtension> {
    namespace = "org.meshtastic.app"

    // The store build uses exactly the sources, resources and manifest of fdroid (no Google services in it).
    sourceSets.getByName("google") {
        kotlin.directories.add("src/fdroid/kotlin")
        res.directories.add("src/fdroid/res")
        manifest.srcFile("src/fdroid/AndroidManifest.xml")
    }

    signingConfigs {
        // Shared debug key (checked in; debug keys are not secret) so local builds and CI snapshots
        // stay update-compatible — AGP's default per-machine key makes every snapshot un-sideloadable.
        getByName("debug") {
            storeFile = isolated.rootProject.projectDirectory.file("config/debug.keystore").asFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = keystoreProperties["storeFile"]?.let { file(it) }
            storePassword = keystoreProperties["storePassword"] as String?
        }
    }
    defaultConfig {
        applicationId = configProperties.getProperty("APPLICATION_ID")

        versionCode = versionInfo.versionCode
        versionName = versionInfo.versionName
        buildConfigField("String", "MIN_FW_VERSION", "\"${versionInfo.minFwVersion}\"")
        buildConfigField("String", "ABS_MIN_FW_VERSION", "\"${versionInfo.absMinFwVersion}\"")
        // Filter library resources to the translated languages, because some libs ship bogus languages that Google
        // Play rejects (gr).
        val ci = providers.gradleProperty("ci").map { it.toBoolean() }.getOrElse(false)
        if (ci) {
            logger.lifecycle("CI build detected - limiting locale filters for faster packaging")
            androidResources.localeFilters.addAll(listOf(DEFAULT_LOCALE_TAG))
        } else {
            val qualifiers =
                providers.of(LocaleQualifiersValueSource::class) { parameters.resourcesDir.set(appResourcesDir) }.get()
            androidResources.localeFilters.addAll(resourceLocaleFilters(qualifiers))
        }
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
    }

    // Disable ABI splits for bundle builds or when explicitly requested via Gradle property.
    // Usage: ./gradlew :androidApp:bundleGoogleRelease -Pmeshtastic.disableAbiSplits=true
    val disableSplits = providers.gradleProperty("meshtastic.disableAbiSplits").map { it.toBoolean() }.getOrElse(false)

    // Enable ABI splits to generate smaller APKs per architecture for F-Droid/IzzyOnDroid
    splits {
        abi {
            isEnable = !disableSplits
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play)
        includeInBundle = false
    }

    packaging {
        jniLibs {
            // Keep debug symbols in native libraries so reproducible builds don't depend
            // on the exact NDK version used for stripping. This avoids RB failures when
            // IzzyOnDroid/F-Droid rebuilds use a different NDK than our CI.
            // See: https://github.com/meshtastic/Meshtastic-Android/issues/3231
            keepDebugSymbols.add("**/*.so")
        }
    }

    // Configure existing product flavors (defined by convention plugin)
    // with their dynamic version names.
    productFlavors {
        configureEach {
            versionName = "${defaultConfig.versionName} (${defaultConfig.versionCode}) $name"
            if (name == "fdroid" || name == "google") {
                proguardFile("proguard-rules-fdroid.pro")
            }
        }
    }

    buildTypes {
        release {
            // Unsigned when keystore.properties is absent — a release must never carry the public
            // debug key. For an installable local release, point keystore.properties at config/debug.keystore.
            if (keystoreProperties["storeFile"] != null) {
                signingConfig = signingConfigs.named("release").get()
            }
            isDebuggable = false
        }
    }
    bundle { language { enableSplit = false } }

    testOptions { unitTests { isIncludeAndroidResources = true } }
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
    propertiesFileName = "secrets.properties"
}

// AppSearch without dynamic-schema support indexes only the v1 XML named by the `android.app.appfunctions` property.
ksp { arg("appfunctions:generateV1Xml", "true") }

// Merging into src/main is what ships the profile in fdroid too.
baselineProfile { mergeIntoMain = true }

// The producer only has the google flavor, so only googleRelease may depend on it: fdroidRelease would fail to resolve
// it. The plugin creates this configuration per variant, after this script runs.
configurations
    .matching { it.name == "googleReleaseBaselineProfile" }
    .configureEach { dependencies.add(projects.baselineprofile) }

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        variant.flavorName?.let { flavor -> variant.applicationId.set("pl.sw.mt.$flavor.debug") }
    }
    onVariants { variant ->
        val generateLocaleConfig =
            tasks.register<GenerateLocaleConfigTask>(
                "generate${variant.name.replaceFirstChar { it.uppercase() }}LocaleConfig",
            ) {
                group = "build"
                description = "Generate locale configuration resources."
                resourcesDir.set(appResourcesDir)
            }
        variant.sources.res?.addGeneratedSourceDirectory(generateLocaleConfig, GenerateLocaleConfigTask::outputDir)
    }
}

dependencies {
    implementation(projects.core.ble)
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.di)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.navigation)
    implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
    implementation(projects.core.network)
    implementation(projects.core.nfc)
    implementation(projects.core.prefs)
    implementation(projects.core.service)
    implementation(projects.core.resources)
    implementation(projects.core.ui)
    implementation(projects.core.barcode)
    implementation(projects.core.takserver)
    implementation(projects.feature.intro)
    implementation(projects.feature.messaging)
    implementation(projects.feature.connections)
    implementation(projects.feature.map)
    implementation(projects.feature.node)
    implementation(projects.feature.settings)
    implementation(projects.feature.discovery)
    implementation(projects.feature.docs)
    implementation(projects.feature.firmware)
    implementation(projects.feature.wifiProvision)
    implementation(projects.feature.widget)

    implementation(libs.jetbrains.compose.material3.adaptive)
    implementation(libs.jetbrains.compose.material3.adaptive.layout)
    implementation(libs.jetbrains.compose.material3.adaptive.navigation)
    implementation(libs.androidx.appcompat)
    implementation(libs.compose.multiplatform.animation)
    implementation(libs.compose.multiplatform.material3)
    implementation(libs.compose.multiplatform.ui.tooling.preview)
    implementation(libs.compose.multiplatform.ui)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.jetbrains.lifecycle.viewmodel.compose)
    implementation(libs.jetbrains.lifecycle.runtime.compose)
    implementation(libs.jetbrains.navigation3.ui)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.coil)
    implementation(libs.coil.network.ktor3)
    implementation(libs.coil.svg)
    implementation(libs.androidx.core.splashscreen)
    // Installs the baseline profile produced by :baselineprofile at app startup (API < 31)
    // and lets ART honor it on first launch. On API 31+ the platform installs it automatically.
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.usb.serial.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.kermit)
    implementation(libs.kotlinx.datetime)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.glance.preview)
    // LeakCanary auto-installs via a manifest-declared ContentProvider -- no init code needed.
    // Debug-only (0 method count in release, per LeakCanary's own design). Added after adversarial
    // mesh-fuzz testing surfaced a GC-thrash concern in the debug log view; this gives continuous,
    // automatic leak detection (dumped heap + leak trace + notification) for future soak/fuzz runs
    // instead of manually polling `dumpsys meminfo`.
    debugImplementation(libs.leakcanary.android)

    // The store build ("google") has no Google services at all: it uses the same sources and the same MapLibre map as
    // fdroid (see sourceSets below). No Google Maps, Firebase, Datadog, ML Kit, Gemini Nano or ARCore.
    googleImplementation(projects.feature.mapMaplibre)
    fdroidImplementation(projects.feature.mapMaplibre)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.androidx.work.testing)
    testImplementation(projects.core.testing)
    testImplementation(libs.koin.test)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.compose.multiplatform.ui.test)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.glance.appwidget)
    // JVM variant provides the host-platform native library for BundledSQLiteDriver under Robolectric
    testRuntimeOnly(libs.androidx.sqlite.bundled.jvm)
}
