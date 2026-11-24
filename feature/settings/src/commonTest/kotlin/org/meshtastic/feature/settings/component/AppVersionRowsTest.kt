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
package org.meshtastic.feature.settings.component

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.SnackbarManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AppVersionRowsTest {

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
    fun `Chirpy Hop is visible to everyone without any taps`() = runComposeUiTest {
        var played = false
        setContent {
            AppTheme {
                AppVersionRows(
                    hiddenFeaturesUnlocked = false,
                    appVersionName = "2.8.0",
                    onUnlockHiddenFeatures = {},
                    onPlayChirpyHop = { played = true },
                )
            }
        }

        onNodeWithText("Chirpy Hop").performClick()
        assertTrue(played)
    }

    @Test
    fun `five taps on the version row still unlock the hidden features`() = runComposeUiTest {
        var unlocked by mutableStateOf(false)
        setContent {
            AppTheme {
                Column {
                    AppVersionRows(
                        hiddenFeaturesUnlocked = unlocked,
                        appVersionName = "2.8.0",
                        onUnlockHiddenFeatures = { unlocked = true },
                        onPlayChirpyHop = {},
                    )
                    if (unlocked) Text("odblokowane")
                }
            }
        }
        onNodeWithText("odblokowane").assertDoesNotExist()

        repeat(5) { onNodeWithText("2.8.0").performClick() }

        onNodeWithText("odblokowane").assertExists()
    }
}
