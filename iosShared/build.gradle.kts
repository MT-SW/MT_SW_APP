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

plugins {
    alias(libs.plugins.meshtastic.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.ui)
        }

        // Everything the iPhone app is assembled from (the same modules as the desktop app).
        iosMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.di)
            implementation(projects.core.model)
            implementation(projects.core.navigation)
            implementation(projects.core.repository)
            implementation(projects.core.domain)
            implementation(projects.core.data)
            implementation(projects.core.database)
            implementation(projects.core.datastore)
            implementation(projects.core.prefs)
            implementation(projects.core.network)
            implementation(projects.core.takserver)
            implementation(projects.core.resources)
            implementation(projects.core.service)
            implementation(projects.core.ui)
            implementation(projects.core.ble)
            implementation(projects.feature.settings)
            implementation(projects.feature.docs)
            implementation(projects.feature.node)
            implementation(projects.feature.messaging)
            implementation(projects.feature.connections)
            implementation(projects.feature.map)
            implementation(projects.feature.discovery)
            implementation(projects.feature.firmware)
            implementation(projects.feature.wifiProvision)
            implementation(projects.feature.intro)
            implementation(projects.feature.mapMaplibre)
            implementation(libs.maplibre.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kermit)
            implementation(libs.okio)
            implementation(libs.ktor.client.darwin)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
        }
    }

    // The framework the Xcode app (iosApp) links. Static, so the app needs nothing else embedded.
    // `./gradlew :iosShared:embedAndSignAppleFrameworkForXcode` is run by Xcode itself (see iosApp/project.yml).
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "MTSWShared"
            isStatic = true
        }
    }
}
