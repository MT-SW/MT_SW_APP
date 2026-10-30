# meshtasticd (Linux), aplikacje, regiony i tabela presetów

### Co to jest meshtasticd i jak go skonfigurować?
meshtasticd to wersja firmware Meshtastic działająca jako usługa na komputerze (Linux, także Raspberry Pi; USB lub SPI do modułu LoRa). Domyślna konfiguracja to /etc/meshtasticd/config.yaml; plik config.yaml w bieżącym katalogu ma pierwszeństwo, a plik podany opcją -c największe. Sprzęt LoRa wybierasz tak: obejrzyj gotowe pliki w /etc/meshtasticd/available.d, skopiuj pasujący (np. lora-meshstick-1262.yaml) do /etc/meshtasticd/config.d/ i zrestartuj usługę. Plik YAML jest wrażliwy na wcięcia. Opcje ręcznego uruchomienia: -c plik konfiguracji, -d katalog danych, -e wyczyść dane, -h adres MAC, -p port API TCP. Bluetooth w meshtasticd nie działa.
Słowa: meshtasticd, Linux, Raspberry Pi, config.yaml, available.d, config.d, SPI, YAML, demon, usługa, węzeł na komputerze
Źródło: https://meshtastic.org/docs/software/linux/usage/ (sprawdzono 2026-10)

### meshtasticd jako usługa systemd, logi i interfejs WWW
Pakiety oficjalne instalują usługę systemd same; ręczny plik /etc/systemd/system/meshtasticd.service jest potrzebny tylko przy instalacji ręcznej. Po zmianie: sudo systemctl daemon-reload, potem enable (start przy starcie systemu), start, status, stop, restart. Logi: journalctl -u meshtasticd -b. Interfejs WWW (od wydania 2.3.0) włącza blok Webserver z Port: 9443 i RootPath: /usr/share/meshtasticd/web. API działa po TCP, np. meshtastic --host localhost; avahi może ogłaszać węzeł jako _meshtastic._tcp na porcie 4403, żeby aplikacja na Androida znalazła go sama.
Słowa: meshtasticd, systemd, journalctl, systemctl, port 9443, port 4403, avahi, webserver, logi, meshtastic --host
Źródło: https://meshtastic.org/docs/software/linux/usage/ (sprawdzono 2026-10)

### Na jakich systemach działa meshtasticd?
Strona instalacji wymienia: macOS (Homebrew, tylko USB), Debian, Raspbian (32-bitowy Raspberry Pi OS), Ubuntu (PPA), Fedora (COPR), Red Hat (EPEL), Docker (DockerHub), Flatpak, OpenWrt i NixOS. Debian, Raspbian, Ubuntu, Fedora i EPEL mają USB, SPI, MUI i WWW; Docker USB i SPI, bez MUI. Same polecenia instalacji są na podstronach poszczególnych systemów, których tu nie pobrano, więc nie podaję ich z pamięci.
Słowa: meshtasticd instalacja, Debian, Raspbian, Ubuntu, Fedora, Docker, Flatpak, OpenWrt, NixOS, Homebrew
Źródło: https://meshtastic.org/docs/meshtasticd/installation/ (sprawdzono 2026-10)

### Gdzie jest dokumentacja aplikacji Meshtastic na Android, iOS i komputer?
Oficjalna dokumentacja (wersja 2.8) obejmuje: aplikację Android oraz wersję desktopową (JVM) na Linux, macOS i Windows; aplikację Apple na iOS, iPadOS, macOS, watchOS i visionOS. Każda ma Przewodnik użytkownika (konfiguracja, wiadomości, węzły, mapy, ustawienia) i dla programistów. Apple opisuje m.in. listę węzłów, miernik sygnału LoRa i jednostki dopasowane do regionu. Wymagań systemowych i źródeł instalacji strony główne nie podają, szczegóły są w podstronach user.
Słowa: aplikacja Android, aplikacja iOS, aplikacja desktop, Windows, macOS, przewodnik użytkownika, wersja 2.8, miernik sygnału
Źródło: https://meshtastic.org/docs/software/android/ oraz https://meshtastic.org/docs/software/apple/ (sprawdzono 2026-10)

### Regiony radiowe w Europie: EU_868, EU_433, EU_866, EU_N_868
Wg oficjalnej strony: EU_868 to 869,40–869,65 MHz, do +27 dBm ERP, wypełnienie 10%, jeden slot (domyślnie 869,525 MHz); jest węższy niż całe pasmo SRD 863–870 MHz, najpopularniejszy w UE, a presety Turbo są tam niedostępne. EU_433: 433–434 MHz, +10 dBm ERP, 4 sloty (domyślny 433,875 MHz). EU_866: 865,6–867,6 MHz, tylko presety Lite. EU_N_868: tylko presety Narrow. Dla naszej sieci (Narrow Fast, 869,44165 MHz) to właśnie wariant wąskopasmowy. Polskiego regionu PL strona nie opisuje; przepisy krajowe trzeba sprawdzić osobno.
Słowa: EU_868, EU_433, EU_866, EU_N_868, region, 869 MHz, 433 MHz, slot, wypełnienie 10%, moc ERP, pasmo SRD, Narrow
Źródło: https://meshtastic.org/docs/overview/radio-settings/ (sprawdzono 2026-10)

### Tabela presetów: SF, szerokość pasma, szybkość
Ogólne (SF/CR/pasmo/szybkość): Short Turbo 7, 4/5, 500 kHz, 21,88 kbps; Short Fast 7, 4/5, 250 kHz, 10,94; Short Slow 8, 4/5, 250 kHz, 6,25; Medium Turbo 9, 4/5, 500 kHz, 7,03; Medium Fast 9, 4/5, 250 kHz, 3,52; Medium Slow 10, 4/5, 250 kHz, 1,95; Long Turbo 11, 4/8, 500 kHz, 1,34; Long Fast 11, 4/5, 250 kHz, 1,07; Long Moderate 11, 4/8, 125 kHz, 0,34. Wąskopasmowe: Lite Fast 9, 4/5, 125 kHz, 1,76; Lite Slow 10, 4/5, 125 kHz, 0,98; Narrow Fast 7, 4/6, 62,5 kHz, 2,28; Narrow Slow 8, 4/6, 62,5 kHz, 1,30; Tiny Fast 7, 4/5, 15,6 kHz, 0,68; Tiny Slow 8, 4/6, 15,6 kHz, 0,33. To teoretyczne maksima, bez nagłówków, przeskoków i powtórzeń.
Słowa: presety, tabela, Long Fast, Narrow Fast, Short Fast, Medium Fast, Lite, Tiny, Turbo, SF, coding rate, szerokość pasma, kbps, prędkość transmisji
Źródło: https://meshtastic.org/docs/overview/radio-settings/ (sprawdzono 2026-10)

### Czas nadawania i obciążenie sieci dla każdego presetu i korekcji błędów (CR)
Czas nadawania pakietu 50 B (rząd wielkości typowej wiadomości z nagłówkiem) i to, ile urządzeń mniej więcej mieści sieć, dla każdego presetu i każdej korekcji błędów (CR), którą dany preset może mieć. Obliczone wzorem Semtech na czas w eterze (preambuła 16 symboli, nagłówek jawny, CRC włączone); liczba urządzeń to rząd wielkości przy założeniu, że sieć może zajmować 25% czasu, a każde urządzenie nadaje 4 pakiety na 15 minut, bez przekazywania pakietów dalej (każde przekazanie dokłada kolejne nadanie). Wyższa korekcja to więcej redundancji: pakiet trwa dłużej i sieć szybciej się zapełnia, ale łącze lepiej znosi zakłócenia. Presety Long mogą mieć korekcję tylko wyższą niż własna; Wąski i Mikro-wolny mogą też 4/5. Tabela (preset, własne CR, potem czas pakietu i liczba urządzeń dla każdego CR):
- Daleki zasięg — Turbo (Long Turbo) — własne CR 4/8; 4/8: 443 ms, ok. 126 urządzeń
- Daleki zasięg — Szybko (Long Fast) — własne CR 4/5; 4/5: 641 ms, ok. 87 urządzeń; 4/6: 723 ms, ok. 77 urządzeń; 4/7: 805 ms, ok. 69 urządzeń; 4/8: 887 ms, ok. 63 urządzeń
- Daleki zasięg — Średnio (Long Moderate) — własne CR 4/8; 4/8: 2036 ms, ok. 27 urządzeń
- Daleki zasięg — Wolno (Long Slow) — własne CR 4/8; 4/8: 3547 ms, ok. 15 urządzeń
- Średni zasięg — Szybko (Medium Fast) — własne CR 4/5; 4/5: 181 ms, ok. 311 urządzeń; 4/6: 205 ms, ok. 273 urządzeń; 4/7: 230 ms, ok. 244 urządzeń; 4/8: 254 ms, ok. 221 urządzeń
- Średni zasięg — wolno (Medium Slow) — własne CR 4/5; 4/5: 341 ms, ok. 164 urządzeń; 4/6: 386 ms, ok. 145 urządzeń; 4/7: 431 ms, ok. 130 urządzeń; 4/8: 476 ms, ok. 118 urządzeń
- Średni zasięg - turbo (Medium Turbo) — własne CR 4/5; 4/5: 90 ms, ok. 622 urządzeń; 4/6: 103 ms, ok. 547 urządzeń; 4/7: 115 ms, ok. 489 urządzeń; 4/8: 127 ms, ok. 442 urządzeń
- Krótki zasięg — Szybko (Short Fast) — własne CR 4/5; 4/5: 53 ms, ok. 1064 urządzeń; 4/6: 61 ms, ok. 929 urządzeń; 4/7: 68 ms, ok. 824 urządzeń; 4/8: 76 ms, ok. 741 urządzeń
- Bliski zasięg — Wolno (Short Slow) — własne CR 4/5; 4/5: 95 ms, ok. 589 urządzeń; 4/6: 109 ms, ok. 517 urządzeń; 4/7: 122 ms, ok. 460 urządzeń; 4/8: 135 ms, ok. 415 urządzeń
- Krótki zasięg — Turbo (Short Turbo) — własne CR 4/5; 4/5: 26 ms, ok. 2128 urządzeń; 4/6: 30 ms, ok. 1858 urządzeń; 4/7: 34 ms, ok. 1648 urządzeń; 4/8: 38 ms, ok. 1482 urządzeń
- Lite - szybki (Lite Fast) — własne CR 4/5; 4/5: 361 ms, ok. 155 urządzeń; 4/6: 411 ms, ok. 136 urządzeń; 4/7: 460 ms, ok. 122 urządzeń; 4/8: 509 ms, ok. 110 urządzeń
- Lite - wolny (Lite Slow) — własne CR 4/5; 4/5: 682 ms, ok. 82 urządzeń; 4/6: 772 ms, ok. 72 urządzeń; 4/7: 862 ms, ok. 65 urządzeń; 4/8: 952 ms, ok. 59 urządzeń
- Wąski - szybki (Narrow Fast) — własne CR 4/6; 4/5: 211 ms, ok. 266 urządzeń; 4/6: 242 ms, ok. 232 urządzeń; 4/7: 273 ms, ok. 206 urządzeń; 4/8: 304 ms, ok. 185 urządzeń
- Wąski - wolny (Narrow Slow) — własne CR 4/6; 4/5: 382 ms, ok. 147 urządzeń; 4/6: 435 ms, ok. 129 urządzeń; 4/7: 488 ms, ok. 115 urządzeń; 4/8: 542 ms, ok. 103 urządzeń
- Mikro - szybki (Tiny Fast) — własne CR 4/5; 4/5: 846 ms, ok. 66 urządzeń; 4/6: 969 ms, ok. 58 urządzeń; 4/7: 1092 ms, ok. 51 urządzeń; 4/8: 1214 ms, ok. 46 urządzeń
- Mikro - wolny (Tiny Slow) — własne CR 4/6; 4/5: 1937 ms, ok. 29 urządzeń; 4/6: 2232 ms, ok. 25 urządzeń; 4/7: 2527 ms, ok. 22 urządzeń; 4/8: 2822 ms, ok. 19 urządzeń
Słowa: czas nadawania, airtime, time on air, obciążenie sieci, korekcja błędów, coding rate, CR, 4/5, 4/6, 4/7, 4/8, preset, ile urządzeń, tabela, pakiet 50 B
Źródło: obliczone w aplikacji wzorem Semtech na czas w eterze z parametrów presetów z https://meshtastic.org/docs/overview/radio-settings/ (sprawdzono 2026-10)
