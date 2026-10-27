# Elementy: kondensator, rezystor, prawo Ohma, LED, Schottky, MOSFET, bezpiecznik

### Kondensatory: rodzaje, oznaczenia i odsprzęganie
Pojemność podaje się w pF, nF i µF; oznaczenie trzycyfrowe: dwie pierwsze cyfry to wartość, trzecia to potęga dziesięciu w pF (473 = 47 nF). Ceramiczne są małe, tanie i dobre w wysokich częstotliwościach, mają bardzo niskie ESR, ale pojemność zależy od napięcia i temperatury. Elektrolityczne dają duże pojemności (powyżej ok. 1 µF), ale mają słabe tolerancje, starzeją się, ich ESR rośnie z ciepłem, i są spolaryzowane: odwrócenie biegunów może je zniszczyć, a aluminiowe po przekroczeniu prądu tętnień potrafią pęknąć. Kondensator odsprzęgający między zasilaniem a masą odprowadza szumy i skoki, działając jak lokalny zbiornik ładunku. Napięcie pracy musi być wyższe od przyłożonego.
Słowa: kondensator, pojemność, 473, ceramiczny, elektrolityczny, ESR, odsprzęganie, polaryzacja, napięcie pracy, nF
Źródło: https://en.wikipedia.org/wiki/Capacitor (sprawdzono 2026-10)

### Rezystory: kod barw i oznaczenia SMD
Rezystor czterokolorowy: dwa pierwsze paski to cyfry, trzeci mnożnik, czwarty tolerancja (brak czwartego to ±20%). Pięć i sześć pasków dodaje trzecią cyfrę i współczynnik temperaturowy. Moc zwykle wynika z rozmiaru: dyskretne to 1/10, 1/8 lub 1/4 W, rezystory mocy od 1 W. Szeregi: E12 ±10%, E24 ±5%, E96 ±1%. Szeregowo opory się sumują, równolegle sumuje się odwrotności (10, 5 i 15 Ω równolegle dają ok. 2,727 Ω). SMD: trzy cyfry (334 = 330 kΩ, 105 = 1 MΩ), "R" jako przecinek (4R7 = 4,7 Ω), cztery cyfry dla precyzyjnych (4992 = 49,9 kΩ) i kod EIA-96 (01C = 10 kΩ).
Słowa: rezystor, kod kolorów, opornik, SMD, E12, tolerancja, moc rezystora, szeregowo, równolegle, 4R7, EIA-96
Źródło: https://en.wikipedia.org/wiki/Resistor (sprawdzono 2026-10)

### Prawo Ohma i rezystor do diody LED
V = I·R, czyli I = V/R i R = V/I. Przykład ze źródła: 9 V na 300 Ω daje 30 mA. Moc (z innego źródła niż strona o prawie Ohma): P = V·I = I²·R. LED potrzebuje ograniczenia prądu, bo prąd rośnie wykładniczo z napięciem; do diod sygnalizacyjnych wystarcza szeregowy rezystor. Napięcia przewodzenia: czerwona 1,63–2,03 V, żółta 2,1–2,18 V, zielona 1,9–4 V, niebieska 2,48–3,7 V, biała 2,7–3,5 V. Przykład własny: zasilanie 5 V, dioda 2 V, prąd 20 mA: R = (5−2)/0,02 = 150 Ω, moc na rezystorze 0,06 W. LED ma anodę i katodę, w kierunku zaporowym nie świeci, a przebicie następuje przy ok. 5 V.
Słowa: prawo Ohma, LED, rezystor ograniczający, napięcie przewodzenia, dioda, anoda, katoda, 150 omów, prąd diody, obliczenia
Źródło: https://en.wikipedia.org/wiki/Ohm%27s_law i https://en.wikipedia.org/wiki/Light-emitting_diode (sprawdzono 2026-10; przykład 150 Ω: obliczenie własne)

### Dioda Schottky'ego: kiedy się przydaje?
Ma niski spadek napięcia w kierunku przewodzenia, ok. 0,15–0,45 V wobec 0,6–0,7 V dla zwykłej krzemowej, i bardzo szybko przełącza. Używa się jej w prostownikach zasilaczy impulsowych, detektorach i mikserach RF, w układach ESD oraz jako dioda blokująca w solarach off-grid, żeby akumulator nie rozładowywał się przez panel w nocy i prąd nie płynął między równoległymi łańcuchami. Wady: niskie napięcie zaporowe (często do 50 V) i stosunkowo duży prąd wsteczny. Źródło nie opisuje ochrony przed odwrotną polaryzacją wprost; to pokrewne zastosowanie blokowania prądu wstecznego, kosztem spadku napięcia.
Słowa: dioda Schottky, spadek napięcia, dioda blokująca, solar, prostownik, ochrona przed odwrotną polaryzacją, SS14, prąd wsteczny
Źródło: https://en.wikipedia.org/wiki/Schottky_diode (sprawdzono 2026-10)

### MOSFET jako przełącznik
W tranzystorze MOSFET z kanałem n wzbogacanym kanał przewodzi, gdy napięcie bramka–źródło przekroczy próg Vth; poniżej płynie tylko niewielki prąd upływu. W stanie włączonym zachowuje się jak rezystor sterowany napięciem bramki, a rezystancja rośnie z temperaturą. Do sterowania w stanie ustalonym prawie nie pobiera prądu, ale przy wysokich częstotliwościach trzeba ładować pojemność bramki. Kanał p włącza się, gdy bramka ma niższy potencjał niż źródło. Mocowe MOSFET-y są szeroko stosowane w zasilaczach impulsowych. Źródło nie opisuje tranzystorów "logic-level" ani ochrony przed odwrotną polaryzacją; przy sterowaniu z 3,3 V sprawdzaj w karcie katalogowej, przy jakim Vgs podano Rds(on).
Słowa: MOSFET, tranzystor, bramka, Vth, Rds(on), kanał p, przełącznik, logic level, sterowanie 3,3 V, przekaźnik elektroniczny
Źródło: https://en.wikipedia.org/wiki/MOSFET (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### Bezpieczniki: parametry i rodzaje
Znamionowy prąd to maksymalny ciągły, napięcie znamionowe musi być co najmniej równe napięciu źródła (niższe może nie przerwać prądu i dać łuk), zdolność wyłączania powinna przekraczać spodziewany prąd zwarciowy. Rodzaje: szybkie, ultraszybkie (półprzewodniki), zwłoczne (znoszą krótkie udary, np. rozruch silnika), wkładkowe, nożowe, szklane, SMD, samochodowe i termiczne. Bezpieczniki polimerowe PTC rosną rezystancją przy przeciążeniu i same się resetują po ostygnięciu, ale na niskim napięciu mają zauważalny spadek. Bezpiecznik jest szybszy i prostszy niż wyłącznik, lecz po zadziałaniu trzeba go wymienić. Dla zasilania akumulatorowego węzłów warto dać bezpiecznik w linii zasilającej (wiedza ogólna).
Słowa: bezpiecznik, zwłoczny, szybki, PTC, resetowalny, zdolność wyłączania, zwarcie, przeciążenie, polifuse, ochrona zasilania
Źródło: https://en.wikipedia.org/wiki/Fuse_(electrical) (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)
