# Parametry LoRa, jakość sygnału i połączenie z radiem

### Co to jest współczynnik rozpraszania (spreading factor, SF) w LoRa?
Spreading factor (SF) to liczba od 7 do 12. Im wyższy SF, tym większy zasięg i czułość odbiornika, ale tym wolniejszy transfer i dłuższy pakiet w eterze; każdy kolejny SF mniej więcej podwaja czas nadawania. Preset wybiera SF razem z szerokością pasma, dlatego zwykle nie ustawia się go ręcznie. Nasz preset Narrow Fast używa SF7.
Słowa: spreading factor, współczynnik rozpraszania, SF, SF7, SF12, czułość, czas nadawania, preset
Źródło: Baza wiedzy: parametry LoRa (sprawdzono 2026-10)

### Co to jest szerokość pasma (bandwidth) w LoRa?
Szerokość pasma (bandwidth, BW) to szerokość kanału radiowego w kHz, np. 62,5, 125, 250 albo 500. Szersze pasmo daje szybszy transfer, ale mniejszą czułość; węższe daje większy zasięg i lepszą odporność na zakłócenia, ale wolniejszy transfer i dłuższy pakiet. Nasz preset Narrow Fast używa wąskiego pasma 62,5 kHz, które mieści się w polskim paśmie 868 MHz. Szerokość pasma jest częścią presetu.
Słowa: szerokość pasma, bandwidth, BW, kHz, 62,5, preset, wąskie pasmo
Źródło: Baza wiedzy: parametry LoRa (sprawdzono 2026-10)

### Co to jest coding rate (korekcja błędów) w LoRa?
Coding rate (CR) to korekcja błędów zapisywana jako 4/5, 4/6, 4/7 albo 4/8. Wyższa korekcja ratuje pakiety zakłócone, ale wydłuża pakiet, więc zajmuje eter dłużej i mniej urządzeń mieści się w sieci. Zasięg zmienia się przy tym tylko trochę. Wszystkie radia w sieci muszą mieć ten sam coding rate, bo inaczej się nie słyszą.
Słowa: coding rate, CR, korekcja błędów, 4/5, 4/8, zakłócenia, czas nadawania
Źródło: Baza wiedzy: parametry LoRa (sprawdzono 2026-10)

### Co to jest SNR i jakie wartości są dobre?
SNR (stosunek sygnału do szumu, w dB) mówi, o ile sygnał jest silniejszy od szumu. LoRa potrafi odebrać sygnał słabszy od szumu, więc ujemny SNR jest normalny. Dolna granica zależy od SF: ok. -7,5 dB dla SF7, ok. -20 dB dla SF12. Dobry odbiór to SNR wyraźnie powyżej tej granicy, np. kilka dB nad nią; blisko granicy pakiety giną.
Słowa: SNR, stosunek sygnału do szumu, szum, ujemny SNR, decybele, jakość sygnału, dlaczego SNR jest ujemny
Źródło: Baza wiedzy: parametry LoRa (sprawdzono 2026-10)

### Co to jest RSSI i jakie wartości są dobre?
RSSI to moc odbieranego sygnału w dBm. Im bliżej zera, tym silniejszy sygnał: około -90 dBm to dobry sygnał, około -110 dBm słaby, a poniżej -120 dBm jest już na granicy odbioru. Samo RSSI nie mówi o jakości łącza, bo sygnał może być mocny, ale zagłuszony; dlatego patrzy się też na SNR.
Słowa: RSSI, siła sygnału, moc odbieranego sygnału, dBm, jakość sygnału
Źródło: Baza wiedzy: parametry LoRa (sprawdzono 2026-10)

### Dlaczego SNR może być ujemny?
Ujemny SNR oznacza, że sygnał jest słabszy od szumu tła. W LoRa to normalne: dzięki rozpraszaniu widma radio odbiera sygnał nawet około 7,5 dB (SF7) do 20 dB (SF12) poniżej szumu. Problem zaczyna się dopiero wtedy, gdy SNR zbliża się do dolnej granicy dla danego presetu.
Słowa: ujemny SNR, dlaczego SNR ujemny, szum, poniżej szumu, LoRa
Źródło: Baza wiedzy: parametry LoRa (sprawdzono 2026-10)

### Co to jest limit wypełnienia (duty cycle)?
Limit wypełnienia (duty cycle) to największy odsetek czasu, w którym wolno nadawać w danym paśmie, np. 10 % godziny na części pasma 868 MHz (w innych podpasmach 1 %). Chroni to wspólny eter przed zapchaniem. Długi preset i duże pakiety zużywają ten limit szybciej, dlatego kalkulator czasu nadawania pokazuje, ile pakietów mieści się w limicie.
Słowa: duty cycle, limit wypełnienia, limit czasu nadawania, 10 %, 1 %, eter, airtime
Źródło: Baza wiedzy: parametry LoRa (sprawdzono 2026-10)

### Co to jest region LoRa i po co go ustawiać?
Region mówi radiu, w jakim paśmie i z jakimi limitami mocy i czasu nadawania wolno mu pracować. Bez ustawionego regionu węzeł nie nadaje. W naszej sieci świętokrzyskiej używamy regionu EU_N_868 z presetem Narrow Fast. Wszystkie radia w sieci muszą mieć ten sam region i preset.
Słowa: region, region LoRa, EU_868, EU_N_868, pasmo, limity mocy, po co region
Źródło: Baza wiedzy: parametry LoRa (sprawdzono 2026-10)

### Jak podłączyć się do urządzenia przez Bluetooth?
Włącz Bluetooth w telefonie, otwórz ekran Połączenia w aplikacji i dotknij Bluetooth, wybierz swoje radio z listy znalezionych urządzeń i wpisz kod PIN, gdy aplikacja o niego poprosi (przy węzłach z ekranem PIN jest na ekranie węzła; domyślny kod na starszych węzłach to 123456). Radio musi być włączone i nie może być połączone z innym telefonem. Gdy radia nie widać, wyłącz i włącz Bluetooth w telefonie albo zrestartuj radio.
Słowa: bluetooth, połączyć, podłączyć, parowanie, PIN, 123456, Połączenia, BLE, urządzenie, radio, telefon
Źródło: Baza wiedzy: połączenie z radiem (sprawdzono 2026-10)
