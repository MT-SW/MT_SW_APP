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

private fun <T> intField(name: String, get: (T) -> Int, set: (T, Int) -> T) =
    Field(name, { get(it).toString() }, { section, text -> text.toIntOrNull()?.let { set(section, it) } })

private fun <T> boolField(name: String, get: (T) -> Boolean, set: (T, Boolean) -> T) =
    Field(name, { get(it).toString() }, { section, text -> parseBool(text)?.let { set(section, it) } })

private fun <T> textField(name: String, get: (T) -> String, set: (T, String) -> T) =
    Field(name, { get(it) }, { section, text -> set(section, text) })

private fun <T, E : Enum<E>> enumField(name: String, values: List<E>, get: (T) -> E, set: (T, E) -> T) =
    Field(
        name,
        { get(it).name },
        { section, text ->
            val key = enumKey(text)
            values.firstOrNull { enumKey(it.name) == key }?.let { set(section, it) }
        },
    )

private fun enumKey(text: String) = ConsoleText.fold(text).replace("_", "").replace("-", "")

private fun parseBool(text: String): Boolean? = when (ConsoleText.fold(text)) {
    "true", "1", "on", "yes", "wlacz", "tak" -> true
    "false", "0", "off", "no", "wylacz", "nie" -> false
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
        { c, _ -> c.lora ?: Config.LoRaConfig() },
        { cfg(Config(lora = it)) },
        listOf(
            enumField("region", Config.LoRaConfig.RegionCode.entries, { it.region }, { s, v -> s.copy(region = v) }),
            enumField(
                "modem_preset",
                Config.LoRaConfig.ModemPreset.entries,
                { it.modem_preset },
                { s, v -> s.copy(modem_preset = v) },
            ),
            boolField("use_preset", { it.use_preset }, { s, v -> s.copy(use_preset = v) }),
            intField("hop_limit", { it.hop_limit }, { s, v -> s.copy(hop_limit = v) }),
            boolField("tx_enabled", { it.tx_enabled }, { s, v -> s.copy(tx_enabled = v) }),
            intField("tx_power", { it.tx_power }, { s, v -> s.copy(tx_power = v) }),
            intField("channel_num", { it.channel_num }, { s, v -> s.copy(channel_num = v) }),
            intField("bandwidth", { it.bandwidth }, { s, v -> s.copy(bandwidth = v) }),
            boolField("override_duty_cycle", { it.override_duty_cycle }, { s, v -> s.copy(override_duty_cycle = v) }),
            boolField("ignore_mqtt", { it.ignore_mqtt }, { s, v -> s.copy(ignore_mqtt = v) }),
            boolField("config_ok_to_mqtt", { it.config_ok_to_mqtt }, { s, v -> s.copy(config_ok_to_mqtt = v) }),
        ),
    ) +
        section<Config.DeviceConfig>(
            "device",
            { c, _ -> c.device ?: Config.DeviceConfig() },
            { cfg(Config(device = it)) },
            listOf(
                enumField("role", Config.DeviceConfig.Role.entries, { it.role }, { s, v -> s.copy(role = v) }),
                enumField(
                    "rebroadcast_mode",
                    Config.DeviceConfig.RebroadcastMode.entries,
                    { it.rebroadcast_mode },
                    { s, v -> s.copy(rebroadcast_mode = v) },
                ),
                intField(
                    "node_info_broadcast_secs",
                    { it.node_info_broadcast_secs },
                    { s, v -> s.copy(node_info_broadcast_secs = v) },
                ),
                intField("button_gpio", { it.button_gpio }, { s, v -> s.copy(button_gpio = v) }),
                intField("buzzer_gpio", { it.buzzer_gpio }, { s, v -> s.copy(buzzer_gpio = v) }),
                boolField(
                    "disable_triple_click",
                    { it.disable_triple_click },
                    { s, v -> s.copy(disable_triple_click = v) },
                ),
                boolField(
                    "led_heartbeat_disabled",
                    { it.led_heartbeat_disabled },
                    { s, v -> s.copy(led_heartbeat_disabled = v) },
                ),
                boolField(
                    "double_tap_as_button_press",
                    { it.double_tap_as_button_press },
                    { s, v -> s.copy(double_tap_as_button_press = v) },
                ),
                textField("tzdef", { it.tzdef }, { s, v -> s.copy(tzdef = v) }),
            ),
        ) +
        section<Config.PositionConfig>(
            "position",
            { c, _ -> c.position ?: Config.PositionConfig() },
            { cfg(Config(position = it)) },
            listOf(
                intField(
                    "position_broadcast_secs",
                    { it.position_broadcast_secs },
                    { s, v -> s.copy(position_broadcast_secs = v) },
                ),
                boolField(
                    "position_broadcast_smart_enabled",
                    { it.position_broadcast_smart_enabled },
                    { s, v -> s.copy(position_broadcast_smart_enabled = v) },
                ),
                enumField(
                    "gps_mode",
                    Config.PositionConfig.GpsMode.entries,
                    { it.gps_mode },
                    { s, v -> s.copy(gps_mode = v) },
                ),
                boolField("fixed_position", { it.fixed_position }, { s, v -> s.copy(fixed_position = v) }),
                intField("gps_update_interval", { it.gps_update_interval }, { s, v -> s.copy(gps_update_interval = v) }),
                intField(
                    "broadcast_smart_minimum_distance",
                    { it.broadcast_smart_minimum_distance },
                    { s, v -> s.copy(broadcast_smart_minimum_distance = v) },
                ),
                intField(
                    "broadcast_smart_minimum_interval_secs",
                    { it.broadcast_smart_minimum_interval_secs },
                    { s, v -> s.copy(broadcast_smart_minimum_interval_secs = v) },
                ),
            ),
        ) +
        section<Config.PowerConfig>(
            "power",
            { c, _ -> c.power ?: Config.PowerConfig() },
            { cfg(Config(power = it)) },
            listOf(
                boolField("is_power_saving", { it.is_power_saving }, { s, v -> s.copy(is_power_saving = v) }),
                intField(
                    "on_battery_shutdown_after_secs",
                    { it.on_battery_shutdown_after_secs },
                    { s, v -> s.copy(on_battery_shutdown_after_secs = v) },
                ),
                intField("wait_bluetooth_secs", { it.wait_bluetooth_secs }, { s, v -> s.copy(wait_bluetooth_secs = v) }),
                intField("sds_secs", { it.sds_secs }, { s, v -> s.copy(sds_secs = v) }),
                intField("min_wake_secs", { it.min_wake_secs }, { s, v -> s.copy(min_wake_secs = v) }),
            ),
        ) +
        section<Config.NetworkConfig>(
            "network",
            { c, _ -> c.network ?: Config.NetworkConfig() },
            { cfg(Config(network = it)) },
            listOf(
                boolField("wifi_enabled", { it.wifi_enabled }, { s, v -> s.copy(wifi_enabled = v) }),
                textField("wifi_ssid", { it.wifi_ssid }, { s, v -> s.copy(wifi_ssid = v) }),
                textField("wifi_psk", { it.wifi_psk }, { s, v -> s.copy(wifi_psk = v) }),
                textField("ntp_server", { it.ntp_server }, { s, v -> s.copy(ntp_server = v) }),
                boolField("eth_enabled", { it.eth_enabled }, { s, v -> s.copy(eth_enabled = v) }),
                textField("rsyslog_server", { it.rsyslog_server }, { s, v -> s.copy(rsyslog_server = v) }),
            ),
        ) +
        section<Config.DisplayConfig>(
            "display",
            { c, _ -> c.display ?: Config.DisplayConfig() },
            { cfg(Config(display = it)) },
            listOf(
                intField("screen_on_secs", { it.screen_on_secs }, { s, v -> s.copy(screen_on_secs = v) }),
                intField(
                    "auto_screen_carousel_secs",
                    { it.auto_screen_carousel_secs },
                    { s, v -> s.copy(auto_screen_carousel_secs = v) },
                ),
                boolField("flip_screen", { it.flip_screen }, { s, v -> s.copy(flip_screen = v) }),
                enumField(
                    "units",
                    Config.DisplayConfig.DisplayUnits.entries,
                    { it.units },
                    { s, v -> s.copy(units = v) },
                ),
                enumField(
                    "displaymode",
                    Config.DisplayConfig.DisplayMode.entries,
                    { it.displaymode },
                    { s, v -> s.copy(displaymode = v) },
                ),
                boolField("heading_bold", { it.heading_bold }, { s, v -> s.copy(heading_bold = v) }),
                boolField(
                    "wake_on_tap_or_motion",
                    { it.wake_on_tap_or_motion },
                    { s, v -> s.copy(wake_on_tap_or_motion = v) },
                ),
                boolField("compass_north_top", { it.compass_north_top }, { s, v -> s.copy(compass_north_top = v) }),
                boolField("use_long_node_name", { it.use_long_node_name }, { s, v -> s.copy(use_long_node_name = v) }),
                boolField(
                    "enable_message_bubbles",
                    { it.enable_message_bubbles },
                    { s, v -> s.copy(enable_message_bubbles = v) },
                ),
            ),
        ) +
        section<Config.BluetoothConfig>(
            "bluetooth",
            { c, _ -> c.bluetooth ?: Config.BluetoothConfig() },
            { cfg(Config(bluetooth = it)) },
            listOf(
                boolField("enabled", { it.enabled }, { s, v -> s.copy(enabled = v) }),
                enumField(
                    "mode",
                    Config.BluetoothConfig.PairingMode.entries,
                    { it.mode },
                    { s, v -> s.copy(mode = v) },
                ),
            ),
        ) +
        section<ModuleConfig.MQTTConfig>(
            "mqtt",
            { _, m -> m.mqtt ?: ModuleConfig.MQTTConfig() },
            { module(ModuleConfig(mqtt = it)) },
            listOf(
                boolField("enabled", { it.enabled }, { s, v -> s.copy(enabled = v) }),
                textField("address", { it.address }, { s, v -> s.copy(address = v) }),
                textField("username", { it.username }, { s, v -> s.copy(username = v) }),
                textField("password", { it.password }, { s, v -> s.copy(password = v) }),
                textField("root", { it.root }, { s, v -> s.copy(root = v) }),
                boolField("encryption_enabled", { it.encryption_enabled }, { s, v -> s.copy(encryption_enabled = v) }),
                boolField("json_enabled", { it.json_enabled }, { s, v -> s.copy(json_enabled = v) }),
                boolField("tls_enabled", { it.tls_enabled }, { s, v -> s.copy(tls_enabled = v) }),
                boolField(
                    "proxy_to_client_enabled",
                    { it.proxy_to_client_enabled },
                    { s, v -> s.copy(proxy_to_client_enabled = v) },
                ),
            ),
        ) +
        section<ModuleConfig.TelemetryConfig>(
            "telemetry",
            { _, m -> m.telemetry ?: ModuleConfig.TelemetryConfig() },
            { module(ModuleConfig(telemetry = it)) },
            listOf(
                intField("device_update_interval", { it.device_update_interval }, { s, v -> s.copy(device_update_interval = v) }),
                intField(
                    "environment_update_interval",
                    { it.environment_update_interval },
                    { s, v -> s.copy(environment_update_interval = v) },
                ),
                boolField(
                    "environment_measurement_enabled",
                    { it.environment_measurement_enabled },
                    { s, v -> s.copy(environment_measurement_enabled = v) },
                ),
                boolField(
                    "environment_screen_enabled",
                    { it.environment_screen_enabled },
                    { s, v -> s.copy(environment_screen_enabled = v) },
                ),
                boolField(
                    "environment_display_fahrenheit",
                    { it.environment_display_fahrenheit },
                    { s, v -> s.copy(environment_display_fahrenheit = v) },
                ),
                boolField("air_quality_enabled", { it.air_quality_enabled }, { s, v -> s.copy(air_quality_enabled = v) }),
                intField("air_quality_interval", { it.air_quality_interval }, { s, v -> s.copy(air_quality_interval = v) }),
                boolField(
                    "power_measurement_enabled",
                    { it.power_measurement_enabled },
                    { s, v -> s.copy(power_measurement_enabled = v) },
                ),
                intField("power_update_interval", { it.power_update_interval }, { s, v -> s.copy(power_update_interval = v) }),
            ),
        )

/** Finds a setting by name; the name may be written without the dots' case, e.g. `LoRa.Region`. */
fun findSetting(name: String): ConsoleSetting? =
    consoleSettings.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
