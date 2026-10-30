<!-- Baza wiedzy MT_SW: jak policzyć czas nadawania pakietu LoRa (Time on Air). Tylko informacyjnie. -->
# LoRa: czas nadawania pakietu (Time on Air) krok po kroku

### Jak policzyć, ile trwa nadawanie jednego pakietu LoRa w paśmie EU868?
Czas nadawania (Time on Air, ToA) to czas, przez który antena jest zasilana i nadaje dane. To nie to samo co czas dotarcia do odbiorcy. Liczy się go w czterech krokach. Najpierw czas symbolu: Ts = 2^SF / BW, gdzie SF to spreading factor (7–12), a BW to szerokość pasma w hercach. Potem preambuła: Tpreamble = (npreamble + 4,25) × Ts, a dla EU868 npreamble wynosi 8. Następnie ładunek: Tpayload = Ts × (8 + max(ceil((8PL − 4SF + 28 + 16CRC − 20H) / (4(SF − 2DE))) × (CR + 4), 0)), gdzie PL to liczba bajtów danych, CRC to 1 przy włączonym CRC, H to 0 dla nagłówka jawnego, DE to flaga LowDataRateOptimize, a CR to współczynnik kodowania. Na końcu ToA = Tpreamble + Tpayload. Dla 10 bajtów, CR = 1 i pasma 125 kHz wychodzi około 41 ms przy SF7 i około 72 ms przy SF8. Każdy wyższy SF wydłuża nadawanie mniej więcej dwukrotnie. Źródło podaje wzory i przykłady, a wniosek o wpływie na limit czasu nadawania w sieci to już z ogólnej wiedzy.
Słowa: time on air, toa, czas nadawania, airtime, obliczenie, wzór, spreading factor, sf7, sf8, preambuła, symbol, pasmo 125 kHz, eu868, payload, pakiet lora, ile trwa wysyłka
Źródło: https://www.mobilefish.com/download/lora/lora_part17.pdf (sprawdzono 2026-10; samouczek LoRa, przykład dla 10 bajtów i CR = 1)

### Kiedy włączyć LowDataRateOptimize przy LoRa?
Flaga LowDataRateOptimize (DE) ma być włączona, gdy pasmo wynosi 125 kHz, a SF jest równy 11 lub wyższy. Nadajnik i odbiornik muszą mieć to samo ustawienie. Dla SF7 i SF8 flaga nie jest używana i DE = 0. Źródło opisuje to jako regułę dla pasma 125 kHz. Jak to wygląda w konkretnym firmware Meshtastic, sprawdź w jego ustawieniach LoRa, bo źródło o nich nie mówi.
Słowa: lowdatarateoptimize, ldro, low data rate, de, sf11, sf12, 125 kHz, optymalizacja, flaga, nadajnik odbiornik, ustawienia lora
Źródło: https://www.mobilefish.com/download/lora/lora_part17.pdf (sprawdzono 2026-10; samouczek LoRa)
