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
package org.meshtastic.core.model.util

import org.meshtastic.core.model.Channel
import org.meshtastic.proto.ChannelSettings

/** MT_SW: the longest private description of a channel, in characters. */
const val CHANNEL_DESCRIPTION_MAX_LENGTH = 200

/**
 * The key under which the private description of this channel is kept on the phone (never sent to the radio).
 *
 * It follows the channel itself (its name and effective key), not its slot, so the description stays with the channel
 * when the channels are reordered.
 */
fun ChannelSettings.descriptionKey(): String = ChannelIdentity(name = name, psk = Channel(settings = this).psk).token
