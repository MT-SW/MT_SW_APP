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
package org.meshtastic.app

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.meshtastic.core.common.log.AppLogControl
import org.meshtastic.core.common.log.AppLogControls
import org.meshtastic.core.common.log.AppLogLevel
import org.meshtastic.core.common.log.MinSeverityLogWriter
import org.meshtastic.core.common.log.MtSwLogging
import java.io.File
import java.util.Date

/**
 * The phone side of the log switch. Logs live in the app's private folder; because a crashed app cannot be asked for
 * them, a zip of them is also kept in the Downloads folder (`Pobrane/MT_SW_logi`): refreshed on its own when the app
 * starts, every few minutes while it runs, and when it goes to the background while logging is on, and on a start that follows a crash even when logging is off
 * (only the crash record is in it then).
 */
internal object AndroidLogging {
    private const val TAG = "AppLifecycle"
    private const val AUTO_NAME = "mt_sw_logi_najnowsze.zip"
    private const val FOLDER = "MT_SW_logi"
    private const val EXIT_REASONS_SHOWN = 6
    private const val FINGERPRINT_NAME = "last_export.txt"
    private const val MB = 1024 * 1024
    private const val AUTO_EXPORT_EVERY_MS = 3 * 60 * 1000L

    /** Kermit's lowest severity for this build when the file log is off. */
    val baseSeverity: Severity
        get() = if (BuildConfig.DEBUG) Severity.Debug else Severity.Info

    fun install(app: Application, scope: CoroutineScope) {
        val dir = File(app.filesDir, "logs")
        Logger.setMinSeverity(baseSeverity)
        Logger.setLogWriters(MinSeverityLogWriter(platformLogWriter(), baseSeverity), MtSwLogging.writer)
        MtSwLogging.init(dir, headerText = { header() }, sample = { sampleExtras(app) }) { level ->
            Logger.setMinSeverity(MtSwLogging.minSeverityFor(baseSeverity, level))
        }
        val control = AndroidLogControl(app, dir)
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

    fun header(): String =
        "MT_SW ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}), Android ${Build.VERSION.RELEASE} " +
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

                override fun onActivityResumed(activity: Activity) = log(activity, "resumed")

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
        ProcessLifecycleOwner.get().lifecycle.addObserver(
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

    private class AndroidLogControl(private val app: Application, private val dir: File) : AppLogControl {
        override val level: StateFlow<AppLogLevel> get() = MtSwLogging.level

        override fun setLevel(level: AppLogLevel) = MtSwLogging.setLevel(level)

        override val folderHint: String get() = "Pobrane/$FOLDER"

        override fun openFolder(): Boolean = false

        /** Refreshes the zip in Downloads when there is something new to put in it. */
        suspend fun autoExport() {
            val wanted = MtSwLogging.level.value != AppLogLevel.OFF || MtSwLogging.crashFiles().isNotEmpty()
            if (!wanted) return
            val files = MtSwLogging.parts() + MtSwLogging.crashFiles()
            val fingerprint = "${files.size}:${files.maxOfOrNull { it.lastModified() } ?: 0L}:${files.sumOf { it.length() }}"
            val marker = File(dir, FINGERPRINT_NAME)
            if (runCatching { marker.readText() }.getOrNull() == fingerprint) return
            if (export(AUTO_NAME, overwrite = true) != null) runCatching { marker.writeText(fingerprint) }
        }

        private suspend fun export(name: String, overwrite: Boolean): String? = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) exportToDownloads(name, overwrite) else exportLegacy(name)
            }
                .onFailure { Logger.w(it, tag = TAG) { "Log export failed" } }
                .getOrNull()
        }

        private fun exportToDownloads(name: String, overwrite: Boolean): String? {
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
                        ?.use { c -> if (c.moveToFirst()) ContentUris.withAppendedId(collection, c.getLong(0)) else null }
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
            resolver.openOutputStream(uri, "wt")?.use { MtSwLogging.writeZip(it) } ?: return null
            return "Pobrane/$FOLDER/$name"
        }

        /** Before Android 10 the Downloads folder needs a storage permission; without it the zip stays in the app's folder. */
        @Suppress("DEPRECATION")
        private fun exportLegacy(name: String): String? {
            val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER)
            val target =
                runCatching { publicDir.mkdirs(); File(publicDir, name).also { it.outputStream().close() } }
                    .getOrElse { File(app.getExternalFilesDir(null) ?: dir, name) }
            target.outputStream().use { MtSwLogging.writeZip(it) }
            return target.absolutePath
        }
    }
}
