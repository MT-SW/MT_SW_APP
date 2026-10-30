### Czym jest VLAN według Wikipedii?
VLAN to wirtualna sieć lokalna, czyli domena rozgłoszeniowa wydzielona i odizolowana na warstwie łącza danych (warstwa 2 OSI). Pozwala wielu logicznym sieciom dzielić tę samą fizyczną infrastrukturę.

Słowa: VLAN, domena rozgłoszeniowa, warstwa 2, OSI, logiczne sieci, infrastruktura
Źródło: https://en.wikipedia.org/wiki/Virtual_LAN (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak działa tagowanie VLAN według Wikipedii?
VLAN działa przez oznaczanie ramek przesyłanych w domenie rozgłoszeniowej. Standard IEEE 802.1Q, opisany po raz pierwszy w wydaniu z 1998 roku, używa pola wewnątrz ramki do tagowania. Pole VID ma 12 bitów, więc sieć może mieć maksymalnie 4094 użyteczne VLAN-y.

Słowa: VLAN, tagowanie, 802.1Q, VID, 12 bitów, 4094, ramka
Źródło: https://en.wikipedia.org/wiki/Virtual_LAN (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Po co segmentować sieć VLAN-ami według Wikipedii?
Segmentacja rozgłoszeń zmniejsza ruch rozgłoszeniowy, który musi obsłużyć każde urządzenie i segment. Bezpieczeństwo: urządzenia, które mają być rozdzielone, nie wchodzą ze sobą w bezpośrednią interakcję. Strona ostrzega, że można to obejść, jeśli VLAN-y nie są wdrożone starannie.

Słowa: VLAN, rozgłoszenia, segmentacja, bezpieczeństwo, izolacja, wdrożenie
Źródło: https://en.wikipedia.org/wiki/Virtual_LAN (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Wake-on-LAN według Wikipedii?
Wake-on-LAN to standard sieci Ethernet lub Token Ring, który pozwala wiadomości sieciowej włączyć komputer albo wybudzić go z uśpienia. Wiadomość zwykle wysyła program na urządzeniu w tej samej sieci lokalnej.

Słowa: Wake-on-LAN, WoL, Ethernet, uśpienie, wybudzanie, sieć lokalna
Źródło: https://en.wikipedia.org/wiki/Wake-on-LAN (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak zbudowany jest magic packet według Wikipedii?
Magic packet to ramka rozgłaszana na warstwie łącza danych do wszystkich urządzeń w sieci, na adres rozgłoszeniowy sieci, bez użycia adresu IP. Ładunek ma 6 bajtów FF, a potem szesnaście powtórzeń 48-bitowego adresu MAC komputera docelowego, razem 102 bajty. Zwykle wysyła się go jako datagram UDP na port 0, 7 lub 9, albo bezpośrednio w Ethernet z EtherType 0x0842.

Słowa: magic packet, WoL, FF, adres MAC, 102 bajty, UDP, port 7, port 9, EtherType 0x0842
Źródło: https://en.wikipedia.org/wiki/Wake-on-LAN (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czego wymaga Wake-on-LAN według Wikipedii?
Karta sieciowa musi mieć zasilane części obwodów i nasłuchiwać pakietów, gdy reszta systemu jest wyłączona. Zwykle trzeba włączyć Wake-on-LAN w ustawieniach zarządzania zasilaniem w BIOS/UEFI płyty głównej, choć niektóre systemy, np. komputery Apple, włączają go domyślnie.

Słowa: Wake-on-LAN, karta sieciowa, BIOS, UEFI, zasilanie, nasłuchiwanie, Apple
Źródło: https://en.wikipedia.org/wiki/Wake-on-LAN (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest NTP według Wikipedii?
NTP to protokół sieciowy, który synchronizuje zegary komputerów w sieciach pakietowych o zmiennym opóźnieniu. Dąży do utrzymania uczestniczących komputerów w granicach kilku milisekund od czasu UTC.

Słowa: NTP, synchronizacja zegarów, UTC, milisekundy, opóźnienie, sieci pakietowe
Źródło: https://en.wikipedia.org/wiki/Network_Time_Protocol (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co to są stratum w NTP według Wikipedii?
NTP porządkuje źródła czasu w poziomy zwane stratum, numerowane od 0 na szczycie. Serwer zsynchronizowany z serwerem stratum n działa na stratum n+1. Stratum 1 to serwery zsynchronizowane z dokładnością kilku mikrosekund do urządzenia stratum 0, np. zegara atomowego lub odbiornika GNSS. Limit to 15, a stratum 16 oznacza brak synchronizacji. Strona zaznacza, że stratum nie zawsze oznacza jakość.

Słowa: NTP, stratum, stratum 1, stratum 0, GNSS, GPS, stratum 16, jakość
Źródło: https://en.wikipedia.org/wiki/Network_Time_Protocol (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jaką dokładność daje NTP według Wikipedii?
W publicznym internecie NTP zwykle utrzymuje czas w granicach dziesiątek milisekund. W idealnych warunkach w sieci lokalnej osiąga lepiej niż 1 ms. Asymetryczne trasy i przeciążenie sieci mogą dawać błędy 100 ms lub więcej.

Słowa: NTP, dokładność, dziesiątki milisekund, 1 ms, sieć lokalna, asymetria, przeciążenie, 100 ms
Źródło: https://en.wikipedia.org/wiki/Network_Time_Protocol (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Na jakim porcie działa NTP według Wikipedii?
NTP używa UDP na porcie 123.

Słowa: NTP, UDP, port 123
Źródło: https://en.wikipedia.org/wiki/Network_Time_Protocol (sprawdzono 2026-10, źródło angielskie, Wikipedia)
