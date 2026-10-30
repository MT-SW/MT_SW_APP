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
 * Which LoRa region fits a place, worked out offline from rough map boxes: nothing is sent anywhere and the place is
 * not stored. It is an approximation (near borders it can be wrong) and never legal advice.
 */
object AssistantRegion {
    /** The place of a fix: a rough region code and whether it is inside our voivodeship. */
    data class Place(val region: String, val ours: Boolean, val known: Boolean)

    // A rough outline of the Świętokrzyskie voivodeship (latitude, longitude), clockwise.
    private val SWIETOKRZYSKIE =
        listOf(
            50.62 to 19.78, 50.93 to 19.90, 51.10 to 20.15, 51.30 to 20.55, 51.37 to 21.00, 51.15 to 21.45,
            50.95 to 21.80, 50.70 to 21.85, 50.45 to 21.40, 50.30 to 20.90, 50.35 to 20.30, 50.42 to 19.95, 50.52 to 19.80,
        )

    private fun inside(lat: Double, lon: Double, outline: List<Pair<Double, Double>>): Boolean {
        var result = false
        var j = outline.lastIndex
        for (i in outline.indices) {
            val (yi, xi) = outline[i]
            val (yj, xj) = outline[j]
            if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) result = !result
            j = i
        }
        return result
    }

    fun place(lat: Double, lon: Double): Place =
        when {
            inside(lat, lon, SWIETOKRZYSKIE) -> Place("EU_N_868", ours = true, known = true)
            lat in 35.0..72.0 && lon in -25.0..45.0 -> Place("EU_868", ours = false, known = true)
            lat in 15.0..72.0 && lon in -170.0..-50.0 -> Place("US", ours = false, known = true)
            lat in -48.0..-10.0 && lon in 110.0..180.0 -> Place("ANZ", ours = false, known = true)
            else -> Place("", ours = false, known = false)
        }

    private fun degrees(v: Double, plus: String, minus: String, polish: Boolean): String {
        val rounded = kotlin.math.round(kotlin.math.abs(v) * 10) / 10
        val text = if (polish) rounded.toString().replace('.', ',') else rounded.toString()
        return "$text° ${if (v >= 0) plus else minus}"
    }

    /** The explanation of a region plus, when a place is known, what fits it. [source] says where the place comes from. */
    fun advice(lat: Double?, lon: Double?, source: String, current: String, polish: Boolean): String {
        fun s(pl: String, en: String) = if (polish) pl else en
        val sb = StringBuilder()
        sb.append(
            s(
                "Region mówi radiu, w jakim paśmie i z jakimi limitami mocy i czasu nadawania wolno mu pracować. Bez ustawionego regionu węzeł nie nadaje, a zły region łamie lokalne przepisy i sprawia, że inne radia Cię nie usłyszą. Ustawiasz go w: Ustawienia → LoRa → Region.",
                "The region tells the radio which band and which power and transmit-time limits it may use. Without a region the node does not transmit, and a wrong region breaks local rules and other radios will not hear you. You set it in: Settings → LoRa → Region.",
            ),
        )
        if (lat == null || lon == null) {
            sb.append("\n\n").append(
                s(
                    "Nie znam Twojego położenia (radio nie podaje pozycji, a telefon nie udostępnił lokalizacji), więc nie podpowiem konkretnego regionu. W Polsce i całej Europie jest to EU_868.",
                    "I do not know your location (the radio gives no position and the phone shared no location), so I cannot suggest a specific region. In Poland and all of Europe it is EU_868.",
                ),
            )
            return sb.toString()
        }
        val place = place(lat, lon)
        val where = "${degrees(lat, "N", "S", polish)}, ${degrees(lon, "E", "W", polish)}"
        sb.append("\n\n").append(s("Twoje położenie (z $source, przybliżone): okolice $where.", "Your location (from $source, approximate): around $where."))
        when {
            place.ours -> {
                sb.append("\n").append(
                    s(
                        "To nasze województwo świętokrzyskie: sieć używa tu regionu EU_N_868 z presetem Narrow Fast (62,5 kHz, 869,44165 MHz).",
                        "This is our Świętokrzyskie voivodeship: the network uses region EU_N_868 here with the Narrow Fast preset (62.5 kHz, 869.44165 MHz).",
                    ),
                )
                if (current != "EU_N_868") {
                    sb.append("\n").append(s("Teraz masz ustawiony region $current — zmień go na EU_N_868, żeby słyszeć naszą sieć.", "Your region is now $current — change it to EU_N_868 to hear our network."))
                } else {
                    sb.append("\n").append(s("Masz już ustawiony EU_N_868, wystarczy sprawdzić preset Narrow Fast.", "You already have EU_N_868; just check the Narrow Fast preset."))
                }
            }
            place.region == "EU_868" -> {
                sb.append("\n").append(
                    s(
                        "W Polsce i w większości Europy pasmo 868 MHz to region EU_868 (EU_433 to pasmo 433 MHz, rzadziej używane).",
                        "In Poland and most of Europe the 868 MHz band is region EU_868 (EU_433 is the less used 433 MHz band).",
                    ),
                )
                if (current.isNotBlank() && current != "EU_868") {
                    sb.append("\n").append(s("Teraz masz ustawiony region $current.", "Your region is now $current."))
                }
            }
            place.region == "US" -> sb.append("\n").append(s("Dla USA i Kanady to region US (pasmo 915 MHz).", "For the USA and Canada this is region US (the 915 MHz band)."))
            place.region == "ANZ" -> sb.append("\n").append(s("Dla Australii i Nowej Zelandii to region ANZ (pasmo 915 MHz).", "For Australia and New Zealand this is region ANZ (the 915 MHz band)."))
            else -> sb.append("\n").append(s("Poza Europą region zależy od kraju — sprawdź listę regionów w ustawieniach LoRa i lokalne przepisy.", "Outside Europe the region depends on the country — check the region list in the LoRa settings and local rules."))
        }
        sb.append("\n\n").append(
            s(
                "To podpowiedź na podstawie przybliżonego położenia: blisko granic może się mylić i nie jest poradą prawną. Pozycji nigdzie nie wysyłam ani nie zapisuję.",
                "This is a hint from an approximate location: near borders it can be wrong, and it is not legal advice. I send or store the position nowhere.",
            ),
        )
        return sb.toString()
    }
}
