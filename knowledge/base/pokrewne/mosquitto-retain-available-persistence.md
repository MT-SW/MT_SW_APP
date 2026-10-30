### Co robi opcja retain_available w Mosquitto?
Domyślnie `retain_available` ma wartość `true`. Gdy ustawisz `false`, broker nie obsługuje wiadomości retained, a klienci, którzy wyślą wiadomość z bitem retain, zostaną rozłączeni.

Słowa: mosquitto, retain_available, wiadomości retained, bit retain, rozłączenie, domyślnie true
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html#retain_available (sprawdzono 2026-10)

### Gdzie Mosquitto zapisuje dane przy włączonej persistence?
Domyślnie `persistence` ma wartość `false`, czyli dane są tylko w pamięci. Po włączeniu wbudowanej trwałości dane połączeń, subskrypcji i wiadomości zapisywane są w pliku `mosquitto.db` w katalogu `persistence_location`. Strona zaleca użycie trwałości opartej na wtyczce zamiast wbudowanej.

Słowa: mosquitto, persistence, mosquitto.db, persistence_location, pamięć, wtyczka, trwałość
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html#retain_available (sprawdzono 2026-10)
