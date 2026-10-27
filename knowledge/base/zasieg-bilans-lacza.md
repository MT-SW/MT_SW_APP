# Zasięg i bilans łącza: wzory, straty, tłumienie przeszkód

### Jak policzyć bilans łącza (link budget)?
Moc odbierana (dBm) = moc nadajnika + zysk anteny nadawczej − straty kabla nadawczego − tłumienie trasy + zysk anteny odbiorczej − straty kabla odbiorczego. Zapas łącza (margin) = moc odbierana − czułość odbiornika. Maksymalne dopuszczalne tłumienie trasy = moc nadajnika + zyski anten − straty kabli − czułość odbiornika − zapas na zaniki (fade margin). Wszystko w decybelach: zyski dodajemy, straty odejmujemy. Przykład ostrzeżenia: zapas 1 dB (odbiór −138 dBm przy czułości −137 dBm) to za mało do niezawodnej pracy.
Słowa: link budget, bilans łącza, zapas, margin, fade margin, czułość, dBm, dB, obliczenia zasięgu, wzór
Źródło: https://hubble.com/community/guides/how-to-calculate-lorawan-link-budget/ (sprawdzono 2026-10)

### Jaki jest wzór na tłumienie w wolnej przestrzeni (FSPL)?
FSPL (dB) = 20·log10(d) + 20·log10(f) − 147,55, gdzie d w metrach, f w hercach. Dla 868 MHz przy 1 km wychodzi ok. 91,2 dB (artykuł podaje 91,5 dB). Każde podwojenie odległości w wolnej przestrzeni dodaje ok. 6 dB straty. Budżet 150 dB odpowiada w idealnych warunkach ok. 800 km, rekord LoRa to 702 km (realny zasięg jest znacznie mniejszy przez teren i przeszkody).
Słowa: FSPL, tłumienie wolnej przestrzeni, 6 dB, podwojenie odległości, 868 MHz, 91 dB, rekord zasięgu, 702 km, propagacja
Źródło: https://hubble.com/community/guides/how-to-calculate-lorawan-link-budget/ oraz https://smartmakers.io/en/lorawan-range-part-1-the-most-important-factors-for-a-good-lorawan-signal-range/ (sprawdzono 2026-10)

### Ile tracą kable, złącza i ile zyskują anteny?
Straty kabli na metr: RG-58 ok. 0,2 dB/m, LMR-240 ok. 0,12 dB/m, LMR-400 ok. 0,07 dB/m; każde złącze dodaje 0,1–0,5 dB (przykład: 5 m RG-58 z dwoma złączami N to ok. 1,5 dB). Zysk anten wg typu: chip −2 do 0 dBi, ścieżka na PCB 0–2 dBi, bat lub krótka antena 2–3 dBi, zewnętrzne dookólne 3–8 dBi. Zalecany jest kabel o małych stratach (LMR-400 lub odpowiednik, poniżej 1,5 dB na 100 m wg drugiego źródła), dobre złącza typu N i jak najkrótszy odcinek do anteny.
Słowa: kabel, RG-58, LMR-400, LMR-240, straty kabla, złącze N, zysk anteny dBi, chip antena, pigtail, dB na metr
Źródło: https://hubble.com/community/guides/how-to-calculate-lorawan-link-budget/ oraz https://smartmakers.io/en/lorawan-range-part-1-the-most-important-factors-for-a-good-lorawan-signal-range/ (sprawdzono 2026-10)

### Ile tłumią ściany i materiały budowlane?
Orientacyjne straty (dB): szkło 6 mm 0,8, szkło 13 mm 2; drewno 76 mm 2,8; cegła 89 mm 3,5, 178 mm 5, 267 mm 7; beton 102 mm 12, 203 mm 23, 305 mm 35; ściana kamienna 203 mm 12, 406 mm 17, 610 mm 28; cegła-beton 192 mm 14; żelbet 89 mm 27. Antena na zewnątrz jest zawsze lepsza niż w środku, a montaż na maszcie lepszy niż na ścianie budynku.
Słowa: tłumienie ścian, beton, cegła, szkło, żelbet, wewnątrz budynku, przenikanie, tłumienie przeszkód, antena w oknie
Źródło: https://smartmakers.io/en/lorawan-range-part-1-the-most-important-factors-for-a-good-lorawan-signal-range/ (sprawdzono 2026-10)

### Czym jest strefa Fresnela i wysokość anteny?
Przedmioty w pierwszej strefie Fresnela osłabiają sygnał i skracają zasięg nawet przy widoczności optycznej. Podniesienie anten poprawia widoczność, ale źródło nie podaje konkretnych wartości wysokości ani zapasu strefy. Odbicia od ziemi i obiektów oraz zakłócenia od stacji GSM/UMTS także wpływają na zasięg.
Słowa: Fresnel, strefa Fresnela, wysokość anteny, LOS, widoczność optyczna, odbicia, maszt, teren
Źródło: https://smartmakers.io/en/lorawan-range-part-1-the-most-important-factors-for-a-good-lorawan-signal-range/ (sprawdzono 2026-10)

### Jak spreading factor wpływa na szybkość i zasięg?
LoRaWAN używa SF7–SF12. SF9 nadaje czterokrotnie wolniej niż SF7. Wyższy SF daje więcej energii na każdy zestaw danych, co zwiększa zasięg kosztem szybkości i czasu antenowego. Przykład źródła: SF12, SNR −20 dB, szerokość 125 kHz, współczynnik szumów 6 dB daje czułość −137 dBm. Meshtastic ma własne presety (np. Long Fast to SF11 przy 250 kHz).
Słowa: spreading factor, SF, SF7, SF12, czułość, SNR -20, kompromis, zasięg a szybkość
Źródło: https://smartmakers.io/en/lorawan-range-part-1-the-most-important-factors-for-a-good-lorawan-signal-range/ (sprawdzono 2026-10)

### Jaki jest limit mocy w pasmie 868 MHz w Europie?
Według artykułu o bilansie łącza: EU868 zwykle 14 dBm ERP (ok. 16 dBm EIRP) na większości kanałów, w niektórych podpasmach do 27 dBm EIRP; dla porównania USA 915: 30 dBm przewodzone lub 36 dBm EIRP z anteną 6 dBi. Dla naszego zakresu 869,4–869,65 MHz fakty projektu MT_SW podają do 500 mW ERP (27 dBm) i 10 % czasu nadawania, zgodnie z dokumentacją Meshtastic dla EU_868 i EU_N_868. Układ SX1276 daje do +20 dBm przewodzone. Zasada ogólna (nie ze źródła powyżej): ERP uwzględnia zysk anteny, więc przy antenie o wyższym zysku moc nadajnika trzeba odpowiednio zmniejszyć.
Słowa: limit mocy, 14 dBm, 27 dBm, 500 mW, ERP, EIRP, przepisy, legalna moc, 10 procent, 869.4
Źródło: https://hubble.com/community/guides/how-to-calculate-lorawan-link-budget/ oraz https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10; akty prawne wymagają potwierdzenia w aktualnym rozporządzeniu)
