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
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.meshtastic.app.ai.GeminiNanoDocAssistant
import org.meshtastic.app.discovery.GeminiNanoSummaryProvider
import org.meshtastic.app.translation.MlKitDocTranslator
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.TranslatorModelManager
import org.meshtastic.feature.messaging.translation.AiModelStoreImpl
import org.meshtastic.feature.messaging.translation.AiTranslationRouter
import org.meshtastic.feature.messaging.translation.GemmaTranslator
import org.meshtastic.feature.messaging.translation.LlamaEngine
import org.meshtastic.core.repository.AiModelStore
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.feature.discovery.DiscoverySummaryGenerator
import org.meshtastic.feature.discovery.ai.DiscoverySummaryAiProvider
import org.meshtastic.feature.docs.ai.AIDocAssistant
import org.meshtastic.feature.docs.data.DocBundleLoader
import org.meshtastic.feature.docs.data.KeywordSearchEngine
import org.meshtastic.feature.docs.translation.DocTranslationCache
import org.meshtastic.feature.docs.translation.DocTranslationService
import org.meshtastic.feature.messaging.translation.MessageTranslationService
import org.meshtastic.feature.messaging.translation.NllbTranslator
import java.io.File

// TODO: Enable Firebase App Check (with Play Integrity provider) if hybrid/cloud
//  fallback is ever adopted. App Check only gates cloud proxy requests — on-device
//  inference (ONLY_ON_DEVICE mode) bypasses it entirely, so no action needed today.

/** Provides the on-device Gemini Nano AI assistant for the Google flavor. */
@Module
@Configuration
class GoogleAiModule {
    @Single
    fun aiDocAssistant(
        searchEngine: KeywordSearchEngine,
        bundleLoader: DocBundleLoader,
        nodeRepository: NodeRepository,
    ): AIDocAssistant = GeminiNanoDocAssistant(searchEngine, bundleLoader, nodeRepository)

    @Single
    fun discoverySummaryAiProvider(generator: DiscoverySummaryGenerator): DiscoverySummaryAiProvider =
        GeminiNanoSummaryProvider(generator)

    @Single
    fun docTranslationCache(context: Context): DocTranslationCache =
        DocTranslationCache(cacheDir = context.cacheDir.toOkioPath(), fileSystem = FileSystem.SYSTEM)

    @Single fun docTranslationService(cache: DocTranslationCache): DocTranslationService = MlKitDocTranslator(cache)

    @Single
    fun nllbTranslator(context: Context, uiPrefs: UiPrefs): NllbTranslator =
        NllbTranslator(File(context.filesDir, "translator"), uiPrefs)


    @Single
    fun aiModelStoreImpl(context: Context, translator: NllbTranslator): AiModelStoreImpl =
        AiModelStoreImpl(translator, File(context.filesDir, "models"))

    @Single fun aiModelStore(impl: AiModelStoreImpl): AiModelStore = impl

    @Single fun llamaEngine(store: AiModelStoreImpl): LlamaEngine = LlamaEngine(store)

    @Single
    fun aiTranslationRouter(
        translator: NllbTranslator,
        engine: LlamaEngine,
        store: AiModelStoreImpl,
        uiPrefs: UiPrefs,
    ): AiTranslationRouter = AiTranslationRouter(translator, GemmaTranslator(engine), store, uiPrefs)

    @Single fun messageTranslationService(router: AiTranslationRouter): MessageTranslationService = router

    @Single fun translatorModelManager(router: AiTranslationRouter): TranslatorModelManager = router
}
