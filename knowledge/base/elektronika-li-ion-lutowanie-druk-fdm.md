# Akumulatory Li-ion, lutowanie i druk FDM

### Jak bezpiecznie ładować i przechowywać akumulatory Li-ion?
Ładowanie: stały prąd, potem stałe napięcie; kończy się, gdy prąd spadnie do około 3% początkowego. Dla zwykłych ogniw NMC maks. 4,2 V na ogniwo; przeładowanie grozi osadzaniem litu, a w skrajnych przypadkach wybuchem. Ładuj w 5–45 °C, przy 0–5 °C zmniejsz prąd, poniżej 0 °C nie ładuj wcale (lit osadza się na anodzie). Ładowanie powyżej ok. 80% przyspiesza starzenie. Przechowywanie: chłodno i przy 60–80% naładowania; samorozładowanie to ok. 1,5–2% miesięcznie, a zabezpieczenie może przy długim leżeniu rozładować ogniwo poniżej progu odcięcia. Ogniwa LFP mniej cierpią od wysokiego naładowania.
Słowa: Li-ion, akumulator, ładowanie, 4,2 V, przechowywanie, 80%, samorozładowanie, LFP, temperatura ładowania, 18650
Źródło: https://en.wikipedia.org/wiki/Lithium-ion_battery (sprawdzono 2026-10)

### Kiedy akumulator Li-ion jest niebezpieczny i co z nim zrobić?
Niebezpieczne są uszkodzenia mechaniczne, przegrzanie, przeładowanie i zwarcie. Ucieczka termiczna może podnieść temperaturę ogniwa powyżej 500 °C, a dym jest palny i toksyczny. Wymień akumulator spuchnięty, uszkodzony lub mocno zużyty (po ok. 3–5 latach lub 1000–2000 cyklach spada do ok. 80% pojemności). Nie wyrzucaj do śmieci, oddaj do punktu zbiórki. Używaj ładowarek i układów BMS z limitami napięcia i prądu. W urządzeniach terenowych trzymaj akumulator z dala od źródeł ciepła i uderzeń.
Słowa: spuchnięty akumulator, ucieczka termiczna, pożar baterii, BMS, utylizacja, zużycie ogniwa, cykle, zwarcie
Źródło: https://en.wikipedia.org/wiki/Lithium-ion_battery (sprawdzono 2026-10)

### Lutowanie: jaka cyna, topnik i technika?
Cyna ołowiowa 63/37 topi się w 183 °C w jednym punkcie, 60/40 w 183–190 °C; bezołowiowa (np. SAC) w ok. 217–220 °C i trudniej o dobre złącze, ale jest zalecana tam, gdzie dotykają dzieci, i na zewnątrz. Topnik usuwa tlenki i poprawia zwilżanie; do elektroniki kalafonia lub typu no-clean, a kwaśnych topników hydraulicznych nie używaj (zostawiają przewodzące, żrące resztki). Technika: oczyść, zestaw części, ogrzewaj elementy (nie cynę), daj topnik, dołóż cynę, zabierz ciepło i nie ruszaj do zastygnięcia. Trzymaj grot pocynowany. Do 25 W może brakować mocy na duże złącza, 100 W bywa za dużo dla płytek. Wentyluj, bo opary szkodzą także bez ołowiu.
Słowa: lutowanie, cyna, topnik, kalafonia, 63/37, bezołowiowa, grot, lutownica, SAC, opary
Źródło: https://en.wikipedia.org/wiki/Soldering (sprawdzono 2026-10)

### Zimne i suche luty: jak je poznać?
Zimny lut: części nie zostały dość nagrzane (np. grzano samą cynę), wygląda matowo, popękany lub z nierozlanymi grudkami; może przejść test i zawieść po miesiącach. Suchy lut: złącze poruszyło się podczas stygnięcia, jest matowe, ziarniste i słabo przewodzi. Za mało cyny daje zawodne złącze, za dużo zwykle oznacza słabe zwilżenie. Dobry lut jest gładki, błyszczący, z wklęsłym fasolkowym łączem; bezołowiowe mogą wyglądać matowo mimo poprawności. Naprawa: odessać starą cynę plecionką lub odsysaczem i polutować od nowa. Zbyt długie grzanie może odkleić ścieżki lub uszkodzić element.
Słowa: zimny lut, suchy lut, wady lutowania, odsysacz, plecionka, ścieżka odklejona, złącze, naprawa lutu
Źródło: https://en.wikipedia.org/wiki/Soldering (sprawdzono 2026-10)

### Druk FDM: jak działa i czym drukować obudowy?
FDM (FFF) przesuwa filament przez grzaną dyszę i układa model warstwa po warstwie. PLA to najłatwiejszy materiał, wystarczy mosiężna dysza, i zwykle druk w temperaturze pokojowej; ABS wymaga ogrzewanej, zamkniętej komory (ok. 60–70 °C) i wydziela więcej lotnych związków, więc potrzebuje wentylacji. Do wypełnianych włóknem (np. węglowych) potrzebna hartowana stalowa dysza. Typowe dysze to 0,3–1,0 mm. Warping wynika z niestabilnej temperatury, przeciągów lub wilgoci. Dyszę wymieniaj na gorąco, żeby nie wyciekała. Konkretnych temperatur dyszy i stołu źródło nie podaje, sprawdź kartę filamentu. Do obudów na zewnątrz zwykle bierze się materiały odporne na słońce (np. PETG lub ASA), to wiedza ogólna, a nie z tego źródła.
Słowa: druk 3D, FDM, FFF, PLA, PETG, ABS, dysza, warping, obudowa, filament, ASA
Źródło: https://en.wikipedia.org/wiki/Fused_filament_fabrication (sprawdzono 2026-10; zalecenie PETG/ASA: wiedza ogólna)
