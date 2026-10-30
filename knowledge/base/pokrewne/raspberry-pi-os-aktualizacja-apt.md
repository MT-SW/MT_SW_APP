<!-- Baza wiedzy MT_SW: codzienna aktualizacja Raspberry Pi OS przez APT i ostrożne użycie rpi-update. Tylko informacyjnie. -->
# Raspberry Pi OS: aktualizacja przez APT

### Jak zaktualizować Raspberry Pi OS: apt update i apt full-upgrade?
Oficjalna dokumentacja zaleca aktualizować system przez APT, a nie przez rpi-update. Najpierw sudo apt update odświeża listę pakietów ze źródeł z /etc/apt/sources.list i /etc/apt/sources.list.d/. Potem sudo apt full-upgrade instaluje nowe wersje, w tym jądro i stabilny firmware. Dokumentacja podaje full-upgrade zamiast zwykłego upgrade, bo Raspberry Pi OS częściej zmienia zależności niż Debian. Ta aktualizacja nie przenosi na nowe duże wydanie systemu. Przejście z Bookworm na Trixie wymaga nowego obrazu, a nie aktualizacji w miejscu.
Słowa: raspberry pi aktualizacja, apt update, apt full-upgrade, upgrade, jądro, firmware, raspi-firmware, bookworm, trixie, aktualizacja systemu, sudo apt, pakiety, zależności
Źródło: https://www.raspberrypi.com/documentation/computers/os.html (sprawdzono 2026-10; oficjalna dokumentacja Raspberry Pi; zalecenie migracji przez nowy obraz z oficjalnej dokumentacji usage)

### Co zrobić przed aktualizacją Raspberry Pi i kiedy użyć rpi-update?
Przed aktualizacją sprawdź wolne miejsce poleceniem df -h, bo full-upgrade może zgłosić, że potrzebuje więcej. Jeśli zabraknie miejsca, zwolnij je poleceniem sudo apt clean, które kasuje pobrane pliki .deb z pamięci podręcznej. Narzędzie rpi-update instaluje przedpremierowe jądro i firmware. Dokumentacja mówi, że nadaje się głównie dla programistów i testerów albo gdy zaleci je inżynier Raspberry Pi, bo może destabilizować system albo uniemożliwić start. Przed użyciem zrób kopię zapasową. Rutynowe aktualizacje rób przez APT. Wniosek dotyczący serwera domowego, że trzeba mieć kopię przed każdą aktualizacją, jest z ogólnej wiedzy.
Słowa: df -h, miejsce na dysku, apt clean, rpi-update, przedpremierowe firmware, kopia zapasowa raspberry, niestabilny system, nie startuje, aktualizacja firmware, zwolnienie miejsca, pamięć podręczna apt
Źródło: https://www.raspberrypi.com/documentation/computers/os.html (sprawdzono 2026-10; oficjalna dokumentacja Raspberry Pi; zalecenie kopii z ogólnej wiedzy)
