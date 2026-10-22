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

/** One language the offline translator can work with. [nllb] is the model's own language code. */
data class TranslatorLanguage(val iso: String, val nllb: String, val polishName: String)

/** The languages offered in the translator (the model knows ~200; these are the ones people on the mesh actually use). */
object TranslatorLanguages {
    val all: List<TranslatorLanguage> =
        listOf(
            TranslatorLanguage("pl", "pol_Latn", "Polski"),
            TranslatorLanguage("en", "eng_Latn", "Angielski"),
            TranslatorLanguage("de", "deu_Latn", "Niemiecki"),
            TranslatorLanguage("cs", "ces_Latn", "Czeski"),
            TranslatorLanguage("sk", "slk_Latn", "Słowacki"),
            TranslatorLanguage("uk", "ukr_Cyrl", "Ukraiński"),
            TranslatorLanguage("ru", "rus_Cyrl", "Rosyjski"),
            TranslatorLanguage("be", "bel_Cyrl", "Białoruski"),
            TranslatorLanguage("fr", "fra_Latn", "Francuski"),
            TranslatorLanguage("es", "spa_Latn", "Hiszpański"),
            TranslatorLanguage("it", "ita_Latn", "Włoski"),
            TranslatorLanguage("pt", "por_Latn", "Portugalski"),
            TranslatorLanguage("nl", "nld_Latn", "Niderlandzki"),
            TranslatorLanguage("sv", "swe_Latn", "Szwedzki"),
            TranslatorLanguage("da", "dan_Latn", "Duński"),
            TranslatorLanguage("nb", "nob_Latn", "Norweski"),
            TranslatorLanguage("fi", "fin_Latn", "Fiński"),
            TranslatorLanguage("hu", "hun_Latn", "Węgierski"),
            TranslatorLanguage("ro", "ron_Latn", "Rumuński"),
            TranslatorLanguage("bg", "bul_Cyrl", "Bułgarski"),
            TranslatorLanguage("hr", "hrv_Latn", "Chorwacki"),
            TranslatorLanguage("sl", "slv_Latn", "Słoweński"),
            TranslatorLanguage("sr", "srp_Cyrl", "Serbski"),
            TranslatorLanguage("lt", "lit_Latn", "Litewski"),
            TranslatorLanguage("lv", "lvs_Latn", "Łotewski"),
            TranslatorLanguage("et", "est_Latn", "Estoński"),
            TranslatorLanguage("el", "ell_Grek", "Grecki"),
            TranslatorLanguage("tr", "tur_Latn", "Turecki"),
            TranslatorLanguage("ar", "arb_Arab", "Arabski"),
            TranslatorLanguage("he", "heb_Hebr", "Hebrajski"),
            TranslatorLanguage("zh", "zho_Hans", "Chiński"),
            TranslatorLanguage("ja", "jpn_Jpan", "Japoński"),
            TranslatorLanguage("ko", "kor_Hang", "Koreański"),
        )

    private val byIso = all.associateBy { it.iso }
    private val byNllb = all.associateBy { it.nllb }

    /** Finds a language from a locale tag such as "pl", "pl-PL" or "en_US". */
    fun fromLocale(tag: String): TranslatorLanguage? {
        val base = tag.substringBefore('-').substringBefore('_').lowercase()
        val fixed =
            when (base) {
                "no" -> "nb"
                "iw" -> "he"
                else -> base
            }
        return byIso[fixed]
    }

    fun fromNllb(code: String): TranslatorLanguage? = byNllb[code]
}
