# Anteny od strony teorii: dipol, ćwierćfalówka, 5/8, Yagi, polaryzacja

### Jak policzyć długość dipola półfalowego?
Dipol skraca się o współczynnik k ok. 0,95 (od 0,98 dla cienkiego drutu do 0,94 dla grubego przewodu), co daje całkowitą długość ok. 143 / f metrów, gdzie f w MHz. Dla 869,44 MHz wychodzi ok. 16,4 cm całości (dwa ramiona po ok. 8,2 cm), a dla 433,5 MHz ok. 33 cm. Na rezonansie impedancja jest czysto rzeczywista, ok. 65–73 Ω. Zysk 2,15 dBi (czyli 0 dBd). Dipol pionowy promieniuje dookólnie w poziomie, z zerami wzdłuż osi drutu. Grubszy przewód daje szersze pasmo. Na koniec i tak dostrój antenę pomiarem SWR.
Słowa: dipol, długość anteny, 143/f, półfalowy, 16,4 cm, 33 cm, 73 omy, 2,15 dBi, obliczyć antenę, 868 MHz, 433 MHz, skrócenie
Źródło: https://en.wikipedia.org/wiki/Dipole_antenna (sprawdzono 2026-10; długości dla 869,44 i 433,5 MHz policzone ze wzoru źródła)

### Dipol a kabel koncentryczny: po co balun?
Dipol zasilany w środku jest symetryczny, a koncentryk niesymetryczny. Bez balunu prąd płynie po zewnętrznej stronie oplotu, co zniekształca charakterystykę i zmienia impedancję. Dlatego między koncentryk a dipol daje się balun (lub dławik na kablu). Dipol pętlowy (folded) ma około czterokrotnie wyższą impedancję niż zwykły. Przy nadawaniu na końcach dipola jest wysokie napięcie, więc izolatory muszą to wytrzymać.
Słowa: balun, dławik, symetryzator, prąd na oplocie, dipol pętlowy, folded dipole, impedancja, izolator
Źródło: https://en.wikipedia.org/wiki/Dipole_antenna (sprawdzono 2026-10)

### Antena ćwierćfalowa (monopol) i przeciwwaga
Monopol ćwierćfalowy na płaszczyźnie masy (ground plane) ma rezystancję promieniowania ok. 36,5 Ω i w idealnych warunkach zysk ok. 5,19 dBi; w realnych instalacjach mniej, bo płaszczyzna masy jest mała. Płaszczyzna masy jest częścią anteny: powinna sięgać co najmniej pół fali od elementu, a za mała zabiera 2–5 dBi. Rzeczywista długość jest ok. 5 % krótsza niż teoretyczna ćwierć fali: dla 869,44 MHz to ok. 8,2 cm, dla 433,5 MHz ok. 16,4 cm. Monopol zasila się zwykłym koncentrykiem.
Słowa: ćwierćfalówka, monopol, ground plane, przeciwwaga, radiale, 36 omów, 8,2 cm, 16,4 cm, płaszczyzna masy, antena GP
Źródło: https://en.wikipedia.org/wiki/Monopole_antenna (sprawdzono 2026-10; długości policzone wg zasady 5 % skrócenia)

### Antena 5/8 fali: kiedy lepsza?
Monopol 5/8 fali kieruje więcej mocy poziomo: zysk ok. 3 dB wyższy niż ćwierćfalówka (ok. 8,16 dBi przy idealnej masie), rezystancja promieniowania ok. 53 Ω. Jest popularny w łączności naziemnej. Dłuższe monopole wysyłają już zbyt dużo mocy w listek pod wysokim kątem. Dla LoRa w terenie oznacza to zwykle lepszy zasięg płasko, ale gorszy do stacji tuż pod anteną lub wysoko nad nią (wniosek ogólny).
Słowa: 5/8 fali, 5/8 lambda, zysk anteny pionowej, 8 dBi, kąt promieniowania, antena bazowa, zasięg płaski
Źródło: https://en.wikipedia.org/wiki/Monopole_antenna (sprawdzono 2026-10; ostatnie zdanie to wniosek ogólny)

### Antena Yagi: budowa i parametry
Yagi ma jeden element zasilany (dipol lub dipol pętlowy), za nim reflektor (nieco dłuższy), a przed nim direktory (nieco krótsze). Zysk sięga do ok. 20 dBi i rośnie z liczbą elementów; stosunek przód/tył do ok. 20 dB. Pasmo typowej konstrukcji to ok. 2–3 % częstotliwości środkowej i zwęża się przy większym zysku. Elementy rozstawia się co 1/10–1/4 fali. Polaryzacja liniowa, montowana poziomo lub pionowo; dla Meshtastic używaj pionowo, jak reszta sieci. Elementy bierne można mocować do metalowej belki bez izolacji. Prostych wzorów nie ma, projektuje się symulacją lub z gotowych opracowań (np. NBS/NIST). Yagi nadaje się do łączy punkt–punkt.
Słowa: Yagi, antena kierunkowa, reflektor, direktor, element zasilany, 20 dBi, przód tył, punkt-punkt, kierunkowa LoRa, belka
Źródło: https://en.wikipedia.org/wiki/Yagi%E2%80%93Uda_antenna (sprawdzono 2026-10; uwaga o pionowym montażu dla sieci to wniosek z zasad polaryzacji)

### Polaryzacja: dlaczego anteny muszą stać tak samo?
Antena liniowa jest niewrażliwa na przeciwną polaryzację: odbiór pionowo nadanego sygnału anteną poziomą znacznie osłabia sygnał, a w kontrolowanych warunkach nawet do zera. Pionowy bat ma polaryzację pionową. Dlatego wszystkie anteny w sieci powinny być ustawione pionowo; węzeł leżący płasko (np. telefon z anteną w kieszeni, antena położona na stole) traci zasięg. Polaryzację kołową odbiera się każdą anteną liniową, ale ze stratą. Źródło nie podaje liczby decybeli strat.
Słowa: polaryzacja, pionowa, pozioma, antena leży, ustawienie anteny, krzyżowa, kołowa, strata, bat
Źródło: https://en.wikipedia.org/wiki/Polarization_(waves) (sprawdzono 2026-10; przykłady z węzłem to wniosek ogólny, zdanie o kołowej to wiedza ogólna)

### Antena kolinearna (kilka dipoli w jednej rurze)
Kilka pionowych dipoli półfalowych ustawionych jeden nad drugim i zasilanych w fazie daje większy zysk, zachowując dookólną charakterystykę w poziomie. To typowa konstrukcja anten bazowych VHF/UHF, w tym większości "wysokozyskowych" anten 868 MHz w rurze z włókna szklanego. Większy zysk oznacza węższy listek w pionie, więc stacja bardzo blisko pod anteną może słyszeć ją gorzej (wniosek ogólny).
Słowa: kolinearna, collinear, antena bazowa, włókno szklane, dookólna, zysk, wąski listek, stacja bazowa, antena 868 w rurze
Źródło: https://en.wikipedia.org/wiki/Dipole_antenna (sprawdzono 2026-10; uwagi o antenach 868 MHz i listku to wnioski ogólne)
