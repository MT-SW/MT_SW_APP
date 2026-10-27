# Elektronika i okablowanie: podstawy dla budujących węzły

### Prawo Ohma: jak liczyć napięcie, prąd i opór?
R = V / I, czyli V = I · R oraz I = V / R (napięcie w woltach, prąd w amperach, opór w omach). Wzór dotyczy elementów "omowych", takich jak rezystory; dioda nie jest omowa, bo jej opór zmienia się z napięciem. Rezystory połączone szeregowo lub równolegle można zastąpić jednym zastępczym. Opór przewodu zależy od jego rezystywności, długości i przekroju (R = ρ · długość / przekrój), więc długi i cienki kabel zasilający zjada napięcie. Moc obliczysz jako P = V · I (wzór standardowy, spoza użytego źródła).
Słowa: prawo Ohma, napięcie, prąd, opór, rezystor, moc, wat, wzór, spadek napięcia, kabel zasilający, dioda
Źródło: https://en.wikipedia.org/wiki/Ohm%27s_law (sprawdzono 2026-10; wzór na moc to wiedza ogólna, nie z tego źródła)

### Czym jest MPPT i czym różni się od zwykłej regulacji PWM?
MPPT to sposób sterowania ładowaniem tak, by panel pracował w punkcie maksymalnej mocy (MPP). Przetwornica DC-DC zmienia wypełnienie, a przez to oporność widzianą przez panel. Przydaje się przy akumulatorze częściowo rozładowanym (rano napięcie ładowania jest dużo niższe niż napięcie MPP panelu) oraz przy zacienieniu i niedopasowaniu paneli. Najpopularniejsza metoda (perturb and observe) jest prosta, ale daje drgania mocy wyjściowej. Metody oparte na stałym stosunku napięć są tanie i proste, ale nie gwarantują znalezienia prawdziwego MPP. Źródło nie porównuje z PWM i nie podaje, ile MPPT zyskuje w małych układach.
Słowa: MPPT, PWM, regulator ładowania, panel solarny, punkt mocy maksymalnej, perturb and observe, ładowanie rano, zacienienie
Źródło: https://en.wikipedia.org/wiki/Maximum_power_point_tracking (sprawdzono 2026-10)

### Jaki kabel koncentryczny wybrać i ile traci?
Do łączności radiowej standardem jest 50 Ω (75 Ω to telewizja i radio). Wszystkie elementy toru powinny mieć tę samą impedancję, bo niedopasowanie daje odbicia i fale stojące. Tłumienie rośnie z częstotliwością. Maksymalne tłumienie przy 750 MHz na 100 stóp (ok. 30 m): RG-174 23,6 dB, RG-58 13,1 dB, RG-213 6,0 dB, LMR-400 3,5 dB, kabel twardy (hardline) 0,5 dB. Dla LMR-400 przy 2,4 GHz źródło podaje 0,223 dB/m. Piankowy dielektryk ma ok. 15 % mniejsze straty niż lity, ale może chłonąć wilgoć. Współczynnik prędkości: pianka PE ok. 0,78–0,88, lite PE ok. 0,66, powietrze 1,0. Kabel RG-174 jest cienki i elastyczny (pigtaile), ale stratny, więc nadaje się tylko na krótkie odcinki.
Słowa: koncentryk, kabel antenowy, RG-58, RG-174, RG-213, LMR-400, 50 omów, straty kabla, tłumienie, impedancja, współczynnik prędkości, pigtail
Źródło: https://en.wikipedia.org/wiki/Coaxial_cable (sprawdzono 2026-10; tabela przeliczona na 100 stóp, wartości dla 750 MHz, nie dla 868 MHz)

### Jak obchodzić się z koncentrykiem i złączami?
Oplot zwiera się z żyłą środkową najczęściej w źle założonym złączu, nawet jedna nitka potrafi to zrobić. Zły styk złącza albo przerwany ekran powodują wyciek sygnału do środka lub na zewnątrz. Ekran musi być porządnie połączony ze złączem. Oplot daje elastyczność, kabel z litym ekranem metalowym załamuje się przy ostrym zgięciu i wtedy ma straty. Izolacja kabla zewnętrznego z czasem się starzeje i może wymagać wymiany. Złącza powinny pasować impedancją do kabla; pozłacane są lepsze na wysokich częstotliwościach, bo posrebrzane matowieją.
Słowa: złącza, zwarcie oplotu, wyciek sygnału, zagięcie kabla, starzenie izolacji, pozłacane, posrebrzane, montaż złącza, SMA, N
Źródło: https://en.wikipedia.org/wiki/Coaxial_cable (sprawdzono 2026-10; szczegółów SMA/RP-SMA/N strona o złączach RF nie podaje)

### Jak chronić maszt i antenę przed piorunem?
Uziemienie ma mieć niski opór i niską indukcyjność; w słabej glebie dodaje się pręty, uziom otokowy lub promienie, a zbrojenie betonu też może służyć jako uziemienie. Przewód odprowadzający prowadź krótko i łagodnymi łukami (inaczej prąd przeskoczy na instalację domu), a z pobliskimi przewodzącymi obiektami połącz go wyrównawczo, by uniknąć przeskoków bocznych. Dawny "stożek ochronny 45°" jest zbyt optymistyczny dla wysokich obiektów; obecnie stosuje się metodę toczącej się kuli o promieniu ok. 46 m. Ograniczniki przepięć montuje się tam, gdzie przewody wchodzą do budynku. Nie ma gwarancji ochrony; po burzy sprawdź maszt, zwód i uziemienie.
Słowa: piorun, uziemienie, maszt, odgromnik, ogranicznik przepięć, zwód, przewód odprowadzający, burza, toczącej się kuli, ochrona odgromowa
Źródło: https://en.wikipedia.org/wiki/Lightning_rod (sprawdzono 2026-10; ogólna informacja, nie zastępuje projektu instalacji)

### Praca na wysokości: co robić przy montażu masztu?
Źródło wymienia hierarchię: najpierw eliminacja zagrożenia (najlepiej nie wchodzić wysoko, np. montaż anteny z ziemi), potem zapobieganie, zatrzymywanie upadku i środki organizacyjne. Zestaw ochrony przed upadkiem to uprząż, punkt kotwiczenia i linka samozwijająca lub lonża. Uprząż zatrzymuje upadek, a systemy "fall restraint" w ogóle nie dopuszczają do krawędzi. Pracownik powinien być przeszkolony i znać sprzęt, a stanowisko czyste i suche. Źródło nie opisuje współczynnika upadku ani ratownictwa, więc tych tematów tu nie omawiam. Przy masztach na dachach i wieżach korzystaj z fachowców i przepisów BHP.
Słowa: praca na wysokości, uprząż, lonża, kotwiczenie, upadek, BHP, maszt, dach, alpinista, zabezpieczenie, linka samozwijająca
Źródło: https://en.wikipedia.org/wiki/Fall_protection (sprawdzono 2026-10; źródło bez współczynnika upadku i ratownictwa)
