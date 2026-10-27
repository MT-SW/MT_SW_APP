# Urządzenia: LILYGO, RAK, Seeed i wybór platformy (tabela z dokumentacji)

### Jak wybrać platformę: nRF52 czy ESP32, SX126x czy SX127x?
Oficjalna strona zdecydowanie zaleca nowsze radia Semtech SX126x lub LR11xx zamiast starszych SX127x. Płytki nRF52 (RAK, Seeed na nRF52840) pobierają mniej prądu, więc zwykle są preferowane do solarów i urządzeń ręcznych. Płytki ESP32 zużywają więcej energii, ale są tańsze; dokumentacja widzi je w węzłach z zasilaniem sieciowym, w ręcznych urządzeniach pracujących dzień lub dwa i tam, gdzie potrzebne jest Wi-Fi lub więcej RAM. Wszystkie wymienione płytki łączą się ze sobą w jednej sieci. Ulubieńcy społeczności według strony: RAK Starter Kit (RAK19007 + RAK4631), Seeed Card Tracker T1000-E, Seeed Wio Tracker L1, Heltec Mesh Node T114, B&Q Nano G2 Ultra, B&Q Station G2 i LILYGO LoRa T3-S3.
Słowa: wybór urządzenia, nRF52, ESP32, SX126x, SX127x, LR1110, pobór prądu, solar, RAK4631, T114, Nano G2, Station G2
Źródło: https://meshtastic.org/docs/hardware/devices/ (sprawdzono 2026-10)

### LILYGO: tabela układów (T-Beam, T-Echo, T-Deck, T-Watch, e-paper)
T-Beam S3-Core i Supreme: ESP32-S3, SX1262, Wi-Fi, BT 5.0, GPS, koszyk na akumulator 18650. T-Echo i T-Echo Plus: nRF52840, SX1262, GPS, ekran e-ink, wbudowany akumulator w obudowie wtryskowej, niski pobór. LoRa32 T3-S3: ESP32-S3, radio do wyboru SX1262, SX1276, SX1280 lub LR1121 (SX1276 nie jest już zalecany), bez GPS. T-Deck: bez GPS, T-Deck Plus i Pro: z GPS. T-Lora Pager: ESP32-S3, LR1121, GPS. T-Watch S3 Plus (z GPS) i S3 (bez): zegarek z ekranem dotykowym i wibracją. Mini ePaper S3: ogniwo 18350 nie w komplecie. T5 E-Paper S3 Pro V2 z GNSS, V1 bez. T-LoRa C6: ESP32-C6, Bluetooth w Meshtastic nie jest tam obsługiwany. Pasm strona nie podaje.
Słowa: LILYGO, T-Beam, T-Beam Supreme, T-Echo, T3-S3, T-Watch, T-Lora Pager, Mini ePaper, T5 E-Paper, T-LoRa C6, 18650
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/ (sprawdzono 2026-10)

### RAK WisBlock: moduły rdzeniowe, bazy i peryferia
Rdzenie: RAK4631 (nRF52840, SX1262, BT 5.0, niski pobór, w Starter Kit; GPS jako dodatek), RAK11200 (ESP32, Wi-Fi, BT 4.2, radio jako dodatek), RAK11310 (RP2040, SX1262, bez Wi-Fi i BT), RAK3312 (ESP32-S3, SX1262, Wi-Fi, BT 5.0). Bazy: RAK19007 (druga generacja), RAK19003 (mini), RAK19001 (podwójne IO), RAK5005-O (starsza, wycofana). Peryferia: GPS RAK1910 i RAK12500; czujniki RAK1901 (temperatura i wilgotność), RAK1902 (ciśnienie), RAK1906 (środowisko), RAK12013 (radar); inne RAK18001 (buzzer), RAK14001 (RGB), RAK12002 (zegar RTC), RAK13800 (Ethernet). Wyświetlacze: RAK1921 OLED 0,96", RAK14000 e-ink 2,13". RAK ma też narzędzie Meshtastic Designer do doboru modułów.
Słowa: RAK WisBlock, RAK4631, RAK19007, RAK19003, RAK11200, RAK3312, RAK1910, RAK12500, RAK14000, modułowy węzeł, Designer
Źródło: https://meshtastic.org/docs/hardware/devices/rak-wireless/ (sprawdzono 2026-10)

### RAK WisMesh: gotowe urządzenia i bramki MQTT
Pocket V2 (ręczny, ekran, GPS, akumulator), Pocket Mini (ultrakompaktowy), Tag (przenośny tracker IP66 z akumulatorem 1000 mAh), Tap (odporny na pogodę z ekranem dotykowym), Board ONE (kompaktowy, rozszerzalny), 1W Booster (SKY66122, moc 1 W i lepsza czułość), Repeater i Repeater Mini (solarne, wodoodporne, stałe z anteną o dużym zysku lub przenośne z kilkoma sposobami montażu). Dodatkowo bramka MQTT Ethernet (opcjonalnie PoE) i bramka MQTT Wi-Fi, które łączą sieć mesh z internetem.
Słowa: WisMesh, Pocket, Tag, Tap, Board ONE, Repeater, bramka MQTT, PoE, 1W Booster, IP66
Źródło: https://meshtastic.org/docs/hardware/devices/rak-wireless/ (sprawdzono 2026-10)

### Seeed Studio: T1000-E, SenseCAP Indicator, Wio Tracker, zestawy XIAO
SenseCAP Indicator: ESP32-S3 i RP2040, SX1262, Wi-Fi, BT 5.0, GPS opcjonalny, 4-calowy ekran dotykowy, dostarczany z wgranym firmware. T1000-E (karta-tracker): nRF52840, radio LR1110, BT 5.1, GPS, bez Wi-Fi. Wio Tracker L1: nRF52840, SX1262, GPS, bez Wi-Fi; wariant "Pro" z obudową i akumulatorem. Zestawy XIAO nRF52840 i XIAO ESP32-S3 z Wio-SX1262: do własnych projektów, GPS opcjonalny. Pasm i problemów z wgrywaniem strona nie opisuje. Do T1000-E używaj adaptera UART 3,3 V, nie 5 V.
Słowa: Seeed, T1000-E, SenseCAP Indicator, Wio Tracker L1, XIAO, nRF52840, LR1110, karta tracker, zestaw deweloperski
Źródło: https://meshtastic.org/docs/hardware/devices/seeed-studio/ (sprawdzono 2026-10; ostatnie zdanie: z modułu Serial)
