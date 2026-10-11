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

/** A published GitHub release as the update check sees it. */
data class ReleaseCandidate(val tag: String, val prerelease: Boolean, val draft: Boolean, val url: String)

/**
 * The position of an MT_SW_APP release in the release order: [release] is the V number from the tag (`v4(2.8.4)`), and
 * [test] is the `.test.N` number, or 0 for the stable release.
 */
data class ReleaseVersion(val release: Int, val test: Int) : Comparable<ReleaseVersion> {
    val isTest: Boolean
        get() = test != 0

    // A stable release comes after all of its own test releases.
    private val order: Int
        get() = if (test == 0) Int.MAX_VALUE else test

    override fun compareTo(other: ReleaseVersion): Int =
        compareValuesBy(this, other, { it.release }, { it.order })
}

/** Decides whether a newer MT_SW_APP release than the running one is published, and when to remind about it. */
object AppUpdatePolicy {
    /** How long to wait before reminding about a release that was already announced. */
    const val REMINDER_INTERVAL_MILLIS: Long = 2L * 24 * 60 * 60 * 1000

    private val RELEASE_REGEX = Regex("""^v(\d+)\(""", RegexOption.IGNORE_CASE)
    private val TEST_REGEX = Regex("""\b(?:test|internal)\.?(\d+)?""", RegexOption.IGNORE_CASE)

    /** Reads the position out of a tag like `v3(2.8.3).test.2`; null when the tag is not an MT_SW_APP release tag. */
    fun parse(tag: String, prerelease: Boolean = false): ReleaseVersion? {
        val release = RELEASE_REGEX.find(tag.trim())?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val marker = TEST_REGEX.find(tag)
        val test =
            when {
                marker != null -> marker.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
                prerelease -> 1
                else -> 0
            }
        return ReleaseVersion(release, test)
    }

    /**
     * The newest published release that is newer than [current]; test releases only count when [includeTests] is set.
     */
    fun newestUpdate(
        current: ReleaseVersion,
        releases: List<ReleaseCandidate>,
        includeTests: Boolean,
    ): ReleaseCandidate? = releases
        .filter { !it.draft }
        .mapNotNull { candidate -> parse(candidate.tag, candidate.prerelease)?.let { candidate to it } }
        .filter { (_, version) -> (includeTests || !version.isTest) && version > current }
        .maxByOrNull { (_, version) -> version }
        ?.first

    /** True when [update] is a release not announced yet, or one announced more than the reminder interval ago. */
    fun shouldNotify(
        update: ReleaseCandidate,
        lastNotifiedTag: String,
        lastNotifiedMillis: Long,
        nowMillis: Long,
    ): Boolean = update.tag != lastNotifiedTag || nowMillis - lastNotifiedMillis >= REMINDER_INTERVAL_MILLIS
}
