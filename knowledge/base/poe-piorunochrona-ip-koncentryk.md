# PoE, ochrona odgromowa, stopnie IP i kabel koncentryczny

### Czym jest PoE i jakie są standardy mocy?
PoE zasila urządzenie tym samym kablem sieciowym co dane. Standardy: 802.3af (Typ 1) do 15,4 W ze źródła i ok. 12,95 W u odbiornika; 802.3at (PoE+) 30 W / 25,5 W; 802.3bt Typ 3 do 60 W (51 W u odbiornika) i Typ 4 do 90 W (71,3 W). Zasilanie idzie parami 2 i 3 (tryb A), parami 1 i 4 (tryb B) albo wszystkimi czterema. W PoE aktywnym źródło wykrywa urządzenie po sygnaturze rezystancji i negocjuje moc. PoE pasywne podaje napięcie bez negocjacji (typowo 24 V lub 48 V, także 12, 18, 54 V), więc napięcie źródła i urządzenia musi się zgadzać; spotyka się je w sprzęcie Ubiquiti i MikroTik. Do większych mocy potrzebny jest kabel kategorii 5 lub lepszy. Maksymalnej długości źródło nie podaje.
Słowa: PoE, Power over Ethernet, 802.3af, 802.3at, 802.3bt, PoE pasywne, PoE aktywne, zasilanie przez Ethernet, injector, węzeł zewnętrzny
Źródło: https://en.wikipedia.org/wiki/Power_over_Ethernet (sprawdzono 2026-10)

### Jak działa ochrona odgromowa i co z anteną na maszcie?
Instalacja odgromowa przechwytuje wyładowanie i odprowadza prąd do ziemi: zwody (rodzaj piorunochronu) na najwyższych punktach, przewody odprowadzające i uziemienie. Uziemienie ma mieć niską rezystancję i indukcyjność, a przewody odprowadzające być krótkie i z łagodnymi łukami. Przedmioty przewodzące w pobliżu trzeba połączyć z instalacją (wyrównanie potencjałów), żeby uniknąć przeskoków. Dla anten i łączy: ograniczniki przepięć obejmują iskierniki lub elementy między przewodem a ziemią, a w torach zasilania i danych montuje się mniejsze ograniczniki. Żadna instalacja nie gwarantuje pełnej ochrony, bo prąd pioruna może przekroczyć 150 000 A. Nie wchodź na maszt ani dach w czasie burzy.
Słowa: piorun, odgromowa, uziemienie, ogranicznik przepięć, maszt, antena, zwód, burza, przepięcie, iskiernik
Źródło: https://en.wikipedia.org/wiki/Lightning_rod (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### Co znaczą stopnie ochrony IP65, IP67, IP68?
Pierwsza cyfra to ochrona przed pyłem: 6 oznacza pyłoszczelność. Druga to woda: 5 odporność na strumień wody z dyszy, 7 zanurzenie (przykład: do 1 m przez 30 minut), 8 zanurzenie głębsze wg deklaracji producenta, zwykle do 3 m. Uwagi: oceny nie sumują się powyżej IPX6, więc IP67 nie gwarantuje odporności na strumień wody (urządzenie spełniające oba zapisuje się IPX5/IPX7). Testy dotyczą tylko słodkiej wody, nie słonej, olejów czy rozpuszczalników. Cyfra 5 przy pyle nie oznacza pełnej szczelności. IP nie jest równoważne klasom NEMA. Przy obudowach na zewnątrz liczy się też szczelność dławic i zamknięcia, a to zależy od wykonania.
Słowa: IP65, IP67, IP68, stopień ochrony, szczelność, pyłoszczelny, wodoodporny, obudowa zewnętrzna, deszcz, dławik
Źródło: https://en.wikipedia.org/wiki/Ingress_protection_rating (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### Kabel koncentryczny: impedancja i straty
Najczęściej stosuje się 50 lub 52 Ω (kompromis między mocą a tłumieniem) i 75 Ω (telewizja, radio); wszystkie elementy toru mają mieć tę samą impedancję, by uniknąć odbić. Tłumienie rośnie z częstotliwością (straty w dielektryku i rezystancja przewodów), a grubszy kabel ma mniejsze straty. Przykłady z tabeli: RG-58 maks. 13,1 dB na 100 stóp przy 750 MHz i ok. 1,06 dB/m przy 2,4 GHz; LMR-400 maks. 3,5 dB na 100 stóp przy 750 MHz i 0,223 dB/m przy 2,4 GHz. RG-58 (50 Ω, pojedynczy ekran) ma wyższe straty niż LMR-400 (50 Ω, piana, podwójny ekran). Kabel o sztywnym ekranie z rury może się załamać i dawać straty. Złączy strona nie omawia; ich typy sprawdź osobno w opisie złączy.
Słowa: koncentryk, kabel koncentryczny, 50 omów, 75 omów, RG-58, LMR-400, tłumienie, straty w kablu, impedancja, ekran
Źródło: https://en.wikipedia.org/wiki/Coaxial_cable (sprawdzono 2026-10)
