# Budowa własnych urządzeń z części (DIY)

### Jak zbudować prosty węzeł z ESP32 i modułu LoRa?
Przykład z poradnika: płytka ESP-32S WiFi+Bluetooth Devkit V1 (30-pin; 38-pin, jeśli dodasz więcej czujników), moduł LoRa RA-02 (wersja DIP), antena z kablem IPEX–SMA, 14 przewodów żeńsko-żeńskich do modułu LoRa, opcjonalnie wyświetlacz OLED 0,96" I2C (ułatwia parowanie) i lutownica z cyną 60/40. Poradnik opisuje wariant 433 MHz; moduł i antena muszą pasować do pasma, w którym pracujesz (u nas 868 MHz). Firmware: web flasher w Chrome lub Edge, urządzenie "DIY V1", najnowsza stabilna wersja; przy nieudanym flashowaniu skrypt CLI. Okablowanie bierz ze schematu (poradnik odsyła do repozytorium NanoVHF), nie z pamięci.
Słowa: DIY, własny węzeł, ESP32, RA-02, SX1278, DIY V1, składanie, części, lutowanie, schemat, okablowanie, NanoVHF
Źródło: https://adrelien.com/blog/meshtastic-diy-how-to-build-your-own-meshtatic-node-esp32-lora-radio/ (sprawdzono 2026-10)

### Na co uważać przy samodzielnym montażu ESP32 + LoRa?
Przed zasilaniem sprawdź etykiety pinów ze schematem (kolejność bywa różna; niektóre wyświetlacze mają SCL zamiast SCK). Cztery piny masy modułu LoRa połącz razem i do jednego przewodu masy ESP32; izoluj połączenia taśmą. Wyświetlacz podłączaj do Vin tylko, jeśli obsługuje 5 V (sprawdź specyfikację). Fabryczna antena RA-02 jest często słaba (zasięg 10–50 m). Antenę podłącz przed włączeniem zasilania. Niektóre płytki ESP32 przy flashowaniu wymagają przytrzymania przycisku BOOT. Przy parowaniu wybierz właściwy region (w poradniku EU433; u nas EU_N_868 lub EU_868).
Słowa: błędy, pułapki, masa, antena, BOOT, piny, SCL, 5V, parowanie, region, zwarcie
Źródło: https://adrelien.com/blog/meshtastic-diy-how-to-build-your-own-meshtatic-node-esp32-lora-radio/ (sprawdzono 2026-10)

### Jakie długości anten podaje poradnik dla 433 MHz?
Około 40 cm dla stacji bazowej i 20–24 cm dla urządzeń ręcznych (to wartości dla 433 MHz z poradnika; na 868 MHz fale są o połowę krótsze, więc anteny też, patrz kalkulator anten w aplikacji).
Słowa: długość anteny, 433, 40 cm, 20 cm, ćwierćfalowa, bat
Źródło: https://adrelien.com/blog/meshtastic-diy-how-to-build-your-own-meshtatic-node-esp32-lora-radio/ (sprawdzono 2026-10)

### Jak zbudować tani węzeł nRF52840 (FakeTec, Pro Micro + HT-RA62)?
Koszt ok. 11 USD: NRF52840 Pro Micro (ok. 3,59 USD), moduł LoRa HT-RA62 SMD (ok. 4,92 USD), płytki FakeTec PCB (5 szt. ok. 2 USD z JLCPCB; pliki Gerber v4 z repozytorium FakeTec), lutownica, pinceta, okulary ochronne, taśma izolacyjna. Montaż: Pro Micro wlutować nóżkami od spodu płytki, złącze IPEX modułu LoRa ustawić w lewym górnym rogu, przykleić moduł taśmą (nie ma pinów), przylutować jeden narożny pad (GND lub ANT), sprawdzić położenie i dopiero lutować resztę. Brak WiFi: nie zadziała PAX counter ani MQTT przez WiFi; GPS tylko z opcjonalnymi elementami.
Słowa: FakeTec, nRF52840, Pro Micro, HT-RA62, nice nano, JLCPCB, Gerber, SMD, tanio, ręczny montaż, lutowanie SMD
Źródło: https://adrelien.com/diy-meshtastic-how-to-build-your-own-meshtastic-device-with-faketec-pcb-nrf52840/ (sprawdzono 2026-10)

### Jak zaktualizować bootloader w płytce nRF52840 Pro Micro (nice!nano)?
Większość płytek ma stary bootloader. Połącz kablem z danymi, wejdź w DFU przez zwarcie GND i RST raz lub dwa razy, sprawdź wersję w INFO_UF2.TXT. Z wydań Adafruit nRF52 Bootloader wybierz następną wersję po Twojej (aktualizacje robi się po jednej wersji), pobierz pliki nice_nano_bootloader-X.X.X (HEX i UF2), przeciągnij najpierw HEX, potem UF2; urządzenie się odłączy i zrestartuje. Powtarzaj do najnowszej. Potem firmware: web flasher, wariant "NRF52 Pro-micro DIY", plik UF2 do dysku w trybie DFU, czekaj 1–2 minuty.
Słowa: bootloader, nice nano, DFU, GND RST, HEX, UF2, po jednej wersji, aktualizacja krok po kroku
Źródło: https://adrelien.com/diy-meshtastic-how-to-build-your-own-meshtastic-device-with-faketec-pcb-nrf52840/ (sprawdzono 2026-10)

### Jaki akumulator i ładowanie w FakeTec?
Autor zaleca akumulator nie większy niż 450 mAh (to ograniczenie nieoficjalne, część użytkowników zgłasza problemy z większymi). Dla większego zewrzyj pady BOOST z tyłu płytki obok RST i VCC. Prąd ładowania: 100 mA bez zworki, 300 mA ze zworką. Częste błędy: pominięcie anteny, zły region, pomijanie lub zła kolejność aktualizacji bootloadera, brak okularów ochronnych (obcinane nóżki odlatują), pośpiech przy lutowaniu modułu LoRa. Opis chipa w poradniku jest niespójny (SX1262 kontra SX1276/SX1278): sprawdź kartę katalogową modułu.
Słowa: akumulator, 450 mAh, BOOST, ładowanie, prąd ładowania, zworka, bateria, HT-RA62 chip
Źródło: https://adrelien.com/diy-meshtastic-how-to-build-your-own-meshtastic-device-with-faketec-pcb-nrf52840/ (sprawdzono 2026-10)
