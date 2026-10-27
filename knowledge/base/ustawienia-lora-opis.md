# Ustawienia LoRa w Meshtastic: pole po polu

### Co robi ustawienie Region i dlaczego bez niego węzeł milczy?
Domyślnie region to UNSET i węzeł nie nadaje, dopóki go nie ustawisz. Region wyznacza pasmo, limity mocy i cykl pracy (duty cycle): dla EU_433 i EU_868 wynosi 10 %, dla EU_866 2,5 % (10 % dla ról ROUTER i ROUTER_LATE). Regiony amatorskie ITU wymagają licencji, szyfrowanie jest w nich niedozwolone i trzeba nadawać znak wywoławczy. Podany w oprogramowaniu limit mocy to sufit firmware, a nie krajowy przepis.
Słowa: region, UNSET, nie nadaje, duty cycle, EU_868, EU_433, EU_866, licencja amatorska, limit mocy, przepisy
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Preset, bandwidth, spread factor i coding rate: kiedy które działa?
Domyślnie włączone jest "użyj presetu" (use_preset = true) i preset LONG_FAST. Węzły słyszą się tylko na tym samym presecie. Gdy wyłączysz preset, używane są ręcznie ustawione: szerokość pasma (domyślnie 250), spreading factor (domyślnie 11, zakres 5–12; starsze układy SX127x tylko 7–12) i coding rate (5 oznacza 4/5, zakres 4–8). W polu szerokości część wartości to kody: 62 oznacza 62,5 kHz, 200 oznacza 203,125 kHz. Wartości poniżej 62,5 kHz mogą wymagać TCXO. Uwaga: węzeł w USA, któremu region ustawiono z jego ekranu, startuje na LONG_TURBO i nie słyszy LONG_FAST. Preset niedozwolony w regionie jest zamieniany na domyślny regionu albo zmiana jest odrzucana, gdy przyszła zdalnie.
Słowa: use_preset, LONG_FAST, LONG_TURBO, bandwidth, spreading factor, coding rate, kod 62, 203,125, TCXO, SX127x, ręczne ustawienia, modem
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Hop limit, moc nadawania i włączenie nadawania
Max Hops (hop_limit) ma zakres 1–7, domyślnie 3, i dokumentacja uważa 3 za dobre dla większości. Moc nadawania (tx_power) 0 oznacza maksymalną legalną moc ciągłą dla regionu; zakres 0–30 dBm; zwykle zostaw 0. Opcja tx_enabled (domyślnie włączona) pozwala wyłączyć nadawanie na czas np. wymiany anteny lub testów.
Słowa: hop_limit, tx_power, moc 0, 30 dBm, tx_enabled, wyłączenie nadawania, wymiana anteny, test
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Slot częstotliwości, przesunięcie i nadpisanie częstotliwości
Frequency Slot (channel_num) równe 0 oznacza, że slot wynika z haszu nazwy kanału głównego; inna wartość to numer od 1 do maksimum regionu. Przesunięcie częstotliwości (frequency_offset, 0–1 000 000) służy do poprawy błędu kwarcu i jest zaawansowane. Override Frequency (override_frequency) omija wyliczenie z kanału (przesunięcie nadal działa) i jest przeznaczone dla zaawansowanych oraz licencjonowanych amatorów; trzeba przestrzegać lokalnych przepisów.
Słowa: channel_num, slot, hash nazwy kanału, frequency_offset, override_frequency, własna częstotliwość, kwarc, korekta
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Ignore MQTT, OK to MQTT, SX126x boost, wyłączenie limitu cyklu pracy
Ignore MQTT (domyślnie wyłączone) odrzuca wiadomości, które do urządzenia dotarły przez MQTT; wymaga firmware 2.2.19 lub nowszego na obu urządzeniach. OK to MQTT (domyślnie wyłączone) to prośba, by pakiety mogły trafiać do brokerów MQTT; to uprzejma deklaracja, nie zabezpieczenie kryptograficzne, i dotyczy tylko kanałów z domyślnym lub eventowym kluczem. SX126x RX Boosted Gain (domyślnie wyłączone) daje lepszą czułość odbioru kosztem odrobiny prądu. Override Duty Cycle Limit pomija godzinny limit cyklu pracy w UE, co może łamać przepisy; nie włączaj bez pewności.
Słowa: ignore_mqtt, ok_to_mqtt, config_ok_to_mqtt, sx126x_rx_boosted_gain, boosted gain, override_duty_cycle, limit cyklu pracy, czułość odbioru
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)
