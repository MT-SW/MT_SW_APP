# Elementy elektroniczne: kondensatory, rezystory, LED, MOSFET, bezpieczniki, przewody, uziemienie

### Kondensatory: rodzaje, polaryzacja i napięcie
Kondensator filtruje szumy i stabilizuje zasilanie (odsprzęganie), wygładza napięcie, przepuszcza prąd zmienny a blokuje stały i magazynuje krótkotrwale energię. Ceramiczne: małe, tanie, dobre przy wysokich częstotliwościach, o niskim ESR, ale pojemność zmienia się z napięciem i temperaturą. Elektrolityczne (aluminiowe, tantalowe): duża pojemność, powyżej ok. 1 µF, słabsza tolerancja i najkrótsza żywotność. Elektrolity są spolaryzowane: odwrócenie biegunowości może je zniszczyć. Napięcie pracy kondensatora musi być wyższe od napięcia w układzie. Oznaczenia: trzy cyfry to pF (np. 473 = 47 nF), litera to tolerancja (J ±5 %, K ±10 %, M ±20 %), zapis 4n7 to 4,7 nF. Żywotność elektrolitu maleje mniej więcej o połowę na każde 10 °C wzrostu temperatury.
Słowa: kondensator, ceramiczny, elektrolit, polaryzacja, odsprzęganie, 473, ESR, pojemność, napięcie pracy, nF, pF, µF
Źródło: https://en.wikipedia.org/wiki/Capacitor (sprawdzono 2026-10)

### Kondensatory i zgromadzony ładunek: bezpieczeństwo
Kondensator może trzymać ładunek po wyłączeniu zasilania; niebezpieczna jest zgromadzona energia: powyżej 10 J zwykle groźna, od 50 J potencjalnie śmiertelna. Duże i wysokonapięciowe rozładuj przed pracą. Absorpcja dielektryka może wtórnie naładować rozładowany kondensator. Przepięcie, odwrotna polaryzacja lub zbyt duży prąd tętnień mogą go spęcznić, rozerwać lub wybuchnąć. Czas ładowania w obwodzie RC określa stała czasowa τ = R · C.
Słowa: rozładowanie kondensatora, wybuch kondensatora, spęczniały kondensator, energia, absorpcja, stała czasowa, RC, bezpieczeństwo elektroniki
Źródło: https://en.wikipedia.org/wiki/Capacitor (sprawdzono 2026-10)

### Rezystory: kolory, moc i połączenia
Cztery paski: dwie cyfry, mnożnik i tolerancja (brak paska tolerancji to ±20 %); pięć lub sześć pasków: trzy cyfry, mnożnik, tolerancja (szósty to współczynnik temperaturowy). Rezystory sygnałowe mają zwykle 1/10, 1/8 lub 1/4 W, mocy 1 W i więcej. Moc: P = V · I = I² · R = V² / R; przekroczenie mocy zmienia opór lub pali rezystor. Szeregowo: R = R1 + R2 + …; równolegle: 1/R = 1/R1 + 1/R2 + … (np. 10, 5 i 15 Ω równolegle daje ok. 2,727 Ω). Szeregi wartości: E12 ±10 %, E24 ±5 %, E96 ±1 %. Kody SMD: 473 = 47 kΩ, 105 = 1 MΩ, 4R7 = 4,7 Ω, 1001 = 1,00 kΩ.
Słowa: rezystor, kod kolorów, pasek, moc rezystora, szeregowo, równolegle, E24, SMD kod, 473, tolerancja
Źródło: https://en.wikipedia.org/wiki/Resistor (sprawdzono 2026-10)

### Rezystor podciągający (pull-up) i ściągający (pull-down)
Wejście, do którego nic nie jest podłączone, "pływa" i ma nieokreślone napięcie. Pull-up łączy je z napięciem zasilania, pull-down z masą, nadając stan domyślny. Wartość ma być na tyle duża, by sygnał nadajnika mógł ją przebić, a na tyle mała, by szybko ściągała linię po puszczeniu. Tak działają np. magistrala I²C i przyciski.
Słowa: pull-up, pull-down, rezystor podciągający, wejście pływające, stan domyślny, przycisk, I2C, GPIO
Źródło: https://en.wikipedia.org/wiki/Resistor (sprawdzono 2026-10)

### Diody LED: napięcie, rezystor ograniczający, biegunowość
Prąd LED rośnie wykładniczo z napięciem, więc trzeba go ograniczyć rezystorem lub źródłem prądowym. Napięcie przewodzenia zależy od koloru: czerwona 1,63–2,03 V, pomarańczowa 2,03–2,10 V, żółta 2,10–2,18 V, zielona ok. 1,9–4,0 V, niebieska 2,48–3,7 V, biała 2,7–3,5 V, UV 3,1–4,4 V. LED świeci tylko w kierunku przewodzenia (anoda do plusa, katoda do minusa); napięcie wsteczne ok. 5 V może ją uszkodzić. Zwykłe diody wskaźnikowe mają od ok. 1 mA do ponad 20 mA; diody mocy potrzebują radiatora. Źródło nie podaje wartości prądu pinu mikrokontrolera, trzeba je sprawdzić w karcie katalogowej.
Słowa: dioda LED, napięcie przewodzenia, rezystor do LED, anoda, katoda, kolor LED, prąd LED, biała, czerwona, wskaźnik
Źródło: https://en.wikipedia.org/wiki/Light-emitting_diode (sprawdzono 2026-10)

### Tranzystor MOSFET jako przełącznik
MOSFET z kanałem N przewodzi, gdy napięcie bramka–źródło przekroczy napięcie progowe; z kanałem P odwrotnie (próg ujemny). Bramka jest izolowana, więc prawie nie pobiera prądu ustalonego, ale przy szybkim przełączaniu trzeba ładować jej pojemność. MOSFET dobrze znosi przepięcia od cewek (obciążenia indukcyjne). Jego opór w stanie włączonym rośnie z temperaturą; bez odpowiedniego chłodzenia tranzystor mocy może się przegrzać i zniszczyć. W typowym układzie dyskretnym obudowa (body) jest połączona ze źródłem, co tworzy wewnętrzną diodę. Za niskie napięcie bramki może nie włączyć tranzystora w pełni.
Słowa: MOSFET, kanał N, kanał P, napięcie progowe, bramka, przełącznik, dioda body, przegrzanie, obciążenie indukcyjne
Źródło: https://en.wikipedia.org/wiki/MOSFET (sprawdzono 2026-10; źródło nie omawia tranzystorów typu logic-level)

### Bezpieczniki: jak dobrać?
Bezpiecznik przerywa obwód, gdy topi się jego element; jest jednorazowy. Znamionowy prąd to maksymalny prąd ciągły. Napięcie bezpiecznika musi być co najmniej równe napięciu źródła (32 V bezpiecznik szklany może nie wyłączyć 230 V i powstanie łuk). Zdolność wyłączania musi przekraczać prąd zwarcia. Typy: szybki (fast-blow), bardzo szybki (dla półprzewodników), zwłoczny (slow-blow, dla silników z prądem rozruchu). Szklane mają małą zdolność wyłączania, ceramiczne i piaskowe większą. Bezpieczniki polimerowe (polyfuse, PPTC) wracają do stanu po ostygnięciu, ale mają dodatni spadek napięcia. Temperatura otoczenia obniża prąd zadziałania.
Słowa: bezpiecznik, fast-blow, slow-blow, polyfuse, PPTC, ochrona nadprądowa, zwarcie, prąd znamionowy, napięcie bezpiecznika
Źródło: https://en.wikipedia.org/wiki/Fuse_(electrical) (sprawdzono 2026-10; źródło nie omawia montażu bezpiecznika przy akumulatorze, ogólna zasada to umieszczać go jak najbliżej plusa baterii)

### Przekrój przewodu (AWG i mm²): dobór do prądu
Wyższy numer AWG to cieńszy przewód; zmniejszenie o 3 AWG podwaja pole przekroju. Opór miedzi to ok. 1 Ω na 1000 stóp (ok. 305 m) dla 10 AWG, 10 Ω dla 20 AWG, 100 Ω dla 30 AWG. Obciążalność zależy od izolacji (60, 75 lub 90 °C): np. 18 AWG (0,823 mm²) 10 A, 14 AWG (2,08 mm²) 15 A, 12 AWG (3,31 mm²) 20 A przy izolacji 60 °C (tabela NFPA 70, wartości mogą być niższe w praktyce). Orientacyjnie: 1 mm średnicy to ok. 18 AWG, 2 mm to ok. 12 AWG. Aluminium przewodzi ok. 61 % tak dobrze jak miedź. Długi cienki przewód zasilający powoduje spadek napięcia.
Słowa: AWG, przekrój przewodu, mm2, obciążalność, miedź, spadek napięcia, kabel zasilający, grubość przewodu, aluminium
Źródło: https://en.wikipedia.org/wiki/American_wire_gauge (sprawdzono 2026-10; konkretna instalacja wymaga lokalnych norm)

### Uziemienie: ziemia ochronna, sygnałowa i antenowa
Uziemienie ochronne (earth) służy bezpieczeństwu: daje prądowi zwarcia drogę o niskiej impedancji, więc wyłączają się zabezpieczenia, i odprowadza ładunki statyczne. Masa sygnałowa to wspólny odnośnik i powrót sygnałów w urządzeniu; zwykle jest połączona z obudową. W instalacji radiowej mogą być potrzebne trzy osobne uziemienia: odgromowe, elektryczne ochronne i wysokiej częstotliwości (RF); przewody ochronne sieci nie nadają się na uziemienie RF, bo mają dużą impedancję i mogą promieniować zakłócenia. Anteny monopolowe niskoczęstotliwościowe potrzebują zakopanych promieni; przy skalistej glebie zastępuje je przeciwwaga. Zdjęcie bolca uziemiającego jest niebezpieczne.
Słowa: uziemienie, masa, ziemia ochronna, uziemienie RF, pętla masy, przeciwwaga, zwarcie, RCD, wtyczka, antena i ziemia
Źródło: https://en.wikipedia.org/wiki/Ground_(electricity) (sprawdzono 2026-10)
