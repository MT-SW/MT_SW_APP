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
@file:Suppress("CompositionLocalAllowlist")

package org.meshtastic.feature.messaging.ui.contact

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.Contact
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.model.util.descriptionKey
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.archived_channel
import org.meshtastic.core.resources.contact_draft_prefix
import org.meshtastic.core.resources.contact_pinned
import org.meshtastic.core.ui.component.Card
import org.meshtastic.core.ui.component.SecurityIcon
import org.meshtastic.core.ui.component.pressSpring
import org.meshtastic.core.ui.icon.History
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.Keep
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.VolumeOff
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.theme.StatusColors.StatusDisconnected
import org.meshtastic.core.ui.component.nodeChipLook
import org.meshtastic.core.ui.theme.rememberUiOptions
import org.meshtastic.proto.ChannelSet

/**
 * MT_SW: the private channel descriptions the conversation list shows in place of the last message, by
 * `ChannelSettings.descriptionKey()`. Empty (the default) means the last message is shown as always.
 */
val LocalChannelDescriptions = staticCompositionLocalOf<Map<String, String>> { emptyMap() }

/** The description to show for this conversation, or null when it is not a channel or has no description. */
private fun Contact.channelDescription(channels: ChannelSet?, descriptions: Map<String, String>): String? {
    if (channels == null || descriptions.isEmpty()) return null
    val isChannel = contactKey.getOrNull(1) == '^' || contactKey.endsWith("^all") || contactKey.endsWith("^broadcast")
    val index = if (isChannel) ContactKey(contactKey).channelOrNull else null
    return index?.let { channels.settings.getOrNull(it) }?.let { descriptions[it.descriptionKey()] }
}

private const val PERCENT = 100f
private const val MIN_CARD_ALPHA = 0.12f

@Suppress("LongMethod")
@Composable
fun ContactItem(
    contact: Contact,
    selected: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onNodeChipClick: () -> Unit = {},
    channels: ChannelSet? = null,
) = with(contact) {
    val isOutlined = !selected && !isActive

    // MT_SW: the look the person chose for the cards (plain, with depth or see-through).
    val cardStyle = rememberUiOptions().element(UiOptions.GROUP_CARDS)
    val withDepth = cardStyle.look == ElementLook.DEPTH
    val seeThrough = cardStyle.look == ElementLook.TRANSPARENT || cardStyle.look == ElementLook.GLASS
    val fade = if (seeThrough) (1f - cardStyle.strength / PERCENT).coerceIn(MIN_CARD_ALPHA, 1f) else 1f

    // MT_SW: an unchosen conversation is nearly white (solid, so nothing shows through from behind it) and is set
    // off
    // only by a gentle shadow; the chosen one (open or selected) has a soft yellowish tint of the theme.
    val surface = MaterialTheme.colorScheme.surface
    val nearlyWhite =
        if (surface.luminance() > LIGHT_SURFACE_LUMINANCE) {
            Color.White.copy(alpha = NEARLY_WHITE_ALPHA).compositeOver(surface)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    val cardColors =
        if (isOutlined) {
            CardDefaults.cardColors(containerColor = nearlyWhite)
        } else {
            val tinted = MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_TINT_ALPHA).compositeOver(surface)
            CardDefaults.cardColors(containerColor = tinted.copy(alpha = fade))
        }

    val border =
        if (isOutlined && !withDepth) {
            CardDefaults.outlinedCardBorder()
        } else {
            null
        }

    Card(
        modifier =
        modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).pressSpring().semantics {
            contentDescription = shortName
        },
        shape = RoundedCornerShape(12.dp),
        colors = cardColors,
        border = border,
    ) {
        // The click sits inside the card, so the hover and press highlight has the card's own rounded shape and
        // does not
        // spill into the margin around it.
        Column(
            modifier =
            Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            ContactHeader(contact = contact, channels = channels, onNodeChipClick = onNodeChipClick)

            val descriptions = LocalChannelDescriptions.current
            val description =
                remember(contact, channels, descriptions) {
                    contact.channelDescription(channels, descriptions)
                }
            ChatMetadata(modifier = Modifier.padding(top = 4.dp), contact = contact, description = description)
        }
    }
}

@Composable
private fun ContactHeader(
    contact: Contact,
    channels: ChannelSet?,
    modifier: Modifier = Modifier,
    onNodeChipClick: () -> Unit = {},
) {
    val chipLabelColor =
        contact.nodeColors?.let { Color(it.first) } ?: MaterialTheme.colorScheme.onSecondaryContainer
    val chipColor = contact.nodeColors?.let { Color(it.second) } ?: MaterialTheme.colorScheme.surfaceContainerHigh

    // The name text carries the colour: main channel in the theme colour, other channels red, direct messages the
    // node's colour.
    val isChannel = with(contact.contactKey) { getOrNull(1) == '^' || endsWith("^all") || endsWith("^broadcast") }
    val channelIndex = if (isChannel) ContactKey(contact.contactKey).channelOrNull else null
    val nodeBackground = contact.nodeColors?.second
    val nameColor =
        when {
            nodeBackground != null -> Color(nodeBackground)
            channelIndex == 0 -> MaterialTheme.colorScheme.primary
            channelIndex != null -> MaterialTheme.colorScheme.StatusDisconnected
            else -> Color.Unspecified
        }

    // The row is as tall as it was with the 48dp touch area of the Material chip, so the rows of conversations with and
    // without a lock icon line up.
    Row(
        modifier = modifier.heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // MT_SW: the same chip as NodeChip (gradient, outline, shadow); built by hand so it covers only the visible
        // chip and not the 48dp touch area.
        val chipShape = MaterialTheme.shapes.small
        Box(
            modifier =
            Modifier.width(IntrinsicSize.Min)
                .then(
                    // A conversation with a node has a chip as big as the node's chip in the node list.
                    if (isChannel) {
                        Modifier.heightIn(min = 32.dp)
                    } else {
                        Modifier.defaultMinSize(
                            minWidth = NODE_CHIP_MIN_WIDTH_DP.dp,
                            minHeight = NODE_CHIP_MIN_HEIGHT_DP.dp,
                        )
                    },
                )
                .then(nodeChipLook(chipColor, chipLabelColor, chipShape))
                .clip(chipShape)
                .clickable(onClick = onNodeChipClick)
                .semantics { contentDescription = contact.shortName },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = contact.shortName,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = chipLabelColor,
            )
        }

        // Show unlock icon for broadcast with default PSK
        val isBroadcast = isChannel

        if (contact.isRetired) {
            // The channel is gone, so there is no key state left to report; say the conversation is archived instead.
            Icon(
                imageVector = MeshtasticIcons.History,
                contentDescription = stringResource(Res.string.archived_channel),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        } else if (isBroadcast && channels != null) {
            ContactKey(contact.contactKey).channelOrNull?.let { index -> SecurityIcon(channels, index) }
        }

        Text(
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            style = MaterialTheme.typography.bodyLargeEmphasized,
            fontWeight = FontWeight.Medium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            color = nameColor,
            text = contact.longName,
        )
        Text(
            text = contact.lastMessageTime?.let { DateFormatter.formatShortDate(it) }.orEmpty(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier,
        )
    }
}

private const val UNREAD_MESSAGE_LIMIT = 99
private const val NODE_CHIP_MIN_WIDTH_DP = 68
private const val NODE_CHIP_MIN_HEIGHT_DP = 32
private const val DESCRIPTION_MAX_LINES = 4

@Composable
private fun ChatMetadata(contact: Contact, modifier: Modifier = Modifier, description: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // An unsent draft outranks the last message: it is the thing this row is waiting on the user for.
        val hasDraft = contact.draft.isNotBlank()
        // A channel with a description shows only that (and the icons beside it), so the row stays short.
        val showDescription = description != null && !hasDraft
        Text(
            text =
            when {
                hasDraft -> stringResource(Res.string.contact_draft_prefix, contact.draft)
                description != null -> description
                else -> contact.lastMessageText.orEmpty()
            },
            modifier = Modifier.weight(1f).semantics { isSensitiveData = !showDescription },
            style = MaterialTheme.typography.bodyMedium,
            color =
            when {
                hasDraft -> MaterialTheme.colorScheme.tertiary
                showDescription -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> Color.Unspecified
            },
            overflow = TextOverflow.Ellipsis,
            maxLines = if (showDescription) DESCRIPTION_MAX_LINES else 2,
        )
        AnimatedVisibility(visible = contact.isPinned) {
            Icon(
                modifier = Modifier.padding(start = 4.dp).size(18.dp),
                imageVector = MeshtasticIcons.Keep,
                contentDescription = stringResource(Res.string.contact_pinned),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        AnimatedVisibility(visible = contact.isMuted) {
            Icon(
                modifier = Modifier.padding(start = 4.dp).size(20.dp),
                imageVector = MeshtasticIcons.VolumeOff,
                contentDescription = null,
            )
        }
        AnimatedVisibility(modifier = Modifier.padding(start = 4.dp), visible = contact.unreadCount > 0) {
            val text =
                if (contact.unreadCount > UNREAD_MESSAGE_LIMIT) {
                    "$UNREAD_MESSAGE_LIMIT+"
                } else {
                    contact.unreadCount.toString()
                }

            Text(
                text = text,
                modifier =
                Modifier.background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    .defaultMinSize(minWidth = 20.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

/**
 * A live channel row above the same conversation once its channel has left the radio.
 *
 * The archived row is the point: it keeps the name the channel had (its slot now belongs to someone else), swaps the
 * key-state lock for a history icon, and is flagged unmessageable.
 */
@PreviewLightDark
@Composable
fun ContactItemArchivedPreview() {
    AppTheme {
        Surface {
            Column {
                ContactItem(
                    contact =
                    Contact(
                        contactKey = ContactKey.broadcast(0).value,
                        shortName = "0",
                        longName = "LongFast",
                        lastMessageTime = null,
                        lastMessageText = "On the air",
                        unreadCount = 0,
                        messageCount = 1,
                        isMuted = false,
                        isUnmessageable = false,
                    ),
                    selected = false,
                )
                ContactItem(
                    contact =
                    Contact(
                        contactKey = ContactKey.retiredBroadcast("a1b2c3d4").value,
                        shortName = "VCFM",
                        longName = "VCFMW",
                        lastMessageTime = null,
                        lastMessageText = "See you next year",
                        unreadCount = 0,
                        messageCount = 42,
                        isMuted = false,
                        isUnmessageable = true,
                        isRetired = true,
                    ),
                    selected = false,
                )
            }
        }
    }
}

private const val SELECTED_TINT_ALPHA = 0.3f
private const val NEARLY_WHITE_ALPHA = 0.9f
private const val LIGHT_SURFACE_LUMINANCE = 0.5f
