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
package org.meshtastic.ios.di

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.meshtastic.core.common.BuildConfigProvider
import org.meshtastic.core.common.di.PROCESS_LIFECYCLE

/**
 * Lifecycle of the whole app: on iPhone the process is treated as always resumed (the app keeps its Bluetooth link).
 */
internal class IosProcessLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    init {
        registry.currentState = Lifecycle.State.RESUMED
    }

    override val lifecycle: Lifecycle
        get() = registry
}

/** iPhone platform basics: build information and the process lifecycle. */
@Module
class IosPlatformModule {
    @Single
    fun buildConfigProvider(): BuildConfigProvider = object : BuildConfigProvider {
        override val isDebug: Boolean = false
        override val applicationId: String = "pl.sw.mt"
        override val versionCode: Int = 1
        override val versionName: String = "2.8.5"
        override val absoluteMinFwVersion: String = "2.3.15"
        override val minFwVersion: String = "2.5.14"
    }

    @Single internal fun processLifecycleOwner(): IosProcessLifecycleOwner = IosProcessLifecycleOwner()

    @Single
    @Named(PROCESS_LIFECYCLE)
    internal fun processLifecycle(owner: IosProcessLifecycleOwner): Lifecycle = owner.lifecycle
}
