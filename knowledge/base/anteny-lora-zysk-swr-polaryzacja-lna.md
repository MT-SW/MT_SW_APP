# LoRa i anteny: modulacja, zysk dBi/dBd, SWR, polaryzacja, LNA, cykl pracy

### LoRa: modulacja chirp, SF i pasma
LoRa używa własnej modulacji pokrewnej z rozpraszaniem widma typu chirp (CSS): każdy symbol to cyklicznie przesunięty chirp w paśmie wokół częstotliwości bazowej. Współczynnik rozproszenia (SF) wybiera się od 5 do 12: wyższy SF daje lepszą czułość, ale niższą szybkość i dłuższy czas nadawania (więcej energii). Szybkości mieszczą się w 0,3 do 27 kbit/s. Budżet łącza LoRa to około 155 do 170 dB, typowe modemy nadają do +22 dBm, a większa moc podnosi budżet kosztem energii. Pasma bez licencji poniżej 1 GHz: EU433 (433,050 do 434,790 MHz), EU868 (863 do 870/873 MHz), US915 (902 do 928), IN865 (865 do 867), AS923 (915 do 928 MHz). Szerokości pasma źródło nie podaje. Nasza sieć używa 869,44165 MHz w EU868 z presetem Narrow Fast.
Słowa: LoRa, CSS, chirp, spreading factor, SF, czułość, szybkość transmisji, EU868, pasma LoRa, budżet łącza 155 dB
Źródło: https://en.wikipedia.org/wiki/LoRa (sprawdzono 2026-10)

### Zysk anteny: dBi, dBd i sprawność
dBi odnosi zysk do idealnego źródła izotropowego (podstawowa definicja), dBd do półfalowego dipola. Bezstratny dipol ma około 2,15 dBi, więc dBd ≈ dBi minus 2,15. Przykład ze źródła: zysk liniowy 5 to 7 dBi, a względem dipola 5/1,64 ≈ 3,05, czyli około 4,84 dBd. Gdy producent podaje samo „dB”, sprawdź drobny druk, jaki to odnośnik. Zysk to kierunkowość razy sprawność promieniowania; kierunkowość ignoruje straty. Zysk rzeczywisty (realized) uwzględnia niedopasowanie impedancji, więc jest zawsze mniejszy. Zysk mówi, jak dobrze antena skupia moc w wybranym kierunku (nadawanie) i jak dobrze zbiera sygnał z tego kierunku (odbiór); zysk odbiorczy równa się nadawczemu (wzajemność). Antena o większym zysku to nie „więcej mocy”, tylko zawężona wiązka.
Słowa: zysk anteny, dBi, dBd, 2.15 dB, kierunkowość, sprawność anteny, realized gain, wzajemność, dipol
Źródło: https://en.wikipedia.org/wiki/Antenna_gain (sprawdzono 2026-10)

### SWR a straty na kablu zależne od częstotliwości
SWR (współczynnik fali stojącej) mówi, jak dobrze obciążenie dopasowane jest do impedancji falowodu; dobry jest bliski 1:1, a nieskończony oznacza całkowite odbicie mocy. SWR odpowiada modułowi współczynnika odbicia, a odbita moc zwiększa straty na kablu. Źródło nie podaje progu „dobrego” SWR ani nie wyjaśnia tłumienia odbicia. Ważne: znaczenie niskiego SWR rośnie z częstotliwością. Przykład: niedopasowanie 6:1 przez 75 m kabla RG-8A daje około 2,2 dB strat przy 3,5 MHz, ale około 10,8 dB przy 146 MHz. Dlatego na UHF/868 MHz lepiej utrzymać krótki kabel i dobre dopasowanie. Termin PSWR (kwadrat VSWR) jest przestarzały.
Słowa: SWR, VSWR, współczynnik fali stojącej, dopasowanie anteny, straty w kablu, 1:1, odbicie, RG-8A
Źródło: https://en.wikipedia.org/wiki/Standing_wave_ratio (sprawdzono 2026-10)

### Polaryzacja anten: pionowa, pozioma i kołowa
Zdecydowana większość anten jest spolaryzowana liniowo. Pozioma Yagi na dachu daje polaryzację poziomą, pionowy bat (whip) pionową. Niedopasowanie polaryzacji liniowych (pozioma antena odbierająca sygnał pionowy) znacząco osłabia sygnał. Polaryzacja kołowa powstaje przy dwóch prostopadłych składowych o równej amplitudzie i przesunięciu fazowym 90°; antena turnstile w trybie osiowym promieniuje kołowo. Satelitarna telewizja wykorzystuje przeciwne polaryzacje na tej samej częstotliwości. Źródło nie podaje wartości 20 do 30 dB dla krzyżowej polaryzacji ani wzoru na stratę (w praktyce ustawia się anteny obu stron w tej samej polaryzacji, zwykle pionowej przy węzłach z antenami prętowymi; to zasada ogólna). Filtrowanie jednej składowej fali kołowej prawie nic nie zmienia.
Słowa: polaryzacja, pionowa, pozioma, kołowa, niedopasowanie polaryzacji, antena prętowa, Yagi, whip, turnstile
Źródło: https://en.wikipedia.org/wiki/Polarization_(waves) (sprawdzono 2026-10)

### LNA: kiedy wzmacniacz odbioru pomaga, a kiedy szkodzi
LNA wzmacnia bardzo słabe sygnały, dodając jak najmniej szumu, by zachować stosunek sygnału do szumu. Współczynnik szumów (NF): 3 dB typowy, 1 dB dobry; im niższy, tym lepszy odbiór. Straty linii zasilającej pogarszają SNR o tyle dB, ile wynoszą (3 dB strat to 3 dB gorszy SNR), więc LNA stawia się przy antenie. Przykład: 10 stóp RG-174 traci około 5 dB na częstotliwościach GPS. Typowe wzmocnienie to około 100 razy (20 dB). Większe wzmocnienie wzmacnia też silne sygnały, które mogą przeciążyć wzmacniacz i dać zniekształcenia harmoniczne i intermodulacyjne, więc liczy się liniowość. Wzory Friisa pokazują, że łączny szum odbiornika zależy głównie od pierwszych stopni, dlatego LNA daje największy zysk na początku toru. Przy silnych zakłóceniach z pasma (np. od sieci komórkowych) filtr przed LNA jest ważniejszy niż samo wzmocnienie (wniosek z opisu przeciążenia).
Słowa: LNA, wzmacniacz niskoszumowy, współczynnik szumów, noise figure, Friis, przeciążenie odbiornika, RG-174, GPS LNA
Źródło: https://en.wikipedia.org/wiki/Low-noise_amplifier (sprawdzono 2026-10)

### Cykl pracy (duty cycle) w ogóle i w radiu
Cykl pracy to ułamek okresu, w którym sygnał lub urządzenie jest aktywne, wyrażany w procentach lub jako stosunek: czas aktywny przez cały okres. Przykłady ze źródła: 60% to włączone 60% czasu, wzór 10101010 to 50%, 10001000 to 25%, silnik pracujący 1 s na 100 s to 1%. Strona nie mówi o nadajnikach radiowych. W radiu LoRa chodzi o to samo: suma czasu antenowego nadajnika w godzinie podzielona przez godzinę; konkretne limity (np. 1% lub 10% w EU868) zależą od podpasma i przepisów, więc sprawdź wpis o pasmach i licencjach oraz ustawienie Override Duty Cycle (łączenie z radiem to wiedza ogólna, nie ze źródła).
Słowa: duty cycle, cykl pracy, wypełnienie, czas antenowy, godzinny limit, procent czasu nadawania, PWM
Źródło: https://en.wikipedia.org/wiki/Duty_cycle (sprawdzono 2026-10)
