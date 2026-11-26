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
@file:Suppress("MatchingDeclarationName", "ParameterNaming")

package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable

/** A picture chosen from the gallery. */
class PickedImage(val bytes: ByteArray, val mimeType: String)

/**
 * MT_SW: lets the person choose many pictures from the gallery at once (up to [MAX_PICKED_IMAGES]); [onPicked] gets
 * them in the order chosen. Returns null where the platform cannot do it.
 */
@Composable expect fun rememberMultiImagePicker(onPicked: (List<PickedImage>) -> Unit): (() -> Unit)?

const val MAX_PICKED_IMAGES = 20
