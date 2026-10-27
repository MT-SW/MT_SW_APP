# Krótkofalarstwo, GPS, Docker, SSH i ochrona przed słońcem

### Czym jest krótkofalarstwo i czym różni się od radia bez licencji?
Radio amatorskie to niekomercyjne używanie wydzielonych pasm do łączności, eksperymentów, nauki, zawodów i pomocy w nagłych wypadkach. Ramy ustala ITU, zasady i licencje krajowe: trzeba zdać egzamin (teoria radia, elektronika, praktyka, przepisy), często są klasy licencji o rosnących uprawnieniach i mocy. Licencja zwykle obowiązuje tylko w kraju wydania, chyba że umowa wzajemna mówi inaczej. Znak wywoławczy podaje się co najmniej co dziesięć minut i na koniec rozmowy. W odróżnieniu od CB, FRS czy PMR446 krótkofalowiec ma szersze pasma, więcej emisji i większą moc, może sam budować nadajniki i wybierać dowolną częstotliwość w paśmie. Szyfrowanie jest co do zasady zabronione. Sieć Meshtastic na ISM to inna sprawa, bo nie wymaga licencji.
Słowa: krótkofalarstwo, radio amatorskie, licencja, znak wywoławczy, egzamin, CB, PMR446, FRS, pasma amatorskie, ITU, SP
Źródło: https://en.wikipedia.org/wiki/Amateur_radio (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### Jakimi emisjami i pasmami pracują krótkofalowcy?
Pasma KF (krótkie fale) pozwalają na łączności z całym światem, UKF (VHF/UHF) zwykle lokalne i regionalne, a mikrofale na amatorską telewizję i szybkie łącza danych. Głos: FM, SSB, AM; dane i tekst: telegrafia (CW), RTTY, packet radio, PSK31, FT8; obraz: SSTV i ATV; do tego łączność satelitarna, odbicie od Księżyca, rozproszenie od meteorów i systemy połączone z internetem jak EchoLink. W razie klęski krótkofalowcy często zapewniają łączność na zasilaniu awaryjnym, gdy komercyjne sieci nie działają.
Słowa: KF, UKF, SSB, FM, CW, FT8, RTTY, packet radio, EchoLink, łączność awaryjna, satelity amatorskie, emisje
Źródło: https://en.wikipedia.org/wiki/Amateur_radio (sprawdzono 2026-10)

### Jak działa GPS i jaka jest dokładność?
Satelity nadają swój czas i położenie, odbiornik mierzy czasy dotarcia sygnałów i wylicza odległości. Do wyznaczenia trzech współrzędnych i błędu zegara potrzeba co najmniej czterech satelitów. Konstelacja liczy nominalnie 24 satelity na sześciu orbitach, przy czystym horyzoncie widać zwykle ok. dziewięciu. Zwykłe odbiorniki mają około 5 m dokładności (od 2000 roku bez umyślnego zakłócania), odbiorniki L5 ok. 30 cm, geodezyjne do 2 cm. Pierwszy odczyt po włączeniu może trwać do 30 s, a pełny almanach zbiera się 12,5 minuty. Sygnał jest słaby, wymaga widoczności nieba i można go zagłuszać. W budynkach, wąwozach i pod gęstym lasem pozycja pogarsza się lub znika.
Słowa: GPS, GNSS, satelity, dokładność, zimny start, almanach, widoczność nieba, zagłuszanie, pozycja, odbiornik GPS, L5
Źródło: https://en.wikipedia.org/wiki/Global_Positioning_System (sprawdzono 2026-10)

### Docker: obraz, kontener, wolumen i compose
Obraz to szablon tylko do odczytu, z którego powstaje kontener; domyślnie szuka się go w Docker Hub. Kontener to odizolowane środowisko z własnym oprogramowaniem, bibliotekami i konfiguracją. Wolumen trzyma dane niezależnie od kontenera, więc przeżywają jego usunięcie. Compose opisuje wiele usług w pliku docker-compose.yml i jednym poleceniem je uruchamia. W odróżnieniu od maszyn wirtualnych kontenery współdzielą jądro hosta, więc są lżejsze; na macOS Docker uruchamia je wewnątrz maszyny wirtualnej z Linuksem. Źródło nie podaje konkretnych poleceń, więc nie wymieniam ich z pamięci.
Słowa: Docker, kontener, obraz, wolumen, docker compose, docker-compose.yml, Docker Hub, maszyna wirtualna, homelab, meshtasticd w Dockerze
Źródło: https://en.wikipedia.org/wiki/Docker_(software) (sprawdzono 2026-10)

### SSH: klucze zamiast haseł i bezpieczne użycie
SSH to zaszyfrowany protokół zdalnego logowania i uruchamiania poleceń, domyślnie TCP 22; zastąpił telnet i rsh. Logować można hasłem lub kluczem: parę kluczy tworzy ssh-keygen, klucz publiczny trafia na serwer, prywatny zostaje u Ciebie i nigdy nie jest wysyłany; warto go zabezpieczyć hasłem. Kopiować pliki można przez scp, sftp lub rsync po SSH; tunelowanie portów i serwer proxy SOCKS też są możliwe. Bezpieczeństwo: nie akceptuj nieznanego klucza serwera bez sprawdzenia, ~/.ssh/authorized_keys ma być zapisywalny tylko przez właściciela, używaj SSH-2, unikaj przestarzałych algorytmów (RC4, 3DES, DES, MD5) i aktualizuj OpenSSH (od 9.6 załatana jest luka Terrapin).
Słowa: SSH, ssh-keygen, klucz publiczny, scp, sftp, rsync, port 22, tunel, authorized_keys, OpenSSH, zdalny dostęp, Terrapin
Źródło: https://en.wikipedia.org/wiki/Secure_Shell (sprawdzono 2026-10)

