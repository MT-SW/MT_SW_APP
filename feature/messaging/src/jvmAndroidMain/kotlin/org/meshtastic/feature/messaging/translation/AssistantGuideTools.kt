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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.navigation.DEEP_LINK_BASE_URI
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.about
import org.meshtastic.core.resources.dictation_enabled_title
import org.meshtastic.core.resources.dictation_external_mic_title
import org.meshtastic.core.resources.ai_models_title
import org.meshtastic.core.resources.antenna_calc_title
import org.meshtastic.core.resources.node_color_pick
import org.meshtastic.core.resources.ai_assistant_title
import org.meshtastic.core.resources.translator_title
import org.meshtastic.core.resources.ai_unload_title
import org.meshtastic.core.resources.auto_load_chat_images
import org.meshtastic.core.resources.import_menu_in_top_bar
import org.meshtastic.core.resources.bottom_nav_settings
import org.meshtastic.core.resources.bluetooth
import org.meshtastic.core.resources.channels
import org.meshtastic.core.resources.clean_node_database_title
import org.meshtastic.core.resources.connections
import org.meshtastic.core.resources.debug_panel
import org.meshtastic.core.resources.device
import org.meshtastic.core.resources.discovery_local_mesh
import org.meshtastic.core.resources.display
import org.meshtastic.core.resources.filter_settings
import org.meshtastic.core.resources.firmware
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.help_and_documentation
import org.meshtastic.core.resources.app_log_level
import org.meshtastic.core.resources.kb_title
import org.meshtastic.core.resources.log_retention_days
import org.meshtastic.core.resources.lora
import org.meshtastic.core.resources.map
import org.meshtastic.core.resources.messages
import org.meshtastic.core.resources.mqtt
import org.meshtastic.core.resources.mt_sw_features_title
import org.meshtastic.core.resources.network
import org.meshtastic.core.resources.nodes
import org.meshtastic.core.resources.position
import org.meshtastic.core.resources.power
import org.meshtastic.core.resources.preferences_language
import org.meshtastic.core.resources.quick_chat
import org.meshtastic.core.resources.schema_bluetooth_enabled
import org.meshtastic.core.resources.schema_bluetooth_fixed_pin
import org.meshtastic.core.resources.schema_bluetooth_mode
import org.meshtastic.core.resources.schema_device_node_info_broadcast_secs
import org.meshtastic.core.resources.schema_device_rebroadcast_mode
import org.meshtastic.core.resources.schema_device_role
import org.meshtastic.core.resources.schema_device_tzdef
import org.meshtastic.core.resources.schema_display_flip_screen
import org.meshtastic.core.resources.schema_display_screen_on_secs
import org.meshtastic.core.resources.schema_display_units
import org.meshtastic.core.resources.schema_display_use_12h_clock
import org.meshtastic.core.resources.schema_lora_bandwidth
import org.meshtastic.core.resources.schema_lora_channel_num
import org.meshtastic.core.resources.schema_lora_coding_rate
import org.meshtastic.core.resources.schema_lora_hop_limit
import org.meshtastic.core.resources.schema_lora_modem_preset
import org.meshtastic.core.resources.schema_lora_override_frequency
import org.meshtastic.core.resources.schema_lora_region
import org.meshtastic.core.resources.schema_lora_spread_factor
import org.meshtastic.core.resources.schema_lora_tx_enabled
import org.meshtastic.core.resources.schema_mqtt_address
import org.meshtastic.core.resources.schema_mqtt_enabled
import org.meshtastic.core.resources.schema_mqtt_map_reporting_enabled
import org.meshtastic.core.resources.schema_mqtt_root
import org.meshtastic.core.resources.schema_network_wifi_enabled
import org.meshtastic.core.resources.schema_network_wifi_psk
import org.meshtastic.core.resources.schema_network_wifi_ssid
import org.meshtastic.core.resources.schema_position_fixed_position
import org.meshtastic.core.resources.schema_position_gps_mode
import org.meshtastic.core.resources.schema_position_position_broadcast_secs
import org.meshtastic.core.resources.schema_power_is_power_saving
import org.meshtastic.core.resources.schema_telemetry_device_update_interval
import org.meshtastic.core.resources.schema_telemetry_environment_measurement_enabled
import org.meshtastic.core.resources.schema_telemetry_environment_update_interval
import org.meshtastic.core.resources.telemetry
import org.meshtastic.core.resources.theme
import org.meshtastic.core.resources.ui_scale

/** Collects the buttons the assistant wants to show under its next answer. */
class AssistantActionCollector {
    private val actions = ArrayList<AssistantAction>()

    @Synchronized
    fun add(action: AssistantAction) {
        if (actions.size < MAX_ACTIONS && actions.none { it.uri == action.uri && it.highlight == action.highlight }) {
            actions += action
        }
    }

    @Synchronized fun size(): Int = actions.size

    /** The newest button, or null. */
    @Synchronized fun last(): AssistantAction? = actions.lastOrNull()

    /** The newest button opens by itself right after the answer (the user asked to go there). */
    @Synchronized
    fun autoOpenLast() {
        if (actions.isNotEmpty()) actions[actions.lastIndex] = actions.last().copy(auto = true)
    }

    /** Returns the collected buttons and forgets them. */
    @Synchronized
    fun drain(): List<AssistantAction> = actions.toList().also { actions.clear() }

    private companion object {
        const val MAX_ACTIONS = 4
    }
}

/**
 * Places in the app the assistant can point at: each adds a button under the answer (and, when the user asked to go
 * there, the app opens it by itself). None of them changes a setting or sends a message; a drafted message only lands
 * in the message box of the conversation and the user sends it.
 */
@Suppress("FunctionName", "LargeClass")
class AssistantGuideTools(
    private val data: AssistantDataProvider,
    private val collector: AssistantActionCollector,
) : AiToolSet {

    @AiTool(
        description =
        "Adds a button that opens a screen of the app (settings pages such as LoRa, device, display, bluetooth, network, " +
            "position, power, MQTT, telemetry, MT_SW features, or the nodes, map, messages, channels, connections, " +
            "firmware and local mesh discovery screens). Use it when the user asks where something is or wants to open a screen.",
    )
    fun open_screen(
        @AiToolParam(name = "screen", description = "Name of the screen, for example \"lora\", \"display\", \"map\", \"nodes\"") screen: String,
    ): String = run("open_screen") {
        val target = bestMatch(SCREENS, screen)
            ?: return@run "No such screen. Available: " + SCREENS.joinToString { it.key }
        val title = aiString(target.title)
        collector.add(AssistantAction(title, "$DEEP_LINK_BASE_URI/${target.path}"))
        "A button \"$title\" was added under the answer. Tell the user to tap it."
    }

    @AiTool(
        description =
        "Adds a button that opens the page of a radio or app setting and makes that setting blink with a yellow frame. " +
            "Use it for questions like where a setting is or how to change it (region, preset, hop limit, role, " +
            "screen timeout, Wi-Fi, MQTT, telemetry interval, app scale, theme, language, ...). It cannot change the setting.",
    )
    fun show_setting(
        @AiToolParam(name = "setting", description = "What the user wants to find, for example \"hop limit\", \"app scale\", \"region\"") setting: String,
    ): String = run("show_setting") {
        val target = bestMatch(SETTINGS, setting)
            ?: return@run "No such setting in the list. Explain where it is from the guide instead."
        // The caption is in the language of the answer; the blinking row is found by its title in the app's language.
        val title = aiString(target.title)
        collector.add(AssistantAction(title, "$DEEP_LINK_BASE_URI/${target.path}", highlight = getStringSuspend(target.title)))
        "A button \"$title\" was added under the answer; it opens the page and makes the setting blink. Tell the user to tap it."
    }

    @AiTool(description = "Adds a button that shows a node on the map. Give the node's short or long name, or \"me\" for the own node.")
    fun show_node_on_map(@AiToolParam(name = "node", description = "Node name") node: String): String = run("show_node_on_map") {
        val num = data.nodeNumFor(node) ?: return@run "No node called \"$node\" is known."
        val name = data.nodeNameFor(num).ifBlank { node }
        collector.add(AssistantAction(name, "$DEEP_LINK_BASE_URI/map/focus/$num"))
        "A button that shows $name on the map was added. Tell the user to tap it."
    }

    @AiTool(
        description =
        "Adds a button that opens the link planner (range and terrain calculation) on the map for a node. Use it when the " +
            "user wants to check range or a radio link to or from a node.",
    )
    fun open_link_planner(@AiToolParam(name = "node", description = "Node name, or \"me\"") node: String): String = run("open_link_planner") {
        val num = data.nodeNumFor(node) ?: return@run "No node called \"$node\" is known."
        val name = data.nodeNameFor(num).ifBlank { node }
        collector.add(AssistantAction(name, "$DEEP_LINK_BASE_URI/map/planner/$num"))
        "A button that opens the link planner for $name was added. Tell the user to tap it."
    }

    @AiTool(
        description =
        "Prepares a message: adds a button that opens the conversation with the text already in the message box. The " +
            "message is NOT sent; the user checks it and sends it himself. Use it when the user asks to write or send a message.",
    )
    fun draft_message(
        @AiToolParam(name = "text", description = "The text of the message") text: String,
        @AiToolParam(name = "to", description = "Channel name or number, or a node name for a direct message; the word main for the main channel") to: String,
    ): String = run("draft_message") {
        val message = text.trim().take(MAX_DRAFT)
        if (message.isEmpty()) return@run "The message is empty."
        val (key, label) = data.contactFor(to) ?: return@run "No channel or node called \"$to\" is known."
        collector.add(
            AssistantAction(label, "$DEEP_LINK_BASE_URI/messages?contactKey=${encode(key)}&message=${encode(message)}"),
        )
        "A button was added that opens the conversation with $label with this text ready: \"$message\". Nothing was sent; tell the user to tap the button and send it."
    }

    private class Target(val key: String, val path: String, val title: StringResource, val words: List<String>)

    private fun bestMatch(list: List<Target>, query: String): Target? {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return null
        list.firstOrNull { it.key == q }?.let { return it }
        var best: Target? = null
        var bestScore = 0
        for (t in list) {
            // A word counts only where a word of the question starts ("dni" must not match inside "odpowiednie").
            val score = t.words.count { startsWord(q, it) } * 2 + (if (t.key == q || startsWord(q, t.key)) 1 else 0)
            if (score > bestScore) {
                best = t
                bestScore = score
            }
        }
        return best
    }

    private fun startsWord(text: String, word: String): Boolean {
        var at = text.indexOf(word)
        while (at >= 0) {
            if (at == 0 || !text[at - 1].isLetterOrDigit()) return true
            at = text.indexOf(word, at + 1)
        }
        return false
    }

    /** Where a button leads, written as a path of screens, for example "Settings → LoRa". */
    suspend fun pathOf(action: AssistantAction): String {
        val path = action.uri.removePrefix("$DEEP_LINK_BASE_URI/").substringBefore('?')
        val settings = aiString(Res.string.bottom_nav_settings)
        val screen = placeOf(action)
        return when {
            path == "settings" -> settings
            path.startsWith("settings/") -> if (screen.isBlank()) settings else "$settings → $screen"
            else -> screen
        }
    }

    /** The name of the screen a button leads to (for example "MT_SW features"), or empty. */
    suspend fun placeOf(action: AssistantAction): String {
        val path = action.uri.removePrefix("$DEEP_LINK_BASE_URI/").substringBefore('?')
        return SCREENS.firstOrNull { it.path == path }?.let { aiString(it.title) }.orEmpty()
    }

    private fun run(name: String, block: suspend () -> String): String {
        Logger.i(tag = TAG) { "Guide tool $name" }
        val text =
            try {
                runBlocking(Dispatchers.Default) { block() }
            } catch (e: Exception) {
                Logger.w(tag = TAG, throwable = e) { "Guide tool $name failed" }
                "This is not available right now."
            }
        return text.take(MAX_RESULT).stripUnsafeChars()
    }

    private fun encode(value: String): String {
        val out = StringBuilder()
        for (b in value.encodeToByteArray()) {
            val c = b.toInt() and 0xFF
            val ch = c.toChar()
            if (c < 128 && (ch.isLetterOrDigit() || ch in "-._~")) {
                out.append(ch)
            } else {
                out.append('%').append(HEX[c shr 4]).append(HEX[c and 15])
            }
        }
        return out.toString()
    }

    private companion object {
        const val TAG = "AssistantGuideTools"
        const val MAX_DRAFT = 500
        const val MAX_RESULT = 600
        const val HEX = "0123456789ABCDEF"

        val SCREENS =
            listOf(
                Target("settings", "settings", Res.string.bottom_nav_settings, listOf("ustawieni", "settings")),
                Target("lora", "settings/lora", Res.string.lora, listOf("lora", "region", "preset", "częstotliw", "modem")),
                Target("device", "settings/device", Res.string.device, listOf("urządz", "device", "rola", "role")),
                Target("display", "settings/display", Res.string.display, listOf("wyświetl", "display", "ekran", "screen")),
                Target("bluetooth", "settings/bluetooth", Res.string.bluetooth, listOf("bluetooth", "parowan", "pairing")),
                Target("network", "settings/network", Res.string.network, listOf("wifi", "wi-fi", "ethernet", "network", "sieć")),
                Target("position", "settings/position", Res.string.position, listOf("gps", "pozycj", "position", "lokaliz")),
                Target("power", "settings/power", Res.string.power, listOf("zasilan", "power", "oszczędz", "bateri")),
                Target("mqtt", "settings/mqtt", Res.string.mqtt, listOf("mqtt")),
                Target("telemetry", "settings/telemetry", Res.string.telemetry, listOf("telemetri", "telemetry", "czujnik")),
                Target("antenna_calculator", "settings/antenna-calculator", Res.string.antenna_calc_title, listOf("kalkulator anten", "kalkulator", "calculator", "przelicz anten", "wymiary anten")),
                Target("mt_sw", "settings/mt-sw-features", Res.string.mt_sw_features_title, listOf("mt_sw", "mt-sw", "funkcje", "features", "skalowan", "motyw")),
                // Only the node database: "baza wiedzy" (knowledge base) must not land here.
                Target("clean_nodes", "settings/clean-node-db", Res.string.clean_node_database_title, listOf("wyczyść", "wyczysc", "clean", "baza węzł", "bazę węzł", "bazy węzł", "baza wezl", "baze wezl", "node db", "node database", "usuń węz", "usun wez")),
                Target("knowledge_base", "settings/mt-sw-features", Res.string.kb_title, listOf("baza wiedzy", "bazę wiedzy", "bazy wiedzy", "baze wiedzy", "wiedz", "knowledge")),
                Target("filter", "settings/filter-settings", Res.string.filter_settings, listOf("filtr", "filter")),
                Target("debug", "settings/debug-panel", Res.string.debug_panel, listOf("debug", "logi", "log", "diagnost")),
                Target("about", "settings/about", Res.string.about, listOf("o aplikacji", "about", "wersj")),
                Target("help", "settings/helpdocs", Res.string.help_and_documentation, listOf("pomoc", "help", "dokument", "instrukcj", "poradnik")),
                Target("nodes", "nodes", Res.string.nodes, listOf("węzł", "węzły", "node", "lista węz")),
                Target("map", "map", Res.string.map, listOf("mapa", "mapie", "map")),
                Target("messages", "messages", Res.string.messages, listOf("wiadomo", "message", "czat", "chat")),
                Target("channels", "channels", Res.string.channels, listOf("kanał", "kanały", "kanal", "channel")),
                Target("connections", "connections", Res.string.connections, listOf("połącz", "connect", "connection")),
                Target("firmware", "firmware", Res.string.firmware, listOf("firmware", "oprogramowan", "aktualizacj")),
                Target("discovery", "discovery", Res.string.discovery_local_mesh, listOf("discovery", "odkry", "skan", "lokalna sieć")),
                Target("quick_chat", "quickchat", Res.string.quick_chat, listOf("szybkie", "quick", "szablon")),
            )

        val SETTINGS =
            listOf(
                Target("region", "settings/lora", Res.string.schema_lora_region, listOf("region", "kraj", "częstotliw")),
                Target("modem_preset", "settings/lora", Res.string.schema_lora_modem_preset, listOf("preset", "modem", "prędkość", "narrow", "long fast", "predkosc", "zasięg", "zasieg")),
                Target("hop_limit", "settings/lora", Res.string.schema_lora_hop_limit, listOf("hop", "skok", "przeskok", "limit")),
                Target("tx_enabled", "settings/lora", Res.string.schema_lora_tx_enabled, listOf("nadawan", "transmit", "tx enabled")),
                Target("bandwidth", "settings/lora", Res.string.schema_lora_bandwidth, listOf("bandwidth", "szerokość pasma", "pasmo")),
                Target("spread_factor", "settings/lora", Res.string.schema_lora_spread_factor, listOf("spread", "sf")),
                Target("coding_rate", "settings/lora", Res.string.schema_lora_coding_rate, listOf("coding", "kodowan")),
                Target("channel_num", "settings/lora", Res.string.schema_lora_channel_num, listOf("slot", "numer kanału", "channel num")),
                Target("override_frequency", "settings/lora", Res.string.schema_lora_override_frequency, listOf("override", "własna częstotliw", "nadpisz")),
                Target("role", "settings/device", Res.string.schema_device_role, listOf("rola", "role", "router", "client")),
                Target("rebroadcast", "settings/device", Res.string.schema_device_rebroadcast_mode, listOf("rebroadcast", "retransmis", "powtarz")),
                Target("nodeinfo_secs", "settings/device", Res.string.schema_device_node_info_broadcast_secs, listOf("nodeinfo", "informacji o węźle", "node info")),
                Target("timezone", "settings/device", Res.string.schema_device_tzdef, listOf("strefa", "timezone", "czas")),
                Target("screen_on", "settings/display", Res.string.schema_display_screen_on_secs, listOf("podświetl", "wygaszan", "screen on", "czas ekranu", "ekran włączony")),
                Target("units", "settings/display", Res.string.schema_display_units, listOf("jednostk", "units", "metryczn", "imperial")),
                Target("flip_screen", "settings/display", Res.string.schema_display_flip_screen, listOf("odwróć", "flip", "obróć ekran")),
                Target("clock_12h", "settings/display", Res.string.schema_display_use_12h_clock, listOf("12h", "12-godzin", "zegar")),
                Target("bluetooth_enabled", "settings/bluetooth", Res.string.schema_bluetooth_enabled, listOf("bluetooth włącz", "bluetooth enabled", "włącz bluetooth")),
                Target("bluetooth_mode", "settings/bluetooth", Res.string.schema_bluetooth_mode, listOf("parowan", "pairing", "tryb parowania")),
                Target("bluetooth_pin", "settings/bluetooth", Res.string.schema_bluetooth_fixed_pin, listOf("pin", "kod")),
                Target("wifi_enabled", "settings/network", Res.string.schema_network_wifi_enabled, listOf("włącz wifi", "wifi enabled", "wifi włącz")),
                Target("wifi_ssid", "settings/network", Res.string.schema_network_wifi_ssid, listOf("ssid", "nazwa sieci", "nazwa wifi")),
                Target("wifi_password", "settings/network", Res.string.schema_network_wifi_psk, listOf("hasło wifi", "wifi password", "hasło sieci")),
                Target("gps_mode", "settings/position", Res.string.schema_position_gps_mode, listOf("gps")),
                Target("position_broadcast", "settings/position", Res.string.schema_position_position_broadcast_secs, listOf("rozgłaszan", "broadcast pozycj", "częstotliwość pozycji", "position broadcast")),
                Target("fixed_position", "settings/position", Res.string.schema_position_fixed_position, listOf("stała pozycj", "fixed position")),
                Target("power_saving", "settings/power", Res.string.schema_power_is_power_saving, listOf("oszczędzan", "power saving", "tryb oszczędn")),
                Target("mqtt_enabled", "settings/mqtt", Res.string.schema_mqtt_enabled, listOf("włącz mqtt", "mqtt enabled", "mqtt włącz")),
                Target("mqtt_address", "settings/mqtt", Res.string.schema_mqtt_address, listOf("adres mqtt", "serwer mqtt", "mqtt address", "mqtt server")),
                Target("mqtt_root", "settings/mqtt", Res.string.schema_mqtt_root, listOf("temat główny", "root topic", "mqtt root")),
                Target("mqtt_map", "settings/mqtt", Res.string.schema_mqtt_map_reporting_enabled, listOf("raport mapy", "map report")),
                Target("telemetry_device", "settings/telemetry", Res.string.schema_telemetry_device_update_interval, listOf("telemetria urządzenia", "device telemetry", "interwał telemetrii", "telemetry interval")),
                Target("telemetry_env", "settings/telemetry", Res.string.schema_telemetry_environment_measurement_enabled, listOf("środowisk", "environment", "pogod", "temperatur")),
                Target("telemetry_env_interval", "settings/telemetry", Res.string.schema_telemetry_environment_update_interval, listOf("interwał środowisk", "environment interval")),
                Target("ui_scale", "settings/mt-sw-features", Res.string.ui_scale, listOf("skalowan", "skala", "rozmiar interfejsu", "scale", "zoom", "powiększ")),
                Target("chat_images", "settings/mt-sw-features", Res.string.auto_load_chat_images, listOf("zdjęci", "obrazk", "images", "podgląd link")),
                Target("import_menu", "settings/mt-sw-features", Res.string.import_menu_in_top_bar, listOf("przycisk", "plus", "dodaj kontakt", "górn", "belk", "zasłania", "button", "top bar", "udostępnij węzeł")),
                Target("theme", "settings", Res.string.theme, listOf("motyw", "theme", "ciemny", "jasny", "dark", "light", "tryb ciemny")),
                Target("language", "settings", Res.string.preferences_language, listOf("język", "language", "jezyk")),
                Target("dictation_switch", "settings/mt-sw-features", Res.string.dictation_enabled_title, listOf("dyktowanie", "dyktuj", "dictation", "dictate", "włącz dyktowanie", "wyłącz dyktowanie", "mikrofon w czacie")),
                Target("external_mic", "settings/mt-sw-features", Res.string.dictation_external_mic_title, listOf("mikrofon", "słuchawk", "sluchawk", "headset", "microphone", "zestaw głośnomówiący", "bluetooth mikrofon")),
                Target("node_color", "settings/security", Res.string.node_color_pick, listOf("kolor", "color", "colour", "barw")),
                Target("ai_model", "settings/mt-sw-features", Res.string.ai_models_title, listOf("model ai", "wybór modelu", "wybor modelu", "model", "gemma", "nllb")),
                Target("ai_unload", "settings/mt-sw-features", Res.string.ai_unload_title, listOf("zwolnij pamięć", "zwolnij pamiec", "wyłącz model", "wylacz model", "ubij model", "pamięć ai", "pamiec ai", "ram", "free memory", "unload")),
                Target("translator", "settings/mt-sw-features", Res.string.translator_title, listOf("tłumacz", "tlumacz", "translator", "tłumaczen", "tlumaczen", "translat")),
                Target("assistant", "settings/mt-sw-features", Res.string.ai_assistant_title, listOf("asystent", "assistant", "model ai", "gemma", "sztuczn")),
                Target("knowledge_base", "settings/mt-sw-features", Res.string.kb_title, listOf("baza wiedzy", "baza", "wiedza", "wpisy", "przeglądać", "przegladac", "knowledge", "offtop", "pokrewne", "poradnik")),
                Target("app_log", "settings/mt-sw-features", Res.string.app_log_level, listOf("logi", "log", "dziennik", "zapis logów", "zapisywanie logów", "diagnoz", "awari", "wywala", "błąd", "blad", "crash", "pobrane")),
                Target("log_retention", "settings/mt-sw-features", Res.string.log_retention_days, listOf("ramk", "logi", "statystyk", "pamięt", "czas", "baz", "dni", "retention", "historia", "ile czasu")),
            )
    }
}
