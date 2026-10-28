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

/**
 * The test questions the assistant asks itself when the user types /test in its chat (the same list as
 * scripts/assistant-test-questions.md). [node] and [channel] are real names from the user's network, so the answers can
 * be checked against the app.
 */
internal object AssistantSelfTest {

    /** One test question; a [followUp] keeps the question before it in the conversation ("a wczoraj?"). */
    class Question(val text: String, val followUp: Boolean = false, val group: String = TOOLS)

    /** The commands typed in the assistant chat: /test runs everything, the others run one group of questions. */
    const val COMMAND = "/test"
    const val TOOLS = "/narzedzia"
    const val CHAT = "/rozmowa"
    const val KNOWLEDGE = "/baza"
    const val HARDWARE = "/sprzet"

    val commands: List<String> = listOf(COMMAND, TOOLS, CHAT, KNOWLEDGE, HARDWARE)

    fun isCommand(text: String): Boolean = commands.any { it.equals(text.trim(), ignoreCase = true) }

    /** Where the results are written: next to the app when it runs from the project (desktop). */
    const val RESULT_FILE = "assistant-test-results.md"

    /** The questions of one command (/test = all of them). */
    fun questions(node: String, channel: String, command: String = COMMAND): List<Question> =
        all(node, channel).filter { command.equals(COMMAND, ignoreCase = true) || it.group.equals(command, ignoreCase = true) }

    private fun all(node: String, channel: String): List<Question> =
        listOf(
            Question("Ile mam teraz węzłów w sieci?"),
            Question("Który węzeł jest najdalej ode mnie?"),
            Question("Jaki jest poziom baterii mojego węzła?"),
            Question("Które węzły słyszałem w ciągu ostatniej godziny?"),
            Question("Jaka jest średnia jakość sygnału (SNR) węzła $node?"),
            Question("Pokaż statystyki mojego radia (pakiety, pamięć)."),
            Question("Podsumuj mi całą sieć w 3 zdaniach."),
            Question("Kto pisał do mnie ostatnio?"),
            Question("Jak zmienić region LoRa?", group = KNOWLEDGE),
            Question("Gdzie ustawić limit skoków (hop limit)?", group = KNOWLEDGE),
            Question("Jak zmienić język aplikacji?", group = KNOWLEDGE),
            Question("Jak włączyć tłumacza offline?", group = KNOWLEDGE),
            Question("Jak zmienić motyw na ciemny?", group = KNOWLEDGE),
            Question("Jak powiększyć interfejs?", group = KNOWLEDGE),
            Question("Jak wyłączyć automatyczne ładowanie obrazków w czacie?", group = KNOWLEDGE),
            Question("Gdzie jest panel debugowania?", group = KNOWLEDGE),
            Question("Co to jest „Funkcje MT_SW”?", group = KNOWLEDGE),
            Question("Co nowego w ostatniej wersji?", group = KNOWLEDGE),
            Question("Jak zaktualizować firmware?", group = KNOWLEDGE),
            Question("Jak połączyć się z urządzeniem przez Bluetooth?", group = KNOWLEDGE),
            Question("Pokaż mi ustawienie regionu.", group = KNOWLEDGE),
            Question("Pokaż węzeł $node na mapie."),
            Question("Pokaż najbliższy router."),
            Question("Czy mam zasięg do $node?"),
            Question("Napisz na kanale $channel, że jestem na miejscu."),
            Question("Otwórz ustawienia wyświetlania.", group = KNOWLEDGE),
            Question("Ile wiadomości było dzisiaj na kanale $channel?"),
            Question("A wczoraj?", followUp = true),
            Question("Kto pisał najwięcej w ostatnim tygodniu?"),
            Question("Czy ktoś pisał o pogodzie?"),
            Question("Ile węzłów było aktywnych przez ostatnie 3 godziny?"),
            Question("Które węzły mają słabą baterię?"),
            Question("Cześć, jak się masz?", group = CHAT),
            Question("Jak zrobić prostą antenę dipol na 868 MHz?", group = CHAT),
            Question("Opowiedz krótko, czym różni się LoRa od Wi-Fi.", group = CHAT),
            Question("Mam słaby zasięg — co sprawdzić i pokaż mi odpowiednie ustawienie.", group = KNOWLEDGE),
            Question("Ile węzłów jest w sieci i jak zmienić w ustawieniach rolę mojego urządzenia?", group = KNOWLEDGE),
            Question("How do I change the LoRa region?", group = KNOWLEDGE),
            Question("Jaka jest stolica Francji?", group = CHAT),
            Question("Jaka jest teraz temperatura?", group = CHAT),
            Question("Jaki preset wybrać w naszym województwie?", group = KNOWLEDGE),
            Question("Jaka antena ćwierćfalowa na 869 MHz, ile ma mieć centymetrów?"),
            Question("Kto może nadawać na 868 MHz w Polsce?", group = KNOWLEDGE),
            Question("Ile to 20 dBm w miliwatach?"),
            Question("Jaki teoretyczny zasięg mam przy antenach 5 dBi?"),
            Question("Ile trwa wysłanie wiadomości 100 znaków?"),
            Question("Jaka jest strefa Fresnela na 15 km?"),
            Question("Jak daleko sięga radio z masztu 40 m?"),
            Question("Jakie EIRP przy 22 dBm i antenie 6 dBi?"),
            Question("Policz 12*7+3"),
            Question("What length should a 433 MHz dipole be?", group = CHAT),
            Question("Ktoś mnie słabo słyszy, jak poprawić zasięg?", group = CHAT),
            Question("Jaki będzie zasięg między moim węzłem a węzłem $node?"),
            Question("Policz zasięg mojego węzła przy antenie 5 dBi na wysokości 10 m."),
            Question("Oblicz długość anteny ćwierćfalowej na 433 MHz."),
            Question("Przygotuj wiadomość do kanału $channel, że wracam za godzinę (nie wysyłaj)."),
        ) + AssistantSelfTestBank.questions(node, channel)
}
