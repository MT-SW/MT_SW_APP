# Heltec: przegląd płytek w Meshtastic

### Jakie płytki Heltec obsługuje Meshtastic i czym się różnią?
Wszystkie opisane na oficjalnej stronie używają radia SX1262. LoRa 32 V4: ESP32-S3R2, pasma 863–870 i 902–928 MHz, ekran OLED 0,96", opcja dużej mocy 28±1 dBm, złącze U.FL, 2-pinowe złącze solarne, 8-pinowe złącze GNSS. V4-R8: ESP32-S3R8 (16 MB flash, 8 MB PSRAM), ekran OLED lub TFT, inne rozmieszczenie wyprowadzeń, więc potrzebuje własnego firmware (nie wgrywaj wersji dla zwykłego V4). LoRa 32 V3/V3.1: ESP32-S3FN8, bez GPS, V3.1 używa tego samego firmware co V3. Wireless Stick Lite V3: bez ekranu i bez GPS, antena LoRa obowiązkowa. Pojemności baterii strona nie podaje.
Słowa: Heltec, LoRa 32 V4, V4-R8, V3, V3.1, Wireless Stick Lite, SX1262, ESP32-S3, OLED, 28 dBm, płytka
Źródło: https://meshtastic.org/docs/hardware/devices/heltec/ (sprawdzono 2026-10)

### Heltec Wireless Tracker i Wireless Paper: różnice
Wireless Tracker v1.0 i v1.1: ESP32-S3FN8, ekran LCD 0,96", złącze anteny GNSS (v1.1 ma poprawiony schemat, I2C na GPIO45/46, dla większości sztuk zastępuje v1.0). Tracker V2: kolorowy TFT 160×80, dwupasmowy GNSS UC6580, moc 28±1 dBm i zarządzanie baterią oraz solarem. Wireless Paper v1.0 i v1.1: ekran e-ink 2,13" czarno-biały, bez GPS, pasma także 433 MHz; firmware nazywa się "Wireless Paper V1.0" dla starszej wersji i "Wireless Paper" dla v1.1. Tracker wchodzi w tryb wgrywania, gdy przy włączaniu przytrzymasz przycisk USER, albo przyciskiem "1200bps reset" we flasherze.
Słowa: Wireless Tracker, Wireless Paper, e-ink, UC6580, GNSS, USER, tryb wgrywania, v1.1, V2, Heltec
Źródło: https://meshtastic.org/docs/hardware/devices/heltec/ (sprawdzono 2026-10)

### Płytka Heltec nie ładuje akumulatora przez kabel USB-C
Strona producenta w dokumentacji Meshtastic ostrzega, że kilka płytek może mieć kłopot z ładowaniem podłączonego akumulatora przez kabel USB-C na USB-C; zaleca kabel USB-A na USB-C. Jeśli ładowanie nie rusza, spróbuj więc takiego kabla i zasilacza z klasycznym wyjściem USB-A. Przyczyn na poziomie układu strona nie podaje.
Słowa: Heltec ładowanie, USB-C, kabel, akumulator nie ładuje, USB-A, zasilacz
Źródło: https://meshtastic.org/docs/hardware/devices/heltec/ (sprawdzono 2026-10)
