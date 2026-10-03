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
package org.meshtastic.feature.settings.debugging

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.ioDispatcher
import org.meshtastic.core.common.util.nowInstant
import org.meshtastic.core.domain.usecase.settings.SetMeshLogSettingsUseCase
import org.meshtastic.core.model.MeshLog
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.MeshLogPrefs
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.debug_clear
import org.meshtastic.core.resources.debug_clear_logs_confirm
import org.meshtastic.core.ui.util.AlertManager
import org.meshtastic.core.ui.viewmodel.safeLaunch
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.feature.settings.util.decodePayloadFromMeshLog
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.PortNum

enum class FilterMode {
    AND,
    OR,
}

// --- Search and Filter Managers ---
class LogSearchManager {
    data class SearchMatch(val logIndex: Int, val start: Int, val end: Int, val field: String)

    data class SearchState(
        val searchText: String = "",
        val currentMatchIndex: Int = -1,
        val allMatches: List<SearchMatch> = emptyList(),
        val hasMatches: Boolean = false,
    )

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(-1)
    val currentMatchIndex = _currentMatchIndex.asStateFlow()

    private val _searchState = MutableStateFlow(SearchState())
    val searchState = _searchState.asStateFlow()

    fun setSearchText(text: String) {
        _searchText.value = text
        _currentMatchIndex.value = -1
    }

    fun goToNextMatch() {
        val matches = _searchState.value.allMatches
        if (matches.isNotEmpty()) {
            val nextIndex = if (_currentMatchIndex.value < matches.lastIndex) _currentMatchIndex.value + 1 else 0
            _currentMatchIndex.value = nextIndex
            _searchState.value = _searchState.value.copy(currentMatchIndex = nextIndex)
        }
    }

    fun goToPreviousMatch() {
        val matches = _searchState.value.allMatches
        if (matches.isNotEmpty()) {
            val prevIndex = if (_currentMatchIndex.value > 0) _currentMatchIndex.value - 1 else matches.lastIndex
            _currentMatchIndex.value = prevIndex
            _searchState.value = _searchState.value.copy(currentMatchIndex = prevIndex)
        }
    }

    fun clearSearch() {
        setSearchText("")
    }

    fun updateMatches(searchText: String, filteredLogs: List<DebugViewModel.UiMeshLog>) {
        val matches = findSearchMatches(searchText, filteredLogs)
        val hasMatches = matches.isNotEmpty()
        _searchState.value =
            _searchState.value.copy(
                searchText = searchText,
                allMatches = matches,
                hasMatches = hasMatches,
                currentMatchIndex = if (hasMatches) _currentMatchIndex.value.coerceIn(0, matches.lastIndex) else -1,
            )
    }

    fun findSearchMatches(searchText: String, filteredLogs: List<DebugViewModel.UiMeshLog>): List<SearchMatch> {
        if (searchText.isEmpty()) {
            return emptyList()
        }
        return filteredLogs
            .flatMapIndexed { logIndex, log ->
                searchText.split(" ").flatMap { term ->
                    val escapedTerm = Regex.escape(term)
                    val regex = escapedTerm.toRegex(RegexOption.IGNORE_CASE)
                    val messageMatches =
                        regex.findAll(log.logMessage).map {
                            SearchMatch(logIndex, it.range.first, it.range.last, "message")
                        }
                    val typeMatches =
                        regex.findAll(log.messageType).map {
                            SearchMatch(logIndex, it.range.first, it.range.last, "type")
                        }
                    val dateMatches =
                        regex.findAll(log.formattedReceivedDate).map {
                            SearchMatch(logIndex, it.range.first, it.range.last, "date")
                        }
                    val decodedPayloadMatches =
                        log.decodedPayload?.let {
                            regex.findAll(it).map {
                                SearchMatch(logIndex, it.range.first, it.range.last, "decodedPayload")
                            }
                        } ?: emptySequence()
                    messageMatches + typeMatches + dateMatches + decodedPayloadMatches
                }
            }
            .sortedBy { it.start }
    }
}

class LogFilterManager {
    private val _filterTexts = MutableStateFlow<List<String>>(emptyList())
    val filterTexts = _filterTexts.asStateFlow()

    private val _filteredLogs = MutableStateFlow<List<DebugViewModel.UiMeshLog>>(emptyList())
    val filteredLogs = _filteredLogs.asStateFlow()

    fun setFilterTexts(filters: List<String>) {
        _filterTexts.value = filters
    }

    fun updateFilteredLogs(logs: List<DebugViewModel.UiMeshLog>) {
        _filteredLogs.value = logs
    }

    fun filterLogs(
        logs: List<DebugViewModel.UiMeshLog>,
        filterTexts: List<String>,
        filterMode: FilterMode,
    ): List<DebugViewModel.UiMeshLog> {
        if (filterTexts.isEmpty()) return logs
        return logs.filter { logItem ->
            when (filterMode) {
                FilterMode.OR ->
                    filterTexts.any { filter ->
                        logItem.logMessage.contains(filter, ignoreCase = true) ||
                            logItem.messageType.contains(filter, ignoreCase = true) ||
                            logItem.formattedReceivedDate.contains(filter, ignoreCase = true) ||
                            (logItem.decodedPayload?.contains(filter, ignoreCase = true) == true)
                    }

                FilterMode.AND ->
                    filterTexts.all { filter ->
                        logItem.logMessage.contains(filter, ignoreCase = true) ||
                            logItem.messageType.contains(filter, ignoreCase = true) ||
                            logItem.formattedReceivedDate.contains(filter, ignoreCase = true) ||
                            (logItem.decodedPayload?.contains(filter, ignoreCase = true) == true)
                    }
            }
        }
    }
}

/**
 * Throttle for the expensive mesh-log decode pipeline (see [DebugViewModel.meshLog]). Chosen to stay well under human
 * perception for a debug log view while bounding decode frequency during a packet flood; not a correctness requirement,
 * just a GC-thrash guard.
 */
private const val LOG_DECODE_DEBOUNCE_MS = 200L

@KoinViewModel
@Suppress("TooManyFunctions")
class DebugViewModel(
    private val meshLogRepository: MeshLogRepository,
    private val nodeRepository: NodeRepository,
    private val meshLogPrefs: MeshLogPrefs,
    private val setMeshLogSettingsUseCase: SetMeshLogSettingsUseCase,
    private val alertManager: AlertManager,
    private val dispatchers: org.meshtastic.core.di.CoroutineDispatchers,
) : ViewModel() {

    @OptIn(FlowPreview::class)
    val meshLog: StateFlow<ImmutableList<UiMeshLog>> =
        meshLogRepository
            .getAllLogs()
            // Room re-emits this Flow on every insert into the mesh_log table. Under a packet
            // flood (hostile mesh traffic, a runaway sender, etc.) that can fire many times a
            // second; without throttling, mapLatest keeps restarting a full re-decode of up to
            // DEFAULT_MAX_LOGS (5000) rows -- including proto toString() + node-DB lookups --
            // faster than it can complete, thrashing the GC instead of making progress.
            // Debouncing caps the decode rate independent of the insert rate.
            .debounce(LOG_DECODE_DEBOUNCE_MS)
            .mapLatest { logs -> withContext(dispatchers.default) { toUiState(logs) } }
            .stateInWhileSubscribed(initialValue = persistentListOf())

    private val _retentionDays = MutableStateFlow(meshLogPrefs.retentionDays.value)
    val retentionDays: StateFlow<Int> = _retentionDays.asStateFlow()

    private val _loggingEnabled = MutableStateFlow(meshLogPrefs.loggingEnabled.value)
    val loggingEnabled: StateFlow<Boolean> = _loggingEnabled.asStateFlow()

    // --- Managers ---
    val searchManager = LogSearchManager()
    val filterManager = LogFilterManager()

    val searchText
        get() = searchManager.searchText

    val currentMatchIndex
        get() = searchManager.currentMatchIndex

    val searchState
        get() = searchManager.searchState

    val filterTexts
        get() = filterManager.filterTexts

    val filteredLogs
        get() = filterManager.filteredLogs

    private val _selectedLogId = MutableStateFlow<String?>(null)
    val selectedLogId = _selectedLogId.asStateFlow()

    fun updateFilteredLogs(logs: List<UiMeshLog>) {
        filterManager.updateFilteredLogs(logs)
        searchManager.updateMatches(searchManager.searchText.value, logs)
    }

    fun setRetentionDays(days: Int) {
        setMeshLogSettingsUseCase.setRetentionDays(days)
        _retentionDays.value = days.coerceIn(MeshLogPrefs.MIN_RETENTION_DAYS, MeshLogPrefs.MAX_RETENTION_DAYS)
    }

    fun setLoggingEnabled(enabled: Boolean) {
        setMeshLogSettingsUseCase.setLoggingEnabled(enabled)
        _loggingEnabled.value = enabled
    }

    suspend fun loadLogsForExport(): ImmutableList<UiMeshLog> = withContext(ioDispatcher) {
        val unbounded = meshLogRepository.getAllLogsUnbounded().first()
        val logs = if (unbounded.isEmpty()) meshLogRepository.getAllLogs().first() else unbounded
        toUiState(logs)
    }

    init {
        Logger.d { "DebugViewModel created" }
        safeLaunch(tag = "searchMatchUpdater") {
            combine(searchManager.searchText, filterManager.filteredLogs) { searchText, logs ->
                searchManager.findSearchMatches(searchText, logs)
            }
                .collect {
                    searchManager.updateMatches(searchManager.searchText.value, filterManager.filteredLogs.value)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Logger.d { "DebugViewModel cleared" }
    }

    private fun toUiState(databaseLogs: List<MeshLog>): ImmutableList<UiMeshLog> {
        // Snapshot the node DB once per decode pass, not once per row. With up to
        // DEFAULT_MAX_LOGS (5000) rows and meshes that can carry hundreds/thousands of nodes
        // (see push_fake_nodedb), re-materializing `nodeDBbyNum.values.toList()` inside the
        // per-row loop was an O(rows * nodes) hotspot that compounded the flood-induced
        // re-decode thrash described above.
        val nodeList = nodeRepository.nodeDBbyNum.value.values.toList()
        val myNodeNum = nodeRepository.myNodeInfo.value?.myNodeNum
        return databaseLogs
            .map {
                UiMeshLog(
                    uuid = it.uuid,
                    messageType = it.message_type,
                    formattedReceivedDate = DateFormatter.formatDateTime(it.received_date),
                    logMessage = annotateMeshLogMessage(it, nodeList, myNodeNum),
                    decodedPayload = decodePayloadFromMeshLog(it, nodeRepository),
                    deviceLogLine = deviceLogLine(it),
                )
            }
            .toImmutableList()
    }

    /** Serial-console style line for a firmware [LogRecord] entry, or null for any other log type. */
    private fun deviceLogLine(meshLog: MeshLog): String? {
        if (meshLog.message_type != DEVICE_LOG_MESSAGE_TYPE) return null
        val record = meshLog.fromRadio.log_record ?: return null
        val level = record.level.name.firstOrNull() ?: '?'
        val source = record.source.ifBlank { "-" }
        return "$level/$source: ${record.message.trimEnd()}"
    }

    /** Transform the input [MeshLog] by enhancing the raw message with annotations. */
    private fun annotateMeshLogMessage(meshLog: MeshLog, nodeList: List<Node>, myNodeNum: Int?): String =
        when (meshLog.message_type) {
            "LogRecord" -> meshLog.fromRadio.log_record.toString().replace("\\n\"", "\"")

            "Packet" ->
                meshLog.meshPacket?.let { packet -> annotatePacketLog(packet, nodeList, myNodeNum) }
                    ?: meshLog.raw_message

            "NodeInfo" ->
                meshLog.nodeInfo?.let { nodeInfo -> annotateRawMessage(meshLog.raw_message, nodeInfo.num) }
                    ?: meshLog.raw_message

            "MyNodeInfo" ->
                meshLog.myNodeInfo?.let { nodeInfo -> annotateRawMessage(meshLog.raw_message, nodeInfo.my_node_num) }
                    ?: meshLog.raw_message

            else -> meshLog.raw_message
        }

    private fun annotatePacketLog(packet: MeshPacket, nodeList: List<Node>, myNodeNum: Int?): String {
        val decoded = packet.decoded
        val basePacket = packet.newBuilder().also { wb -> wb.decoded = null }.build()
        val baseText = basePacket.toString().trimEnd()
        var result =
            if (decoded != null) {
                val decodedText = decoded.toString().trimEnd().prependIndent("  ")
                "$baseText\ndecoded {\n$decodedText\n}"
            } else {
                baseText
            }

        val relayNode = packet.relay_node
        var relayNodeAnnotation: String? = null
        val placeholder = "___RELAY_NODE___"

        if (relayNode != 0) {
            Node.getRelayNode(relayNode, nodeList, myNodeNum)?.let { node ->
                val relayId = node.user.id
                val relayName = node.user.long_name
                // Wire's toString prints `relay_node=245`; rows stored before the Wire
                // migration carry protobuf-java's `relay_node: 245`. Match both.
                val regex = Regex("""\brelay_node[=:] ?${relayNode.toUInt()}\b""")
                if (regex.containsMatchIn(result)) {
                    relayNodeAnnotation = "relay_node=$relayName ($relayId)"
                    result = regex.replace(result, placeholder)
                }
            }
        }

        result = annotateRawMessage(result, packet.from, packet.to)

        if (relayNodeAnnotation != null) {
            result = result.replace(placeholder, relayNodeAnnotation)
        } else {
            // Not annotated with name, so use hex.
            result = annotateRawMessage(result, relayNode)
        }

        return result
    }

    /** Annotate the raw message string with the node IDs provided, in hex, if they are present. */
    private fun annotateRawMessage(rawMessage: String, vararg nodeIds: Int): String {
        val msg = StringBuilder(rawMessage)
        var mutated = false
        nodeIds.toSet().forEach { nodeId -> mutated = mutated or msg.annotateNodeId(nodeId) }
        return if (mutated) {
            msg.toString()
        } else {
            rawMessage
        }
    }

    /** Look for a single node ID integer in the string and annotate it with the hex equivalent if found. */
    private fun StringBuilder.annotateNodeId(nodeId: Int): Boolean {
        // Wire's toString prints int fields SIGNED and `=`-delimited (`from=-1897181963,`),
        // which is what users see since the Wire migration (#6520). Rows stored before it
        // carry protobuf-java's text format: unsigned, colon-separated, whitespace-bounded
        // (`from: 2397785333`). Match either representation at a value position.
        val signed = Regex.escape(nodeId.toString())
        val unsigned = Regex.escape(nodeId.toUInt().toString())
        val regex = Regex("""(?<=[=\s]|^)($signed|$unsigned)(?=[,}\s]|$)""")
        if (!regex.containsMatchIn(this)) return false
        regex.findAll(this).toList().asReversed().forEach {
            val idx = it.range.last + 1
            insert(idx, " (${NodeAddress.numToDefaultId(nodeId)})")
        }
        return true
    }

    fun requestDeleteAllLogs() {
        alertManager.showAlert(
            titleRes = Res.string.debug_clear,
            messageRes = Res.string.debug_clear_logs_confirm,
            onConfirm = { deleteAllLogs() },
        )
    }

    fun deleteAllLogs() = safeLaunch(context = ioDispatcher, tag = "deleteAllLogs") { meshLogRepository.deleteAll() }

    @Immutable
    data class UiMeshLog(
        val uuid: String,
        val messageType: String,
        val formattedReceivedDate: String,
        val logMessage: String,
        val decodedPayload: String? = null,
        /** For device log records: "L/source: message" like the radio's serial console; null for other types. */
        val deviceLogLine: String? = null,
    )

    val presetFilters: List<String>
        get() = buildList {
            // Our address if available
            nodeRepository.myNodeInfo.value?.myNodeNum?.let { add(NodeAddress.numToDefaultId(it)) }
            // broadcast
            add("!ffffffff")
            // decoded
            add("decoded")
            // today (locale-dependent short date format)
            add(DateFormatter.formatShortDate(nowInstant.toEpochMilliseconds()))
            // Each app name
            addAll(PortNum.entries.map { it.name })
        }

    fun setSelectedLogId(id: String?) {
        _selectedLogId.value = id
    }
}
