### Jaki rozmiar bloku i klucze ma AES według Wikipedii?
AES, zwany też Rijndael, ma blok 128-bitowy i trzy długości klucza: 128, 192 i 256 bitów.

Słowa: AES, Rijndael, blok 128 bitów, klucz 128, 192, 256 bitów, szyfr blokowy
Źródło: https://en.wikipedia.org/wiki/Advanced_Encryption_Standard (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Ile rund ma AES według Wikipedii?
Liczba rund zależy od długości klucza. Klucz 128-bitowy ma 10 rund, 192-bitowy 12, a 256-bitowy 14.

Słowa: AES, rundy, 10 rund, 12 rund, 14 rund, długość klucza
Źródło: https://en.wikipedia.org/wiki/Advanced_Encryption_Standard (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Kiedy AES został ustandaryzowany według Wikipedii?
Standard ustanowił NIST w 2001 roku, a ogłoszono go jako FIPS PUB 197 26 listopada 2001 roku.

Słowa: AES, NIST, 2001, FIPS PUB 197, standard
Źródło: https://en.wikipedia.org/wiki/Advanced_Encryption_Standard (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest RSA według Wikipedii?
RSA, czyli Rivest–Shamir–Adleman, to rodzina kryptosystemów z kluczem publicznym, jedna z najstarszych, szeroko używana do bezpiecznego przesyłania danych. Rivest, Shamir i Adleman opisali algorytm publicznie w 1977 roku, a równoważny system opracował w tajemnicy w GCHQ w 1973 roku Clifford Cocks.

Słowa: RSA, klucz publiczny, kryptosystem, 1977, GCHQ, 1973, Clifford Cocks
Źródło: https://en.wikipedia.org/wiki/RSA_(cryptosystem) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Na czym opiera się bezpieczeństwo RSA według Wikipedii?
Na trudności rozkładu na czynniki iloczynu dwóch dużych liczb pierwszych. Czy złamanie RSA jest równie trudne jak rozkład na czynniki, pozostaje otwartym pytaniem.

Słowa: RSA, rozkład na czynniki, dwie duże liczby pierwsze, bezpieczeństwo, pytanie otwarte
Źródło: https://en.wikipedia.org/wiki/RSA_(cryptosystem) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak działają klucze RSA według Wikipedii?
Klucz prywatny to para dużych liczb pierwszych wybranych losowo i trzymanych w tajemnicy. Może podpisywać wiadomości lub odszyfrowywać wiadomości przeznaczone dla użytkownika. Klucz publiczny to iloczyn tych liczb. Może weryfikować wiadomości od użytkownika albo szyfrować je tak, by tylko on mógł je odczytać.

Słowa: RSA, klucz prywatny, klucz publiczny, iloczyn liczb pierwszych, podpis, szyfrowanie, weryfikacja
Źródło: https://en.wikipedia.org/wiki/RSA_(cryptosystem) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie właściwości ma funkcja skrótu według Wikipedii?
Funkcja skrótu kryptograficznego ma zachowywać się jak losowa funkcja, będąc jednocześnie deterministyczną i wydajnie obliczalną. Strona wymienia odporność na preimage, czyli trudność znalezienia wiadomości dla danego skrótu, oraz odporność na kolizje, czyli trudność znalezienia dwóch różnych wiadomości o tym samym skrócie. Małe zmiany wejścia dają bardzo różny skrót.

Słowa: funkcja skrótu, kryptograficzna, deterministyczna, odporność na preimage, odporność na kolizje, zmiana wejścia
Źródło: https://en.wikipedia.org/wiki/Cryptographic_hash_function (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czemu odporność na kolizje wymaga dłuższego skrótu według Wikipedii?
Strona podaje, że odporność na kolizje wymaga wartości skrótu około dwa razy dłuższej niż odporność na preimage.

Słowa: funkcja skrótu, odporność na kolizje, dwa razy dłuższa, preimage, długość skrótu
Źródło: https://en.wikipedia.org/wiki/Cryptographic_hash_function (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest wariant Diffiego–Hellmana ECDH według Wikipedii?
ECDH to wariant protokołu, który przedstawia element grupy jako punkt na krzywej eliptycznej zamiast jako liczbę modulo n. Autorzy analizy Logjam zalecają kryptografię krzywych eliptycznych, dla której nie znano podobnego ataku.

Słowa: ECDH, krzywa eliptyczna, punkt na krzywej, modulo, Logjam, kryptografia krzywych eliptycznych
Źródło: https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czy zwykły Diffie–Hellman uwierzytelnia strony według Wikipedii?
Nie. Strona podaje, że zwykły Diffie–Hellman nie uwierzytelnia komunikujących się stron i może być podatny na atak man-in-the-middle. Jednocześnie jest podstawą wielu protokołów uwierzytelnionych. DH nie może być bezpośrednio użyty do podpisywania certyfikatów, ale algorytmy podpisów ElGamal i DSA są z nim matematycznie powiązane.

Słowa: Diffie–Hellman, uwierzytelnianie, man-in-the-middle, protokoły uwierzytelnione, ElGamal, DSA, certyfikaty
Źródło: https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange (sprawdzono 2026-10, źródło angielskie, Wikipedia)
