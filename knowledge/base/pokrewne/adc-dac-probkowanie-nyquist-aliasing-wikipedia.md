### Co robi przetwornik ADC według Wikipedii?
ADC zamienia sygnał analogowy, np. dźwięk z mikrofonu albo światło w aparacie cyfrowym, na sygnał cyfrowy.

Słowa: ADC, przetwornik analogowo-cyfrowy, sygnał analogowy, cyfrowy, mikrofon, dźwięk
Źródło: https://en.wikipedia.org/wiki/Analog-to-digital_converter (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie parametry ma przetwornik ADC według Wikipedii?
Rozdzielczość, czyli liczba różnych wartości dyskretnych w zakresie wejściowym, zwykle wyrażana w bitach. Częstotliwość próbkowania, czyli tempo pobierania nowych wartości, która w dużym stopniu wyznacza pasmo ADC. Napięcie odniesienia, z którego wynika rozdzielczość napięciowa jako zakres pełnej skali podzielony przez liczbę przedziałów.

Słowa: ADC, rozdzielczość, bity, częstotliwość próbkowania, napięcie odniesienia, pasmo
Źródło: https://en.wikipedia.org/wiki/Analog-to-digital_converter (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co to jest kwantyzacja w ADC według Wikipedii?
Konwersja obejmuje kwantyzację wejścia, która wprowadza niewielki błąd kwantyzacji. Strona opisuje go jako błąd zaokrąglenia między napięciem wejściowym a zcyfryzowaną wartością. Rozdzielczość napięciowa (LSB) równa się zakresowi pełnej skali podzielonemu przez 2 do potęgi M, gdzie M to liczba bitów. Przykład: zakres 0–1 V przy 3 bitach daje 0,125 V.

Słowa: ADC, kwantyzacja, błąd zaokrąglenia, LSB, 2^M, 0,125 V, zakres 0–1 V, 3 bity
Źródło: https://en.wikipedia.org/wiki/Analog-to-digital_converter (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie jest twierdzenie o próbkowaniu według Wikipedii?
Sygnał bez częstotliwości powyżej B herców można w pełni opisać próbkami pobieranymi w odstępach krótszych niż 1/(2B) sekundy. Równoważnie częstotliwość próbkowania musi przekraczać dwukrotność najwyższej częstotliwości, czyli pasma, by odtworzenie było idealne.

Słowa: twierdzenie o próbkowaniu, Nyquist, pasmo B, 1/(2B), dwukrotność, idealne odtworzenie
Źródło: https://en.wikipedia.org/wiki/Nyquist%E2%80%93Shannon_sampling_theorem (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co to jest częstotliwość Nyquista według Wikipedii?
Próg, czyli dwukrotność pasma sygnału ciągłego, którego częstotliwość próbkowania musi przekraczać. Strona podaje, że częstotliwość Nyquista jest cechą sprzętu do próbkowania.

Słowa: Nyquist, częstotliwość Nyquista, próg, dwukrotność pasma, sprzęt próbkujący
Źródło: https://en.wikipedia.org/wiki/Nyquist%E2%80%93Shannon_sampling_theorem (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co się dzieje przy próbkowaniu poniżej progu według Wikipedii?
Powstaje aliasing, który pojawia się jako niedoskonałości w odtworzonym sygnale. Składowe powyżej połowy częstotliwości próbkowania wyglądają jak niższe składowe, czyli aliasy, i rekonstrukcja daje alias zamiast oryginału. Strona zaznacza, że niedopróbkowanie powodujące aliasing nie jest ogólnie operacją odwracalną.

Słowa: aliasing, niedopróbkowanie, połowa częstotliwości, alias, nieodwracalne, rekonstrukcja
Źródło: https://en.wikipedia.org/wiki/Nyquist%E2%80%93Shannon_sampling_theorem (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest aliasing według Wikipedii?
Aliasing to zniekształcenie, które pojawia się, gdy sygnał jest próbkowany zbyt wolno. Odtworzony sygnał zawiera składowe niskiej częstotliwości, których nie było w oryginale. Dzieje się tak, gdy składowe oryginału leżą powyżej częstotliwości Nyquista, czyli połowy częstotliwości próbkowania.

Słowa: aliasing, zniekształcenie, zbyt wolne próbkowanie, częstotliwość Nyquista, składowe niskiej częstotliwości
Źródło: https://en.wikipedia.org/wiki/Aliasing (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie przykłady aliasingu podaje Wikipedia?
Wzór moiré na źle spikselowanym obrazie cegieł oraz efekt koła wozu w wideo, gdzie szprychy mogą wyglądać na wolno lub wstecz obracające się. W audio muzyka próbkowana z 32 000 Hz zaliasuje składowe od 16 000 Hz wzwyż.

Słowa: aliasing, moiré, efekt koła wozu, wideo, audio, 32 000 Hz, 16 000 Hz
Źródło: https://en.wikipedia.org/wiki/Aliasing (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak filtr antyaliasingowy działa według Wikipedii?
Filtr jest nakładany na sygnał przed próbkowaniem i usuwa składowe powyżej częstotliwości Nyquista. Strona zaznacza, że osłabione wysokie częstotliwości nadal mogą dać aliasy niskiej częstotliwości, ale zwykle o amplitudzie zbyt małej, by sprawiać kłopot.

Słowa: filtr antyaliasingowy, przed próbkowaniem, Nyquist, osłabienie, amplituda, aliasy
Źródło: https://en.wikipedia.org/wiki/Aliasing (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co robi przetwornik DAC według Wikipedii?
DAC zamienia sygnał cyfrowy na analogowy. Często zamienia skończoną precyzję danych cyfrowych w ciągle zmieniający się sygnał fizyczny, np. napięcie.

Słowa: DAC, przetwornik cyfrowo-analogowy, sygnał analogowy, napięcie, precyzja
Źródło: https://en.wikipedia.org/wiki/Digital-to-analog_converter (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak liczy się rozdzielczość DAC według Wikipedii?
Rozdzielczość to liczba różnych poziomów wyjściowych, zwykle podawana w bitach. Liczba bitów to logarytm dwójkowy z liczby poziomów. Przetwornik 1-bitowy daje 2 poziomy, a 8-bitowy 256.

Słowa: DAC, rozdzielczość, bity, poziomy, logarytm, 256, 1-bitowy
Źródło: https://en.wikipedia.org/wiki/Digital-to-analog_converter (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Do czego służą przetworniki DAC według Wikipedii?
Strona wymienia audio, czyli zamianę dźwięku cyfrowego na analogowy w odtwarzaczach CD, komputerach i głośnikach USB. Wideo, czyli zamianę obrazu cyfrowego na analogowy w telewizorach i telefonach. Komunikację, np. rozmowy telefoniczne i VoIP, oraz generowanie sygnałów transmisyjnych w systemach mobilnych i optycznych.

Słowa: DAC, audio, wideo, komunikacja, VoIP, generowanie sygnałów, telewizor, głośnik USB
Źródło: https://en.wikipedia.org/wiki/Digital-to-analog_converter (sprawdzono 2026-10, źródło angielskie, Wikipedia)
