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
@file:Suppress("TooManyFunctions")

package org.meshtastic.ios.di

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.meshtastic.core.database.iosDataDir
import org.meshtastic.core.repository.AiModelStore
import org.meshtastic.core.repository.AiModels
import org.meshtastic.core.repository.AiPlatform
import org.meshtastic.core.repository.AssistantKnowledge
import org.meshtastic.core.repository.AssistantMemory
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.TranslatorModelManager
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.feature.discovery.ai.AlgorithmicSummaryProvider
import org.meshtastic.feature.discovery.ai.DiscoverySummaryAiProvider
import org.meshtastic.feature.docs.ai.AIDocAssistant
import org.meshtastic.feature.docs.ai.KeywordFallbackAssistant
import org.meshtastic.feature.docs.translation.DocTranslationService
import org.meshtastic.feature.docs.translation.NoOpDocTranslator
import org.meshtastic.feature.messaging.translation.AiTextEngine
import org.meshtastic.feature.messaging.translation.AssistantHistoryStore
import org.meshtastic.feature.messaging.translation.AssistantService
import org.meshtastic.feature.messaging.translation.DictationService
import org.meshtastic.feature.messaging.translation.GemmaTranslator
import org.meshtastic.feature.messaging.translation.IosAiModelStore
import org.meshtastic.feature.messaging.translation.IosAssistantHistoryStore
import org.meshtastic.feature.messaging.translation.IosAssistantMemory
import org.meshtastic.feature.messaging.translation.IosAssistantTestReports
import org.meshtastic.feature.messaging.translation.AssistantDataProvider
import org.meshtastic.feature.messaging.translation.LlamaAssistantService
import org.meshtastic.feature.messaging.translation.IosDictationService
import org.meshtastic.feature.messaging.translation.IosEngineLog
import org.meshtastic.feature.messaging.translation.IosLlamaEngine
import org.meshtastic.feature.messaging.translation.IosTranslationRouter
import org.meshtastic.feature.messaging.translation.MessageTranslationService

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
        return IosAiModelStore(iosDataDir())
    }

    @Single fun aiModelStore(impl: IosAiModelStore): AiModelStore = impl

    @Single
    fun aiTextEngine(store: IosAiModelStore): AiTextEngine {
        IosEngineLog.dir = iosDataDir() + "/engine-logs"
        return IosLlamaEngine({ store.modelPath(it) }, { store.hearingPath(it) })
    }

    @Single
    fun iosTranslationRouter(engine: AiTextEngine, store: IosAiModelStore, uiPrefs: UiPrefs): IosTranslationRouter =
        IosTranslationRouter(GemmaTranslator(engine), store, uiPrefs)

    @Single fun messageTranslationService(router: IosTranslationRouter): MessageTranslationService = router

    @Single fun translatorModelManager(router: IosTranslationRouter): TranslatorModelManager = router

    @Single
    fun assistantDataProvider(
        nodeRepository: NodeRepository,
        radioConfigRepository: RadioConfigRepository,
        meshLogRepository: MeshLogRepository,
        packetRepository: PacketRepository,
        connection: ConnectionStateProvider,
        uiPrefs: UiPrefs,
    ): AssistantDataProvider = AssistantDataProvider(
        nodeRepository,
        radioConfigRepository,
        meshLogRepository,
        packetRepository,
        connection,
        null,
    ) {
        uiPrefs.currentInverted.value
    }

    @Single
    fun assistantService(
        engine: AiTextEngine,
        store: IosAiModelStore,
        uiPrefs: UiPrefs,
        data: AssistantDataProvider,
        knowledge: AssistantKnowledge,
        memory: AssistantMemory,
    ): AssistantService = LlamaAssistantService(
        engine,
        store,
        uiPrefs,
        data,
        memoryStore = memory,
        knowledge = knowledge,
        testReports = IosAssistantTestReports(),
    )

    @Single
    fun dictationService(engine: AiTextEngine, store: IosAiModelStore, uiPrefs: UiPrefs): DictationService =
        IosDictationService(engine, store, uiPrefs)

    @Single fun assistantHistoryStore(): AssistantHistoryStore = IosAssistantHistoryStore(iosDataDir())

    @Single fun assistantMemory(): AssistantMemory = IosAssistantMemory(iosDataDir())
}
