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
package org.meshtastic.core.common.log

import co.touchlab.kermit.Severity
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MtSwLoggingTest {
    @Test
    fun `the level lowers Kermit's severity only as far as chosen`() {
        assertEquals(Severity.Info, MtSwLogging.minSeverityFor(Severity.Info, AppLogLevel.OFF))
        assertEquals(Severity.Info, MtSwLogging.minSeverityFor(Severity.Info, AppLogLevel.BASIC))
        assertEquals(Severity.Debug, MtSwLogging.minSeverityFor(Severity.Info, AppLogLevel.DETAILED))
        assertEquals(Severity.Verbose, MtSwLogging.minSeverityFor(Severity.Info, AppLogLevel.FULL))
        assertEquals(Severity.Debug, MtSwLogging.minSeverityFor(Severity.Debug, AppLogLevel.BASIC))
    }

    @Test
    fun `an unknown remembered level means off`() {
        assertEquals(AppLogLevel.OFF, AppLogLevel.fromKey(null))
        assertEquals(AppLogLevel.OFF, AppLogLevel.fromKey("nonsense"))
        assertEquals(AppLogLevel.FULL, AppLogLevel.fromKey("full\n"))
    }

    @Test
    fun `lines reach a part and the zip holds a table of contents`() {
        val dir: File = Files.createTempDirectory("mtsw-log").toFile()
        MtSwLogging.init(dir, headerText = { "test header" }) {}
        MtSwLogging.setLevel(AppLogLevel.FULL)
        MtSwLogging.writer.log(Severity.Warn, "something odd", "Test", null)
        MtSwLogging.flush()

        val parts = MtSwLogging.parts()
        assertTrue(parts.isNotEmpty())
        assertTrue(parts.first().readText().contains("something odd"))

        val bytes = ByteArrayOutputStream().also { MtSwLogging.writeZip(it) }.toByteArray()
        val names = mutableListOf<String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { names += it.name }
        }
        assertTrue("spis_tresci.txt" in names, names.toString())
        MtSwLogging.setLevel(AppLogLevel.OFF)
    }
}
