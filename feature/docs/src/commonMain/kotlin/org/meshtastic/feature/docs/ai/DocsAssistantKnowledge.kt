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
package org.meshtastic.feature.docs.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import meshtasticandroid.feature.docs.generated.resources.Res
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.currentLocaleTag
import org.meshtastic.core.repository.AssistantKnowledge
import org.meshtastic.core.repository.KnowledgeIndex
import org.meshtastic.feature.docs.data.DefaultDocBundleLoader
import org.meshtastic.feature.docs.model.DocSection

/**
 * Knowledge base of the offline assistant, built from the user guide and the change list that are packaged into the
 * app at build time (nothing is downloaded). The search itself is [KnowledgeIndex].
 */
@Single(binds = [AssistantKnowledge::class])
class DocsAssistantKnowledge(private val loader: DefaultDocBundleLoader) : AssistantKnowledge {

    private val mutex = Mutex()
    private var cachedLanguage = ""
    private var cache: KnowledgeIndex? = null

    override suspend fun lookup(question: String, maxChars: Int): String = lookup(question, maxChars, null)

    override suspend fun lookup(question: String, maxChars: Int, language: String?): String {
        if (question.isBlank()) return ""
        return indexFor(language ?: languageOf(question)).lookup(question, maxChars)
    }

    override suspend fun coreFacts(language: String): String =
        org.meshtastic.core.repository.knowledgeSection(readText("files/knowledge/core.md"), language)

    private val stringsCache = HashMap<String, Map<String, String>>()

    override suspend fun answerStrings(language: String): Map<String, String> = mutex.withLock {
        stringsCache.getOrPut(language) { parseStrings(readText("files/assistant/strings-$language.xml")) }
    }

    private suspend fun readText(path: String): String = try {
        Res.readBytes(path).decodeToString()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        ""
    }

    override suspend fun latestNews(maxChars: Int): String = indexFor(languageOf("")).latestNews(maxChars)

    private suspend fun indexFor(language: String): KnowledgeIndex = mutex.withLock {
        cache?.let { if (language == cachedLanguage) return@withLock it }
        val documents = ArrayList<KnowledgeIndex.Document>()
        try {
            for (page in loader.load().pages.filter { it.section == DocSection.UserGuide }) {
                val markdown = loader.readPageLocalized(page.id, language).first?.markdown ?: continue
                documents += KnowledgeIndex.Document(page.title, markdown)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A missing page only makes the knowledge smaller.
        }
        try {
            val changelog = Res.readBytes(CHANGELOG_PATH).decodeToString()
            documents += KnowledgeIndex.Document("CHANGELOG", changelog, news = true)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // No change list in this build.
        }
        for ((file, title) in PROJECT_FILES) {
            try {
                documents += KnowledgeIndex.Document(title, Res.readBytes("files/knowledge/$file").decodeToString())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // A missing file only makes the knowledge smaller.
            }
        }
        val built = KnowledgeIndex(documents)
        cachedLanguage = language
        cache = built
        built
    }

    private fun languageOf(question: String): String {
        if (question.any { it in POLISH_LETTERS }) return "pl"
        return currentLocaleTag().substringBefore('-').substringBefore('_').ifEmpty { "en" }
    }

    companion object {
        /** Reads `<string name="key">text</string>` lines of a strings file into a map of plain texts. */
        fun parseStrings(xml: String): Map<String, String> =
            STRING_LINE.findAll(xml).associate { m ->
                m.groupValues[1] to
                    m.groupValues[2].replace("\\'", "'").replace("\\\"", "\"").replace("&lt;", "<").replace("&gt;", ">")
                        .replace("&amp;", "&").replace("\\n", "\n")
            }

        private val STRING_LINE = Regex("<string name=\"([^\"]+)\">(.*?)</string>")

        /** The other parts of the MT_SW project; kept apart from the user guide, so only the assistant sees them. */
        val PROJECT_FILES =
            listOf(
                "project.md" to "MT_SW project",
                "achievements.md" to "MT_SW achievements",
                "firmware.md" to "MT_SW firmware",
                "integration.md" to "MT_SW Home Assistant integration",
                "integration-web-client.md" to "MT_SW integration web client",
                "integration-planner-api.md" to "MT_SW integration planner API",
            )
        const val CHANGELOG_PATH = "files/knowledge/CHANGELOG.md"
        const val POLISH_LETTERS = "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ"
    }
}
