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
package org.meshtastic.feature.messaging.translation

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.meshtastic.core.repository.AiEngine
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Fetches the model programs (llama.cpp and whisper.cpp servers) the first time a model needs one, from the official
 * release pages of those projects, and unpacks them under [root]. Nothing is fetched until the user starts a model
 * download. The kind of program (graphics card version or plain processor version) is picked from the operating system.
 */
class ModelServerInstaller(private val root: File) : AiEngineInstaller {

    init {
        // Older versions remembered even a failed download as "broken" for days. Once per reset number, those marks are
        // cleared so the chips (NPU, graphics card) get a fresh try; raise BROKEN_RESET when that is needed again.
        runCatching {
            val done = File(root, ".broken-reset-$BROKEN_RESET")
            if (!done.exists()) {
                root.listFiles { f -> f.name.startsWith(".broken-") }?.forEach { it.delete() }
                root.mkdirs()
                done.writeText("done")
            }
        }
        // Our own speech programs keep the same file name when they are rebuilt (a fix in the build), so an installed
        // copy is never fetched again by itself. Raise WHISPER_REVISION after such a rebuild: once per number, the
        // installed speech programs are removed and fetched again at the next use.
        runCatching {
            val done = File(root, ".whisper-revision-$WHISPER_REVISION")
            if (!done.exists()) {
                root.listFiles { f -> f.isDirectory && f.name.startsWith("whisper-") }?.forEach { it.deleteRecursively() }
                root.mkdirs()
                done.writeText("done")
            }
        }
    }

    private val lock = Mutex()

    /** One downloadable program: the file on the release page, the name of the program inside, its size in MB. */
    class Asset(val url: String, val folder: String, val exeName: String, val sizeMb: Int)

    private val os: String = System.getProperty("os.name").orEmpty().lowercase()
    private val arch: String = System.getProperty("os.arch").orEmpty().lowercase()
    /** True inside the Android app (the Java runtime there reports an Android vendor). */
    val android: Boolean = System.getProperty("java.vendor").orEmpty().contains("Android") ||
        System.getProperty("java.vm.vendor").orEmpty().contains("Android")
    val mac = !android && os.contains("mac")
    private val windows = !android && os.contains("win")
    private val arm = arch.contains("aarch64") || arch.contains("arm64")
    private val exe = if (windows) ".exe" else ""

    /** True when the llama.cpp program exists for this computer or phone (a 32-bit phone has none). */
    val llamaSupported: Boolean get() = !android || (arm && arch.contains("64"))

    /** True when a speech program exists for this system (none for a Mac or a phone). */
    val whisperSupported: Boolean get() = whisperAsset() != null

    /**
     * The command words that start the program: on Android an app may not start a file from its own folder directly, so
     * the system's own loader runs it; elsewhere the file itself.
     */
    fun launchPrefix(program: File): List<String> =
        if (android && File(LINKER).isFile) listOf(LINKER, program.absolutePath) else listOf(program.absolutePath)

    /** True on a phone with a Qualcomm Snapdragon chip (Adreno graphics), where OpenCL is the first choice. */
    val qualcomm: Boolean by lazy { android && llamaSupported && isQualcomm() }

    /** True on a phone whose processor has the i8mm instructions (newer Snapdragon, Tensor, Dimensity): Q8 models run faster. */
    val i8mm: Boolean by lazy {
        android && runCatching {
            File("/proc/cpuinfo").useLines { lines -> lines.any { it.startsWith("Features") && Regex("\\bi8mm\\b").containsMatchIn(it) } }
        }.getOrDefault(false)
    }

    private fun isQualcomm(): Boolean {
        fun prop(name: String): String =
            runCatching {
                ProcessBuilder("getprop", name).redirectErrorStream(true).start().inputStream.bufferedReader().readText().trim().lowercase()
            }.getOrDefault("")
        return prop("ro.soc.manufacturer").contains("qualcomm") || prop("ro.hardware") == "qcom" ||
            prop("ro.board.platform").let { it.startsWith("sm") || it in QUALCOMM_PLATFORMS }
    }

    /**
     * The graphics card programs to try on this phone, best first: our own builds of llama.cpp with OpenCL (Snapdragon /
     * Adreno only) and Vulkan (any phone), published on this project's release page. The official Android build runs on
     * the processor only. A program that was missing or did not work on this phone is skipped for a while.
     */
    fun androidGpuAssets(modelId: String? = null): List<Asset> = androidGpuAssetsAll().filterNot { isBroken(it, modelId) }

    /** The graphics card programs this phone can try, whatever failed before. */
    fun androidGpuAssetsAll(): List<Asset> {
        if (!android || !llamaSupported) return emptyList()
        val all = buildList {
            if (qualcomm) add(gpuAsset("opencl"))
            add(gpuAsset("vulkan"))
        }
        return all
    }

    private fun gpuAsset(kind: String) =
        Asset(
            url = "https://github.com/MT-SW/MT_SW_APP/releases/download/engines-$LLAMA_TAG/llama-$LLAMA_TAG-android-arm64-$kind.tar.gz",
            folder = "llama-$LLAMA_TAG-android-arm64-$kind",
            exeName = "llama-server",
            sizeMb = LLAMA_ANDROID_GPU_MB,
        )

    /** Forgets every remembered failure (NPU and graphics card programs), so the next start tries them all again. */
    fun clearBrokenMarks() {
        runCatching {
            root.listFiles { f -> f.name.startsWith(".broken-") && !f.name.startsWith(".broken-reset-") }?.forEach { it.delete() }
        }
    }

    private fun systemProperty(name: String): String =
        runCatching {
            ProcessBuilder("getprop", name).redirectErrorStream(true).start().inputStream.bufferedReader().readText().trim()
        }.getOrDefault("")

    /**
     * A few lines about this device for the `/sprzet` report: chip, graphics driver, memory. People send them in, so that
     * phones where a chip does not work can be found and handled.
     */
    fun deviceSummary(): String = buildString {
        if (android) {
            val soc = listOf(systemProperty("ro.soc.manufacturer"), systemProperty("ro.soc.model")).filter { it.isNotBlank() }.joinToString(" ")
            appendLine(tr("Device", "Urządzenie") + ": ${systemProperty("ro.product.manufacturer")} ${systemProperty("ro.product.model")}, Android ${systemProperty("ro.build.version.release")} (API ${systemProperty("ro.build.version.sdk")})")
            appendLine(tr("Chip", "Układ") + ": ${soc.ifBlank { "?" }}, " + tr("platform", "platforma") + " ${systemProperty("ro.board.platform").ifBlank { "?" }}, " + tr("hardware", "sprzęt") + " ${systemProperty("ro.hardware").ifBlank { "?" }}")
            appendLine(tr("Graphics driver", "Sterownik grafiki") + ": vulkan=${systemProperty("ro.hardware.vulkan").ifBlank { "?" }}, egl=${systemProperty("ro.hardware.egl").ifBlank { "?" }}, " + tr("Vulkan level", "poziom Vulkan") + " ${systemProperty("ro.hardware.vulkan.level").ifBlank { "?" }}")
        } else {
            appendLine(tr("Computer", "Komputer") + ": ${System.getProperty("os.name")} $arch, " + tr("processor", "procesor") + ": ${cpuName().ifBlank { "?" }}")
        }
        val memoryMb = runCatching {
            File("/proc/meminfo").takeIf { it.isFile }?.readLines()?.firstOrNull { it.startsWith("MemTotal") }
                ?.filter { it.isDigit() }?.toLong()?.div(1024)
        }.getOrNull()
        if (memoryMb != null) appendLine(tr("Device memory (RAM)", "Pamięć urządzenia (RAM)") + ": $memoryMb MB")
        intelNpuDriver.takeIf { intelNpu && it.isNotEmpty() }?.let { appendLine(tr("Intel NPU driver", "Sterownik NPU Intela") + ": $it") }
    }

    /**
     * The version of the Intel NPU driver ("" when not known). A failure of the NPU is remembered together with it, so an
     * updated driver gets a new try at once instead of waiting for the week to pass.
     */
    val intelNpuDriver: String by lazy {
        if (!windows) return@lazy ""
        runCatching {
            val script = "(Get-PnpDevice -FriendlyName '*AI Boost*' -ErrorAction SilentlyContinue | Select-Object -First 1 | " +
                "Get-PnpDeviceProperty -KeyName DEVPKEY_Device_DriverVersion -ErrorAction SilentlyContinue).Data"
            val process = ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", script).redirectErrorStream(true).start()
            val text = process.inputStream.bufferedReader().readText().trim()
            process.waitFor(DRIVER_QUERY_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
            text.takeIf { Regex("[0-9]+(\\.[0-9]+)+").matches(it) }.orEmpty()
        }.getOrDefault("")
    }

    private fun tr(en: String, pl: String): String = if (java.util.Locale.getDefault().language == "pl") pl else en

    private fun brokenMark(asset: Asset, key: String?): File {
        val driver = if (asset.folder.contains("openvino", ignoreCase = true)) intelNpuDriver else ""
        val tag = (key?.let { "-" + it.replace(Regex("[^A-Za-z0-9._-]"), "_") } ?: "") + (if (driver.isNotEmpty()) "-drv$driver" else "")
        return File(root, ".broken-" + asset.folder + tag)
    }

    /** True when [asset] (with [key]: only for that model) failed here recently and is skipped for now. */
    fun isBroken(asset: Asset, key: String? = null): Boolean {
        val mark = brokenMark(asset, key)
        return mark.isFile && System.currentTimeMillis() - mark.lastModified() < BROKEN_RETRY_MS
    }

    /**
     * Remembers that [asset] could not be fetched or did not work on this device, so it is not tried again for a few
     * days. With [key] (a model id) only that model skips it: an NPU may run one model and refuse another.
     */
    fun markBroken(asset: Asset, key: String? = null) {
        runCatching { brokenMark(asset, key).apply { parentFile?.mkdirs() }.writeText("broken") }
            .onFailure { Logger.w(tag = TAG, throwable = it) { "Could not remember that ${asset.folder} failed: ${it.message}" } }
    }

    /**
     * The official llama.cpp build for Snapdragon phones with the Hexagon NPU (and the Adreno graphics card). The NPU
     * exists on Snapdragon 8 Gen 2 and newer; on other chips the program starts without it and the app goes on.
     */
    fun androidNpuAsset(): Asset? {
        if (!qualcomm) return null
        return llama("android-arm64-snapdragon.tar.gz", LLAMA_SNAPDRAGON_MB)
    }

    /**
     * True on a computer with an Intel Core Ultra processor, which has an NPU (Windows or Linux, 64-bit Intel). Older
     * Intel processors and AMD ones have none that the engine can use.
     */
    val intelNpu: Boolean by lazy { !android && !mac && !arm && isCoreUltra() }

    override val npuSupported: Boolean get() = intelNpu || androidNpuAsset() != null

    private fun cpuName(): String = runCatching {
        if (windows) {
            ProcessBuilder("reg", "query", "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "/v", "ProcessorNameString")
                .redirectErrorStream(true).start().inputStream.bufferedReader().readText().trim()
        } else if (mac) {
            ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").redirectErrorStream(true).start().inputStream.bufferedReader().readText().trim()
        } else {
            File("/proc/cpuinfo").takeIf { it.isFile }?.readLines()?.firstOrNull { it.startsWith("model name") }.orEmpty().substringAfter(':').trim()
        }
    }.getOrDefault("")

    private fun isCoreUltra(): Boolean = runCatching {
        val name = cpuName()
        val ultra = Regex("core\\(tm\\)\\s*ultra|core\\s+ultra", RegexOption.IGNORE_CASE).containsMatchIn(name)
        // On Linux the NPU also needs its driver (it shows up as an accelerator device).
        ultra && (windows || File("/dev/accel").isDirectory)
    }.getOrDefault(false)

    /** The official llama.cpp build with OpenVINO (Intel CPU, graphics and NPU); null where there is no Intel NPU. */
    fun intelNpuAsset(): Asset? {
        if (!intelNpu) return null
        return if (windows) llama("win-openvino-$OPENVINO_VERSION-x64.zip", LLAMA_OPENVINO_WIN_MB) else llama("ubuntu-openvino-$OPENVINO_VERSION-x64.tar.gz", LLAMA_OPENVINO_LINUX_MB)
    }

    /** On a Windows computer with a Snapdragon X (Adreno graphics): the official OpenCL build, faster there than Vulkan. */
    fun windowsArmGpuAsset(): Asset? = if (windows && arm) llama("win-opencl-adreno-arm64.zip", LLAMA_WIN_ADRENO_MB) else null

    /** A folder for the NPU's compiled models, so the slow first compilation is done only once per model. */
    fun cacheDir(name: String): File = File(root, "cache-$name").apply { mkdirs() }

    /** Installs a graphics card program; null (and remembered) when it cannot be fetched. */
    @Volatile
    var lastInstallError: String = ""
        private set

    suspend fun installGpu(asset: Asset): File? =
        try {
            install(asset)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Logger.w(tag = TAG, throwable = e) { "${asset.folder} is not available" }
            lastInstallError = "${asset.folder}: ${e.message}"
            null
        }

    /** Installs the official llama.cpp program of this system and returns it. */
    suspend fun installLlama(onProgress: (Float) -> Unit = {}): File = install(llamaAsset(), onProgress)

    /** The official llama.cpp program of this system: graphics card support on a Mac (Metal), Windows and Linux (Vulkan); on Android it runs on the processor only. */
    fun llamaAsset(): Asset {
        val suffix =
            when {
                android -> "android-arm64.tar.gz"
                mac -> if (arm) "macos-arm64.tar.gz" else "macos-x64.tar.gz"
                windows -> if (arm) "win-vulkan-arm64.zip" else "win-vulkan-x64.zip"
                else -> if (arm) "ubuntu-vulkan-arm64.tar.gz" else "ubuntu-vulkan-x64.tar.gz"
            }
        val size = if (android) LLAMA_ANDROID_MB else if (mac) LLAMA_MAC_MB else if (windows) LLAMA_WIN_MB else LLAMA_LINUX_MB
        return llama(suffix, size)
    }

    /** The llama.cpp program for the processor only, used when the graphics card version does not start (null on a Mac and on Android: their program has both). */
    fun llamaCpuAsset(): Asset? {
        val suffix =
            when {
                android || mac -> return null
                windows -> if (arm) "win-cpu-arm64.zip" else "win-cpu-x64.zip"
                else -> if (arm) "ubuntu-arm64.tar.gz" else "ubuntu-x64.tar.gz"
            }
        return llama(suffix, LLAMA_CPU_MB)
    }

    /**
     * The whisper.cpp program that always works (processor). On a Mac it is our own Metal build, run without the
     * graphics card; null for an Intel Mac, where there is no program.
     */
    /**
     * The speech programs for the processor, best first: on a phone with i8mm the faster build, then the plain one (used
     * when the faster one cannot be fetched or does not start).
     */
    fun whisperProcessorAssets(): List<Asset> {
        val plain = whisperAsset() ?: return emptyList()
        if (!android || !i8mm) return listOf(plain)
        val fast =
            Asset(
                url = "https://github.com/MT-SW/MT_SW_APP/releases/download/engines-$LLAMA_TAG/whisper-$WHISPER_REF-android-arm64-i8mm.tar.gz",
                folder = "whisper-$WHISPER_REF-android-arm64-i8mm",
                exeName = "whisper-server",
                sizeMb = WHISPER_ANDROID_MB,
            )
        return listOf(fast, plain)
    }

    fun whisperAsset(): Asset? {
        if (android) {
            // The whisper.cpp project publishes no Android program: our own build (workflow "Build whisper.cpp for Android").
            if (!llamaSupported) return null
            return Asset(
                url = "https://github.com/MT-SW/MT_SW_APP/releases/download/engines-$LLAMA_TAG/whisper-$WHISPER_REF-android-arm64.tar.gz",
                folder = "whisper-$WHISPER_REF-android-arm64",
                exeName = "whisper-server",
                sizeMb = WHISPER_ANDROID_MB,
            )
        }
        val file =
            when {
                mac -> return whisperDesktopChip()
                windows -> if (arm) "whisper-bin-win-cpu-arm64.zip" else "whisper-bin-x64.zip"
                else -> if (arm) "whisper-bin-ubuntu-arm64.tar.gz" else "whisper-bin-ubuntu-x64.tar.gz"
            }
        return Asset(
            url = "https://github.com/ggml-org/whisper.cpp/releases/download/$WHISPER_TAG/$file",
            folder = "whisper-$WHISPER_TAG-" + file.substringBefore('.'),
            exeName = "whisper-server$exe",
            sizeMb = WHISPER_MB,
        )
    }

    /**
     * The whisper.cpp builds that use the phone's chips, best first: the Hexagon NPU (Snapdragon only), then the
     * graphics card (OpenCL on a Snapdragon, Vulkan on any phone). Our own builds (workflow "Build llama.cpp and
     * whisper.cpp for Android"); the processor build is [whisperAsset]. Empty where there is no such build.
     */
    fun whisperChipAssets(): List<Asset> {
        if (!android) return listOfNotNull(whisperDesktopChip())
        if (!llamaSupported) return emptyList()
        return buildList {
            if (qualcomm) add(whisperChip("snapdragon", WHISPER_SNAPDRAGON_MB))
            if (qualcomm) add(whisperChip("opencl", WHISPER_GPU_MB))
            // The same card with the general OpenCL code instead of the Adreno-only kernels (the Adreno ones crash at loading).
            if (qualcomm) add(whisperChip("openclgen", WHISPER_GPU_MB))
            add(whisperChip("vulkan", WHISPER_GPU_MB))
        }
    }

    /**
     * The computer builds of whisper.cpp that use the graphics card: Metal on an Apple Silicon Mac, Vulkan on Windows
     * (64-bit Intel/AMD) and Linux. Our own builds (workflow "Build whisper.cpp for computers"); null elsewhere.
     */
    private fun whisperDesktopChip(): Asset? {
        val name =
            when {
                mac -> if (arm) "macos-arm64-metal.tar.gz" else return null
                windows -> if (arm) return null else "win-x64-vulkan.zip"
                else -> if (arm) "linux-arm64-vulkan.tar.gz" else "linux-x64-vulkan.tar.gz"
            }
        return Asset(
            url = "https://github.com/MT-SW/MT_SW_APP/releases/download/engines-$LLAMA_TAG/whisper-$WHISPER_REF-$name",
            folder = "whisper-$WHISPER_REF-" + name.substringBefore('.'),
            exeName = "whisper-server$exe",
            sizeMb = WHISPER_GPU_MB,
        )
    }

    private fun whisperChip(kind: String, sizeMb: Int) =
        Asset(
            url = "https://github.com/MT-SW/MT_SW_APP/releases/download/engines-$LLAMA_TAG/whisper-$WHISPER_REF-android-arm64-$kind.tar.gz",
            folder = "whisper-$WHISPER_REF-android-arm64-$kind",
            exeName = "whisper-server",
            sizeMb = sizeMb,
        )

    private fun llama(suffix: String, sizeMb: Int) =
        Asset(
            url = "https://github.com/ggml-org/llama.cpp/releases/download/$LLAMA_TAG/llama-$LLAMA_TAG-bin-$suffix",
            folder = "llama-$LLAMA_TAG-" + suffix.substringBefore('.'),
            exeName = "llama-server$exe",
            sizeMb = sizeMb,
        )

    private fun assetFor(engine: AiEngine): Asset? =
        when (engine) {
            AiEngine.LLAMA_CPP -> llamaAsset()
            AiEngine.WHISPER_CPP -> whisperAsset()
            else -> null
        }

    override fun handles(engine: AiEngine): Boolean = assetFor(engine) != null

    override fun missingMb(engine: AiEngine): Int = assetFor(engine)?.let { if (executable(it) == null) it.sizeMb else 0 } ?: 0

    /** The program of [asset] when it is installed, or null. */
    fun executable(asset: Asset): File? {
        val dir = File(root, asset.folder)
        if (!File(dir, READY_MARK).isFile) return null
        return dir.walkTopDown().firstOrNull { it.isFile && it.name == asset.exeName }
    }

    override suspend fun ensure(engine: AiEngine, onProgress: (Float) -> Unit) {
        val asset = assetFor(engine) ?: return
        install(asset, onProgress)
    }

    /** Installs [asset] unless it is there already and returns its program file. */
    suspend fun install(asset: Asset, onProgress: (Float) -> Unit = {}): File = lock.withLock {
        executable(asset)?.let { return it }
        withContext(Dispatchers.IO) {
            val dir = File(root, asset.folder)
            dir.deleteRecursively()
            dir.mkdirs()
            val archive = File(root, asset.folder + ARCHIVE_PART)
            try {
                download(asset.url, archive) { onProgress(it * DOWNLOAD_SHARE) }
                onProgress(DOWNLOAD_SHARE)
                unpack(archive, dir)
                val program = dir.walkTopDown().firstOrNull { it.isFile && it.name == asset.exeName }
                    ?: error("${asset.exeName} is missing in ${asset.url.substringAfterLast('/')}")
                if (android) prune(dir)
                program.setExecutable(true)
                dir.walkTopDown().filter { it.isFile && (it.name.endsWith(".dylib") || it.name.contains(".so")) }.forEach { it.setExecutable(true) }
                if (mac) runCatching { ProcessBuilder("xattr", "-dr", "com.apple.quarantine", dir.absolutePath).start().waitFor(UNPACK_SECONDS, TimeUnit.SECONDS) }
                File(dir, READY_MARK).writeText(asset.url)
                onProgress(1f)
                Logger.i(tag = TAG) { "Installed ${asset.exeName} from ${asset.url}" }
                program
            } catch (e: Throwable) {
                runCatching { dir.deleteRecursively() }
                throw e
            } finally {
                archive.delete()
            }
        }
    }

    /** The Android archive carries many tools the app does not use (benchmarks, quantizers, ...); they only take space. */
    private fun prune(dir: File) {
        val keep = Regex("whisper-server|llama-server|libllama-server-impl\\.so|libllama\\.so|libllama-common\\.so|libwhisper.*\\.so|libggml.*\\.so|libmtmd\\.so|LICENSE")
        dir.walkTopDown().filter { it.isFile && !keep.matches(it.name) }.forEach { it.delete() }
        // The remote-procedure backend is not used.
        File(dir, "libggml-rpc.so").delete()
    }

    private suspend fun download(url: String, target: File, onProgress: (Float) -> Unit) {
        target.parentFile?.mkdirs()
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            if (connection.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${connection.responseCode} for $url")
            val total = connection.contentLengthLong
            var written = 0L
            FileOutputStream(target).use { out ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        written += n
                        if (total > 0) onProgress(written.toFloat() / total)
                    }
                }
            }
        } catch (e: CancellationException) {
            target.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }

    private fun unpack(archive: File, into: File) {
        if (archive.name.contains(".tar.gz") || archive.name.endsWith(ARCHIVE_PART) && !isZip(archive)) {
            val process = ProcessBuilder("tar", "-xzf", archive.absolutePath, "-C", into.absolutePath).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            if (!process.waitFor(UNPACK_SECONDS, TimeUnit.SECONDS) || process.exitValue() != 0) error("Unpacking failed: ${output.take(MAX_ERROR)}")
            return
        }
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            val base = into.canonicalFile
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = File(into, entry.name).canonicalFile
                // An entry that points outside the folder is never written.
                if (!target.path.startsWith(base.path + File.separator)) continue
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { zip.copyTo(it) }
                }
            }
        }
    }

    private fun isZip(file: File): Boolean = file.inputStream().use { it.read() == 'P'.code && it.read() == 'K'.code }

    /** Removes every installed program (they are fetched again when a model needs one). Returns the megabytes freed. */
    fun deleteAll(): Long {
        val size = root.walkTopDown().filter { it.isFile }.sumOf { it.length() } / MB
        root.deleteRecursively()
        return size
    }

    companion object {
        private const val TAG = "ModelServerInstaller"

        /** The llama.cpp build the app is tested with. */
        const val LLAMA_TAG = "b11960"

        /** The whisper.cpp source version of the Android build (our own, see the workflow). */
        const val WHISPER_REF = "v1.9.5"

        /** The whisper.cpp build the app is tested with. */
        const val WHISPER_TAG = "b5454"

        private const val LINKER = "/system/bin/linker64"
        private const val LLAMA_ANDROID_MB = 78
        private const val LLAMA_ANDROID_GPU_MB = 15
        private const val LLAMA_SNAPDRAGON_MB = 60
        private const val LLAMA_OPENVINO_WIN_MB = 99
        private const val LLAMA_OPENVINO_LINUX_MB = 116
        private const val LLAMA_WIN_ADRENO_MB = 13

        /** The OpenVINO version in the names of the llama.cpp release files (changes together with [LLAMA_TAG]). */
        const val OPENVINO_VERSION = "2026.4.1"
        private const val BROKEN_RESET = 6

        /** Raise when our speech programs were rebuilt under the same file name (see the init block). */
        private const val WHISPER_REVISION = 1
        private const val DRIVER_QUERY_SECONDS = 10L
        private const val BROKEN_RETRY_MS = 7L * 24L * 60L * 60L * 1000L
        private val QUALCOMM_PLATFORMS = setOf("kalama", "pineapple", "sun", "taro", "lahaina", "kona", "msmnile", "lito", "bengal", "crow", "parrot", "waipio", "cliffs", "canoe")
        private const val LLAMA_MAC_MB = 16
        private const val LLAMA_WIN_MB = 37
        private const val LLAMA_LINUX_MB = 35
        private const val LLAMA_CPU_MB = 24
        private const val WHISPER_MB = 10
        private const val WHISPER_ANDROID_MB = 3
        private const val WHISPER_SNAPDRAGON_MB = 30
        private const val WHISPER_GPU_MB = 6
        private const val READY_MARK = ".installed"
        private const val ARCHIVE_PART = ".archive.part"
        private const val DOWNLOAD_SHARE = 0.9f
        private const val UNPACK_SECONDS = 120L
        private const val MAX_ERROR = 300
        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val BUFFER = 64 * 1024
        private const val MB = 1024L * 1024L
    }
}
