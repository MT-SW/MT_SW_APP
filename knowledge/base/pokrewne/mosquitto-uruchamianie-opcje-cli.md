### Co robi uruchomienie mosquitto bez pliku konfiguracyjnego?
Jeśli nie podasz flagi `-c`, broker słucha na porcie 1883 na interfejsie loopback i używa domyślnych wartości opisanych w mosquitto.conf(5).

Słowa: mosquitto, -c, config, port 1883, loopback, domyślne wartości
Źródło: https://mosquitto.org/man/mosquitto-8.html (sprawdzono 2026-10)

### Do czego służy flaga -d w mosquitto?
Flaga `-d` (`--daemon`) uruchamia mosquitto w tle jako demon. Strona podaje, że reszta zachowania pozostaje taka sama.

Słowa: mosquitto, -d, daemon, demon, tło, uruchamianie
Źródło: https://mosquitto.org/man/mosquitto-8.html (sprawdzono 2026-10)

### Ile nasłuchów można otworzyć flagą -p w mosquitto?
Flagę `-p` można podać do 10 razy, żeby otworzyć wiele gniazd nasłuchu. Od wersji 2.0 nasłuch z `-p` wiąże się tylko z interfejsem loopback. Jeśli ten sam nasłuch jest w pliku konfiguracyjnym, opcje `-p` są ignorowane. W wersjach 1.6.x i starszych nasłuch wiązał się ze wszystkimi interfejsami.

Słowa: mosquitto, -p, port, loopback, listener, wersja 2.0, nasłuch
Źródło: https://mosquitto.org/man/mosquitto-8.html (sprawdzono 2026-10)

### Co robi flaga -v w mosquitto?
Flaga `-v` (`--verbose`) włącza szczegółowe logowanie i odpowiada ustawieniu `log_type all` w pliku konfiguracyjnym. Nadpisuje opcje logowania z pliku.

Słowa: mosquitto, -v, verbose, log_type, logowanie, all
Źródło: https://mosquitto.org/man/mosquitto-8.html (sprawdzono 2026-10)
