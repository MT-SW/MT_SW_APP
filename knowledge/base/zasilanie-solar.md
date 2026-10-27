# Zasilanie, baterie i solary

### Jak zmierzyć pobór prądu węzła?
Jeden odczyt prądu nie wystarcza: węzeł większość czasu nasłuchuje, a więcej prądu bierze przy nadawaniu, więc mierz energię w czasie. Moc (W) = napięcie × prąd (np. 5 V × 50 mA = 250 mW). Energia (Wh) = moc × czas (250 mW przez 24 h = 6 Wh). Użyj miernika lub zasilacza sumującego Wh/Ah (zwykły multimetr pokazuje chwilową wartość i bywa, że zaokrągla małe prądy do zera). Test trwaj co najmniej godzinę, najlepiej 2–6 godzin przy ruchu podobnym do docelowego.
Słowa: pobór prądu, zużycie energii, pomiar, miernik, Wh, mAh, USB tester, multimetr, ile bierze
Źródło: https://meshtastic.org/docs/hardware/solar-powered/measure-power-consumption/ (sprawdzono 2026-10)

### Jak ustawić test poboru dla routera solarnego?
Użyj tej samej roli, regionu, presetu i ustawień energii co w terenie. Ustawienie z trzech węzłów: nadawca, testowany router i odbiornik, który słyszy router, ale nie słyszy nadawcy bezpośrednio, więc dostaje pakiety tylko po retransmisji. Nie testuj w zaludnionej sieci (sąsiednie węzły powtórzą pakiety testowe): użyj slotu częstotliwości, którego nikt tam nie używa. Hop limit 1 sprawia, że router przekaże każdy pakiet raz; hop limit 0 mierzy tylko odbiór. W dokumentacji jest skrypt Pythona wysyłający wiadomość co 120 s.
Słowa: test routera, router solarny, hop limit 1, trzy węzły, skrypt, obciążenie retransmisją
Źródło: https://meshtastic.org/docs/hardware/solar-powered/measure-power-consumption/ (sprawdzono 2026-10)

### Jak policzyć średnią moc i dobrać panel oraz akumulator?
Średnia moc = Wh ÷ godziny testu (np. 0,72 Wh przez 3 h = 240 mW). Z amperogodzin: Ah × napięcie testu = Wh, potem dziel przez godziny (5,1 V × 142 mAh = 724,2 mWh; ÷ 3 h ≈ 241,4 mW). Dobowa energia = średnia moc × 24 h (241,4 mW daje ok. 5,8 Wh na dobę). Na tej podstawie dobierz panel i akumulator z zapasem na pochmurną pogodę i straty układu.
Słowa: panel słoneczny, akumulator, pojemność, dobór, dobowa energia, zapas, pochmurno, bateria, solar, Wh na dobę
Źródło: https://meshtastic.org/docs/hardware/solar-powered/measure-power-consumption/ (sprawdzono 2026-10)

### Ile prądu bierze węzeł ESP32 i jaki jest realny wynik pomiaru (Heltec V3)?
Wg autora kaspars.net i konsensusu społeczności: węzeł ESP32 bez Bluetooth, ekranu i GPS ok. 0,1 W średnio. Jego pomiar przy 5 % wykorzystania (jedna wiadomość telemetrii na minutę): średnio 0,046 W (12 mA przy 3,7 V) przez 6 dni na jednym ogniwie 6,6 Wh. RAK4631 (nRF52840) ma brać wyraźnie mniej prądu niż ESP32, więc jest lepszy na repeater. Regulator Heltec V3 (CE6260) ma spadek napięcia 0,12 V przy 100 mA i 0,25 V przy 200 mA (nadawanie), więc przestaje regulować przy ok. 3,42–3,55 V i traci część pojemności.
Słowa: pobór prądu, 0,1 W, 12 mA, Heltec V3, RAK4631, dropout, regulator, ile bierze węzeł, repeater solarny
Źródło: https://kaspars.net/blog/solar-lora-meshtastic/md (sprawdzono 2026-10; pomiar jednego autora)

### Czego uczy budowa solara z panelem 4 W i ogniwami 18650?
Większość ogniw 18650 nie ma zabezpieczenia podnapięciowego; zabezpieczenie panelu (XB5352A) chroni tylko wejście solarne, więc taki panel jest bezużyteczny bez dodatkowego zabezpieczenia akumulatora przed głębokim rozładowaniem. Panel trzeba przewymiarować, bo zimą wydajność jest dużo niższa (w zimie ładowanie nie przekraczało 1 W). Reguła kciuka: 10 Wp panelu na 10 Wh akumulatora, ok. 3 godziny pełnego słońca na pełne naładowanie. Kontrolery: CN3791 (prawdziwy MPPT, wejście do 28 V, 4 A), CN3163 (adaptacyjny, min. 4,4 V), TP4056/CL4056 (zwykła ładowarka bez funkcji solarnych). Podłączenie INA219 powoduje, że firmware używa jego napięcia w telemetrii urządzenia (nieudokumentowane).
Słowa: 18650, zabezpieczenie podnapięciowe, głębokie rozładowanie, CN3791, MPPT, TP4056, zima, 10 Wp, INA219, panel 4 W
Źródło: https://kaspars.net/blog/solar-lora-meshtastic/md (sprawdzono 2026-10)

### Który akumulator na zimę i jak nie niszczyć go mrozem (LFP)?
Źródło zaleca LiFePO4 (LFP): użytecznie rozładowuje się do ok. −20 °C przy zmniejszonej pojemności. Nigdy nie ładuj LFP poniżej 0 °C: ładowanie na mrozie powoduje osadzanie litu i trwale obniża pojemność. Ochrona: kontroler z czujnikiem temperatury akumulatora, wstrzymujący ładowanie poniżej 0 °C, albo izolowana obudowa z grzałką 12 V z termostatem ok. 1–5 W. Preferuj MPPT zamiast PWM, sprawdź w karcie RAK maksymalne napięcie i prąd wejścia solarnego.
Słowa: LiFePO4, LFP, mróz, ładowanie poniżej zera, 0 stopni, litowanie, grzałka, akumulator zimą, temperatura, kontroler MPPT
Źródło: https://d-central.tech/solar-meshtastic-node-build/ (sprawdzono 2026-10)

### Jak dobrać panel i akumulator do routera (przykłady obciążenia)?
Szacunki społeczności przy 3,7 V: RAK4631 mały ruch 5–20 mA (0,44–1,78 Wh/dobę), umiarkowany 15–40 mA (1,33–3,55 Wh/dobę); Heltec HT-CT62 20–60 mA (1,78–5,33 Wh/dobę); Heltec WiFi LoRa 32 V3 60–150 mA (5,33–13,3 Wh/dobę). Wzór: mA ÷ 1000 × 24 h × 3,7 V. Przykład RAK4631 20 mA: 1,78 Wh/dobę, 7 dni autonomii 12,5 Wh, z zapasem 80 % DoD ok. 16 Wh (4300 mAh), panel graniczny 1,5 W, zalecany 5–10 W (3× zapasu). Zakresy: RAK4631 mały ruch panel 5–10 W i LFP 3000–6000 mAh; Heltec V3 panel 40–60 W i LFP 20 000+ mAh. Wartości to szacunki: zmierz własną konstrukcję.
Słowa: dobór panelu, dobór akumulatora, autonomia, DoD, Wh na dobę, RAK4631, HT-CT62, panel 10 W, ile mAh
Źródło: https://d-central.tech/solar-meshtastic-node-build/ (sprawdzono 2026-10; dane dla Kanady, zimowe godziny słońca 0,5–3 h/dobę zależnie od miasta)

### Jak zamontować panel i obudowę?
Pochylenie panelu: przez cały rok blisko szerokości geograficznej, zimowe: szerokość + 10–15° (lepiej zrzuca śnieg). Cień 1–2 godziny dziennie potrafi obniżyć produkcję o 15–30 %. Obudowa minimum IP67 (pyłoszczelna, wytrzymuje 30 minut zanurzenia 1 m), ABS odporny na UV lub poliwęglan (lepszy przy zamarzaniu i rozmarzaniu), dławiki skierowane w dół, saszetki silikażelu wymieniane co rok, złącza koncentryczne uszczelnione taśmą samowulkanizującą odporną na UV, złącza MC4 do paneli i oczka lub Anderson do akumulatora. Weryfikuj przez PVGIS dla swoich współrzędnych.
Słowa: kąt panelu, tilt, IP67, obudowa, dławik, silikażel, MC4, cień, śnieg, PVGIS, montaż solara
Źródło: https://d-central.tech/solar-meshtastic-node-build/ (sprawdzono 2026-10)
