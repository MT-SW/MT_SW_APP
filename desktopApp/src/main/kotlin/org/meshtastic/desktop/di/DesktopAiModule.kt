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
package org.meshtastic.desktop.di

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
import org.meshtastic.feature.messaging.translation.LiteRtEngine
import org.meshtastic.feature.messaging.translation.UpdatableKnowledge
import org.meshtastic.core.repository.AiModelStore
import org.meshtastic.feature.messaging.translation.AssistantDataProvider
import org.meshtastic.core.repository.AssistantKnowledge
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.feature.messaging.translation.NllbTranslator
import java.io.File

/** Keyword-only fallback AI assistant and no-op translators for Desktop (no on-device model). */
@Module
class DesktopAiModule {

    @Single fun aiDocAssistant(fallback: KeywordFallbackAssistant): AIDocAssistant = fallback

    @Single fun discoverySummaryAiProvider(fallback: AlgorithmicSummaryProvider): DiscoverySummaryAiProvider = fallback

    @Single fun docTranslationService(): DocTranslationService = NoOpDocTranslator()

    @Single
    fun nllbTranslator(uiPrefs: UiPrefs): NllbTranslator =
        NllbTranslator(File(System.getProperty("user.home"), ".meshtastic/translator"), uiPrefs)

    @Single
    fun aiModelStoreImpl(translator: NllbTranslator): AiModelStoreImpl =
        AiModelStoreImpl(translator, File(System.getProperty("user.home"), ".meshtastic/models"))

    @Single fun aiModelStore(impl: AiModelStoreImpl): AiModelStore = impl

    @Single fun liteRtEngine(store: AiModelStoreImpl): LiteRtEngine = LiteRtEngine(store)

    @Single
    fun aiTranslationRouter(
        translator: NllbTranslator,
        engine: LiteRtEngine,
        store: AiModelStoreImpl,
        uiPrefs: UiPrefs,
    ): AiTranslationRouter = AiTranslationRouter(translator, GemmaTranslator(engine), store, uiPrefs)

    @Single
    fun assistantHistoryStore(): AssistantHistoryStore =
        FileAssistantHistoryStore(File(System.getProperty("user.home"), ".meshtastic/assistant"))

    @Single
    fun assistantDataProvider(
        nodeRepository: NodeRepository,
        radioConfigRepository: RadioConfigRepository,
        meshLogRepository: MeshLogRepository,
        packetRepository: PacketRepository,
    ): AssistantDataProvider =
        AssistantDataProvider(nodeRepository, radioConfigRepository, meshLogRepository, packetRepository)

    @Single
    fun assistantService(
        engine: LiteRtEngine,
        store: AiModelStoreImpl,
        uiPrefs: UiPrefs,
        data: AssistantDataProvider,
        knowledge: AssistantKnowledge,
    ): AssistantService = LlamaAssistantService(
            engine, store, uiPrefs, data,
            UpdatableKnowledge(knowledge, File(System.getProperty("user.home"), ".meshtastic/knowledge"), uiPrefs),
        )

    @Single fun messageTranslationService(router: AiTranslationRouter): MessageTranslationService = router

    @Single fun translatorModelManager(router: AiTranslationRouter): TranslatorModelManager = router
}
