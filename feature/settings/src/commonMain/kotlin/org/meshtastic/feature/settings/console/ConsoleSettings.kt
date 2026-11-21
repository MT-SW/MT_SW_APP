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
@file:Suppress("TooManyFunctions", "LongMethod")

package org.meshtastic.feature.settings.console

import org.meshtastic.proto.Config
import org.meshtastic.proto.LocalConfig
import org.meshtastic.proto.LocalModuleConfig
import org.meshtastic.proto.ModuleConfig

/** A change of one section of the configuration of the device. */
sealed interface ConsoleChange {
    data class Cfg(val config: Config) : ConsoleChange

    data class Module(val config: ModuleConfig) : ConsoleChange
}

/** One setting of the device as the Meshtastic command line names it (`lora.region`, `device.role`, …). */
class ConsoleSetting(
    val name: String,
    val read: (LocalConfig, LocalModuleConfig) -> String,
    val write: (LocalConfig, LocalModuleConfig, String) -> ConsoleChange?,
)

private class Field<T>(val name: String, val get: (T) -> String, val set: (T, String) -> T?)

private fun <T> intField(name: String, get: (T) -> Int, set: (T, Int) -> T): Field<T> =
    Field<T>(name, { get(it).toString() }, { section, text -> text.toIntOrNull()?.let { set(section, it) } })

private fun <T> boolField(name: String, get: (T) -> Boolean, set: (T, Boolean) -> T): Field<T> =
    Field<T>(name, { get(it).toString() }, { section, text -> parseBool(text)?.let { set(section, it) } })

private fun <T> textField(name: String, get: (T) -> String, set: (T, String) -> T): Field<T> =
    Field<T>(name, { get(it) }, { section, text -> set(section, text) })

private fun <T, E : Enum<E>> enumField(name: String, values: List<E>, get: (T) -> E, set: (T, E) -> T): Field<T> =
    Field<T>(
        name,
        { get(it).name },
        { section, text ->
            val key = enumKey(text)
            values.firstOrNull { enumKey(it.name) == key }?.let { set(section, it) }
        },
    )

private fun enumKey(text: String) = ConsoleText.fold(text).replace("_", "").replace("-", "")

private fun parseBool(text: String): Boolean? = when (ConsoleText.fold(text)) {
    "true",
    "1",
    "on",
    "yes",
    "wlacz",
    "tak",
    -> true

    "false",
    "0",
    "off",
    "no",
    "wylacz",
    "nie",
    -> false

    else -> null
}

private fun <T> section(
    prefix: String,
    load: (LocalConfig, LocalModuleConfig) -> T,
    wrap: (T) -> ConsoleChange,
    fields: List<Field<T>>,
): List<ConsoleSetting> = fields.map { field ->
    ConsoleSetting(
        "$prefix.${field.name}",
        { config, module -> field.get(load(config, module)) },
        { config, module, text -> field.set(load(config, module), text)?.let(wrap) },
    )
}

private fun cfg(config: Config) = ConsoleChange.Cfg(config)

private fun module(config: ModuleConfig) = ConsoleChange.Module(config)

/** The settings the console can read and change, named like in the Meshtastic command line. */
val consoleSettings: List<ConsoleSetting> =
    section<Config.LoRaConfig>(
        "lora",
        { c, _ -> c.lora ?: Config.LoRaConfig.Builder().build() },
        { cfg(Config.Builder().also { b -> b.lora = it }.build()) },
        listOf(
            enumField<Config.LoRaConfig, Config.LoRaConfig.RegionCode>(
                "region",
                Config.LoRaConfig.RegionCode.entries,
                { it.region },
                { s, v -> s.newBuilder().also { b -> b.region = v }.build() },
            ),
            enumField<Config.LoRaConfig, Config.LoRaConfig.ModemPreset>(
                "modem_preset",
                Config.LoRaConfig.ModemPreset.entries,
                { it.modem_preset },
                { s, v -> s.newBuilder().also { b -> b.modem_preset = v }.build() },
            ),
            boolField<Config.LoRaConfig>(
                "use_preset",
                { it.use_preset },
                { s, v -> s.newBuilder().also { b -> b.use_preset = v }.build() },
            ),
            intField<Config.LoRaConfig>(
                "hop_limit",
                { it.hop_limit },
                { s, v -> s.newBuilder().also { b -> b.hop_limit = v }.build() },
            ),
            boolField<Config.LoRaConfig>(
                "tx_enabled",
                { it.tx_enabled },
                { s, v -> s.newBuilder().also { b -> b.tx_enabled = v }.build() },
            ),
            intField<Config.LoRaConfig>(
                "tx_power",
                { it.tx_power },
                { s, v -> s.newBuilder().also { b -> b.tx_power = v }.build() },
            ),
            intField<Config.LoRaConfig>(
                "channel_num",
                { it.channel_num },
                { s, v -> s.newBuilder().also { b -> b.channel_num = v }.build() },
            ),
            intField<Config.LoRaConfig>(
                "bandwidth",
                { it.bandwidth },
                { s, v -> s.newBuilder().also { b -> b.bandwidth = v }.build() },
            ),
            boolField<Config.LoRaConfig>(
                "override_duty_cycle",
                { it.override_duty_cycle },
                { s, v -> s.newBuilder().also { b -> b.override_duty_cycle = v }.build() },
            ),
            boolField<Config.LoRaConfig>(
                "ignore_mqtt",
                { it.ignore_mqtt },
                { s, v -> s.newBuilder().also { b -> b.ignore_mqtt = v }.build() },
            ),
            boolField<Config.LoRaConfig>(
                "config_ok_to_mqtt",
                { it.config_ok_to_mqtt },
                { s, v -> s.newBuilder().also { b -> b.config_ok_to_mqtt = v }.build() },
            ),
        ),
    ) +
        section<Config.DeviceConfig>(
            "device",
            { c, _ -> c.device ?: Config.DeviceConfig.Builder().build() },
            { cfg(Config.Builder().also { b -> b.device = it }.build()) },
            listOf(
                enumField<Config.DeviceConfig, Config.DeviceConfig.Role>(
                    "role",
                    Config.DeviceConfig.Role.entries,
                    { it.role },
                    { s, v -> s.newBuilder().also { b -> b.role = v }.build() },
                ),
                enumField<Config.DeviceConfig, Config.DeviceConfig.RebroadcastMode>(
                    "rebroadcast_mode",
                    Config.DeviceConfig.RebroadcastMode.entries,
                    { it.rebroadcast_mode },
                    { s, v -> s.newBuilder().also { b -> b.rebroadcast_mode = v }.build() },
                ),
                intField<Config.DeviceConfig>(
                    "node_info_broadcast_secs",
                    { it.node_info_broadcast_secs },
                    { s, v -> s.newBuilder().also { b -> b.node_info_broadcast_secs = v }.build() },
                ),
                intField<Config.DeviceConfig>(
                    "button_gpio",
                    { it.button_gpio },
                    { s, v -> s.newBuilder().also { b -> b.button_gpio = v }.build() },
                ),
                intField<Config.DeviceConfig>(
                    "buzzer_gpio",
                    { it.buzzer_gpio },
                    { s, v -> s.newBuilder().also { b -> b.buzzer_gpio = v }.build() },
                ),
                boolField<Config.DeviceConfig>(
                    "disable_triple_click",
                    { it.disable_triple_click },
                    { s, v -> s.newBuilder().also { b -> b.disable_triple_click = v }.build() },
                ),
                boolField<Config.DeviceConfig>(
                    "led_heartbeat_disabled",
                    { it.led_heartbeat_disabled },
                    { s, v -> s.newBuilder().also { b -> b.led_heartbeat_disabled = v }.build() },
                ),
                boolField<Config.DeviceConfig>(
                    "double_tap_as_button_press",
                    { it.double_tap_as_button_press },
                    { s, v -> s.newBuilder().also { b -> b.double_tap_as_button_press = v }.build() },
                ),
                textField<Config.DeviceConfig>(
                    "tzdef",
                    { it.tzdef },
                    { s, v -> s.newBuilder().also { b -> b.tzdef = v }.build() },
                ),
            ),
        ) +
        section<Config.PositionConfig>(
            "position",
            { c, _ -> c.position ?: Config.PositionConfig.Builder().build() },
            { cfg(Config.Builder().also { b -> b.position = it }.build()) },
            listOf(
                intField<Config.PositionConfig>(
                    "position_broadcast_secs",
                    { it.position_broadcast_secs },
                    { s, v -> s.newBuilder().also { b -> b.position_broadcast_secs = v }.build() },
                ),
                boolField<Config.PositionConfig>(
                    "position_broadcast_smart_enabled",
                    { it.position_broadcast_smart_enabled },
                    { s, v -> s.newBuilder().also { b -> b.position_broadcast_smart_enabled = v }.build() },
                ),
                enumField<Config.PositionConfig, Config.PositionConfig.GpsMode>(
                    "gps_mode",
                    Config.PositionConfig.GpsMode.entries,
                    { it.gps_mode },
                    { s, v -> s.newBuilder().also { b -> b.gps_mode = v }.build() },
                ),
                boolField<Config.PositionConfig>(
                    "fixed_position",
                    { it.fixed_position },
                    { s, v -> s.newBuilder().also { b -> b.fixed_position = v }.build() },
                ),
                intField<Config.PositionConfig>(
                    "gps_update_interval",
                    { it.gps_update_interval },
                    { s, v -> s.newBuilder().also { b -> b.gps_update_interval = v }.build() },
                ),
                intField<Config.PositionConfig>(
                    "broadcast_smart_minimum_distance",
                    { it.broadcast_smart_minimum_distance },
                    { s, v -> s.newBuilder().also { b -> b.broadcast_smart_minimum_distance = v }.build() },
                ),
                intField<Config.PositionConfig>(
                    "broadcast_smart_minimum_interval_secs",
                    { it.broadcast_smart_minimum_interval_secs },
                    { s, v -> s.newBuilder().also { b -> b.broadcast_smart_minimum_interval_secs = v }.build() },
                ),
            ),
        ) +
        section<Config.PowerConfig>(
            "power",
            { c, _ -> c.power ?: Config.PowerConfig.Builder().build() },
            { cfg(Config.Builder().also { b -> b.power = it }.build()) },
            listOf(
                boolField<Config.PowerConfig>(
                    "is_power_saving",
                    { it.is_power_saving },
                    { s, v -> s.newBuilder().also { b -> b.is_power_saving = v }.build() },
                ),
                intField<Config.PowerConfig>(
                    "on_battery_shutdown_after_secs",
                    { it.on_battery_shutdown_after_secs },
                    { s, v -> s.newBuilder().also { b -> b.on_battery_shutdown_after_secs = v }.build() },
                ),
                intField<Config.PowerConfig>(
                    "wait_bluetooth_secs",
                    { it.wait_bluetooth_secs },
                    { s, v -> s.newBuilder().also { b -> b.wait_bluetooth_secs = v }.build() },
                ),
                intField<Config.PowerConfig>(
                    "sds_secs",
                    { it.sds_secs },
                    { s, v -> s.newBuilder().also { b -> b.sds_secs = v }.build() },
                ),
                intField<Config.PowerConfig>(
                    "min_wake_secs",
                    { it.min_wake_secs },
                    { s, v -> s.newBuilder().also { b -> b.min_wake_secs = v }.build() },
                ),
            ),
        ) +
        section<Config.NetworkConfig>(
            "network",
            { c, _ -> c.network ?: Config.NetworkConfig.Builder().build() },
            { cfg(Config.Builder().also { b -> b.network = it }.build()) },
            listOf(
                boolField<Config.NetworkConfig>(
                    "wifi_enabled",
                    { it.wifi_enabled },
                    { s, v -> s.newBuilder().also { b -> b.wifi_enabled = v }.build() },
                ),
                textField<Config.NetworkConfig>(
                    "wifi_ssid",
                    { it.wifi_ssid },
                    { s, v -> s.newBuilder().also { b -> b.wifi_ssid = v }.build() },
                ),
                textField<Config.NetworkConfig>(
                    "wifi_psk",
                    { it.wifi_psk },
                    { s, v -> s.newBuilder().also { b -> b.wifi_psk = v }.build() },
                ),
                textField<Config.NetworkConfig>(
                    "ntp_server",
                    { it.ntp_server },
                    { s, v -> s.newBuilder().also { b -> b.ntp_server = v }.build() },
                ),
                boolField<Config.NetworkConfig>(
                    "eth_enabled",
                    { it.eth_enabled },
                    { s, v -> s.newBuilder().also { b -> b.eth_enabled = v }.build() },
                ),
                textField<Config.NetworkConfig>(
                    "rsyslog_server",
                    { it.rsyslog_server },
                    { s, v -> s.newBuilder().also { b -> b.rsyslog_server = v }.build() },
                ),
            ),
        ) +
        section<Config.DisplayConfig>(
            "display",
            { c, _ -> c.display ?: Config.DisplayConfig.Builder().build() },
            { cfg(Config.Builder().also { b -> b.display = it }.build()) },
            listOf(
                intField<Config.DisplayConfig>(
                    "screen_on_secs",
                    { it.screen_on_secs },
                    { s, v -> s.newBuilder().also { b -> b.screen_on_secs = v }.build() },
                ),
                intField<Config.DisplayConfig>(
                    "auto_screen_carousel_secs",
                    { it.auto_screen_carousel_secs },
                    { s, v -> s.newBuilder().also { b -> b.auto_screen_carousel_secs = v }.build() },
                ),
                boolField<Config.DisplayConfig>(
                    "flip_screen",
                    { it.flip_screen },
                    { s, v -> s.newBuilder().also { b -> b.flip_screen = v }.build() },
                ),
                enumField<Config.DisplayConfig, Config.DisplayConfig.DisplayUnits>(
                    "units",
                    Config.DisplayConfig.DisplayUnits.entries,
                    { it.units },
                    { s, v -> s.newBuilder().also { b -> b.units = v }.build() },
                ),
                enumField<Config.DisplayConfig, Config.DisplayConfig.DisplayMode>(
                    "displaymode",
                    Config.DisplayConfig.DisplayMode.entries,
                    { it.displaymode },
                    { s, v -> s.newBuilder().also { b -> b.displaymode = v }.build() },
                ),
                boolField<Config.DisplayConfig>(
                    "heading_bold",
                    { it.heading_bold },
                    { s, v -> s.newBuilder().also { b -> b.heading_bold = v }.build() },
                ),
                boolField<Config.DisplayConfig>(
                    "wake_on_tap_or_motion",
                    { it.wake_on_tap_or_motion },
                    { s, v -> s.newBuilder().also { b -> b.wake_on_tap_or_motion = v }.build() },
                ),
                boolField<Config.DisplayConfig>(
                    "compass_north_top",
                    { it.compass_north_top },
                    { s, v -> s.newBuilder().also { b -> b.compass_north_top = v }.build() },
                ),
                boolField<Config.DisplayConfig>(
                    "use_long_node_name",
                    { it.use_long_node_name },
                    { s, v -> s.newBuilder().also { b -> b.use_long_node_name = v }.build() },
                ),
                boolField<Config.DisplayConfig>(
                    "enable_message_bubbles",
                    { it.enable_message_bubbles },
                    { s, v -> s.newBuilder().also { b -> b.enable_message_bubbles = v }.build() },
                ),
            ),
        ) +
        section<Config.BluetoothConfig>(
            "bluetooth",
            { c, _ -> c.bluetooth ?: Config.BluetoothConfig.Builder().build() },
            { cfg(Config.Builder().also { b -> b.bluetooth = it }.build()) },
            listOf(
                boolField<Config.BluetoothConfig>(
                    "enabled",
                    { it.enabled },
                    { s, v -> s.newBuilder().also { b -> b.enabled = v }.build() },
                ),
                enumField<Config.BluetoothConfig, Config.BluetoothConfig.PairingMode>(
                    "mode",
                    Config.BluetoothConfig.PairingMode.entries,
                    { it.mode },
                    { s, v -> s.newBuilder().also { b -> b.mode = v }.build() },
                ),
            ),
        ) +
        section<ModuleConfig.MQTTConfig>(
            "mqtt",
            { _, m -> m.mqtt ?: ModuleConfig.MQTTConfig.Builder().build() },
            { module(ModuleConfig.Builder().also { b -> b.mqtt = it }.build()) },
            listOf(
                boolField<ModuleConfig.MQTTConfig>(
                    "enabled",
                    { it.enabled },
                    { s, v -> s.newBuilder().also { b -> b.enabled = v }.build() },
                ),
                textField<ModuleConfig.MQTTConfig>(
                    "address",
                    { it.address },
                    { s, v -> s.newBuilder().also { b -> b.address = v }.build() },
                ),
                textField<ModuleConfig.MQTTConfig>(
                    "username",
                    { it.username },
                    { s, v -> s.newBuilder().also { b -> b.username = v }.build() },
                ),
                textField<ModuleConfig.MQTTConfig>(
                    "password",
                    { it.password },
                    { s, v -> s.newBuilder().also { b -> b.password = v }.build() },
                ),
                textField<ModuleConfig.MQTTConfig>(
                    "root",
                    { it.root },
                    { s, v -> s.newBuilder().also { b -> b.root = v }.build() },
                ),
                boolField<ModuleConfig.MQTTConfig>(
                    "encryption_enabled",
                    { it.encryption_enabled },
                    { s, v -> s.newBuilder().also { b -> b.encryption_enabled = v }.build() },
                ),
                boolField<ModuleConfig.MQTTConfig>(
                    "json_enabled",
                    { it.json_enabled },
                    { s, v -> s.newBuilder().also { b -> b.json_enabled = v }.build() },
                ),
                boolField<ModuleConfig.MQTTConfig>(
                    "tls_enabled",
                    { it.tls_enabled },
                    { s, v -> s.newBuilder().also { b -> b.tls_enabled = v }.build() },
                ),
                boolField<ModuleConfig.MQTTConfig>(
                    "proxy_to_client_enabled",
                    { it.proxy_to_client_enabled },
                    { s, v -> s.newBuilder().also { b -> b.proxy_to_client_enabled = v }.build() },
                ),
            ),
        ) +
        section<ModuleConfig.TelemetryConfig>(
            "telemetry",
            { _, m -> m.telemetry ?: ModuleConfig.TelemetryConfig.Builder().build() },
            { module(ModuleConfig.Builder().also { b -> b.telemetry = it }.build()) },
            listOf(
                intField<ModuleConfig.TelemetryConfig>(
                    "device_update_interval",
                    { it.device_update_interval },
                    { s, v -> s.newBuilder().also { b -> b.device_update_interval = v }.build() },
                ),
                intField<ModuleConfig.TelemetryConfig>(
                    "environment_update_interval",
                    { it.environment_update_interval },
                    { s, v -> s.newBuilder().also { b -> b.environment_update_interval = v }.build() },
                ),
                boolField<ModuleConfig.TelemetryConfig>(
                    "environment_measurement_enabled",
                    { it.environment_measurement_enabled },
                    { s, v -> s.newBuilder().also { b -> b.environment_measurement_enabled = v }.build() },
                ),
                boolField<ModuleConfig.TelemetryConfig>(
                    "environment_screen_enabled",
                    { it.environment_screen_enabled },
                    { s, v -> s.newBuilder().also { b -> b.environment_screen_enabled = v }.build() },
                ),
                boolField<ModuleConfig.TelemetryConfig>(
                    "environment_display_fahrenheit",
                    { it.environment_display_fahrenheit },
                    { s, v -> s.newBuilder().also { b -> b.environment_display_fahrenheit = v }.build() },
                ),
                boolField<ModuleConfig.TelemetryConfig>(
                    "air_quality_enabled",
                    { it.air_quality_enabled },
                    { s, v -> s.newBuilder().also { b -> b.air_quality_enabled = v }.build() },
                ),
                intField<ModuleConfig.TelemetryConfig>(
                    "air_quality_interval",
                    { it.air_quality_interval },
                    { s, v -> s.newBuilder().also { b -> b.air_quality_interval = v }.build() },
                ),
                boolField<ModuleConfig.TelemetryConfig>(
                    "power_measurement_enabled",
                    { it.power_measurement_enabled },
                    { s, v -> s.newBuilder().also { b -> b.power_measurement_enabled = v }.build() },
                ),
                intField<ModuleConfig.TelemetryConfig>(
                    "power_update_interval",
                    { it.power_update_interval },
                    { s, v -> s.newBuilder().also { b -> b.power_update_interval = v }.build() },
                ),
            ),
        )

/** Finds a setting by name; the name may be written without the dots' case, e.g. `LoRa.Region`. */
fun findSetting(name: String): ConsoleSetting? = consoleSettings.firstOrNull {
    it.name.equals(name.trim(), ignoreCase = true)
}
