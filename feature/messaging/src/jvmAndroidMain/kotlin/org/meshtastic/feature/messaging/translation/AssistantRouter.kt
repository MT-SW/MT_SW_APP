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

    /**
     * The kind of [question], or null on failure. [conversation] is the recent exchange (oldest first), [notes] what is
     * remembered about the user from earlier conversations (empty when memory is off) and [lastNode] the node talked
     * about last, so short follow-ups like "a jaka jest tam bateria?" can be understood.
     */
    suspend fun understand(
        modelId: String,
        question: String,
        conversation: String = "",
        notes: String = "",
        lastNode: String = "",
    ): AssistantIntent? {
        val ask =
            buildString {
                if (notes.isNotBlank()) append("Notes about the user: ").append(notes.take(NOTES_CHARS).replace('\n', ' ')).append('\n')
                if (lastNode.isNotBlank()) append("Last node talked about: ").append(lastNode.take(LAST_NODE_CHARS)).append('\n')
                if (conversation.isNotBlank()) append("Conversation so far:\n").append(conversation.takeLast(CONVERSATION_CHARS))
                append("Question: ").append(question.take(QUESTION_CHARS))
            }
        val raw =
            try {
                engine.chat(
                    modelId = modelId,
                    messages = listOf("system" to RULES, "user" to ask),
                    maxTokens = MAX_TOKENS,
                    temperature = TEMPERATURE,
                    // The forced JSON format of the library is not used: on the graphics chip it left the model's memory
                    // locked and the next answers failed. The model writes the JSON well from the examples, and the
                    // code below accepts only known kinds, so a wrong answer only falls back to the old way.
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
        fun known(key: String, allowed: List<String>): String = text(key).lowercase().takeIf { it in allowed }.orEmpty()
        // A summary of "the network" is about the nodes, not about messages; a kind written into a name field is a slip.
        val channel = text("channel").takeUnless { it in INTENTS }.orEmpty()
        if (kind == "summarize" && (text("topic").isNotBlank() || text("channel") in INTENTS || text("channel").lowercase() in NETWORK_WORDS)) {
            return AssistantIntent(intent = "network_question", topic = "overview")
        }
        return AssistantIntent(
            intent = kind,
            node = text("node"),
            channel = channel,
            text = text("text"),
            role = known("role", ROLES),
            metric = known("metric", METRICS),
            topic = known("topic", TOPICS),
            period = known("period", PERIODS),
            amount = runCatching { obj["amount"]?.jsonPrimitive?.intOrNull }.getOrNull() ?: 0,
        )
    }

    companion object {
        private const val TAG = "AssistantRouter"
        private const val MAX_TOKENS = 70
        private const val TEMPERATURE = 0.05f
        private const val NOTES_CHARS = 500
        private const val CONVERSATION_CHARS = 3500
        private const val LAST_NODE_CHARS = 60
        private const val QUESTION_CHARS = 600
        private const val LOG_CHARS = 300

        val INTENTS =
            listOf(
                "chat", "app_help", "open_screen", "show_setting", "show_node_on_map", "nearest_node", "open_link_planner",
                "draft_message", "count_messages", "search_messages", "summarize", "list_nodes", "signal_list",
                "traceroute", "neighbors", "app_status", "waypoints", "weather", "network_question", "messages_to_me", "web_question",
            )
        val NETWORK_WORDS = setOf("network", "sieć", "siec", "sieci", "mesh", "całą sieć", "cała sieć")
        val ROLES = listOf("any", "router", "repeater", "sensor", "tracker")
        val METRICS =
            listOf("nearest", "farthest", "snr", "rx", "tx", "dupe", "bad", "heap", "battery", "channel_util", "uptime", "per_channel", "favorites")
        val TOPICS = listOf("node", "radio", "ranking", "settings", "activity", "overview", "other")
        val PERIODS = listOf("none", "today", "yesterday", "minutes", "hours", "days", "weeks", "months", "years", "all")

        private fun enumOf(values: List<String>) = values.joinToString(",", "[", "]") { "\"$it\"" }

        private fun str(max: Int) = """{"type":"string","maxLength":$max}"""

        /** The only shape the model may write. Only "intent" is required, so simple questions stay short and quick. */
        val SCHEMA: String =
            """{"type":"object","properties":{""" +
                """"intent":{"type":"string","enum":${enumOf(INTENTS)}},""" +
                """"node":${str(40)},""" +
                """"channel":${str(40)},""" +
                """"text":${str(200)},""" +
                """"role":{"type":"string","enum":${enumOf(ROLES)}},""" +
                """"metric":{"type":"string","enum":${enumOf(METRICS)}},""" +
                """"topic":{"type":"string","enum":${enumOf(TOPICS)}},""" +
                """"period":{"type":"string","enum":${enumOf(PERIODS)}},""" +
                """"amount":{"type":"integer","minimum":0,"maximum":1000}""" +
                """},"required":["intent"],"additionalProperties":false}"""

        private const val RULES =
            "You sort questions for the assistant of MT_SW, an app for Meshtastic LoRa mesh radios. The question is " +
                "usually in Polish. Answer ONLY with one JSON object on one line, nothing else: the kind of question in \"intent\" and only the " +
                "fields that the question really gives. Copy node and channel names exactly as the user wrote them. " +
                "Use the conversation so far only for a short follow-up like \"a wczoraj?\" or \"a jaka jest tam bateria?\", or when the user only gives a name, a place, a time or a number: then " +
                "keep the kind of the earlier question and fill in the new information. When the user says \"there\", \"it\", \"that node\" (\"tam\", \"ten węzeł\") without a name, node = the Last node talked about. " +
                "When the last answer says a node is unknown and the user now gives its name, ask the earlier question again with that name. " +
                "The notes about the user may name the user's own nodes, places or channels: when the question says \"my node at home\" or similar and the notes name it, use that name.\n" +
                "Kinds:\n" +
                "chat: talk, greetings, opinions, advice, radio, antennas, LoRa or Meshtastic in general; anything not about this app or the user's own data.\n" +
                "web_question: a question about facts that can be looked up: rules and laws, frequencies allowed, prices, news, dates, products. text = short search words.\n" +
                "app_help: how to use the app, where something is, how to change something, what a feature, screen or setting of the app is or does (also MT_SW features, the translator, the assistant), what is new, questions about the MT_SW project. text = the setting or feature.\n" +
                "open_screen: the user tells you to open or go to a screen (settings page, map, nodes, messages, channels, firmware, ...). text = the screen.\n" +
                "show_setting: the user tells you to open or show one setting. text = the setting.\n" +
                "show_node_on_map: show, find or locate one node on the map, where a node is. node = its name.\n" +
                "nearest_node: the user tells you to SHOW the nearest or farthest node (on the map). role, metric = nearest or farthest. A question like which node is farthest is signal_list.\n" +
                "open_link_planner: range, radio link or terrain between the user and a node. node.\n" +
                "draft_message: write, send or prepare a message. channel = channel name or number, or a node name for a private message (leave out for the main channel); text = the message itself, ready to send.\n" +
                "count_messages: how many messages, who writes most. channel, period, amount; metric = per_channel for which channel has most messages.\n" +
                "search_messages: did someone write about something, who wrote a word, find a message. text = the word (its stem), channel.\n" +
                "summarize: what is being said in MESSAGES, summary of a channel or of a private chat with a node. channel = channel or node name. A summary of the network itself is network_question with topic overview.\n" +
                "messages_to_me: who wrote to me, the latest private messages to me.\n" +
                "list_nodes: how many nodes there are, which or how many were heard or active in a time, nodes with low battery (metric = battery), favourite nodes (metric = favorites). period, amount.\n" +
                "signal_list: which nodes are nearest, farthest or have the best signal, or the signal (SNR, RSSI) of one node. role, metric = nearest, farthest or snr; node = the node when the question is about one node.\n" +
                "traceroute: the route packets take to a node. node.\n" +
                "neighbors: who hears whom, the neighbours of a node. node.\n" +
                "app_status: connection to the radio, unread messages, whether my message was delivered.\n" +
                "waypoints: waypoints, map pins.\n" +
                "weather: weather, temperature, humidity, pressure, sensors of the nodes.\n" +
                "network_question: any other question about the user's own network, nodes, radio, statistics or settings. topic = node (one node, node = its name, \"me\" for the user's own), radio (statistics of the user's own radio: packets, memory, uptime, channel use), ranking (metric), settings, activity (who was heard recently or is silent), overview, other.\n" +
                "period: today, yesterday, or minutes/hours/days/weeks/months/years with amount (\"ostatnie 3 dni\" = days, 3); all = everything saved.\n" +
                "Examples:\n" +
                "cześć, co tam? -> {\"intent\":\"chat\"}\n" +
                "jak zrobić antenę dipol na 868 MHz? -> {\"intent\":\"chat\"}\n" +
                "gdzie zmienię region? -> {\"intent\":\"app_help\",\"text\":\"region\"}\n" +
                "co to jest Funkcje MT_SW? -> {\"intent\":\"app_help\",\"text\":\"Funkcje MT_SW\"}\n" +
                "kto może nadawać na 868 MHz w Polsce? -> {\"intent\":\"web_question\",\"text\":\"868 MHz kto może nadawać Polska\"}\n" +
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
                "podsumuj mi całą sieć -> {\"intent\":\"network_question\",\"topic\":\"overview\"}\n" +
                "kto pisał do mnie ostatnio? -> {\"intent\":\"messages_to_me\"}\n" +
                "o czym rozmawiali na kanale MT_SW? -> {\"intent\":\"summarize\",\"channel\":\"MT_SW\"}\n" +
                "ile węzłów było aktywnych przez 3 godziny? -> {\"intent\":\"list_nodes\",\"period\":\"hours\",\"amount\":3}\n" +
                "które węzły mają słabą baterię? -> {\"intent\":\"list_nodes\",\"metric\":\"battery\"}\n" +
                "jaka jest teraz temperatura? -> {\"intent\":\"weather\"}\n" +
                "przez które węzły idą pakiety do Łysicy? -> {\"intent\":\"traceroute\",\"node\":\"Łysica\"}\n" +
                "czy jestem połączony z radiem? -> {\"intent\":\"app_status\"}\n" +
                "pokaż statystyki mojego radia -> {\"intent\":\"network_question\",\"topic\":\"radio\"}\n" +
                "jaki jest poziom baterii mojego węzła? -> {\"intent\":\"network_question\",\"node\":\"me\",\"metric\":\"battery\",\"topic\":\"node\"}\n" +
                "jak się czuje mój węzeł? -> {\"intent\":\"network_question\",\"node\":\"me\",\"topic\":\"node\"}\n" +
                "który węzeł odebrał najwięcej pakietów? -> {\"intent\":\"network_question\",\"metric\":\"rx\",\"topic\":\"ranking\"}\n" +
                "show me the nearest node -> {\"intent\":\"nearest_node\",\"metric\":\"nearest\"}\n" +
                "Conversation so far: User: sprawdź napięcie węzła na Karczówce / Assistant: Nie znam węzła: Karczówce / Question: nazywa się Karczowka -> " +
                "{\"intent\":\"network_question\",\"node\":\"Karczowka\",\"metric\":\"voltage\",\"topic\":\"node\"}"
    }
}
