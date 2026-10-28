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
    const val TRANSLATE = "/tlumacz"

    val commands: List<String> = listOf(COMMAND, TOOLS, CHAT, KNOWLEDGE, TRANSLATE, HARDWARE)

    /** One translation test: [keep] are the things (names, numbers, links) that must stay exactly as they are. */
    class Translation(val text: String, val from: String, val to: String, val keep: List<String> = emptyList())

    private const val PL = "pol_Latn"
    private const val EN = "eng_Latn"
    private const val DE = "deu_Latn"
    private const val CS = "ces_Latn"
    private const val UK = "ukr_Cyrl"
    private const val FR = "fra_Latn"
    private const val ES = "spa_Latn"

    /** The translator test (the same for every model, so the results can be compared). */
    val translations: List<Translation> =
        listOf(
            Translation("Dzień dobry, jestem na miejscu.", PL, EN),
            Translation("Wracam za godzinę, nie czekajcie na mnie.", PL, EN),
            Translation("Węzeł SM_Base ma 56% baterii i słyszy 14 węzłów.", PL, EN, listOf("SM_Base", "56", "14")),
            Translation("Zasięg do Łysicy wynosi 54 km przy antenie 5 dBi.", PL, EN, listOf("54", "5 dBi")),
            Translation("Zmień region na EU_868 i ustaw limit skoków na 3.", PL, EN, listOf("EU_868", "3")),
            Translation("Sprawdź kanał LongFast, bo SNR spadł do -12 dB.", PL, EN, listOf("LongFast", "-12")),
            Translation("Mapa jest tutaj: https://meshtastic-swietokrzyskie.pl/mapa", PL, EN, listOf("https://meshtastic-swietokrzyskie.pl/mapa")),
            Translation("Czy ktoś słyszy węzeł SP7UNR_atk-20 w okolicy Kielc?", PL, EN, listOf("SP7UNR_atk-20", "Kielc")),
            Translation("Antena ćwierćfalowa na 868 MHz ma około 8,6 cm.", PL, EN, listOf("868", "8,6")),
            Translation("Proszę o pomoc, mam słaby zasięg i rozładowaną baterię.", PL, EN),
            Translation("Spotkanie sieci w sobotę o 10:30 przy radiostacji.", PL, EN, listOf("10:30")),
            Translation("Good morning, I am on the hill and can hear three nodes.", EN, PL),
            Translation("The battery of node SM_Mobile is at 56 percent.", EN, PL, listOf("SM_Mobile", "56")),
            Translation("Please set the region to EU_868 and restart the device.", EN, PL, listOf("EU_868")),
            Translation("My signal is weak, can anyone relay my message?", EN, PL),
            Translation("The range between the two nodes is 12.5 km.", EN, PL, listOf("12.5")),
            Translation("Meet me at 14:00 near the tower, channel LongFast.", EN, PL, listOf("14:00", "LongFast")),
            Translation("Do you hear my node, SNR is 8 dB?", EN, PL, listOf("8 dB")),
            Translation("Dzień dobry, czy ktoś słyszy mój węzeł?", PL, DE),
            Translation("Bateria ma 40%, wracam do domu.", PL, DE, listOf("40")),
            Translation("Gdzie jest najbliższy router?", PL, CS),
            Translation("Węzeł SM_Base działa od 12 dni.", PL, CS, listOf("SM_Base", "12")),
            Translation("Dobry wieczór, odbieram Was głośno i wyraźnie.", PL, UK),
            Translation("Zmieniam kanał na LongFast.", PL, UK, listOf("LongFast")),
            Translation("Je suis arrivé, la batterie est à 80 %.", FR, PL, listOf("80")),
            Translation("¿Alguien oye mi nodo? Estoy en la montaña.", ES, PL),
            Translation("Guten Tag, ich höre den Knoten SM_Base mit 12 dB.", DE, PL, listOf("SM_Base", "12")),
            Translation("Test zasięgu: 3 km, 5 km, 10 km, bez przeszkód.", PL, EN, listOf("3 km", "5 km", "10 km")),
            Translation("Uwaga: jutro aktualizacja firmware MT_SW_FIRMWARE v3.", PL, EN, listOf("MT_SW_FIRMWARE", "v3")),
            Translation("Dziękuję za pomoc, do usłyszenia!", PL, EN),
        )

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
