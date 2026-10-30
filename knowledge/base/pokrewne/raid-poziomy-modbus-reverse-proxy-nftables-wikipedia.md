### Czym jest RAID według Wikipedii?
RAID, czyli macierz niezależnych dysków, rozkłada dane na co najmniej dwa urządzenia pamięci zgrupowane w macierz. Różne układy dysków nazywa się poziomami RAID.

Słowa: RAID, macierz dysków, poziomy RAID, rozkład danych, dyski
Źródło: https://en.wikipedia.org/wiki/RAID (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co daje RAID 0 według Wikipedii?
RAID 0 rozkłada dane na dyski bez lustrzenia i bez parzystości. Poprawia przepustowość, ale awaria jednego dysku czyni cały wolumin niedostępnym. Pliki z RAID 0 nie da się odzyskać bez kopii zapasowej.

Słowa: RAID 0, striping, przepustowość, awaria dysku, brak redundancji, kopia zapasowa
Źródło: https://en.wikipedia.org/wiki/RAID (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co daje RAID 1 według Wikipedii?
RAID 1 lustrzy identyczne dane na dwóch lub więcej dyskach. Macierz działa dalej, dopóki działa choć jeden dysk.

Słowa: RAID 1, lustrzenie, mirror, redundancja, jeden dysk
Źródło: https://en.wikipedia.org/wiki/RAID (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co daje RAID 5 według Wikipedii?
RAID 5 rozkłada dane z rozproszoną parzystością i wymaga co najmniej trzech dysków. Może odtworzyć dane po awarii jednego dysku.

Słowa: RAID 5, parzystość, trzy dyski, awaria jednego dysku, odtwarzanie
Źródło: https://en.wikipedia.org/wiki/RAID (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co daje RAID 6 według Wikipedii?
RAID 6 rozkłada dane z podwójną rozproszoną parzystością i wymaga co najmniej czterech dysków. Toleruje awarię do dwóch dysków.

Słowa: RAID 6, podwójna parzystość, cztery dyski, dwa dyski, tolerancja awarii
Źródło: https://en.wikipedia.org/wiki/RAID (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co daje RAID 10 według Wikipedii?
RAID 10 to zestaw paskowany z lustrzanych dysków. Może przetrwać utratę wielu dysków, o ile żadne lustro nie straci wszystkich swoich dysków.

Słowa: RAID 10, lustro, pasek, wiele dysków, tolerancja
Źródło: https://en.wikipedia.org/wiki/RAID (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czy RAID zastępuje kopię zapasową według Wikipedii?
Nie. RAID może chronić przed fizyczną awarią dysku, ale dane nadal narażone są na zniszczenie przez operatora, oprogramowanie, sprzęt i wirusy.

Słowa: RAID, kopia zapasowa, awaria dysku, wirus, operator, nie zastępuje
Źródło: https://en.wikipedia.org/wiki/RAID (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Modbus według Wikipedii?
Modbus to otwarty, wolny od opłat protokół sieci przemysłowej (fieldbus), pierwotnie zaprojektowany dla sterowników PLC. Jest dziś de facto standardem komunikacji między przemysłowymi urządzeniami elektronicznymi. Może działać przez linie szeregowe, Ethernet albo stos TCP/IP.

Słowa: Modbus, fieldbus, PLC, protokół przemysłowy, szeregowy, Ethernet, TCP/IP, otwarty
Źródło: https://en.wikipedia.org/wiki/Modbus (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie warianty Modbus wymienia Wikipedia?
Modbus RTU używa zwartej reprezentacji binarnej z kontrolą błędów CRC i strona nazywa go najpowszechniejszą implementacją. Modbus ASCII używa znaków ASCII, z dwukropkiem na początku, CR/LF na końcu i sumą LRC. Modbus TCP/IP działa w sieciach TCP/IP na porcie 502 i nie potrzebuje osobnej sumy kontrolnej, bo niższe warstwy chronią przed błędami.

Słowa: Modbus, RTU, ASCII, TCP/IP, port 502, CRC, LRC, suma kontrolna
Źródło: https://en.wikipedia.org/wiki/Modbus (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak działa model klient-serwer w Modbus według Wikipedii?
Klient inicjuje każdą transakcję, wysyłając żądanie, a serwer odsyła odpowiedź albo wyjątek. Dawniej mówiono master i slave. Na magistrali szeregowej jeden master może zaadresować do 247 slave'ów, które odpowiadają tylko na żądania. Slave'y nie komunikują się ze sobą. Master może też wysłać żądanie rozgłoszeniowe do wszystkich, na które nikt nie odpowiada.

Słowa: Modbus, klient, serwer, master, slave, 247, rozgłoszenie, żądanie, wyjątek
Źródło: https://en.wikipedia.org/wiki/Modbus (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Do czego najczęściej służy Modbus według Wikipedii?
Do łączenia nadzorującego komputera zakładu z zdalnymi jednostkami terminalowymi w systemach SCADA.

Słowa: Modbus, SCADA, RTU, nadzór, zdalne jednostki terminalowe
Źródło: https://en.wikipedia.org/wiki/Modbus (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest reverse proxy według Wikipedii?
Reverse proxy stoi przed jednym lub kilkoma zwykłymi serwerami WWW i wygląda dla klienta jak zwykły serwer. Przyjmuje żądania i przekazuje je do wewnętrznych serwerów. Strona podaje, że zwiększa skalowalność, wydajność, odporność i bezpieczeństwo.

Słowa: reverse proxy, serwer WWW, żądania, skalowalność, wydajność, odporność, bezpieczeństwo
Źródło: https://en.wikipedia.org/wiki/Reverse_proxy (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie korzyści daje reverse proxy według Wikipedii?
Równoważenie obciążenia, czyli rozdzielanie żądań między kilka serwerów. Zakończenie TLS, czyli odciążenie serwerów z szyfrowania, czasem sprzętowo. Buforowanie treści statycznej i dynamicznej. Ukrywanie serwerów zaplecza, które mogą stać za zaporą, a na zewnątrz widoczny jest tylko reverse proxy.

Słowa: reverse proxy, równoważenie obciążenia, TLS, buforowanie, cache, zapora, serwery zaplecza
Źródło: https://en.wikipedia.org/wiki/Reverse_proxy (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym różni się reverse proxy od forward proxy według Wikipedii?
Forward proxy jest używany, gdy klient jest ograniczony do wewnętrznej sieci i prosi serwer proxy o pobranie zasobów z internetu. Reverse proxy stoi przed serwerami i przekazuje do nich żądania klientów.

Słowa: reverse proxy, forward proxy, sieć wewnętrzna, internet, żądania klientów
Źródło: https://en.wikipedia.org/wiki/Reverse_proxy (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest nftables według Wikipedii?
nftables to podsystem jądra Linuksa do filtrowania i klasyfikowania pakietów sieciowych. Jest dostępny od Linuksa 3.13 (styczeń 2014) i ma licencję GPLv2. Zastępuje przestarzały komponent iptables w Netfilter.

Słowa: nftables, Linux, jądro, filtrowanie, iptables, Netfilter, GPLv2, 3.13
Źródło: https://en.wikipedia.org/wiki/Nftables (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie zalety i wady ma nftables względem iptables według Wikipedii?
Zaletami są mniej powielanego kodu i łatwiejsze rozszerzanie na nowe protokoły. Wadą jest brak obsługi DPI, którą dawało dopasowywanie łańcuchów w iptables, np. filtrowanie po SNI.

Słowa: nftables, iptables, zalety, wady, DPI, SNI, dopasowanie łańcuchów
Źródło: https://en.wikipedia.org/wiki/Nftables (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakim narzędziem konfiguruje się nftables według Wikipedii?
Konfiguruje się go narzędziem przestrzeni użytkownika `nft`. Legacy narzędzia, jak iptables, ip6tables, arptables i ebtables, mają własne programy. Strona podaje, że Debian 10 używa nftables jako domyślnego backendu filtrowania, z iptables-translate.

Słowa: nftables, nft, iptables, ip6tables, Debian 10, backend, iptables-translate
Źródło: https://en.wikipedia.org/wiki/Nftables (sprawdzono 2026-10, źródło angielskie, Wikipedia)
