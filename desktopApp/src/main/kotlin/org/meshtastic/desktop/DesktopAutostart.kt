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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.common.background.AutostartControl
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Start at login on the three desktop systems: a LaunchAgent on macOS, the `Run` registry key on Windows and an
 * autostart `.desktop` file on Linux. Only the installed app can register itself: a run from the IDE or Gradle has no
 * launcher of its own, so [create] returns null and the switch is not shown.
 */
internal class DesktopAutostart private constructor(private val launcher: String) : AutostartControl {
    private val os = System.getProperty("os.name").orEmpty().lowercase()
    private val state = MutableStateFlow(isRegistered())
    override val enabled: StateFlow<Boolean> = state

    override fun setEnabled(on: Boolean) {
        runCatching { if (on) register() else unregister() }
        state.value = isRegistered()
    }

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
            os.contains("win") -> run("reg", "add", RUN_KEY, "/v", NAME, "/t", "REG_SZ", "/d", "\"$launcher\"", "/f")

            os.contains("mac") ->
                macPlist().apply {
                    parentFile?.mkdirs()
                    writeText(
                        """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key><string>$LABEL</string>
    <key>ProgramArguments</key><array><string>${escapeXml(launcher)}</string></array>
    <key>RunAtLoad</key><true/>
</dict>
</plist>
""",
                    )
                }

            else ->
                linuxEntry().apply {
                    parentFile?.mkdirs()
                    writeText(
                        "[Desktop Entry]\nType=Application\nName=MT_SW\nExec=\"$launcher\"\n" +
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

    private fun run(vararg command: String): Int {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        process.inputStream.readBytes()
        if (!process.waitFor(WAIT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return -1
        }
        return process.exitValue()
    }

    private fun escapeXml(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    companion object {
        private const val NAME = "MT_SW"
        private const val LABEL = "pl.sw.mt.autostart"
        private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        private const val WAIT_SECONDS = 10L

        /** The control for the installed app, or null when the app was not started from its own launcher. */
        fun create(): DesktopAutostart? {
            val command = ProcessHandle.current().info().command().orElse(null) ?: return null
            val name = File(command).name.lowercase().removeSuffix(".exe")
            return if (name == "java" || name == "javaw") null else DesktopAutostart(command)
        }
    }
}
