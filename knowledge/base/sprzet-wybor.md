# Sprzęt: rodziny urządzeń i wybór

### Jakie rodziny urządzeń są wspierane i na jakich układach?
RAK: głównie nRF52840 (RAK4631, zestaw z płytą RAK19007), RAK11310 (RP2040), RAK3312 (ESP32-S3). LILYGO: głównie ESP32-S3 (T-Beam S3-Core, T-Beam SUPREME, T-Beam 1W, LoRa T3-S3, T-Deck, T-Lora Pager), T-Echo na nRF52840. Heltec: głównie ESP32-S3 (LoRa32 V3/V3.1, V4, Wireless Tracker, Vision Master), a Mesh Node T114, T096, T1, MeshPocket na nRF52840. Seeed: Card Tracker T1000-E, SenseCAP Solar Node, Wio Tracker L1 (nRF52840), Wio Tracker L2 Pro i SenseCAP Indicator (ESP32-S3). B&Q: seria Nano (np. Nano G2 Ultra) na nRF52840.
Słowa: sprzęt, urządzenia, RAK4631, T-Beam, Heltec V3, T114, T1000-E, T-Echo, Nano G2, ESP32-S3, nRF52840, lista, model
Źródło: https://meshtastic.org/docs/hardware/devices/ (sprawdzono 2026-10)

### ESP32 czy nRF52: co wybrać?
nRF52 pobiera mniej prądu, więc jest zwykle preferowany do zasilania słonecznego i urządzeń ręcznych. ESP32 pobiera więcej prądu, ale jest zwykle tańszy; nadaje się do zasilania z sieci, do urządzeń na dzień-dwa pracy i do projektów potrzebujących Wi-Fi lub więcej pamięci RAM. Wszystkie wymienione płytki mogą ze sobą tworzyć sieć.
Słowa: ESP32, nRF52, pobór prądu, solar, bateria, Wi-Fi, RAM, wybór sprzętu, energooszczędność
Źródło: https://meshtastic.org/docs/hardware/devices/ (sprawdzono 2026-10)

### Jaki układ radiowy wybrać: SX126x czy SX127x?
Dokumentacja zdecydowanie zaleca urządzenia z nowszymi układami Semtech SX126x lub LR11xx zamiast starszej serii SX127x. Jako ulubione społeczności wymieniono: RAK19007 z RAK4631, Seeed Card Tracker T1000-E, Seeed Wio Tracker L1, Heltec Mesh Node T114, Nano G2 Ultra, Station G2 i LILYGO LoRa T3-S3.
Słowa: SX1262, SX1276, SX127x, LR1110, LR11xx, układ radiowy, chip, polecany sprzęt, ulubione
Źródło: https://meshtastic.org/docs/hardware/devices/ (sprawdzono 2026-10)
