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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The inference engine a model runs on. */
enum class AiEngine {
    /** NLLB-200 through ONNX Runtime (translation only). */
    NLLB_ONNX,

    /** GGUF language models through llama.cpp (a separate program the app starts; graphics card and processor share the work). */
    LLAMA_CPP,

    /** Whisper speech recognition models through whisper.cpp (dictation only). */
    WHISPER_CPP,
}

/** What a model can be used for. Each has its own list of models in the settings. */
enum class AiRole {
    TRANSLATOR,
    ASSISTANT,
    DICTATION,
}

/** The kind of app a model can run in. */
enum class AiPlatform {
    ANDROID,
    DESKTOP,
}

/** The licence a model is distributed under; the model list shows its name and a link to the model's page. */
enum class AiLicense(val label: String) {
    /** NLLB-200 (Meta): Creative Commons BY-NC 4.0, non-commercial use only. */
    CC_BY_NC_4_0("CC BY-NC 4.0"),

    /** Apache License 2.0 (Gemma 4, Qwen 3.5, Bielik). */
    APACHE_2_0("Apache 2.0"),

    /** MIT licence (Whisper). */
    MIT("MIT"),

    /** Gemma Terms of Use (TranslateGemma). */
    GEMMA_TERMS("Gemma Terms of Use"),

    /** Not checked by the app: the licence is on the model's page. */
    SEE_MODEL_PAGE("see the model page"),
}

/** What a file of a model is for. */
enum class AiFileKind {
    /** The model itself. */
    MODEL,

    /** The part that lets a Gemma model hear (dictation); downloaded together with the model. */
    HEARING,
}

/** One file of a model that is downloaded as a whole. [sizeMb] is the real size (used to see that a download is complete). */
data class AiModelFile(
    val url: String,
    val fileName: String,
    val sizeMb: Int = 0,
    val kind: AiFileKind = AiFileKind.MODEL,
)

/**
 * A model the user can download. [ramMb] is the memory the model needs while running (with some margin) and
 * [minDeviceRamGb] the total memory of the device where it runs comfortably (a smaller one still works: the engine
 * moves part of the model between the graphics card and the main memory). [platforms] says where the model's engine
 * exists. [family] groups models in the list (Gemma, Qwen, Bielik, ...); [recommendedFor] marks the roles for which the
 * app suggests this model (a hint only: nothing is chosen or downloaded by itself).
 */
data class AiModelInfo(
    val id: String,
    val name: String,
    val family: String,
    val engine: AiEngine,
    val roles: Set<AiRole>,
    val sizeMb: Int,
    val ramMb: Int,
    val minDeviceRamGb: Int,
    val license: AiLicense,
    val files: List<AiModelFile>,
    /**
     * Other files for phones, when the version used on computers does not suit a phone's graphics card or NPU (the K-quant
     * files write nonsense or crash on Adreno phones; Q4_0 is handled by those engines). Null: the same files everywhere.
     */
    val phoneFiles: List<AiModelFile>? = null,
    val platforms: Set<AiPlatform>,
    val hearing: Boolean = false,
    /**
     * True when the model may think quietly before it answers. Off for models that think far too long for their size
     * (Qwen 3.5): there thinking is switched off in the engine itself, so even simple questions get quick answers.
     */
    val quietThinking: Boolean = true,
    /**
     * False for models that the Intel NPU (OpenVINO) cannot run: they skip it and use the graphics card. Taken from the
     * engine's own list of tested models (Qwen 3.5 and the 12B and bigger models fail there).
     */
    val intelNpu: Boolean = true,
    val recommendedFor: Set<AiRole> = emptySet(),
    val descriptionPl: String = "",
    val descriptionEn: String = "",
    val pageUrl: String = "",
) {
    /** True when the engine of this model exists on the platform the app runs on. */
    val engineReady: Boolean get() = AiModels.platform in platforms && engine !in AiModels.unavailableEngines

    /** The files this system downloads and uses: the phone's own list on a phone when there is one. */
    val activeFiles: List<AiModelFile> get() = if (AiModels.platform == AiPlatform.ANDROID) phoneFiles ?: files else files

    fun canDo(role: AiRole): Boolean = role in roles
}

/** The list of models offered by the app (light to strong, grouped in families). */
object AiModels {
    const val NLLB_600M_ID = "nllb-600m"

    // The ids differ from those of the old engine (gemma4-e2b, ...) so old and new can never be mixed up.
    const val G4_E2B = "g4-e2b-q4"
    const val G4_E4B = "g4-e4b-q4"
    const val G4_12B = "g4-12b-q4"
    const val G4_26B = "g4-26b-a4b-q4"
    const val QWEN_08B = "qwen35-0_8b-q4"
    const val QWEN_2B = "qwen35-2b-q4"
    const val QWEN_4B = "qwen35-4b-q4"
    const val QWEN_9B = "qwen35-9b-q4"
    const val QWEN_35B = "qwen35-35b-a3b-q4"
    const val BIELIK_15B = "bielik-1_5b-q8"
    const val BIELIK_45B = "bielik-4_5b-q8"
    const val BIELIK_7B = "bielik-minitron-7b-q4"
    const val BIELIK_11B = "bielik-11b-v3-q4"
    const val PLLUM_12B = "pllum-12b-q4"
    const val TG_4B = "translategemma-4b-q4"
    const val TG_12B = "translategemma-12b-q4"
    const val WHISPER_BASE = "whisper-base-q5"
    const val WHISPER_SMALL = "whisper-small-q5"
    const val WHISPER_MEDIUM = "whisper-medium-q5"
    const val WHISPER_TURBO = "whisper-large-v3-turbo-q5"
    const val WHISPER_SMALL_Q8 = "whisper-small-q8"
    const val WHISPER_MEDIUM_Q8 = "whisper-medium-q8"
    const val WHISPER_TURBO_Q8 = "whisper-large-v3-turbo-q8"

    /** True when the assistant screen exists. */
    const val ASSISTANT_SCREEN_READY = true

    /** Which kind of app is running; set once at start by the platform's own module. Decides which models are offered. */
    @Volatile var platform: AiPlatform = AiPlatform.ANDROID

    /** Engines that cannot run on this particular system (for example the speech program on a Mac); set once at start. */
    @Volatile var unavailableEngines: Set<AiEngine> = emptySet()

    private const val HF = "https://huggingface.co/"

    private val BOTH = setOf(AiPlatform.ANDROID, AiPlatform.DESKTOP)
    private val DESKTOP = setOf(AiPlatform.DESKTOP)
    private val TEXT = setOf(AiRole.TRANSLATOR, AiRole.ASSISTANT)

    private fun hf(repo: String, name: String, mb: Int, kind: AiFileKind = AiFileKind.MODEL) =
        AiModelFile("$HF$repo/resolve/main/$name", name, mb, kind)

    /** Light: translation only, runs on phones from 4 GB of RAM. Its files are handled by the NLLB engine itself. */
    val Nllb600 =
        AiModelInfo(
            id = NLLB_600M_ID,
            name = "NLLB-200 600M",
            family = "NLLB",
            engine = AiEngine.NLLB_ONNX,
            roles = setOf(AiRole.TRANSLATOR),
            sizeMb = 870,
            ramMb = 2300,
            minDeviceRamGb = 4,
            license = AiLicense.CC_BY_NC_4_0,
            files = emptyList(),
            platforms = BOTH,
            descriptionPl = "Lekki tłumacz (200 języków), działa na procesorze, także na starszych telefonach. Tylko tłumaczenie.",
            descriptionEn = "Light translator (200 languages), runs on the processor, also on older phones. Translation only.",
            pageUrl = "${HF}facebook/nllb-200-distilled-600M",
        )

    // ---- GGUF models run by llama.cpp (computer and phone) ----

    private const val G4_E_MMPROJ_REPO = "ggml-org/gemma-4-E2B-it-GGUF"

    val G4E2b =
        AiModelInfo(
            id = G4_E2B,
            name = "Gemma 4 E2B",
            family = "Gemma 4",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT + AiRole.DICTATION,
            sizeMb = 3350 + 557,
            ramMb = 4500,
            minDeviceRamGb = 6,
            license = AiLicense.APACHE_2_0,
            files =
            listOf(
                hf("google/gemma-4-E2B-it-qat-q4_0-gguf", "gemma-4-E2B_q4_0-it.gguf", 3350),
                hf(G4_E_MMPROJ_REPO, "mmproj-gemma-4-E2B-it-Q8_0.gguf", 557, AiFileKind.HEARING),
            ),
            platforms = BOTH,
            hearing = true,
            descriptionPl = "Mały model Google (wersja QAT od Google): tłumaczy, odpowiada i rozumie mowę. Szybki, nawet na samym procesorze.",
            descriptionEn = "Small Google model (Google's own QAT version): translates, answers and understands speech. Fast, even on the processor alone.",
            pageUrl = "${HF}google/gemma-4-E2B-it-qat-q4_0-gguf",
        )

    val G4E4b =
        AiModelInfo(
            id = G4_E4B,
            name = "Gemma 4 E4B",
            family = "Gemma 4",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT + AiRole.DICTATION,
            sizeMb = 5155 + 560,
            ramMb = 6500,
            minDeviceRamGb = 8,
            license = AiLicense.APACHE_2_0,
            files =
            listOf(
                hf("google/gemma-4-E4B-it-qat-q4_0-gguf", "gemma-4-E4B_q4_0-it.gguf", 5155),
                hf("ggml-org/gemma-4-E4B-it-GGUF", "mmproj-gemma-4-E4B-it-Q8_0.gguf", 560, AiFileKind.HEARING),
            ),
            platforms = BOTH,
            hearing = true,
            recommendedFor = setOf(AiRole.ASSISTANT, AiRole.TRANSLATOR, AiRole.DICTATION),
            descriptionPl = "Dobry początek: lepsze odpowiedzi niż E2B, rozumie mowę. Na komputery od 8 GB RAM.",
            descriptionEn = "A good start: better answers than E2B, understands speech. For computers from 8 GB of RAM.",
            pageUrl = "${HF}google/gemma-4-E4B-it-qat-q4_0-gguf",
        )

    val G4_12b =
        AiModelInfo(
            id = G4_12B,
            intelNpu = false,
            name = "Gemma 4 12B",
            family = "Gemma 4",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 6976,
            ramMb = 9000,
            minDeviceRamGb = 12,
            license = AiLicense.APACHE_2_0,
            files = listOf(hf("google/gemma-4-12B-it-qat-q4_0-gguf", "gemma-4-12b-it-qat-q4_0.gguf", 6976)),
            platforms = BOTH,
            descriptionPl = "Mocniejszy model Google. Zmieści się częściowo na karcie graficznej, resztę weźmie procesor.",
            descriptionEn = "A stronger Google model. Part of it fits on the graphics card, the processor takes the rest.",
            pageUrl = "${HF}google/gemma-4-12B-it-qat-q4_0-gguf",
        )

    val G4_26b =
        AiModelInfo(
            id = G4_26B,
            intelNpu = false,
            name = "Gemma 4 26B A4B",
            family = "Gemma 4",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 14439,
            ramMb = 17000,
            minDeviceRamGb = 24,
            license = AiLicense.APACHE_2_0,
            files = listOf(hf("google/gemma-4-26B-A4B-it-qat-q4_0-gguf", "gemma-4-26B_q4_0-it.gguf", 14439)),
            platforms = BOTH,
            descriptionPl = "Bardzo duży model, ale w trakcie pracy używa tylko części (4 mld parametrów), więc jest szybszy niż wielkość sugeruje. Wymaga dużo RAM.",
            descriptionEn = "A very large model that uses only part of itself while working (4 billion parameters), so it is faster than its size suggests. Needs a lot of RAM.",
            pageUrl = "${HF}google/gemma-4-26B-A4B-it-qat-q4_0-gguf",
        )

    val Qwen08b =
        AiModelInfo(
            id = QWEN_08B,
            intelNpu = false,
            quietThinking = false,
            name = "Qwen 3.5 0.8B",
            family = "Qwen 3.5",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 507,
            ramMb = 1200,
            minDeviceRamGb = 3,
            license = AiLicense.APACHE_2_0,
            files = listOf(hf("unsloth/Qwen3.5-0.8B-GGUF", "Qwen3.5-0.8B-Q4_0.gguf", 507)),
            platforms = BOTH,
            descriptionPl = "Najmniejszy i najszybszy model: do bardzo prostych pytań i najsłabszych telefonów. Odpowiada krótko, polski prosty.",
            descriptionEn = "The smallest and fastest model: for very simple questions and the weakest phones. Answers briefly, simple Polish.",
            pageUrl = "${HF}unsloth/Qwen3.5-0.8B-GGUF",
        )

    val Qwen2b =
        AiModelInfo(
            id = QWEN_2B,
            intelNpu = false,
            quietThinking = false,
            name = "Qwen 3.5 2B",
            family = "Qwen 3.5",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 1215,
            ramMb = 2200,
            minDeviceRamGb = 4,
            license = AiLicense.APACHE_2_0,
            files = listOf(hf("unsloth/Qwen3.5-2B-GGUF", "Qwen3.5-2B-Q4_0.gguf", 1215)),
            platforms = BOTH,
            descriptionPl = "Bardzo mały model do prostych zadań i do słabszych komputerów.",
            descriptionEn = "A very small model for simple tasks and weaker computers.",
            pageUrl = "${HF}unsloth/Qwen3.5-2B-GGUF",
        )

    val Qwen4b =
        AiModelInfo(
            id = QWEN_4B,
            intelNpu = false,
            quietThinking = false,
            name = "Qwen 3.5 4B",
            family = "Qwen 3.5",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 2464,
            ramMb = 4000,
            minDeviceRamGb = 6,
            license = AiLicense.APACHE_2_0,
            files = listOf(hf("unsloth/Qwen3.5-4B-GGUF", "Qwen3.5-4B-Q4_0.gguf", 2464)),
            platforms = BOTH,
            descriptionPl = "Mały model dobrze radzący sobie z narzędziami i rozumowaniem; polski słabszy niż w modelach Google i Bielik.",
            descriptionEn = "A small model that handles tools and reasoning well; its Polish is weaker than the Google and Bielik models.",
            pageUrl = "${HF}unsloth/Qwen3.5-4B-GGUF",
        )

    val Qwen9b =
        AiModelInfo(
            id = QWEN_9B,
            intelNpu = false,
            quietThinking = false,
            name = "Qwen 3.5 9B",
            family = "Qwen 3.5",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 5130,
            ramMb = 7500,
            minDeviceRamGb = 12,
            license = AiLicense.APACHE_2_0,
            files = listOf(hf("unsloth/Qwen3.5-9B-GGUF", "Qwen3.5-9B-Q4_0.gguf", 5130)),
            platforms = BOTH,
            descriptionPl = "Średni model z dobrym korzystaniem z narzędzi aplikacji.",
            descriptionEn = "A mid-size model that uses the app's tools well.",
            pageUrl = "${HF}unsloth/Qwen3.5-9B-GGUF",
        )

    val Qwen35b =
        AiModelInfo(
            id = QWEN_35B,
            intelNpu = false,
            quietThinking = false,
            name = "Qwen 3.5 35B A3B",
            family = "Qwen 3.5",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 22016,
            ramMb = 25000,
            minDeviceRamGb = 32,
            license = AiLicense.APACHE_2_0,
            files = listOf(hf("unsloth/Qwen3.5-35B-A3B-GGUF", "Qwen3.5-35B-A3B-Q4_K_M.gguf", 22016)),
            platforms = BOTH,
            descriptionPl = "Bardzo duży model, który w trakcie pracy używa tylko części (3 mld parametrów). Tylko dla komputerów z dużą ilością RAM.",
            descriptionEn = "A very large model that uses only part of itself while working (3 billion parameters). Only for computers with a lot of RAM.",
            pageUrl = "${HF}unsloth/Qwen3.5-35B-A3B-GGUF",
        )

    val Bielik15b =
        AiModelInfo(
            id = BIELIK_15B,
            name = "Bielik 1.5B v3",
            family = "Bielik",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 1700,
            ramMb = 2600,
            minDeviceRamGb = 4,
            license = AiLicense.APACHE_2_0,
            files = listOf(hf("speakleash/Bielik-1.5B-v3.0-Instruct-GGUF", "Bielik-1.5B-v3.0-Instruct.Q8_0.gguf", 1700)),
            platforms = BOTH,
            descriptionPl = "Najmniejszy polski model od SpeakLeash: lekki, dobrze radzi sobie z polskim w prostych rozmowach.",
            descriptionEn = "The smallest Polish model from SpeakLeash: light, handles Polish well in simple conversations.",
            pageUrl = "${HF}speakleash/Bielik-1.5B-v3.0-Instruct-GGUF",
        )

    val Bielik45b =
        AiModelInfo(
            id = BIELIK_45B,
            name = "Bielik 4.5B v3",
            family = "Bielik",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 5061,
            ramMb = 6500,
            minDeviceRamGb = 8,
            license = AiLicense.APACHE_2_0,
            files = listOf(hf("speakleash/Bielik-4.5B-v3.0-Instruct-GGUF", "Bielik-4.5B-v3.0-Instruct.Q8_0.gguf", 5061)),
            platforms = BOTH,
            descriptionPl = "Polski model od SpeakLeash, mały; bardzo dobra polszczyzna.",
            descriptionEn = "A Polish model from SpeakLeash, small; very good Polish.",
            pageUrl = "${HF}speakleash/Bielik-4.5B-v3.0-Instruct-GGUF",
        )

    val Bielik7b =
        AiModelInfo(
            id = BIELIK_7B,
            name = "Bielik Minitron 7B v3",
            family = "Bielik",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 4502,
            ramMb = 6000,
            minDeviceRamGb = 8,
            license = AiLicense.APACHE_2_0,
            files =
            listOf(
                hf(
                    "speakleash/Bielik-Minitron-7B-v3.0-Instruct-GGUF",
                    "minitron-Bielik-7B-v3.0-Instruct-GGUF.Q4_K_M.gguf",
                    4502,
                ),
            ),
            platforms = BOTH,
            descriptionPl = "Polski model od SpeakLeash, odchudzona wersja 11B: dobra polszczyzna, mniejsze wymagania.",
            descriptionEn = "A Polish model from SpeakLeash, a slimmed-down 11B: good Polish, lower requirements.",
            pageUrl = "${HF}speakleash/Bielik-Minitron-7B-v3.0-Instruct-GGUF",
        )

    val Bielik11b =
        AiModelInfo(
            id = BIELIK_11B,
            intelNpu = false,
            name = "Bielik 11B v3",
            family = "Bielik",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 6724,
            ramMb = 8500,
            minDeviceRamGb = 12,
            license = AiLicense.APACHE_2_0,
            files = listOf(hf("speakleash/Bielik-11B-v3.0-Instruct-GGUF", "Bielik-11B-v3.0-Instruct.Q4_K_M.gguf", 6724)),
            platforms = BOTH,
            descriptionPl = "Polski model od SpeakLeash, najlepsza polszczyzna w tej liście.",
            descriptionEn = "A Polish model from SpeakLeash, the best Polish in this list.",
            pageUrl = "${HF}speakleash/Bielik-11B-v3.0-Instruct-GGUF",
        )

    val Pllum12b =
        AiModelInfo(
            id = PLLUM_12B,
            intelNpu = false,
            name = "PLLuM 12B instruct",
            family = "PLLuM",
            engine = AiEngine.LLAMA_CPP,
            roles = TEXT,
            sizeMb = 7477,
            ramMb = 9500,
            minDeviceRamGb = 12,
            license = AiLicense.SEE_MODEL_PAGE,
            files = listOf(hf("mradermacher/PLLuM-12B-instruct-2512-GGUF", "PLLuM-12B-instruct-2512.Q4_K_M.gguf", 7477)),
            platforms = BOTH,
            descriptionPl = "Polski model (polskie ministerstwo cyfryzacji i uczelnie): dobry język urzędowy i ogólny. Plik pochodzi od osoby trzeciej (mradermacher).",
            descriptionEn = "A Polish model (made for the Polish government and universities): good official and general language. The file comes from a third party (mradermacher).",
            pageUrl = "${HF}mradermacher/PLLuM-12B-instruct-2512-GGUF",
        )

    val TranslateGemma4b =
        AiModelInfo(
            id = TG_4B,
            name = "TranslateGemma 4B",
            family = "TranslateGemma",
            engine = AiEngine.LLAMA_CPP,
            roles = setOf(AiRole.TRANSLATOR),
            sizeMb = 2490,
            ramMb = 3800,
            minDeviceRamGb = 6,
            license = AiLicense.GEMMA_TERMS,
            files = listOf(hf("bullerwins/translategemma-4b-it-GGUF", "translategemma-4b-it-Q4_K_M.gguf", 2490)),
            platforms = BOTH,
            descriptionPl = "Model Google stworzony tylko do tłumaczenia (55 języków). Plik pochodzi od osoby trzeciej (bullerwins).",
            descriptionEn = "A Google model made only for translation (55 languages). The file comes from a third party (bullerwins).",
            pageUrl = "${HF}bullerwins/translategemma-4b-it-GGUF",
        )

    val TranslateGemma12b =
        AiModelInfo(
            id = TG_12B,
            intelNpu = false,
            name = "TranslateGemma 12B",
            family = "TranslateGemma",
            engine = AiEngine.LLAMA_CPP,
            roles = setOf(AiRole.TRANSLATOR),
            sizeMb = 7301,
            ramMb = 9500,
            minDeviceRamGb = 12,
            license = AiLicense.GEMMA_TERMS,
            files = listOf(hf("bullerwins/translategemma-12b-it-GGUF", "translategemma-12b-it-Q4_K_M.gguf", 7301)),
            platforms = BOTH,
            descriptionPl = "Większy model do tłumaczenia od Google. Plik pochodzi od osoby trzeciej (bullerwins).",
            descriptionEn = "A larger translation model from Google. The file comes from a third party (bullerwins).",
            pageUrl = "${HF}bullerwins/translategemma-12b-it-GGUF",
        )

    private fun whisper(id: String, name: String, file: String, mb: Int, ramMb: Int, pl: String, en: String) =
        AiModelInfo(
            id = id,
            name = name,
            family = "Whisper",
            engine = AiEngine.WHISPER_CPP,
            roles = setOf(AiRole.DICTATION),
            sizeMb = mb,
            ramMb = ramMb,
            minDeviceRamGb = 4,
            license = AiLicense.MIT,
            files = listOf(hf("ggerganov/whisper.cpp", file, mb)),
            platforms = BOTH,
            descriptionPl = pl,
            descriptionEn = en,
            pageUrl = "${HF}ggerganov/whisper.cpp",
        )

    val WhisperBase =
        whisper(
            WHISPER_BASE, "Whisper base", "ggml-base-q5_1.bin", 60, 400,
            "Najmniejszy model do dyktowania; szybki, ale myli się częściej.",
            "The smallest dictation model; fast, but makes more mistakes.",
        )
    val WhisperSmall =
        whisper(
            WHISPER_SMALL, "Whisper small", "ggml-small-q5_1.bin", 190, 800,
            "Mały model do dyktowania: dobry kompromis szybkości i dokładności.",
            "A small dictation model: a good balance of speed and accuracy.",
        )
    val WhisperMedium =
        whisper(
            WHISPER_MEDIUM, "Whisper medium", "ggml-medium-q5_0.bin", 539, 1800,
            "Dokładniejszy model do dyktowania, także dla polskiego.",
            "A more accurate dictation model, also for Polish.",
        )
    val WhisperTurbo =
        whisper(
            WHISPER_TURBO, "Whisper large v3 turbo", "ggml-large-v3-turbo-q5_0.bin", 574, 2200,
            "Najdokładniejszy z modeli do dyktowania w tej liście, a mimo to dość szybki.",
            "The most accurate dictation model in this list, and still fairly fast.",
        )

    // The same models in the Q8_0 format: the Hexagon NPU of Snapdragon phones does the matrix work only for Q4_0, Q8_0
    // and FP16 (not for Q5), so these are the ones that can run on the NPU. Bigger files, a little slower on the processor.
    val WhisperSmallQ8 =
        whisper(
            WHISPER_SMALL_Q8, "Whisper small (Q8)", "ggml-small-q8_0.bin", 264, 1000,
            "Mały model do dyktowania w formacie Q8: może działać na NPU telefonu ze Snapdragonem, na procesorze jest nieco większy niż zwykły.",
            "A small dictation model in the Q8 format: it can run on the NPU of a Snapdragon phone, on the processor it is a little bigger than the normal one.",
        )
    val WhisperMediumQ8 =
        whisper(
            WHISPER_MEDIUM_Q8, "Whisper medium (Q8)", "ggml-medium-q8_0.bin", 823, 2200,
            "Dokładny model do dyktowania, także dla polskiego, w formacie Q8: może działać na NPU telefonu ze Snapdragonem.",
            "An accurate dictation model, also for Polish, in the Q8 format: it can run on the NPU of a Snapdragon phone.",
        )
    val WhisperTurboQ8 =
        whisper(
            WHISPER_TURBO_Q8, "Whisper large v3 turbo (Q8)", "ggml-large-v3-turbo-q8_0.bin", 874, 2500,
            "Najdokładniejszy model do dyktowania w formacie Q8: może działać na NPU telefonu ze Snapdragonem.",
            "The most accurate dictation model in the Q8 format: it can run on the NPU of a Snapdragon phone.",
        )

    /** Every model the app knows, for every platform. */
    val all: List<AiModelInfo> =
        listOf(
            Nllb600,
            G4E2b, G4E4b, G4_12b, G4_26b,
            Qwen08b, Qwen2b, Qwen4b, Qwen9b, Qwen35b,
            Bielik15b, Bielik45b, Bielik7b, Bielik11b, Pllum12b,
            TranslateGemma4b, TranslateGemma12b,
            WhisperBase, WhisperSmall, WhisperMedium, WhisperTurbo,
            WhisperSmallQ8, WhisperMediumQ8, WhisperTurboQ8,
        )

    /** The models that exist on the platform the app runs on. */
    val available: List<AiModelInfo> get() = all.filter { it.engineReady }

    /** The models of the platform that can do [role], in the order of the list. */
    fun forRole(role: AiRole): List<AiModelInfo> = available.filter { it.canDo(role) }

    fun byId(id: String?): AiModelInfo? = all.firstOrNull { it.id == id }

    /** Ids of the models of the old engine (Google LiteRT-LM); their files are removed once, when the app first starts with the new engine. */
    val legacyModelIds: List<String> = listOf("gemma4-e2b", "gemma4-e4b", "gemma4-12b")
}

/**
 * Tracks the downloaded state of every model in [AiModels.all]. Several models can be on the device at once; each one
 * is downloaded, cancelled and deleted on its own. Which downloaded model is used is a preference (see [UiPrefs]).
 */
interface AiModelStore {
    val states: StateFlow<Map<String, TranslatorModelState>>

    /** Megabytes freed when the files of the old engine were removed at this start (0 = nothing removed); only the first start after the change. */
    val legacyFreedMb: StateFlow<Int> get() = MutableStateFlow(0)

    suspend fun download(id: String)

    fun cancelDownload(id: String)

    suspend fun delete(id: String)

    /** Ids of the models that are in memory right now (empty when none is running). */
    val loadedModels: StateFlow<Set<String>> get() = MutableStateFlow(emptySet())

    /** Stops every running model and frees its memory; the next question starts the model again. */
    suspend fun unloadModels() {}

    /** Forgets the remembered failed chip attempts so the next start tries every chip again. */
    suspend fun clearFailedAttempts() {}
}
