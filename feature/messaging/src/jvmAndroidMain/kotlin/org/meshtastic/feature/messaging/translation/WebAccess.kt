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
package org.meshtastic.feature.messaging.translation

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * The only door through which the assistant reaches the internet, used for web search, reading a page and for keeping
 * the knowledge about the app up to date. Only https, never to this computer or the local network, small answers, no
 * cookies and nothing about the user in the request except the text of the query itself.
 */
internal object WebAccess {
    private const val MAX_BYTES = 400_000
    private const val MAX_REDIRECTS = 3
    private const val CONNECT_MS = 8_000
    private const val READ_MS = 12_000
    private const val USER_AGENT = "MT_SW_APP assistant (offline-first; text only)"

    /** Downloads [url] as text. Throws when the address is not allowed or the server does not answer properly. */
    fun fetch(url: String, accept: String = "text/html,application/json,text/plain"): String {
        var current = url
        repeat(MAX_REDIRECTS + 1) {
            checkAllowed(current)
            val connection = URL(current).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = CONNECT_MS
                connection.readTimeout = READ_MS
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", accept)
                val code = connection.responseCode
                if (code in 300..399) {
                    val next = connection.getHeaderField("Location") ?: error("Redirect without a target")
                    current = URI(current).resolve(next).toString()
                    return@repeat
                }
                if (code !in 200..299) error("HTTP $code")
                val out = ByteArrayOutputStream()
                connection.inputStream.use { stream ->
                    val buffer = ByteArray(8192)
                    while (out.size() < MAX_BYTES) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                    }
                }
                return out.toString(Charsets.UTF_8.name())
            } finally {
                connection.disconnect()
            }
        }
        error("Too many redirects")
    }

    /** Raw bytes of [url] for checks that need the exact content (same rules as [fetch]). */
    fun fetchBytes(url: String): ByteArray {
        checkAllowed(url)
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_MS
            connection.readTimeout = READ_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val out = ByteArrayOutputStream()
            connection.inputStream.use { stream ->
                val buffer = ByteArray(8192)
                while (out.size() <= MAX_BYTES) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                }
            }
            check(out.size() <= MAX_BYTES) { "File too large" }
            return out.toByteArray()
        } finally {
            connection.disconnect()
        }
    }

    fun checkAllowed(url: String) {
        val uri = URI(url)
        require(uri.scheme.equals("https", ignoreCase = true)) { "Only https addresses are allowed" }
        val host = uri.host ?: error("No host")
        require(!host.equals("localhost", ignoreCase = true) && !host.endsWith(".local", ignoreCase = true)) {
            "Local addresses are not allowed"
        }
        for (address in InetAddress.getAllByName(host)) {
            require(
                !address.isLoopbackAddress &&
                    !address.isSiteLocalAddress &&
                    !address.isLinkLocalAddress &&
                    !address.isAnyLocalAddress &&
                    !address.isMulticastAddress,
            ) {
                "Local addresses are not allowed"
            }
        }
    }

    /** Plain readable text of an HTML page, without scripts, styles and tags. */
    fun htmlToText(html: String): String =
        html
            .replace(Regex("(?is)<(script|style|noscript|svg|head)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?i)<br\\s*/?>|</p>|</div>|</li>|</h[1-6]>|</tr>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .lines()
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    /** Search results of DuckDuckGo's plain HTML page as `title - address` and a short snippet. */
    fun search(query: String, limit: Int): List<String> {
        val page = fetch("https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(query, Charsets.UTF_8.name()))
        val links = Regex("""<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).findAll(page).toList()
        val snippets = Regex("""<a[^>]*class="result__snippet"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).findAll(page).toList()
        return links.take(limit).mapIndexed { index, match ->
            val target = realAddress(match.groupValues[1])
            val title = htmlToText(match.groupValues[2])
            val snippet = snippets.getOrNull(index)?.groupValues?.get(1)?.let { htmlToText(it) }.orEmpty()
            "$title - $target\n$snippet"
        }
    }

    /** DuckDuckGo wraps every result address in a redirect; this takes the real address out. */
    private fun realAddress(href: String): String {
        val wrapped = Regex("[?&]uddg=([^&]+)").find(href)?.groupValues?.get(1)
        val decoded = wrapped?.let { URLDecoder.decode(it, Charsets.UTF_8.name()) } ?: href
        return if (decoded.startsWith("//")) "https:$decoded" else decoded
    }

    fun hostOf(url: String): String = runCatching { URI(url).host.orEmpty() }.getOrDefault("")
}
