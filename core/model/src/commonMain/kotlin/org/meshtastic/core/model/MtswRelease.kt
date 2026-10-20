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

/**
 * The MT_SW_APP release this build belongs to. Edit these two numbers by hand together with the release you publish on
 * GitHub (the tag looks like `v3(2.8.3).test.2` or `v4(2.8.4)`):
 * - [RELEASE] is the number before the parenthesis (the V in V3), raised with every stable release;
 * - [TEST] is the test number (`.test.N`), or 0 for the stable release itself.
 *
 * Code changes between releases leave them alone: the update notification only compares them with the releases that are
 * actually published on GitHub.
 */
object MtswRelease {
    const val RELEASE = 4
    const val TEST = 1
}
