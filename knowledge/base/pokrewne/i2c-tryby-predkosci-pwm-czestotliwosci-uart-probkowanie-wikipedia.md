### Jakie tryby prędkości ma magistrala I2C według Wikipedii?
Standard to 100 kbit/s, fast mode to 400 kbit/s, low-speed to 10 kbit/s, fast mode plus to 1 Mbit/s, high-speed to 3,4 Mbit/s, a ultra-fast to 5 Mbit/s.

Słowa: I2C, tryb standardowy, 100 kbit/s, fast mode, 400 kbit/s, 1 Mbit/s, 3,4 Mbit/s, 5 Mbit/s, prędkość
Źródło: https://en.wikipedia.org/wiki/I%C2%B2C (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak I2C nazywa role urządzeń według Wikipedii?
Od 1980 do 2021 patenty i specyfikacje używały terminów master i slave. Wersja 7 zmieniła je na controller i target, a role techniczne pozostały takie same. Controller generuje zegar i inicjuje komunikację, a target odbiera zegar i odpowiada, gdy zostanie zaadresowany.

Słowa: I2C, master, slave, controller, target, rewizja 7, zegar, adresowanie
Źródło: https://en.wikipedia.org/wiki/I%C2%B2C (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Kiedy I2C wymaga rezystorów podciągających według Wikipedii?
Obie linie SCL i SDA są podciągane rezystorami, bo urządzenia tylko ciągną linię w dół, a inaczej linia wisi w górze. Typowe napięcia to +5 V lub +3,3 V.

Słowa: I2C, rezystory podciągające, SCL, SDA, pull-up, 5 V, 3,3 V
Źródło: https://en.wikipedia.org/wiki/I%C2%B2C (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie częstotliwości PWM stosuje się w różnych zastosowaniach według Wikipedii?
Piec elektryczny ma kilka przełączeń na minutę. Ściemniacz lamp używa 100 lub 120 Hz. Napędy silników od kilku do kilkudziesięciu kHz. Wzmacniacze audio i zasilacze komputerowe od dziesiątek do setek kHz. Za wysoka częstotliwość może przedwcześnie zużywać mechaniczne elementy sterujące, a za niska powoduje oscylacje w odbiorniku.

Słowa: PWM, częstotliwość przełączania, 100 Hz, 120 Hz, kHz, ściemniacz, silnik, zasilacz, zużycie, oscylacje
Źródło: https://en.wikipedia.org/wiki/Pulse-width_modulation (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Dlaczego przy PWM diody LED nie migoczą według Wikipedii?
Diody LED szybko reagują i mogą migotać przy małych częstotliwościach sterowania. Wyższa częstotliwość PWM zmniejsza zauważalne migotanie. Powyżej progu zlewania się migotania oko widzi średnią jasność.

Słowa: PWM, LED, migotanie, częstotliwość, próg zlewania, średnia jasność, ściemnianie
Źródło: https://en.wikipedia.org/wiki/Pulse-width_modulation (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Dlaczego PWM dobrze nadaje się do obciążeń bezwładnościowych według Wikipedii?
Silniki i inne obciążenia bezwładnościowe nie reagują tak łatwo na dyskretne przełączanie, więc PWM dobrze nadaje się do regulacji ich prędkości, np. w robotyce.

Słowa: PWM, silnik, bezwładność, robotyka, prędkość, dyskretne przełączanie
Źródło: https://en.wikipedia.org/wiki/Pulse-width_modulation (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak UART tworzy ramkę znaku według Wikipedii?
Linia w bezczynności jest w stanie wysokim. Bit startu jest niski i zapowiada nowy znak. Dane to zwykle 5–8 bitów, wysyłane najmniej znaczący jako pierwszy. Bit parzystości jest opcjonalny i po danych. Bit lub dwa bity stopu są wysokie i kończą znak.

Słowa: UART, ramka, bit startu, bity danych, parzystość, bit stopu, LSB, bezczynność
Źródło: https://en.wikipedia.org/wiki/Universal_asynchronous_receiver-transmitter (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak odbiornik UART synchronizuje próbkowanie według Wikipedii?
Odbiornik szuka opadającego zbocza bitu startu, a potem próbkuje każdy bit w pobliżu jego środka. Każdy UART ma wewnętrzny zegar, który działa z wielokrotnością szybkości bitowej, zwykle 8 lub 16 razy, co umożliwia próbkowanie w środku bitu.

Słowa: UART, zbocze opadające, próbkowanie, środek bitu, oversampling, 8 lub 16 razy, zegar wewnętrzny
Źródło: https://en.wikipedia.org/wiki/Universal_asynchronous_receiver-transmitter (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Ile znaków na sekundę daje UART w trybie 8N1 według Wikipedii?
W typowym ustawieniu 8N1, czyli 8 bitów danych, bez parzystości i 1 bit stopu, liczba znaków na sekundę to szybkość bitowa podzielona przez 10.

Słowa: UART, 8N1, znaki na sekundę, szybkość bitowa, dzielone przez 10, baud
Źródło: https://en.wikipedia.org/wiki/Universal_asynchronous_receiver-transmitter (sprawdzono 2026-10, źródło angielskie, Wikipedia)
