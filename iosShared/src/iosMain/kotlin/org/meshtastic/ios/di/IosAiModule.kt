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
package org.meshtastic.ios.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.meshtastic.core.repository.AiModelStore
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.AiPlatform
import org.meshtastic.core.repository.AssistantMemory
import org.meshtastic.core.repository.MemoryNote
import org.meshtastic.core.repository.TranslatorModelManager
import org.meshtastic.core.repository.TranslatorModelState
import org.meshtastic.feature.discovery.ai.AlgorithmicSummaryProvider
import org.meshtastic.feature.discovery.ai.DiscoverySummaryAiProvider
import org.meshtastic.feature.docs.ai.AIDocAssistant
import org.meshtastic.feature.docs.ai.KeywordFallbackAssistant
import org.meshtastic.feature.docs.translation.DocTranslationService
import org.meshtastic.feature.docs.translation.NoOpDocTranslator
import org.meshtastic.feature.messaging.translation.AssistantHistoryStore
import org.meshtastic.feature.messaging.translation.AssistantService
import org.meshtastic.feature.messaging.translation.AssistantTurn
import org.meshtastic.feature.messaging.translation.DictationService
import org.meshtastic.feature.messaging.translation.MessageTranslationService
import org.meshtastic.feature.messaging.translation.NoOpMessageTranslator
import org.meshtastic.feature.messaging.translation.StoredChat

/**
 * AI on iPhone: the on-device engines (assistant, translator, dictation) are not ported yet, so these are inactive
 * stand-ins; the guide search and the discovery summary work without a model.
 */
@Module
class IosAiModule {
    @Single fun aiDocAssistant(fallback: KeywordFallbackAssistant): AIDocAssistant = fallback

    @Single fun discoverySummaryAiProvider(fallback: AlgorithmicSummaryProvider): DiscoverySummaryAiProvider = fallback

    @Single fun docTranslationService(): DocTranslationService = NoOpDocTranslator()

    @Single fun messageTranslationService(): MessageTranslationService = NoOpMessageTranslator()

    @Single
    fun aiModelStore(): AiModelStore {
        AiModels.platform = AiPlatform.IOS
        return IosAiModelStore()
    }

    @Single fun translatorModelManager(): TranslatorModelManager = IosTranslatorModelManager()

    @Single fun assistantService(): AssistantService = IosAssistantService()

    @Single fun dictationService(): DictationService = IosDictationService()

    @Single fun assistantHistoryStore(): AssistantHistoryStore = IosAssistantHistoryStore()

    @Single fun assistantMemory(): AssistantMemory = IosAssistantMemory()
}

private class IosAiModelStore : AiModelStore {
    override val states = MutableStateFlow<Map<String, TranslatorModelState>>(emptyMap())

    override suspend fun download(id: String) {}

    override fun cancelDownload(id: String) {}

    override suspend fun delete(id: String) {}
}

private class IosTranslatorModelManager : TranslatorModelManager {
    override val state: StateFlow<TranslatorModelState> = MutableStateFlow(TranslatorModelState.NotDownloaded)
    override val approximateSizeMb: Int = 0
    override val approximateRamMb: Int = 0

    override suspend fun download() {}

    override fun cancelDownload() {}

    override suspend fun deleteModel() {}
}

private class IosAssistantService : AssistantService {
    override val ready: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun reply(history: List<AssistantTurn>, onPiece: (String) -> Unit): String = ""
}

private class IosDictationService : DictationService {
    override val available: StateFlow<Boolean> = MutableStateFlow(false)
    override val translatorAvailable: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun transcribe(wav: ByteArray, translator: Boolean): String = ""
}

private class IosAssistantHistoryStore : AssistantHistoryStore {
    private val chats = MutableStateFlow<List<StoredChat>>(emptyList())

    override suspend fun list(): List<StoredChat> = chats.value.sortedByDescending { it.updatedAt }

    override suspend fun save(chat: StoredChat) = chats.update { old -> old.filterNot { it.id == chat.id } + chat }

    override suspend fun delete(id: String) = chats.update { old -> old.filterNot { it.id == id } }
}

private class IosAssistantMemory : AssistantMemory {
    override val notes = MutableStateFlow<List<MemoryNote>>(emptyList())

    override fun save(name: String, text: String) {
        notes.update { old -> old.filterNot { it.name == name } + MemoryNote(name, text, byUser = true) }
    }

    override fun delete(name: String) {
        notes.update { old -> old.filterNot { it.name == name } }
    }

    override fun clearLearned() {
        notes.update { old -> old.filter { it.byUser } }
    }

    override fun promptText(maxChars: Int): String = ""
}
