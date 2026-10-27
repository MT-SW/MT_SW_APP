# Typy anten, balun, SDR, VNA, LNA i filtry SAW

### Dipol półfalowy: długość i parametry
Dipol półfalowy ma długość około λ/2, skróconą współczynnikiem k (ok. 0,94–0,98 zależnie od grubości przewodnika); dla k≈0,95 całkowita długość wynosi ok. 143/f metrów (f w MHz). Dla 869,44 MHz (nasza sieć) to wyliczone ok. 16,4 cm w sumie, czyli po ok. 8,2 cm na ramię. Zysk 2,15 dBi (0 dBd), wzór kierunkowy jak obwarzanek: dookólny w płaszczyźnie prostopadłej do przewodu, zera wzdłuż osi. Impedancja ok. 65–70 Ω w praktyce (73 Ω teoretycznie), pasmo wąskie. Polaryzacja zależy od ustawienia: pionowy dipol daje polaryzację pionową. Zasilanie w środku, a kabel koncentryczny wymaga baluna.
Słowa: dipol, dipol półfalowy, długość anteny, 143/f, 868 MHz antena, zysk 2,15 dBi, impedancja 73, ramię anteny, DIY antena
Źródło: https://en.wikipedia.org/wiki/Dipole_antenna (sprawdzono 2026-10; wartości dla 869,44 MHz: obliczenie własne)

### Antena z płaszczyzną odniesienia (ground plane) i monopol ćwierćfalowy
Monopol ćwierćfalowy nad płaszczyzną uziemienia zachowuje się jak dipol, bo jego odbicie tworzy drugą połowę. Płaszczyzna ma mieć promień co najmniej ¼λ, być dobrze przewodząca (jej rezystancja szeregowo z anteną marnuje moc) i nie musi być ciągła: zamiast blachy mogą być przewody ¼λ rozchodzące się od podstawy (radiale). Źródło nie podaje kąta radiali ani impedancji. Dla 869 MHz ćwierć fali w wolnej przestrzeni to ok. 8,6 cm (po skróceniu ok. 8,2 cm), czyli dlatego anteny gumowe mają kilka centymetrów. Ciało telefonu lub obudowa węzła działają częściowo jak płaszczyzna.
Słowa: ground plane, monopol, ćwierćfalowa, radiale, płaszczyzna uziemienia, antena gumowa, GP antena, ćwierć fali, przewód zerowy
Źródło: https://en.wikipedia.org/wiki/Ground_plane (sprawdzono 2026-10; wartości w cm: obliczenie własne; ostatnie zdanie: wiedza ogólna)

### Antena kolinearna: zysk pionowy dookólny
To stos dipoli lub elementów ćwierćfalowych na wspólnej osi, o wysokim zysku i dookólnej charakterystyce w poziomie, polaryzacja pionowa. Układanie elementów skupia moc w stronę horyzontu zamiast w górę lub w ziemię; teoretycznie podwojenie liczby elementów daje +3 dB, w praktyce mniej przez straty. Typowa konstrukcja używa odcinków kabla koncentrycznego z przemiennym fazowaniem i radomem z włókna szklanego. Wersja z cewkami fazującymi jest samonośna i bez radomu, ale mniej sprawna. Stosuje się je jako anteny bazowe łączności ruchomej. Idealne do przekaźnika z szerokim pokryciem poziomym.
Słowa: kolinearna, antena kolinearna, zysk 5 dBi, 8 dBi, antena bazowa, radom, dookólna, antena na maszt, przekaźnik, fazowanie
Źródło: https://en.wikipedia.org/wiki/Collinear_antenna_array (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### Antena patch (płaska) i antena PIFA
Patch to niskoprofilowa antena łatwa do wykonania na płytce drukowanej, długości około połowy fali (skracana przez efekt brzegowy); praktyczna głównie na mikrofalach, gdzie jest wygodnie mała. Wiele patchy tworzy szyk o dużym zysku, a szyk fazowany może sterować wiązką. Wariant PIFA (planarna odwrócona F) jest powszechny w telefonach i ma lepsze dopasowanie. Źródło nie podaje zysku ani szerokości wiązki pojedynczego patcha; w urządzeniach LoRa 868/869 MHz częściej spotyka się pręty, anteny helikalne i dipole, a patche w antenach kierunkowych panelowych (wiedza ogólna).
Słowa: patch, antena płaska, PIFA, antena PCB, antena panelowa, mikrofale, szyk anten, antena kierunkowa
Źródło: https://en.wikipedia.org/wiki/Patch_antenna (sprawdzono 2026-10; ostatnie zdanie: wniosek ogólny)

### Balun i dławik: po co przy antenie dipolowej?
Balun łączy symetryczną antenę (dipol) z niesymetrycznym kablem koncentrycznym, nie zakłócając impedancji, i może ją transformować. Bez niego ekran kabla zaczyna promieniować prądem współbieżnym, który zniekształca wzór promieniowania i pomiary. Baluny napięciowe (transformatorowe, autotransformatorowe) i prądowe (liniowe, dławikowe). Zamiast baluna można nawinąć kabel koncentryczny w kilka zwojów przy punkcie zasilania lub nasunąć ferrytowy toroid: efekt zbliżony do baluna prądowego 1:1. Klasyczne baluny magnetyczne sięgają około 1 GHz, bo przenikalność ferrytu spada powyżej kilku MHz.
Słowa: balun, dławik ferrytowy, prąd współbieżny, ferryt, koncentryk, dipol zasilanie, choke, symetryzator, ekran kabla
Źródło: https://en.wikipedia.org/wiki/Balun (sprawdzono 2026-10)

### SDR i tani odbiornik RTL-SDR
Radio programowe (SDR) wykonuje mieszanie, filtrowanie i demodulację w oprogramowaniu; jedno urządzenie obsłuży wiele protokołów. RTL-SDR to tanie odbiorniki na układzie Realtek RTL2832U, pierwotnie do telewizji DVB-T (w 2010 odkryto surowe dane); sterowniki rozwijał projekt Osmocom. Wersje dedykowane z lepszym ekranowaniem kosztują ok. 30 USD (2025). Zakres to w przybliżeniu 24–1766 MHz przy paśmie do 3,2 MHz. Zastosowania: radio FM, ADS-B i ACARS (samoloty), sondy meteorologiczne, satelity pogodowe, radioastronomia amatorska, tryby krótkofalarskie. Do odbioru 868 MHz nadaje się zakresem, więc można obejrzeć zajętość pasma lub sprawdzić, czy węzeł nadaje; to jednak odbiornik, nie narzędzie do dekodowania szyfrowanych wiadomości Meshtastic.
Słowa: SDR, RTL-SDR, RTL2832U, odbiornik, widmo, 868 MHz podgląd, ADS-B, Osmocom, dongle USB, analiza pasma
Źródło: https://en.wikipedia.org/wiki/Software-defined_radio (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### Analizator wektorowy (VNA, NanoVNA): do czego służy?
VNA mierzy parametry S urządzeń radiowych (amplitudę i fazę); S11 to odbicie na porcie, z którego wynika SWR i impedancja. Kalibracja SOLT (short, open, load, through) z wzorcami 50 Ω koryguje błędy układu pomiarowego. Tanie, amatorskie VNA poniżej 100 USD mają mniej funkcji, ale wystarczą do nauki i hobby do kilku GHz. Źródło nie opisuje wprost strojenia anten ani nazwy NanoVNA w tekście; w praktyce hobbyści używają takiego urządzenia do sprawdzania rezonansu i SWR własnych anten na 868/869 MHz (wiedza ogólna).
Słowa: VNA, NanoVNA, S11, SWR pomiar, kalibracja SOLT, strojenie anteny, analizator anten, rezonans, impedancja
Źródło: https://en.wikipedia.org/wiki/Vector_network_analyzer (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### LNA: wzmacniacz niskoszumny i jego miejsce
Liczy się współczynnik szumów (NF): typowy LNA ma ok. 3 dB, dobry ok. 1 dB, a wzmocnienie typowo ok. 20 dB. Montuje się go przy antenie, bo straty kabla pogarszają stosunek sygnału do szumu (przykład z GPS: 10 stóp RG-174 to ok. 5 dB straty). W łańcuchu odbiorczym o szumach decydują pierwsze stopnie (wzór Friisa). Ryzyka: własny szum LNA, zła polaryzacja, zniekształcenia intermodulacyjne przy silnych sygnałach (zwłaszcza blisko silnych nadajników), niestabilność. Za duże wzmocnienie przy silnym sygnale przekracza zakres dynamiczny. W sieci z wieloma blisko położonymi nadajnikami LNA bez filtra może pogorszyć odbiór (wiedza ogólna).
Słowa: LNA, wzmacniacz niskoszumny, współczynnik szumów, NF, Friis, wzmacniacz antenowy, intermodulacja, czułość odbiornika, filtr
Źródło: https://en.wikipedia.org/wiki/Low-noise_amplifier (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### Filtry SAW: po co w odbiorniku radiowym?
Filtry z powierzchniową falą akustyczną mają wąskie, dokładnie określone pasma przepustowe i przewyższają filtry kwarcowe, LC i falowodowe wydajnością, ceną i rozmiarem poniżej ok. 1,5–2,5 GHz (powyżej stosuje się rezonatory cienkowarstwowe). Wysoka dobroć pozwala silnie tłumić sygnały zakłócające. Na 868/869 MHz mieszczą się w zakresie, w którym są korzystne, ale źródło nie podaje konkretnych zastosowań ani strat wtrąceniowych. Tłumienie wtrąceniowe jest zwykle zauważalne (kilka dB, wiedza ogólna), więc filtr przed wzmacniaczem kosztuje trochę czułości.
Słowa: filtr SAW, filtr pasmowy, tłumienie wtrąceniowe, selektywność, 868 MHz, zakłócenia, GSM 900, odbiornik, filtr dla LoRa
Źródło: https://en.wikipedia.org/wiki/Surface_acoustic_wave (sprawdzono 2026-10; część o stratach: wiedza ogólna)
