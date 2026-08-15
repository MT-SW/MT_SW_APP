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
package org.meshtastic.desktop.siteplanner

import co.touchlab.kermit.Logger
import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.meshtastic.core.database.desktopDataDir
import java.io.File

private const val TAG = "JcefRuntime"

/**
 * Lazily-initialized singleton owner of the app's one [CefApp] instance — CEF (Chromium Embedded Framework) only
 * supports a single instance per process. Used exclusively by the Site Planner coverage-estimate bridge, so init is
 * deferred to first use rather than paid at app startup; the native Chromium bundle (~100+MB) downloads and extracts
 * on first run if not already present under [installDir].
 *
 * OSR (off-screen rendering) mode is [CefAppBuilder]'s default — we rely on that rather than setting it explicitly,
 * since the Site Planner bridge never displays the browser (see [SitePlannerCoverageRunner]).
 */
object JcefRuntime {
    private val installDir = File(desktopDataDir(), "jcef-bundle")

    private val cefApp: CefApp by lazy {
        Logger.withTag(TAG).i { "Initializing JCEF (first use) — installDir=${installDir.absolutePath}" }
        val builder = CefAppBuilder()
        builder.setInstallDir(installDir)
        // Do not use CefApp.addAppHandler(...) — the README explicitly warns it breaks on macOS.
        builder.setAppHandler(object : MavenCefAppHandlerAdapter() {})
        builder.build()
    }

    /** Returns the shared [CefApp], initializing it on first call. May block for seconds (or minutes on first-ever
     * run, while the native bundle downloads) — call from a background dispatcher, never the UI thread. */
    fun get(): CefApp = cefApp
}
