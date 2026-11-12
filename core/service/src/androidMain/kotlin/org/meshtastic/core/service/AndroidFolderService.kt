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
package org.meshtastic.core.service

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.buffer
import okio.sink
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.FolderService
import java.io.FileOutputStream

/** The chosen folder is a document tree the user picked; the file is found (or created) directly inside it. */
@Single
class AndroidFolderService(private val context: Application, private val dispatchers: CoroutineDispatchers) :
    FolderService {
    override suspend fun writeFile(folder: String, name: String, block: suspend (BufferedSink) -> Unit): Boolean =
        withContext(dispatchers.io) {
            try {
                val resolver = context.contentResolver
                val tree = Uri.parse(folder)
                val treeId = DocumentsContract.getTreeDocumentId(tree)
                val target =
                    findChild(tree, treeId, name)
                        ?: DocumentsContract.createDocument(
                            resolver,
                            DocumentsContract.buildDocumentUriUsingTree(tree, treeId),
                            mimeOf(name),
                            name,
                        )
                if (target == null) {
                    Logger.e { "Could not create $name in the chosen folder" }
                    return@withContext false
                }
                val pfd = resolver.openFileDescriptor(target, "wt") ?: return@withContext false
                pfd.use { descriptor ->
                    FileOutputStream(descriptor.fileDescriptor).sink().buffer().use { sink -> block(sink) }
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Failed to write $name to the chosen folder" }
                false
            }
        }

    /** The document inside the tree whose display name is [name], or null when there is none. */
    private fun findChild(tree: Uri, treeId: String, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId)
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        context.contentResolver.query(children, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == name) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                }
            }
        }
        return null
    }

    private fun mimeOf(name: String): String = when (name.substringAfterLast('.')) {
        "csv" -> "text/csv"
        "md" -> "text/markdown"
        else -> "text/plain"
    }
}
