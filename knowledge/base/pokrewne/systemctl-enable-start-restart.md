### Czym różni się systemctl enable od start?
Polecenie `enable` tworzy dowiązania symboliczne z sekcji `[Install]` i przeładowuje konfigurację menedżera, ale nie uruchamia jednostki. Żeby zrobić oba naraz, dodaje się `--now`. Analogicznie `disable` usuwa dowiązania i nie zatrzymuje jednostki, chyba że podasz `--now`.

Słowa: systemctl, enable, disable, start, --now, dowiązania, unit
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemctl.html (sprawdzono 2026-10)

### Czym różni się restart od try-restart w systemctl?
Polecenie `restart` zatrzymuje, a potem uruchamia jednostki, a te nieuruchomione też startuje. Polecenie `try-restart` restartuje tylko jednostki, które już działają, a dla nieuruchomionych nic nie robi.

Słowa: systemctl, restart, try-restart, działająca usługa, unit
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemctl.html (sprawdzono 2026-10)

### Czym różni się reload od daemon-reload w systemd?
Polecenie `reload` prosi jednostki o przeładowanie ich konfiguracji specyficznej dla usługi, ale nie czyta ponownie pliku jednostki. Polecenie `daemon-reload` przeładowuje konfigurację menedżera systemd, uruchamia generatory, wczytuje pliki jednostek i odbudowuje drzewo zależności. To nie jest to samo co `reload`.

Słowa: systemctl, reload, daemon-reload, plik jednostki, generatory, zależności
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemctl.html (sprawdzono 2026-10)

### Co się dzieje przy systemctl stop, gdy zatrzymanie się nie uda?
Polecenie `stop` kończy się błędem, gdy jednostka nie istnieje albo gdy `RefuseManualStop=` tego zabrania. Jeśli same polecenia zatrzymujące zawiodą, menedżer wymusza zakończenie jednostki i wtedy błąd nie jest zgłaszany.

Słowa: systemctl, stop, RefuseManualStop, wymuszenie, zakończenie jednostki
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemctl.html (sprawdzono 2026-10)
