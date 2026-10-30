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
package org.meshtastic.core.common

import co.touchlab.kermit.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The one folder that holds everything the desktop app keeps for itself: the message database, the settings, the logs,
 * the map files, the AI models and engine programs. By default it is `~/.meshtastic` (or the folder named by the
 * `MESHTASTIC_DATA_DIR` variable, which always wins). A person can choose another place (another disk) in the settings.
 *
 * The choice is a small pointer file that always stays in [defaultDir], so it is found again at every start. Moving is
 * done in two steps because the database and the settings are open while the app runs and must not be copied then:
 * [schedule] only writes a request, and [prepare] (called first thing at the next start, before anything is opened)
 * copies the whole folder, checks every file, switches the pointer and removes the old files.
 */
object AppDataRoot {
    private const val TAG = "AppDataRoot"
    private const val POINTER = "data-folder.txt"
    private const val PENDING = "data-move-pending.txt"
    private const val OLD_FOLDERS = "old-data-folders.txt"
    private const val ERROR = "data-move-error.txt"
    private const val ASKED = "data-folder-asked"

    /** The folder created inside a place a person picks, so that picking a disk or a home folder never scatters files. */
    const val SUBFOLDER = "MT_SW"

    private const val MB = 1024L * 1024L
    private const val SPARE_MB = 100L

    /** Files that describe where the data is; they always stay in [defaultDir] and are never copied or removed. */
    private val SETTINGS_FILES = setOf(POINTER, PENDING, OLD_FOLDERS, ERROR, ASKED)

    private val overridden: Boolean = !System.getenv("MESHTASTIC_DATA_DIR").isNullOrBlank()

    /** Where the pointer file lives and the folder used when nothing else was chosen. */
    val defaultDir: File =
        System.getenv("MESHTASTIC_DATA_DIR")?.takeIf { it.isNotBlank() }?.let(::File)
            ?: File(System.getProperty("user.home"), ".meshtastic")

    @Volatile private var prepared = false

    @Volatile private var current: File? = null

    /** The folder used in this run. Makes sure a requested move is done first (without a progress window). */
    val dir: File
        get() {
            current?.let { return it }
            prepare()
            return current ?: defaultDir
        }

    /** True when a move was requested and will be done by [prepare]. */
    fun hasPendingMove(): Boolean = pendingFile().isFile

    /** The folder a requested move will switch to at the next start; null = none requested. */
    fun pendingTarget(): File? = readPath(File(defaultDir, PENDING))

    /** The reason the last move failed (shown once in the settings); null = none. */
    fun lastMoveError(): String? = runCatching { File(defaultDir, ERROR).takeIf { it.isFile }?.readText()?.trim() }.getOrNull()?.takeIf { it.isNotEmpty() }

    fun clearMoveError() {
        runCatching { File(defaultDir, ERROR).delete() }
    }

    /** True until the first-start question about the folder has been answered. */
    fun needsFirstQuestion(): Boolean = !overridden && !File(defaultDir, ASKED).exists() && !File(defaultDir, POINTER).exists()

    fun markAsked() {
        runCatching {
            defaultDir.mkdirs()
            File(defaultDir, ASKED).writeText("done")
        }
    }

    /** False when the folder comes from `MESHTASTIC_DATA_DIR`: then it cannot be changed in the settings. */
    val canChange: Boolean get() = !overridden

    /** Megabytes free on the disk that holds [folder] (or the nearest folder above it that exists). */
    fun freeMb(folder: File = dir): Long {
        var d: File? = folder.absoluteFile
        while (d != null && !d.exists()) d = d.parentFile
        return (d?.usableSpace ?: 0L) / MB
    }

    /** Megabytes the app's data takes in the current folder. */
    fun usedMb(): Long = treeSize(dir) / MB

    /**
     * Requests a move of everything to [chosen] (a subfolder [SUBFOLDER] is made inside it). Returns null when the
     * request was saved, or a short reason when it cannot be done. Nothing is copied yet.
     */
    fun schedule(chosen: File): String? =
        runCatching {
                if (overridden) return@runCatching "the folder is set by MESHTASTIC_DATA_DIR"
                val picked = chosen.absoluteFile.canonicalFile
                val target =
                    if (picked == defaultDir.absoluteFile.canonicalFile || picked.name == SUBFOLDER) picked else File(picked, SUBFOLDER)
                val from = dir.absoluteFile.canonicalFile
                if (target == from) return@runCatching "this folder is already used"
                if (target.path.startsWith(from.path + File.separator)) return@runCatching "the new folder is inside the old one"
                if (from.path.startsWith(target.path + File.separator)) return@runCatching "the old folder is inside the new one"
                if (!target.isDirectory && !target.mkdirs()) return@runCatching "the folder cannot be created"
                val needMb = treeSize(from) / MB * 105 / 100 + SPARE_MB
                if (freeMb(target) < needMb) return@runCatching "not enough free space in the new folder (needs about $needMb MB)"
                defaultDir.mkdirs()
                File(defaultDir, PENDING).writeText(target.path)
                File(defaultDir, ERROR).delete()
                null
            }
            .getOrElse { e ->
                Logger.w(tag = TAG, throwable = e) { "Requesting the move failed: ${e.message}" }
                e.message ?: "unknown error"
            }

    /** Takes back a requested move that has not been done yet. */
    fun cancelPending() {
        runCatching { File(defaultDir, PENDING).delete() }
    }

    /**
     * Must run before anything uses [dir]. Does the requested move (if any), then settles the folder for this run.
     * [onProgress] gets the fraction done (0..1) while files are copied; it is called on the calling thread.
     */
    @Synchronized
    fun prepare(onProgress: ((Float) -> Unit)? = null) {
        if (prepared) return
        prepared = true
        if (overridden) {
            current = defaultDir
            return
        }
        val pointed = readPath(File(defaultDir, POINTER))
        var use = pointed?.takeIf { it.isDirectory } ?: defaultDir
        if (pointed != null && use != pointed) {
            Logger.w(tag = TAG) { "The chosen folder ${pointed.path} cannot be used now, using ${defaultDir.path}" }
        }
        val target = pendingTarget()
        if (target != null) {
            val error = doMove(use, target, onProgress)
            if (error == null) {
                use = target
            } else {
                Logger.w(tag = TAG) { "Moving the data to ${target.path} failed: $error" }
                runCatching { File(defaultDir, ERROR).writeText(error) }
            }
            runCatching { File(defaultDir, PENDING).delete() }
        }
        current = use
        use.mkdirs()
    }

    private fun pendingFile() = File(defaultDir, PENDING)

    private fun readPath(file: File): File? =
        runCatching { file.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }?.let(::File) }.getOrNull()

    private fun treeSize(root: File): Long =
        runCatching {
                if (!root.isDirectory) return@runCatching 0L
                root.walkTopDown()
                    .onEnter { dir -> !(dir.parentFile == root && dir.name in SETTINGS_FILES) }
                    .filter { it.isFile && !(it.parentFile == root && it.name in SETTINGS_FILES) }
                    .sumOf { it.length() }
            }
            .getOrDefault(0L)

    /** Copies everything from [from] to [to], checks it, switches the pointer and removes the old files. Null = done. */
    private fun doMove(from: File, to: File, onProgress: ((Float) -> Unit)?): String? =
        runCatching {
                val source = from.absoluteFile.canonicalFile
                val dest = to.absoluteFile.canonicalFile
                if (dest == source) return@runCatching null
                if (dest.path.startsWith(source.path + File.separator) || source.path.startsWith(dest.path + File.separator)) {
                    return@runCatching "one folder is inside the other"
                }
                if (!dest.isDirectory && !dest.mkdirs()) return@runCatching "the folder cannot be created"
                val files =
                    source.walkTopDown().filter { it.isFile && !(it.parentFile == source && it.name in SETTINGS_FILES) }.toList()
                val total = files.sumOf { it.length() }
                if (freeMb(dest) * MB < total + total / 20 + SPARE_MB * MB) return@runCatching "not enough free space in the new folder"
                var done = 0L
                onProgress?.invoke(0f)
                for (file in files) {
                    val out = File(dest, file.relativeTo(source).path)
                    out.parentFile?.mkdirs()
                    val part = File(out.path + ".copy")
                    Files.copy(file.toPath(), part.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    if (part.length() != file.length()) {
                        part.delete()
                        return@runCatching "a file was not copied completely: ${file.name}"
                    }
                    Files.move(part.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    done += file.length()
                    onProgress?.invoke(if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 1f)
                }
                defaultDir.mkdirs()
                val pointer = File(defaultDir, POINTER)
                if (dest == defaultDir.absoluteFile.canonicalFile) pointer.delete() else pointer.writeText(dest.path)
                File(defaultDir, ASKED).writeText("done")
                File(defaultDir, ERROR).delete()
                removeOld(source, files)
                null
            }
            .getOrElse { e ->
                Logger.w(tag = TAG, throwable = e) { "Moving the data failed: ${e.message}" }
                e.message ?: "unknown error"
            }

    /** Removes the files that were copied from the old folder (not the pointer files, and nothing the app did not make). */
    private fun removeOld(source: File, copied: List<File>) {
        runCatching {
            copied.forEach { it.delete() }
            // Empty folders left behind, deepest first; the old folder itself only when it was a made one.
            source.walkBottomUp().filter { it.isDirectory && it != source }.forEach { it.delete() }
            if (source.name == SUBFOLDER && source != defaultDir.absoluteFile.canonicalFile) source.delete()
        }
    }
}
