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
package org.meshtastic.core.repository

import kotlinx.coroutines.flow.StateFlow

/** The inference engine a model runs on. */
enum class AiEngine {
    /** NLLB-200 through ONNX Runtime (translation only). */
    NLLB_ONNX,

    /** GGUF language models through llama.cpp (translation and assistant). */
    LLAMA_GGUF,
}

/** What a model can be used for. */
enum class AiRole {
    TRANSLATOR,
    ASSISTANT,
}

/** The licence a model is distributed under; the settings screen shows the matching notice. */
enum class AiLicense {
    /** NLLB-200 (Meta): Creative Commons BY-NC 4.0, non-commercial use only. */
    CC_BY_NC_4_0,

    /** Gemma 3 (Google): the Gemma Terms of Use and Prohibited Use Policy. */
    GEMMA_TERMS,
}

/** One file of a model that is downloaded as a whole. */
data class AiModelFile(val url: String, val fileName: String)

/**
 * A model the user can download. [ramMb] is the measured peak memory while running (with some margin) and
 * [minDeviceRamGb] the smallest total device RAM the settings screen recommends. [engineReady] is false while the
 * engine for this model is not part of the app yet: such a model is listed but cannot be downloaded.
 */
data class AiModelInfo(
    val id: String,
    val name: String,
    val engine: AiEngine,
    val roles: Set<AiRole>,
    val sizeMb: Int,
    val ramMb: Int,
    val minDeviceRamGb: Int,
    val license: AiLicense,
    val files: List<AiModelFile>,
    val engineReady: Boolean,
) {
    fun canDo(role: AiRole): Boolean = role in roles
}

/** The fixed list of models offered by the app (light, standard, strong). */
object AiModels {
    const val NLLB_600M_ID = "nllb-600m"
    const val GEMMA_4B_ID = "gemma3-4b"
    const val GEMMA_12B_ID = "gemma3-12b"

    /** Flipped to true when the assistant screen exists; until then the assistant switch stays off. */
    const val ASSISTANT_SCREEN_READY = true

    private const val GEMMA_4B_REPO = "https://huggingface.co/unsloth/gemma-3-4b-it-GGUF/resolve/main/"
    private const val GEMMA_12B_REPO = "https://huggingface.co/unsloth/gemma-3-12b-it-GGUF/resolve/main/"

    /** Light: translation only, runs on phones from 4 GB of RAM. Its files are handled by the NLLB engine itself. */
    val Nllb600 =
        AiModelInfo(
            id = NLLB_600M_ID,
            name = "NLLB-200 600M",
            engine = AiEngine.NLLB_ONNX,
            roles = setOf(AiRole.TRANSLATOR),
            sizeMb = 870,
            ramMb = 2300,
            minDeviceRamGb = 4,
            license = AiLicense.CC_BY_NC_4_0,
            files = emptyList(),
            engineReady = true,
        )

    /** Standard: translation and assistant in one file, for phones from 8 GB of RAM. */
    val Gemma4b =
        AiModelInfo(
            id = GEMMA_4B_ID,
            name = "Gemma 3 4B",
            engine = AiEngine.LLAMA_GGUF,
            roles = setOf(AiRole.TRANSLATOR, AiRole.ASSISTANT),
            sizeMb = 2490,
            ramMb = 5000,
            minDeviceRamGb = 8,
            license = AiLicense.GEMMA_TERMS,
            files = listOf(AiModelFile(GEMMA_4B_REPO + "gemma-3-4b-it-Q4_K_M.gguf", "gemma-3-4b-it-Q4_K_M.gguf")),
            engineReady = true,
        )

    /** Strong: the best quality, for computers and phones with 16 GB of RAM. */
    val Gemma12b =
        AiModelInfo(
            id = GEMMA_12B_ID,
            name = "Gemma 3 12B",
            engine = AiEngine.LLAMA_GGUF,
            roles = setOf(AiRole.TRANSLATOR, AiRole.ASSISTANT),
            sizeMb = 7300,
            ramMb = 10000,
            minDeviceRamGb = 16,
            license = AiLicense.GEMMA_TERMS,
            files = listOf(AiModelFile(GEMMA_12B_REPO + "gemma-3-12b-it-Q4_K_M.gguf", "gemma-3-12b-it-Q4_K_M.gguf")),
            engineReady = true,
        )

    val all: List<AiModelInfo> = listOf(Nllb600, Gemma4b, Gemma12b)

    fun byId(id: String?): AiModelInfo? = all.firstOrNull { it.id == id }
}

/**
 * Tracks the downloaded state of every model in [AiModels.all]. Several models can be on the device at once; each one
 * is downloaded, cancelled and deleted on its own. Which downloaded model is used is a preference (see [UiPrefs]).
 */
interface AiModelStore {
    val states: StateFlow<Map<String, TranslatorModelState>>

    suspend fun download(id: String)

    fun cancelDownload(id: String)

    suspend fun delete(id: String)
}
