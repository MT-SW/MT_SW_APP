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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.meshtastic.core.common.util.CommonUri
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSString
import platform.Foundation.NSURL.Companion.fileURLWithPath
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.Foundation.NSUTF8StringEncoding
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

private fun topViewController(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
    val window: UIWindow? =
        scene?.windows?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
            ?: scene?.windows?.firstOrNull() as? UIWindow
    var top = window?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

private fun openUrlString(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
}

private fun openAppSettings() = openUrlString(UIApplicationOpenSettingsURLString)

private fun percentEncode(text: String): String = buildString {
    for (b in text.encodeToByteArray()) {
        val c = b.toInt() and 0xFF
        val plain = c.toChar().let { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "-._~" }
        if (plain) append(c.toChar()) else append('%').append(c.toString(16).uppercase().padStart(2, '0'))
    }
}

@Composable actual fun rememberOpenUrl(): (url: String) -> Unit = { url -> openUrlString(url) }

@Composable
actual fun rememberShareText(): (text: String, subject: String) -> Unit = { text, _ ->
    topViewController()?.presentViewController(
        UIActivityViewController(activityItems = listOf(text), applicationActivities = null),
        animated = true,
        completion = null,
    )
}

@Composable
actual fun rememberOpenMap(): (latitude: Double, longitude: Double, label: String) -> Unit = { lat, lon, label ->
    openUrlString("https://maps.apple.com/?ll=$lat,$lon&q=${percentEncode(label)}")
}

@Composable actual fun rememberOpenLocationSettings(): () -> Unit = { openAppSettings() }

@Composable actual fun rememberOpenBluetoothSettings(): () -> Unit = { openAppSettings() }

@Composable actual fun rememberOpenWifiSettings(): () -> Unit = { openAppSettings() }

private class PickerDelegate(var onUri: (CommonUri?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        onUri(url?.absoluteString?.let { CommonUri.parse(it) })
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onUri(null)
    }
}

@Composable
actual fun rememberOpenFileLauncher(onUriReceived: (CommonUri?) -> Unit): (mimeType: String) -> Unit {
    // The delegate is held here because the picker only keeps a weak reference to it.
    val delegate = remember { PickerDelegate(onUriReceived) }
    delegate.onUri = onUriReceived
    return { _ ->
        // The chosen file is copied into the app's own folder, so it can be read without extra permission.
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem), asCopy = true)
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun showToastNow(text: String) {
    val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
    val window = (scene?.windows?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } ?: scene?.windows?.firstOrNull()) as? UIWindow
        ?: return
    val (width, height) = window.bounds.useContents { size.width to size.height }
    val label = UILabel()
    label.text = text
    label.textColor = UIColor.whiteColor
    label.backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.85)
    label.textAlignment = NSTextAlignmentCenter
    label.numberOfLines = 0L
    label.font = UIFont.systemFontOfSize(14.0)
    label.layer.cornerRadius = 14.0
    label.clipsToBounds = true
    val (tw, th) = label.sizeThatFits(CGSizeMake(width - 80.0, 400.0)).useContents { this.width to this.height }
    val w = tw + 32.0
    val h = th + 20.0
    label.setFrame(CGRectMake((width - w) / 2.0, height - h - 110.0, w, h))
    label.alpha = 0.0
    window.addSubview(label)
    UIView.animateWithDuration(0.25) { label.alpha = 1.0 }
    UIView.animateWithDuration(
        duration = 0.4,
        delay = 2.5,
        options = 0uL,
        animations = { label.alpha = 0.0 },
        completion = { _ -> label.removeFromSuperview() },
    )
}

@Composable actual fun rememberShowToast(): suspend (String) -> Unit = { text -> showToastNow(text) }

@Composable
actual fun rememberShowToastResource(): suspend (StringResource) -> Unit = { res -> showToastNow(getString(res)) }

@Composable
actual fun rememberSaveFileLauncher(
    onUriReceived: (CommonUri) -> Unit,
): (defaultFilename: String, mimeType: String) -> Unit {
    val delegate = remember { PickerDelegate { uri -> uri?.let(onUriReceived) } }
    delegate.onUri = { uri -> uri?.let(onUriReceived) }
    return { filename, _ ->
        // The system asks where to keep a file, so an empty file with the wanted name is offered first; the caller
        // then writes into the place that was chosen.
        val safeName = filename.replace('/', '_').ifEmpty { "file" }
        val path = NSTemporaryDirectory().trimEnd('/') + "/" + safeName
        NSString.create(string = "").writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        val picker = UIDocumentPickerViewController(forExportingURLs = listOf(fileURLWithPath(path)), asCopy = true)
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}
