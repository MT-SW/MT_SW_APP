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

/** What a question is about; it decides which tools, instructions and knowledge the assistant gets for it. */
internal enum class AssistantTopic {
    /** Questions about the user's own network, nodes, sensors, signal, messages: data tools only. */
    DATA,

    /** Questions about how to use the app, where a setting is, what is new, or a request to open or prepare something. */
    GUIDE,

    /** Both kinds at once, or unclear: everything is available. */
    MIXED,
}

/** Picks the [AssistantTopic] of a question from its words (Polish and English). */
internal object AssistantTopics {

    fun of(question: String): AssistantTopic {
        val q = " " + question.lowercase() + " "
        val data = DATA_WORDS.any { q.contains(it) }
        val guide = GUIDE_WORDS.any { q.contains(it) }
        // A question about the project itself is answered from the manual and change list, never from live data.
        // A channel called MT_SW is not the project: questions about messages, channels, counts or nodes keep their data.
        if (PROJECT_WORDS.any { q.contains(it) } && DATA_ONLY_WORDS.none { q.contains(it) }) return AssistantTopic.GUIDE
        return when {
            data && !guide -> AssistantTopic.DATA
            guide && !data -> AssistantTopic.GUIDE
            else -> AssistantTopic.MIXED
        }
    }

    private val DATA_ONLY_WORDS =
        listOf("kanał", "kanal", "kanale", "wiadomo", "streść", "streszcz", "podsumuj", "napisa", "ile ", "kto ", "węz", "wezl", "message", "channel")

    private val PROJECT_WORDS =
        listOf(
            "projekt", "założeni", "zalozeni", "osiągni", "osiagni", "bazuje", "mt_sw", "mt sw", "mtsw", "fork",
            "czym się różni", "czym sie rozni", "co dodaliśmy", "co dodalismy", "nasz cel", "nasze cele", "our project",
            "achievement", "nasz firmware", "naszym firmware", "naszego firmware", "integracj", "home assistant", "spontaniczne", "cheaterenator", "mt-sw",
        )

    private val DATA_WORDS =
        listOf(
            "temperatur", "wilgotn", "ciśnien", "cisnien", "pogod", "czujnik", "sensor", "sygnał", "sygnal", "snr", "rssi",
            "węzł", "wezl", "node", "bateri", "najwięcej", "najwiecej", "ranking", "najbliż", "najdalej", "uptime",
            "pamięć", "pamiec", "heap", "cpu", "flash", "statystyk", "streść", "streszcz", "podsumuj", "hops", "przeskok",
            "ile ", "kto ", "który ", "ktory ", "jaka jest", "jaki jest", "jakie są", "how many", "which node", "weather",
            "humidity", "pressure", "battery", "summar", "ramek", "ramki", "najmniej", "najwyż", "najniż", "ostatnio słysz",
            "offline", "nieaktyw", "wysłał", "wyslal", "odebrał", "odebral", "duplik", "kanał", "kanal", "wiadomo", "message",
            "napisa", "pisał", "pisal", "czat", "rozmow", "ulubion", "aktywn", "dzisiaj", "wczoraj", "traceroute", "trasa", "trasy",
            "nieprzeczyta", "unread", "połączon", "dostarczon", "doszła", "waypoint", "znacznik", "pinezk", "sąsiad", "sasiad", "neighbo", "route", "kogo słyszy", "kogo slyszy",
        )

    private val GUIDE_WORDS =
        listOf(
            "jak ", "gdzie", "ustaw", "włącz", "wlacz", "wyłącz", "wylacz", "zmień", "zmien", "otwórz", "otworz", "przejdź",
            "przejdz", "pokaż mi", "pokaz mi", "na mapie", "mapie", "planer", "przygotuj", "napisz", "wyślij", "wyslij",
            "co to", "co oznacza", "do czego", "po co", "nowości", "nowosci", "nowego", "zmian", "wersj", "funkcj",
            "aplikacj", "instrukcj", "how do", "how to", "where", "enable", "disable", "open ", "setting", "draft",
            "write a", "what is", "what's new", "what does",
        )
}
