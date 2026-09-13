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
package org.meshtastic.core.model

/**
 * Which security indicator a surface shows beside a node's name (design#149).
 *
 * 2.8 nodes show verification state instead of the PKI lock, older and unversioned ones keep the locks, and a mismatch
 * shows at any version. Every surface goes through [of] so they cannot drift apart.
 */
enum class NodeSecurityIndicator {
    /** The node's latest key does not match the stored one. Outranks every other state, at any version. */
    KEY_MISMATCH,

    /** This device — the radio the phone is currently connected to. Distinct from [VERIFIED_CONTACT]. */
    LOCAL_NODE,

    /** The user verified this node's key in person. */
    VERIFIED_CONTACT,

    /**
     * The radio verified this node's XEdDSA-signed broadcast. Every 2.8 node signs, so it is that firmware's baseline.
     */
    SIGNED_NODE,

    /** Pre-2.8 or unknown firmware: a public key is on file and matches. */
    PUBLIC_KEY,

    /** NodeInfo has not arrived yet, so whether this node even has a public key is still unknown. */
    NODE_INFO_PENDING,

    /** Pre-2.8 or unknown firmware: no public key on file. Not a channel-key fallback, the send is refused (#145). */
    NO_PUBLIC_KEY,

    ;

    companion object {
        /** The indicator for [node], where [isOwnNode] marks the row for the connected radio. */
        fun of(node: Node, isOwnNode: Boolean = false): NodeSecurityIndicator = resolve(
            firmwareVersion = node.metadata?.firmware_version,
            hasPublicKey = node.hasPKC,
            mismatchKey = node.mismatchKey,
            signed = node.signsPackets,
            verified = node.manuallyVerified,
            isOwnNode = isOwnNode,
            hasNodeInfo = !node.isUnknownUser,
        )

        /** Decides the indicator from snapshot fields alone, so the precedence is testable without a [Node]. */
        fun resolve(
            firmwareVersion: String?,
            hasPublicKey: Boolean,
            mismatchKey: Boolean,
            signed: Boolean = false,
            verified: Boolean = false,
            isOwnNode: Boolean = false,
            hasNodeInfo: Boolean = true,
        ): NodeSecurityIndicator = when {
            mismatchKey -> KEY_MISMATCH

            // Your own radio's key — distinct from a mesh contact verified in person.
            isOwnNode -> LOCAL_NODE

            verified -> VERIFIED_CONTACT

            // A heard signature is the fact itself, the version gate is only its proxy.
            signed || signsBroadcasts(firmwareVersion) -> SIGNED_NODE

            hasPublicKey -> PUBLIC_KEY

            // NodeInfo hasn't arrived yet — this isn't "no key", it's "don't know yet".
            !hasNodeInfo -> NODE_INFO_PENDING

            else -> NO_PUBLIC_KEY
        }

        /**
         * True when [firmwareVersion] is known and is 2.8 or newer.
         *
         * Strict on unknown, unlike the permissive [Capabilities] gates. [DeviceVersion] pads a two-component version,
         * so a radio reporting "2.8" still reads as 2.8.
         */
        fun signsBroadcasts(firmwareVersion: String?): Boolean =
            firmwareVersion?.let { DeviceVersion(it) }?.takeIf { it.isValid }?.let { it >= DeviceVersion("2.8.0") }
                ?: false
    }
}
