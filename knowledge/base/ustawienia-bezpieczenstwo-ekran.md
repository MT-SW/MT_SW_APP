# Ustawienia bezpieczeństwa i ekranu

### Do czego służą klucze w ustawieniach Security (public, private, admin)?
Public Key jest rozsyłany innym węzłom, by obliczyły wspólny sekret i sprawdziły podpisy pakietów; z niego wynika numer węzła; firmware generuje go po ustawieniu regionu LoRa. Private Key buduje wspólne klucze z urządzeniami zdalnymi i ma pozostać tajny. Admin Key: do trzech kluczy publicznych uprawnionych do wiadomości administracyjnych; zdalna administracja przyjmuje tylko wiadomość bezpośrednią zaszyfrowaną od wymienionego klucza. Kasowanie i ponowna instalacja firmware niszczy oba klucze: węzeł dostaje nowy numer i inne węzły traktują go jak innego. Zrób kopię kluczy.
Słowa: klucz publiczny, klucz prywatny, admin key, zdalna administracja, PKC, security, numer węzła, kopia kluczy
Źródło: https://meshtastic.org/docs/configuration/radio/security/ (sprawdzono 2026-10)

### Co to Managed Mode, Serial Console, Debug Log i Admin Channel?
Managed Mode blokuje aplikacjom zapis konfiguracji, zmiany idą tylko przez PKC; wymaga co najmniej jednego klucza admina (bez niego firmware go wyłącza). Przed włączeniem sprawdź, czy zdalna administracja lub starszy kanał admina działa, aby się nie zablokować. Serial Console wyłączony nie inicjuje API strumieniowego. Debug Log true utrzymuje logi na żywo przez port szeregowy lub Bluetooth przy aktywnym API (domyślnie wyłączone, gdy klient się łączy). Admin Channel Enabled pozwala na starszy kanał administracyjny, który dokumentacja opisuje jako niebezpieczny.
Słowa: managed mode, serial console, debug log, kanał admin, zablokowanie, lockout, logi przez bluetooth
Źródło: https://meshtastic.org/docs/configuration/radio/security/ (sprawdzono 2026-10)

### Jakie są ustawienia ekranu i ich wartości domyślne?
Screen On Duration 0 = 10 minut. Auto Carousel Interval 0 = wyłączone. GPS Format: DEC (domyślnie), DMS, UTM, MGRS, OLC, OSGR, MLS. Flip Screen, Preferred Display Units METRIC (lub IMPERIAL), OLED Definition OLED_AUTO (SSD1306, SH1106, SH1107), Display Mode DEFAULT/TWOCOLOR/INVERTED/COLOR, Heading Bold, Wake on Tap or Motion, Always Point North (display.compass_north_top), zegar 12/24-godzinny.
Słowa: ekran, wyświetlacz, OLED, SSD1306, SH1106, screen on, carousel, GPS format, jednostki, kompas, display
Źródło: https://meshtastic.org/docs/configuration/radio/display/ (sprawdzono 2026-10)
