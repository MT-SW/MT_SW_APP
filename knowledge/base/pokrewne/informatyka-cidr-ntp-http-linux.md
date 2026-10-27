# Informatyka: CIDR, NTP, kody HTTP, polecenia Linuksa

### CIDR: jak policzyć liczbę adresów w podsieci?
Według Wikipedii zapis CIDR to adres, ukośnik i długość prefiksu (liczba wiodących jedynek maski), np. 198.51.100.14/24. Liczba adresów to 2^(32 − prefiks) dla IPv4 (dla IPv6 2^(128 − prefiks)): /24 to 256, /22 to 1024, /16 to 65 536, /29 to 8, /30 to 4 adresy. W podsieciach większych niż /31 odejmuje się 2 adresy (adres sieci i broadcast), więc /24 daje 254 hosty, a /30 tylko 2. /31 służy do łączy punkt–punkt, /32 to pojedynczy host. Maski: /24 to 255.255.255.0, /22 to 255.255.252.0, /16 to 255.255.0.0, /30 to 255.255.255.252.
Słowa: cidr podsieć maska prefiks /24 /16 /30 adresy ip broadcast hosty obliczenia sieć
Źródło: https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing (sprawdzono 2026-10)

### NTP: synchronizacja czasu przez sieć
NTP synchronizuje zegary komputerów z dokładnością do kilku milisekund względem UTC. Działa przez UDP, zwykle na porcie 123. Dokładność: w internecie zwykle kilkadziesiąt ms, w LAN w dobrych warunkach lepiej niż 1 ms; asymetryczne trasy i przeciążenie mogą dać błędy rzędu 100 ms. Warstwy (stratum): 0 to zegary atomowe i odbiorniki GNSS, 1 to komputery z nimi zsynchronizowane, 2 i 3 synchronizują się przez sieć, 16 oznacza brak synchronizacji; niższy numer nie zawsze znaczy dokładniej. SNTP to uproszczona wersja (jeden serwer, bez zaawansowanych algorytmów), wygodna dla prostych urządzeń IoT, ale mniej dokładna.
Słowa: ntp czas synchronizacja udp 123 stratum sntp zegar serwer czasu iot
Źródło: https://en.wikipedia.org/wiki/Network_Time_Protocol (sprawdzono 2026-10)

### Kody odpowiedzi HTTP: co znaczą?
Grupy: 1xx informacyjne, 2xx sukces, 3xx przekierowania, 4xx błędy klienta, 5xx błędy serwera. Najczęstsze: 200 OK; 301 przeniesiono na stałe; 302 tymczasowo pod innym adresem; 400 błędne żądanie; 401 wymagane uwierzytelnienie; 403 odmowa dostępu mimo poprawnego żądania; 404 nie znaleziono zasobu; 429 zbyt wiele żądań; 500 ogólny błąd serwera; 502 zła odpowiedź od serwera nadrzędnego; 503 serwer przeciążony lub w przerwie technicznej (zwykle tymczasowo); 504 serwer pośredniczący nie doczekał się odpowiedzi.
Słowa: http kod 200 301 302 400 401 403 404 429 500 502 503 504 błąd serwera przeglądarka api
Źródło: https://en.wikipedia.org/wiki/List_of_HTTP_status_codes (sprawdzono 2026-10)

### Podstawowe polecenia Linuksa: lista z opisem
Pliki: ls (zawartość katalogu), cd (zmiana katalogu), chmod (uprawnienia), du (zajęte miejsce katalogu), df (wolne miejsce na dyskach), tar (archiwa, np. tar -czf tworzy, tar -xzf rozpakowuje). Szukanie: grep (wzorce tekstu w plikach lub wyniku innego polecenia), find (pliki wg nazwy, daty, rozmiaru). Procesy: ps (aktywne procesy), top (na żywo), kill PID (kończy proces; kill -9 wymusza), killall (po nazwie), free (pamięć), uname (informacje o systemie). Sieć: ping, ssh (bezpieczne połączenie ze zdalnym komputerem).
Słowa: linux polecenia ls cd grep find tar df du ps top kill free uname ping ssh terminal konsola
Źródło: https://www.curiaweb.ch/knowledgebase/9263/A-Selection-of-Important-Linux-Commands.html (sprawdzono 2026-10)
