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
import org.meshtastic.core.model.antenna.AntennaInput
import org.meshtastic.core.model.antenna.AntennaMath
import org.meshtastic.core.model.antenna.AntennaType
import org.meshtastic.core.model.antenna.Dim
import org.meshtastic.core.model.antenna.DimKind
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
    private const val OUR_ERP_LIMIT_DBM = 27.0
    /** A link margin of this many dB over free space usually survives some obstacles and fading. */
    private const val GOOD_MARGIN_DB = 20.0

    fun answer(question: String, polish: Boolean, defaults: Defaults = Defaults()): String? {
        lastAntenna = null
        val q = question.lowercase().replace(' ', ' ')
        val t = Texts(polish)
        return arithmetic(q, t) ?: airtime(q, t, defaults) ?: presets(q, t, defaults) ?: range(q, t, defaults) ?: fresnel(q, t) ?: horizon(q, t) ?:
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

    private const val ANT = "anten|dipol|ćwierćfal|cwiercfal|półfal|polfal|antenna|ground ?plane|groundplane|\\bgp\\b|yagi|5/8|1/4|1/2|bat\\b|whip|radial|przeciwwag|j-?pole|slim ?jim|kolinear|collinear|\\bcoco\\b|moxon|biquad|heli[sx]|helic|patch"
    private const val LENGTH = "\\bile\\b|dług|dlug|\\bcm\\b|centymetr|\\bmm\\b|wymiar|length|how long|size|oblicz|policz|wylicz|calculate"
    private const val BUILD = "zrob|zrób|zbud|wykona|zrobić|zrobic|zmajstr|domow|samemu|samodziel|make|build|diy|homemade|construct"
    private const val ADVICE = "najlepsz|polec|kupi|wybra|jaką kupić|best|recommend|buy|which antenna"

    /** The antenna the question is about, or null for "any antenna" (then the three simple ones are shown). */
    private fun antennaType(q: String): AntennaType? = when {
        has(q, "yagi") -> AntennaType.YAGI
        has(q, "moxon") -> AntennaType.MOXON
        has(q, "biquad") -> AntennaType.BIQUAD
        has(q, "heli[sx]|helic") -> AntennaType.HELIX
        has(q, "patch|płask|plask|mikropask|microstrip") -> AntennaType.PATCH
        has(q, "slim ?jim") -> AntennaType.SLIM_JIM
        has(q, "j-?pole") -> AntennaType.J_POLE
        has(q, "kolinear|collinear|\\bcoco\\b") -> AntennaType.COLLINEAR
        has(q, "pętlow|petlow|folded") -> AntennaType.FOLDED_DIPOLE
        has(q, "5/8|pięć ósmych|piec osmych|five.eighth") -> AntennaType.FIVE_EIGHTHS
        has(q, "dipol|półfal|polfal|1/2|half") -> AntennaType.DIPOLE
        has(q, "ćwierćfal|cwiercfal|ground ?plane|groundplane|\\bgp\\b|1/4|quarter|radial|przeciwwag|whip|bat\\b") -> AntennaType.GROUND_PLANE
        else -> null
    }

    private fun Texts.dimName(d: Dim): String = when (d.kind) {
        DimKind.TOTAL -> s("długość całkowita", "total length")
        DimKind.RADIATOR -> s("promiennik", "radiator")
        DimKind.RADIAL -> s("każda przeciwwaga", "each radial")
        DimKind.ARM -> s("każde ramię", "each arm")
        DimKind.GAP -> s("przerwa", "gap")
        DimKind.SPACING -> s("odstęp", "spacing")
        DimKind.STUB -> s("odcinek dopasowujący", "matching stub")
        DimKind.FEED -> s("punkt zasilania od dołu", "feed point from the bottom")
        DimKind.SECTION -> s("każdy odcinek kabla", "each coax section")
        DimKind.WHIP -> s("bat na szczycie", "top whip")
        DimKind.REFLECTOR -> s("reflektor", "reflector")
        DimKind.DRIVEN -> s("element zasilany", "driven element")
        DimKind.DIRECTOR -> s("direktor ${d.index}", "director ${d.index}")
        DimKind.BOOM -> s("długość nośnika", "boom length")
        DimKind.POSITION -> s("pozycja", "position")
        DimKind.WIDTH -> s("szerokość", "width")
        DimKind.DEPTH -> s("głębokość", "depth")
        DimKind.TAIL_DRIVER -> s("zagięcie elementu zasilanego", "driver tail")
        DimKind.TAIL_REFLECTOR -> s("zagięcie reflektora", "reflector tail")
        DimKind.SIDE -> s("bok kwadratu", "side of a square")
        DimKind.REFLECTOR_PLATE -> s("płyta reflektora", "reflector plate")
        DimKind.DIAMETER -> s("średnica zwojów", "coil diameter")
        DimKind.PITCH -> s("skok zwoju", "turn spacing")
        DimKind.TURN_WIRE -> s("drut na zwój", "wire per turn")
        DimKind.LENGTH -> s("długość helisy", "helix length")
        DimKind.GROUND -> s("płaszczyzna masy", "ground plane")
        DimKind.PATCH_W -> s("szerokość łaty", "patch width")
        DimKind.PATCH_L -> s("długość łaty", "patch length")
        DimKind.INSET -> s("wcięcie dla 50 Ω", "inset for 50 Ω")
        DimKind.FEED_WIDTH -> s("ścieżka 50 Ω", "50 Ω track")
        DimKind.QUARTER -> s("odcinek λ/4", "λ/4 piece")
        DimKind.HALF -> s("odcinek λ/2", "λ/2 piece")
        DimKind.WAVELENGTH -> s("długość fali", "wavelength")
    }

    private fun Texts.len(m: Double): String = when {
        m >= 1.0 -> n(m, 3) + " m"
        m < 0.02 -> n(m * 1000, 1) + " mm"
        else -> n(m * 100, 1) + " cm"
    }

    private fun Texts.typeName(t: AntennaType): String = when (t) {
        AntennaType.GROUND_PLANE -> s("Ćwierćfalowa (ground plane)", "Quarter-wave (ground plane)")
        AntennaType.DIPOLE -> s("Dipol półfalowy", "Half-wave dipole")
        AntennaType.FOLDED_DIPOLE -> s("Dipol pętlowy", "Folded dipole")
        AntennaType.FIVE_EIGHTHS -> s("5/8 fali", "5/8 wave")
        AntennaType.J_POLE -> "J-pole"
        AntennaType.SLIM_JIM -> "Slim Jim"
        AntennaType.COLLINEAR -> s("Kolinearna z kabla", "Coaxial collinear")
        AntennaType.YAGI -> s("Yagi ${YAGI_ELEMENTS}-elementowa", "${YAGI_ELEMENTS}-element Yagi")
        AntennaType.MOXON -> "Moxon"
        AntennaType.BIQUAD -> "Biquad"
        AntennaType.HELIX -> s("Helisa osiowa", "Axial helix")
        AntennaType.PATCH -> s("Płaska (patch, FR4 1,6 mm)", "Patch (FR4 1.6 mm)")
        AntennaType.COAX_STUB -> s("Odcinek kabla", "Coax section")
    }

    private const val YAGI_ELEMENTS = 5

    private fun Texts.describe(input: AntennaInput): String {
        val d = AntennaMath.design(input)
        val sb = StringBuilder("• ").append(typeName(input.type)).append(':')
        d.dims.filter { it.kind != DimKind.POSITION }.forEach { dim -> sb.append("\n   ").append(dim.letter).append(" — ").append(dimName(dim)).append(": ").append(len(dim.meters)) }
        if (input.type != AntennaType.COAX_STUB) sb.append("\n   ").append(s("zysk ok. ${n(d.gainDbi)} dBi", "gain about ${n(d.gainDbi)} dBi"))
        d.impedanceOhm?.let { sb.append(s(", impedancja ok. ${n(it, 0)} Ω", ", impedance about ${n(it, 0)} Ω")) }
        return sb.toString()
    }

    /** The antenna of the last answer, so the assistant can add a button to the calculator with the same values. */
    var lastAntenna: Pair<AntennaType, Double>? = null
        private set

    private fun antenna(q: String, t: Texts): String? {
        if (!has(q, ANT)) return null
        val asked = has(q, LENGTH) || has(q, BUILD)
        val freq = mhz(q, bare = true)
        if (!asked && freq == null) return null
        if (!asked && has(q, ADVICE)) return null
        val f = freq ?: OUR_MHZ
        val wire = first(q, "mm")?.takeIf { it in 0.1..60.0 } ?: 1.5
        val type = antennaType(q)
        val lambda = C / (f * 1e6) * 100
        val base = AntennaInput(mhz = f, diameterMm = wire, elements = YAGI_ELEMENTS)
        val k = AntennaMath.thicknessFactor(lambda / 100, wire / 1000)
        lastAntenna = (type ?: AntennaType.GROUND_PLANE) to f
        val sb = StringBuilder()
        sb.append(
            t.s(
                "Obliczenia dla ${t.n(f, 2)} MHz${if (freq == null) " (częstotliwość naszej sieci)" else ""}: długość fali λ = c / f = ${t.n(lambda)} cm. Elementy z drutu ${t.n(wire)} mm, współczynnik skrócenia ${t.n(k, 3)} (z grubości przewodu).",
                "Calculation for ${t.n(f, 2)} MHz${if (freq == null) " (our network's frequency)" else ""}: wavelength λ = c / f = ${t.n(lambda)} cm. Elements of ${t.n(wire)} mm wire, shortening factor ${t.n(k, 3)} (from the conductor thickness).",
            ),
        )
        val types = if (type != null) listOf(type) else listOf(AntennaType.GROUND_PLANE, AntennaType.DIPOLE, AntennaType.FIVE_EIGHTHS)
        types.forEach { sb.append("\n").append(t.describe(base.copy(type = it))) }
        if (has(q, BUILD)) {
            sb.append("\n\n").append(
                if (type == AntennaType.DIPOLE || type == AntennaType.FOLDED_DIPOLE) {
                    t.s(
                        "Jak zrobić dipol: utnij dwa kawałki drutu na długość ramienia, przylutuj jeden do żyły środkowej kabla koncentrycznego, drugi do oplotu, i ustaw je w jednej linii, z małą przerwą pośrodku. Kabel prowadź prostopadle do anteny, a na końcu przylutuj złącze pasujące do radia (zwykle SMA).",
                        "How to make a dipole: cut two pieces of wire to the arm length, solder one to the centre conductor of the coax and the other to the braid, and set them in one line with a small gap in the middle. Run the cable away at a right angle and fit a connector that matches the radio (usually SMA).",
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
                "Na koniec dostrój miernikiem SWR albo NanoVNA: gdy rezonans wypada za nisko, skracaj po 1–2 mm. Inne typy anten, inną grubość przewodu i straty kabla policzysz w kalkulatorze anten.",
                "Finally tune it with an SWR meter or a NanoVNA: if the resonance is too low, shorten it 1–2 mm at a time. Other antenna types, other wire thickness and cable losses are in the antenna calculator.",
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
        if (!has(q, "zasięg|zasieg|range|link budget|budżet łącza|budzet lacza|budżet mocy|bilans\\p{L}* łącza|bilans\\p{L}* lacza|bilans\\p{L}* mocy|bilans\\p{L}* radiow")) return null
        if (!has(q, "teoretycz|maksymal|\\bmax|oblicz|policz|wylicz|ile km|how far|jaki (?:będzie |bedzie |mam |jest )?zasięg|jaki (?:będzie |bedzie |mam |jest )?zasieg|what range|calculate|budżet|budzet|budget|bilans")) return null
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
        // A distance in the question: what is left of the budget over exactly that path.
        val distance = km(q)?.let { d ->
            val margin = budget - fspl(d, f)
            val verdict =
                when {
                    margin >= GOOD_MARGIN_DB -> t.s("wystarczy z zapasem", "enough, with room to spare")
                    margin >= 0 -> t.s("styk — przy przeszkodach może nie działać", "borderline — obstacles may break it")
                    else -> t.s("za mało nawet w wolnej przestrzeni", "not enough even in free space")
                }
            t.s(
                "• na ${t.n(d, 2)} km: tłumienie w wolnej przestrzeni ${t.n(fspl(d, f))} dB, zapas ${t.n(margin)} dB — $verdict\n",
                "• over ${t.n(d, 2)} km: free-space path loss ${t.n(fspl(d, f))} dB, margin ${t.n(margin)} dB — $verdict\n",
            )
        }.orEmpty()
        val presetNote = if (named) "" else t.s(" (ustawiony w radiu)", " (set in the radio)")
        val powerNote = if (pGiven != null) "" else if (defaults.txDbm > 0) t.s(" (z radia)", " (from the radio)") else t.s(" (typowe radio SX1262)", " (a typical SX1262 radio)")
        return t.s(
            "Budżet łącza dla presetu ${presetName(o)}$presetNote (SF${o.spreadingFactor}, ${t.n(o.bandwidth * 1000.0, 2)} kHz) przy ${t.n(f, 2)} MHz:\n" +
                "• moc ${t.n(p)} dBm$powerNote, anteny ${t.n(gt)} dBi i ${t.n(gr)} dBi\n" +
                "• czułość odbiornika ok. ${t.n(sens)} dBm (granica SNR ${t.n(snrLimit(o.spreadingFactor))} dB)\n" +
                "• dopuszczalne tłumienie ok. ${t.n(budget)} dB\n" +
                "• zasięg teoretyczny w wolnej przestrzeni: ok. ${t.n(dKm, 0)} km\n" + distance +
                "To górna granica przy pełnej widoczności anten. Na Ziemi zasięg ogranicza horyzont radiowy: ${horizonList(q, t)}. Do tego przeszkody (budynki, las, wzgórza). Konkretne łącze sprawdzisz w planerze łącza.",
            "Link budget for the ${presetName(o)} preset$presetNote (SF${o.spreadingFactor}, ${t.n(o.bandwidth * 1000.0, 2)} kHz) at ${t.n(f, 2)} MHz:\n" +
                "• power ${t.n(p)} dBm$powerNote, antennas ${t.n(gt)} dBi and ${t.n(gr)} dBi\n" +
                "• receiver sensitivity about ${t.n(sens)} dBm (SNR limit ${t.n(snrLimit(o.spreadingFactor))} dB)\n" +
                "• allowed path loss about ${t.n(budget)} dB\n" +
                "• theoretical free-space range: about ${t.n(dKm, 0)} km\n" + distance +
                "That is the upper limit with the antennas in full view of each other. On Earth the radio horizon limits it: ${horizonList(q, t)}. Obstacles (buildings, forest, hills) come on top. Check a real link in the link planner.",
        )
    }

    /** The radio horizon for the heights in the question, or for both antennas at 2, 10 and 30 m. */
    private fun horizonList(q: String, t: Texts): String {
        val h = metres(q)
        if (h.isNotEmpty()) {
            val h1 = h[0]
            val h2 = h.getOrNull(1) ?: h1
            return t.s("anteny na ${t.n(h1)} m i ${t.n(h2)} m — ok. ${t.n(4.12 * (sqrt(h1) + sqrt(h2)))} km", "antennas at ${t.n(h1)} m and ${t.n(h2)} m — about ${t.n(4.12 * (sqrt(h1) + sqrt(h2)))} km")
        }
        return listOf(2.0, 10.0, 30.0).joinToString(", ") { m ->
            t.s("obie anteny na ${t.n(m)} m — ok. ${t.n(4.12 * 2 * sqrt(m))} km", "both antennas at ${t.n(m)} m — about ${t.n(4.12 * 2 * sqrt(m))} km")
        }
    }


    // ---------------------------------------------------------------- which preset

    /** Every preset firmware still has (Turbo, Lite and Tiny too), fastest first, so a new preset shows up in the table by itself. */
    private val COMPARED =
        ChannelOption.entries
            .filter { it.removedInFirmware == null }
            .sortedByDescending { it.spreadingFactor.let { sf -> sf * (it.bandwidth * 1e6 / 2.0.pow(sf)) * 4.0 / it.codingRate } }

    /** Share of the air time a network may use before it clogs (a rule of thumb), and the packets one node sends per 15 minutes. */
    private const val USABLE_AIR_SHARE = 0.25
    private const val PACKETS_PER_NODE_15_MIN = 4
    private const val PRESET_PAYLOAD_BYTES = 50

    private fun sensitivityDbm(o: ChannelOption) = -174 + 10 * log10(o.bandwidth * 1e6) + 6 + snrLimit(o.spreadingFactor)

    private fun bitRate(o: ChannelOption, cr: Int) = o.spreadingFactor * (o.bandwidth * 1e6 / 2.0.pow(o.spreadingFactor)) * 4.0 / cr

    /** About how many nodes a network can have, from the air time of one 50-byte packet (a rough order of magnitude, relaying not counted). */
    private fun nodesFit(o: ChannelOption, cr: Int) = (USABLE_AIR_SHARE * 900_000 / (airtimeMs(o, PRESET_PAYLOAD_BYTES, cr) * PACKETS_PER_NODE_15_MIN)).toInt()

    /** The coding rate a preset really takes: the asked one when firmware allows it on that preset, else its own. */
    private fun crFor(o: ChannelOption, asked: Int?) = if (asked != null && (asked == o.codingRate || asked in o.codingRateOverrides)) asked else o.codingRate

    /**
     * "Which preset do you recommend": a table of the presets with speed, sensitivity, time of a packet and about how
     * many nodes fit, worked out from the presets themselves, for the coding rate asked (4/5 ... 4/8) and with a pick for
     * the number of nodes in the question. The model never invents these numbers.
     */
    private fun presets(q: String, t: Texts, defaults: Defaults): String? {
        if (!has(q, "preset")) return null
        if (!has(q, "zalecan|polec|najlepsz|najlepiej|wybra|dobra[ćc]|dobierz|dobór|tabel|porówn|porown|zestawien|jaki preset|który preset|ktory preset|recommend|best preset|which preset|compare|choose|what preset")) return null
        if (has(q, "zmień|zmien|ustaw |przełącz|przelacz|change|switch|set the")) return null
        val asked = Regex("4\\s*[/:]\\s*([5-8])").find(q)?.groupValues?.get(1)?.toInt()
        val nodes = Regex("(\\d+)\\s*(?:urządz|urzadz|węzł|wezl|nod|device)").find(q)?.groupValues?.get(1)?.toIntOrNull()
        val sb = StringBuilder()
        sb.append(
            t.s(
                "Preset musi być taki sam u wszystkich w sieci, bo inaczej radia się nie słyszą. W naszej sieci (region EU_N_868) jest to Narrow Fast i tego nie zmieniaj, o ile nie zakładasz własnej, osobnej sieci. Dla własnej sieci dobór zależy od terenu, liczby urządzeń i tego, co ma przesyłać.",
                "The preset must be the same for everyone in the network, or the radios do not hear each other. In our network (region EU_N_868) it is Narrow Fast and you should not change it unless you build your own separate network. For your own network the choice depends on the terrain, the number of devices and what it has to carry.",
            ),
        )
        defaults.preset?.let { sb.append(" ").append(t.s("Twoje radio używa teraz: ${presetName(it)}.", "Your radio uses now: ${presetName(it)}.")) }
        sb.append("\n\n").append(
            t.s(
                "Porównanie (pakiet ${PRESET_PAYLOAD_BYTES} B, korekcja błędów ${asked?.let { "4/$it" } ?: "własna każdego presetu"}):",
                "Comparison (${PRESET_PAYLOAD_BYTES} B packet, error correction ${asked?.let { "4/$it" } ?: "each preset's own"}):",
            ),
        )
        val rows = COMPARED.map { o -> o to crFor(o, asked) }
        for ((o, cr) in rows) {
            val rate = bitRate(o, cr)
            val speed = if (rate >= 1000) "${t.n(rate / 1000, 1)} kb/s" else "${t.n(rate, 0)} b/s"
            sb.append("\n• ").append(presetName(o)).append(" (4/").append(cr).append("): ").append(speed)
                .append(t.s(", czułość ", ", sensitivity ")).append(t.n(sensitivityDbm(o), 1)).append(" dBm")
                .append(t.s(", pakiet ", ", packet ")).append(t.n(airtimeMs(o, PRESET_PAYLOAD_BYTES, cr), 0)).append(" ms")
                .append(t.s(", ok. ", ", about ")).append(nodesFit(o, cr)).append(t.s(" urządzeń", " devices"))
        }
        if (nodes != null) {
            // The slowest (longest range) preset that still leaves room for twice the nodes.
            val pick = rows.filter { (o, cr) -> nodesFit(o, cr) >= nodes * 2 }.minByOrNull { (o, cr) -> bitRate(o, cr) }
            sb.append("\n\n").append(
                if (pick != null) {
                    t.s("Dla $nodes urządzeń rozsądny jest ${presetName(pick.first)}: największy zasięg, przy którym ruch jeszcze się mieści z zapasem.", "For $nodes devices ${presetName(pick.first)} is a sensible pick: the longest range that still leaves room to spare.")
                } else {
                    t.s("Dla $nodes urządzeń w jednej sieci żaden z tych presetów nie ma zapasu: podziel sieć na kanały albo ogranicz ruch (rzadsze pozycje i telemetria).", "For $nodes devices in one network none of these presets has room to spare: split the network into channels or cut the traffic (rarer positions and telemetry).")
                },
            )
        }
        sb.append("\n\n").append(
            t.s(
                "Jak wybierać: mało urządzeń i długie łącza w otwartym terenie — Long Fast lub wolniejszy (większy zasięg, mała przepustowość). Średnia sieć — Medium Fast. Dużo urządzeń, miasto, krótkie łącza — Short Fast lub Short Slow. Las, wzgórza i zabudowa — wolniejszy preset daje zapas, ale zajmuje eter dłużej. Presety Turbo (500 kHz) są najszybsze, ale potrzebują szerokiego pasma i mają mniejszy zasięg. Lite (125 kHz), Narrow (62,5 kHz) i Tiny (15,6 kHz) są wąskie: mieszczą się w wąskich pasmach, jak nasze 868 MHz, i lepiej radzą sobie z zakłóceniami. Wyższa korekcja błędów (4/6 ... 4/8) ratuje pakiety zakłócone, ale wydłuża pakiet i zmniejsza liczbę urządzeń; zasięgu prawie nie zmienia.",
                "How to choose: few devices and long links in open country — Long Fast or slower (more range, little capacity). A medium network — Medium Fast. Many devices, a city, short links — Short Fast or Short Slow. Forest, hills and buildings — a slower preset gives margin but uses the air longer. Stronger error correction (4/6 ... 4/8) saves packets that are disturbed, but makes the packet longer and fits fewer devices; it hardly changes the range. The Turbo presets (500 kHz) are the fastest but need a wide band and have less range. Lite (125 kHz), Narrow (62.5 kHz) and Tiny (15.6 kHz) are narrow: they fit narrow bands such as our 868 MHz and cope better with interference.",
            ),
        )
        sb.append("\n").append(
            t.s(
                "Liczba urządzeń to rząd wielkości (przy ${(USABLE_AIR_SHARE * 100).toInt()} % czasu w eterze i $PACKETS_PER_NODE_15_MIN pakietach na urządzenie co 15 minut, bez przekazywania pakietów dalej; w sieci z przekazywaniem jest mniej).",
                "The number of devices is an order of magnitude (at ${(USABLE_AIR_SHARE * 100).toInt()} % of the air time and $PACKETS_PER_NODE_15_MIN packets per device every 15 minutes, relaying not counted; a network that relays fits fewer).",
            ),
        )
        return sb.toString()
    }

    /** LoRa time on air (Semtech formula, 16-symbol preamble, explicit header, CRC on). */
    fun airtimeMs(o: ChannelOption, payloadBytes: Int, codingRate: Int = o.codingRate): Double {
        val sf = o.spreadingFactor
        val bw = o.bandwidth * 1e6
        val tSym = 2.0.pow(sf) / bw * 1000
        val de = if (tSym > 16) 1 else 0
        val pre = (16 + 4.25) * tSym
        val n = 8 + max(ceil((8.0 * payloadBytes - 4 * sf + 28 + 16) / (4 * (sf - 2 * de))) * codingRate, 0.0)
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
