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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Where the big AI files live: the models, the translator files and (on computers) the engine programs. By default that
 * is [defaultBase]; a person can choose another folder (another disk, an SD card). The choice is a small pointer file in
 * [settingsDir], which always stays in the same place, so it is found again at every start.
 *
 * A move copies the files, checks them and writes the pointer; the new folder is used from the next start, and the old
 * files are deleted then (they may still be in use now).
 *
 * [withEngines] is true on computers. On a phone the engine programs must stay in the app's own memory (the system does
 * not run programs from anywhere else), so only the models and the translator move; [choicesProvider] then lists the
 * folders the app is allowed to use (memory, SD card).
 */
class AiStorageLocation(
    private val settingsDir: File,
    val defaultBase: File,
    private val withEngines: Boolean,
    private val choicesProvider: () -> List<File> = { emptyList() },
    private val askOnFirstRun: Boolean = false,
    /** Opens the system folder chooser (computers); null where the choice is a fixed list. */
    val folderPicker: (suspend () -> String?)? = null,
) {
    private val pointer = File(settingsDir, "data-folder.txt")
    private val oldFolders = File(settingsDir, "old-data-folders.txt")
    private val askedMarker = File(settingsDir, "data-folder-asked")

    /** The folder used in this run. */
    val base: File = resolve()

    /** Set after a successful move: the folder used from the next start. */
    @Volatile var nextBase: File? = null
        private set

    val modelsDir: File get() = File(base, MODELS)
    val translatorDir: File get() = File(base, TRANSLATOR)
    val enginesDir: File get() = File(base, ENGINES)

    /** True while the first-start question about the folder has not been answered (computers only). */
    fun needsFirstQuestion(): Boolean = askOnFirstRun && !askedMarker.exists() && !pointer.exists()

    fun markAsked() {
        runCatching {
            settingsDir.mkdirs()
            askedMarker.writeText("done")
        }
    }

    val choices: List<File> get() = runCatching { choicesProvider() }.getOrDefault(emptyList())

    private fun resolve(): File {
        val chosen =
            runCatching { pointer.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }?.let(::File) }
                .getOrNull()
        // A folder that cannot be reached now (a disk that is not connected) is not created again: the default is used.
        val use = chosen?.takeIf { it.isDirectory || runCatching { it.mkdirs() }.getOrDefault(false) } ?: defaultBase
        if (chosen != null && use != chosen) {
            Logger.w(tag = TAG) { "The chosen folder ${chosen.path} cannot be used, using ${defaultBase.path}" }
        }
        cleanOld(use)
        return use
    }

    /** Deletes the AI files left in the folders used before a move, once the new folder holds the files. */
    private fun cleanOld(current: File) {
        runCatching {
            if (!oldFolders.isFile) return
            val olds = oldFolders.readLines().map { it.trim() }.filter { it.isNotEmpty() }.map(::File)
            val currentPath = current.canonicalPath
            Thread {
                    runCatching {
                        for (old in olds) {
                            if (old.canonicalPath == currentPath) continue
                            // Only the folders the app created, and only when the new place has the models.
                            if (!File(current, MODELS).isDirectory && File(old, MODELS).isDirectory) continue
                            for (name in listOf(MODELS, TRANSLATOR, ENGINES)) File(old, name).deleteRecursively()
                        }
                        oldFolders.delete()
                    }
                }
                .apply {
                    isDaemon = true
                    start()
                }
        }
    }

    /** Megabytes free on the disk of [dir] (or of the nearest folder that exists). */
    fun freeMb(dir: File = base): Long {
        var d: File? = dir
        while (d != null && !d.exists()) d = d.parentFile
        return (d?.usableSpace ?: 0L) / MB
    }

    /**
     * Copies the files to [target] and remembers it as the folder for the next start. Returns null when it worked, or a
     * short reason when it did not (nothing is changed then, the old files stay).
     */
    suspend fun move(target: File, onProgress: (Float) -> Unit): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                    val dest = target.absoluteFile.canonicalFile
                    val from = base.canonicalFile
                    if (dest == from) return@runCatching "this folder is already used"
                    if (dest.path.startsWith(from.path + File.separator)) return@runCatching "the new folder is inside the old one"
                    val names = listOf(MODELS, TRANSLATOR) + if (withEngines) listOf(ENGINES) else emptyList()
                    val sources = names.map { File(from, it) }.filter { it.isDirectory }
                    val files = sources.flatMap { dir -> dir.walkTopDown().filter { it.isFile }.map { dir to it }.toList() }
                    val total = files.sumOf { it.second.length() }
                    if (!dest.isDirectory && !dest.mkdirs()) return@runCatching "the folder cannot be created"
                    if (dest.usableSpace < total + total / 20 + 100L * MB) return@runCatching "not enough free space in the new folder"
                    var done = 0L
                    for ((dir, file) in files) {
                        val relative = file.relativeTo(dir.parentFile)
                        val out = File(dest, relative.path)
                        out.parentFile?.mkdirs()
                        val part = File(out.path + ".copy")
                        Files.copy(file.toPath(), part.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        if (part.length() != file.length()) {
                            part.delete()
                            return@runCatching "a file was not copied completely"
                        }
                        Files.move(part.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        done += file.length()
                        onProgress(if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 1f)
                    }
                    settingsDir.mkdirs()
                    pointer.writeText(dest.path)
                    oldFolders.appendText(from.path + "\n")
                    nextBase = dest
                    markAsked()
                    null
                }
                .getOrElse { e ->
                    Logger.w(tag = TAG, throwable = e) { "Moving the AI files failed: ${e.message}" }
                    e.message ?: "unknown error"
                }
        }

    companion object {
        private const val TAG = "AiStorageLocation"
        private const val MODELS = "models"
        private const val TRANSLATOR = "translator"
        private const val ENGINES = "engines"
        private const val MB = 1024L * 1024L
    }
}
