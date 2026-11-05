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
package org.meshtastic.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.SnackbarManager

@OptIn(ExperimentalTestApi::class)
class AboutScreenTest {

    @BeforeTest
    fun startKoinForToasts() {
        // On desktop the toast helper looks up the snackbar manager from Koin.
        startKoin { modules(module { single { SnackbarManager() } }) }
    }

    @AfterTest
    fun stopKoinForToasts() {
        stopKoin()
    }

    @Test
    fun `about screen displays all sections and content`() = runComposeUiTest {
        var navigatedUp = false
        var navigatedToAcknowledgements = false

        setContent {
            AppTheme {
                AboutScreen(
                    appVersionName = "2.5.0",
                    onNavigateUp = { navigatedUp = true },
                    onNavigateToAcknowledgements = { navigatedToAcknowledgements = true },
                )
            }
        }

        // Top app bar
        onNodeWithText("About").assertIsDisplayed()

        // What is MT_SW section
        onNodeWithText("What is MT_SW?").assertIsDisplayed()
        onNodeWithText(
            "MT_SW is an app and a community around an open LoRa mesh network that works without the " +
                "internet or base stations: radios pass messages on from node to node. The app extends the " +
                "original with a link planner, a terrain panorama, an offline AI assistant and translator, " +
                "extra statistics, a richer node list and many more settings. We build it together with the " +
                "local community of the Świętokrzyskie region, and anyone can join.",
        )
            .performScrollTo()
            .assertIsDisplayed()

        // Apps section
        onNodeWithText("Apps").assertIsDisplayed()
        onNodeWithText("Need Hardware?").performScrollTo().assertIsDisplayed()
        onNodeWithText(
            "MT_SW works with LoRa radios running mesh firmware (the original or MT_SW_FIRMWARE). " +
                "The documentation and the project website explain how to choose the hardware and set it up.",
        )
            .performScrollTo()
            .assertIsDisplayed()
        onNodeWithText("GitHub Repository").performScrollTo().assertIsDisplayed()
        onNodeWithText("Version").performScrollTo().assertIsDisplayed()
        onNodeWithText("2.5.0").performScrollTo().assertIsDisplayed()
        onNodeWithText("Acknowledgements").performScrollTo().assertIsDisplayed()

        // Project information section
        onNodeWithText("Project information").performScrollTo().assertIsDisplayed()
        onNodeWithText("Website").performScrollTo().assertIsDisplayed()
        onNodeWithText("Documentation").performScrollTo().assertIsDisplayed()
        onNodeWithText("License").performScrollTo().assertIsDisplayed()

        // Copyright footer
        onNodeWithText("Meshtastic® Copyright Meshtastic LLC").performScrollTo().assertIsDisplayed()
        onNodeWithText(
                "Free software under the GNU General Public License v3, with no warranty. " +
                    "You may redistribute it under the same license."
            )
            .performScrollTo()
            .assertIsDisplayed()

        onNodeWithText("Acknowledgements").performScrollTo().performClick()
        assertTrue(navigatedToAcknowledgements)

        onNodeWithContentDescription("Navigate Back").performClick()
        assertTrue(navigatedUp)
    }

    @Test
    fun `license row opens the GPLv3 license`() = runComposeUiTest {
        val openedUris = mutableListOf<String>()
        val uriHandler =
            object : UriHandler {
                override fun openUri(uri: String) {
                    openedUris += uri
                }
            }

        setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                AppTheme { AboutScreen(appVersionName = "2.5.0", onNavigateUp = {}, onNavigateToAcknowledgements = {}) }
            }
        }

        onNodeWithText("License").performScrollTo().performClick()

        assertEquals(listOf("https://www.gnu.org/licenses/gpl-3.0.html"), openedUris)
    }
}
