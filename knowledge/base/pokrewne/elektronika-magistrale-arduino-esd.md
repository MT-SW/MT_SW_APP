# Elektronika: Arduino, I2C, SPI, UART, rezystory podciągające i ESD

### Czym jest Arduino i jak się go programuje?
Arduino to otwarta platforma jednopłytkowych mikrokontrolerów. Większość płytek ma 8-bitowy układ AVR (Uno: ATmega328), Due ma ARM Cortex-M3. Uno ma 14 pinów cyfrowych (6 z PWM) i 6 analogowych, stabilizator 5 V i oscylator 16 MHz; programy wgrywa się przez USB, zwykle przez układ USB–UART. Nakładki (shields) dodają silniki, ekrany, Ethernet czy GPS. Środowisko Arduino IDE (Windows, macOS, Linux) kompiluje i wgrywa jednym kliknięciem; IDE 2.0 jest stabilne od września 2022; jest też arduino-cli. Programy to C/C++ z funkcjami setup() (raz) i loop() (w pętli), zapisywane jako .ino. Napięć logicznych 5 V i 3,3 V strona nie porównuje, więc sprawdzaj kartę katalogową płytki, zwłaszcza przy modułach LoRa (3,3 V).
Słowa: Arduino, Uno, ATmega328, Arduino IDE, sketch, setup, loop, shield, mikrokontroler, 5 V, 3,3 V, arduino-cli
Źródło: https://en.wikipedia.org/wiki/Arduino (sprawdzono 2026-10; uwaga o module LoRa: wiedza ogólna)

### I2C: dwa przewody, adresy i rezystory podciągające
I2C używa dwóch dwukierunkowych linii: SDA (dane) i SCL (zegar), bez osobnego wyboru układu dla każdego urządzenia. Adres jest 7-bitowy (rzadko 10-bitowy), część adresów zarezerwowana; zdarzają się kolizje, więc układy mają piny wyboru adresu, a ten sam adres mogą mieć niezgodne układy. Linie są z otwartym drenem i wymagają rezystorów podciągających; urządzenie zwiera linię do masy, by wysłać 0. Prędkości: 100 kbit/s (standard), 400 (fast), 1 Mbit/s (fast-plus), 3,4 i 5 Mbit/s. Limit pojemności magistrali 400 pF ogranicza długość do kilku metrów, a uszkodzone urządzenie trzymające SDA lub SCL na dole potrafi zawiesić magistralę.
Słowa: I2C, SDA, SCL, adres I2C, pull-up, 400 kHz, czujnik I2C, magistrala, kolizja adresów, zawieszenie magistrali
Źródło: https://en.wikipedia.org/wiki/I%C2%B2C (sprawdzono 2026-10)

### SPI i różnice względem I2C
SPI ma cztery sygnały: MOSI (master do slave), MISO (slave do master), SCK (zegar od mastera) i CS/SS (aktywny niski wybór układu; w układzie wielu urządzeń po jednym CS na slave). Tryby 0–3 wynikają z polaryzacji (CPOL) i fazy (CPHA) zegara; master i slave muszą się zgadzać co do trybu. Protokół nie ustala maksymalnej szybkości. SPI jest szybszy od I2C (pełny dupleks), zwykle zużywa mniej prądu (bez rezystorów podciągających), ale ma więcej przewodów i nie ma wbudowanych potwierdzeń ani kontroli błędów. Moduły LoRa (SX126x) zwykle łączy się z mikrokontrolerem właśnie przez SPI.
Słowa: SPI, MOSI, MISO, SCK, CS, tryb SPI, CPOL, CPHA, magistrala, pełny dupleks, SX1262 SPI
Źródło: https://en.wikipedia.org/wiki/Serial_Peripheral_Interface (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### UART: baud, ramka 8N1 i poziomy logiczne
UART wysyła bity szeregowo; prędkość (baud) musi być taka sama po obu stronach. Najczęstsza ramka 8N1: bit startowy, 8 bitów danych, bez parzystości, 1 bit stopu, czyli 10 bitów na znak (sprawność 80%; liczba znaków na sekundę to prędkość/10). Linia w spoczynku ma stan wysoki. Dwa UART-y dobrze współpracują przy tym samym napięciu (3 V do 3 V); przy różnych potrzeba przesuwnika poziomów. Sam UART daje poziomy logiczne, a RS-232 to zewnętrzny standard z innymi napięciami; surowy TTL nadaje się do krótkich łączy diagnostycznych. Przy łączeniu dwóch urządzeń krzyżuj linie: TX jednego do RX drugiego (wiedza ogólna, zgodna z opisem modułu Serial).
Słowa: UART, baud, 8N1, TX, RX, poziomy logiczne, RS-232, TTL, adapter USB-UART, przesuwnik poziomów
Źródło: https://en.wikipedia.org/wiki/UART (sprawdzono 2026-10)

### Rezystory podciągające i ściągające: po co i jakie wartości?
Ustalają znany poziom logiczny na linii, gdy nic jej nie napędza. Bez nich wejście "pływa" i szum lub upływności dają losowe stany. Pull-up ciągnie do plusa zasilania, pull-down do masy. Typowo dla logiki bipolarnej 5 V ok. 1–5 kΩ; w CMOS wolno więcej. Mały opór daje szybkie zbocza, ale marnuje prąd; duży oszczędza, ale spowalnia sygnał (z pojemnością tworzy filtr dolnoprzepustowy). W TTL preferuje się pull-up, w CMOS pull-down jest bezpieczny. Przy czujnikach na I2C zwykle potrzebne są pull-upy na SDA i SCL (często już na płytce modułu). Wykorzystanie: sygnały konfiguracyjne zworką, szyny z otwartym kolektorem.
Słowa: pull-up, pull-down, rezystor podciągający, wejście pływające, stan logiczny, INPUT_PULLUP, przycisk, otwarty dren
Źródło: https://en.wikipedia.org/wiki/Pull-up_resistor (sprawdzono 2026-10; zdanie o I2C: wiedza ogólna)

### ESD: jak nie zniszczyć elektroniki ładunkiem statycznym
Wyładowanie elektrostatyczne może trwale uszkodzić układy scalone nawet przy około 30 V, a skutek bywa ukryty i skraca życie urządzenia. Ładunek powstaje przez tarcie (tribo) lub indukcję od izolatorów, jak plastikowe worki czy styropianowe kubki. Środki: uziemione stanowisko, opaska na nadgarstek, mata antystatyczna, kontrola wilgotności, trzymanie izolatorów, które mogą się naładować powyżej 2000 V, co najmniej 30 cm od układów, jonizatory. Przechowuj i wysyłaj elementy w antystatycznych woreczkach (działają jak klatka Faradaya). Przed dotknięciem płytki dotknij uziemionego metalu.
Słowa: ESD, wyładowanie elektrostatyczne, antystatyczna opaska, mata ESD, woreczek antystatyczny, wilgotność, uszkodzenie układu, płytka
Źródło: https://en.wikipedia.org/wiki/Electrostatic_discharge (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)
