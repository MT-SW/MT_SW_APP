### Czym jest Zabbix według Wikipedii?
Zabbix to otwartoźródłowe oprogramowanie do monitorowania sieci, serwerów, maszyn wirtualnych i usług chmurowych. Zbiera metryki, zapisuje je w bazie relacyjnej i udostępnia alerty, wizualizacje i raporty przez interfejs webowy.

Słowa: Zabbix, monitoring, sieci, serwery, maszyny wirtualne, chmura, alerty, raporty
Źródło: https://en.wikipedia.org/wiki/Zabbix (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Z jakich części składa się Zabbix według Wikipedii?
Serwer zbiera dane od agentów i innych źródeł, ocenia warunki wyzwalaczy, wysyła alerty i zapisuje dane w bazie relacyjnej. Agent zbiera metryki systemowe, np. CPU, pamięć, dysk i sieć. Pierwszy agent jest napisany w C, a agent2 w Go. Proxy zbiera dane w zdalnych lokalizacjach i przekazuje je do serwera. Frontend w PHP daje panele, mapy, ekrany i raporty. Brama Java obsługuje monitoring JMX.

Słowa: Zabbix, serwer, agent, agent2, proxy, frontend, brama Java, JMX, wyzwalacze
Źródło: https://en.wikipedia.org/wiki/Zabbix (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie bazy danych obsługuje Zabbix według Wikipedii?
Strona wymienia PostgreSQL, MySQL/MariaDB, Oracle i TimescaleDB.

Słowa: Zabbix, baza danych, PostgreSQL, MySQL, MariaDB, Oracle, TimescaleDB
Źródło: https://en.wikipedia.org/wiki/Zabbix (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czy Zabbix wymaga agentów według Wikipedii?
Zabbix używa obu podejść. Agenty zbierają metryki systemowe, a jednocześnie obsługuje sprawdzenia bez agenta, np. SNMP, IPMI, HTTP/HTTPS, SSH i Telnet.

Słowa: Zabbix, agent, bez agenta, SNMP, IPMI, HTTP, SSH, Telnet
Źródło: https://en.wikipedia.org/wiki/Zabbix (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Jellyfin według Wikipedii?
Jellyfin to darmowy i otwartoźródłowy serwer multimediów. Organizuje, zarządza i udostępnia cyfrowe pliki multimedialne urządzeniom w sieci. Działa na Linuksie, Windows i macOS, a aplikacje klienckie są na wielu innych urządzeniach.

Słowa: Jellyfin, serwer multimediów, open source, Linux, Windows, macOS, sieć, pliki
Źródło: https://en.wikipedia.org/wiki/Jellyfin (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Na jakiej licencji jest Jellyfin według Wikipedii?
Strona podaje licencję GPLv2 w ramce informacyjnej.

Słowa: Jellyfin, GPLv2, licencja, otwarte źródło
Źródło: https://en.wikipedia.org/wiki/Jellyfin (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czy Jellyfin wymaga subskrypcji według Wikipedii?
Nie. Strona podaje, że Jellyfin nie ma modelu konsumpcji opartego na subskrypcji, bo działa jako samodzielny serwer.

Słowa: Jellyfin, subskrypcja, samodzielny serwer, bez opłat
Źródło: https://en.wikipedia.org/wiki/Jellyfin (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Pi-hole według Wikipedii?
Pi-hole to aplikacja w Linuksie do blokowania reklam i trackerów na poziomie sieci. Działa jako DNS sinkhole i opcjonalnie jako serwer DHCP dla prywatnej sieci.

Słowa: Pi-hole, blokowanie reklam, tracker, DNS sinkhole, DHCP, Linux, sieć prywatna
Źródło: https://en.wikipedia.org/wiki/Pi-hole (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak Pi-hole blokuje reklamy według Wikipedii?
Porównuje zapytania DNS z listami domen reklamowych i śledzących. Gdy zapytanie pasuje do listy, Pi-hole odmawia rozwiązania domeny i zwraca urządzeniu fikcyjny adres. Ponieważ działa na poziomie sieci, blokuje reklamy także na Androidzie, iOS i telewizorach smart, a nie tylko w jednej przeglądarce.

Słowa: Pi-hole, zapytanie DNS, lista domen, adres fikcyjny, poziom sieci, Android, iOS, smart TV
Źródło: https://en.wikipedia.org/wiki/Pi-hole (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Na jakim sprzęcie działa Pi-hole według Wikipedii?
Strona podaje, że Pi-hole jest zaprojektowany dla urządzeń wbudowanych o niskim poborze mocy, np. Raspberry Pi, ale może działać na niemal każdym systemie Linux.

Słowa: Pi-hole, Raspberry Pi, niski pobór mocy, Linux, urządzenie wbudowane
Źródło: https://en.wikipedia.org/wiki/Pi-hole (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest DNSSEC według Wikipedii?
DNSSEC to zestaw rozszerzeń IETF, który dodaje bezpieczeństwo danych DNS wymienianych w sieciach IP. Dodaje uwierzytelnianie danych przez podpisy cyfrowe, więc resolver może sprawdzić, czy informacja nie została zmieniona i zgadza się z tym, co opublikował właściciel strefy.

Słowa: DNSSEC, IETF, DNS, podpisy cyfrowe, uwierzytelnianie, integralność, resolver
Źródło: https://en.wikipedia.org/wiki/Domain_Name_System_Security_Extensions (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czego DNSSEC nie zapewnia według Wikipedii?
DNSSEC nie zapewnia poufności, bo odpowiedzi są uwierzytelnione, ale nie zaszyfrowane. Nie chroni też bezpośrednio przed atakami DoS.

Słowa: DNSSEC, poufność, szyfrowanie, DoS, dostępność, ograniczenia
Źródło: https://en.wikipedia.org/wiki/Domain_Name_System_Security_Extensions (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak działa łańcuch zaufania w DNSSEC według Wikipedii?
Weryfikacja zaczyna się od kotwicy zaufania, zwykle zweryfikowanych kluczy publicznych strefy głównej DNS. Każdy poziom łączy się z następnym rekordami DS w strefie nadrzędnej, które wskazują rekordy DNSKEY w strefie podrzędnej. Łańcuch schodzi do rekordów domeny. Bez pełnego łańcucha odpowiedzi nie da się bezpiecznie uwierzytelnić.

Słowa: DNSSEC, łańcuch zaufania, kotwica zaufania, strefa główna, rekord DS, DNSKEY, uwierzytelnienie
Źródło: https://en.wikipedia.org/wiki/Domain_Name_System_Security_Extensions (sprawdzono 2026-10, źródło angielskie, Wikipedia)
