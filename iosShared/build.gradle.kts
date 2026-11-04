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
    alias(libs.plugins.meshtastic.kmp.library)
    alias(libs.plugins.meshtastic.kmp.library.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.material3)
            implementation(libs.compose.multiplatform.ui)
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
