# Informatyka: sieci, DNS, MQTT, Docker, magistrale

### Jak działa MQTT: broker, tematy, QoS, wiadomości zachowane?
MQTT ma dwa rodzaje uczestników: broker (serwer), który odbiera publikowane wiadomości i rozsyła je do subskrybentów, oraz klientów (od mikrokontrolera po serwer). Nadawca nie musi wiedzieć, kto odbiera. Wiadomości trafiają do hierarchii tematów, każda do jednego tematu. QoS 0: najwyżej raz, bez potwierdzenia. QoS 1: co najmniej raz, z powtórkami aż do potwierdzenia, więc możliwe duplikaty. QoS 2: dokładnie raz, z dwuetapowym uzgodnieniem i największym narzutem. Broker przechowuje tylko ostatnią wiadomość "retained" na temat i daje ją od razu nowemu subskrybentowi; zwykłe wiadomości bez subskrybentów przepadają. Klient może przy połączeniu ustawić wiadomość wysyłaną, gdy nagle zniknie (last will).
Słowa: MQTT, broker, temat, topic, publish, subscribe, QoS 0, QoS 1, QoS 2, retained, last will, subskrypcja
Źródło: https://en.wikipedia.org/wiki/MQTT (sprawdzono 2026-10)

### Porty, bezpieczeństwo i wersje MQTT
Domyślny port bez szyfrowania to 1883, z TLS 8883. Dane logowania idą otwartym tekstem, a sam protokół nie ma wbudowanych zabezpieczeń; szyfrowanie zapewnia TLS, do tego certyfikaty, użytkownik i hasło. Wersje: 3.1.1 (OASIS, 2014, też ISO/IEC 20922) i 5.0 (2019, dodaje kody przyczyn, współdzielone subskrypcje, wygasanie wiadomości i aliasy tematów). Popularne brokery: Eclipse Mosquitto, EMQX, HiveMQ. Specyfikacja opisuje tylko połączenie klient–broker; łączenie brokerów (bridge) zależy od implementacji.
Słowa: port 1883, port 8883, TLS, hasło MQTT, MQTT 5, 3.1.1, Mosquitto, EMQX, HiveMQ, bridge, szyfrowanie MQTT
Źródło: https://en.wikipedia.org/wiki/MQTT (sprawdzono 2026-10)

### Co to jest Docker i czym kontener różni się od maszyny wirtualnej?
Kontener to odizolowane środowisko z aplikacją, jej bibliotekami i konfiguracją. Obraz (image) to szablon tylko do odczytu, z którego uruchamia się kontenery; obrazy pobiera się z rejestrów (domyślnie Docker Hub). Dockerfile opisuje budowę obrazu (FROM, COPY, RUN, ENV, VOLUME, EXPOSE). Docker Compose uruchamia kilka kontenerów naraz z pliku YAML (docker-compose.yml). Wolumeny (volumes) przechowują dane poza kontenerem, więc przetrwają jego usunięcie. Kontenery dzielą jądro systemu gospodarza, więc są lżejsze niż maszyny wirtualne. Na macOS Docker działa w maszynie wirtualnej z Linuksem, na Windows przez WSL 2 (wcześniej Hyper-V). Docker Engine na Linuksie jest na licencji Apache 2.0, a Docker Desktop nie jest darmowy dla dużych firm.
Słowa: Docker, kontener, obraz, image, Dockerfile, Compose, docker-compose.yml, wolumen, volume, maszyna wirtualna, WSL 2, Docker Desktop, Docker Hub
Źródło: https://en.wikipedia.org/wiki/Docker_(software) (sprawdzono 2026-10)

### Jakie adresy IP są prywatne i co to jest NAT?
Prywatne zakresy IPv4: 10.0.0.0/8, 172.16.0.0/12 (172.16–172.31) i 192.168.0.0/16. Adres 169.254.x.x (link-local) komputer nadaje sobie sam, gdy nie dostał adresu z DHCP, więc zwykle oznacza problem z routerem lub kablem. Zakres 100.64.0.0/10 to CGNAT operatora: jeśli router dostaje taki adres "z zewnątrz", nie masz publicznego IP i przekierowanie portów nie zadziała. NAT pozwala wielu urządzeniom w domu dzielić jeden publiczny adres. Prywatne adresy nie są trasowane w internecie. NAT ukrywa urządzenia, ale nie zastępuje zapory. W IPv6 odpowiednikiem są adresy ULA fd00::/8.
Słowa: adres IP, prywatny, 192.168, 10.0, 172.16, 169.254, CGNAT, 100.64, NAT, publiczne IP, przekierowanie portów, DHCP, IPv6, ULA
Źródło: https://en.wikipedia.org/wiki/Private_network (sprawdzono 2026-10; uwagi o 169.254 i CGNAT to praktyczne wnioski z opisu zakresów)

### Jak działa DNS i dlaczego zmiany nie działają od razu?
DNS zamienia nazwy (np. mt-sw.pl) na adresy IP. Rekordy: A (adres IPv4), AAAA (IPv6), CNAME (alias), MX (serwer poczty), NS (serwery nazw domeny), PTR (odwrotne: adres na nazwę), TXT (tekst). Każdy rekord ma TTL w sekundach, czyli czas ważności w pamięci podręcznej; dopóki nie wygaśnie, część ludzi widzi stare dane. Przeglądarki też mają własną krótką pamięć (ok. minuty). Zwykłe zapytania idą przez UDP port 53, duże odpowiedzi przez TCP 53. Szyfrowane: DNS over TLS (port 853) i DNS over HTTPS (port 443).
Słowa: DNS, rekord A, AAAA, CNAME, MX, TXT, PTR, TTL, propagacja DNS, port 53, DoH, DoT, domena, nie działa nowa domena
Źródło: https://en.wikipedia.org/wiki/Domain_Name_System (sprawdzono 2026-10)

### UART (port szeregowy): jak działa i co trzeba ustawić?
UART wysyła bajt bit po bicie, otoczony bitem startu i stopu. Obie strony muszą mieć tę samą prędkość (baud), długość znaku, parzystość i liczbę bitów stopu. 8N1 to 8 bitów danych, bez parzystości, 1 bit stopu; wtedy liczba znaków na sekundę to prędkość podzielona przez 10. UART pracuje na poziomach logicznych; do RS-232 lub RS-485 potrzebny jest konwerter. Dwa UART-y o tym samym napięciu (np. 3 V i 3 V) łączy się bezpośrednio, przy różnych napięciach potrzebny jest konwerter poziomów. Komputery zamiast portów COM używają mostków USB–UART. Według wiedzy ogólnej (nie z tego źródła) TX jednego urządzenia łączy się z RX drugiego i odwrotnie, a masy muszą być wspólne.
Słowa: UART, port szeregowy, baud, 8N1, TX, RX, RS-232, RS-485, konwerter poziomów, 3,3 V, 5 V, USB-UART, COM
Źródło: https://en.wikipedia.org/wiki/Universal_asynchronous_receiver-transmitter (sprawdzono 2026-10; zdanie o krzyżowaniu TX/RX to wiedza ogólna)

### I²C: jak podłączyć czujniki i co zrobić, gdy nie działają?
Dwie linie: SDA (dane) i SCL (zegar), typowo 3,3 V lub 5 V. Obie są typu otwarty dren, więc każda potrzebuje rezystora podciągającego (pull-up). Adresy są 7-bitowe; dwa takie same czujniki kolidują, chyba że mają piny wyboru adresu albo użyjesz multipleksera. Prędkości: 100 kbit/s i 400 kbit/s; wszystkie urządzenia muszą obsłużyć najwyższą używaną. Pojemność magistrali do 400 pF ogranicza długość do kilku metrów; dłuższe przewody wymagają wolniejszego zegara lub buforów. Gdy jedno urządzenie trzyma linię w stanie niskim, cała magistrala staje; pomaga reset lub odłączenie zasilania. Przy błędach sprawdź rezystory, długość przewodów, kolizje adresów i prędkość.
Słowa: I2C, SDA, SCL, pull-up, rezystor podciągający, adres czujnika, kolizja adresów, 400 kHz, długość przewodów, czujnik nie działa, magistrala zawieszona
Źródło: https://en.wikipedia.org/wiki/I%C2%B2C (sprawdzono 2026-10)
