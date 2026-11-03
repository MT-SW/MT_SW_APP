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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
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

/** Fast: any colour close enough, about 20 s at most. Precise: much closer match, but may take a few minutes. */
private const val FAST_TOLERANCE = 12
private const val PRECISE_TOLERANCE = 4
private const val FAST_TIMEOUT_MS = 20_000L
private const val PRECISE_TIMEOUT_MS = 180_000L
private const val MAX_RECENT = 8

/** Colours used earlier in this session (newest first), so they can be picked again quickly. */
private val recentColors = mutableStateListOf<Int>()

private fun rgbToHsv(rgb: Int): FloatArray {
    val r = ((rgb shr 16) and 0xFF) / 255f
    val g = ((rgb shr 8) and 0xFF) / 255f
    val b = (rgb and 0xFF) / 255f
    val hi = max(r, max(g, b))
    val lo = min(r, min(g, b))
    val d = hi - lo
    val h =
        when {
            d == 0f -> 0f
            hi == r -> 60f * (((g - b) / d) % 6f)
            hi == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }
    return floatArrayOf(if (h < 0f) h + 360f else h, if (hi == 0f) 0f else d / hi, hi)
}

private fun hsvToRgb(h: Float, s: Float, v: Float): Int = Color.hsv(h, s, v).let {
    ((it.red * 255f).roundToInt() shl 16) or ((it.green * 255f).roundToInt() shl 8) or (it.blue * 255f).roundToInt()
}

private fun hexOf(rgb: Int): String = (rgb and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

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
    var precise by remember { mutableStateOf(false) }
    var hexText by remember { mutableStateOf(hexOf(VANITY_COLOR_PALETTE.first().rgb)) }
    var seconds by remember { mutableIntStateOf(0) }
    var grindJob by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf<VanityGrindStatus>(VanityGrindStatus.Idle) }

    LaunchedEffect(status) {
        seconds = 0
        while (status is VanityGrindStatus.Grinding) {
            delay(1000)
            seconds++
        }
    }

    fun choose(rgb: Int) {
        selectedColor = rgb and 0xFFFFFF
        hexText = hexOf(selectedColor)
        status = VanityGrindStatus.Idle
    }

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
                                .clickable(enabled = status !is VanityGrindStatus.Grinding) { choose(option.rgb) },
                        )
                    }
                }

                val editable = status !is VanityGrindStatus.Grinding
                val hsv = rgbToHsv(selectedColor)
                Text(text = "Dowolny kolor", modifier = Modifier.padding(top = 16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier =
                        Modifier.size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF000000.toInt() or selectedColor))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    )
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { input ->
                            val clean = input.removePrefix("#").filter { it.isLetterOrDigit() }.take(6).uppercase()
                            hexText = clean
                            if (clean.length == 6) {
                                clean.toIntOrNull(16)?.let {
                                    selectedColor = it
                                    status = VanityGrindStatus.Idle
                                }
                            }
                        },
                        label = { Text("Kod koloru (np. FF8800)") },
                        singleLine = true,
                        enabled = editable,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(text = "Odcień")
                Slider(
                    value = hsv[0],
                    onValueChange = { choose(hsvToRgb(it, hsv[1], hsv[2])) },
                    enabled = editable,
                    valueRange = 0f..359f,
                )
                Text(text = "Nasycenie")
                Slider(
                    value = hsv[1],
                    onValueChange = { choose(hsvToRgb(hsv[0], it, hsv[2])) },
                    enabled = editable,
                    valueRange = 0f..1f,
                )
                Text(text = "Jasność")
                Slider(
                    value = hsv[2],
                    onValueChange = { choose(hsvToRgb(hsv[0], hsv[1], it)) },
                    enabled = editable,
                    valueRange = 0f..1f,
                )

                if (recentColors.isNotEmpty()) {
                    Text(text = "Ostatnio użyte", modifier = Modifier.padding(top = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentColors.forEach { rgb ->
                            Box(
                                modifier =
                                Modifier.size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF000000.toInt() or rgb))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable(enabled = editable) { choose(rgb) },
                            )
                        }
                    }
                }

                Text(text = "Tryb szukania", modifier = Modifier.padding(top = 16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !precise,
                        onClick = { precise = false },
                        enabled = editable,
                        label = { Text("Szybko (do 20 s)") },
                    )
                    FilterChip(
                        selected = precise,
                        onClick = { precise = true },
                        enabled = editable,
                        label = { Text("Dokładnie (do 3 min)") },
                    )
                }
                Text(
                    text =
                    if (precise) {
                        "Kolor wyjdzie bardzo blisko wybranego, ale szukanie może trwać kilka minut."
                    } else {
                        "Kolor będzie podobny do wybranego, szukanie zwykle trwa kilka sekund."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )

                when (val current = status) {
                    is VanityGrindStatus.Grinding -> {
                        Row(modifier = Modifier.padding(top = 16.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Szukam klucza... ${seconds} s")
                        }
                        TextButton(
                            onClick = {
                                grindJob?.cancel()
                                status = VanityGrindStatus.Idle
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "Przerwij szukanie")
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
                            text = "Nie znaleziono w tym czasie — spróbuj trybu „Szybko” albo innego koloru.",
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
                                    val result =
                                        grinder(
                                            selectedColor,
                                            if (precise) PRECISE_TOLERANCE else FAST_TOLERANCE,
                                            if (precise) PRECISE_TIMEOUT_MS else FAST_TIMEOUT_MS,
                                        )
                                    status =
                                        if (result != null) {
                                            recentColors.remove(selectedColor)
                                            recentColors.add(0, selectedColor)
                                            while (recentColors.size > MAX_RECENT) recentColors.removeAt(recentColors.lastIndex)
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
