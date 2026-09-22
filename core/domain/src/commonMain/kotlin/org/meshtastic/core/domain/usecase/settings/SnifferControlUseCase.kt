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
package org.meshtastic.core.domain.usecase.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import org.meshtastic.core.model.MeshLog
import org.meshtastic.core.model.util.ON_DEMAND_PORT_NUM
import org.meshtastic.core.repository.MeshLogRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.proto.OnDemand
import org.meshtastic.proto.OnDemandType

/**
 * Use case for the Sniffer module's enable/disable/state protocol.
 *
 * Sniffer used to be a [org.meshtastic.proto.ModuleConfig] section (`nodemodadmin`) synced and saved like any other
 * module config. The protocol this now follows (matching the "original" protobufs, replacing our team's abandoned fork)
 * instead uses three OnDemand requests on port 354 (see PrivatePortNum.kt) -- [OnDemandType.REQUEST_SNIFFER_ENABLE] /
 * [OnDemandType.REQUEST_SNIFFER_DISABLE] / [OnDemandType.REQUEST_SNIFFER_STATE] -- answered by
 * [OnDemandType.RESPONSE_SNIFFER_STATE]. Firmware keeps this state in RAM only (it is never written to ModuleConfig),
 * so the app does not persist it either: [snifferEnabledFlow] always reflects the most recently logged response for
 * that node, the same way OnDemand diagnostics already work (see OnDemandLogViewModel).
 */
@Single
open class SnifferControlUseCase(
    private val radioController: RadioController,
    private val meshLogRepository: MeshLogRepository,
) {

    /** Sends REQUEST_SNIFFER_STATE to [destNum]; the answer arrives asynchronously via [snifferEnabledFlow]. */
    open suspend fun requestState(destNum: Int) {
        radioController.requestOnDemand(destNum, OnDemandType.REQUEST_SNIFFER_STATE)
    }

    /** Sends REQUEST_SNIFFER_ENABLE or REQUEST_SNIFFER_DISABLE to [destNum]. */
    open suspend fun setEnabled(destNum: Int, enabled: Boolean) {
        val type = if (enabled) OnDemandType.REQUEST_SNIFFER_ENABLE else OnDemandType.REQUEST_SNIFFER_DISABLE
        radioController.requestOnDemand(destNum, type)
    }

    /**
     * The most recently logged RESPONSE_SNIFFER_STATE for [destNum], or `null` when none has been received yet --
     * either it hasn't been queried this session, or the connected firmware doesn't implement the Sniffer OnDemand
     * protocol at all.
     */
    open fun snifferEnabledFlow(destNum: Int): Flow<Boolean?> =
        meshLogRepository.getLogsFrom(destNum, ON_DEMAND_PORT_NUM).map(::decodeLatestSnifferState)

    private fun decodeLatestSnifferState(logs: List<MeshLog>): Boolean? = logs
        .mapNotNull { log ->
            log.fromRadio
                ?.packet
                ?.decoded
                ?.payload
                ?.let { payload -> runCatching { OnDemand.ADAPTER.decode(payload) }.getOrNull() }
                ?.response
                ?.takeIf { it.response_type == OnDemandType.RESPONSE_SNIFFER_STATE }
                ?.sniffer_state
                ?.enabled
                ?.let { enabled -> enabled to log.received_date }
        }
        .maxByOrNull { it.second }
        ?.first

    /**
     * Sends REQUEST_FW_PLUS_VERSION to [destNum]; the answer arrives asynchronously via [fwPlusVersionFlow]. This is a
     * custom "firmware edition" version number (see `FwPlusVersion` in ondemand.proto) maintained by this fork's own
     * firmware, distinct from official Meshtastic semver -- other firmware never answers it.
     */
    open suspend fun requestFwPlusVersion(destNum: Int) {
        radioController.requestOnDemand(destNum, OnDemandType.REQUEST_FW_PLUS_VERSION)
    }

    /**
     * The most recently logged RESPONSE_FW_PLUS_VERSION's `version_number` for [destNum], or `null` when none has been
     * received yet -- either it hasn't been queried this session, or the connected firmware doesn't implement fw+
     * versioning at all (official Meshtastic firmware, or an fw+ build predating this query).
     */
    open fun fwPlusVersionFlow(destNum: Int): Flow<Int?> =
        meshLogRepository.getLogsFrom(destNum, ON_DEMAND_PORT_NUM).map(::decodeLatestFwPlusVersion)

    private fun decodeLatestFwPlusVersion(logs: List<MeshLog>): Int? = logs
        .mapNotNull { log ->
            log.fromRadio
                ?.packet
                ?.decoded
                ?.payload
                ?.let { payload -> runCatching { OnDemand.ADAPTER.decode(payload) }.getOrNull() }
                ?.response
                ?.takeIf { it.response_type == OnDemandType.RESPONSE_FW_PLUS_VERSION }
                ?.fw_plus_version
                ?.version_number
                ?.let { versionNumber -> versionNumber to log.received_date }
        }
        .maxByOrNull { it.second }
        ?.first

    companion object {
        /**
         * Minimum fw+ `version_number` (see [requestFwPlusVersion]/[fwPlusVersionFlow]) at which Sniffer is considered
         * supported -- agreed with firmware at 2 (not the originally-discussed 3), to preserve release-numbering
         * continuity.
         */
        const val MIN_FW_PLUS_VERSION_FOR_SNIFFER = 2
    }
}
