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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.okay
import org.meshtastic.core.ui.theme.link
import org.meshtastic.core.ui.util.annotatedStringFromHtml

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
) {
    val titleText = title ?: titleRes?.let { stringResource(it) } ?: ""
    val messageText = message ?: messageRes?.let { stringResource(it) }
    val confirmButtonText = confirmText ?: confirmTextRes?.let { stringResource(it) }
    val dismissButtonText = dismissText ?: dismissTextRes?.let { stringResource(it) }

    val htmlAnnotated =
        html?.let {
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

    // A dialog opens in its own window whose composition resets LocalDensity to the system value, which would
    // ignore the in-app scale setting. The density is captured here and provided again inside the dialog.
    val appDensity = LocalDensity.current
    val scrollState = rememberScrollState()
    val bodyScroll = if (choices.isNotEmpty()) Modifier.verticalScroll(scrollState) else Modifier

    BasicAlertDialog(onDismissRequest = { if (dismissable) onDismiss?.invoke() }, modifier = modifier) {
        CompositionLocalProvider(LocalDensity provides appDensity) {
            Surface(
                modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
                shape = RoundedCornerShape(28.dp),
                color = AlertDialogDefaults.containerColor,
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
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        Column(
                            modifier =
                            Modifier.weight(1f, fill = false).then(bodyScroll),
                        ) {
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
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    colors =
                                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                ) {
                                    Text(text = dismissButtonText ?: stringResource(Res.string.cancel))
                                }
                            }
                            if (onConfirm != null) {
                                TextButton(
                                    onClick = onConfirm,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    colors =
                                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
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
