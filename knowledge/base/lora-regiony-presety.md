# LoRa: regiony, presety i parametry

### Jaki region i preset działa w Polsce?
W Polsce pasmo 868 MHz: regiony EU_868 (869,4–869,65 MHz, 10 % czasu nadawania, do 27 dBm) i EU_N_868 (to samo pasmo, inny plan kanałów, do 27 dBm). Każdy z regionów EU przyjmuje tylko własne presety. Nasza sieć świętokrzyska używa EU_N_868 z presetem Narrow Fast (62,5 kHz), częstotliwość 869,44165 MHz.
Słowa: region, preset, EU_868, EU_N_868, Narrow Fast, Polska, 868, 869, moc, dBm, duty cycle, wąskopasmowy
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Jakie są presety wąskopasmowe?
LITE_FAST i LITE_SLOW mają 125 kHz (region EU_866). NARROW_FAST i NARROW_SLOW mają 62,5 kHz (EU_N_868 oraz pasma ITU 70 cm i 125 cm). TINY_FAST i TINY_SLOW mają 15,6 kHz (pasma ITU 2 m). Presety ITU wymagają trybu licencjonowanego i licencji krótkofalarskiej.
Słowa: Lite, Narrow, Tiny, 62.5, 15.6, 125 kHz, szerokość pasma, bandwidth, ITU, krótkofalarskie
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Jakie są domyślne wartości ustawień LoRa?
Hop limit: 1–7, domyślnie 3 (nie może być większy niż 7). Moc nadawania: 0–30 dBm, domyślnie 0, co oznacza maksymalną dozwoloną moc ciągłą dla regionu. Preset: LONG_FAST. Spreading factor 5–12 (domyślnie 11), coding rate 4–8 (domyślnie 5, mianownik ułamka), nadawanie włączone.
Słowa: hop limit, skoki, moc, tx power, spreading factor, SF, coding rate, CR, domyślne, ustawienia lora
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Czym są presety TURBO?
SHORT_TURBO, MEDIUM_TURBO i LONG_TURBO używają szerokości 500 kHz: są najszybsze, ale mają najkrótszy zasięg i nie są dozwolone we wszystkich regionach.
Słowa: turbo, 500 kHz, szybki, zasięg
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)
