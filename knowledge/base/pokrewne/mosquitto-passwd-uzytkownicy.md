### Jak utworzyć plik haseł dla Mosquitto za pomocą mosquitto_passwd?
Polecenie `mosquitto_passwd -c <plik> <użytkownik>` tworzy nowy plik haseł. Flaga `-c` nadpisuje istniejący plik, więc trzeba jej używać tylko przy pierwszym utworzeniu.

Słowa: mosquitto, mosquitto_passwd, plik haseł, -c, użytkownik, passwd
Źródło: https://mosquitto.org/man/mosquitto_passwd-1.html (sprawdzono 2026-10)

### Jak dodać użytkownika albo zmienić hasło w Mosquitto?
Polecenie bez flagi `-c`, np. `mosquitto_passwd /etc/mosquitto/passwd ral`, dodaje użytkownika do istniejącego pliku albo aktualizuje hasło. Hasło jest pytane interaktywnie.

Słowa: mosquitto, mosquitto_passwd, dodawanie użytkownika, zmiana hasła, passwd
Źródło: https://mosquitto.org/man/mosquitto_passwd-1.html (sprawdzono 2026-10)

### Dlaczego flaga -b w mosquitto_passwd jest ryzykowna?
Flaga `-b` przyjmuje hasło jako argument z linii poleceń. Strona ostrzega, że hasło jest wtedy widoczne w linii poleceń i w historii poleceń, więc trzeba jej używać ostrożnie.

Słowa: mosquitto, mosquitto_passwd, -b, batch, historia poleceń, hasło
Źródło: https://mosquitto.org/man/mosquitto_passwd-1.html (sprawdzono 2026-10)

### Jak usunąć użytkownika z pliku haseł Mosquitto?
Flaga `-D` usuwa wskazanego użytkownika z pliku haseł. Składnia to `mosquitto_passwd -D <plik> <użytkownik>`.

Słowa: mosquitto, mosquitto_passwd, -D, usuwanie użytkownika, plik haseł
Źródło: https://mosquitto.org/man/mosquitto_passwd-1.html (sprawdzono 2026-10)

### Jakie algorytmy haszowania hasła obsługuje mosquitto_passwd?
Domyślnym hashem jest argon2id. Opcja `-H` przyjmuje też `sha512-pbkdf2` i `sha512`, przy czym `sha512` jest dla Mosquitto 1.6 i starszych. Nazwa użytkownika nie może zawierać znaku `:`.

Słowa: mosquitto, mosquitto_passwd, argon2id, sha512, -H, haszowanie, dwukropek
Źródło: https://mosquitto.org/man/mosquitto_passwd-1.html (sprawdzono 2026-10)
