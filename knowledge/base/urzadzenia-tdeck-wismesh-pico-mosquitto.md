# T-Deck, WisMesh, Raspberry Pi Pico, integracje i własny Mosquitto

### LILYGO T-Deck, T-Deck Plus i Pro: dane i skróty klawiszowe
T-Deck i T-Deck Plus: ESP32-S3FN16R8 (osobny ESP32-C3 obsługuje tylko klawiaturę), SX1262 (915, 868 lub 433 MHz), złącze U.FL, ekran 2,8" IPS 320×240 z dotykiem, podświetlana klawiatura, trackball, głośnik i mikrofon; Plus ma GPS i akumulator 2000 mAh. T-Deck Pro: ekran e-ink 3,1" z dotykiem, antena wbudowana na płytce, akumulator 1400 mAh, warianty 4G i Voice. Skróty w interfejsie Base UI: Alt+B podświetlenie klawiatury, Alt+C modyfikator, potem M (powiadomienia), Q (wyjście), T (zakładka: raz odbiorca DM, dwa razy kanał), G (GPS); w interfejsie MUI skróty Alt+C nie działają. Przed wgraniem koniecznie podłącz antenę.
Słowa: T-Deck, T-Deck Plus, T-Deck Pro, klawiatura, trackball, Alt+C, skróty, Base UI, MUI, ekran e-ink, urządzenie samodzielne
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/tdeck/ (sprawdzono 2026-10)

### T-Deck: jak wejść w tryb wgrywania firmware?
Najprościej przyciskiem "1200bps reset" we flasherze WWW, wybierając "T Deck". Ręcznie: wyłącz zasilanie przełącznikiem, naciśnij i przytrzymaj trackball, włącz zasilanie, puść trackball po 2–3 sekundach. Jeśli ekran jest czarny i podświetlenie wyłączone, urządzenie jest w trybie wgrywania; jeśli podświetlenie świeci, powtórz. Pliki: firmware-t-deck oraz firmware-t-deck-pro.
Słowa: T-Deck, tryb wgrywania, download mode, trackball, 1200bps, flasher, firmware-t-deck
Źródło: https://meshtastic.org/docs/hardware/devices/lilygo/tdeck/ (sprawdzono 2026-10)

### RAK WisMesh: przegląd rodziny
Dokumentacja podaje tylko krótkie opisy. Pocket V2: kompaktowy ręczny węzeł z ekranem i GPS. Pocket Mini: ultrakompaktowy. TAP: ekran dotykowy, odporny na pogodę. Board ONE: kompaktowy i rozszerzalny, jako obudowany ręczny węzeł albo płytka do własnej budowy. 1W Booster: moc nadawania 1 W (wzmacniacz SKY66122), lepsza czułość odbioru. Repeater: stały, zasilany słonecznie, wodoodporny, z anteną o dużym zysku; Repeater Mini: przenośny, solarny, wodoodporny. Pojemności baterii, pasm i anten strona nie podaje.
Słowa: WisMesh, Pocket V2, Pocket Mini, TAP, Board ONE, 1W Booster, SKY66122, Repeater, solar, wodoodporny
Źródło: https://meshtastic.org/docs/hardware/devices/rak-wireless/wismesh/ (sprawdzono 2026-10)

### Raspberry Pi Pico w Meshtastic
Płytki Pico (RP2040) i Pico 2 (RP2350) wymagają zewnętrznego modułu LoRa (SX1276 lub SX1262); GPS dokłada się przez UART. Pico i Pico 2 nie mają Wi-Fi ani Bluetooth. Pico W i Pico 2 W mają Wi-Fi 2,4 GHz, ale w Meshtastic bez serwera WWW i API HTTP; w Pico 2 W sprzęt Bluetooth jest, lecz Meshtastic go nie obsługuje. Linuksowe komputery Pi z nakładką LoRa to osobna sprawa (meshtasticd), nieopisana na tej stronie.
Słowa: Raspberry Pi Pico, Pico W, Pico 2, RP2040, RP2350, SX1276, SX1262, moduł LoRa zewnętrzny, bez Bluetooth
Źródło: https://meshtastic.org/docs/hardware/devices/raspberrypi/ (sprawdzono 2026-10)

### Jakie integracje oferuje Meshtastic?
Oficjalna strona wymienia: MQTT (łączy sieci mesh przez internet oraz narzędzia takie jak Home Assistant, Node-RED i Adafruit IO), wtyczkę ATAK (przekazuje ruch Cursor on Target przez sieć mesh) oraz CalTopo/SARTopo (śledzenie węzłów na mapach). Boty i bramki APRS nie są tam opisane. Wsparcie do poszczególnych produktów zapewniają ich autorzy.
Słowa: integracje, MQTT, Node-RED, Adafruit IO, ATAK, TAK, CalTopo, SARTopo, Home Assistant, mapy
Źródło: https://meshtastic.org/docs/software/integrations/ (sprawdzono 2026-10)

### Własny broker Mosquitto: szybki start
macOS: brew install mosquitto, brew services restart mosquitto. Linux: sudo systemctl start mosquitto. Test: w jednym oknie mosquitto_sub -t test/hello, w drugim mosquitto_pub -h localhost -q 0 -t test/hello -m 'yo!'. Żeby inne urządzenia mogły się łączyć, w mosquitto.conf (Linux: /etc/mosquitto/mosquitto.conf) dodaj listener 1883 0.0.0.0 i allow_anonymous true, potem zrestartuj usługę; jeśli działa zapora systemu, zezwól na Mosquitto. Uwaga: ta konfiguracja pozwala każdemu, kto dotrze do portu 1883, podłączyć się bez hasła, więc nie wystawiaj jej do internetu; do użytku produkcyjnego ustaw użytkownika i hasło oraz TLS (strona tego nie opisuje, zobacz dokumentację Mosquitto).
Słowa: Mosquitto, broker, mosquitto.conf, listener 1883, allow_anonymous, mosquitto_sub, mosquitto_pub, brew, własny serwer MQTT, hasło
Źródło: https://meshtastic.org/docs/software/integrations/mqtt/mosquitto/ (sprawdzono 2026-10; ostrzeżenie o braku hasła to wniosek ogólny)
