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
@file:Suppress("LongMethod", "MagicNumber")

package org.meshtastic.feature.node.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.add_contact
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.gpio_action_off
import org.meshtastic.core.resources.gpio_action_on
import org.meshtastic.core.resources.gpio_mask_display
import org.meshtastic.core.resources.gpio_pin
import org.meshtastic.core.resources.gpio_pin_hint
import org.meshtastic.core.resources.gpio_pin_invalid
import org.meshtastic.core.resources.gpio_pin_title
import org.meshtastic.core.resources.gpio_pins_get
import org.meshtastic.core.resources.gpio_pins_need_v3
import org.meshtastic.core.resources.gpio_pins_pick
import org.meshtastic.core.resources.gpio_read
import org.meshtastic.core.resources.long_name
import org.meshtastic.core.resources.node_id
import org.meshtastic.core.resources.quick_cmd_hello_desc
import org.meshtastic.core.resources.quick_cmd_ping_desc
import org.meshtastic.core.resources.quick_cmd_test_desc
import org.meshtastic.core.resources.quick_commands_hint
import org.meshtastic.core.resources.quick_commands_need_keys
import org.meshtastic.core.resources.quick_commands_title
import org.meshtastic.core.resources.remote_add_to_list
import org.meshtastic.core.resources.remote_clear_selection
import org.meshtastic.core.resources.remote_contact_hint
import org.meshtastic.core.resources.remote_contact_title
import org.meshtastic.core.resources.remote_id_invalid
import org.meshtastic.core.resources.remote_ignore_confirm_text
import org.meshtastic.core.resources.remote_ignore_confirm_title
import org.meshtastic.core.resources.remote_list_favorites
import org.meshtastic.core.resources.remote_list_ignored
import org.meshtastic.core.resources.remote_manage_hint
import org.meshtastic.core.resources.remote_manage_title
import org.meshtastic.core.resources.remote_pick_from_list
import org.meshtastic.core.resources.remote_remove_from_list
import org.meshtastic.core.resources.remote_remove_node
import org.meshtastic.core.resources.remote_remove_node_text
import org.meshtastic.core.resources.remote_remove_node_title
import org.meshtastic.core.resources.remote_selected_count
import org.meshtastic.core.resources.remove
import org.meshtastic.core.resources.short_name
import org.meshtastic.core.ui.component.Button
import org.meshtastic.core.ui.component.Card
import org.meshtastic.core.ui.component.NodeChip
import org.meshtastic.core.ui.component.OutlinedButton
import org.meshtastic.core.ui.component.pressSpring
import org.meshtastic.core.ui.icon.CheckCircle
import org.meshtastic.core.ui.icon.DoDisturb
import org.meshtastic.core.ui.icon.ErrorOutline
import org.meshtastic.core.ui.icon.Favorite
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.icon.Lock
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.NearMe
import org.meshtastic.core.ui.icon.NotFavorite
import org.meshtastic.core.ui.icon.Output
import org.meshtastic.core.ui.icon.PersonAdd
import org.meshtastic.core.ui.icon.PowerSettingsNew
import org.meshtastic.core.ui.icon.Refresh
import org.meshtastic.core.ui.icon.Send
import org.meshtastic.core.ui.icon.Tune
import org.meshtastic.core.ui.icon.WavingHand
import org.meshtastic.core.ui.theme.StatusColors.StatusOnline
import org.meshtastic.feature.node.health.HealthLevel
import org.meshtastic.feature.node.health.color
import org.meshtastic.feature.node.model.NodeDetailAction
import org.meshtastic.proto.RemoteHardwarePin

private const val MAX_GPIO_PIN = 62
private const val TINT_ALPHA = 0.14f
private const val DISABLED_ALPHA = 0.45f
private const val ICON_BOX_DP = 40

/** A coloured round icon with a title and a short explanation; used to head every panel on this screen. */
@Composable
private fun PanelHeader(icon: ImageVector, title: String, hint: String, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(ICON_BOX_DP.dp).clip(CircleShape).background(accent.copy(alpha = TINT_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A plain card made tappable by hand. The built-in tappable card draws its own touch and hover layer (a lighter
 * rectangle with square corners inside the rounded card, seen when the pointer rests on it and on a greyed out tile);
 * here the whole tile changes colour evenly instead, and gives way under the touch.
 */
@Composable
private fun TileCard(
    onClick: () -> Unit,
    enabled: Boolean,
    container: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    val shown =
        if (hovered && enabled) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = TILE_HOVER_ALPHA).compositeOver(container)
        } else {
            container.dimmed(enabled)
        }
    val inside: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier =
            Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .hoverable(hoverSource)
                .clickable(
                    interactionSource = hoverSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
            content = content,
        )
    }
    val colors = CardDefaults.cardColors(containerColor = shown, disabledContainerColor = shown)
    if (enabled) {
        // Active: the card keeps its own look (depth, glass edge); only what is inside it is tappable, so the shadow
        // outside is not cut off.
        Card(
            modifier = modifier.pressSpring(),
            shape = shape,
            colors = colors,
            border = border,
            content = inside,
        )
    } else {
        // Greyed out: flat, with no shadow.
        Surface(modifier = modifier, shape = shape, color = shown, border = border) { Column(content = inside) }
    }
}

private const val TILE_HOVER_ALPHA = 0.08f

/** A greyed out look is made by fading the colour itself, not the whole card, so the card's depth stays as it is. */
private fun Color.dimmed(enabled: Boolean): Color = if (enabled) this else copy(alpha = alpha * DISABLED_ALPHA)

/** A big tappable tile: icon in a coloured circle over a label. Greyed out and inert when [enabled] is false. */
@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    TileCard(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        container = color.copy(alpha = TINT_ALPHA).compositeOver(base),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f).dimmed(enabled)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Remote Hardware module control. The pin number becomes a bit mask (`1 shl pin`, as in
 * https://meshtastic.org/docs/configuration/module/remote-hardware/#masks); the tiles send WRITE_GPIOS (on / off) or
 * READ_GPIOS to [node]. The module has to be enabled on the node and a shared "gpio" channel must exist on both ends.
 */
@Suppress("UnnecessaryLaunchedEffect")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GpioPanel(
    node: Node,
    onAction: (NodeDetailAction) -> Unit,
    pins: List<RemoteHardwarePin>? = null,
    adminSupported: Boolean = false,
) {
    val gold = MaterialTheme.colorScheme.StatusOnline
    val red = HealthLevel.MEDIUM.color()
    val pinState = rememberTextFieldState("")
    val pinText = pinState.text.toString()
    val pinNumber = pinText.toIntOrNull()
    val gpioMask = pinNumber?.takeIf { it in 0..MAX_GPIO_PIN }?.let { 1L shl it }
    val pinInvalid = pinText.isNotBlank() && gpioMask == null

    // The firmware version is asked for when the card shows (at most once a week); the list of pins needs v3.
    val latestOnAction by rememberUpdatedState(onAction)
    LaunchedEffect(node.num) { latestOnAction(NodeDetailAction.CheckGpioVersion(node.num)) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PanelHeader(
            icon = MeshtasticIcons.Output,
            title = stringResource(Res.string.gpio_pin_title),
            hint = stringResource(Res.string.gpio_pin_hint),
            accent = gold,
        )
        OutlinedTextField(
            state = pinState,
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.gpio_pin)) },
            isError = pinInvalid,
            supportingText = {
                when {
                    pinInvalid -> Text(stringResource(Res.string.gpio_pin_invalid))
                    gpioMask != null -> Text(stringResource(Res.string.gpio_mask_display, "0x" + gpioMask.toString(16)))
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionTile(
                icon = MeshtasticIcons.PowerSettingsNew,
                label = stringResource(Res.string.gpio_action_on),
                color = gold,
                enabled = gpioMask != null,
                onClick = { gpioMask?.let { onAction(NodeDetailAction.WriteGpio(node.num, it, it)) } },
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = MeshtasticIcons.PowerSettingsNew,
                label = stringResource(Res.string.gpio_action_off),
                color = red,
                enabled = gpioMask != null,
                onClick = { gpioMask?.let { onAction(NodeDetailAction.WriteGpio(node.num, it, 0L)) } },
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = MeshtasticIcons.Refresh,
                label = stringResource(Res.string.gpio_read),
                color = MaterialTheme.colorScheme.primary,
                enabled = gpioMask != null,
                onClick = { gpioMask?.let { onAction(NodeDetailAction.ReadGpio(node.num, it)) } },
                modifier = Modifier.weight(1f),
            )
        }
        // A wide fourth button a little lower: it asks the node, over the remote admin, which pins it may control.
        Spacer(Modifier.height(4.dp))
        // The same look as the three tiles above: a tint of the accent colour, a thin border in it and, when active,
        // the card's own shadow (greyed out and flat while the node is not known to run MT_SW_FIRMWARE v3).
        val pinsAccent = MaterialTheme.colorScheme.primary
        val pinsBase = MaterialTheme.colorScheme.surfaceContainerHighest
        TileCard(
            onClick = { onAction(NodeDetailAction.RequestGpioPins(node.num)) },
            enabled = adminSupported,
            modifier = Modifier.fillMaxWidth(),
            container = pinsAccent.copy(alpha = TINT_ALPHA).compositeOver(pinsBase),
            border = BorderStroke(1.dp, pinsAccent.copy(alpha = 0.5f).dimmed(adminSupported)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = MeshtasticIcons.Refresh,
                    contentDescription = null,
                    tint = pinsAccent,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(Res.string.gpio_pins_get),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (!adminSupported) {
            Text(
                text = stringResource(Res.string.gpio_pins_need_v3),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!pins.isNullOrEmpty()) {
            Text(
                text = stringResource(Res.string.gpio_pins_pick),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                pins.forEach { pin ->
                    TileCard(
                        onClick = { pinState.setTextAndPlaceCursorAtEnd(pin.gpio_pin.toString()) },
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        container = pinsAccent.copy(alpha = TINT_ALPHA).compositeOver(pinsBase),
                        border = BorderStroke(1.dp, pinsAccent.copy(alpha = 0.5f)),
                    ) {
                        Text(
                            text = if (pin.name.isBlank()) "${pin.gpio_pin}" else "${pin.name} · ${pin.gpio_pin}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preset text commands sent as a private message to [node] — independent of the normal chat entry point, so they work
 * even for nodes whose role would otherwise hide "Message". Enabled only once a valid public key has been exchanged
 * ([Node.hasPKC] and no [Node.mismatchKey]), so the command really goes out PKI-encrypted.
 */
@Composable
internal fun QuickCommandsPanel(node: Node, onAction: (NodeDetailAction) -> Unit) {
    val canSend = node.hasPKC && !node.mismatchKey
    val commands =
        listOf(
            Triple("/ping", Res.string.quick_cmd_ping_desc, MeshtasticIcons.NearMe),
            Triple("/hello", Res.string.quick_cmd_hello_desc, MeshtasticIcons.WavingHand),
            Triple("/test", Res.string.quick_cmd_test_desc, MeshtasticIcons.Tune),
        )
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PanelHeader(
            icon = MeshtasticIcons.Send,
            title = stringResource(Res.string.quick_commands_title),
            hint = stringResource(Res.string.quick_commands_hint),
            accent = MaterialTheme.colorScheme.primary,
        )
        if (!canSend) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = MeshtasticIcons.Lock,
                    contentDescription = null,
                    tint = HealthLevel.BAD.color(),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(Res.string.quick_commands_need_keys),
                    style = MaterialTheme.typography.labelMedium,
                    color = HealthLevel.BAD.color(),
                )
            }
        }
        commands.forEach { (command, descRes, icon) ->
            CommandRow(
                command = command,
                description = stringResource(descRes),
                icon = icon,
                enabled = canSend,
                onClick = { onAction(NodeDetailAction.SendQuickMessage(node, command)) },
            )
        }
    }
}

@Composable
private fun CommandRow(command: String, description: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    TileCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        container = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(imageVector = MeshtasticIcons.Send, contentDescription = null, tint = accent)
        }
    }
}

private enum class RemoteListType {
    FAVORITE,
    IGNORE,
}

/** A selectable tile for choosing which list on the remote device a change applies to. */
@Composable
private fun ChoiceTile(
    icon: ImageVector,
    label: StringResource,
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    TileCard(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        container = if (selected) color.copy(alpha = TINT_ALPHA * 2).compositeOver(base) else base,
        border =
        BorderStroke(
            if (selected) 2.dp else 1.dp,
            (if (selected) color else color.copy(alpha = 0.25f)).dimmed(enabled),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * While an admin session with [destNum] is active: tell that remote radio to favorite or ignore a node, or add a
 * contact, by its numeric ID. The target need not be in the local node list; everything is administered on the remote
 * radio's own node database over the mesh. The admin protocol only has set / unset commands (no getter), so the app
 * cannot show the real current state of a list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemoteNodeManagementPanel(
    destNum: Int,
    isEnsuringSession: Boolean,
    onAction: (NodeDetailAction) -> Unit,
) {
    val gold = MaterialTheme.colorScheme.StatusOnline
    val purple = HealthLevel.BAD.color()
    val targetIdState = rememberTextFieldState("")
    val longNameState = rememberTextFieldState("")
    val shortNameState = rememberTextFieldState("")
    val idText = targetIdState.text.toString()
    // The app's usual hex node ID ("!a1b2c3d4", with or without the "!").
    val targetNodeNum = NodeAddress.idToNum(idText)
    val idInvalid = idText.isNotBlank() && targetNodeNum == null
    // MT_SW: nodes ticked from our own node list; they are managed together with the typed ID.
    val nodeDb by koinInject<NodeRepository>().nodeDBbyNum.collectAsState()
    val selected = remember { mutableStateListOf<Int>() }
    var pickerOpen by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmIgnore by remember { mutableStateOf(false) }
    val targets = (selected.toList() + listOfNotNull(targetNodeNum)).distinct()
    var listType by remember { mutableStateOf(RemoteListType.FAVORITE) }
    val listColor = if (listType == RemoteListType.FAVORITE) gold else purple

    fun send(add: Boolean) {
        targets.forEach { id ->
            when (listType) {
                RemoteListType.FAVORITE -> onAction(NodeDetailAction.SetRemoteFavorite(destNum, id, add))
                RemoteListType.IGNORE -> onAction(NodeDetailAction.SetRemoteIgnored(destNum, id, add))
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PanelHeader(
            icon = MeshtasticIcons.Favorite,
            title = stringResource(Res.string.remote_manage_title),
            hint = stringResource(Res.string.remote_manage_hint),
            accent = gold,
        )
        OutlinedTextField(
            state = targetIdState,
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.node_id)) },
            placeholder = { Text("!a1b2c3d4") },
            isError = idInvalid,
            supportingText = if (idInvalid) ({ Text(stringResource(Res.string.remote_id_invalid)) }) else null,
            trailingIcon =
            when {
                targetNodeNum != null -> ({ Icon(MeshtasticIcons.CheckCircle, null, tint = gold) })
                idInvalid -> ({ Icon(MeshtasticIcons.ErrorOutline, null, tint = purple) })
                else -> null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            enabled = !isEnsuringSession,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isEnsuringSession,
            onClick = { pickerOpen = true },
        ) {
            Text(stringResource(Res.string.remote_pick_from_list))
        }
        if (selected.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.remote_selected_count, selected.size),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                ) {
                    selected.toList().forEach { num ->
                        nodeDb[num]?.let { node -> NodeChip(node = node, onClick = { selected.remove(num) }) }
                    }
                }
                OutlinedButton(onClick = { selected.clear() }) {
                    Text(stringResource(Res.string.remote_clear_selection))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ChoiceTile(
                icon =
                if (listType == RemoteListType.FAVORITE) MeshtasticIcons.Favorite else MeshtasticIcons.NotFavorite,
                label = Res.string.remote_list_favorites,
                color = gold,
                selected = listType == RemoteListType.FAVORITE,
                enabled = !isEnsuringSession,
                onClick = { listType = RemoteListType.FAVORITE },
                modifier = Modifier.weight(1f),
            )
            ChoiceTile(
                icon = MeshtasticIcons.DoDisturb,
                label = Res.string.remote_list_ignored,
                color = purple,
                selected = listType == RemoteListType.IGNORE,
                enabled = !isEnsuringSession,
                onClick = { listType = RemoteListType.IGNORE },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = !isEnsuringSession && targets.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = listColor, contentColor = Color.Black),
                // Ignoring makes the remote device delete what it stored from the node, so it asks first.
                onClick = { if (listType == RemoteListType.IGNORE) confirmIgnore = true else send(add = true) },
            ) {
                Text(stringResource(Res.string.remote_add_to_list))
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = !isEnsuringSession && targets.isNotEmpty(),
                onClick = { send(add = false) },
            ) {
                Text(stringResource(Res.string.remote_remove_from_list))
            }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isEnsuringSession && targets.isNotEmpty(),
            onClick = { confirmRemove = true },
        ) {
            Text(stringResource(Res.string.remote_remove_node))
        }

        SectionDivider()

        // Adding a contact is a separate action: the admin protocol has no matching "remove contact".
        PanelHeader(
            icon = MeshtasticIcons.PersonAdd,
            title = stringResource(Res.string.remote_contact_title),
            hint = stringResource(Res.string.remote_contact_hint),
            accent = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            state = longNameState,
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.long_name)) },
            enabled = !isEnsuringSession,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            state = shortNameState,
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.short_name)) },
            enabled = !isEnsuringSession,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled =
            !isEnsuringSession &&
                (selected.isNotEmpty() || (targetNodeNum != null && longNameState.text.isNotBlank())),
            onClick = {
                // Ticked nodes are saved with the names from our own list; a typed ID with the names typed here.
                selected.toList().forEach { num ->
                    nodeDb[num]?.let { node ->
                        val contact =
                            NodeDetailAction.AddRemoteContact(destNum, num, node.user.long_name, node.user.short_name)
                        onAction(contact)
                    }
                }
                if (targetNodeNum != null && longNameState.text.isNotBlank()) {
                    onAction(
                        NodeDetailAction.AddRemoteContact(
                            destNum,
                            targetNodeNum,
                            longNameState.text.toString(),
                            shortNameState.text.toString(),
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(MeshtasticIcons.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.add_contact), modifier = Modifier.padding(start = 8.dp))
        }
    }

    if (pickerOpen) {
        RemoteNodePickerSheet(
            nodes = nodeDb.values.filter { it.num != destNum },
            selected = selected.toSet(),
            onToggle = { num ->
                val added = !selected.remove(num)
                if (added) selected.add(num)
                val state = if (added) "zaznaczony" else "odznaczony"
                Logger.i { "Zdalne listy: wybór węzła ${num.toUInt()} $state, razem ${selected.size}" }
            },
            onDismiss = { pickerOpen = false },
        )
    }
    if (confirmIgnore) {
        AlertDialog(
            onDismissRequest = { confirmIgnore = false },
            title = { Text(stringResource(Res.string.remote_ignore_confirm_title)) },
            text = { Text(stringResource(Res.string.remote_ignore_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmIgnore = false
                        send(add = true)
                    },
                ) {
                    Text(stringResource(Res.string.remote_add_to_list))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmIgnore = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(Res.string.remote_remove_node_title)) },
            text = { Text(stringResource(Res.string.remote_remove_node_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        targets.forEach { id -> onAction(NodeDetailAction.RemoveRemoteNode(destNum, id)) }
                    },
                ) {
                    Text(stringResource(Res.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}
