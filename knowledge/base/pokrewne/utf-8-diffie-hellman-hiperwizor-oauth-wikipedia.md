### Jak działa UTF-8 według Wikipedii?
UTF-8 to kodowanie o zmiennej szerokości dla Unicode. Koduje każdy punkt kodowy jedną do czterech jednobajtowych jednostek. Punkty o niższych wartościach, które występują częściej, zajmują mniej bajtów.

Słowa: UTF-8, kodowanie, zmienna szerokość, Unicode, punkt kodowy, bajty
Źródło: https://en.wikipedia.org/wiki/UTF-8 (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Ile bajtów zajmują znaki w UTF-8 według Wikipedii?
Punkty od U+0000 do U+007F (ASCII) zajmują jeden bajt. Do U+07FF są dwa bajty, od U+0800 do U+FFFF trzy, a od U+10000 do U+10FFFF cztery.

Słowa: UTF-8, jeden bajt, dwa bajty, trzy bajty, cztery bajty, U+007F, U+07FF, U+FFFF, U+10FFFF
Źródło: https://en.wikipedia.org/wiki/UTF-8 (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czy UTF-8 jest zgodny z ASCII według Wikipedii?
Tak. Pierwsze 128 znaków zajmuje jeden bajt o tej samej wartości binarnej co w ASCII, więc plik UTF-8 zawierający tylko te znaki jest identyczny z plikiem ASCII.

Słowa: UTF-8, ASCII, zgodność, 128 znaków, identyczny plik
Źródło: https://en.wikipedia.org/wiki/UTF-8 (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czy UTF-8 pozwala na synchronizację od dowolnego miejsca według Wikipedii?
Tak, strona podaje, że UTF-8 jest samosynchronizujący. Czytelnik może zacząć w losowej pozycji i znaleźć początek punktu kodowego, cofając się najwyżej o 3 bajty.

Słowa: UTF-8, samosynchronizacja, losowa pozycja, początek znaku, cofnięcie, 3 bajty
Źródło: https://en.wikipedia.org/wiki/UTF-8 (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest wymiana klucza Diffiego–Hellmana według Wikipedii?
Pozwala dwóm stronom bez wcześniejszego kontaktu wspólnie ustalić tajny klucz przez niezabezpieczony kanał. Klucz można potem użyć z szyfrem symetrycznym do szyfrowania dalszej komunikacji.

Słowa: Diffie–Hellman, wymiana klucza, tajny klucz, niezabezpieczony kanał, szyfr symetryczny
Źródło: https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Na czym opiera się bezpieczeństwo Diffiego–Hellmana według Wikipedii?
Każda strona łączy swoją prywatną wartość z publicznymi wartościami przez potęgowanie modularne. Obie strony dochodzą do tego samego wyniku g^(ab) mod p. Podsłuchujący widzi p, g, A i B, ale strona podaje, że obliczenie wspólnej wartości z tych danych jest niewykonalne dla dużych parametrów. Opiera się to na trudności logarytmu dyskretnego, a zalecane są liczby pierwsze co najmniej 2048-bitowe.

Słowa: Diffie–Hellman, potęgowanie modularne, logarytm dyskretny, funkcja jednokierunkowa, 2048 bitów, podsłuch
Źródło: https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jaka jest słabość Diffiego–Hellmana według Wikipedii?
Oryginalna wymiana nie uwierzytelnia stron i może być podatna na atak man-in-the-middle, w którym atakujący prowadzi osobne wymiany z każdą stroną i odszyfrowuje oraz ponownie szyfruje wiadomości. Strona podaje, że zwykle potrzebne jest uwierzytelnianie, żeby temu zapobiec.

Słowa: Diffie–Hellman, man-in-the-middle, uwierzytelnianie, podsłuch, odszyfrowanie, słabość
Źródło: https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest hiperwizor według Wikipedii?
Hiperwizor, zwany też monitorem maszyn wirtualnych (VMM), to oprogramowanie, firmware lub sprzęt, który tworzy i uruchamia maszyny wirtualne. Komputer, na którym działa, to host, a każda maszyna wirtualna to gość.

Słowa: hiperwizor, VMM, maszyna wirtualna, host, gość, tworzenie, uruchamianie
Źródło: https://en.wikipedia.org/wiki/Hypervisor (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym różni się hiperwizor typu 1 od typu 2 według Wikipedii?
Typ 1, natywny lub bare-metal, działa bezpośrednio na sprzęcie hosta i zarządza systemami gości. Przykłady to Hyper-V, Xen i VMware ESXi. Typ 2, hostowany, działa jako zwykły program w konwencjonalnym systemie operacyjnym. Przykłady to VirtualBox i VMware Workstation. Strona zaznacza, że granica między nimi nie zawsze jest wyraźna.

Słowa: hiperwizor, typ 1, typ 2, bare-metal, hostowany, Hyper-V, Xen, ESXi, VirtualBox, VMware Workstation
Źródło: https://en.wikipedia.org/wiki/Hypervisor (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest OAuth według Wikipedii?
OAuth to otwarty standard delegowania dostępu. Pozwala użytkownikowi przyznać stronie lub aplikacji dostęp do jego informacji w innym serwisie bez podawania hasła.

Słowa: OAuth, delegowanie dostępu, otwarty standard, hasło, aplikacja, zasoby
Źródło: https://en.wikipedia.org/wiki/OAuth (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie role są w OAuth według Wikipedii?
Właściciel zasobów to użytkownik, który zezwala na dostęp. Klient to aplikacja trzeciej strony, która chce dostępu w imieniu właściciela. Serwer autoryzacji wydaje tokeny dostępu za zgodą właściciela. Serwer zasobów przechowuje chronione zasoby.

Słowa: OAuth, właściciel zasobów, klient, serwer autoryzacji, serwer zasobów, role
Źródło: https://en.wikipedia.org/wiki/OAuth (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak działa token dostępu w OAuth według Wikipedii?
Serwer autoryzacji wydaje klientowi token dostępu po zgodzie właściciela. Klient przedstawia token serwerowi zasobów, żeby uzyskać dostęp do chronionych zasobów. Strona opisuje token jako rodzaj klucza do parkowania, który dowodzi, że użytkownik zezwolił klientowi na korzystanie z API.

Słowa: OAuth, token dostępu, serwer autoryzacji, serwer zasobów, klucz do parkowania, API, zgoda
Źródło: https://en.wikipedia.org/wiki/OAuth (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czy OAuth służy do uwierzytelniania według Wikipedii?
Nie. Strona zaznacza, że OAuth to protokół autoryzacji, a nie uwierzytelniania.

Słowa: OAuth, autoryzacja, uwierzytelnianie, nie uwierzytelnia, protokół
Źródło: https://en.wikipedia.org/wiki/OAuth (sprawdzono 2026-10, źródło angielskie, Wikipedia)
