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
package org.meshtastic.feature.messaging

import dev.mokkery.MockMode
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.meshtastic.core.repository.SenderMutePrefs
import org.meshtastic.core.repository.TranslatorModelManager
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.testing.FakeUiPrefs
import org.meshtastic.core.ui.util.SnackbarManager
import org.meshtastic.feature.messaging.translation.DictationService
import org.meshtastic.feature.messaging.translation.MessageTranslationService

/** A dictation service that is never available, so the screens under test show no microphone. */
private class UnavailableDictationService : DictationService {
    override val available: StateFlow<Boolean> = MutableStateFlow(false)
    override val translatorAvailable: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun transcribe(wav: ByteArray, translator: Boolean): String = ""
}

/**
 * Starts a Koin container with plain test doubles for what the message screens inject, so a screen can be shown in a
 * Compose test without the whole app graph. Pair with [stopMessagingTestKoin] after the test.
 */
internal fun startMessagingTestKoin() {
    stopKoin()
    startKoin {
        modules(
            module {
                single<UiPrefs> { FakeUiPrefs() }
                single<DictationService> { UnavailableDictationService() }
                single { SnackbarManager() }
                single<SenderMutePrefs> { mock(MockMode.autofill) }
                single<TranslatorModelManager> { mock(MockMode.autofill) }
                single<MessageTranslationService> { mock(MockMode.autofill) }
            },
        )
    }
}

/** Closes the container started by [startMessagingTestKoin]. */
internal fun stopMessagingTestKoin() = stopKoin()
