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
package org.meshtastic.core.service

import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.util.toSharedContact
import org.meshtastic.proto.SharedContact
import org.meshtastic.proto.User

/**
 * MT_SW: the contact sent to a remote device. A node we hold in our database goes with its trusted public key, so the
 * remote device can exchange direct messages with it. A node typed in by its ID (or one without a trusted key) goes
 * without a key, with just the ID and the given names.
 */
internal fun buildRemoteContact(node: Node?, nodeNum: Int, longName: String, shortName: String): SharedContact {
    val known = node?.toSharedContact()
    val key = known?.user?.public_key
    if (known != null && key != null && key.size == Node.PUBLIC_KEY_SIZE && key != Node.ERROR_BYTE_STRING) return known
    val user =
        User.Builder()
            .also { wb ->
                wb.id = NodeAddress.numToDefaultId(nodeNum)
                wb.long_name = longName
                wb.short_name = shortName
            }
            .build()
    return SharedContact.Builder()
        .also { wb ->
            wb.node_num = nodeNum
            wb.user = user
        }
        .build()
}
