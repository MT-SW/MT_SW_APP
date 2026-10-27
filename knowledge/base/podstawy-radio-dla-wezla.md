# Podstawy radia dla węzła: decybele, SWR, ERP, Fresnel, modulacja

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

### Decybele: wzory i zasady dodawania
Poziom mocy L = 10 · log10(P / P0) dB; poziom amplitudy (napięcia) L = 20 · log10(F / F0) dB. Odwrotnie: P = P0 · 10^(L/10). dBm = 10 · log10(P / 1 mW), dBW = 10 · log10(P / 1 W); 0 dBm = 1 mW, 0 dBW = 1 W, 1 dBW = 30 dBm. Wartości do zapamiętania: 1 dB to około 1,259 razy mocy, 3 dB to około 2 razy mocy, 6 dB to około 4 razy mocy (2 razy amplitudy), 10 dB to 10 razy mocy, 20 dB to 10 razy amplitudy. Zyski i straty w szeregu dodają się w dB (10 + 8 + 7 = 25 dB, czyli około 316 razy). Niezależnych mocy w dB się nie dodaje: dwa źródła po 90 dB dają około 93 dB, nie 180; trzeba przeliczyć na liniowe, dodać i wrócić. Średnia logarytmiczna 70 i 90 dB to 87 dB, arytmetyczna 80 dB. Źródło zaznacza, że równania nie wyrenderowały się w pełni, więc to standardowe postacie zgodne z definicjami.
Słowa: decybel, dB, dBm, dBW, 3 dB podwojenie, 10 dB dziesięciokrotnie, logarytm, moc, wzmocnienie, tłumienie
Źródło: https://en.wikipedia.org/wiki/Decibel (sprawdzono 2026-10)

### Kabel koncentryczny: 50 czy 75 omów, straty, wilgoć
Najczęściej używane impedancje to 50 (52) i 75 Ω; 50 Ω to kompromis między mocą a tłumiennością, a 75 Ω typowo telewizja i anteny krótkofalowe małej mocy. Najmniejsze straty przy typowych dielektrykach dają 52 do 64 Ω. Tłumienność maksymalna przy 750 MHz na 100 stóp: RG-58 13,10 dB, RG-213 5,98 dB, LMR-400 3,5 dB (zawsze 50 Ω). Współczynnik skrócenia: RG-58 i RG-213 0,66, LMR-400 0,85; dielektryk piankowy 0,78 do 0,88. Pianka może chłonąć wilgoć, co mocno zwiększa straty, a podpory gwiazdowe są bardzo podatne na wnikanie wody. Kable „flooded” mają żel blokujący wodę. O złączach N i SMA źródło nic nie mówi. Przy 868 MHz krótki, grubszy kabel (jak LMR-400) mocno ogranicza stratę; to wniosek z podanych liczb (częstotliwość inna niż 750 MHz).
Słowa: koncentryk, RG-58, RG-213, LMR-400, 50 omów, impedancja, tłumienność, współczynnik skrócenia, wilgoć w kablu, pigtail
Źródło: https://en.wikipedia.org/wiki/Coaxial_cable (sprawdzono 2026-10)

### GPS: ile satelitów, jaka dokładność, jak szybko fix
Konstelacja nominalna to 24 satelity, a według źródła na 25 kwietnia 2026 działało 31. Do pozycji potrzeba co najmniej czterech widocznych; z dowolnego miejsca widać co najmniej sześć, a przy czystym horyzoncie zwykle dziewięć. Dokładność: lepsza niż 3,5 m dla dobrych odbiorników, około 5 m po wyłączeniu Selective Availability (maj 2000), około 30 cm z L5, do 2 cm w odbiornikach geodezyjnych, w smartfonie 4,9 m lub lepiej ze wspomaganiem Wi-Fi. Pierwsza pozycja zajmuje do 30 s (odczyt efemeryd), a pobranie pełnego almanachu 12,5 minuty. Na dokładność wpływają jakość odbiornika i anteny, układ satelitów (skupione w jednym miejscu pogarszają), opóźnienia atmosferyczne, zasłonięcie widoku i zagłuszanie. Terminów cold/warm/hot start ani systemów GLONASS, Galileo i BeiDou źródło nie opisuje.
Słowa: GPS, GNSS, satelity, dokładność GPS, fix, almanach, efemerydy, Selective Availability, L5, zimny start
Źródło: https://en.wikipedia.org/wiki/Global_Positioning_System (sprawdzono 2026-10)

