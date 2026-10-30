### Czym jest Redis według Wikipedii?
Redis to baza danych klucz–wartość działająca w pamięci RAM, która może pełnić rolę rozproszonego cache i brokera wiadomości, z opcjonalną trwałością.

Słowa: Redis, baza klucz–wartość, pamięć RAM, cache, broker wiadomości, trwałość
Źródło: https://en.wikipedia.org/wiki/Redis (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie typy danych obsługuje Redis według Wikipedii?
Strona wymienia ciągi znaków, dokumenty JSON, hashe, listy, zbiory i zbiory wektorowe, a także inne.

Słowa: Redis, ciągi znaków, JSON, hashe, listy, zbiory, wektory
Źródło: https://en.wikipedia.org/wiki/Redis (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak Redis zapisuje dane na dysk według Wikipedii?
Snapshotting RDB: zbiór danych jest okresowo zrzucany z pamięci na dysk jako binarny zrzut. Journaling AOF: każda operacja modyfikująca dane jest zapisywana w pliku dołączanym na końcu.

Słowa: Redis, RDB, AOF, snapshot, zrzut binarny, dziennik, dołączany plik
Źródło: https://en.wikipedia.org/wiki/Redis (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest WebSocket według Wikipedii?
WebSocket to protokół komunikacyjny zapewniający dwukierunkowy kanał przez jedno połączenie TCP. Standaryzowany przez IETF jako RFC 6455 w 2011 roku.

Słowa: WebSocket, dwukierunkowy, TCP, RFC 6455, 2011, IETF
Źródło: https://en.wikipedia.org/wiki/WebSocket (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak zaczyna się połączenie WebSocket według Wikipedii?
Połączenie zaczyna się uzgodnieniem przez HTTP. Klient wysyła żądanie GET w wersji 1.1 lub nowszej, a serwer odpowiada kodem 101 (Switching Protocols). Potem komunikacja przechodzi na protokół ramek binarnych. Ponieważ uzgodnienie używa HTTP, klienty WebSocket i HTTP mogą korzystać z tych samych portów 80 i 443.

Słowa: WebSocket, uzgodnienie, HTTP, GET, 101, Switching Protocols, porty 80 i 443, ramki binarne
Źródło: https://en.wikipedia.org/wiki/WebSocket (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym WebSocket różni się od odpytywania (polling) według Wikipedii?
WebSocket umożliwia komunikację pełnodupleksową z mniejszym narzutem niż półdupleksowe alternatywy, np. odpytywanie HTTP. Serwer może wysyłać treść do klienta bez wcześniejszego żądania, a połączenie jest utrzymywane otwarte, więc wiadomości płyną ciągle.

Słowa: WebSocket, polling, pełny dupleks, półdupleks, narzut, otwarte połączenie, bez żądania
Źródło: https://en.wikipedia.org/wiki/WebSocket (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest adres MAC według Wikipedii?
Adres MAC to unikalny identyfikator przypisany do karty sieciowej, używany jako adres sieciowy w obrębie segmentu sieci. Działa na warstwie łącza danych, w podwarstwie kontroli dostępu do medium.

Słowa: adres MAC, karta sieciowa, NIC, warstwa łącza danych, segment, podwarstwa MAC
Źródło: https://en.wikipedia.org/wiki/MAC_address (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak zbudowany jest adres MAC według Wikipedii?
Adres ma 48 bitów (EUI-48). Składa się z sześciu grup po dwie cyfry szesnastkowe, rozdzielonych myślnikami, dwukropkami lub bez separatora. Pierwsze trzy oktety identyfikują organizację, która go wydała. To identyfikator OUI, czyli 24 bity.

Słowa: adres MAC, 48 bitów, EUI-48, szesnastkowe, dwukropek, myślnik, OUI, 24 bity, oktet
Źródło: https://en.wikipedia.org/wiki/MAC_address (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest bcrypt według Wikipedii?
Bcrypt to funkcja haszowania haseł zaprojektowana przez Nielsa Provosa i Davida Mazièresa, przedstawiona na USENIX w 1999 roku. Opiera się na szyfrze Blowfish i używa kosztownej inicjalizacji klucza, zwanej Eksblowfish.

Słowa: bcrypt, haszowanie haseł, Blowfish, Eksblowfish, USENIX, 1999
Źródło: https://en.wikipedia.org/wiki/Bcrypt (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co to jest współczynnik kosztu w bcrypt według Wikipedii?
Koszt to parametr liczbowy ustawiający liczbę rund jako potęgę dwójki. Koszt 12 odpowiada 2^12, czyli 4096 rund. Liczbę iteracji można z czasem podnosić, żeby haszowanie było wolniejsze i odporne na ataki siłowe przy wzroście mocy obliczeniowej.

Słowa: bcrypt, koszt, runda, potęga dwójki, 4096, iteracje, ataki siłowe, moc obliczeniowa
Źródło: https://en.wikipedia.org/wiki/Bcrypt (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak bcrypt używa soli według Wikipedii?
Każdy skrót zawiera 16-bajtową (128-bitową) sól, zwykle losową. Chroni ona przed atakami z tęczowymi tablicami. Sól jest zakodowana w wynikowym ciągu o postaci $2a$[koszt]$[22 znaki soli][31 znaków skrótu].

Słowa: bcrypt, sól, 16 bajtów, 128 bitów, tęczowe tablice, format, $2a$
Źródło: https://en.wikipedia.org/wiki/Bcrypt (sprawdzono 2026-10, źródło angielskie, Wikipedia)
