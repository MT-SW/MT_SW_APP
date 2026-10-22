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
import org.meshtastic.feature.messaging.translation.LiteRtEngine
import org.meshtastic.core.repository.AiModelStore
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


    @Single
    fun aiModelStoreImpl(context: Context, translator: NllbTranslator): AiModelStoreImpl =
        AiModelStoreImpl(translator, File(context.filesDir, "models"))

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
    fun assistantHistoryStore(context: Context): AssistantHistoryStore =
        FileAssistantHistoryStore(File(context.filesDir, "assistant"))

    @Single
    fun assistantService(engine: LiteRtEngine, store: AiModelStoreImpl, uiPrefs: UiPrefs): AssistantService =
        LlamaAssistantService(engine, store, uiPrefs)

    @Single fun messageTranslationService(router: AiTranslationRouter): MessageTranslationService = router

    @Single fun translatorModelManager(router: AiTranslationRouter): TranslatorModelManager = router
}
