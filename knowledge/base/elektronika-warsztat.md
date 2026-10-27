# Warsztat elektronika: lutowanie, multimetr, zasilanie, BMS, SDR

### Jak dobrze lutować?
Cyna Sn63Pb37 topi się w jednym punkcie 183 °C (60/40 w zakresie 183–190 °C); bezołowiowe stopy SAC topią się ok. 217–220 °C i trudniej z nimi o dobry lut. Topnik usuwa tlenki i poprawia zwilżanie; do elektroniki kalafoniowy lub "no-clean", nigdy kwasowy (hydrauliczny), bo przewodzi i koroduje. Technika: oczyść, daj topnik, grzej łączone elementy (nie cynę), podaj cynę do złącza, zabierz drut, potem grot; nie ruszaj, aż zastygnie. Grot trzymaj czysty i pocynowany; lutownica z regulacją temperatury jest najlepsza (25 W bywa za słabe na duże złącza, 100 W może przegrzać płytkę). Dobry lut jest gładki, błyszczący, wklęsły; matowy i ziarnisty oznacza ruch przy stygnięciu. Opary są szkodliwe także przy cynie bezołowiowej, więc wietrz lub używaj pochłaniacza. Stary lut zdejmuj plecionką lub odsysaczem.
Słowa: lutowanie, cyna, Sn63Pb37, bezołowiowa, topnik, kalafonia, grot, lutownica, zimny lut, opary, odsysacz, plecionka, temperatura lutowania
Źródło: https://en.wikipedia.org/wiki/Soldering (sprawdzono 2026-10)

### Zimny lut i inne usterki połączeń
Zimny lut powstaje, gdy elementy nie nagrzały się dość, by stopić cynę; przewodzi słabo albo przerywanie i potrafi zawieść po miesiącach. Lut "suchy" to taki, który poruszył się przy stygnięciu: słaby mechanicznie i elektrycznie. Za mało cyny to lut zawodny, za dużo zwykle oznacza złe zwilżanie. Bezołowiowe luty mogą wyglądać matowo mimo że są dobre. Przy elementach wrażliwych na ciepło pomaga radiator (np. krokodylek na nóżce). Przegrzanie odkleja ścieżki z płytki.
Słowa: zimny lut, suchy lut, przerywa, kontakt, usterka, naprawa, przegrzanie, ścieżka, płytka PCB, odpadła ścieżka
Źródło: https://en.wikipedia.org/wiki/Soldering (sprawdzono 2026-10)

### Jak mierzyć multimetrem i czego nie robić?
Napięcie mierzysz równolegle, przykładając sondy do elementu. Prąd szeregowo: przerywasz obwód i puszczasz prąd przez miernik, na właściwym zakresie (w gnieździe prądowym jest bezpiecznik). Opór i ciągłość (brzęczyk) mierzysz tylko przy wyłączonym zasilaniu i najlepiej z elementem odłączonym. Test diody pokazuje spadek napięcia w kierunku przewodzenia. Najczęstszy błąd: miernik ustawiony na prąd lub opór podłączony wprost do źródła napięcia, co robi zwarcie przez miernik i może go zniszczyć. Kategorie bezpieczeństwa: CAT II gniazdka, CAT III rozdzielnice i instalacja stała, CAT IV przyłącze; wybierz miernik z kategorią dla najgorszego miejsca pracy.
Słowa: multimetr, miernik, pomiar napięcia, pomiar prądu, szeregowo, równolegle, ciągłość, brzęczyk, dioda, bezpiecznik miernika, CAT III, zwarcie miernikiem
Źródło: https://en.wikipedia.org/wiki/Multimeter (sprawdzono 2026-10)

### Przetwornica (buck) czy stabilizator liniowy (LDO)?
Przetwornica obniżająca (buck) zamienia wyższe napięcie na niższe z dużą sprawnością, często ponad 90 %, bo zamiast palić nadmiar w ciepło magazynuje energię w cewce. Napięcie wyjściowe to napięcie wejściowe razy wypełnienie (z 12 V na 3 V to ok. 25 %). Wada: tętnienia i zakłócenia przełączania (zwykle 100 kHz – kilka MHz), więc potrzebne są kondensatory na wejściu i wyjściu. Stabilizator LDO działa przy małej różnicy napięć, nie hałasuje i jest prosty, ale cały nadmiar napięcia zamienia w ciepło, więc przy dużej różnicy i dużym prądzie się grzeje lub wyłącza. LDO pobiera też mały prąd spoczynkowy nawet bez obciążenia. Do zasilania węzła z 12 V lepsza jest przetwornica; LDO za przetwornicą może odfiltrować jej tętnienia, jeśli ma wysoki PSRR.
Słowa: przetwornica, buck, step-down, LDO, stabilizator, sprawność, ciepło, tętnienia, zakłócenia, 12 V na 5 V, zasilanie węzła, prąd spoczynkowy
Źródło: https://en.wikipedia.org/wiki/Buck_converter i https://en.wikipedia.org/wiki/Low-dropout_regulator (sprawdzono 2026-10; zakłócenia przełączania mogą przeszkadzać radiu, to wniosek ogólny)

### Po co BMS w pakiecie litowym?
BMS chroni przed przeładowaniem, nadmiernym rozładowaniem, za dużym prądem i złą temperaturą, a także wyrównuje (balansuje) ogniwa. Po przekroczeniu limitu odłącza obwód (przekaźnik lub tranzystor MOSFET). Balansowanie pasywne spala nadmiar z najbardziej naładowanych ogniw, aktywne przenosi energię do słabszych. Ogniwa Li-ion i LiFePO4 są szczególnie wrażliwe na za wysokie i za niskie napięcie, dlatego pakiet bez BMS jest ryzykowny. W LiFePO4 napięcie ogniwa prawie nic nie mówi o stopniu naładowania, więc proste balansowanie napięciem ma ograniczenia.
Słowa: BMS, balanser, balansowanie, przeładowanie, rozładowanie, pakiet ogniw, LiFePO4, Li-ion, zabezpieczenie, MOSFET, akumulator solarny
Źródło: https://en.wikipedia.org/wiki/Battery_management_system (sprawdzono 2026-10)

### Jak podejrzeć pasmo 868 MHz tanim odbiornikiem SDR?
SDR to radio, w którym większość obróbki sygnału robi program. Tani odbiornik RTL-SDR (układ Realtek RTL2832U z tunerem R820T/R820T2/R860) obejmuje ok. 24–1766 MHz, czyli także 433 i 868 MHz, ale widzi naraz tylko do 3,2 MHz pasma. Kosztuje ok. 30 dolarów (wersje dedykowane, 2025). Tanie przeróbki tunerów DVB-T mają gorsze ekranowanie i stabilność częstotliwości niż wersje robione pod SDR. Do oglądania widma służą programy SDR (np. GNU Radio, OpenWebRX); na wodospadzie widać impulsy LoRa i zakłócenia w paśmie. Odbiornik tylko słucha, nie nadaje.
Słowa: SDR, RTL-SDR, odbiornik, widmo, wodospad, 868 MHz, podgląd pasma, zakłócenia, RTL2832U, R820T, GNU Radio, OpenWebRX
Źródło: https://en.wikipedia.org/wiki/Software-defined_radio (sprawdzono 2026-10; zdanie o widoczności impulsów LoRa to wniosek ogólny)
