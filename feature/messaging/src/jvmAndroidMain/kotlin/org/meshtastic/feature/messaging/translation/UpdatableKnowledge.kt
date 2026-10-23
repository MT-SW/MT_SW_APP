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

import co.touchlab.kermit.Logger
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.meshtastic.core.repository.AssistantKnowledge
import org.meshtastic.core.repository.KnowledgeIndex
import org.meshtastic.core.repository.UiPrefs

/**
 * Knowledge about the app that can be refreshed from our own repository. It works only while the user has switched
 * the assistant's internet access on; with the switch off nothing is downloaded and the knowledge built into the app
 * is used. Every downloaded file is checked against the SHA-1 that GitHub publishes for it, so a damaged or swapped
 * file is dropped. If anything fails, the built-in knowledge keeps working.
 */
class UpdatableKnowledge(
    private val builtIn: AssistantKnowledge,
    private val directory: File,
    private val uiPrefs: UiPrefs,
) : AssistantKnowledge {

    @Volatile private var index: KnowledgeIndex? = null

    @Volatile private var loadedFrom = 0L

    override suspend fun lookup(question: String, maxChars: Int): String {
        if (uiPrefs.assistantInternetEnabled.value) {
            withContext(Dispatchers.IO) { refreshIfDue() }
        }
        // With the switch off the downloaded copy is still used: it was verified, it is just not refreshed.
        val downloaded = withContext(Dispatchers.IO) { loadIndex() }
        val fromDownloaded = downloaded?.lookup(question, maxChars).orEmpty()
        return fromDownloaded.ifBlank { builtIn.lookup(question, maxChars) }
    }

    override suspend fun latestNews(maxChars: Int): String {
        val downloaded = withContext(Dispatchers.IO) { loadIndex() }
        return downloaded?.latestNews(maxChars).orEmpty().ifBlank { builtIn.latestNews(maxChars) }
    }

    private fun loadIndex(): KnowledgeIndex? {
        val stamp = File(directory, STAMP).lastModified()
        if (stamp == 0L) return null
        if (stamp == loadedFrom && index != null) return index
        val docs = buildList {
            File(directory, "docs").listFiles { f -> f.extension == "md" }?.sortedBy { it.name }?.forEach { f ->
                add(KnowledgeIndex.Document(f.nameWithoutExtension, f.readText()))
            }
            File(directory, "project").listFiles { f -> f.extension == "md" }?.sortedBy { it.name }?.forEach { f ->
                add(KnowledgeIndex.Document("MT_SW ${f.nameWithoutExtension}", f.readText()))
            }
            File(directory, "CHANGELOG.md").takeIf { it.exists() }?.let {
                add(KnowledgeIndex.Document("CHANGELOG", it.readText(), news = true))
            }
        }
        index = docs.takeIf { it.isNotEmpty() }?.let { KnowledgeIndex(it) }
        loadedFrom = stamp
        return index
    }

    @Synchronized
    private fun refreshIfDue() {
        val stamp = File(directory, STAMP)
        if (System.currentTimeMillis() - stamp.lastModified() < DAY_MS) return
        try {
            directory.mkdirs()
            File(directory, "docs").mkdirs()
            download("CHANGELOG.md", File(directory, "CHANGELOG.md"), "CHANGELOG.md")
            for (lang in listOf("pl", "en")) {
                val listing = WebAccess.fetch("$API/docs/$lang/user", "application/json")
                val entries = ENTRY.findAll(listing).map { it.groupValues[1] to it.groupValues[2] }.toList()
                for ((name, sha) in entries) {
                    if (!name.endsWith(".md")) continue
                    val target = File(directory, "docs/$lang-$name")
                    if (target.exists() && gitSha(target.readBytes()) == sha) continue
                    download("docs/$lang/user/$name", target, null, sha)
                }
            }
            // The other parts of the project (firmware, integration) come from their own repositories.
            File(directory, "project").mkdirs()
            for ((repo, path, name) in PROJECT_SOURCES) {
                try {
                    download(repo, path, File(directory, "project/$name.md"), path)
                } catch (e: Exception) {
                    Logger.w { "Assistant knowledge: $repo/$path skipped: ${e.message}" }
                }
            }
            stamp.writeText(System.currentTimeMillis().toString())
            stamp.setLastModified(System.currentTimeMillis())
            Logger.i { "Assistant knowledge updated from the repository" }
        } catch (e: Exception) {
            Logger.w { "Assistant knowledge update skipped: ${e.message}" }
        }
    }

    /** Downloads one file and keeps it only if its git blob hash matches [expectedSha] (looked up when null). */
    private fun download(repo: String, path: String, target: File, apiPath: String?) =
        download(path, target, apiPath, null, repo)

    private fun download(
        path: String,
        target: File,
        apiPath: String?,
        expectedSha: String? = null,
        repo: String = REPO,
    ) {
        val sha =
            expectedSha
                ?: ENTRY_SHA.find(WebAccess.fetch("https://api.github.com/repos/$repo/contents/${apiPath ?: path}", "application/json"))?.groupValues?.get(1)
                ?: error("No hash for $path")
        val bytes = WebAccess.fetchBytes("https://raw.githubusercontent.com/$repo/main/$path")
        check(gitSha(bytes) == sha) { "Hash mismatch for $path" }
        target.writeBytes(bytes)
    }

    private fun gitSha(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${bytes.size}\u0000".toByteArray())
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val REPO = "MT-SW/MT_SW_APP"
        const val API = "https://api.github.com/repos/$REPO/contents"
        const val RAW = "https://raw.githubusercontent.com/$REPO/main"
        const val STAMP = "updated.stamp"
        const val DAY_MS = 24L * 60 * 60 * 1000
        val PROJECT_SOURCES =
            listOf(
                Triple("MT-SW/MT_SW_APP", "knowledge/project.md", "project"),
                Triple("MT-SW/MT_SW_APP", "knowledge/achievements.md", "achievements"),
                Triple("MT-SW/MT_SW_FIRMWARE", "README.md", "firmware"),
                Triple("MT-SW/MT_SW_INTEGRATION", "README.md", "integration"),
                Triple("MT-SW/MT_SW_INTEGRATION", "custom_components/meshtastic/meshtastic_web/MTSW_WEB_CLIENT.md", "integration-web-client"),
                Triple("MT-SW/MT_SW_INTEGRATION", "custom_components/meshtastic/ha_frontend/planner/API.md", "integration-planner-api"),
            )
        val ENTRY = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"[^{}]*?\"sha\"\\s*:\\s*\"([0-9a-f]{40})\"")
        val ENTRY_SHA = Regex("\"sha\"\\s*:\\s*\"([0-9a-f]{40})\"")
    }
}
