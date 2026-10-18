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
package org.meshtastic.feature.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.intro_mtsw_diag_subtitle
import org.meshtastic.core.resources.intro_mtsw_diag_title
import org.meshtastic.core.resources.intro_mtsw_header
import org.meshtastic.core.resources.intro_mtsw_health_subtitle
import org.meshtastic.core.resources.intro_mtsw_health_title
import org.meshtastic.core.resources.intro_mtsw_map_subtitle
import org.meshtastic.core.resources.intro_mtsw_map_title
import org.meshtastic.core.resources.intro_mtsw_messages_subtitle
import org.meshtastic.core.resources.intro_mtsw_messages_title
import org.meshtastic.core.resources.intro_mtsw_planner_subtitle
import org.meshtastic.core.resources.intro_mtsw_planner_title
import org.meshtastic.core.resources.intro_mtsw_remote_subtitle
import org.meshtastic.core.resources.intro_mtsw_remote_title
import org.meshtastic.core.resources.intro_mtsw_scale_subtitle
import org.meshtastic.core.resources.intro_mtsw_scale_title
import org.meshtastic.core.resources.intro_mtsw_signal_subtitle
import org.meshtastic.core.resources.intro_mtsw_signal_title
import org.meshtastic.core.resources.next
import org.meshtastic.core.ui.icon.Antenna
import org.meshtastic.core.ui.icon.BugReport
import org.meshtastic.core.ui.icon.Chart
import org.meshtastic.core.ui.icon.Device
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Message
import org.meshtastic.core.ui.icon.PinDrop
import org.meshtastic.core.ui.icon.Rssi
import org.meshtastic.core.ui.icon.Settings
import org.meshtastic.core.ui.theme.AppTheme

/**
 * The second card of the app introduction: what this app adds on top of the original Meshtastic app. It is kept apart
 * from [WelcomeScreen] so that neither card has to scroll much on a phone.
 *
 * @param onContinue Callback invoked when the user proceeds to the next screen.
 */
@Composable
internal fun MtSwFeaturesScreen(onContinue: () -> Unit, maxContentWidth: Dp = Dp.Unspecified) {
    val features =
        listOf(
            FeatureUIData(
                icon = MeshtasticIcons.Device,
                titleRes = Res.string.intro_mtsw_remote_title,
                subtitleRes = Res.string.intro_mtsw_remote_subtitle,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.Chart,
                titleRes = Res.string.intro_mtsw_health_title,
                subtitleRes = Res.string.intro_mtsw_health_subtitle,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.PinDrop,
                titleRes = Res.string.intro_mtsw_map_title,
                subtitleRes = Res.string.intro_mtsw_map_subtitle,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.Antenna,
                titleRes = Res.string.intro_mtsw_planner_title,
                subtitleRes = Res.string.intro_mtsw_planner_subtitle,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.BugReport,
                titleRes = Res.string.intro_mtsw_diag_title,
                subtitleRes = Res.string.intro_mtsw_diag_subtitle,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.Rssi,
                titleRes = Res.string.intro_mtsw_signal_title,
                subtitleRes = Res.string.intro_mtsw_signal_subtitle,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.Message,
                titleRes = Res.string.intro_mtsw_messages_title,
                subtitleRes = Res.string.intro_mtsw_messages_subtitle,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.Settings,
                titleRes = Res.string.intro_mtsw_scale_title,
                subtitleRes = Res.string.intro_mtsw_scale_subtitle,
            ),
        )

    Scaffold(
        bottomBar = {
            IntroBottomBar(
                onSkip = {},
                onConfigure = onContinue,
                maxContentWidth = maxContentWidth,
                skipButtonText = "",
                configureButtonText = stringResource(Res.string.next),
                showSkipButton = false,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier =
                Modifier
                        .widthIn(max = maxContentWidth)
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.intro_mtsw_header),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                features.forEach { feature ->
                    FeatureRow(feature = feature)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Suppress("PreviewPublic")
@PreviewLightDark
@Composable
fun MtSwFeaturesScreenPreview() {
    AppTheme { Surface { MtSwFeaturesScreen(onContinue = {}) } }
}
