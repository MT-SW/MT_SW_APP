<!-- Baza wiedzy MT_SW: konfiguracja Raspberry Pi do pracy jako serwer lub brama. Tylko informacyjnie. -->
# Raspberry Pi: konfiguracja systemu i plik config.txt

### Gdzie na Raspberry Pi są ustawienia rozruchu i jak je zmienić?
Na Raspberry Pi większość ustawień sprzętowych i rozruchowych zmienia się w pliku /boot/firmware/config.txt. To plik, który firmware czyta przy starcie. Zawiera ustawienia audio, HDMI, opcje rozruchu, GPIO, podkręcanie, pamięć, wideo i kamerę. Zmiany zwykle robi się przez edycję tego pliku i innych powiązanych plików. Przed edycją zrób kopię tego pliku, bo błąd w nim może utrudnić start. Po zmianach trzeba zrestartować urządzenie.
Słowa: raspberry pi, raspi, config.txt, /boot/firmware, rozruch, boot, firmware, gpio, hdmi, pamięć, konfiguracja, restart, kopia pliku
Źródło: https://www.raspberrypi.com/documentation/computers/configuration.html (sprawdzono 2026-10; strona jest długa, opisano początek i spis treści)

### Co robi narzędzie raspi-config i kiedy jest najwygodniejsze?
Narzędzie raspi-config uruchamia się w terminalu poleceniem sudo raspi-config. Ma menu do zarządzania podstawowymi ustawieniami systemu i jest przydatne na wersji Raspberry Pi OS Lite oraz przy pracy bez monitora (headless). Pozwala ustawić to samo, co wpisujesz ręcznie w configu, ale bez ryzyka literówki. Dla początkującego to bezpieczniejsza droga, a ręczną edycję config.txt zostaw na przypadki, których menu nie obejmuje. Dokładny zakres pozycji menu zależy od wersji systemu, więc sprawdź je na swoim obrazie.
Słowa: raspi-config, sudo raspi-config, menu, raspberry pi os lite, headless, bez monitora, konfiguracja, ustawienia systemu, terminal
Źródło: https://www.raspberrypi.com/documentation/computers/configuration.html (sprawdzono 2026-10)
