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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ElementLook
import org.meshtastic.core.model.UiOptions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.okay
import org.meshtastic.core.ui.icon.Icon
import org.meshtastic.core.ui.theme.link
import org.meshtastic.core.ui.theme.rememberUiOptions
import org.meshtastic.core.ui.util.annotatedStringFromHtml

/** The confirm and dismiss buttons of a [MeshtasticDialog], for tests that cannot match their resolved labels. */
const val DIALOG_CONFIRM_BUTTON_TAG = "dialog_confirm_button"
const val DIALOG_DISMISS_BUTTON_TAG = "dialog_dismiss_button"

/**
 * A comprehensive and flexible dialog component for the Meshtastic application.
 *
 * @param modifier Modifier for the dialog.
 * @param title The title text of the dialog.
 * @param titleRes The title string resource of the dialog.
 * @param message Optional plain text message.
 * @param messageRes Optional string resource message.
 * @param messageColor Optional color for the plain text message; [Color.Unspecified] uses the default text color.
 * @param html Optional HTML formatted message.
 * @param icon Optional leading icon.
 * @param text Optional custom composable content for the body.
 * @param confirmText Text for the confirmation button.
 * @param confirmTextRes String resource for the confirmation button.
 * @param onConfirm Callback for the confirmation button.
 * @param dismissText Text for the dismiss button.
 * @param dismissTextRes String resource for the dismiss button.
 * @param onDismiss Callback for when the dialog is dismissed or the dismiss button is clicked.
 * @param choices If provided, displays a list of buttons instead of the standard confirm/dismiss actions.
 * @param dismissable Whether the dialog can be dismissed by clicking outside or pressing back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun MeshtasticDialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleRes: StringResource? = null,
    message: String? = null,
    messageRes: StringResource? = null,
    messageColor: Color = Color.Unspecified,
    html: String? = null,
    icon: ImageVector? = null,
    text: @Composable (() -> Unit)? = null,
    confirmText: String? = null,
    confirmTextRes: StringResource? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    dismissTextRes: StringResource? = null,
    onDismiss: (() -> Unit)? = null,
    choices: Map<String, () -> Unit> = emptyMap(),
    dismissable: Boolean = true,
    compact: Boolean = false,
) {
    val titleText = title ?: titleRes?.let { stringResource(it) } ?: ""
    val messageText = message ?: messageRes?.let { stringResource(it) }
    val confirmButtonText = confirmText ?: confirmTextRes?.let { stringResource(it) }
    val dismissButtonText = dismissText ?: dismissTextRes?.let { stringResource(it) }

    val htmlAnnotated = html?.let {
        annotatedStringFromHtml(
            it,
            linkStyles =
            TextLinkStyles(
                style =
                SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.link,
                ),
            ),
        )
    }

    // MT_SW: a window with nothing to decide (only closing) is our window with a cross at the top, like the others.
    if (onConfirm == null && onDismiss != null && choices.isEmpty() && dismissable) {
        InfoWindow(title = titleText, onDismiss = onDismiss, scrolls = text == null, compact = compact) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            if (text != null) {
                text()
            } else if (htmlAnnotated != null) {
                Text(text = htmlAnnotated)
            } else if (messageText != null) {
                Text(text = messageText, color = messageColor)
            }
        }
        return
    }

    // A dialog opens in its own window whose composition resets LocalDensity to the system value, which would
    // ignore the in-app scale setting. The density is captured here and provided again inside the dialog.
    val appDensity = LocalDensity.current
    val scrollState = rememberScrollState()

    BasicAlertDialog(onDismissRequest = { if (dismissable) onDismiss?.invoke() }, modifier = modifier) {
        CompositionLocalProvider(LocalDensity provides appDensity) {
            // MT_SW: the look the person chose for dialogs (plain, with depth, see-through or glass).
            val dialogLook = rememberUiOptions().element(UiOptions.GROUP_DIALOGS)
            val dialogFade =
                if (dialogLook.look == ElementLook.TRANSPARENT || dialogLook.look == ElementLook.GLASS) {
                    // The strength 0-100% covers the whole allowed range (from opaque down to the readable limit).
                    (1f - dialogLook.strength / DIALOG_PERCENT * (1f - DIALOG_MIN_ALPHA)).coerceIn(DIALOG_MIN_ALPHA, 1f)
                } else {
                    1f
                }
            Surface(
                modifier =
                Modifier.widthIn(min = 280.dp, max = 560.dp)
                    .elementLook(UiOptions.GROUP_DIALOGS, RoundedCornerShape(28.dp))
                    // MT_SW: every dialog has the gradient sheen and a thin rim (at least 1 dp) like the other
                    // elements.
                    .popupRim(RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = AlertDialogDefaults.containerColor.copy(alpha = dialogFade),
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
                        )
                    }
                    Text(
                        text = titleText,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                    )
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                        LocalScrollbarOutset provides DIALOG_BAR_OUTSET_DP.dp,
                    ) {
                        val bodyScroll =
                            if (choices.isNotEmpty()) Modifier.verticalScrollWithBar(scrollState) else Modifier
                        Column(modifier = Modifier.weight(1f, fill = false).then(bodyScroll)) {
                            if (text != null) {
                                text()
                            } else if (htmlAnnotated != null) {
                                Text(text = htmlAnnotated)
                            } else if (messageText != null) {
                                Text(text = messageText, color = messageColor)
                            }

                            if (choices.isNotEmpty()) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    choices.forEach { (choice, action) ->
                                        Button(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            onClick = {
                                                action()
                                                onDismiss?.invoke()
                                            },
                                        ) {
                                            Text(text = choice)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (choices.isEmpty() && (onDismiss != null || onConfirm != null)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (onDismiss != null) {
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.padding(horizontal = 16.dp).testTag(DIALOG_DISMISS_BUTTON_TAG),
                                    colors =
                                    ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                ) {
                                    Text(text = dismissButtonText ?: stringResource(Res.string.cancel))
                                }
                            }
                            if (onConfirm != null) {
                                TextButton(
                                    onClick = onConfirm,
                                    modifier = Modifier.padding(horizontal = 16.dp).testTag(DIALOG_CONFIRM_BUTTON_TAG),
                                    colors =
                                    ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                ) {
                                    Text(text = confirmButtonText ?: stringResource(Res.string.okay))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A simplified [MeshtasticDialog] using only string resources. */
@Composable
fun MeshtasticResourceDialog(
    modifier: Modifier = Modifier,
    titleRes: StringResource,
    messageRes: StringResource,
    confirmTextRes: StringResource? = null,
    dismissTextRes: StringResource? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    dismissable: Boolean = true,
) {
    MeshtasticDialog(
        modifier = modifier,
        titleRes = titleRes,
        messageRes = messageRes,
        confirmTextRes = confirmTextRes,
        dismissTextRes = dismissTextRes,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        dismissable = dismissable,
    )
}

/** A simplified [MeshtasticDialog] using a title resource and a plain text message. */
@Composable
fun MeshtasticTextDialog(
    modifier: Modifier = Modifier,
    titleRes: StringResource,
    message: String,
    confirmTextRes: StringResource? = null,
    dismissTextRes: StringResource? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    dismissable: Boolean = true,
) {
    MeshtasticDialog(
        modifier = modifier,
        titleRes = titleRes,
        message = message,
        confirmTextRes = confirmTextRes,
        dismissTextRes = dismissTextRes,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        dismissable = dismissable,
    )
}

/** Both windows have a 24 dp side margin, which the scroll bar uses. */
private const val DIALOG_BAR_OUTSET_DP = 24

private const val DIALOG_PERCENT = 100f
private const val DIALOG_MIN_ALPHA = 0.85f

/**
 * MT_SW: the same as the Material pop-up window `AlertDialog` (same parameters), but in the look of our app: the shadow
 * and the thin rim with the sheen of the "dialogs" group, and its see-through level. Use this instead of the Material
 * one so that every window looks the same.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    val options = rememberUiOptions()
    val dialogLook = options.element(UiOptions.GROUP_DIALOGS)
    val fade =
        if (dialogLook.look == ElementLook.TRANSPARENT || dialogLook.look == ElementLook.GLASS) {
            (1f - dialogLook.strength / DIALOG_PERCENT * (1f - DIALOG_MIN_ALPHA)).coerceIn(DIALOG_MIN_ALPHA, 1f)
        } else {
            1f
        }
    // The dialog opens in its own window, which would lose the in-app scale; its content gets it back.
    val appDensity = LocalDensity.current
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.elementLook(UiOptions.GROUP_DIALOGS, shape).popupRim(shape),
        properties = properties,
    ) {
        ProvideAppDensity(appDensity) {
            Surface(
                modifier = Modifier.widthIn(min = ALERT_MIN_WIDTH_DP.dp, max = ALERT_MAX_WIDTH_DP.dp),
                shape = shape,
                color = containerColor.copy(alpha = containerColor.alpha * fade),
                tonalElevation = tonalElevation,
            ) {
                Column(modifier = Modifier.padding(ALERT_PADDING_DP.dp)) {
                    val centered = icon != null
                    if (icon != null) {
                        CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                            Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)) {
                                icon()
                            }
                        }
                    }
                    if (title != null) {
                        CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                            ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                                Box(
                                    modifier =
                                    Modifier.align(if (centered) Alignment.CenterHorizontally else Alignment.Start)
                                        .padding(bottom = 16.dp),
                                ) {
                                    title()
                                }
                            }
                        }
                    }
                    if (text != null) {
                        CompositionLocalProvider(
                            LocalContentColor provides textContentColor,
                            LocalScrollbarOutset provides DIALOG_BAR_OUTSET_DP.dp,
                        ) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                                Box(
                                    modifier =
                                    Modifier.weight(1f, fill = false).align(Alignment.Start).padding(bottom = 24.dp),
                                ) {
                                    text()
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.align(Alignment.End)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

private const val ALERT_MIN_WIDTH_DP = 280
private const val ALERT_MAX_WIDTH_DP = 560
private const val ALERT_PADDING_DP = 24

/**
 * MT_SW: how much of the fill of a pop-up window (dialog, menu, sheet) is left for the look chosen for the "dialogs"
 * group: 1 for the plain look and the look with depth, less for the see-through and glass ones.
 */
@Composable
internal fun popupFade(): Float {
    val look = rememberUiOptions().element(UiOptions.GROUP_DIALOGS)
    return if (look.look == ElementLook.TRANSPARENT || look.look == ElementLook.GLASS) {
        (1f - look.strength / DIALOG_PERCENT * (1f - DIALOG_MIN_ALPHA)).coerceIn(DIALOG_MIN_ALPHA, 1f)
    } else {
        1f
    }
}

/** MT_SW: the thin rim with the sheen of a pop-up window, except for the look with depth, which draws its own. */
@Composable
fun Modifier.popupRim(shape: Shape): Modifier {
    val options = rememberUiOptions()
    return if (options.element(UiOptions.GROUP_DIALOGS).look == ElementLook.DEPTH) {
        this
    } else {
        glassEdge(shape, maxOf(options.rimDp(), 1f))
    }
}
