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

import org.meshtastic.core.common.AppDataRoot
import java.awt.BorderLayout
import java.awt.GraphicsEnvironment
import java.util.Locale
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/**
 * First thing at every start, before the logs, the database or the settings are opened: if the person asked in the
 * settings to move the data folder, the copy is made now (nothing is open yet, so every file is safe to copy) with a
 * small window that shows the progress. Without a requested move this only settles the folder.
 */
internal fun prepareDataFolder() {
    if (!AppDataRoot.hasPendingMove() || GraphicsEnvironment.isHeadless()) {
        AppDataRoot.prepare()
        return
    }
    val polish = Locale.getDefault().language == "pl"
    var frame: JFrame? = null
    var bar: JProgressBar? = null
    runCatching {
        SwingUtilities.invokeAndWait {
            val progress = JProgressBar(0, PERCENT).apply { isStringPainted = true }
            val window =
                JFrame("MT_SW").apply {
                    defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
                    val label =
                        JLabel(
                            if (polish) {
                                "Przenoszenie danych aplikacji do nowego folderu. Nie zamykaj aplikacji."
                            } else {
                                "Moving the app's data to the new folder. Do not close the app."
                            },
                        )
                    val panel = javax.swing.JPanel(BorderLayout(0, 12))
                    panel.border = BorderFactory.createEmptyBorder(20, 24, 20, 24)
                    panel.add(label, BorderLayout.NORTH)
                    panel.add(progress, BorderLayout.CENTER)
                    contentPane = panel
                    setSize(WIDTH, HEIGHT)
                    setLocationRelativeTo(null)
                    isVisible = true
                }
            frame = window
            bar = progress
        }
    }
    try {
        AppDataRoot.prepare { fraction ->
            bar?.let { b -> SwingUtilities.invokeLater { b.value = (fraction * PERCENT).toInt() } }
        }
    } finally {
        frame?.let { f -> SwingUtilities.invokeLater { f.dispose() } }
    }
}

private const val PERCENT = 100
private const val WIDTH = 460
private const val HEIGHT = 140
