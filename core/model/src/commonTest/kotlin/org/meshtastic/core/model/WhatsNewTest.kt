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
package org.meshtastic.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WhatsNewTest {
    private val text =
        """
        # 5 | Version 5
        ## new | New
        - **A.** one
        - two
        ## fix | Fixed
        - three
        # 0 | Before
        ## new | New
        - old
        ## orig | Empty
        """
            .trimIndent()

    @Test
    fun `parse reads releases groups and points`() {
        val releases = WhatsNew.parse(text)
        assertEquals(listOf(5, 0), releases.map { it.order })
        assertEquals(listOf("new", "fix"), releases[0].categories.map { it.kind })
        assertEquals(listOf("**A.** one", "two"), releases[0].categories[0].items)
        assertEquals(1, releases[1].categories.size)
    }

    @Test
    fun `a new installation gets everything`() {
        val request = WhatsNew.plan(firstRun = true, seenBuild = "", seenStable = 0, release = 5, test = 1)
        val shown = WhatsNew.select(WhatsNew.parse(text), request!!, release = 5)
        assertEquals(listOf(5, 0), shown.map { it.order })
    }

    @Test
    fun `nothing is decided before the stored build is read or when it is this build`() {
        assertNull(WhatsNew.plan(false, null, 0, 5, 1))
        assertNull(WhatsNew.plan(false, "5.1", 0, 5, 1))
    }

    @Test
    fun `a test build shows the release in progress`() {
        val request = WhatsNew.plan(false, "5.0", 4, 5, 2)!!
        assertEquals(WhatsNewRequest(full = false, fromRelease = 4), request)
        assertEquals(listOf(5), WhatsNew.select(WhatsNew.parse(text), request, release = 5).map { it.order })
    }

    @Test
    fun `a stable build shows everything since the last stable release seen`() {
        assertEquals(3, WhatsNew.plan(false, "4.0", 3, 5, 0)!!.fromRelease)
        assertEquals(4, WhatsNew.plan(false, "", 0, 5, 0)!!.fromRelease)
        assertEquals(4, WhatsNew.plan(false, "4.0", 5, 5, 0)!!.fromRelease)
    }
}
