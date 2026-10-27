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

### Elektryczność statyczna: jak nie uszkodzić płytki?
Niektóre układy uszkadza już wyładowanie rzędu 30 V, a skutek bywa widoczny dopiero po czasie. Iskrę czuć dopiero przy kilku tysiącach woltów, więc płytkę można zniszczyć niezauważenie. Zabezpieczenia: opaska uziemiająca na nadgarstek, mata antystatyczna, przechowywanie w woreczkach antystatycznych (działają jak klatka Faradaya), trzymanie materiałów łatwo się elektryzujących (folie, styropian, sweter) co najmniej 30 cm od płytek. Wilgotne powietrze zmniejsza ładunki, a suche zimą je zwiększa. Nie dotykaj pinów anteny i złączy bez potrzeby.
Słowa: elektryczność statyczna, ESD, wyładowanie, opaska antystatyczna, mata, woreczek antystatyczny, uszkodzenie płytki, zima, suche powietrze
Źródło: https://en.wikipedia.org/wiki/Electrostatic_discharge (sprawdzono 2026-10; wzmianki o kilku tysiącach woltów i zimie to wiedza ogólna)

### Burza: jak powstaje i ile trwa?
Burza potrzebuje wilgoci, chwiejnego powietrza i czynnika unoszącego (np. frontu). Ma trzy etapy, każdy średnio ok. 30 minut: rozwój (prądy wstępujące, chmura kłębiasta), dojrzałość (prądy wstępujące i zstępujące, kowadło, grad, silny wiatr, pioruny) i zanik. Pojedyncza komórka trwa zwykle 20–30 minut, superkomórka nawet godzinami; przeciętna burza ma ok. 24 km średnicy. W klimacie umiarkowanym najwięcej burz jest wiosną i latem, a w Europie najczęściej późnym popołudniem i wieczorem. Zagrożenia: pioruny, grad, gwałtowne porywy wiatru (downburst), nagłe powodzie. Jeśli słychać grzmot, piorun może cię trafić.
Słowa: burza, powstawanie burzy, etapy, grad, downburst, porywy wiatru, superkomórka, kiedy burze, popołudnie, front, pogoda
Źródło: https://en.wikipedia.org/wiki/Thunderstorm (sprawdzono 2026-10)
