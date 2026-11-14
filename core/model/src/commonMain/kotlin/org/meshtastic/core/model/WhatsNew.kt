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

/** One group of the list ("new features", "fixed bugs"...). [kind] is `new`, `better`, `fix` or `orig`. */
data class WhatsNewCategory(val kind: String, val title: String, val items: List<String>)

/** What one release brought. [order] is the release number (the V in V5); 0 is "everything before the first list". */
data class WhatsNewRelease(val order: Int, val title: String, val categories: List<WhatsNewCategory>)

/** What to show: the whole history, or only the releases after [fromRelease]. */
data class WhatsNewRequest(val full: Boolean, val fromRelease: Int)

/**
 * The "what's new" list is a small text file in each language:
 * ```
 * # 5 | Wersja v5(2.8.5)
 * ## new | Nowe funkcje
 * - **Title.** The text of one point.
 * ```
 *
 * A `#` line starts a release (its number, then its title), a `##` line a group in it (its kind, then its title) and a
 * `-` line one point. Anything else is ignored.
 */
object WhatsNew {
    private const val SEPARATOR = '|'

    fun parse(text: String): List<WhatsNewRelease> {
        val releases = mutableListOf<WhatsNewRelease>()
        var order = -1
        var title = ""
        var categories = mutableListOf<WhatsNewCategory>()
        var kind = ""
        var categoryTitle = ""
        var items = mutableListOf<String>()

        fun closeCategory() {
            if (kind.isNotEmpty() && items.isNotEmpty()) categories += WhatsNewCategory(kind, categoryTitle, items)
            kind = ""
            items = mutableListOf()
        }

        fun closeRelease() {
            closeCategory()
            if (order >= 0 && categories.isNotEmpty()) releases += WhatsNewRelease(order, title, categories)
            order = -1
            categories = mutableListOf()
        }
        for (raw in text.lines()) {
            val line = raw.trim()
            when {
                line.startsWith("## ") -> {
                    closeCategory()
                    val (k, t) = split(line.removePrefix("## "))
                    kind = k
                    categoryTitle = t
                }

                line.startsWith("# ") -> {
                    closeRelease()
                    val (o, t) = split(line.removePrefix("# "))
                    order = o.toIntOrNull() ?: -1
                    title = t
                }

                line.startsWith("- ") && kind.isNotEmpty() -> items += line.removePrefix("- ").trim()
            }
        }
        closeRelease()
        return releases
    }

    private fun split(text: String): Pair<String, String> {
        val at = text.indexOf(SEPARATOR)
        return if (at < 0) text.trim() to text.trim() else text.take(at).trim() to text.drop(at + 1).trim()
    }

    /** The name of one build: `5.1` for test 1 of release 5, `5.0` for the stable release. */
    fun buildKey(release: Int, test: Int): String = "$release.$test"

    /**
     * What to show now, or null for nothing. A new installation ([firstRun]) gets everything. After an update the
     * person gets what changed since the last stable release: for a test build that is the release before it, for a
     * stable build the last stable release they saw ([seenStable]; the one before it when none was seen yet). A null
     * [seenBuild] means the stored value is not read yet, so nothing is decided.
     */
    fun plan(firstRun: Boolean, seenBuild: String?, seenStable: Int, release: Int, test: Int): WhatsNewRequest? = when {
        firstRun -> WhatsNewRequest(full = true, fromRelease = 0)
        seenBuild == null || seenBuild == buildKey(release, test) -> null
        test > 0 -> WhatsNewRequest(full = false, fromRelease = release - 1)
        seenStable in 1 until release -> WhatsNewRequest(full = false, fromRelease = seenStable)
        else -> WhatsNewRequest(full = false, fromRelease = release - 1)
    }

    /** The releases of the request, the newest first; [release] is the release of this build. */
    fun select(releases: List<WhatsNewRelease>, request: WhatsNewRequest, release: Int): List<WhatsNewRelease> =
        releases
            .filter { request.full || (it.order > request.fromRelease && it.order <= release) }
            .sortedByDescending { it.order }
}
