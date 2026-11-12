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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLinkStyles

@OptIn(ExperimentalComposeUiApi::class)
actual fun createClipEntry(text: String, label: String, sensitive: Boolean): ClipEntry = ClipEntry.withPlainText(text)

actual fun ClipEntry.plainText(): String? = platform.UIKit.UIPasteboard.generalPasteboard.string

actual fun annotatedStringFromHtml(html: String, linkStyles: TextLinkStyles?): AnnotatedString =
    simpleAnnotatedStringFromHtml(html, linkStyles)

@Composable actual fun rememberOpenNfcSettings(): () -> Unit = {}

actual val isDesktopPlatform: Boolean = false

actual val bleScanRequiresLocationServices: Boolean = false

@Composable actual fun isGpsDisabled(): Boolean = false

@Composable actual fun isBluetoothSupported(): Boolean = true

@Composable actual fun isWifiUnavailable(): Boolean = false

@Composable actual fun rememberBluetoothPermissionState(): PermissionUiState = grantedPermissionUiState()

@Composable actual fun rememberLocalNetworkPermissionState(): PermissionUiState = grantedPermissionUiState()

@Composable actual fun rememberCameraPermissionState(): PermissionUiState = grantedPermissionUiState()

actual fun KeyEvent.isFromSoftKeyboard(): Boolean = false
