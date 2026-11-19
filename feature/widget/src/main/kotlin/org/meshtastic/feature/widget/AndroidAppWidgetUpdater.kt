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
package org.meshtastic.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.repository.AppWidgetUpdater

private const val MESH_STATUS_ROTATION_MS = 60_000L

@Single
class AndroidAppWidgetUpdater(private val context: Context, meshStatusProvider: MeshStatusWidgetStateProvider) :
    AppWidgetUpdater {
    @Suppress("InjectDispatcher") // feature:widget does not depend on core:di, where CoroutineDispatchers lives
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            meshStatusProvider.state.collect {
                if (hasInstances(MeshStatusWidget::class.java)) update(MeshStatusWidget())
            }
        }

        // The status widget shows one node at a time and switches every minute, so it is redrawn on that beat too.
        scope.launch {
            while (true) {
                delay(MESH_STATUS_ROTATION_MS - System.currentTimeMillis() % MESH_STATUS_ROTATION_MS)
                if (hasInstances(MeshStatusWidget::class.java)) update(MeshStatusWidget())
            }
        }
    }

    private suspend fun hasInstances(widget: Class<out GlanceAppWidget>): Boolean =
        GlanceAppWidgetManager(context).getGlanceIds(widget).isNotEmpty()

    override suspend fun updateAll() {
        update(MeshStatusWidget())
    }

    private suspend fun update(widget: GlanceAppWidget) {
        @Suppress("TooGenericExceptionCaught")
        try {
            widget.updateAll(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e) { "Failed to update ${widget::class.simpleName}" }
        }
    }
}
