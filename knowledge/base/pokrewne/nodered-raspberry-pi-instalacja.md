<!-- Baza wiedzy MT_SW: instalacja i uruchamianie Node-RED na Raspberry Pi. Tylko informacyjnie. -->
# Node-RED na Raspberry Pi: instalacja i usługa

### Jak zainstalować Node-RED na Raspberry Pi oficjalnym skryptem?
Oficjalny skrypt Node-RED instaluje Node.js, npm i Node-RED na systemach opartych na Debianie, w tym Ubuntu i Diet-Pi. Według dokumentacji skrypt instaluje Node.js v22 LTS, jeśli go nie ma, albo podnosi go do v20 lub nowszego, gdy jest. Skrypt konfiguruje też usługę. Przed uruchomieniem na nowym systemie dokumentacja zaleca doinstalować build-essential, git i curl, żeby npm mógł zbudować moduły binarne. Skrypt uruchamia się poleceniem bash <(curl -sL ...) z adresu podanego w dokumentacji. Wersję Node-RED, jaka pojawi się po instalacji, sprawdź w logach, bo zależy od czasu wydania.
Słowa: node-red raspberry pi, instalacja node-red, skrypt instalacyjny, nodejs, npm, node 22, build-essential, git, curl, diet-pi, debian, ubuntu, linux installers
Źródło: https://nodered.org/docs/getting-started/raspberrypi (sprawdzono 2026-10; oficjalna dokumentacja Node-RED)

### Jak uruchomić Node-RED na Raspberry Pi z mniejszą ilością pamięci?
Dokumentacja podaje polecenie node-red-pi z argumentem --max-old-space-size=256 do uruchomienia w terminalu. Argument każe Node.js szybciej zwalniać nieużywaną pamięć, co pomaga przy małej ilości RAM w Raspberry Pi. Ta opcja dotyczy uruchamiania ręcznego, a nie usługi działającej w tle. Dokumentacja nie podaje, ile pamięci zużywa Node-RED w praktyce, więc tej liczby nie podaję. Przy niewielkim modelu, np. Pi Zero, warto zacząć od tej wartości i obserwować zużycie.
Słowa: node-red pamięć, max-old-space-size, 256, node-red-pi, mało ram, raspberry pi zero, uruchomienie ręczne, zwalnianie pamięci, pi 3, nodejs pamięć
Źródło: https://nodered.org/docs/getting-started/raspberrypi (sprawdzono 2026-10; oficjalna dokumentacja Node-RED; zużycie pamięci nie podane w źródle)

### Jak zarządzać usługą Node-RED na Raspberry Pi i włączyć autostart?
Polecenia node-red-restart restartują usługę, node-red-log pokazuje jej dziennik, a node-red-stop ją zatrzymuje. Autostart po włączeniu lub restarcie Pi włącza się poleceniem sudo systemctl enable nodered.service, a wyłącza sudo systemctl disable nodered.service. Edytor otworzysz z innego komputera pod adresem http://nazwa_hosta:1880 albo pod adresem IP Pi. Adres IP sprawdzisz poleceniem hostname -I na Pi. Na samym Pi z pulpitem używa się http://localhost:1880.
Słowa: node-red restart, node-red-log, node-red-stop, systemctl enable nodered, autostart node-red, port 1880, hostname -I, edytor node-red, localhost 1880, usługa nodered
Źródło: https://nodered.org/docs/getting-started/raspberrypi (sprawdzono 2026-10; oficjalna dokumentacja Node-RED)
