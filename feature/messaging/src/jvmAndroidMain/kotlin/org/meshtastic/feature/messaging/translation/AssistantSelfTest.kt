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
    class Question(val text: String, val followUp: Boolean = false)

    const val COMMAND = "/test"

    /** Where the results are written: next to the app when it runs from the project (desktop). */
    const val RESULT_FILE = "assistant-test-results.md"

    fun questions(node: String, channel: String): List<Question> =
        listOf(
            Question("Ile mam teraz węzłów w sieci?"),
            Question("Który węzeł jest najdalej ode mnie?"),
            Question("Jaki jest poziom baterii mojego węzła?"),
            Question("Które węzły słyszałem w ciągu ostatniej godziny?"),
            Question("Jaka jest średnia jakość sygnału (SNR) węzła $node?"),
            Question("Pokaż statystyki mojego radia (pakiety, pamięć)."),
            Question("Podsumuj mi całą sieć w 3 zdaniach."),
            Question("Kto pisał do mnie ostatnio?"),
            Question("Jak zmienić region LoRa?"),
            Question("Gdzie ustawić limit skoków (hop limit)?"),
            Question("Jak zmienić język aplikacji?"),
            Question("Jak włączyć tłumacza offline?"),
            Question("Jak zmienić motyw na ciemny?"),
            Question("Jak powiększyć interfejs?"),
            Question("Jak wyłączyć automatyczne ładowanie obrazków w czacie?"),
            Question("Gdzie jest panel debugowania?"),
            Question("Co to jest „Funkcje MT_SW”?"),
            Question("Co nowego w ostatniej wersji?"),
            Question("Jak zaktualizować firmware?"),
            Question("Jak połączyć się z urządzeniem przez Bluetooth?"),
            Question("Pokaż mi ustawienie regionu."),
            Question("Pokaż węzeł $node na mapie."),
            Question("Pokaż najbliższy router."),
            Question("Czy mam zasięg do $node?"),
            Question("Napisz na kanale $channel, że jestem na miejscu."),
            Question("Otwórz ustawienia wyświetlania."),
            Question("Ile wiadomości było dzisiaj na kanale $channel?"),
            Question("A wczoraj?", followUp = true),
            Question("Kto pisał najwięcej w ostatnim tygodniu?"),
            Question("Czy ktoś pisał o pogodzie?"),
            Question("Ile węzłów było aktywnych przez ostatnie 3 godziny?"),
            Question("Które węzły mają słabą baterię?"),
            Question("Cześć, jak się masz?"),
            Question("Jak zrobić prostą antenę dipol na 868 MHz?"),
            Question("Opowiedz krótko, czym różni się LoRa od Wi-Fi."),
            Question("Mam słaby zasięg — co sprawdzić i pokaż mi odpowiednie ustawienie."),
            Question("Ile węzłów jest w sieci i jak zmienić w ustawieniach rolę mojego urządzenia?"),
            Question("How do I change the LoRa region?"),
            Question("Jaka jest stolica Francji?"),
            Question("Jaka jest teraz temperatura?"),
        )
}
