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
import org.meshtastic.core.common.AppDataRoot
import org.meshtastic.feature.messaging.translation.AiModelStoreImpl
import org.meshtastic.feature.messaging.translation.StorageScheduler
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
import org.meshtastic.feature.messaging.translation.AiStorageLocation
import java.io.File

/** Keyword-only fallback AI assistant and no-op translators for Desktop (no on-device model). */
@Module
class DesktopAiModule {

    @Single fun aiDocAssistant(fallback: KeywordFallbackAssistant): AIDocAssistant = fallback

    @Single fun discoverySummaryAiProvider(fallback: AlgorithmicSummaryProvider): DiscoverySummaryAiProvider = fallback

    @Single fun docTranslationService(): DocTranslationService = NoOpDocTranslator()

    /**
     * The whole data folder (database, settings, logs, maps, models, engine programs) is the one a person chose
     * (default: ~/.meshtastic); a change is made at the next start by [AppDataRoot].
     */
    @Single
    fun aiStorageLocation(): AiStorageLocation {
        return AiStorageLocation(
            settingsDir = AppDataRoot.defaultDir,
            defaultBase = AppDataRoot.dir,
            withEngines = true,
            askOnFirstRun = AppDataRoot.canChange,
            scheduler =
            object : StorageScheduler {
                override val root: File get() = AppDataRoot.dir

                override fun schedule(target: File): String? = AppDataRoot.schedule(target)

                override fun pending(): File? = AppDataRoot.pendingTarget()

                override fun usedMb(): Long = AppDataRoot.usedMb()

                override val canChange: Boolean get() = AppDataRoot.canChange

                override fun lastError(): String? = AppDataRoot.lastMoveError()
            },
            folderPicker = {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                            val chooser =
                                javax.swing.JFileChooser().apply {
                                    dialogTitle = "MT_SW"
                                    fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                                }
                            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) chooser.selectedFile.path else null
                        }
                        .getOrNull()
                }
            },
        )
    }

    @Single
    fun nllbTranslator(location: AiStorageLocation, uiPrefs: UiPrefs): NllbTranslator =
        NllbTranslator(location.translatorDir, uiPrefs)

    @Single
    fun modelServerInstaller(location: AiStorageLocation): ModelServerInstaller =
        ModelServerInstaller(location.enginesDir)

    @Single
    fun aiModelStoreImpl(location: AiStorageLocation, translator: NllbTranslator, installer: ModelServerInstaller): AiModelStoreImpl {
        AiModels.platform = AiPlatform.DESKTOP
        AiModels.unavailableEngines =
            buildSet {
                if (!installer.whisperSupported) add(AiEngine.WHISPER_CPP)
                if (!installer.llamaSupported) add(AiEngine.LLAMA_CPP)
            }
        return AiModelStoreImpl(translator, location.modelsDir, installer, location)
    }

    @Single fun aiModelStore(impl: AiModelStoreImpl): AiModelStore = impl

    @Single
    fun aiTextEngine(store: AiModelStoreImpl, installer: ModelServerInstaller, uiPrefs: UiPrefs): AiTextEngine {
        val logs = File(AppDataRoot.dir, "engine-logs")
        val whisper = if (installer.whisperSupported) WhisperCppEngine(store, installer, logs) else null
        return AiEngineRouter(store, LlamaCppEngine(store, installer, logs), whisper, uiPrefs)
    }

    @Single
    fun aiTranslationRouter(
        translator: NllbTranslator,
        engine: AiTextEngine,
        store: AiModelStoreImpl,
        uiPrefs: UiPrefs,
    ): AiTranslationRouter = AiTranslationRouter(translator, GemmaTranslator(engine), store, uiPrefs)

    @Single
    fun assistantHistoryStore(): AssistantHistoryStore =
        FileAssistantHistoryStore(File(AppDataRoot.dir, "assistant"))

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
    fun assistantMemory(): AssistantMemory = FileAssistantMemory(File(AppDataRoot.dir, "assistant-memory"))

    @Single
    fun assistantService(
        engine: AiTextEngine,
        store: AiModelStoreImpl,
        uiPrefs: UiPrefs,
        data: AssistantDataProvider,
        knowledge: AssistantKnowledge,
        memory: AssistantMemory,
    ): AssistantService = LlamaAssistantService(
            engine, store, uiPrefs, data,
            memoryStore = memory,
            knowledge = UpdatableKnowledge(knowledge, File(AppDataRoot.dir, "knowledge"), uiPrefs),
        )

    @Single
    fun dictationService(engine: AiTextEngine, store: AiModelStoreImpl, uiPrefs: UiPrefs): DictationService =
        ModelDictationService(engine, store, uiPrefs)

    @Single fun messageTranslationService(router: AiTranslationRouter): MessageTranslationService = router

    @Single fun translatorModelManager(router: AiTranslationRouter): TranslatorModelManager = router
}
