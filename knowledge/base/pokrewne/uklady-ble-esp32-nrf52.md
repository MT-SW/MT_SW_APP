# Układy i Bluetooth LE: ESP32, nRF52

### Bluetooth Low Energy: zasięg, pobór prądu, parowanie
BLE ma nominalny zasięg do ok. 100 m (praktycznie mniej), moc nadawania do 10 mW (100 mW w Bluetooth 5) i bardzo mały pobór prądu: beacon może pracować 1–2 lata na baterii guzikowej, a ciągłe skanowanie opróżnia ją w kilka godzin. Używa pasma 2,4 GHz, 40 kanałów po 2 MHz, szyfrowanie AES-128 w trybie CCM. Urządzenia odkrywa się przez pakiety rozgłoszeniowe na trzech kanałach. Dane w modelu GATT: serwer (np. czujnik) udostępnia usługi i charakterystyki, a klient (telefon) je odczytuje lub zapisuje; powiadomienia i potwierdzane "indications". Bluetooth 5 dodał szybki tryb 2M, tryb Coded PHY zwiększający zasięg przy niższej prędkości i większe pakiety. W Androidzie 6 i nowszym połączenie BLE wymaga uprawnienia do lokalizacji.
Słowa: Bluetooth, BLE, GATT, zasięg Bluetooth, parowanie, 2,4 GHz, Bluetooth 5, Coded PHY, uprawnienie lokalizacji, Android
Źródło: https://en.wikipedia.org/wiki/Bluetooth_Low_Energy (sprawdzono 2026-10)

### Układy ESP32: warianty i różnice
ESP32: dwurdzeniowy Xtensa LX6, 240 MHz, 520 KiB SRAM, 34 piny GPIO, Wi-Fi b/g/n, Bluetooth 4.2, bez USB. ESP32-S2: jeden rdzeń, USB OTG, bez Bluetooth. ESP32-S3: dwa rdzenie, Bluetooth 5 LE, USB OTG, do 8 MiB PSRAM, 45 GPIO. ESP32-C3: RISC-V 160 MHz, Bluetooth 5, 22 GPIO. ESP32-C6: RISC-V, Wi-Fi 6, Bluetooth 5.3, bez PSRAM. Zasilanie 3,3 V. Frameworki: ESP-IDF (oficjalny), Arduino-ESP32, MicroPython, CircuitPython. Źródło nie podaje poboru prądu, trybów uśpienia ani pinów tylko-wejściowych (te piny GPIO 34–39 są opisane w modułach Meshtastic).
Słowa: ESP32, ESP32-S3, ESP32-C3, ESP32-C6, PSRAM, Wi-Fi 6, USB OTG, RISC-V, Xtensa, ESP-IDF, Arduino, MicroPython
Źródło: https://en.wikipedia.org/wiki/ESP32 (sprawdzono 2026-10)

### nRF52840 i seria nRF52 firmy Nordic
Seria nRF52 (od 2015) ma rdzeń ARM Cortex-M4 i Bluetooth LE; nRF52840 i nRF52833 to bardzo popularne układy (myszki, klawiatury, urządzenia audio); Nordic deklaruje 40 % światowego rynku BLE. Układy są projektowane do niskiego poboru energii, więc nadają się do zasilania z małej baterii lub energii z otoczenia. Źródło nie podaje wartości prądu ani opisu bootloaderów; w Meshtastic płytki nRF52 mają bootloader UF2 i pobierają mniej prądu niż ESP32 (wiedza z naszych wpisów o nRF52).
Słowa: nRF52, nRF52840, Nordic, Cortex-M4, BLE, mały pobór, bootloader UF2, RAK4631, T-Echo, solar
Źródło: https://en.wikipedia.org/wiki/Nordic_Semiconductor (sprawdzono 2026-10; część o bootloaderze z wpisów bazy)
