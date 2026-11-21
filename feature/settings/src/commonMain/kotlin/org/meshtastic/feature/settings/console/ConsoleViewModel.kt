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
package org.meshtastic.feature.settings.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.Position
import org.meshtastic.core.repository.AdminController
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.ServiceRepository

/** What the console screen shows and remembers: the printed lines, the typed line and the earlier lines. */
@KoinViewModel
class ConsoleViewModel(
    private val nodeRepository: NodeRepository,
    private val serviceRepository: ServiceRepository,
    private val adminController: AdminController,
) : ViewModel() {
    private val history = ConsoleHistory()
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    fun nodes(): List<Node> = nodeRepository.nodeDBbyNum.value.values.toList()

    fun connection(): ConnectionState = serviceRepository.connectionState.value

    /** Sets the fixed position of the connected device; false when no device is connected yet. */
    suspend fun setPosition(latitude: Double, longitude: Double, altitude: Int?): Boolean {
        val num = nodeRepository.myNodeInfo.value?.myNodeNum ?: return false
        adminController.setFixedPosition(num, Position(latitude, longitude, altitude))
        return true
    }

    fun setInput(text: String) {
        _input.value = text
    }

    fun previous() {
        history.previous()?.let { _input.value = it }
    }

    fun next() {
        history.next()?.let { _input.value = it }
    }

    /** Runs the typed line with [registry] and prints the line and what the command answered. */
    fun run(registry: ConsoleRegistry) {
        val line = _input.value.trim()
        if (line.isEmpty()) return
        history.add(line)
        _input.value = ""
        viewModelScope.launch {
            val result = registry.execute(line)
            _lines.update { old ->
                val all = if (result.clear) result.lines else old + "> $line" + result.lines
                all.takeLast(MAX_LINES)
            }
        }
    }

    private companion object {
        const val MAX_LINES = 500
    }
}
