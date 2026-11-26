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
@file:Suppress("ParameterNaming")

package org.meshtastic.feature.messaging

import androidx.compose.runtime.Composable

/**
 * MT_SW: takes a picture with the camera and hands its bytes over, like [rememberImagePickerLauncher] does for a
 * picture from the gallery. Returns null where the platform has no camera to take a picture with (desktop), so the
 * button is not shown there.
 */
@Composable
expect fun rememberCameraLauncher(onPhotoTaken: (bytes: ByteArray, mimeType: String) -> Unit): (() -> Unit)?
