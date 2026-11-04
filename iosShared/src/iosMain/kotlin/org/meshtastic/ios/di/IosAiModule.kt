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
import org.meshtastic.core.database.iosDataDir
import org.meshtastic.core.repository.AiEngine
import org.meshtastic.core.repository.AiModelStore
import org.meshtastic.core.repository.AssistantKnowledge
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.feature.messaging.translation.AiTextEngine
import org.meshtastic.feature.messaging.translation.GemmaTranslator
import org.meshtastic.feature.messaging.translation.IosAiModelStore
import org.meshtastic.feature.messaging.translation.IosAssistantService
import org.meshtastic.feature.messaging.translation.IosLlamaEngine
import org.meshtastic.feature.messaging.translation.IosTranslationRouter
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.AiPlatform
import org.meshtastic.core.repository.AssistantMemory
import org.meshtastic.core.repository.MemoryNote
import org.meshtastic.core.repository.TranslatorModelManager
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
import org.meshtastic.feature.messaging.translation.StoredChat

/**
 * AI on iPhone: the assistant and the translator run on-device in llama.cpp; dictation (speech recognition) is not
 * ported yet; the guide search and the discovery summary work without a model.
 */
@Module
class IosAiModule {
    @Single fun aiDocAssistant(fallback: KeywordFallbackAssistant): AIDocAssistant = fallback

    @Single fun discoverySummaryAiProvider(fallback: AlgorithmicSummaryProvider): DiscoverySummaryAiProvider = fallback

    @Single fun docTranslationService(): DocTranslationService = NoOpDocTranslator()

    @Single
    fun iosAiModelStore(): IosAiModelStore {
        AiModels.platform = AiPlatform.IOS
        // The speech engine and the ONNX translator are not on the iPhone; the language models run in llama.cpp.
        AiModels.unavailableEngines = setOf(AiEngine.NLLB_ONNX, AiEngine.WHISPER_CPP)
        return IosAiModelStore(iosDataDir())
    }

    @Single fun aiModelStore(impl: IosAiModelStore): AiModelStore = impl

    @Single fun aiTextEngine(store: IosAiModelStore): AiTextEngine = IosLlamaEngine { store.modelPath(it) }

    @Single
    fun iosTranslationRouter(engine: AiTextEngine, store: IosAiModelStore, uiPrefs: UiPrefs): IosTranslationRouter =
        IosTranslationRouter(GemmaTranslator(engine), store, uiPrefs)

    @Single fun messageTranslationService(router: IosTranslationRouter): MessageTranslationService = router

    @Single fun translatorModelManager(router: IosTranslationRouter): TranslatorModelManager = router

    @Single
    fun assistantService(
        engine: AiTextEngine,
        store: IosAiModelStore,
        uiPrefs: UiPrefs,
        knowledge: AssistantKnowledge,
    ): AssistantService = IosAssistantService(engine, store, uiPrefs, knowledge)

    @Single fun dictationService(): DictationService = IosDictationService()

    @Single fun assistantHistoryStore(): AssistantHistoryStore = IosAssistantHistoryStore()

    @Single fun assistantMemory(): AssistantMemory = IosAssistantMemory()
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
