# Podstawy: decybele, SWR, ERP, strefa Fresnela, baterie

### Jak liczyć w decybelach (dB, dBm, dBi, dBd)?
Moc: poziom w dB = 10 · log10(P / P0). Napięcie: 20 · log10(V / V0), bo moc rośnie z kwadratem napięcia. Zapamiętaj: +3 dB to w przybliżeniu dwa razy więcej mocy, +10 dB to dziesięć razy, +6 dB to dwa razy wyższe napięcie (cztery razy moc). Wzmocnienia i straty w torze dodaje się w dB. dBm to moc względem 1 mW (0 dBm = 1 mW), a 30 dBm = 1 W. dBi to zysk anteny względem anteny izotropowej, dBd względem półfalowego dipola, przy czym 0 dBd = 2,15 dBi.
Słowa: decybele, dB, dBm, dBi, dBd, +3 dB, logarytm, moc, wzmocnienie, strata, jednostki, 2,15
Źródło: https://en.wikipedia.org/wiki/Decibel (sprawdzono 2026-10)

### Co to jest SWR i jaki jest zwrot mocy?
SWR (współczynnik fali stojącej) mówi, jak dobrze obciążenie (antena) pasuje do impedancji linii; 1:1 oznacza brak fali odbitej. SWR = (1 + |Γ|) / (1 − |Γ|), gdzie |Γ| to stosunek napięcia odbitego do padającego. Wartości (SWR: odbita moc, tłumienie odbicia): 1,1:1 0,23 % 26,4 dB; 1,2:1 0,83 % 20,8 dB; 1,5:1 4 % 14 dB; 2:1 11,1 % 9,5 dB; 3:1 25 % 6 dB; 6:1 51 % 2,9 dB. Odbita moc to |Γ|² mocy padającej. Źródło nie podaje liczbowej granicy "dobrego" SWR, ale mówi, że bliskie 1:1 jest optymalne i bezpieczne dla nadajnika, a wysoki SWR szkodzi bardziej przy długich liniach i wyższych częstotliwościach. Dokumentacja Meshtastic podaje kryterium VSWR poniżej 2.
Słowa: SWR, VSWR, KFS, fala stojąca, odbita moc, return loss, tłumienie odbicia, impedancja, 50 omów, dopasowanie, współczynnik odbicia
Źródło: https://en.wikipedia.org/wiki/Standing_wave_ratio (sprawdzono 2026-10; tabela wartości wyliczona w źródle)

### Jak mierzy się SWR i czy kabel go zmienia?
Mierniki przelotowe (kierunkowe sprzęgacze) przepuszczają pełną moc nadajnika i muszą pasować do impedancji linii (zwykle 50 Ω lub 75 Ω). Analizatory anten, mostki i analizatory sieci pracują z małą mocą zamiast nadajnika. Przykład: SWR 6:1 przez 75 m kabla RG-8A daje ok. 2,2 dB straty na 3,5 MHz, a na 146 MHz ok. 10,8 dB: straty rosną z częstotliwością, więc przy wysokich częstotliwościach niski SWR jest ważniejszy.
Słowa: pomiar SWR, miernik, analizator anten, NanoVNA, kabel, straty, RG-8, częstotliwość, sprzęgacz
Źródło: https://en.wikipedia.org/wiki/Standing_wave_ratio (sprawdzono 2026-10)

### Czym różni się ERP od EIRP i jak je liczyć?
ERP to moc, jakiej wymagałby idealny półfalowy dipol, by dać taką samą gęstość mocy w kierunku głównym jak badana antena. EIRP to to samo względem anteny izotropowej. EIRP jest zawsze o 2,15 dB większe niż ERP (EIRP = 1,64 × ERP). ERP = moc wejściowa anteny × zysk względem dipola; moc wejściowa anteny to moc nadajnika pomniejszona o straty toru (filtry, kabel, dopasowanie). Przykład: 100 W (20 dBW), 6 dB strat, antena Yagi 8,77 dBd daje ERP tuż poniżej 22,77 dBW i EIRP tuż poniżej 24,92 dBW.
Słowa: ERP, EIRP, moc promieniowana, limit mocy, zysk anteny, straty toru, 2.15, dipol, izotropowa, jak policzyć moc
Źródło: https://en.wikipedia.org/wiki/Effective_radiated_power (sprawdzono 2026-10)

### Czym jest strefa Fresnela i ile powinna być wolna?
To zagnieżdżone elipsoidalne obszary wokół prostej linii nadajnik–odbiornik; w pierwszej biegnie najsilniejsza ścieżka sygnału. Przeszkody w niej osłabiają sygnał nawet przy widoczności optycznej. Zasada: pierwsza strefa powinna być wolna w ok. 80 %, a co najmniej w 60 % (zasłonięcie nie więcej niż 40 %, najlepiej 20 % lub mniej). Odbicia od powierzchni wewnątrz strefy mogą pomagać lub szkodzić zależnie od fazy i polaryzacji; mgła i wilgoć mogą rozpraszać część częstotliwości.
Słowa: Fresnel, strefa Fresnela, 60 procent, widoczność optyczna, LOS, przeszkody, odbicia, linia widzenia, wysokość masztu
Źródło: https://en.wikipedia.org/wiki/Fresnel_zone (sprawdzono 2026-10)

### Jak działa modulacja LoRa i jaki ma zakres parametrów?
Modulacja rozpraszająca widmo, wywodząca się z chirp spread spectrum: symbol to cyklicznie przesunięty chirp w paśmie wokół częstotliwości nośnej. Spreading factor od SF5 do SF12: niższy SF to większa szybkość i gorsza czułość, wyższy to lepsza czułość, mniejsza szybkość, dłuższy czas nadawania i większe zużycie energii. Szybkość 0,3 do 27 kb/s zależnie od SF, korekcja błędów (FEC), budżet łącza ok. 155–170 dB, typowa moc modemów do +22 dBm. Pasma: EU433 433,05–434,79 MHz, EU868 863–870/873 MHz, US915 902–928 MHz, IN865 865–867 MHz. Zasięg w idealnych warunkach ok. 330 km, praktycznie ok. 10 km; w miastach do 4,8 km, na wsi z widocznością 16 km i więcej.
Słowa: LoRa, chirp, CSS, spreading factor, SF5, SF12, FEC, budżet łącza, pasma, zasięg praktyczny, modulacja
Źródło: https://en.wikipedia.org/wiki/LoRa (sprawdzono 2026-10)

### Jakie napięcia ma ogniwo Li-Po / Li-ion i jak je przechowywać?
Nominalnie 3,6–3,7 V, maksymalnie ok. 4,2 V po naładowaniu, minimalnie ok. 2,7–3,0 V po rozładowaniu (dokładne wartości z karty katalogowej). Do długiego przechowywania trzymaj ogniwa na 3,6–3,9 V; samorozładowanie ok. 5 % miesięcznie. Ogniwa pęcznieją przy wysokim naładowaniu lub przeładowaniu; ogniwa kieszonkowe (pouch) pęcznieją widocznie, bo nie mają twardej obudowy. Ryzyko pożaru dotyczy użytkowania niezgodnego z instrukcją, najczęściej podczas ładowania. Ogniwa muszą mieć elektroniczne zabezpieczenie przed przeładowaniem i nadmiernym rozładowaniem.
Słowa: Li-Po, LiPo, Li-ion, napięcie ogniwa, 4,2 V, 3,7 V, przechowywanie, pęcznienie, pożar, zabezpieczenie, rozładowanie, akumulator
Źródło: https://en.wikipedia.org/wiki/Lithium_polymer_battery (sprawdzono 2026-10)

### Jakie napięcia ma ogniwo LiFePO4 (LFP) i jak je ładować?
Napięcie nominalne 3,2 V; nie przekraczaj 3,6 V na ogniwo przy ładowaniu i nie rozładowuj poniżej 2,5 V (głębsze rozładowanie powoduje poważne, zwykle nieodwracalne uszkodzenia, więc potrzebny jest system zabezpieczeń). Trwałość 2500 do ponad 9000 cykli (zwykle ponad 3000), dla porównania NMC ok. 1000–2300. Bezpieczniejsze termicznie i chemicznie niż LiCoO2 i NMC, ale pożar jest możliwy przy przeładowaniu, bardzo wysokiej temperaturze lub uszkodzeniu mechanicznym. Ogniwa trzeba wyrównać przed złożeniem pakietu; unikaj zbyt wysokich napięć, wyrównywania na siłę i ciągłego ładowania podtrzymującego. Cztery ogniwa szeregowo dają nominalnie 12,8 V, co zbliża LFP do akumulatora kwasowego 12 V po dostosowaniu ładowarki.
Słowa: LiFePO4, LFP, 3,2 V, 3,6 V, 2,5 V, cykle, trwałość, bezpieczeństwo, ładowanie LFP, pakiet 12 V, akumulator solarny
Źródło: https://en.wikipedia.org/wiki/Lithium_iron_phosphate_battery (sprawdzono 2026-10)
