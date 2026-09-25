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
package org.meshtastic.feature.settings.sniffer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.datastore.SnifferBufferOverflowPolicy
import org.meshtastic.core.datastore.SnifferLogFormat
import org.meshtastic.core.datastore.SnifferLogPrefs
import org.meshtastic.core.datastore.SnifferSource
import org.meshtastic.core.model.Channel
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.feature.settings.sniffer.mqtt.GroupedMqttSniffedPacket
import org.meshtastic.feature.settings.sniffer.mqtt.MqttSniffedPacket
import org.meshtastic.feature.settings.sniffer.mqtt.MqttSnifferManager
import org.meshtastic.proto.Config.LoRaConfig

/** A previously saved log the user asked to view -- see [SnifferPanelViewModel.loadLog]. Not persisted. */
data class LoadedSnifferLog(val fileName: String, val rows: List<SnifferExportRow>)

/**
 * Everything [org.meshtastic.feature.settings.sniffer.SnifferSettingsScreen] needs to know about what to show --
 * computed by its own `rememberSnifferDisplayState` helper and shared with the top-bar actions, empty-state text and
 * packet list composables that live in SnifferPanelComponents.kt.
 */
data class SnifferDisplayState(
    val showingMqtt: Boolean,
    val showingRadio: Boolean,
    val displayedMqttPackets: List<MqttSniffedPacket>,
    val displayedRadioPackets: List<SniffedPacket>,
    val groupedMqttPackets: List<GroupedMqttSniffedPacket>,
    val groupedRadioPackets: List<GroupedSniffedPacket>,
    val itemCount: Int,
    val newestKey: String?,
)

/**
 * Owns the combined Sniffer panel's cross-cutting state: which single sniffer (Radio/MQTT/Off) is currently selected,
 * the persisted display/export preferences from [SnifferLogPrefs], whether MQTT is even selectable right now, the
 * "clear panel" watermark used by the trash icon, and a loaded-from-file log shown instead of the live stream.
 *
 * This does not own either live sniffer's packets itself -- [SnifferLogViewModel] (Radio, reading from the shared
 * MeshLog stream) and [org.meshtastic.feature.settings.sniffer.mqtt.MqttSnifferLogViewModel] (MQTT, reading from the
 * shared [MqttSnifferManager]) are obtained separately at the screen layer and keep working exactly as before -- this
 * just arbitrates which one is active and how the combined screen should display/export/load whichever one that is.
 *
 * Mirrors [org.meshtastic.feature.settings.sniffer.mqtt.MqttSnifferManager]'s activation call directly from
 * [activeSource] so the MQTT connection is only ever open while MQTT is the selected source -- selecting Radio or Off
 * stops it, same as before this screen merged the two sniffers together.
 */
@KoinViewModel
class SnifferPanelViewModel(
    private val mqttSnifferManager: MqttSnifferManager,
    private val prefs: SnifferLogPrefs,
    private val nodeRepository: NodeRepository,
    radioConfigRepository: RadioConfigRepository,
) : ViewModel() {

    val activeSource: StateFlow<SnifferSource> = prefs.activeSource.stateInWhileSubscribed(SnifferSource.OFF)

    private val _displaySource = MutableStateFlow(SnifferSource.OFF)
    private var lastRealSource: SnifferSource? = null

    /**
     * Which source's packets the panel currently shows. Tracks [activeSource] while it's RADIO/MQTT, but freezes at
     * whatever it last was while [activeSource] is OFF -- turning the sniffer off must not blank the log, only the
     * trash icon ([clearDisplayedLogs]) does. Switching to a genuinely different source than what's frozen here clears
     * the panel and starts fresh instead -- see the `activeSource.onEach` in [init].
     */
    val displaySource: StateFlow<SnifferSource> = _displaySource.asStateFlow()

    private val _freezeAtMillis = MutableStateFlow(Long.MAX_VALUE)

    /**
     * Upper bound for what's shown from the live packet stream -- [Long.MAX_VALUE] (no bound) while a real source is
     * selected, or the exact moment [activeSource] became OFF. Without this, turning the sniffer off wouldn't stop the
     * displayed log from silently growing: the underlying MeshLog stream keeps logging ordinary (non-sniffed) traffic
     * regardless of the toggle, and a not-yet-fully-torn-down MQTT session can still deliver a packet or two in flight.
     * Reset back to unbounded once a real source is reselected. See [clearedAtMillis] for the lower bound.
     */
    val freezeAtMillis: StateFlow<Long> = _freezeAtMillis.asStateFlow()

    val groupByGateway: StateFlow<Boolean> = prefs.groupByGateway.stateInWhileSubscribed(true)
    val autoScroll: StateFlow<Boolean> = prefs.autoScroll.stateInWhileSubscribed(true)
    val decryptPayloads: StateFlow<Boolean> = prefs.decryptPayloads.stateInWhileSubscribed(true)
    val exportFormat: StateFlow<SnifferLogFormat> = prefs.exportFormat.stateInWhileSubscribed(SnifferLogFormat.TXT)
    val bufferOverflowPolicy: StateFlow<SnifferBufferOverflowPolicy> =
        prefs.bufferOverflowPolicy.stateInWhileSubscribed(SnifferBufferOverflowPolicy.STOP)

    // Raw, unseeded version of mqttConfigured below, for init's own reactive fallback -- see the comment there
    // for why the seeded public StateFlow is the wrong thing to react to internally.
    private val rawMqttConfigured: Flow<Boolean> =
        radioConfigRepository.moduleConfigFlow.map { it.mqtt?.enabled == true }

    /** Whether the MQTT module is enabled (and so configured enough to sniff) on the currently connected device. */
    val mqttConfigured: StateFlow<Boolean> = rawMqttConfigured.stateInWhileSubscribed(false)

    private val channelSet = radioConfigRepository.channelSetFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Packets timestamped at or before this watermark are hidden from the panel -- see [clearDisplayedLogs]. Backed by
     * [SnifferLogPrefs.clearedAtMillis] (not a local MutableStateFlow) so a clear survives this ViewModel being
     * recreated -- otherwise leaving the Sniffer screen and coming back would silently un-clear the log.
     *
     * Also set automatically (see [init]) the first time a real source is ever selected, while it's still at its 0L
     * sentinel -- without that, the very first activation would show every "not addressed to us" packet the shared
     * MeshLog table has ever accumulated, going back to whenever this install started logging, rather than just what
     * this sniffing run actually captures. [SnifferLogViewModel.sniffedPackets] queries from this same watermark.
     */
    val clearedAtMillis: StateFlow<Long> = prefs.clearedAtMillis.stateInWhileSubscribed(0L)

    private val _loadedLog = MutableStateFlow<LoadedSnifferLog?>(null)

    /** Non-null while the panel is showing a file loaded via [loadLog] instead of the live [activeSource] stream. */
    val loadedLog: StateFlow<LoadedSnifferLog?> = _loadedLog.asStateFlow()

    private val _loadFailed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** One-shot signal that the last [loadLog] call wasn't a file this app could read -- for a Snackbar/toast. */
    val loadFailed: SharedFlow<Unit> = _loadFailed.asSharedFlow()

    init {
        // The three blocks below drive real side effects (opening/closing the MQTT connection, auto-falling-back to
        // Off, freezing the displayed log) from the *persisted* selection -- so they deliberately collect
        // prefs.activeSource / rawMqttConfigured directly, never the activeSource/mqttConfigured StateFlows exposed
        // above. Those StateFlows are seeded with a synthetic placeholder (Off / false) via stateIn's initialValue so
        // the screen has something to render before the real DataStore/BLE value has loaded -- harmless for a label,
        // but wrong here: this ViewModel is recreated every time the user leaves this screen and comes back (it's
        // scoped to the destination's own back stack entry), and reacting to that placeholder as if it were a
        // genuine change used to immediately stop a still-selected MQTT session, and could even auto-select Off --
        // i.e. the sniffer looked like it had turned itself off just from revisiting the screen. Reading the raw
        // flows instead sidesteps that: their first emission is always the real current value, never a placeholder.

        // Keep the shared MQTT session open only while MQTT is the selected source, same gating the old standalone
        // MQTT Sniffer screen had -- selecting Radio or Off must stop it, not leave a stale connection running in
        // the background.
        prefs.activeSource
            .onEach { source -> mqttSnifferManager.setActive(source == SnifferSource.MQTT) }
            .launchIn(viewModelScope)

        // If the device's MQTT module gets disabled/unconfigured while MQTT is selected, fall back to Off rather than
        // silently keep a now-invalid selection around.
        rawMqttConfigured
            .onEach { configured ->
                if (!configured && activeSource.value == SnifferSource.MQTT) selectSource(SnifferSource.OFF)
            }
            .launchIn(viewModelScope)

        // lastRealSource starts null so the very first value seen (e.g. a source restored from prefs on a fresh
        // launch) only seeds displaySource, without wiping out that source's existing MeshLog history. From then
        // on, a genuine switch between two real sources clears the panel; passing through Off does not.
        //
        // Paired with prefs.lastRealSource: this ViewModel is recreated every time the Sniffer screen is reopened
        // (it's scoped to the destination's own back stack entry), so a plain in-memory `lastRealSource` field is
        // blank on every fresh instance. While activeSource is Off, that used to mean displaySource had nothing to
        // fall back to and stayed Off forever -- the log looked like it had vanished just from leaving the screen
        // and coming back, even though nothing was ever cleared. Restoring from the persisted lastRealSource (set
        // below, right alongside activeSource, whenever a real source is selected) fixes that: Off still freezes
        // the log instead of blanking it, exactly as it does within a single visit to the screen.
        combine(prefs.activeSource, prefs.lastRealSource, prefs.clearedAtMillis) {
                source,
                persistedLastReal,
                clearedAt,
            ->
            Triple(source, persistedLastReal, clearedAt)
        }
            .onEach { (source, persistedLastReal, clearedAt) ->
                if (source != SnifferSource.OFF) {
                    when {
                        lastRealSource != null && lastRealSource != source -> clearDisplayedLogs()

                        // First-ever activation: clearedAt is still at its 0L sentinel because the trash icon has
                        // never been clicked. Anchor it to now instead of leaving the panel (and
                        // SnifferLogViewModel's query) unbounded -- see clearedAtMillis's doc.
                        clearedAt == 0L ->
                            viewModelScope.launch { prefs.setClearedAtMillis(Clock.System.now().toEpochMilliseconds()) }
                    }
                    lastRealSource = source
                    _displaySource.value = source
                    _freezeAtMillis.value = Long.MAX_VALUE
                    if (persistedLastReal != source) prefs.setLastRealSource(source)
                } else {
                    _freezeAtMillis.value = Clock.System.now().toEpochMilliseconds()
                    if (lastRealSource == null && persistedLastReal != null) {
                        lastRealSource = persistedLastReal
                        _displaySource.value = persistedLastReal
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectSource(source: SnifferSource) {
        viewModelScope.launch { prefs.setActiveSource(source) }
    }

    fun setGroupByGateway(enabled: Boolean) {
        viewModelScope.launch { prefs.setGroupByGateway(enabled) }
    }

    fun setAutoScroll(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoScroll(enabled) }
    }

    fun setDecryptPayloads(enabled: Boolean) {
        viewModelScope.launch { prefs.setDecryptPayloads(enabled) }
    }

    fun setExportFormat(format: SnifferLogFormat) {
        viewModelScope.launch { prefs.setExportFormat(format) }
    }

    fun setBufferOverflowPolicy(policy: SnifferBufferOverflowPolicy) {
        viewModelScope.launch { prefs.setBufferOverflowPolicy(policy) }
    }

    /**
     * Hides everything currently displayed without touching underlying data: the Radio side's packets still live in
     * [org.meshtastic.core.model.MeshLog] (unaffected -- this only advances the display watermark), and the MQTT side's
     * in-memory buffer is dropped via [MqttSnifferManager.clear] since nothing else persists it. Also drops any loaded
     * file, so the trash icon always returns to a clean live view.
     */
    fun clearDisplayedLogs() {
        viewModelScope.launch { prefs.setClearedAtMillis(Clock.System.now().toEpochMilliseconds()) }
        mqttSnifferManager.clear()
        _loadedLog.value = null
    }

    /**
     * Parses a previously exported JSON log and shows it in place of the live stream (the live sniffers keep running or
     * staying off exactly as [activeSource] has them -- loading a file only changes what's displayed). When
     * [decryptPayloads] is on, retries decoding every still-encrypted row against this app's current channel keys (see
     * [SnifferExportRow.reattemptDecryption]) -- useful for a log saved before a channel was known. Emits [loadFailed]
     * instead if [bytes] isn't a JSON log this app wrote.
     */
    fun loadLog(bytes: ByteArray, fileName: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val parsed = parseSnifferExportJson(bytes)
            if (parsed == null) {
                _loadFailed.emit(Unit)
                return@launch
            }
            val rows =
                if (decryptPayloads.value) {
                    val psks = knownChannelPsks()
                    parsed.map { it.reattemptDecryption(nodeRepository, psks) }
                } else {
                    parsed
                }
            _loadedLog.value = LoadedSnifferLog(fileName, rows)
        }
    }

    fun dismissLoadedLog() {
        _loadedLog.value = null
    }

    /**
     * Every channel this app currently holds an (already-expanded) key for -- mirrors the live sniffers' own helper.
     */
    private fun knownChannelPsks(): List<ByteArray> {
        val set = channelSet.value ?: return emptyList()
        val loraConfig = set.lora_config ?: LoRaConfig.Builder().build()
        return set.settings.map { Channel(it, loraConfig).psk.toByteArray() }
    }
}
