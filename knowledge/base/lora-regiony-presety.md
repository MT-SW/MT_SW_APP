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
Hop limit: 1–7, domyślnie 3 (nie może być większy niż 7); od firmware 2.8.x w MT_SW zalecamy 7. Moc nadawania: 0–30 dBm, domyślnie 0, co oznacza maksymalną dozwoloną moc ciągłą dla regionu. Preset: LONG_FAST. Spreading factor 5–12 (domyślnie 11), coding rate 4–8 (domyślnie 5, mianownik ułamka), nadawanie włączone.
Słowa: hop limit, skoki, moc, tx power, spreading factor, SF, coding rate, CR, domyślne, ustawienia lora
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Czym są presety TURBO?
SHORT_TURBO, MEDIUM_TURBO i LONG_TURBO używają szerokości 500 kHz: są najszybsze, ale mają najkrótszy zasięg i nie są dozwolone we wszystkich regionach.
Słowa: turbo, 500 kHz, szybki, zasięg
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Co trzeba ustawić przed rozpoczęciem pracy w sieci i jakie są ostrzeżenia dla regionów?
Aby nawiązać łączność, trzeba ustawić region (w CLI: pip3 install --upgrade meshtastic, potem meshtastic --set lora.region KOD_REGIONU; w aplikacji na iOS Ustawienia > Radio Configuration > LoRa). Po ustawieniu regionu węzeł zostaje na domyślnym presecie, który może nie pasować do lokalnej sieci: węzły muszą mieć zgodne ustawienia, żeby się słyszeć. EU_433 i EU_868 mają duty cycle 10 %, EU_866 2,5 % (10 % dla ROUTER i ROUTER_LATE). Regiony ITU są pasmami amatorskimi: tylko w trybie licencjonowanym i nie używać bez licencji krótkofalarskiej. Podana moc to sufit firmware, nie krajowy limit prawny: sprawdź krajowy plan pasm.
Słowa: ustawienie regionu, lora.region, pierwsza konfiguracja, region unset, duty cycle, limit prawny, ITU, plan pasm
Źródło: https://meshtastic.org/docs/getting-started/initial-config/ (sprawdzono 2026-10)

### Jak asystent podpowiada region LoRa dla mojego miejsca?
Gdy zapytasz o region, asystent opisuje, do czego służy, i sprawdza Twoje położenie: najpierw z pozycji radia, a gdy jej nie ma, z lokalizacji telefonu (tylko w tej chwili, nic nie jest wysyłane ani zapisywane). W Polsce i Europie podpowiada EU_868, a w województwie świętokrzyskim nasz EU_N_868 z presetem Narrow Fast (869,44165 MHz). To przybliżona podpowiedź: blisko granic może się mylić i nie jest poradą prawną.
Słowa: jaki region ustawić, region dla mnie, lokalizacja, położenie, podpowiedź regionu, EU_868, EU_N_868, świętokrzyskie
Źródło: Funkcje MT_SW: Inteligentne funkcje: asystent, tłumacz i dyktowanie

### Jaki preset jest zalecany i jak go dobrać (tabela, korekcja błędów)?
Preset musi być taki sam u wszystkich w sieci. W naszej sieci (EU_N_868) jest to Narrow Fast i nie zmieniamy go, chyba że ktoś zakłada własną, osobną sieć. Dla własnej sieci: mało urządzeń i długie łącza w otwartym terenie — Long Fast lub wolniejszy; średnia sieć — Medium Fast; dużo urządzeń, miasto, krótkie łącza — Short Fast lub Short Slow; las, wzgórza i zabudowa — wolniejszy preset daje zapas, ale zajmuje eter dłużej. Wyższa korekcja błędów (4/6, 4/7, 4/8) ratuje zakłócone pakiety, ale wydłuża pakiet i zmniejsza liczbę urządzeń, zasięg zmienia prawie wcale. Asystent pokazuje tabelę presetów (szybkość, czułość, czas pakietu, orientacyjna liczba urządzeń) policzoną dla wybranej korekcji błędów i dla podanej liczby urządzeń.
Słowa: jaki preset, zalecany preset, który preset wybrać, tabela presetów, porównanie presetów, korekcja błędów, coding rate, CR, 4/5, 4/8, ile urządzeń, pojemność sieci
Źródło: Funkcje MT_SW: Inteligentne funkcje: asystent, tłumacz i dyktowanie
