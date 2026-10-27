# Użytkownik, tryb licencjonowany i inne drobne ustawienia

### Co oznaczają ustawienia użytkownika: nazwa, skrót, licencja, "unmessageable"?
Long Name to nazwa urządzenia (domyślnie generowana; w trybie licencjonowanym wpisz znak wywoławczy). Short Name to krótki identyfikator (domyślnie generowany; CLI: --set-owner-short, do 4 znaków). Is Licensed (tryb krótkofalarski) domyślnie wyłączony: przed włączeniem przeczytaj uprawnienia i ograniczenia krótkofalowców; dokumentacja każe ustawić Long Name na znak wywoławczy i klucz kanału na pusty, co usuwa szyfrowanie. Is Unmessageable: czy węzeł może odbierać wiadomości bezpośrednie; domyślnie False, poza rolami zwykle niemonitorowanymi (ROUTER, ROUTER_LATE, REPEATER, SENSOR, LOST_AND_FOUND, TRACKER, TAK_TRACKER). CLI: --set-owner, --set-owner-short, --set-ham.
Słowa: long name, short name, znak wywoławczy, ham, licencja, krótkofalowiec, unmessageable, nazwa węzła, set-ham, szyfrowanie wyłączone
Źródło: https://meshtastic.org/docs/configuration/radio/user/ (sprawdzono 2026-10)

### Jak działa moduł Ambient Lighting (podświetlenie RGB)?
Wymaga firmware 2.2.5+. Powstał dla diody RGB RAK14001 (sterownik NCP5623 po I2C). Ustawienia: LED State (1 włączone, 0 wyłączone), Current (domyślnie 10), Red, Green, Blue (0–255). CLI: meshtastic --set ambient_lighting.led_state 1, --set ambient_lighting.red 103 i analogicznie green, blue; podgląd --get ambient_lighting.
Słowa: ambient lighting, dioda RGB, RAK14001, NCP5623, led_state, kolor diody, podświetlenie
Źródło: https://meshtastic.org/docs/configuration/module/ambient-lighting/ (sprawdzono 2026-10)

### Jakie integracje i narzędzia oferuje Meshtastic?
Integracje: CalTopo/SARTopo (śledzenie węzłów), wtyczka ATAK (przekazywanie ruchu Cursor on Target przez sieć), MQTT (łączenie sieci przez internet, Home Assistant, Node-RED, Adafruit IO). Oprogramowanie: aplikacje Android i Apple, klient WWW, Python CLI, mPWRD-OS, Meshtastic Site Planner (planowanie zasięgu) i Meshtasticator (symulator sieci). Wsparcia dla integracji szukaj u ich autorów.
Słowa: integracje, ATAK, CalTopo, Node-RED, Adafruit IO, Site Planner, Meshtasticator, symulator, mPWRD, narzędzia
Źródło: https://meshtastic.org/docs/software/integrations/ (sprawdzono 2026-10)

### Jaki jest status prawny i licencyjny projektu Meshtastic?
Projekt jest "w większości" stabilny, rozwój trwa. Urządzenia nie są wytwarzane przez projekt ani badane przez regulatorów (UL, FCC); użycie należy traktować jako eksperymentalne. Całe zastosowanie komercyjne musi spełniać licencję GPLv3 i wytyczne znaku towarowego; Meshtastic jest zastrzeżonym znakiem towarowym Meshtastic LLC. Maintainerzy nie wiedzą, czy projekt używa opatentowanych algorytmów, i nie są prawnikami. To nie jest porada prawna.
Słowa: GPLv3, licencja oprogramowania, znak towarowy, komercyjne użycie, patenty, regulator, status prawny
Źródło: https://meshtastic.org/docs/legal/ (sprawdzono 2026-10)
