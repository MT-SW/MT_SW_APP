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
@file:Suppress("MagicNumber", "ReturnCount")

package org.meshtastic.feature.messaging.translation


/**
 * The only door through which the assistant reaches the internet, used for web search, reading a page and for keeping
 * the knowledge about the app up to date. Only https, never to this computer or the local network, small answers, no
 * cookies and nothing about the user in the request except the text of the query itself.
 */
/** The platform's web client: Java's HTTP on Android and the computer, Ktor on iPhone and iPad. */
internal expect fun platformWebTransport(): WebAccess.Transport

internal object WebAccess {
    private const val MAX_BYTES = 400_000
    private const val MAX_REDIRECTS = 3
    private const val CONNECT_MS = 8_000
    private const val READ_MS = 12_000
    private const val USER_AGENT = "MT_SW_APP assistant (offline-first; text only)"
    private val QUESTION_WORDS =
        setOf(
            "jaka",
            "jaki",
            "jakie",
            "jest",
            "co",
            "to",
            "kto",
            "czy",
            "jak",
            "ile",
            "dlaczego",
            "gdzie",
            "kiedy",
            "what",
            "is",
            "the",
            "who",
            "how",
            "are",
            "do",
            "does",
            "of",
            "a",
            "an",
            "why",
            "where",
            "when",
            "powiedz",
            "opowiedz",
            "wyjaśnij",
            "podaj",
        )
    private const val BROWSER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0"

    /** The answer of one request without redirects: [location] is set for a 3xx answer. */
    class Response(val code: Int, val location: String?, val body: ByteArray)

    /**
     * The platform part: one plain GET, no redirects, at most [maxBytes] of the body, with the given timeouts.
     * [resolvesToLocalNetwork] says whether the host name points to this device or the local network (null = unknown).
     */
    interface Transport {
        fun get(url: String, accept: String, agent: String, maxBytes: Int): Response

        fun resolvesToLocalNetwork(host: String): Boolean = false
    }

    private val transport: Transport by lazy { platformWebTransport() }

    /** Downloads [url] as text. Throws when the address is not allowed or the server does not answer properly. */
    fun fetch(
        url: String,
        accept: String = "text/html,application/json,text/plain",
        agent: String = USER_AGENT,
    ): String {
        var current = url
        repeat(MAX_REDIRECTS + 1) {
            checkAllowed(current)
            val response = transport.get(current, accept, agent, MAX_BYTES)
            if (response.code in 300..399) {
                current = resolve(current, response.location ?: error("Redirect without a target"))
                return@repeat
            }
            if (response.code !in 200..299) error("HTTP ${response.code}")
            return response.body.decodeToString()
        }
        error("Too many redirects")
    }

    /** Raw bytes of [url] for checks that need the exact content (same rules as [fetch]). */
    fun fetchBytes(url: String): ByteArray {
        checkAllowed(url)
        val response = transport.get(url, "*/*", USER_AGENT, MAX_BYTES + 1)
        if (response.code !in 200..299) error("HTTP ${response.code}")
        check(response.body.size <= MAX_BYTES) { "File too large" }
        return response.body
    }

    fun checkAllowed(url: String) {
        require(url.startsWith("https://", ignoreCase = true)) { "Only https addresses are allowed" }
        val host = hostOf(url).ifEmpty { error("No host") }
        require(!host.equals("localhost", ignoreCase = true) && !host.endsWith(".local", ignoreCase = true)) {
            "Local addresses are not allowed"
        }
        require(!isLocalAddress(host) && !transport.resolvesToLocalNetwork(host)) { "Local addresses are not allowed" }
    }

    /** True for an address written as numbers that points to this device or the local network. */
    private fun isLocalAddress(host: String): Boolean {
        if (host.contains(':')) {
            val h = host.trim('[', ']').lowercase()
            return h == "::1" || h == "::" || h.startsWith("fe80") || h.startsWith("fc") || h.startsWith("fd")
        }
        val parts = host.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        val (a, b) = parts
        return a == 0 || a == 10 || a == 127 || (a == 169 && b == 254) || (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) || a >= 224
    }

    private fun resolve(base: String, next: String): String = when {
        next.startsWith("https://", ignoreCase = true) || next.startsWith("http://", ignoreCase = true) -> next
        next.startsWith("//") -> "https:$next"
        next.startsWith("/") -> "https://" + hostPartOf(base) + next
        else -> base.substringBefore('?').substringBeforeLast('/') + "/" + next
    }

    private fun hostPartOf(url: String): String = url.substringAfter("://").substringBefore('/').substringBefore('?')

    /** Plain readable text of an HTML page, without scripts, styles and tags. */
    fun htmlToText(html: String): String = html
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
        val fromWeb = runCatching { searchDuckDuckGo(query, limit) }.getOrDefault(emptyList())
        if (fromWeb.isNotEmpty()) return fromWeb
        // DuckDuckGo often answers programs with a "are you a robot" page: then Wikipedia (an open interface) is asked.
        val words =
            query
                .split(Regex("[^\\p{L}\\p{N}]+"))
                .filter { it.isNotEmpty() && it.lowercase() !in QUESTION_WORDS }
                .joinToString(" ")
                .ifBlank { query }
        for (language in listOf("pl", "en")) {
            val found = runCatching { searchWikipedia(words, limit, language) }.getOrDefault(emptyList())
            if (found.isNotEmpty()) return found
        }
        return emptyList()
    }

    private fun searchWikipedia(query: String, limit: Int, language: String): List<String> {
        val address =
            "https://$language.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=" +
                percentEncode(query) +
                "&gsrlimit=$limit&prop=extracts&exintro=1&explaintext=1&exsentences=4&redirects=1&format=json"
        val json = fetch(address, "application/json")
        val title = Regex(""""title":"((?:[^"\\]|\\.)*)"""")
        val extract = Regex(""""extract":"((?:[^"\\]|\\.)*)"""")
        // The pages come in one object each: title and extract are read in the order they are written.
        val pages =
            Regex("""\{"pageid":.*?"extract":"(?:[^"\\]|\\.)*"\}""", RegexOption.DOT_MATCHES_ALL)
                .findAll(json)
                .map { it.value }
                .toList()
        return pages
            .mapNotNull { page ->
                val name = title.find(page)?.groupValues?.get(1)?.let { unescapeJson(it) } ?: return@mapNotNull null
                val text =
                    extract
                        .find(page)
                        ?.groupValues
                        ?.get(1)
                        ?.let { unescapeJson(it) }
                        .orEmpty()
                        .replace('\n', ' ')
                        .trim()
                if (text.length < 40) {
                    null
                } else {
                    "$name - https://$language.wikipedia.org/wiki/" +
                        percentEncode(name.replace(' ', '_')) +
                        "\n" +
                        text
                }
            }
            .take(limit)
    }

    private fun unescapeJson(text: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                when (val n = text[i + 1]) {
                    'n',
                    't',
                    'r',
                    -> {
                        out.append(' ')
                        i += 2
                    }

                    'u' ->
                        if (i + 5 < text.length) {
                            out.append(text.substring(i + 2, i + 6).toInt(16).toChar())
                            i += 6
                        } else {
                            i += 2
                        }

                    else -> {
                        out.append(n)
                        i += 2
                    }
                }
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private fun searchDuckDuckGo(query: String, limit: Int): List<String> {
        val page =
            fetch(
                "https://html.duckduckgo.com/html/?kl=pl-pl&q=" + percentEncode(query),
                agent = BROWSER_AGENT,
            )
        val links =
            Regex("""<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(page)
                .toList()
        val snippets =
            Regex("""<a[^>]*class="result__snippet"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(page)
                .toList()
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
        val decoded = wrapped?.let { percentDecode(it) } ?: href
        return if (decoded.startsWith("//")) "https:$decoded" else decoded
    }

    fun hostOf(url: String): String =
        hostPartOf(url).substringAfter('@').let { if (it.startsWith("[")) it.substringBefore(']') + "]" else it.substringBefore(':') }

    private const val HEX = "0123456789ABCDEF"

    /** UTF-8 percent encoding (a space becomes +), like the form encoding of a query. */
    private fun percentEncode(text: String): String = buildString {
        for (b in text.encodeToByteArray()) {
            val v = b.toInt() and 0xFF
            val c = v.toChar()
            when {
                c.isLetterOrDigit() && v < 128 || c in "-_.~" -> append(c)
                c == ' ' -> append('+')
                else -> append('%').append(HEX[v shr 4]).append(HEX[v and 0xF])
            }
        }
    }

    private fun percentDecode(text: String): String {
        val out = ArrayList<Byte>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val code = if (c == '%' && i + 2 < text.length) text.substring(i + 1, i + 3).toIntOrNull(16) else null
            when {
                code != null -> {
                    out.add(code.toByte())
                    i += 3
                }
                c == '+' -> {
                    out.add(' '.code.toByte())
                    i++
                }
                else -> {
                    out.addAll(c.toString().encodeToByteArray().toList())
                    i++
                }
            }
        }
        return out.toByteArray().decodeToString()
    }
}
