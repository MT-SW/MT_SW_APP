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
@file:Suppress("LambdaParameterInRestartableEffect", "ReturnCount")

package org.meshtastic.feature.node.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import org.meshtastic.core.repository.MirrorFrame
import org.meshtastic.feature.node.model.GlbBounds
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIColor
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private class ViewerPage(val dir: String, val html: String)

/**
 * Writes the page for the 3D view into a temporary folder: the bundled `model-viewer` library (Google, Apache-2.0) and
 * the model, which the page reads from its own text so no network and no file access from the page is needed.
 */
@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class, BetaInteropApi::class)
private fun prepareViewerPage(glb: ByteArray): ViewerPage? {
    val library = NSBundle.mainBundle.pathForResource("model-viewer.min", ofType = "js") ?: return null
    val fm = NSFileManager.defaultManager
    val dir = NSTemporaryDirectory().trimEnd('/') + "/device-viewer"
    fm.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    val target = "$dir/model-viewer.min.js"
    fm.removeItemAtPath(target, error = null)
    if (!fm.copyItemAtPath(library, toPath = target, error = null)) return null
    val page =
        """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>html,body{margin:0;height:100%;background:transparent;overflow:hidden}
        model-viewer{width:100%;height:100%;background:transparent;--poster-color:transparent}</style>
        <script src="model-viewer.min.js"></script></head><body>
        <model-viewer id="v" camera-controls touch-action="none" environment-image="neutral" shadow-intensity="0.6"
          exposure="1.1" camera-orbit="30deg 70deg auto" interaction-prompt="none"></model-viewer>
        <script>
        const b=atob("${Base64.encode(glb)}");const a=new Uint8Array(b.length);
        for(let i=0;i<b.length;i++)a[i]=b.charCodeAt(i);
        document.getElementById('v').src=URL.createObjectURL(new Blob([a],{type:'model/gltf-binary'}));
        </script></body></html>
        """
            .trimIndent()
    val path = "$dir/index.html"
    val ok =
        NSString.create(string = page)
            .writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    return if (ok) ViewerPage(dir, path) else null
}

/** The 3D device view: the model is shown in a web view with `model-viewer`, so it can be turned with a finger. */
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun DeviceModelScene(
    glb: ByteArray,
    bounds: GlbBounds,
    onError: () -> Unit,
    modifier: Modifier,
    visible: Boolean,
    screen: GlbBounds?,
    screenFrame: MirrorFrame?,
) {
    if (!visible) return
    val page = remember(glb) { prepareViewerPage(glb) }
    if (page == null) {
        LaunchedEffect(Unit) { onError() }
        return
    }
    UIKitView(
        factory = {
            val web = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = WKWebViewConfiguration())
            web.opaque = false
            web.backgroundColor = UIColor.clearColor
            web.scrollView.backgroundColor = UIColor.clearColor
            web.scrollView.bounces = false
            web.loadFileURL(NSURL.fileURLWithPath(page.html), allowingReadAccessToURL = NSURL.fileURLWithPath(page.dir))
            web
        },
        modifier = modifier,
    )
}

internal actual val deviceModelDrawsScreen: Boolean = false
