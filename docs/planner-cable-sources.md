# Cable / connector data sources (for licence and credit review)

Values are factual datasheet figures re-typed into CableDb.kt; no datasheet text or graphics are copied.
Conversion: dB/100 ft -> dB/100 m = x * 100 / 30.48 (done in code, `ft()` helper); dB/m -> x*100.
Model: log-log interpolation inside the datasheet range; below it sqrt(f); above it a*sqrt(f)+b*f fit on the
last two points bounded by sqrt(f) and linear-in-f scaling. Valid range clamped to 20 MHz..20 GHz.
Spec pages were read via a text-extraction tool; values were not re-verified against the PDFs by eye.

| Cable | Manufacturer / URL | Notes |
|---|---|---|
| RG174 | Huber+Suhner RG_174/U: https://www.mouser.com/datasheet/3/1498/1/H+S_RG_174_U_EN.PDF | dB/m x100; to 1 GHz; approximate |
| RG178 | Huber+Suhner RG_178_B/U: https://www.hubersuhner.com/Asset/eyJpZGVudGlmaWVyIjoxMTY0NDksInR5cGUiOiJhc3NldCJ9/5uPHdE9YtZiMGuxL/H+S_RG_178_BU_EN.PDF | to 3 GHz |
| RG316 | Huber+Suhner RG_316/U: https://www.mouser.com/datasheet/3/1498/1/H+S_RG_316_U_EN.PDF | to 3 GHz |
| RG58 | Huber+Suhner RG_58_C/U: https://www.mouser.com/datasheet/2/829/HUBER_2bSUHNER_RG_58_CU_DataSheet-1489871.pdf | values rounded to 0.01 dB/m; to 1 GHz; approximate |
| RG59 | Belden 8241 (75 ohm RG-59/U type): https://www.markertek.com/Attachments/Specifications/Belden/82410101000-Specifications.pdf | per 100 ft; 75 ohm, to 1 GHz; approximate |
| RG8X | Belden 9258: https://catalog.belden.com/techdata/EN/9258_techdata.pdf | per 100 ft; to 1 GHz; approximate |
| RG213 | Huber+Suhner RG_213/U: https://www.mouser.com/datasheet/2/829/HUBER_2bSUHNER_RG_213_U_DataSheet-1489695.pdf (formula a=0.1679, b=0.0585, f in GHz, from the H+S 3628 sheet) | points generated from the formula, to 1 GHz; approximate |
| LMR-100A/195/200/240/600 | Times Microwave: https://www.talleycom.com/images/pdf/LMR-100A.pdf , https://www.talleycom.com/images/pdf/TIMLMR-195.pdf , https://timesmicrowave.com/wp-content/uploads/2022/06/lmr-200-datasheet.pdf , https://timesmicrowave.com/wp-content/uploads/2022/06/lmr-240-datasheet.pdf , https://www.talleycom.com/images/pdf/TIMLMR600.pdf | published dB/100 m column used |
| LMR-400 | Times Microwave via Fairview: https://www.fairviewmicrowave.com/content/dam/infinite-electronics/product-assets/fairview-microwave/product-datasheets/LMR-400-BULK.pdf | 12.8 dB/100 m @ 900 MHz (3.9 dB/100 ft) |
| H155 | Belden H155 PE: https://uk.misumi-ec.com/pdf/vona/el/ZBD1/ZBD1_H155PE_Datasheet_en_1.pdf | dB/100 m, to 2.05 GHz |
| H1000 | Belden H1000 PE: https://www.qsl.net/ok1mkq/technika/draty_koaxy/koaxy/belden/h1000pe-belden.pdf | dB/100 m, to 2.05 GHz |
| Aircell 5 | SSB-Electronic (copy): https://www.rigpix.com/otherusefulinfo/coax/aircell5.pdf | dB/100 m |
| Aircell 7 | SSB-Electronic (copy): https://www.haje.nl/pub/pdf/coax/DB_Aircell7_UK.pdf | dB/100 m |
| Ecoflex 10 | SSB-Electronic (copy): https://www.professionalwireless.com/wp-content/uploads/2020/07/Ecoflex10-Spec-Sheet.pdf | dB/100 m |
| Ecoflex 15 | SSB-Electronic (copy): https://www.dnd.hu/uploads/termek_doc/SSB_ecoflex_15_EN.pdf | dB/100 m |
| Semi-rigid .085 | RG405-type .086 sheet: https://www.uniteng.com/neildocs/datasheets/RG405-U.pdf | only 1/10/20 GHz, per 100 ft; approximate |
| Semi-rigid .141 | Fairview FM-SR141CU: https://www.fairviewmicrowave.com/content/dam/infinite-electronics/product-assets/fairview-microwave/product-datasheets/FM-SR141CU-STR.pdf | only 1/10/20 GHz, per 100 ft; approximate |
| LDF4-50A | CommScope/Andrew HELIAX (copy): https://www.talleycom.com/images/pdf/LDF4-50A.pdf | dB/100 m, selected of 35 points |

Connectors: no manufacturer publishes a single insertion-loss figure; values are engineering estimates from
typical VSWR/loss class (SMA/N/7-16 precision connectors ~0.02-0.05 dB, U.FL/MHF4 0.1-0.25 dB, UHF
non-constant-impedance and poor above ~300 MHz). Flagged approximate: U.FL, MHF4, MMCX, UHF, adapter.
