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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdatePolicyTest {
    private fun release(tag: String, prerelease: Boolean = false, draft: Boolean = false) =
        ReleaseCandidate(tag, prerelease, draft, "https://example.invalid/$tag")

    @Test
    fun `parses stable and test tags`() {
        assertEquals(ReleaseVersion(3, 0), AppUpdatePolicy.parse("v3(2.8.3)"))
        assertEquals(ReleaseVersion(3, 0), AppUpdatePolicy.parse("v3_2.8.3"))
        assertEquals(ReleaseVersion(3, 1), AppUpdatePolicy.parse("v3_2.8.3.test.1"))
        assertEquals(ReleaseVersion(4, 2), AppUpdatePolicy.parse("v4(2.8.4).test.2"))
        assertEquals(ReleaseVersion(4, 1), AppUpdatePolicy.parse("v4(2.8.4).test"))
        assertEquals(ReleaseVersion(5, 1), AppUpdatePolicy.parse("v5(2.8.5)", prerelease = true))
        assertNull(AppUpdatePolicy.parse("v2.8.3-internal.7"))
    }

    @Test
    fun `stable comes after its own tests but before the next cycle`() {
        assertTrue(ReleaseVersion(4, 0) > ReleaseVersion(4, 9))
        assertTrue(ReleaseVersion(4, 2) > ReleaseVersion(4, 1))
        assertTrue(ReleaseVersion(5, 1) > ReleaseVersion(4, 0))
    }

    @Test
    fun `stable user is told about a newer stable only`() {
        val releases =
            listOf(release("v3(2.8.3)"), release("v4(2.8.4).test.1", prerelease = true), release("v4(2.8.4)"))
        assertEquals("v4(2.8.4)", AppUpdatePolicy.newestUpdate(ReleaseVersion(3, 0), releases, false)?.tag)
        assertNull(
            AppUpdatePolicy.newestUpdate(
                ReleaseVersion(3, 0),
                listOf(release("v4(2.8.4).test.1", prerelease = true)),
                false,
            ),
        )
    }

    @Test
    fun `tests are offered only when asked for`() {
        val releases = listOf(release("v3(2.8.3).test.3", prerelease = true), release("v3(2.8.3).test.2", true))
        assertNull(AppUpdatePolicy.newestUpdate(ReleaseVersion(3, 2), releases, false))
        assertEquals("v3(2.8.3).test.3", AppUpdatePolicy.newestUpdate(ReleaseVersion(3, 2), releases, true)?.tag)
    }

    @Test
    fun `a test build is told about the stable release of its own cycle`() {
        val releases = listOf(release("v3(2.8.3)"))
        assertEquals("v3(2.8.3)", AppUpdatePolicy.newestUpdate(ReleaseVersion(3, 2), releases, false)?.tag)
    }

    @Test
    fun `drafts and foreign tags are ignored`() {
        val releases = listOf(release("v9(9.9.9)", draft = true), release("v2.9.0"))
        assertNull(AppUpdatePolicy.newestUpdate(ReleaseVersion(3, 0), releases, true))
    }

    @Test
    fun `new release notifies at once and a known one only after two days`() {
        val update = release("v4(2.8.4)")
        val day = 24L * 60 * 60 * 1000
        assertTrue(AppUpdatePolicy.shouldNotify(update, "", 0, 10 * day))
        assertTrue(AppUpdatePolicy.shouldNotify(update, "v3(2.8.3)", 9 * day, 10 * day))
        assertFalse(AppUpdatePolicy.shouldNotify(update, "v4(2.8.4)", 9 * day, 10 * day))
        assertTrue(AppUpdatePolicy.shouldNotify(update, "v4(2.8.4)", 8 * day, 10 * day))
    }
}
