### Czym jest mDNS według Wikipedii?
mDNS to protokół, który tłumaczy nazwy hostów na adresy IP w małych sieciach bez lokalnego serwera nazw. Działa jak zwykły DNS, z podobnymi zapytaniami i formatami pakietów, ale wysyła wiadomości przez multicast IP.

Słowa: mDNS, multicast, DNS, nazwy hostów, sieć lokalna, bez serwera nazw
Źródło: https://en.wikipedia.org/wiki/Multicast_DNS (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak działa zapytanie mDNS według Wikipedii?
Klient wysyła multicastem zapytanie o host o danej nazwie, a host odsyła odpowiedź ze swoim adresem IP. Inne maszyny w podsieci mogą zaktualizować pamięć podręczną. Host rezygnuje z nazwy, wysyłając odpowiedź z TTL równym zero.

Słowa: mDNS, zapytanie, odpowiedź, pamięć podręczna, TTL zero, podsieć
Źródło: https://en.wikipedia.org/wiki/Multicast_DNS (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Na jakim porcie i adresach działa mDNS według Wikipedii?
mDNS używa UDP na porcie 5353. Adresem jest 224.0.0.251 dla IPv4 i ff02::fb dla IPv6.

Słowa: mDNS, UDP, port 5353, 224.0.0.251, ff02::fb, IPv4, IPv6
Źródło: https://en.wikipedia.org/wiki/Multicast_DNS (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jaką domenę obsługuje mDNS według Wikipedii?
Domyślnie mDNS obsługuje tylko nazwy hostów kończące się na .local. To może kolidować z hostami, które używają .local, ale są osiągane przez zwykły serwer DNS.

Słowa: mDNS, .local, kolizja, serwer DNS, nazwy hostów
Źródło: https://en.wikipedia.org/wiki/Multicast_DNS (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak mDNS łączy się z DNS-SD według Wikipedii?
mDNS można łączyć z DNS Service Discovery (DNS-SD), opisanym w RFC 6763.

Słowa: mDNS, DNS-SD, DNS Service Discovery, RFC 6763, usługi
Źródło: https://en.wikipedia.org/wiki/Multicast_DNS (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Samba według Wikipedii?
Samba to darmowa implementacja protokołu SMB. Pozwala udostępniać pliki i drukarki klientom Microsoft Windows i może współpracować z domenami Windows Server, jako kontroler domeny albo członek domeny. Działa też na większości systemów uniksowych, np. Linuksie, Solarisie, AIX, BSD i macOS.

Słowa: Samba, SMB, pliki, drukarki, Windows, kontroler domeny, Linux, BSD, macOS
Źródło: https://en.wikipedia.org/wiki/Samba_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Na jakiej licencji jest Samba według Wikipedii?
Samba jest wydawana na licencji GNU GPL. Strona podaje historię: od 1993 GPL-2.0-or-later, a od wersji 3.2.0 z 2008 GPL-3.0-or-later, z częściami na LGPL-3.0-or-later.

Słowa: Samba, licencja, GPL, LGPL, 1993, 2008, wersja 3.2.0
Źródło: https://en.wikipedia.org/wiki/Samba_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Btrfs według Wikipedii?
Btrfs, czyli B-tree file system, to format pamięci łączący system plików copy-on-write z menedżerem wolumenów logicznych. Stworzył go Chris Mason w 2007 roku dla Linuksa. Format na dysku jest uznany za stabilny w jądrze Linuksa od wersji 3.13.

Słowa: Btrfs, copy-on-write, system plików, wolumeny logiczne, Chris Mason, 2007, 3.13
Źródło: https://en.wikipedia.org/wiki/Btrfs (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie funkcje ma Btrfs według Wikipedii?
Migawki zapisywalne lub tylko do odczytu, które powstają szybko i na początku zajmują bardzo mało miejsca. Podwoluminy, czyli zagnieżdżalne korzenie systemu plików w partycji. Sumy kontrolne danych i metadanych, np. CRC-32C, xxHash, SHA256, BLAKE2B. Przezroczysta kompresja zlib, LZO i ZSTD, klonowanie plików reflink, wysyłanie i odbieranie do przyrostowych kopii, skrubowanie online i grupy limitów.

Słowa: Btrfs, migawki, podwoluminy, sumy kontrolne, CRC-32C, kompresja, ZSTD, reflink, skrubowanie, limity
Źródło: https://en.wikipedia.org/wiki/Btrfs (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie poziomy RAID obsługuje Btrfs według Wikipedii?
Profile fragmentów to single, dup, RAID 0, RAID 1, RAID 1C3, RAID 1C4, RAID 5, RAID 6 i RAID 10. Strona nie podaje w treści, jak stabilne są RAID 5 i 6.

Słowa: Btrfs, RAID 0, RAID 1, RAID 1C3, RAID 5, RAID 6, RAID 10, profile, stabilność
Źródło: https://en.wikipedia.org/wiki/Btrfs (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest restic według dokumentacji?
Restic to program do kopii zapasowych, który dokumentacja nazywa szybkim i bezpiecznym.

Słowa: restic, kopia zapasowa, szybki, bezpieczny, program
Źródło: https://restic.readthedocs.io/en/stable/010_introduction.html (sprawdzono 2026-10, źródło: dokumentacja restic)

### Jak działają migawki w restic według dokumentacji?
Każda kopia tworzy migawkę, którą wypisuje się poleceniem `restic snapshots` i przywraca po identyfikatorze.

Słowa: restic, migawka, snapshots, przywracanie, identyfikator
Źródło: https://restic.readthedocs.io/en/stable/010_introduction.html (sprawdzono 2026-10, źródło: dokumentacja restic)

### Jak zabezpieczyć repozytorium restic hasłem według dokumentacji?
Repozytorium tworzy się z hasłem podanym przez zmienną `RESTIC_PASSWORD`. Lokalizację repozytorium ustawia się zmienną `RESTIC_REPOSITORY`. Szczegóły szyfrowania są w osobnej sekcji dokumentacji.

Słowa: restic, hasło, RESTIC_PASSWORD, RESTIC_REPOSITORY, szyfrowanie, repozytorium
Źródło: https://restic.readthedocs.io/en/stable/010_introduction.html (sprawdzono 2026-10, źródło: dokumentacja restic)

### Jak sprawdzić repozytorium restic według dokumentacji?
Polecenie `restic check` sprawdza metadane repozytorium, a opcja `--read-data` sprawdza pełne dane.

Słowa: restic, check, metadane, --read-data, weryfikacja, repozytorium
Źródło: https://restic.readthedocs.io/en/stable/010_introduction.html (sprawdzono 2026-10, źródło: dokumentacja restic)
