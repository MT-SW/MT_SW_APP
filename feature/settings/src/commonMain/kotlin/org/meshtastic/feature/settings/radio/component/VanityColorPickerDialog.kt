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
package org.meshtastic.feature.settings.radio.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.ByteString
import org.koin.compose.koinInject
import org.meshtastic.core.domain.usecase.settings.GenerateVanityKeyUseCase
import org.meshtastic.core.domain.usecase.settings.VanityKeyResult
import org.meshtastic.core.ui.component.MeshtasticDialog
import kotlin.math.roundToInt

private data class VanityColorOption(val name: String, val rgb: Int)

private val VANITY_COLOR_PALETTE =
    listOf(
        VanityColorOption("Crimson", 0xdc143c),
        VanityColorOption("Coral", 0xff7f50),
        VanityColorOption("Gold", 0xffd700),
        VanityColorOption("Lime", 0x32cd32),
        VanityColorOption("Teal", 0x008080),
        VanityColorOption("SkyBlue", 0x87ceeb),
        VanityColorOption("RoyalBlue", 0x4169e1),
        VanityColorOption("Indigo", 0x4b0082),
        VanityColorOption("Orchid", 0xda70d6),
        VanityColorOption("HotPink", 0xff69b4),
        VanityColorOption("Tomato", 0xff6347),
        VanityColorOption("Chocolate", 0xd2691e),
        VanityColorOption("Olive", 0x808000),
        VanityColorOption("SeaGreen", 0x2e8b57),
        VanityColorOption("Turquoise", 0x40e0d0),
        VanityColorOption("SlateBlue", 0x6a5acd),
        VanityColorOption("Plum", 0xdda0dd),
        VanityColorOption("Salmon", 0xfa8072),
        VanityColorOption("Khaki", 0xf0e68c),
        VanityColorOption("SteelBlue", 0x4682b4),
    )

private const val DEFAULT_TOLERANCE = 8
private const val MAX_TOLERANCE = 24

private sealed interface VanityGrindStatus {
    data object Idle : VanityGrindStatus

    data object Grinding : VanityGrindStatus

    data class Found(val result: VanityKeyResult) : VanityGrindStatus

    data object TimedOut : VanityGrindStatus

    data object FirmwareTooOld : VanityGrindStatus
}

@Composable
private fun ColorPreview(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
            Modifier.size(48.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
        Text(text = label, modifier = Modifier.padding(top = 4.dp))
    }
}

/**
 * Lets the user pick a target node color and grinds a matching X25519 private key on-device (see
 * [GenerateVanityKeyUseCase]). Always openable regardless of firmware version — [supported] only gates what happens
 * when "Zatwierdź" is pressed: on firmware without nodeNum-from-public-key (pre-2.8.0) the found key would never
 * actually paint that color once connected, so we explain that instead of grinding.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VanityColorPickerDialog(supported: Boolean, onKeyFound: (ByteString) -> Unit, onDismiss: () -> Unit) {
    val grinder: GenerateVanityKeyUseCase = koinInject()
    val scope = rememberCoroutineScope()

    var selectedColor by remember { mutableStateOf(VANITY_COLOR_PALETTE.first().rgb) }
    var tolerance by remember { mutableFloatStateOf(DEFAULT_TOLERANCE.toFloat()) }
    var grindJob by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf<VanityGrindStatus>(VanityGrindStatus.Idle) }

    if (status is VanityGrindStatus.FirmwareTooOld) {
        MeshtasticDialog(
            title = "Wymagana aktualizacja",
            message = "Aby użyć tej funkcji, zaktualizuj oprogramowanie urządzenia do wersji 2.8.0 lub wyższej.",
            dismissText = "Zamknij",
            onDismiss = onDismiss,
        )
        return
    }

    MeshtasticDialog(
        title = "Wybierz kolor",
        dismissText = "Anuluj",
        dismissable = status !is VanityGrindStatus.Grinding,
        onDismiss = {
            grindJob?.cancel()
            onDismiss()
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VANITY_COLOR_PALETTE.forEach { option ->
                        val swatchColor = Color(0xFF000000.toInt() or option.rgb)
                        Box(
                            modifier =
                            Modifier.size(40.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (option.rgb == selectedColor) 3.dp else 1.dp,
                                    color =
                                    if (option.rgb == selectedColor) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape,
                                )
                                .clickable(enabled = status !is VanityGrindStatus.Grinding) {
                                    selectedColor = option.rgb
                                    status = VanityGrindStatus.Idle
                                },
                        )
                    }
                }

                Text(text = "Tolerancja: ${tolerance.roundToInt()}", modifier = Modifier.padding(top = 16.dp))
                Slider(
                    value = tolerance,
                    onValueChange = { tolerance = it },
                    enabled = status !is VanityGrindStatus.Grinding,
                    valueRange = 0f..MAX_TOLERANCE.toFloat(),
                    steps = MAX_TOLERANCE - 1,
                )

                when (val current = status) {
                    is VanityGrindStatus.Grinding -> {
                        Row(modifier = Modifier.padding(top = 16.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Szukam klucza...")
                        }
                    }

                    is VanityGrindStatus.Found -> {
                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            ColorPreview(label = "Wybrany", color = Color(0xFF000000.toInt() or selectedColor))
                            ColorPreview(label = "Wynik", color = Color(current.result.nodeColor))
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            TextButton(onClick = { status = VanityGrindStatus.Idle }, modifier = Modifier.weight(1f)) {
                                Text(text = "Szukaj ponownie")
                            }
                            TextButton(
                                onClick = { onKeyFound(current.result.privateKey) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(text = "Zastosuj")
                            }
                        }
                    }

                    is VanityGrindStatus.TimedOut -> {
                        Text(
                            text = "Nie znaleziono w rozsądnym czasie — spróbuj zwiększyć tolerancję.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }

                    else -> Unit
                }

                if (status !is VanityGrindStatus.Found) {
                    TextButton(
                        onClick = {
                            if (!supported) {
                                status = VanityGrindStatus.FirmwareTooOld
                                return@TextButton
                            }
                            status = VanityGrindStatus.Grinding
                            grindJob =
                                scope.launch {
                                    val result = grinder(selectedColor, tolerance.roundToInt())
                                    status =
                                        if (result != null) {
                                            VanityGrindStatus.Found(result)
                                        } else {
                                            VanityGrindStatus.TimedOut
                                        }
                                }
                        },
                        enabled = status !is VanityGrindStatus.Grinding,
                        colors =
                        if (supported) {
                            ButtonDefaults.textButtonColors()
                        } else {
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        Text(text = "Zatwierdź")
                    }
                }
            }
        },
    )
}
