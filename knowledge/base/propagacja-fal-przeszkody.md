# Rozchodzenie się fal radiowych, przeszkody i horyzont radiowy

### Jak rozchodzą się fale na częstotliwościach LoRa (433, 868 MHz)?
To pasma VHF/UHF (UHF to 300–3000 MHz). Główny sposób propagacji to linia widzenia (fala bezpośrednia); na powierzchni Ziemi ogranicza ją horyzont optyczny, w tekście źródła rzędu 64 km. Czasem występuje też ducting, czyli prowadzenie fali w warstwie atmosfery, i wtedy sygnał dociera znacznie dalej. Fale długie (LF, MF) podążają za krzywizną Ziemi (fala przyziemna), a krótkie (HF) odbijają się od jonosfery; to nie dotyczy LoRa na 433 i 868 MHz.
Słowa: propagacja, linia widzenia, LOS, UHF, VHF, ducting, jonosfera, fala przyziemna, horyzont, zasięg fal
Źródło: https://en.wikipedia.org/wiki/Radio_propagation (sprawdzono 2026-10)

### Dlaczego inwersja temperatury i pogoda zmieniają zasięg?
Inwersje temperatury mogą uginać fale VHF z powrotem ku ziemi, przez co stacje FM słychać nawet o setki kilometrów dalej. Burze lodowe, powiązane z inwersjami, powodują rozpraszanie i zakłócenia. Deszcz i para wodna tłumią niektóre częstotliwości w linii widzenia, a telefony na UHF 700–2600 MHz są bardziej wrażliwe na zmiany pogody. To tłumaczy, dlaczego w niektóre dni łączność LoRa jest zaskakująco daleka lub słabsza niż zwykle.
Słowa: inwersja, pogoda, zasięg zmienny, deszcz, dobra propagacja, ducting, nietypowy zasięg, para wodna
Źródło: https://en.wikipedia.org/wiki/Radio_propagation (sprawdzono 2026-10)

### Jak rosną straty wraz z odległością (FSPL)?
Moc spada z kwadratem odległości: podwojenie odległości daje czterokrotnie mniejszą gęstość mocy (−6 dB). Straty w wolnej przestrzeni w decybelach (odległość w km, częstotliwość w GHz): FSPL ≈ 92,45 + 20·log10(d) + 20·log10(f). Przykład ze źródła-formuły: 10 km i 2 GHz daje ok. 118,5 dB. Dla LoRa 868 MHz (0,868 GHz) i 10 km wychodzi 92,45 + 20 + (−1,23) ≈ 111,2 dB (obliczenie z tego wzoru). Wzór zakłada brak odbić, pasującą polaryzację i wolną co najmniej 60 % pierwszej strefy Fresnela; w rzeczywistości dochodzą straty na przeszkodach.
Słowa: FSPL, straty w wolnej przestrzeni, 92,45, odległość, log10, kwadrat odległości, 6 dB, budżet łącza, Friis
Źródło: https://en.wikipedia.org/wiki/Free-space_path_loss (sprawdzono 2026-10; przykład 868 MHz policzony wg wzoru ze źródła)

### Czym jest horyzont radiowy i k = 4/3?
To miejsce, gdzie promienie z anteny stykają się stycznie z powierzchnią Ziemi. Przy normalnej pogodzie załamanie w atmosferze działa tak, jakby promień Ziemi był o 4/3 większy, co wydłuża zasięg o ok. 15 %. W złej pogodzie współczynnik k może spaść, nawet poniżej 1, co skraca zasięg i powoduje zaniki. Proste wzory dają tylko najlepszy możliwy zasięg i nie mówią o jakości sygnału w danym miejscu. Średni promień Ziemi to ok. 6370 km. Im wyżej antena, tym dalej horyzont; praktycznie wysokość masztu daje więcej niż zwiększanie mocy.
Słowa: horyzont radiowy, k-factor, 4/3 Ziemi, wysokość anteny, maszt, zasięg, krzywizna Ziemi, refrakcja
Źródło: https://en.wikipedia.org/wiki/Radio_horizon (sprawdzono 2026-10; zdanie o maszcie to wniosek ogólny)

### Jak zachowują się przeszkody: budynki, drzewa, wzgórza?
Budynki, drzewa, wzgórza, góry i niektóre linie energetyczne powodują brak linii widzenia. Jedne przeszkody odbijają sygnał, inne go pochłaniają lub zniekształcają. Niskie częstotliwości lepiej przechodzą przez budynki i listowie niż wysokie. Dyfrakcja pozwala falom zaginać się za ostre krawędzie (efekt "ostrza noża"), ale kosztem sporych strat. Częściowe zasłonięcie pierwszej strefy Fresnela da się często obsłużyć lepszą anteną. Gdy linii widzenia nie ma, pomagają przekaźniki lub odbicia od okolicznych obiektów (wielodrogowość).
Słowa: przeszkody, budynki, drzewa, wzgórza, dyfrakcja, ostrze noża, brak LOS, NLOS, odbicia, wielodrogowość, listowie, przekaźnik
Źródło: https://en.wikipedia.org/wiki/Radio_propagation i https://en.wikipedia.org/wiki/Knife-edge_effect (sprawdzono 2026-10; liczb strat na przeszkodach źródła nie podają)

### Dlaczego przy odbiciu od ziemi sygnał potrafi się wzmocnić albo osłabić?
Odbicie od powierzchni (ziemia, woda, dach) dociera do odbiornika z innym opóźnieniem niż fala bezpośrednia. Zależnie od fazy sygnały się dodają lub odejmują. Na VHF odbicie od ziemi powoduje interferencje. Dlatego czasem przesunięcie anteny o kilkanaście centymetrów albo zmiana wysokości zmienia odbiór. Modele propagacji są empiryczne i przewidują medianę strat na trasie, a nie dokładne zachowanie w danym punkcie.
Słowa: odbicie, interferencja, faza, wielodrogowość, zaniki, fading, przesunięcie anteny, model empiryczny
Źródło: https://en.wikipedia.org/wiki/Radio_propagation (sprawdzono 2026-10; zdanie o przesunięciu anteny to wniosek ogólny)
