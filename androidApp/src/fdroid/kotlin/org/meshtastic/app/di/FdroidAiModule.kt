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
package org.meshtastic.app.di

import android.content.Context
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.meshtastic.feature.discovery.ai.AlgorithmicSummaryProvider
import org.meshtastic.feature.discovery.ai.DiscoverySummaryAiProvider
import org.meshtastic.feature.docs.ai.AIDocAssistant
import org.meshtastic.feature.docs.ai.KeywordFallbackAssistant
import org.meshtastic.feature.docs.translation.DocTranslationService
import org.meshtastic.feature.docs.translation.NoOpDocTranslator
import org.meshtastic.feature.messaging.translation.MessageTranslationService
import org.meshtastic.core.repository.TranslatorModelManager
import org.meshtastic.feature.messaging.translation.AiModelStoreImpl
import org.meshtastic.feature.messaging.translation.AiTranslationRouter
import org.meshtastic.feature.messaging.translation.AssistantHistoryStore
import org.meshtastic.feature.messaging.translation.AssistantService
import org.meshtastic.feature.messaging.translation.FileAssistantHistoryStore
import org.meshtastic.feature.messaging.translation.GemmaTranslator
import org.meshtastic.feature.messaging.translation.LlamaAssistantService
import org.meshtastic.feature.messaging.translation.AiEngineRouter
import org.meshtastic.feature.messaging.translation.AiTextEngine
import org.meshtastic.feature.messaging.translation.LlamaCppEngine
import org.meshtastic.feature.messaging.translation.ModelServerInstaller
import org.meshtastic.feature.messaging.translation.WhisperCppEngine
import org.meshtastic.core.repository.AiEngine
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.AiPlatform
import org.meshtastic.feature.messaging.translation.ModelDictationService
import org.meshtastic.feature.messaging.translation.DictationService
import org.meshtastic.feature.messaging.translation.UpdatableKnowledge
import org.meshtastic.core.repository.AiModelStore
import org.meshtastic.feature.messaging.translation.AssistantDataProvider
import org.meshtastic.core.repository.AssistantKnowledge
import org.meshtastic.core.repository.AssistantMemory
import org.meshtastic.feature.messaging.translation.FileAssistantMemory
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.feature.messaging.translation.NllbTranslator
import java.io.File

/** Provides keyword-only fallback AI assistant for the F-Droid flavor (no on-device model). */
@Module
@Configuration
class FdroidAiModule {
    @Single fun aiDocAssistant(fallback: KeywordFallbackAssistant): AIDocAssistant = fallback

    @Single fun discoverySummaryAiProvider(fallback: AlgorithmicSummaryProvider): DiscoverySummaryAiProvider = fallback

    @Single fun docTranslationService(): DocTranslationService = NoOpDocTranslator()

    @Single
    fun nllbTranslator(context: Context, uiPrefs: UiPrefs): NllbTranslator =
        NllbTranslator(File(context.filesDir, "translator"), uiPrefs)


    @Single fun modelServerInstaller(context: Context): ModelServerInstaller = ModelServerInstaller(File(context.filesDir, "engines"))

    @Single
    fun aiModelStoreImpl(context: Context, translator: NllbTranslator, installer: ModelServerInstaller): AiModelStoreImpl {
        // The language model and speech programs exist only for 64-bit phones.
        AiModels.platform = AiPlatform.ANDROID
        AiModels.unavailableEngines =
            buildSet {
                if (!installer.whisperSupported) add(AiEngine.WHISPER_CPP)
                if (!installer.llamaSupported) add(AiEngine.LLAMA_CPP)
            }
        return AiModelStoreImpl(translator, File(context.filesDir, "models"), installer)
    }

    @Single fun aiModelStore(impl: AiModelStoreImpl): AiModelStore = impl

    @Single
    fun aiTextEngine(context: Context, store: AiModelStoreImpl, installer: ModelServerInstaller): AiTextEngine =
        File(context.filesDir, "engine-logs").let { logs ->
            AiEngineRouter(
                store,
                LlamaCppEngine(store, installer, logs),
                if (installer.whisperSupported) WhisperCppEngine(store, installer, logs) else null,
            )
        }

    @Single
    fun aiTranslationRouter(
        translator: NllbTranslator,
        engine: AiTextEngine,
        store: AiModelStoreImpl,
        uiPrefs: UiPrefs,
    ): AiTranslationRouter = AiTranslationRouter(translator, GemmaTranslator(engine), store, uiPrefs)

    @Single
    fun assistantHistoryStore(context: Context): AssistantHistoryStore =
        FileAssistantHistoryStore(File(context.filesDir, "assistant"))

    @Single
    fun assistantDataProvider(
        nodeRepository: NodeRepository,
        radioConfigRepository: RadioConfigRepository,
        meshLogRepository: MeshLogRepository,
        packetRepository: PacketRepository,
        connection: org.meshtastic.core.repository.ConnectionStateProvider,
    ): AssistantDataProvider =
        AssistantDataProvider(nodeRepository, radioConfigRepository, meshLogRepository, packetRepository, connection)

    @Single
    fun assistantMemory(context: Context): AssistantMemory = FileAssistantMemory(File(context.filesDir, "assistant-memory"))

    @Single
    fun assistantService(
        context: Context,
        engine: AiTextEngine,
        store: AiModelStoreImpl,
        uiPrefs: UiPrefs,
        data: AssistantDataProvider,
        knowledge: AssistantKnowledge,
        memory: AssistantMemory,
    ): AssistantService = LlamaAssistantService(
            engine, store, uiPrefs, data,
            memoryStore = memory,
            knowledge = UpdatableKnowledge(knowledge, File(context.filesDir, "knowledge"), uiPrefs),
        )

    @Single
    fun dictationService(engine: AiTextEngine, store: AiModelStoreImpl, uiPrefs: UiPrefs): DictationService =
        ModelDictationService(engine, store, uiPrefs)

    @Single fun messageTranslationService(router: AiTranslationRouter): MessageTranslationService = router

    @Single fun translatorModelManager(router: AiTranslationRouter): TranslatorModelManager = router
}
