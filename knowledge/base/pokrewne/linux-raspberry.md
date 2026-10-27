# Linux i Raspberry Pi: podstawy dla serwera i bramki

### Uprawnienia plików w Linuksie (chmod)
Każdy plik ma trzy grupy uprawnień: właściciel (user), grupa (group) i pozostali (others), a w każdej trzy bity: odczyt (4), zapis (2), wykonanie (1). Cyfra to suma, np. 7 = rwx, 6 = rw-, 5 = r-x, 4 = r--. Przykłady: chmod 664 plik (właściciel i grupa czytają i piszą, reszta tylko czyta), chmod 744 plik (właściciel wszystko, reszta czyta). Według tej samej tabeli 755 to rwxr-xr-x (typowe dla skryptów i katalogów), 644 to rw-r--r-- (zwykłe pliki), 600 to rw------- (pliki z hasłami). Zapis literowy: chmod g+w (dodaj zapis grupie), chmod a-w (odbierz zapis wszystkim), -R działa rekursywnie. Uprawnienia może zmieniać tylko właściciel i root.
Słowa: chmod, uprawnienia, 755, 644, 600, rwx, właściciel, grupa, root, permission denied, Linux pliki, wykonywalny skrypt
Źródło: https://en.wikipedia.org/wiki/Chmod (sprawdzono 2026-10; znaczenia 755/644/600 wyliczone z tabeli źródła)

### Usługi w Linuksie: co to jest systemd?
systemd uruchamia się pierwszy przy starcie i zarządza pozostałymi programami (usługami). Opis uruchamiania usługi jest w pliku jednostki (unit file) w formacie podobnym do .ini; typy to m.in. .service, .timer, .socket, .mount, .target. Jednostka .timer może zastąpić crona. Logi zbiera systemd-journald w plikach binarnych. Polecenie systemctl kontroluje usługi. Według wiedzy ogólnej (spoza źródła): systemctl status nazwa pokazuje stan, systemctl restart nazwa restartuje, systemctl enable --now nazwa włącza przy starcie i uruchamia, a journalctl -u nazwa -f pokazuje logi na żywo. Tak zarządza się np. usługą meshtasticd lub mosquitto.
Słowa: systemd, systemctl, usługa, service, journalctl, logi Linux, timer, cron, uruchomienie przy starcie, restart usługi, meshtasticd, mosquitto
Źródło: https://en.wikipedia.org/wiki/Systemd (sprawdzono 2026-10; konkretne polecenia to wiedza ogólna, nie z tego źródła)

### Raspberry Pi: który model i jakie zasilanie?
Pi 3 B (2016): 4 rdzenie 1,2 GHz, Wi-Fi i Bluetooth. Pi 4 (2019): 4 rdzenie 1,5 GHz, 1–8 GB RAM, gigabitowy Ethernet, zasilacz USB-C 15 W. Pi 5 (2023): 4 rdzenie 2,4 GHz, 1–16 GB RAM, PCIe (dyski NVMe), zasilacz USB-C 27 W. Zero 2 W (2021): 4 rdzenie 1 GHz, 512 MB RAM, Wi-Fi. Zasilanie 5 V. Przy dużym obciążeniu procesor zwalnia przy 85 °C lub przy wykrytym zbyt niskim napięciu, więc słaby zasilacz to częsta przyczyna problemów. System: Raspberry Pi OS (Debian, wersje 32 i 64 bit, Lite bez pulpitu), wgrywany narzędziem Raspberry Pi Imager. Karta microSD najlepiej klasy A2/C10.
Słowa: Raspberry Pi, Pi 4, Pi 5, Zero 2 W, zasilacz, 27 W, 15 W, undervoltage, throttling, Raspberry Pi OS, Imager, karta SD, serwer domowy
Źródło: https://en.wikipedia.org/wiki/Raspberry_Pi (sprawdzono 2026-10)

### Raspberry Pi GPIO: na co uważać przy podłączaniu radia LoRa?
40-pinowe złącze ma zasilanie 3,3 V i 5 V oraz interfejsy UART, SPI, I²C i I²S. Piny GPIO pracują w logice 3,3 V, więc moduł radiowy i czujniki muszą używać 3,3 V; podanie 5 V na pin może go uszkodzić (wiedza ogólna). Radio LoRa (np. SX1262) łączy się przez SPI, a meshtasticd ma gotowe opisy pinów popularnych nakładek (HAT).
Słowa: GPIO, 3,3 V, SPI, HAT, nakładka LoRa, Raspberry Pi radio, meshtasticd, piny, 40 pin, uszkodzenie pinu
Źródło: https://en.wikipedia.org/wiki/Raspberry_Pi i https://meshtastic.org/docs/meshtasticd/ (sprawdzono 2026-10; ostrzeżenie o 5 V to wiedza ogólna)
