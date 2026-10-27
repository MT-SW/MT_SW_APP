# Budowa własnych węzłów: moduły i zestawy dla majsterkowiczów

### Jakie gotowe moduły nadają się do samodzielnej budowy węzła?
Dokumentacja Meshtastic wymienia kilka rodzin: modułowy system RAK WisBlock (rdzeń, płyta bazowa, czujniki, GPS, wyświetlacze), gotowe moduły RAK WisMesh, rdzenie Heltec RadioCore (SX1262 plus ESP32-S3, ESP32-C6 albo nRF52840, do własnych płytek), przylutowywany moduł Heltec HT-CT62 (ESP32-C3 z SX1262, bez Wi-Fi i Bluetooth w Meshtastic) oraz zestawy Seeed Wio (XIAO z modułem Wio-SX1262), przeznaczone do prototypów. Dokumentacja zaleca wybierać układy radiowe SX126x lub LR11xx, a nie SX127x. Płytki nRF52 pobierają mniej prądu (dobre do solara i na rękę), płytki ESP32 więcej, ale są zwykle tańsze i mają Wi-Fi.
Słowa: DIY, zrób to sam, moduły, WisBlock, RadioCore, HT-CT62, Wio, zestaw, własny węzeł, prototyp, lutowanie, płytka
Źródło: https://meshtastic.org/docs/hardware/devices/ (sprawdzono 2026-10)

### RAK WisBlock: jakie rdzenie i czym się różnią?
RAK4631 to nRF52840 z SX1262 i Bluetooth 5.0, bardzo mały pobór prądu, bez Wi-Fi; wersje częstotliwości 433, 470, 799, 865, 868, 915, 920 i 923 MHz; antena na złączu U.FL. RAK11310 to RP2040 z SX1262, bez Bluetooth i bez Wi-Fi. RAK3312 to ESP32-S3 z SX1262, Wi-Fi i Bluetooth 5.0. Płyta bazowa nowej generacji to RAK19007 (starsza RAK5005-O jest wycofana). Do płyty dokłada się moduły: GPS, czujniki, buzzer, diodę, zegar RTC, ekran OLED lub e-ink.
Słowa: RAK4631, RAK11310, RAK3312, RAK19007, RAK5005, WisBlock, rdzeń, płyta bazowa, nRF52840, RP2040, ESP32-S3
Źródło: https://meshtastic.org/docs/hardware/devices/rak-wireless/wisblock/core-module/ i https://meshtastic.org/docs/hardware/devices/ (sprawdzono 2026-10)

### Jak wgrać firmware na RAK4631 i RAK11310? Co to RAK4631-R?
Pliki firmware mają postać firmware-rak4631-X.X.X.xxxxxxx.uf2 oraz firmware-rak11310-X.X.X.xxxxxxx.uf2. RAK4631 potrzebuje bootloadera Arduino. Wersja RAK4631-R (bootloader RUI3) musi być najpierw przestawiona na bootloader Arduino według osobnego poradnika w dokumentacji. Krok po kroku strona rdzenia nie opisuje wgrywania; ogólne zasady wgrywania plików .uf2 są w naszym wpisie o bootloaderze nRF52. Numer GPIO w ustawieniach podaje się w numeracji Arduino (np. polecenie z external_notification.output 10 steruje wyjściem IO6); IO1 i IO2 są zajęte na zasilanie peryferiów, a IO5 to przycisk użytkownika.
Słowa: RAK4631-R, RUI3, bootloader Arduino, uf2, GPIO, IO6, przycisk, wgrywanie RAK, konwersja bootloadera
Źródło: https://meshtastic.org/docs/hardware/devices/rak-wireless/wisblock/core-module/ (sprawdzono 2026-10)

### Seeed XIAO z modułem Wio-SX1262: co to za zestawy?
Dwa zestawy o wymiarach około 21 × 18 mm, z SX1262 (862–930 MHz), złączem USB-C i U.FL na antenę, opcjonalnie z modułem GNSS L76K. Wersja nRF52840 ma Bluetooth i wbudowany układ zarządzania zasilaniem. Wersja ESP32-S3 ma Wi-Fi 2,4 GHz, Bluetooth 5 LE, 8 MB flash i 8 MB PSRAM, w zestawie antena 2 dBi i firmware Meshtastic fabrycznie wgrany (jest też wersja z drukowaną obudową). Plik firmware dla S3 nazywa się firmware-seeed-xiao-s3-X.X.X.xxxxxxx.bin. Uwaga: moduł Wio-SX1262 z jednego zestawu ma inne przypisanie pinów niż z drugiego, więc nie zamienia się ich bez firmware z właściwymi definicjami pinów.
Słowa: XIAO, Wio-SX1262, Seeed, nRF52840, ESP32-S3, L76K, piny, zestaw, mały węzeł, 21x18
Źródło: https://meshtastic.org/docs/hardware/devices/seeed-studio/wio-series/wio-sx1262s/ (sprawdzono 2026-10)

### Czym jest meshtasticd i do czego służy?
To program (natywny plik wykonywalny), który uruchamia Meshtastic na komputerze z Linuksem lub macOS i radiem podłączonym przez SPI albo USB. Działa na tym samym rdzeniu co firmware, pozwala wysyłać i odbierać wiadomości, dzielić się pozycją i rozmawiać z innymi urządzeniami przez LoRa lub UDP. Konfiguruje się go plikami YAML, są gotowe opisy pinów popularnych płytek, a pakiety istnieją dla większości dystrybucji. Typowe zastosowania: stała stacja bazowa, brama sieciowa, most MQTT, integracja z automatyką domową, zdalny węzeł pomiarowy. Szczegółów instalacji, listy obsługiwanych modułów i podłączenia do Raspberry Pi ta strona nie podaje; są w podstronach Installation, Usage, Hardware i OpenWrt dokumentacji meshtasticd.
Słowa: meshtasticd, Raspberry Pi, Linux, SPI, USB radio, stacja bazowa, brama, yaml, config.d, natywny, OpenWrt
Źródło: https://meshtastic.org/docs/meshtasticd/ i https://meshtastic.org/docs/hardware/devices/linux-native-hardware/ (sprawdzono 2026-10; brak szczegółów instalacji w tych źródłach)
