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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * What the user wants, as the model understood it. Empty texts mean "not given". The code, not the model, then finds
 * the data, counts, opens the place in the app or lets the model answer in its own words.
 */
internal data class AssistantIntent(
    val intent: String,
    val node: String = "",
    val channel: String = "",
    val text: String = "",
    val role: String = "",
    val metric: String = "",
    val topic: String = "",
    val period: String = "",
    val amount: Int = 0,
)

/**
 * Understands the question with one short, quick run of the model whose answer is forced into a fixed JSON shape: the
 * model can only pick one of the known kinds of question and fill a few fields, so it cannot write a broken tool call or
 * invent a kind that does not exist. This replaces guessing the topic from fragments of words.
 */
internal class AssistantRouter(private val engine: LiteRtEngine) {

    /** The kind of [question] (with [previous], the user's question before it, for short follow-ups), or null on failure. */
    suspend fun understand(modelId: String, question: String, previous: String): AssistantIntent? {
        val ask =
            buildString {
                if (previous.isNotBlank()) append("Previous question: ").append(previous.take(PREVIOUS_CHARS)).append('\n')
                append("Question: ").append(question.take(QUESTION_CHARS))
            }
        val raw =
            try {
                engine.chat(
                    modelId = modelId,
                    messages = listOf("system" to RULES, "user" to ask),
                    maxTokens = MAX_TOKENS,
                    temperature = TEMPERATURE,
                    jsonSchema = SCHEMA,
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(tag = TAG, throwable = e) { "Router failed: ${e.message}" }
                return null
            }
        val intent = parse(raw)
        Logger.i(tag = TAG) { "Router: \"${question.take(LOG_CHARS)}\" -> ${raw.take(LOG_CHARS).replace("\n", " ")}" }
        return intent
    }

    private fun parse(raw: String): AssistantIntent? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val obj: JsonObject = runCatching { Json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject }.getOrNull() ?: return null
        fun text(key: String): String = runCatching { obj[key]?.jsonPrimitive?.contentOrNull }.getOrNull().orEmpty().trim()
        val kind = text("intent")
        if (kind !in INTENTS) return null
        return AssistantIntent(
            intent = kind,
            node = text("node"),
            channel = text("channel"),
            text = text("text"),
            role = text("role"),
            metric = text("metric"),
            topic = text("topic"),
            period = text("period"),
            amount = runCatching { obj["amount"]?.jsonPrimitive?.intOrNull }.getOrNull() ?: 0,
        )
    }

    companion object {
        private const val TAG = "AssistantRouter"
        private const val MAX_TOKENS = 120
        private const val TEMPERATURE = 0.05f
        private const val PREVIOUS_CHARS = 300
        private const val QUESTION_CHARS = 600
        private const val LOG_CHARS = 300

        val INTENTS =
            listOf(
                "chat", "app_help", "open_screen", "show_setting", "show_node_on_map", "nearest_node", "open_link_planner",
                "draft_message", "count_messages", "search_messages", "summarize", "list_nodes", "signal_list",
                "traceroute", "neighbors", "app_status", "waypoints", "weather", "network_question",
            )

        private fun enumOf(values: List<String>) = values.joinToString(",", "[", "]") { "\"$it\"" }

        private fun str(max: Int) = """{"type":"string","maxLength":$max}"""

        /** The only shape the model may write. Only "intent" is required, so simple questions stay short and quick. */
        val SCHEMA: String =
            """{"type":"object","properties":{""" +
                """"intent":{"type":"string","enum":${enumOf(INTENTS)}},""" +
                """"node":${str(40)},""" +
                """"channel":${str(40)},""" +
                """"text":${str(200)},""" +
                """"role":{"type":"string","enum":["any","router","repeater","sensor","tracker"]},""" +
                """"metric":{"type":"string","enum":["nearest","farthest","snr","rx","tx","dupe","bad","heap","battery","channel_util","uptime","per_channel","favorites"]},""" +
                """"topic":{"type":"string","enum":["node","ranking","settings","activity","overview","other"]},""" +
                """"period":{"type":"string","enum":["none","today","yesterday","minutes","hours","days","weeks","months","years","all"]},""" +
                """"amount":{"type":"integer","minimum":0,"maximum":1000}""" +
                """},"required":["intent"],"additionalProperties":false}"""

        private const val RULES =
            "You sort questions for the assistant of MT_SW, an app for Meshtastic LoRa mesh radios. The question is " +
                "usually in Polish. Answer ONLY with one JSON object: the kind of question in \"intent\" and only the " +
                "fields that the question really gives. Copy node and channel names exactly as the user wrote them. " +
                "Use the previous question only for a short follow-up like \"a wczoraj?\".\n" +
                "Kinds:\n" +
                "chat: talk, greetings, opinions, general knowledge, radio, antennas, LoRa or Meshtastic in general; anything not about this app or the user's own data.\n" +
                "app_help: how to use the app, where something is, how to change something, what a feature or setting does, what is new, questions about the MT_SW project. text = the setting or feature.\n" +
                "open_screen: the user tells you to open or go to a screen (settings page, map, nodes, messages, channels, firmware, ...). text = the screen.\n" +
                "show_setting: the user tells you to open or show one setting. text = the setting.\n" +
                "show_node_on_map: show, find or locate one node on the map, where a node is. node = its name.\n" +
                "nearest_node: the user tells you to SHOW the nearest or farthest node (on the map). role, metric = nearest or farthest. A question like which node is farthest is signal_list.\n" +
                "open_link_planner: range, radio link or terrain between the user and a node. node.\n" +
                "draft_message: write, send or prepare a message. channel = channel name or number, or a node name for a private message (leave out for the main channel); text = the message itself, ready to send.\n" +
                "count_messages: how many messages, who writes most. channel, period, amount; metric = per_channel for which channel has most messages.\n" +
                "search_messages: did someone write about something, who wrote a word, find a message. text = the word (its stem), channel.\n" +
                "summarize: what is being said, summary of a channel or of a private chat with a node. channel = channel or node name.\n" +
                "list_nodes: how many nodes there are, which or how many were heard or active in a time, nodes with low battery (metric = battery), favourite nodes (metric = favorites). period, amount.\n" +
                "signal_list: which nodes are nearest, farthest or have the best signal, or the signal (SNR, RSSI) of one node. role, metric = nearest, farthest or snr; node = the node when the question is about one node.\n" +
                "traceroute: the route packets take to a node. node.\n" +
                "neighbors: who hears whom, the neighbours of a node. node.\n" +
                "app_status: connection to the radio, unread messages, whether my message was delivered.\n" +
                "waypoints: waypoints, map pins.\n" +
                "weather: weather, temperature, humidity, pressure, sensors of the nodes.\n" +
                "network_question: any other question about the user's own network, nodes, radio, statistics or settings. topic = node (one node, node = its name, \"me\" for the user's own), ranking (metric), settings, activity (who was heard recently or is silent), overview, other.\n" +
                "period: today, yesterday, or minutes/hours/days/weeks/months/years with amount (\"ostatnie 3 dni\" = days, 3); all = everything saved.\n" +
                "Examples:\n" +
                "cześć, co tam? -> {\"intent\":\"chat\"}\n" +
                "jak zrobić antenę dipol na 868 MHz? -> {\"intent\":\"chat\"}\n" +
                "gdzie zmienię region? -> {\"intent\":\"app_help\",\"text\":\"region\"}\n" +
                "co nowego w aplikacji? -> {\"intent\":\"app_help\",\"text\":\"nowości\"}\n" +
                "otwórz ustawienia LoRa -> {\"intent\":\"open_screen\",\"text\":\"lora\"}\n" +
                "pokaż mi limit przeskoków -> {\"intent\":\"show_setting\",\"text\":\"hop limit\"}\n" +
                "pokaż węzeł Łysica na mapie -> {\"intent\":\"show_node_on_map\",\"node\":\"Łysica\"}\n" +
                "gdzie jest SW-Kielce? -> {\"intent\":\"show_node_on_map\",\"node\":\"SW-Kielce\"}\n" +
                "pokaż najbliższy router -> {\"intent\":\"nearest_node\",\"role\":\"router\",\"metric\":\"nearest\"}\n" +
                "który węzeł jest najdalej ode mnie? -> {\"intent\":\"signal_list\",\"metric\":\"farthest\"}\n" +
                "jaki jest średni SNR węzła Telegraf? -> {\"intent\":\"signal_list\",\"node\":\"Telegraf\",\"metric\":\"snr\"}\n" +
                "ile mam węzłów w sieci? -> {\"intent\":\"list_nodes\"}\n" +
                "czy mam zasięg do Chełmowej? -> {\"intent\":\"open_link_planner\",\"node\":\"Chełmowa\"}\n" +
                "napisz na kanale MT_SW, że spotkanie jest o 18 -> {\"intent\":\"draft_message\",\"channel\":\"MT_SW\",\"text\":\"Spotkanie jest o 18.\"}\n" +
                "ile wiadomości było wczoraj na kanale 1? -> {\"intent\":\"count_messages\",\"channel\":\"1\",\"period\":\"yesterday\"}\n" +
                "kto pisał najwięcej przez ostatni tydzień? -> {\"intent\":\"count_messages\",\"period\":\"weeks\",\"amount\":1}\n" +
                "czy ktoś pisał o burzy? -> {\"intent\":\"search_messages\",\"text\":\"burz\"}\n" +
                "o czym rozmawiali na kanale MT_SW? -> {\"intent\":\"summarize\",\"channel\":\"MT_SW\"}\n" +
                "ile węzłów było aktywnych przez 3 godziny? -> {\"intent\":\"list_nodes\",\"period\":\"hours\",\"amount\":3}\n" +
                "które węzły mają słabą baterię? -> {\"intent\":\"list_nodes\",\"metric\":\"battery\"}\n" +
                "jaka jest teraz temperatura? -> {\"intent\":\"weather\"}\n" +
                "przez które węzły idą pakiety do Łysicy? -> {\"intent\":\"traceroute\",\"node\":\"Łysica\"}\n" +
                "czy jestem połączony z radiem? -> {\"intent\":\"app_status\"}\n" +
                "jak się czuje mój węzeł? -> {\"intent\":\"network_question\",\"node\":\"me\",\"topic\":\"node\"}\n" +
                "który węzeł odebrał najwięcej pakietów? -> {\"intent\":\"network_question\",\"metric\":\"rx\",\"topic\":\"ranking\"}\n" +
                "show me the nearest node -> {\"intent\":\"nearest_node\",\"metric\":\"nearest\"}"
    }
}
