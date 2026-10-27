# Układy, BLE i ogniwa w węźle

### Bluetooth Low Energy: pasmo, zasięg, moc, bezpieczeństwo
BLE działa w paśmie 2,400 do 2,4835 GHz, używa 40 kanałów po 2 MHz (klasyczny Bluetooth: 79 po 1 MHz) i modulacji GFSK z 1 Mbit/s; Bluetooth 5 dodaje opcjonalnie 2 Mbit/s. Nominalny zasięg poniżej 100 m, moc maksymalna 10 mW, a w Bluetooth 5 do 100 mW. Szyfrowanie to 128-bitowy AES w trybie CCM, pakiety mają 24-bitowe CRC. Wersja 5 ma tryby Coded PHY: S=2 mniej więcej podwaja zasięg przy 500 kbit/s, S=8 około czterokrotnie przy 125 kbit/s. Dane rozgłoszeniowe urosły z 31 bajtów (4.x) do 2 do 256 bajtów na serię. Klasyczny Bluetooth i BLE nie są bezpośrednio zgodne, ale urządzenie dwutrybowe obsłuży oba. Zasięg BLE do telefonu to więc kilkadziesiąt metrów, co tłumaczy, czemu węzeł Meshtastic gubi połączenie przez ścianę (wniosek ogólny, nie ze źródła).
Słowa: Bluetooth Low Energy, BLE, GATT, Coded PHY, zasięg bluetooth, AES-CCM, 2.4 GHz, Bluetooth 5
Źródło: https://en.wikipedia.org/wiki/Bluetooth_Low_Energy (sprawdzono 2026-10)

### BLE kontra klasyczny Bluetooth: zużycie energii i opóźnienia
Według tabeli ze źródła: prąd szczytowy poniżej 15 mA (klasyczny poniżej 30 mA), pobór mocy 0,01 do 0,50 W zależnie od zastosowania (klasyczny 1 W jako odniesienie), czas wybudzenia 6 ms (klasyczny około 100 ms), minimalny czas wysłania danych 3 ms, przepustowość użyteczna 0,27 do 1,37 Mbit/s (klasyczny 0,7 do 2,1). BLE komunikuje się przez profil GATT: serwer udostępnia usługi, charakterystyki i deskryptory z identyfikatorami UUID, a klient odczytuje, zapisuje i subskrybuje powiadomienia. LE Audio (5.2/5.3) używa kodeka LC3 i daje opóźnienie 20 do 30 ms zamiast 100 do 200 ms.
Słowa: BLE kontra Bluetooth, GATT, UUID, zużycie energii bluetooth, LE Audio, LC3, wybudzanie
Źródło: https://en.wikipedia.org/wiki/Bluetooth_Low_Energy (sprawdzono 2026-10)

### Rodzina ESP32: czym się różnią warianty
Według tabeli ze źródła: ESP32 (Xtensa LX6, 1 lub 2 rdzenie, 240 MHz, Wi-Fi b/g/n, Bluetooth 4.2 z BR/EDR i LE, 520 KiB SRAM, 34 GPIO, brak USB), ESP32-S2 (jeden rdzeń LX7, Wi-Fi, bez Bluetooth, 43 GPIO), ESP32-S3 (dwa rdzenie LX7, Wi-Fi, Bluetooth 5 LE, 512 KiB SRAM, 45 GPIO, USB OTG, instrukcje wektorowe), ESP32-C3 (RISC-V 160 MHz, Bluetooth 5, 22 GPIO) i ESP32-C6 (RISC-V, Wi-Fi 6, Bluetooth 5.3, 30 GPIO). Oryginalny ESP32 ma 3 UART-y, 2 I²C, 2 I²S, dwa 12-bitowe przetworniki ADC i dwa 8-bitowe DAC. Wersje D0WDQ6 i D0WD są oznaczone „nie zalecane do nowych projektów”. Prądu uśpienia strona nie podaje. Wiele płytek Meshtastic (Heltec V3, T-Beam, T-Deck) opiera się na ESP32 lub S3, ale który chip ma konkretny model, sprawdź w jego opisie.
Słowa: ESP32, ESP32-S3, ESP32-C3, ESP32-C6, ESP32-S2, Xtensa, RISC-V, GPIO, PSRAM, mikrokontroler
Źródło: https://en.wikipedia.org/wiki/ESP32 (sprawdzono 2026-10)

### nRF52: czym jest ten mikrokontroler
Seria nRF52 firmy Nordic Semiconductor to układy z rdzeniem ARM Cortex-M4 (od 2015), z Bluetooth LE i nastawione na bardzo niski pobór energii; według źródła są w milionach urządzeń, np. myszach i klawiaturach. Szczegóły podane tylko dla nRF52833: 64 MHz, 128 KB RAM, 512 KB flash, +8 dBm, temperatura od minus 40 do 105 °C. Układ nRF52840 jest wymieniony jako jeden z najpopularniejszych, ale bez opisu. Nic o bootloaderze UF2 strona nie mówi; o wgrywaniu na nRF52 zobacz wpisy o firmware. Takie płytki (np. RAK4631, T-Echo) słyną z małego poboru prądu, co przydaje się przy solarach (to wnioskowanie z ogólnej wiedzy).
Słowa: nRF52, nRF52840, Nordic, Cortex-M4, RAK4631, T-Echo, niski pobór prądu, bluetooth LE
Źródło: https://en.wikipedia.org/wiki/Nordic_Semiconductor (sprawdzono 2026-10)

### Ogniwa fotowoltaiczne: rodzaje, cień, napięcie
Monokrystaliczne są najbardziej wydajne wśród krzemowych, polikrystaliczne tańsze i mniej wydajne. Cienkowarstwowe: CIGS osiąga około 20%, tellurek kadmu rywalizuje kosztem na wat. Krzemowe ogniwo jednozłączowe zbliża się do praktycznych około 26% przy pełnym słońcu, granica teoretyczna wynosi 33,16%. Pojedyncze ogniwo krzemowe daje maksymalnie około 0,5 do 0,6 V, więc panele łączy się szeregowo (wyższe napięcie) lub równolegle (większy prąd). Cień na ogniwach bez diod bocznikujących może znacznie obniżyć moc, wyłączyć gałąź, a nawet uszkodzić zacienione ogniwa; diody bypass to ograniczają. Współczynnik wypełnienia typowych ogniw komercyjnych przekraczał 0,70. Wartości współczynnika temperaturowego strona nie podaje; sprawdź kartę katalogową panelu.
Słowa: panel słoneczny, ogniwo fotowoltaiczne, monokrystaliczny, polikrystaliczny, cień, dioda bypass, Voc, fill factor, solar
Źródło: https://en.wikipedia.org/wiki/Solar_cell (sprawdzono 2026-10)

### MPPT: po co śledzenie punktu mocy maksymalnej
MPPT dopasowuje obciążenie tak, aby panel pracował w punkcie mocy maksymalnej (MPP), który zmienia się z nasłonecznieniem, cieniem i temperaturą. Zwykle jest to przetwornica DC-DC zmieniająca impedancję widzianą przez panel. Pomaga przy zmiennych warunkach, rano, gdy rozładowany akumulator ma napięcie dużo niższe niż MPP panelu, i przy zacienieniu (krzywa ma wtedy kilka szczytów, niektóre algorytmy utykają na lokalnym maksimum). Gdy akumulator jest pełny, a nie ma odbiornika, MPPT musi odsunąć panel od szczytu. Porównania z kontrolerem PWM strona nie zawiera i nie podaje procentowego zysku, więc dla małych paneli nie obiecuj konkretnej poprawy.
Słowa: MPPT, PWM, regulator solarny, punkt mocy maksymalnej, przetwornica DC-DC, panel słoneczny, akumulator
Źródło: https://en.wikipedia.org/wiki/Maximum_power_point_tracking (sprawdzono 2026-10)

