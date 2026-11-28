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
@file:Suppress("MagicNumber", "ReturnCount", "InjectDispatcher")

package org.meshtastic.app

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ClipData
import android.content.ComponentCallbacks2
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.meshtastic.core.common.log.AppLogControl
import org.meshtastic.core.common.log.AppLogControls
import org.meshtastic.core.common.log.AppLogLevel
import org.meshtastic.core.common.log.LogInsights
import org.meshtastic.core.common.log.MinSeverityLogWriter
import org.meshtastic.core.common.log.MtSwLogging
import org.meshtastic.core.common.log.ReportFile
import org.meshtastic.core.common.log.ReportTarget
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ref.WeakReference
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The phone side of the log switch. Logs live in the app's private folder; because a crashed app cannot be asked for
 * them, a zip of them is also kept in the Downloads folder (`Pobrane/MT_SW_logi`): refreshed on its own when the app
 * starts, every few minutes while it runs, and when it goes to the background while logging is on, and on a start that
 * follows a crash even when logging is off (only the crash record is in it then).
 */
internal object AndroidLogging {
    private const val TAG = "AppLifecycle"
    private const val NAME_24H = "mt_sw_logi_24h.zip"
    private const val NAME_FULL = "mt_sw_logi_pelne.zip"
    private const val FOLDER = "MT_SW_logi"
    private const val APP_FOLDER = "MT_SW"
    private const val EXIT_REASONS_SHOWN = 6
    private const val FINGERPRINT_24H = "last_export_24h.txt"
    private const val FINGERPRINT_FULL = "last_export_full.txt"
    private const val MB = 1024 * 1024
    private const val PNG_QUALITY = 100
    private const val PICTURE_WAIT_SECONDS = 1L
    private const val AUTO_EXPORT_EVERY_MS = 3 * 60 * 1000L

    /** Kermit's lowest severity for this build when the file log is off. */
    val baseSeverity: Severity
        get() = if (BuildConfig.DEBUG) Severity.Debug else Severity.Info

    fun install(app: Application, scope: CoroutineScope) {
        val dir = File(app.filesDir, "logs")
        Logger.setMinSeverity(baseSeverity)
        Logger.setLogWriters(
            MinSeverityLogWriter(platformLogWriter(), baseSeverity),
            MtSwLogging.writer,
            LogInsights.writer,
        )
        MtSwLogging.init(dir, headerText = { header() }, sample = { sampleExtras(app) }) { level ->
            Logger.setMinSeverity(MtSwLogging.minSeverityFor(baseSeverity, level))
        }
        MtSwLogging.screenProvider = { screenInfo() }
        val control = AndroidLogControl(app, dir, scope)
        AppLogControls.current = control
        Logger.i(tag = TAG) { "App start: ${header()}; log level ${MtSwLogging.level.value.key}" }
        registerCallbacks(app, control, scope)
        logPreviousExits(app)
        scope.launch {
            // Logs are saved on their own: at start, while the app runs, and when it goes to the background.
            while (true) {
                control.autoExport()
                delay(AUTO_EXPORT_EVERY_MS)
            }
        }
    }

    @Volatile private var resumedActivity: WeakReference<Activity>? = null

    /**
     * What is saved when the interface goes wrong (only while the full log is on): the size of the screen and window,
     * and a picture of the window. Best effort: a picture is taken only while an activity is on screen.
     */
    private fun screenInfo(): List<Pair<String, ByteArray>> {
        val activity = resumedActivity?.get() ?: return emptyList()
        val metrics = activity.resources.displayMetrics
        val config = activity.resources.configuration
        val info =
            "Ekran: ${metrics.widthPixels} x ${metrics.heightPixels} px, ${metrics.densityDpi} dpi, " +
                "okno ${config.screenWidthDp} x ${config.screenHeightDp} dp, orientacja ${config.orientation}\n"
        val result = mutableListOf("ekran.txt" to info.toByteArray())
        val draw: () -> ByteArray? = {
            runCatching {
                val view = activity.window.decorView
                if (view.width <= 0 || view.height <= 0) {
                    null
                } else {
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    ByteArrayOutputStream()
                        .also { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
                        .toByteArray()
                }
            }
                .getOrNull()
        }
        val png =
            if (Looper.myLooper() == Looper.getMainLooper()) {
                draw()
            } else {
                val latch = CountDownLatch(1)
                var taken: ByteArray? = null
                activity.runOnUiThread {
                    taken = draw()
                    latch.countDown()
                }
                latch.await(PICTURE_WAIT_SECONDS, TimeUnit.SECONDS)
                taken
            }
        png?.let { result += "ekran.png" to it }
        return result
    }

    fun header(): String = "MT_SW ${org.meshtastic.core.model.MtswRelease.displayName(
        BuildConfig.VERSION_NAME,
    )} (${BuildConfig.VERSION_NAME}, ${BuildConfig.VERSION_CODE}), Android ${Build.VERSION.RELEASE} " +
        "(API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}, ABI ${Build.SUPPORTED_ABIS.firstOrNull()}"

    private fun sampleExtras(app: Application): String {
        val parts = mutableListOf<String>()
        runCatching {
            val battery = app.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            parts += "battery ${battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
        }
        runCatching {
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            parts += "system free ${info.availMem / MB} MB${if (info.lowMemory) " LOW" else ""}"
        }
        return parts.joinToString(", ")
    }

    private fun registerCallbacks(app: Application, control: AndroidLogControl, scope: CoroutineScope) {
        app.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                private fun log(activity: Activity, what: String) =
                    Logger.d(tag = TAG) { "${activity.javaClass.simpleName} $what" }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) =
                    log(activity, "created (restored=${savedInstanceState != null})")

                override fun onActivityStarted(activity: Activity) = log(activity, "started")

                override fun onActivityResumed(activity: Activity) {
                    resumedActivity = WeakReference(activity)
                    log(activity, "resumed")
                }

                override fun onActivityPaused(activity: Activity) = log(activity, "paused")

                override fun onActivityStopped(activity: Activity) = log(activity, "stopped")

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) =
                    log(activity, "saving state")

                override fun onActivityDestroyed(activity: Activity) =
                    log(activity, "destroyed (finishing=${activity.isFinishing})")
            },
        )
        app.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onTrimMemory(level: Int) {
                    Logger.w(tag = TAG) { "Trim memory, level $level" }
                }

                override fun onConfigurationChanged(newConfig: Configuration) {
                    Logger.d(tag = TAG) {
                        "Configuration changed: orientation ${newConfig.orientation}, " +
                            "${newConfig.screenWidthDp}x${newConfig.screenHeightDp} dp, night ${newConfig.uiMode}"
                    }
                }

                @Deprecated("Replaced by onTrimMemory")
                override fun onLowMemory() {
                    Logger.w(tag = TAG) { "Low memory" }
                }
            },
        )
        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(
                LifecycleEventObserver { _, event ->
                    Logger.i(tag = TAG) { "App process: $event" }
                    if (event == Lifecycle.Event.ON_STOP) scope.launch { control.autoExport() }
                },
            )
    }

    /** Why Android ended the previous runs of this app (killed for memory, crash, user swipe...). */
    private fun logPreviousExits(app: Application) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        runCatching {
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getHistoricalProcessExitReasons(null, 0, EXIT_REASONS_SHOWN).forEach { exit ->
                Logger.i(tag = TAG) {
                    "Earlier exit: reason ${exit.reason}, status ${exit.status}, importance ${exit.importance}, " +
                        "pss ${exit.pss} KB, rss ${exit.rss} KB, at ${Date(exit.timestamp)}, ${exit.description}"
                }
            }
        }
    }

    @Suppress("TooManyFunctions")
    private class AndroidLogControl(
        private val app: Application,
        private val dir: File,
        private val scope: CoroutineScope,
    ) : AppLogControl {
        override val level: StateFlow<AppLogLevel>
            get() = MtSwLogging.level

        override fun setLevel(level: AppLogLevel) = MtSwLogging.setLevel(level)

        override val folderHint: String
            get() = if (appFolderFile.exists()) APP_FOLDER else "Pobrane/$FOLDER"

        // The folder the person chose for the logs (a tree address of the system's folder dialog), kept in a small
        // file.
        private val folderFile = File(dir, "log_folder.txt")
        private val appFolderFile = File(dir, "log_folder_app.txt")
        private val folderName = MutableStateFlow<String?>(null)

        init {
            folderName.value = if (appFolderFile.exists()) APP_FOLDER else readFolder()?.let { nameOf(it) }
        }

        override val canChooseFolder: Boolean = true

        override val chosenFolderName: StateFlow<String?>
            get() = folderName

        private fun readFolder(): Uri? = runCatching {
            Uri.parse(folderFile.readText().trim())
        }
            .getOrNull()
            ?.takeIf {
                it.toString().isNotBlank()
            }

        private fun nameOf(tree: Uri): String? = runCatching {
            val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            app.contentResolver
                .query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }
            .getOrNull() ?: tree.lastPathSegment

        override fun onFolderPicked(uri: String) {
            val tree = runCatching { Uri.parse(uri) }.getOrNull() ?: return
            runCatching { folderFile.writeText(uri) }
                .onFailure { Logger.w(it, tag = TAG) { "The log folder choice could not be kept" } }
            runCatching { appFolderFile.delete() }
            folderName.value = nameOf(tree)
            Logger.i(tag = TAG) { "The log folder was chosen (${folderName.value})" }
            // Fresh zips go to the new folder at once.
            for (marker in listOf(FINGERPRINT_24H, FINGERPRINT_FULL)) runCatching { File(dir, marker).delete() }
            scope.launch(Dispatchers.IO) { autoExport() }
        }

        override fun useAppFolder() {
            runCatching {
                dir.mkdirs()
                folderFile.delete()
                appFolderFile.writeText(APP_FOLDER)
            }
                .onFailure { Logger.w(it, tag = TAG) { "The app folder choice could not be kept" } }
            folderName.value = APP_FOLDER
            Logger.i(tag = TAG) { "The app's own folder was chosen for the logs" }
            for (marker in listOf(FINGERPRINT_24H, FINGERPRINT_FULL)) runCatching { File(dir, marker).delete() }
            scope.launch(Dispatchers.IO) { autoExport() }
        }

        override fun openFolder(): Boolean = false

        override val recent: StateFlow<Boolean>
            get() = MtSwLogging.recent

        override fun setRecent(on: Boolean) = MtSwLogging.setRecent(on)

        override val canMakeReport: Boolean = true

        override val sharesFileToApp: Boolean = true

        override suspend fun buildReport(
            note: String,
            withLogs: Boolean,
            extras: List<Pair<String, ByteArray>>,
        ): ReportFile? = withContext(Dispatchers.IO) {
            runCatching {
                val now = System.currentTimeMillis()
                val folder = File(app.cacheDir, "reports").also { it.mkdirs() }
                folder
                    .listFiles()
                    ?.filter { it.lastModified() < now - MtSwLogging.RECENT_WINDOW_MS }
                    ?.forEach { runCatching { it.delete() } }
                val name =
                    "mt_sw_zgloszenie_" +
                        java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US).format(Date(now)) +
                        ".zip"
                val file = File(folder, name)
                // A report always carries all the logs: the last 24 hours, the full log (up to 7 days) and the
                // crash records.
                val since = 0L
                val engineLogs =
                    File(app.filesDir, "engine-logs")
                        .listFiles { f -> f.isFile && (f.extension == "log" || f.extension == "txt") }
                        .orEmpty()
                file.outputStream().use { out ->
                    MtSwLogging.writeReportZip(
                        out,
                        since,
                        extras = listOf("opis_zgloszenia.txt" to note.toByteArray()) + extras,
                        extraFiles = engineLogs.map { "silnik/${it.name}" to it },
                    )
                }
                // A copy in Downloads too, so the file can also be attached by hand from there.
                val shown = copyToDownloads(file, name) ?: file.absolutePath
                ReportFile(file.absolutePath, shown, file.length() / 1024)
            }
                .onFailure { Logger.w(it, tag = TAG) { "Report could not be built" } }
                .getOrNull()
        }

        override fun shareReport(report: ReportFile, target: ReportTarget, text: String): Boolean {
            val uri =
                runCatching { FileProvider.getUriForFile(app, app.packageName + ".reportshare", File(report.path)) }
                    .getOrNull() ?: return false
            fun send(pkg: String?) = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, text)
                clipData = ClipData.newRawUri(null, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                pkg?.let { setPackage(it) }
            }
            // The chosen app first; when it is not installed, the system's own list of apps.
            return runCatching {
                app.startActivity(send(target.packageName))
                true
            }
                .recoverCatching {
                    app.startActivity(
                        Intent.createChooser(send(null), text.ifBlank { null }).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    true
                }
                .getOrDefault(false)
        }

        private fun copyToDownloads(file: File, name: String): String? = runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@runCatching null
            val resolver = app.contentResolver
            val uri =
                resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/")
                    },
                ) ?: return@runCatching null
            resolver.openOutputStream(uri, "wt")?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: return@runCatching null
            "Pobrane/$FOLDER/$name"
        }
            .getOrNull()

        /**
         * Refreshes the zips in Downloads when there is something new to put in them: the log of the last 24 hours (the
         * automatic one) and, apart from it, the full log (only while it is on).
         */
        suspend fun autoExport() {
            val crash = MtSwLogging.crashFiles().isNotEmpty()
            if (MtSwLogging.recent.value || MtSwLogging.level.value != AppLogLevel.OFF || crash) {
                val since = System.currentTimeMillis() - MtSwLogging.RECENT_WINDOW_MS
                exportIfNew(NAME_24H, FINGERPRINT_24H, since)
            }
            if (MtSwLogging.level.value != AppLogLevel.OFF) exportIfNew(NAME_FULL, FINGERPRINT_FULL, 0L)
        }

        private suspend fun exportIfNew(name: String, markerName: String, since: Long) {
            val files = (MtSwLogging.parts() + MtSwLogging.crashFiles()).filter { it.lastModified() >= since }
            val fingerprint =
                "${files.size}:${files.maxOfOrNull { it.lastModified() } ?: 0L}:${files.sumOf { it.length() }}"
            val marker = File(dir, markerName)
            if (runCatching { marker.readText() }.getOrNull() == fingerprint) return
            if (export(name, overwrite = true, since = since) != null) runCatching { marker.writeText(fingerprint) }
        }

        private suspend fun export(name: String, overwrite: Boolean, since: Long): String? =
            withContext(Dispatchers.IO) {
                runCatching {
                    val tree = readFolder()
                    if (appFolderFile.exists()) {
                        val file = File(app.getExternalFilesDir(null) ?: dir, name)
                        file.outputStream().use { MtSwLogging.writeReportZip(it, since) }
                        APP_FOLDER
                    } else if (tree != null) {
                        exportToTree(tree, name, since)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        exportToDownloads(name, overwrite, since)
                    } else {
                        exportLegacy(name, since)
                    }
                }
                    .onFailure { Logger.w(it, tag = TAG) { "Log export failed" } }
                    .getOrNull()
            }

        /** Writes the zip into the folder the person chose (replacing the file of the same name). */
        private fun exportToTree(tree: Uri, name: String, since: Long): String? {
            val resolver = app.contentResolver
            val treeId = DocumentsContract.getTreeDocumentId(tree)
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeId)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId)
            var existing: Uri? = null
            resolver
                .query(
                    children,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    ),
                    null,
                    null,
                    null,
                )
                ?.use { c ->
                    while (c.moveToNext()) {
                        if (c.getString(1) == name) {
                            existing = DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                            break
                        }
                    }
                }
            val target = existing ?: DocumentsContract.createDocument(resolver, parent, "application/zip", name)
            if (target == null) {
                Logger.w(tag = TAG) { "The log file could not be created in the chosen folder" }
                return null
            }
            resolver.openOutputStream(target, "wt")?.use { MtSwLogging.writeReportZip(it, since) } ?: return null
            return folderName.value ?: tree.toString()
        }

        private fun exportToDownloads(name: String, overwrite: Boolean, since: Long): String? {
            val resolver = app.contentResolver
            val relative = "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/"
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val existing =
                if (overwrite) {
                    resolver
                        .query(
                            collection,
                            arrayOf(MediaStore.Downloads._ID),
                            "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?",
                            arrayOf(name, relative),
                            null,
                        )
                        ?.use { c ->
                            if (c.moveToFirst()) ContentUris.withAppendedId(collection, c.getLong(0)) else null
                        }
                } else {
                    null
                }
            val uri =
                existing
                    ?: resolver.insert(
                        collection,
                        ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, name)
                            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                            put(MediaStore.Downloads.RELATIVE_PATH, relative)
                        },
                    )
                    ?: return null
            resolver.openOutputStream(uri, "wt")?.use { MtSwLogging.writeReportZip(it, since) } ?: return null
            return "Pobrane/$FOLDER/$name"
        }

        /**
         * Before Android 10 the Downloads folder needs a storage permission; without it the zip stays in the app's
         * folder.
         */
        @Suppress("DEPRECATION")
        private fun exportLegacy(name: String, since: Long): String? {
            val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER)
            val target = runCatching {
                publicDir.mkdirs()
                File(publicDir, name).also { it.outputStream().close() }
            }
                .getOrElse { File(app.getExternalFilesDir(null) ?: dir, name) }
            target.outputStream().use { MtSwLogging.writeReportZip(it, since) }
            return target.absolutePath
        }
    }
}
