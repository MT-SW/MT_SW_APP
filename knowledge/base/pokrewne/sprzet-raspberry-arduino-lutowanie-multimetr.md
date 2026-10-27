# Raspberry Pi, Arduino, lutowanie, multimetr

### Raspberry Pi: modele, zasilanie, GPIO, karta
Zero 2 W (2021): czterordzeniowy Cortex-A53 1 GHz, 512 MB RAM, wbudowane Wi-Fi. Pi 3 B: 1,2 GHz, 1 GB; Pi 4: Cortex-A72 1,5 GHz, 1 do 8 GB, gigabit i USB 3.0; Pi 5 (2023): Cortex-A76 2,4 GHz, 1 do 16 GB, PCIe i zegar RTC, obsługuje dyski NVMe. Pico to mikrokontroler (Cortex-M0+ 133 MHz, 264 KB RAM, 2 MB flash), nie uruchamia Linuksa; Pico 2 ma RP2350. Piny GPIO mają logikę 3,3 V; źródło nie mówi, czy znoszą 5 V, więc przyjmij 3,3 V. Zasilanie: starsze micro-USB (zasilacz 12,5 W), Pi 4 USB-C 15 W, Pi 5 USB-C 27 W; może też iść przez piny 5 V albo PoE HAT. Przy dużym obciążeniu potrzebny radiator lub wentylator, a przy przegrzaniu lub zbyt niskim napięciu moc jest dławiona. Zwykle używa się karty microSD (A2, C10, UHS-I). Bez monitora Wi-Fi bywa niestabilne z powodu domyślnego oszczędzania energii.
Słowa: Raspberry Pi, Pi 5, Pi Zero 2 W, Pico, GPIO 3.3V, microSD, NVMe, zasilacz, PoE HAT, meshtasticd
Źródło: https://en.wikipedia.org/wiki/Raspberry_Pi (sprawdzono 2026-10)

### Arduino: płytki, IDE, szkice
Arduino to otwarty projekt płytek z mikrokontrolerami (obecnie należy do Qualcomm). Większość oficjalnych płytek ma 8-bitowe układy AVR (ATmega328 itd.); Due ma ARM Cortex-M3, GIGA R1 WiFi dwa rdzenie M7 i M4. Uno, Diecimila i Duemilanove mają 14 pinów cyfrowych (6 z PWM) i 6 analogowych wejść. Środowisko Arduino IDE działa na Windows, macOS i Linuksie (wersja 2.x ma menedżer płytek i bibliotek, plotter szeregowy i debugowanie), kod wgrywa avrdude; pliki to .ino, a każdy szkic ma setup() uruchamiane raz i loop() powtarzane. Jest też arduino-cli. Shieldy to nakładki dodające silniki, ekrany, Ethernet czy GPS. Źródło nie porównuje logiki 5 V i 3,3 V; napięcie logiczne sprawdź w karcie danych płytki, zanim podłączysz czujniki.
Słowa: Arduino, Uno, Nano, IDE, szkic, setup loop, AVR, shield, arduino-cli, avrdude, logika 5V 3.3V
Źródło: https://en.wikipedia.org/wiki/Arduino (sprawdzono 2026-10)

### Lutowanie: rodzaje cyny, temperatury, topnik
Cyna ołowiowa 63/37 jest eutektyczna i topi się w jednym punkcie 183 °C; 60/40 topi się w zakresie 183 do 190 °C. Bezołowiowa SAC (cyna-srebro-miedź) topi się około 217 do 220 °C, a że nie jest eutektyczna, trudniej o pewne połączenie. Bezołowiowa jest wskazana tam, gdzie dziecko może wziąć coś do ust, przy wodzie pitnej i na zewnątrz. Zbyt duża temperatura uszkadza elementy i płytkę lub spala topnik, zbyt mała daje słabe luty. Topnik zapobiega utlenianiu i pomaga cynie rozpływać się; w elektronice stosuje się kalafonię (R, RMA, RA; pozostałości RA są żrące i trzeba je zmyć), no-clean lub topnik rozpuszczalny w wodzie. Topnik kwasowy do hydrauliki jest przewodzący i żrący, nie nadaje się do elektroniki. Opary są szkodliwe nawet przy cynie bezołowiowej, więc wietrz.
Słowa: lutowanie, cyna, 63/37, SAC, bezołowiowa, topnik, kalafonia, lutownica, temperatura lutowania, opary
Źródło: https://en.wikipedia.org/wiki/Soldering (sprawdzono 2026-10)

### Technika lutowania, zimny lut i rozlutowywanie
Kroki: oczyść powierzchnie do gołego metalu, dopasuj połączenie i nałóż topnik, grzej elementy (nie cynę), przyłóż cynę do rozgrzanego miejsca, zdejmij ciepło i nie ruszaj, aż lut zastygnie. Zimny lut powstaje, gdy części nie osiągnęły temperatury topnienia (np. lutownica grzała cynę, nie element); jest matowy, spękany lub w grudkach i może przewodzić z przerwami lub zawieść po latach. Lut „suchy” powstaje, gdy cyna poruszy się podczas stygnięcia: wygląda matowo i ziarniście, jest słaby. Rozlutowywanie: najpierw usuń starą cynę, użyj plecionki (zawiera topnik) lub odsysacza; zużytej cyny nie używaj ponownie, bo zawiera rozpuszczony metal bazowy i daje kruche połączenia.
Słowa: zimny lut, suchy lut, rozlutowywanie, plecionka, odsysacz, technika lutowania, połączenie lutowane
Źródło: https://en.wikipedia.org/wiki/Soldering (sprawdzono 2026-10)

### Multimetr: pomiary, ciągłość, kategorie CAT
Multimetr działa jako woltomierz, amperomierz lub omomierz zależnie od funkcji. Prąd mierzy się przez mały bocznik, co daje niewielki spadek napięcia (burden voltage) ważny przy bardzo niskich napięciach. Pomiar AC w prostych miernikach jest dokładny tylko dla sinusoidy, a True-RMS radzi sobie z innymi kształtami w granicach współczynnika szczytu. Opór mierz po odłączeniu elementu od układu, bo równoległe ścieżki zakłamują wynik. Dźwięk ciągłości jest przybliżony, bo próg zależy od miernika. Kategorie IEC 61010: CAT I (poza siecią), CAT II (obwody końcowe jednofazowe), CAT III (rozdzielnie, instalacje stałe), CAT IV (przyłącza, bardzo duże prądy zwarciowe). Typowe błędy: miernik w trybie oporu lub prądu podłączony do źródła napięcia (niezabezpieczony często ginie, z bezpiecznikiem zwykle przeżywa), pomiar w układzie pod napięciem i ignorowanie oporu przewodów pomiarowych.
Słowa: multimetr, woltomierz, amperomierz, ciągłość, CAT III, True-RMS, bezpiecznik miernika, pomiar prądu, błędy pomiaru
Źródło: https://en.wikipedia.org/wiki/Multimeter (sprawdzono 2026-10)
