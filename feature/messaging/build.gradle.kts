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

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

plugins {
    alias(libs.plugins.meshtastic.kmp.feature)
    alias(libs.plugins.meshtastic.kotlinx.serialization)
    alias(libs.plugins.meshtastic.kmp.jvm.android)
}

kotlin {
    // No withHostTest: commonTest holds Compose UI tests, which NPE on the host-test stubs' null Build.FINGERPRINT.
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.model)
            implementation(projects.core.navigation)
            implementation(projects.core.prefs)
            implementation(libs.meshtastic.protobufs)
            implementation(projects.core.repository)
            implementation(projects.core.resources)
            implementation(projects.core.ui)

            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.coil)

            // JetBrains Material 3 Adaptive (multiplatform ListDetailPaneScaffold)
            implementation(libs.jetbrains.compose.material3.adaptive)
            implementation(libs.jetbrains.compose.material3.adaptive.layout)
            implementation(libs.jetbrains.compose.material3.adaptive.navigation)
            implementation(libs.jetbrains.compose.material3.adaptive.navigation3)
        }

        // The offline translator runs the same ONNX Runtime API on Android and on desktop; only the native build differs.
        // compileOnly here lets the shared code compile; each platform brings its own runtime below.
        getByName("jvmAndroidMain") {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                compileOnly(libs.onnxruntime)
            }
        }

        androidMain.dependencies {
            implementation(libs.onnxruntime.android)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
        }

        jvmMain.dependencies {
            implementation(libs.onnxruntime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
        }

        // iOS: the HTTP engine for the picture upload/download. Lazy, so hosts without iOS targets are not affected.
        matching { it.name == "iosMain" }
            .configureEach {
                dependencies {
                    implementation(libs.ktor.client.core)
                    implementation(libs.ktor.client.darwin)
                    implementation(libs.okio)
                }
            }

        commonTest.dependencies { implementation(libs.compose.multiplatform.ui.test) }

        jvmTest.dependencies { implementation(compose.desktop.currentOs) }
    }
}

// Gradle's KMP variant resolution follows `available-at` redirects in module

// iPhone: the AI engine (llama.cpp) is linked in the app itself. It is built once by `scripts/build-llama-ios.sh`
// into native/ios/llama.xcframework; without it the iPhone app still builds, only the on-device AI stays off.
val llamaXcframework = rootDir.resolve("native/ios/llama.xcframework")
if (llamaXcframework.isDirectory) {
    kotlin.targets.withType<KotlinNativeTarget>().configureEach {
        if (konanTarget.family == Family.IOS) {
            val slice =
                if (konanTarget.name == "ios_arm64") "ios-arm64" else "ios-arm64_x86_64-simulator"
            val frameworkDir = File(llamaXcframework, "$slice")
            compilations.getByName("main").cinterops.create("llama") {
                defFile(project.file("src/nativeInterop/cinterop/llama.def"))
                compilerOpts("-I${frameworkDir}/llama.framework/Headers")
            }
        }
    }
}
