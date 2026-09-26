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
package org.meshtastic.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor

@Suppress("EnumEntryName")
enum class FlavorDimension {
    marketplace,
}

@Suppress("EnumEntryName")
enum class MeshtasticFlavor(val dimension: FlavorDimension, val default: Boolean = false) {
    // The "google" flavor (Play Services/Maps/Crashlytics/Datadog) has been removed for this fork -- fdroid is
    // now the only marketplace flavor and the default, so a plain assembleDebug/assembleRelease (no flavor
    // specified) or opening the project in Android Studio builds it, not a nonexistent Google variant.
    fdroid(FlavorDimension.marketplace, default = true),
}

fun configureFlavors(
    commonExtension: CommonExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: MeshtasticFlavor) -> Unit = {},
) {
    commonExtension.apply {
        FlavorDimension.entries.forEach { flavorDimension -> flavorDimensions += flavorDimension.name }

        when (this) {
            is ApplicationExtension ->
                productFlavors {
                    MeshtasticFlavor.entries.forEach { meshtasticFlavor ->
                        register(meshtasticFlavor.name) {
                            dimension = meshtasticFlavor.dimension.name
                            flavorConfigurationBlock(this, meshtasticFlavor)
                            if (meshtasticFlavor.default) {
                                isDefault = true
                            }
                        }
                    }
                }

            is LibraryExtension ->
                productFlavors {
                    MeshtasticFlavor.entries.forEach { meshtasticFlavor ->
                        register(meshtasticFlavor.name) {
                            dimension = meshtasticFlavor.dimension.name
                            flavorConfigurationBlock(this, meshtasticFlavor)
                            if (meshtasticFlavor.default) {
                                isDefault = true
                            }
                        }
                    }
                }
        }
    }
}
