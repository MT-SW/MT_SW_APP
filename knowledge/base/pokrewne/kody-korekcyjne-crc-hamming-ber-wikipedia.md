### Czym jest korekcja błędów w przód (FEC) według Wikipedii?
FEC, zwana też kodowaniem kanałowym, to technika kontroli błędów transmisji danych w niepewnych lub zaszumionych kanałach. Nadawca dodaje nadmiarowość do wiadomości, więc odbiorca może wykryć błędy i często poprawić ograniczoną ich liczbę, bez kanału zwrotnego do żądania ponownego przesłania.

Słowa: FEC, korekcja błędów w przód, kodowanie kanałowe, nadmiarowość, zaszumiony kanał, retransmisja
Źródło: https://en.wikipedia.org/wiki/Forward_error_correction (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jaki koszt ma FEC według Wikipedii?
Bity nadmiarowe zajmują te same zasoby komunikacyjne, które chronią. Oznacza to stałe, wyższe pasmo w kanale wprzód i obniżenie efektywnej szybkości bitowej. Mocniejsze kody dodają więcej nadmiarowości, co poprawia niezawodność, ale zmniejsza przepustowość.

Słowa: FEC, koszt, nadmiarowość, pasmo, szybkość bitowa, niezawodność, przepustowość
Źródło: https://en.wikipedia.org/wiki/Forward_error_correction (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie rodzaje kodów FEC wymienia Wikipedia?
Kody blokowe działają na blokach bitów o stałym rozmiarze, np. Reed–Solomon, BCH, Golay, Hamminga i LDPC. Kody splotowe działają na strumieniach bitów o dowolnej długości i często dekoduje się je algorytmem Viterbiego.

Słowa: FEC, kody blokowe, kody splotowe, Reed–Solomon, BCH, Golay, Hamming, LDPC, Viterbi
Źródło: https://en.wikipedia.org/wiki/Forward_error_correction (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest CRC według Wikipedii?
CRC, czyli cykliczny kod nadmiarowy, to kod wykrywający błędy. Dołącza krótką wartość kontrolną obliczoną jako reszta z dzielenia wielomianowego zawartości bloku danych. Przy odczycie obliczenie się powtarza, a niezgodność oznacza uszkodzenie danych.

Słowa: CRC, cykliczny kod nadmiarowy, suma kontrolna, dzielenie wielomianowe, reszta, uszkodzenie danych
Źródło: https://en.wikipedia.org/wiki/Cyclic_redundancy_check (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Gdzie i po co stosuje się CRC według Wikipedii?
CRC służy w sieciach cyfrowych i urządzeniach pamięci do wykrywania przypadkowych zmian danych, co wspiera integralność danych. Jest prosty w implementacji sprzętowej binarnej i szczególnie dobry w wykrywaniu błędów serii (burst), wywołanych szumem transmisji.

Słowa: CRC, sieci, pamięć, integralność danych, prosta implementacja, błędy serii, szum
Źródło: https://en.wikipedia.org/wiki/Cyclic_redundancy_check (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie są ograniczenia CRC według Wikipedii?
CRC wykrywa błąd, ale nie gwarantuje jego korekcji. Niektóre błędy pozostają niewykryte z małym prawdopodobieństwem. CRC nie chroni przed celowym manipulowaniem, bo atakujący może zmienić dane i przeliczyć CRC, więc do tego potrzebne jest uwierzytelnianie kryptograficzne.

Słowa: CRC, wykrywanie, brak gwarancji korekcji, niewykryte błędy, manipulacja, uwierzytelnianie kryptograficzne
Źródło: https://en.wikipedia.org/wiki/Cyclic_redundancy_check (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest kod Hamminga według Wikipedii?
Kod Hamminga to liniowy kod korekcyjny. Dodaje bity parzystości do danych, żeby wykrywać błędy, a w wielu przypadkach je lokalizować i korygować. Wprowadził go Richard W. Hamming w 1950 roku. Wersja (7,4) dodaje trzy bity parzystości do czterech bitów danych.

Słowa: kod Hamminga, liniowy, bity parzystości, Richard Hamming, 1950, (7,4), korekcja
Źródło: https://en.wikipedia.org/wiki/Hamming_code (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co potrafi kod Hamminga (7,4) według Wikipedii?
Kod (7,4) może albo wykryć i poprawić pojedynczy błąd bitu, albo wykryć (bez poprawiania) zarówno pojedynczy, jak i podwójny błąd bitu.

Słowa: kod Hamminga, (7,4), pojedynczy błąd, podwójny błąd, wykrywanie, poprawianie
Źródło: https://en.wikipedia.org/wiki/Hamming_code (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak działają bity parzystości w kodzie Hamminga według Wikipedii?
Bity parzystości stoją na pozycjach będących potęgami dwójki (1, 2, 4, 8 itd.). Każdy sprawdza zbiór pozycji, a zbiory się nakładają, więc bity parzystości też się nawzajem sprawdzają. Każdy bit danych jest objęty unikalną kombinacją bitów parzystości. Gdy kontrole nie przechodzą, wzorzec błędów (syndrom) wskazuje pozycję błędnego bitu.

Słowa: kod Hamminga, bity parzystości, potęgi dwójki, nakładające się zbiory, syndrom, pozycja błędu
Źródło: https://en.wikipedia.org/wiki/Hamming_code (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest BER według Wikipedii?
BER to miara jakości łącza: liczba błędnych bitów podzielona przez łączną liczbę przesłanych bitów w badanym przedziale czasu. Często wyraża się ją w procentach.

Słowa: BER, bit error rate, błędne bity, łączna liczba bitów, procent, łącze
Źródło: https://en.wikipedia.org/wiki/Bit_error_rate (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co wpływa na BER i jak go poprawić według Wikipedii?
Na BER wpływają szum kanału, zakłócenia, zniekształcenia, problemy z synchronizacją bitów, tłumienie i zanik wielodrogowy. Poprawić go można mocnym sygnałem, odpornym schematem modulacji lub kodowania linii oraz kodami FEC. Na zaszumionych kanałach często rysuje się go względem Eb/N0 lub Es/N0, powiązanych ze stosunkiem sygnału do szumu.

Słowa: BER, szum, zakłócenia, tłumienie, zanik wielodrogowy, FEC, Eb/N0, Es/N0
Źródło: https://en.wikipedia.org/wiki/Bit_error_rate (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak testuje się BER według Wikipedii?
Używa się testera BER (BERT), który wysyła znane wzorce testowe i liczy błędy. Wynik często podaje się jako stosunek, np. 1 na 1 000 000. Testery służą do sprawdzania jakości transmisji komponentów lub całych systemów.

Słowa: BERT, tester BER, wzorce testowe, 1 na 1 000 000, jakość transmisji, testy
Źródło: https://en.wikipedia.org/wiki/Bit_error_rate (sprawdzono 2026-10, źródło angielskie, Wikipedia)
