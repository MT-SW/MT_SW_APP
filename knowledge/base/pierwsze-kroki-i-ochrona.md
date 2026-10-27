# Pierwsze kroki z węzłem, sterowniki w praktyce, szczelność i elektryczność statyczna

### Od czego zacząć z nowym urządzeniem Meshtastic?
Najpierw sprawdź, jaki układ ma płytka: ESP32 (Wi-Fi i Bluetooth), nRF52 (tylko Bluetooth), RP2040/RP2350 albo STM32 (bez Wi-Fi i Bluetooth, wiele funkcji domyślnie wyłączonych). Nigdy nie włączaj radia bez podłączonej anteny, bo może się uszkodzić. Sprawdź, czy kabel przesyła dane (skopiuj plik na telefon i z powrotem). ESP32 zwykle potrzebuje sterowników, nRF52 i RP2040 zwykle nie (pokazują się jak pendrive). Potem wgraj firmware (np. flasher.meshtastic.org), połącz się aplikacją i koniecznie ustaw region; bez niego węzeł nie nadaje. Na końcu sprawdź preset: po ustawieniu regionu jest domyślny i może nie pasować do lokalnej sieci (w naszej sieci to Narrow Fast).
Słowa: pierwsze kroki, nowe urządzenie, start, jak zacząć, ustaw region, antena, kabel, sterowniki, flasher, preset, konfiguracja początkowa
Źródło: https://meshtastic.org/docs/getting-started/ i https://meshtastic.org/docs/getting-started/initial-config/ (sprawdzono 2026-10; preset naszej sieci wg ustaleń administratora)

### Jak sprawdzić w Windows, czy komputer widzi węzeł?
Otwórz Menedżer urządzeń, rozwiń "Porty (COM i LPT)" i szukaj wpisu w rodzaju "Silicon Labs CP210X USB to UART Bridge (COM5)", "USB-Enhanced-SERIAL CH9102 (COM5)" albo "USB Serial Device (COM5)"; numer COM może być inny. Brak wpisu: najpewniej kabel tylko do ładowania, albo trzeba zainstalować sterownik. Sterowniki: CP210x od Silicon Labs, CH9102/CH340 od WCH (paczka CH343SER), dla Windows 7 osobna wersja od LILYGO. Po instalacji koniecznie uruchom komputer ponownie, bo bez tego wygląda, jakby nie działało. Jeśli nie wiesz, jaki układ ma płytka, spróbuj najpierw CP210x, potem CH9102. Na Linuksie poprawnie wykryte urządzenie pokazuje się na liście USB np. jako "Silicon Labs CP210x UART Bridge" lub "QinHeng Electronics USB Single Serial".
Słowa: Menedżer urządzeń, port COM, COM5, nie widzi urządzenia, CP210x, CH9102, CH340, sterownik Windows, restart, lsusb, Linux, kabel
Źródło: https://meshtastic.org/docs/getting-started/serial-drivers/esp32/ i https://meshtastic.org/docs/getting-started/serial-drivers/test-serial-driver-installation/ (sprawdzono 2026-10)

### Co znaczy stopień ochrony IP (IP65, IP67, IP68)?
Pierwsza cyfra to ochrona przed ciałami stałymi: 5 oznacza ochronę przed pyłem (trochę może wejść, ale nie szkodzi), 6 pyłoszczelność. Druga cyfra to woda: 4 bryzgi z każdej strony, 5 strumień z dyszy 6,3 mm, 6 silny strumień z dyszy 12,5 mm, 7 zanurzenie do 1 m na 30 minut, 8 ciągłe zanurzenie głębiej niż 1 m na warunkach producenta. Zatem IP65 to pyłoszczelna i odporna na strumień wody, IP67 pyłoszczelna i wytrzymuje krótkie zanurzenie, IP68 pyłoszczelna do dłuższego zanurzenia. Odporność na wodę powyżej IPX6 nie sumuje się: zanurzenie (7) nie oznacza odporności na strumień (5), wtedy pisze się IPX5/IPX7. Testy robi się czystą wodą. Dla węzła na maszcie (deszcz z wiatrem) rozsądne minimum to IP65 (wniosek ogólny); pamiętaj o uszczelnieniu przepustów kabli.
Słowa: IP65, IP67, IP68, szczelność, wodoodporna, obudowa, pyłoszczelna, deszcz, zanurzenie, stopień ochrony, puszka hermetyczna
Źródło: https://en.wikipedia.org/wiki/IP_code (sprawdzono 2026-10; zalecenie IP65 dla masztu to wniosek ogólny)

