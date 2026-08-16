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
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.network.CefRequest

private const val TAG = "SitePlannerCoverageRunner"

/**
 * Shims `window.__meshtasticNative.onCoverage(geoJson)` — the bridge API the hosted Site Planner calls once its
 * `run=1&bridge=1` estimate completes — onto JCEF's built-in `window.cefQuery` mechanism. Injected via [onLoadStart] so
 * it exists before the planner's own scripts run (same ordering guarantee Android's transparent-attached-WebView trick
 * relies on, just via a different mechanism).
 */
private const val BRIDGE_SHIM_JS =
    """
    window.__meshtasticNative = {
        onCoverage: function(geoJson) {
            window.cefQuery({
                request: geoJson,
                onSuccess: function(response) {},
                onFailure: function(errorCode, errorMessage) {}
            });
        }
    };
"""

/**
 * Runs one Site Planner coverage estimate headlessly: creates an off-screen-rendered (OSR — JCEF's default) browser for
 * [url], never attaches it to any visible UI component (so [BRIDGE_SHIM_JS]'s result is the only thing this class
 * produces — no Compose overlay-hiding tricks needed, unlike Android's WebView), and reports the coverage GeoJSON via
 * [onResult] once the planner's bridge call arrives. A fresh [CefClient] is created per run and disposed after, so
 * router/handler state never accumulates across estimates.
 */
class SitePlannerCoverageRunner {
    private var client: CefClient? = null
    private var browser: CefBrowser? = null

    /**
     * Starts loading [url] and estimating. [onResult]/[onError] fire at most once each; call [dispose] afterward (or on
     * user cancellation) to release the browser and client.
     */
    fun start(url: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        val cefApp = JcefRuntime.get()
        val newClient = cefApp.createClient()
        client = newClient

        val router = CefMessageRouter.create(CefMessageRouter.CefMessageRouterConfig())
        router.addHandler(
            object : CefMessageRouterHandlerAdapter() {
                override fun onQuery(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    queryId: Long,
                    request: String?,
                    persistent: Boolean,
                    callback: CefQueryCallback?,
                ): Boolean {
                    callback?.success("")
                    if (request != null) {
                        onResult(request)
                    } else {
                        onError("Empty coverage response")
                    }
                    return true
                }
            },
            false,
        )
        newClient.addMessageRouter(router)

        newClient.addLoadHandler(
            object : CefLoadHandlerAdapter() {
                override fun onLoadStart(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    transitionType: CefRequest.TransitionType?,
                ) {
                    if (frame?.isMain == true) {
                        frame.executeJavaScript(BRIDGE_SHIM_JS, frame.url, 0)
                    }
                }

                override fun onLoadError(
                    browser: CefBrowser?,
                    frame: CefFrame?,
                    errorCode: CefLoadHandler.ErrorCode?,
                    errorText: String?,
                    failedUrl: String?,
                ) {
                    if (frame?.isMain == true) {
                        Logger.withTag(TAG).e { "Site Planner load failed: $errorCode $errorText" }
                        onError("$errorCode $errorText")
                    }
                }
            },
        )

        val newBrowser = newClient.createBrowser(url, true, false)
        browser = newBrowser
        newBrowser.createImmediately()
    }

    /** Releases the browser and client. Safe to call more than once. */
    fun dispose() {
        runCatching { browser?.close(true) }.onFailure { Logger.withTag(TAG).w(it) { "Error closing browser" } }
        runCatching { client?.dispose() }.onFailure { Logger.withTag(TAG).w(it) { "Error disposing client" } }
        browser = null
        client = null
    }
}
