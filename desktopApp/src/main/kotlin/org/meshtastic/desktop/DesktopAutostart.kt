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
package org.meshtastic.desktop

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.common.background.AutostartControl
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Start at login on the three desktop systems: a LaunchAgent on macOS, the `Run` registry key on Windows and an
 * autostart `.desktop` file on Linux. Only the installed app can register itself: a run from the IDE or Gradle has no
 * launcher of its own, so [create] returns null and the switch is not shown.
 *
 * Every step is written to the log (exit code and output of the system tool), because a start at login that silently
 * does nothing cannot be diagnosed otherwise. A registration that is already there is written again at every start of
 * the app, so it follows the app when it is reinstalled in another folder.
 */
internal class DesktopAutostart private constructor(private val launcher: String) : AutostartControl {
    private val os = System.getProperty("os.name").orEmpty().lowercase()
    private val state = MutableStateFlow(isRegistered())
    override val enabled: StateFlow<Boolean> = state

    // The wish is kept in the registration itself (the launch command carries MINIMIZED_ARG), so the app needs no
    // settings to be read before its window opens.
    private val minimized = MutableStateFlow(readMinimized())
    override val startMinimized: StateFlow<Boolean> = minimized

    override fun setStartMinimized(on: Boolean) {
        minimized.value = on
        if (state.value) setEnabled(true)
        Logger.i { "Autostart: startMinimized=$on" }
    }

    init {
        // A registration made by an older install may point at a folder that no longer exists: write it again.
        if (state.value) setEnabled(true)
    }

    override fun setEnabled(on: Boolean) {
        runCatching { if (on) register() else unregister() }
            .onFailure { Logger.w(it) { "Autostart: ${if (on) "register" else "unregister"} failed" } }
        state.value = isRegistered()
        Logger.i { "Autostart: wanted=$on registered=${state.value} launcher=$launcher" }
    }

    private fun readMinimized(): Boolean = runCatching {
        when {
            os.contains("win") -> runOutput("reg", "query", RUN_KEY, "/v", NAME).second.contains(MINIMIZED_ARG)
            os.contains("mac") -> macPlist().takeIf { it.exists() }?.readText()?.contains(MINIMIZED_ARG) == true
            else -> linuxEntry().takeIf { it.exists() }?.readText()?.contains(MINIMIZED_ARG) == true
        }
    }
        .getOrDefault(false)

    private fun isRegistered(): Boolean = runCatching {
        when {
            os.contains("win") -> run("reg", "query", RUN_KEY, "/v", NAME) == 0
            os.contains("mac") -> macPlist().exists()
            else -> linuxEntry().exists()
        }
    }
        .getOrDefault(false)

    private fun register() {
        when {
            os.contains("win") -> {
                // The command is written with its own escaped quotes, which Java wraps into one argument.
                val command = if (minimized.value) "\\\"$launcher\\\" $MINIMIZED_ARG" else "\"$launcher\""
                val code = run("reg", "add", RUN_KEY, "/v", NAME, "/t", "REG_SZ", "/d", command, "/f")
                // Windows keeps a separate "disabled in Startup apps" mark per entry. Once the person (or the system)
                // has turned the app off there, the Run entry is ignored for good, so the mark is removed here.
                run("reg", "delete", APPROVED_KEY, "/v", NAME, "/f")
                if (code != 0) Logger.w { "Autostart: reg add ended with $code" }
            }

            os.contains("mac") ->
                macPlist().apply {
                    parentFile?.mkdirs()
                    val macArguments = if (minimized.value) "<string>$MINIMIZED_ARG</string>" else ""
                    writeText(
                        """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key><string>$LABEL</string>
    <key>ProgramArguments</key><array><string>${escapeXml(launcher)}</string>$macArguments</array>
    <key>RunAtLoad</key><true/>
</dict>
</plist>
""",
                    )
                }

            else ->
                linuxEntry().apply {
                    parentFile?.mkdirs()
                    val linuxArguments = if (minimized.value) " $MINIMIZED_ARG" else ""
                    writeText(
                        "[Desktop Entry]\nType=Application\nName=MT_SW\nExec=\"$launcher\"$linuxArguments\n" +
                            "X-GNOME-Autostart-enabled=true\n",
                    )
                }
        }
    }

    private fun unregister() {
        when {
            os.contains("win") -> run("reg", "delete", RUN_KEY, "/v", NAME, "/f")
            os.contains("mac") -> macPlist().delete()
            else -> linuxEntry().delete()
        }
    }

    private fun macPlist() = File(System.getProperty("user.home"), "Library/LaunchAgents/$LABEL.plist")

    private fun linuxEntry(): File {
        val base =
            System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() } ?: "${System.getProperty("user.home")}/.config"
        return File(base, "autostart/mt_sw.desktop")
    }

    private fun run(vararg command: String): Int = runOutput(*command).first

    private fun runOutput(vararg command: String): Pair<Int, String> {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.readBytes().decodeToString().trim()
        if (!process.waitFor(WAIT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            Logger.w { "Autostart: ${command.first()} did not finish in $WAIT_SECONDS s" }
            return -1 to output
        }
        val code = process.exitValue()
        if (code != 0) Logger.d { "Autostart: ${command.joinToString(" ")} -> $code $output" }
        return code to output
    }

    private fun escapeXml(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    companion object {
        /** Added to the launch command when the start at login should wait in the tray. */
        const val MINIMIZED_ARG = "--minimized"
        private const val NAME = "MT_SW"
        private const val LABEL = "pl.sw.mt.autostart"
        private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        private const val APPROVED_KEY =
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run"
        private const val WAIT_SECONDS = 10L

        /**
         * The file that starts the installed app: the AppImage itself on Linux (its running copy lives in a temporary
         * folder that is gone after a restart), else the launcher the packager reports, else the running process.
         */
        private fun launcherPath(): String? {
            val appImage = System.getenv("APPIMAGE")?.takeIf { it.isNotBlank() && File(it).exists() }
            val packaged = System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() && File(it).exists() }
            val running = ProcessHandle.current().info().command().orElse(null)
            return appImage ?: packaged ?: running
        }

        /** The control for the installed app, or null when the app was not started from its own launcher. */
        fun create(): DesktopAutostart? {
            val command = launcherPath() ?: return null
            val name = File(command).name.lowercase().removeSuffix(".exe")
            if (name == "java" || name == "javaw") {
                Logger.i { "Autostart: not shown, the app was not started from its own launcher ($command)" }
                return null
            }
            return DesktopAutostart(command)
        }
    }
}
