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
@file:Suppress("MagicNumber", "TooManyFunctions")

package org.meshtastic.feature.messaging.translation

import org.meshtastic.core.model.ChannelOption
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Radio calculations done by the app, never by the model (a small model guesses numbers). Each one recognises its
 * question by words and by numbers with units, so it works for any wording and any values, not only prepared
 * questions. [answer] gives the written result in Polish or English, or null when the question is not a calculation.
 */
object AssistantCalculators {
    /** The radio's own values, used when the question does not give them. */
    data class Defaults(val preset: ChannelOption? = null, val txDbm: Int = 0)

    /** The frequency of our network, used when a question gives none. */
    const val OUR_MHZ = 869.44165
    private const val C = 299_792_458.0
    private const val WIRE = 0.95
    private const val OUR_ERP_LIMIT_DBM = 27.0

    fun answer(question: String, polish: Boolean, defaults: Defaults = Defaults()): String? {
        val q = question.lowercase().replace(' ', ' ')
        val t = Texts(polish)
        return arithmetic(q, t) ?: airtime(q, t, defaults) ?: range(q, t, defaults) ?: fresnel(q, t) ?: horizon(q, t) ?:
            pathLoss(q, t) ?: eirp(q, t) ?: power(q, t) ?: antenna(q, t) ?: wavelength(q, t)
    }

    // ---------------------------------------------------------------- numbers in the question

    private const val NUM = "(\\d+(?:[.,]\\d+)?)"

    private fun num(s: String) = s.replace(',', '.').toDoubleOrNull()

    private fun first(q: String, unit: String): Double? = Regex("$NUM\\s*(?:$unit)(?![\\p{L}])").find(q)?.groupValues?.get(1)?.let(::num)

    private fun all(q: String, unit: String): List<Double> =
        Regex("$NUM\\s*(?:$unit)(?![\\p{L}])").findAll(q).mapNotNull { num(it.groupValues[1]) }.toList()

    /** A frequency in MHz: "868 MHz", "0,868 GHz", "433 mhz"; with [bare] also a lone 3–4 digit number like "868". */
    private fun mhz(q: String, bare: Boolean = false): Double? =
        first(q, "mhz")?.takeIf { it > 0 } ?: first(q, "ghz")?.takeIf { it > 0 }?.times(1000)
            ?: first(q, "khz")?.takeIf { it > 0 }?.div(1000)
            ?: if (bare) Regex("(?<![\\d.,])(\\d{3,4}(?:[.,]\\d+)?)(?![\\d.,]|\\s*(?:km|m\\b|cm|mm|dbm|mw|w\\b))").find(q)?.groupValues?.get(1)?.let(::num)?.takeIf { it in 100.0..6000.0 } else null

    private fun km(q: String): Double? = first(q, "km|kilometr\\p{L}*")?.takeIf { it > 0 }

    /** Heights in metres ("30 m", "10 metrów"), never km, cm or mm. */
    private fun metres(q: String): List<Double> = all(q, "m|metr\\p{L}*|meter\\p{L}*|metre\\p{L}*").filter { it > 0 }

    /** A power in dBm from "27 dBm", "0,5 W", "100 mW". */
    private fun dbm(q: String): Double? =
        first(q, "dbm") ?: first(q, "mw|miliwat\\p{L}*|milliwatt\\p{L}*")?.takeIf { it > 0 }?.let { 10 * log10(it) }
            ?: first(q, "w|wat\\p{L}*|watt\\p{L}*")?.takeIf { it > 0 }?.let { 10 * log10(it * 1000) }

    private fun has(q: String, words: String) = Regex(words).containsMatchIn(q)

    // ---------------------------------------------------------------- formatting

    private class Texts(val pl: Boolean) {
        fun n(v: Double, decimals: Int = 1): String {
            val p = 10.0.pow(decimals)
            val scaled = round(abs(v) * p).toLong()
            val whole = scaled / p.toLong()
            val frac = (scaled % p.toLong()).toString().padStart(decimals, '0').trimEnd('0')
            val sign = if (v < 0 && scaled != 0L) "-" else ""
            return sign + whole + if (frac.isEmpty()) "" else (if (pl) "," else ".") + frac
        }

        fun s(pl: String, en: String) = if (this.pl) pl else en
    }

    private fun mw(dbm: Double) = 10.0.pow(dbm / 10)

    private fun Texts.power(dbm: Double): String {
        val m = mw(dbm)
        return if (m >= 1000) "${n(dbm)} dBm = ${n(m / 1000, 2)} W" else "${n(dbm)} dBm = ${n(m, if (m < 10) 2 else 0)} mW"
    }

    // ---------------------------------------------------------------- arithmetic

    private val ASK_WORDS = Regex("^(policz|oblicz|wylicz|ile to jest|ile to|ile jest|ile wynosi|ile|calculate|compute|what is|what's|how much is)\\s*:?\\s*")
    private val PERCENT_OF = Regex("$NUM\\s*(?:%|procent\\p{L}*|percent)\\s*(?:z|od|of)\\s*$NUM")
    private val ROOT = Regex("(?:pierwiastek(?: kwadratowy)?|square root|sqrt)\\s*(?:z|of)?\\s*$NUM")

    private fun arithmetic(q: String, t: Texts): String? {
        PERCENT_OF.find(q)?.let { m ->
            val p = num(m.groupValues[1]) ?: return null
            val v = num(m.groupValues[2]) ?: return null
            return "${t.n(p, 3)} % ${t.s("z", "of")} ${t.n(v, 3)} = ${t.n(p * v / 100, 4)}"
        }
        ROOT.find(q)?.let { m -> val v = num(m.groupValues[1]) ?: return null; return "√${t.n(v, 3)} = ${t.n(sqrt(v), 4)}" }
        val expr = q.trim().trimEnd('?', '!', '.', ' ', '=').replace(ASK_WORDS, "").trim()
        if (!Regex("^[\\d\\s+\\-*/^().,x×:%]+$").matches(expr) || !Regex("\\d").containsMatchIn(expr)) return null
        if (!Regex("\\d\\s*[+\\-*/^x×:]\\s*[\\d(]").containsMatchIn(expr)) return null
        val value = Expr(expr.replace(',', '.').replace('x', '*').replace('×', '*').replace(':', '/')).parse() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return "${expr.trim()} = ${t.n(value, 6)}"
    }

    /** A small calculator: + - * / ^, brackets, unary minus and a trailing % (divide by 100). */
    private class Expr(val s: String) {
        var i = 0

        fun parse(): Double? = try { sum().takeIf { skip(); i == s.length } } catch (@Suppress("TooGenericExceptionCaught") _: Exception) { null }

        private fun skip() { while (i < s.length && s[i] == ' ') i++ }

        private fun sum(): Double {
            var v = product()
            while (true) {
                skip()
                if (i >= s.length) return v
                when (s[i]) {
                    '+' -> { i++; v += product() }
                    '-' -> { i++; v -= product() }
                    else -> return v
                }
            }
        }

        private fun product(): Double {
            var v = powerOf()
            while (true) {
                skip()
                if (i >= s.length) return v
                when (s[i]) {
                    '*' -> { i++; v *= powerOf() }
                    '/' -> { i++; v /= powerOf() }
                    else -> return v
                }
            }
        }

        private fun powerOf(): Double {
            val base = unary()
            skip()
            return if (i < s.length && s[i] == '^') { i++; base.pow(powerOf()) } else base
        }

        private fun unary(): Double {
            skip()
            if (i < s.length && s[i] == '-') { i++; return -unary() }
            if (i < s.length && s[i] == '+') { i++; return unary() }
            var v: Double
            if (i < s.length && s[i] == '(') {
                i++
                v = sum()
                skip()
                check(i < s.length && s[i] == ')')
                i++
            } else {
                val start = i
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                check(i > start)
                v = s.substring(start, i).toDouble()
            }
            skip()
            if (i < s.length && s[i] == '%') { i++; v /= 100 }
            return v
        }
    }

    // ---------------------------------------------------------------- antennas

    private const val ANT = "anten|dipol|ćwierćfal|cwiercfal|półfal|polfal|antenna|ground ?plane|groundplane|\\bgp\\b|yagi|5/8|1/4|1/2|bat\\b|whip|radial|przeciwwag"
    private const val LENGTH = "\\bile\\b|dług|dlug|\\bcm\\b|centymetr|\\bmm\\b|wymiar|length|how long|size|oblicz|policz|wylicz|calculate"
    private const val BUILD = "zrob|zrób|zbud|wykona|zrobić|zrobic|zmajstr|domow|samemu|samodziel|make|build|diy|homemade|construct"
    private const val ADVICE = "najlepsz|polec|kupi|wybra|jaką kupić|best|recommend|buy|which antenna"

    private fun antenna(q: String, t: Texts): String? {
        if (!has(q, ANT)) return null
        val asked = has(q, LENGTH) || has(q, BUILD)
        val freq = mhz(q, bare = true)
        if (!asked && freq == null) return null
        if (!asked && has(q, ADVICE)) return null
        val f = freq ?: OUR_MHZ
        val lambda = C / (f * 1e6) * 100
        val quarter = lambda / 4 * WIRE
        val half = lambda / 2 * WIRE
        val fiveEighths = lambda * 0.625 * WIRE
        val dipole = has(q, "dipol|półfal|polfal|1/2|half")
        val gp = has(q, "ćwierćfal|cwiercfal|ground ?plane|groundplane|\\bgp\\b|1/4|quarter|radial|przeciwwag|whip|bat\\b")
        val five = has(q, "5/8|pięć ósmych|piec osmych|five.eighth")
        val yagi = has(q, "yagi")
        val build = has(q, BUILD)
        val sb = StringBuilder()
        sb.append(
            t.s(
                "Obliczenia dla ${t.n(f, 2)} MHz${if (freq == null) " (częstotliwość naszej sieci)" else ""}: długość fali λ = c / f = ${t.n(lambda)} cm. Długości anten są skrócone o 5 % (współczynnik 0,95), jak dla elementu z drutu.",
                "Calculation for ${t.n(f, 2)} MHz${if (freq == null) " (our network's frequency)" else ""}: wavelength λ = c / f = ${t.n(lambda)} cm. Antenna lengths are shortened by 5 % (factor 0.95), as for a wire element.",
            ),
        )
        val all = !dipole && !gp && !five && !yagi
        if (gp || all) {
            sb.append("\n• ").append(
                t.s(
                    "Ćwierćfalowa (ground plane): promiennik ok. ${t.n(quarter)} cm, do tego 3–4 przeciwwagi po ok. ${t.n(lambda / 4)} cm, odchylone w dół o ok. 45°.",
                    "Quarter-wave (ground plane): radiator about ${t.n(quarter)} cm, plus 3–4 radials of about ${t.n(lambda / 4)} cm, bent down by about 45°.",
                ),
            )
        }
        if (dipole || all) {
            sb.append("\n• ").append(
                t.s(
                    "Dipol półfalowy: całość ok. ${t.n(half)} cm, czyli dwa ramiona po ok. ${t.n(half / 2)} cm.",
                    "Half-wave dipole: about ${t.n(half)} cm in total, two arms of about ${t.n(half / 2)} cm each.",
                ),
            )
        }
        if (five || all) {
            sb.append("\n• ").append(
                t.s(
                    "5/8 fali: promiennik ok. ${t.n(fiveEighths)} cm; potrzebuje cewki dopasowującej u podstawy.",
                    "5/8 wave: radiator about ${t.n(fiveEighths)} cm; it needs a matching coil at the base.",
                ),
            )
        }
        if (yagi) {
            sb.append("\n• ").append(
                t.s(
                    "Yagi 3-elementowa (punkt wyjścia): reflektor ok. ${t.n(lambda * 0.495)} cm, element zasilany ok. ${t.n(lambda * 0.473)} cm, direktor ok. ${t.n(lambda * 0.44)} cm; odstęp reflektor–element ok. ${t.n(lambda * 0.2)} cm, element–direktor ok. ${t.n(lambda * 0.15)} cm. Dokładne wymiary daje kalkulator Yagi dla konkretnej średnicy pręta.",
                    "3-element Yagi (a starting point): reflector about ${t.n(lambda * 0.495)} cm, driven element about ${t.n(lambda * 0.473)} cm, director about ${t.n(lambda * 0.44)} cm; spacing reflector–driven about ${t.n(lambda * 0.2)} cm, driven–director about ${t.n(lambda * 0.15)} cm. Exact sizes come from a Yagi calculator for the rod diameter.",
                ),
            )
        }
        if (build) {
            sb.append("\n\n").append(
                if (dipole && !gp) {
                    t.s(
                        "Jak zrobić dipol: utnij dwa kawałki drutu (np. miedzianego 1–2 mm) na długość ramienia, przylutuj jeden do żyły środkowej kabla koncentrycznego, drugi do oplotu, i ustaw je w jednej linii, z małą przerwą pośrodku. Kabel prowadź prostopadle do anteny, a na końcu przylutuj złącze pasujące do radia (zwykle SMA).",
                        "How to make a dipole: cut two pieces of wire (e.g. 1–2 mm copper) to the arm length, solder one to the centre conductor of the coax and the other to the braid, and set them in one line with a small gap in the middle. Run the cable away at a right angle and fit a connector that matches the radio (usually SMA).",
                    )
                } else {
                    t.s(
                        "Jak zrobić ground plane: weź gniazdo N lub SMA do montażu na panelu, przylutuj promiennik do środkowego pinu, a 3–4 przeciwwagi do korpusu gniazda i odegnij je w dół o ok. 45°. Antenę montuj pionowo, jak najwyżej i z dala od metalu.",
                        "How to make a ground plane: take a panel-mount N or SMA socket, solder the radiator to the centre pin and 3–4 radials to the socket body, and bend them down by about 45°. Mount it vertically, as high as possible and away from metal.",
                    )
                },
            )
        }
        sb.append("\n").append(
            t.s(
                "Na koniec dostrój miernikiem SWR albo NanoVNA: gdy rezonans wypada za nisko, skracaj po 1–2 mm.",
                "Finally tune it with an SWR meter or a NanoVNA: if the resonance is too low, shorten it 1–2 mm at a time.",
            ),
        )
        return sb.toString()
    }

    private fun wavelength(q: String, t: Texts): String? {
        if (!has(q, "długość fali|dlugosc fali|długości fali|dlugosci fali|wavelength|lambda|λ")) return null
        val f = mhz(q, bare = true) ?: return null
        val lambda = C / (f * 1e6)
        return t.s(
            "Dla ${t.n(f, 2)} MHz długość fali λ = c / f = ${t.n(lambda * 100)} cm (${t.n(lambda, 3)} m).",
            "For ${t.n(f, 2)} MHz the wavelength λ = c / f = ${t.n(lambda * 100)} cm (${t.n(lambda, 3)} m).",
        )
    }

    // ---------------------------------------------------------------- power

    private fun power(q: String, t: Texts): String? {
        val convert = has(q, "\\bile\\b|przelicz|zamie|konwert|ile to|to ile|w mw|na mw|w dbm|na dbm|w wat|na wat|convert|how much|in mw|in dbm|in watt|to mw|to dbm")
        val dbms = all(q, "dbm")
        if (dbms.isNotEmpty() && (convert || has(q, "\\bmw\\b|wat|watt"))) return dbms.joinToString("\n") { t.power(it) }
        val mws = all(q, "mw|miliwat\\p{L}*|milliwatt\\p{L}*").filter { it > 0 }
        val ws = all(q, "w|wat\\p{L}*|watt\\p{L}*").filter { it > 0 }
        if ((mws.isNotEmpty() || ws.isNotEmpty()) && has(q, "dbm")) {
            return (mws.map { "${t.n(it, 2)} mW = ${t.n(10 * log10(it))} dBm" } + ws.map { "${t.n(it, 2)} W = ${t.n(10 * log10(it * 1000))} dBm" }).joinToString("\n")
        }
        return null
    }

    private fun eirp(q: String, t: Texts): String? {
        if (!has(q, "eirp|\\berp\\b|moc wypromieniowan|moc promieniowan|radiated power")) return null
        val p = dbm(q) ?: return null
        val gain = first(q, "dbi") ?: 0.0
        val loss = Regex("$NUM\\s*db(?![im\\p{L}])").find(q)?.groupValues?.get(1)?.let(::num) ?: 0.0
        val e = p + gain - loss
        val erp = e - 2.15
        val ok = erp <= OUR_ERP_LIMIT_DBM + 0.05
        return t.s(
            "Moc ${t.power(p)}, zysk anteny ${t.n(gain)} dBi, straty ${t.n(loss)} dB:\n• EIRP = ${t.power(e)}\n• ERP = EIRP − 2,15 = ${t.power(erp)}\n" +
                "W zakresie naszej sieci (869,4–869,65 MHz) limit to 500 mW ERP (27 dBm): ${if (ok) "mieścisz się." else "to za dużo — zmniejsz moc."}",
            "Power ${t.power(p)}, antenna gain ${t.n(gain)} dBi, losses ${t.n(loss)} dB:\n• EIRP = ${t.power(e)}\n• ERP = EIRP − 2.15 = ${t.power(erp)}\n" +
                "In our network's band (869.4–869.65 MHz) the limit is 500 mW ERP (27 dBm): ${if (ok) "you are within it." else "that is too much — lower the power."}",
        )
    }

    // ---------------------------------------------------------------- propagation

    private fun fspl(km: Double, mhz: Double) = 20 * log10(km) + 20 * log10(mhz) + 32.44

    private fun pathLoss(q: String, t: Texts): String? {
        if (!has(q, "tłumien|tlumien|strat\\p{L}* (?:na )?(?:trasie|propagac|w wolnej)|path loss|fspl|free.space|wolnej przestrzeni")) return null
        val d = km(q) ?: return null
        val f = mhz(q) ?: OUR_MHZ
        return t.s(
            "Tłumienie w wolnej przestrzeni na ${t.n(d, 2)} km przy ${t.n(f, 2)} MHz: FSPL = 20·log(d) + 20·log(f) + 32,44 = ${t.n(fspl(d, f))} dB. W terenie z przeszkodami jest więcej.",
            "Free-space path loss over ${t.n(d, 2)} km at ${t.n(f, 2)} MHz: FSPL = 20·log(d) + 20·log(f) + 32.44 = ${t.n(fspl(d, f))} dB. With obstacles it is more.",
        )
    }

    private fun fresnel(q: String, t: Texts): String? {
        if (!has(q, "fresnel")) return null
        val d = km(q) ?: return null
        val f = mhz(q) ?: OUR_MHZ
        val r = 8.657 * sqrt(d / (f / 1000))
        return t.s(
            "Strefa Fresnela dla ${t.n(d, 2)} km przy ${t.n(f, 2)} MHz: w połowie trasy ma promień ok. ${t.n(r)} m. Żeby łącze działało dobrze, co najmniej 60 % (ok. ${t.n(r * 0.6)} m) powinno być wolne od przeszkód.",
            "Fresnel zone for ${t.n(d, 2)} km at ${t.n(f, 2)} MHz: at mid-path its radius is about ${t.n(r)} m. For a good link at least 60 % (about ${t.n(r * 0.6)} m) should be free of obstacles.",
        )
    }

    private fun horizon(q: String, t: Texts): String? {
        val words = has(q, "horyzont|horizon|linii wzroku|line of sight|widoczno|krzywizn|curvature")
        val far = has(q, "jak daleko|how far|na ile km|ile km") && has(q, "wysoko|wysokoś|wysokos|height|maszt|mast|dach|roof|góry|gory|szczyt")
        if (!words && !far) return null
        val h = metres(q)
        if (h.isEmpty()) return null
        val h1 = h[0]
        val h2 = h.getOrNull(1) ?: 2.0
        val d = 4.12 * (sqrt(h1) + sqrt(h2))
        return t.s(
            "Horyzont radiowy dla anten na ${t.n(h1)} m i ${t.n(h2)} m${if (h.size < 2) " (druga antena w ręku)" else ""}: ok. ${t.n(d)} km (d = 4,12·(√h1 + √h2)). To granica przy płaskim terenie; góry i budynki ją skracają, a łącze na tej odległości potrzebuje jeszcze wolnej strefy Fresnela.",
            "Radio horizon for antennas at ${t.n(h1)} m and ${t.n(h2)} m${if (h.size < 2) " (the other one hand-held)" else ""}: about ${t.n(d)} km (d = 4.12·(√h1 + √h2)). That is the limit over flat ground; hills and buildings shorten it, and a link at that distance also needs a clear Fresnel zone.",
        )
    }

    // ---------------------------------------------------------------- LoRa

    private fun preset(q: String, defaults: Defaults): Pair<ChannelOption, Boolean> {
        val flat = q.replace(Regex("[\\s_-]"), "")
        val named = ChannelOption.entries.firstOrNull { flat.contains(it.name.replace("_", "").lowercase()) }
        return when {
            named != null -> named to true
            defaults.preset != null -> defaults.preset to false
            else -> ChannelOption.NARROW_FAST to false
        }
    }

    private fun presetName(o: ChannelOption) = o.name.split('_').joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }

    private fun snrLimit(sf: Int) = -7.5 - 2.5 * (sf - 7)

    private fun range(q: String, t: Texts, defaults: Defaults): String? {
        if (!has(q, "zasięg|zasieg|range|link budget|budżet łącza|budzet lacza|budżet mocy")) return null
        if (!has(q, "teoretycz|maksymal|\\bmax|oblicz|policz|wylicz|ile km|how far|jaki (?:będzie |bedzie |mam |jest )?zasięg|jaki (?:będzie |bedzie |mam |jest )?zasieg|what range|calculate|budżet|budzet|budget")) return null
        if (has(q, "słab|slab|kiepsk|poprawi|zwiększ|zwieksz|sprawdz|sprawdź|weak|poor|improve")) return null
        val (o, named) = preset(q, defaults)
        val pGiven = dbm(q)
        val p = pGiven ?: defaults.txDbm.takeIf { it > 0 }?.toDouble() ?: 22.0
        val gains = all(q, "dbi")
        val gt = gains.getOrNull(0) ?: 2.15
        val gr = gains.getOrNull(1) ?: gt
        val f = mhz(q) ?: OUR_MHZ
        val bwHz = o.bandwidth * 1e6
        val sens = -174 + 10 * log10(bwHz) + 6 + snrLimit(o.spreadingFactor)
        val budget = p + gt + gr - sens
        val dKm = 10.0.pow((budget - 32.44 - 20 * log10(f)) / 20)
        val presetNote = if (named) "" else t.s(" (ustawiony w radiu)", " (set in the radio)")
        val powerNote = if (pGiven != null) "" else if (defaults.txDbm > 0) t.s(" (z radia)", " (from the radio)") else t.s(" (typowe radio SX1262)", " (a typical SX1262 radio)")
        return t.s(
            "Budżet łącza dla presetu ${presetName(o)}$presetNote (SF${o.spreadingFactor}, ${t.n(o.bandwidth * 1000.0, 2)} kHz) przy ${t.n(f, 2)} MHz:\n" +
                "• moc ${t.n(p)} dBm$powerNote, anteny ${t.n(gt)} dBi i ${t.n(gr)} dBi\n" +
                "• czułość odbiornika ok. ${t.n(sens)} dBm (granica SNR ${t.n(snrLimit(o.spreadingFactor))} dB)\n" +
                "• dopuszczalne tłumienie ok. ${t.n(budget)} dB\n" +
                "• zasięg teoretyczny w wolnej przestrzeni: ok. ${t.n(dKm, 0)} km\n" +
                "To górna granica przy pełnej widoczności anten. W praktyce decydują wysokość anten i przeszkody: na płaskim terenie przy ziemi to zwykle kilka km, z wysokich punktów kilkadziesiąt i więcej. Konkretne łącze sprawdzisz w planerze łącza.",
            "Link budget for the ${presetName(o)} preset$presetNote (SF${o.spreadingFactor}, ${t.n(o.bandwidth * 1000.0, 2)} kHz) at ${t.n(f, 2)} MHz:\n" +
                "• power ${t.n(p)} dBm$powerNote, antennas ${t.n(gt)} dBi and ${t.n(gr)} dBi\n" +
                "• receiver sensitivity about ${t.n(sens)} dBm (SNR limit ${t.n(snrLimit(o.spreadingFactor))} dB)\n" +
                "• allowed path loss about ${t.n(budget)} dB\n" +
                "• theoretical free-space range: about ${t.n(dKm, 0)} km\n" +
                "That is the upper limit with the antennas in full view of each other. In practice antenna height and obstacles decide: near the ground on flat land usually a few km, from high places tens of km and more. Check a real link in the link planner.",
        )
    }

    /** LoRa time on air (Semtech formula, 16-symbol preamble, explicit header, CRC on). */
    fun airtimeMs(o: ChannelOption, payloadBytes: Int): Double {
        val sf = o.spreadingFactor
        val bw = o.bandwidth * 1e6
        val tSym = 2.0.pow(sf) / bw * 1000
        val de = if (tSym > 16) 1 else 0
        val pre = (16 + 4.25) * tSym
        val n = 8 + max(ceil((8.0 * payloadBytes - 4 * sf + 28 + 16) / (4 * (sf - 2 * de))) * o.codingRate, 0.0)
        return pre + n * tSym
    }

    private const val OVERHEAD = 20

    private fun airtime(q: String, t: Texts, defaults: Defaults): String? {
        if (!has(q, "czas (?:nadawania|transmisji|w eterze|wysyłania|wysylania)|czasu (?:nadawania|transmisji)|airtime|air time|time on air|w eterze|ile trwa (?:wysłan|wysylan|nadawan|transmisj|pakiet|wiadomo)|jak długo trwa|jak dlugo trwa|duty.?cycle|ile (?:wiadomości|wiadomosci|pakietów|pakietow) (?:mogę|moge|można|mozna|da się|da sie) (?:wysłać|wyslac|nadać|nadac)")) return null
        val (o, named) = preset(q, defaults)
        val bytes = first(q, "bajt\\p{L}*|byte\\p{L}*|b")?.toInt()
        val chars = first(q, "znak\\p{L}*|liter\\p{L}*|char\\p{L}*")?.toInt()
        val sizes = when {
            bytes != null -> listOf(bytes)
            chars != null -> listOf(chars + OVERHEAD)
            else -> listOf(10 + OVERHEAD, 50 + OVERHEAD, 200 + OVERHEAD)
        }.map { it.coerceIn(1, 255) }
        val presetNote = if (named) "" else t.s(" (ustawiony w radiu)", " (set in the radio)")
        val sb = StringBuilder(t.s("Czas nadawania dla presetu ${presetName(o)}$presetNote:", "Time on air for the ${presetName(o)} preset$presetNote:"))
        for (b in sizes) {
            val ms = airtimeMs(o, b)
            val label = if (bytes != null) t.s("$b B", "$b B") else t.s("wiadomość ${b - OVERHEAD} znaków", "a ${b - OVERHEAD}-character message")
            sb.append("\n• ").append(label).append(": ").append(t.n(ms, 0)).append(" ms")
                .append(t.s(", przy limicie 10 % czasu ok. ${t.n(360_000 / ms, 0)} takich pakietów na godzinę", ", with the 10 % limit about ${t.n(360_000 / ms, 0)} such packets per hour"))
        }
        sb.append("\n").append(
            t.s(
                "Każdy węzeł, który przekazuje pakiet dalej, nadaje go jeszcze raz. Do tekstu doliczam ok. $OVERHEAD bajtów nagłówka.",
                "Every node that relays the packet sends it again. About $OVERHEAD header bytes are added to a text.",
            ),
        )
        return sb.toString()
    }
}
