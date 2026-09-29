# 🇵🇱 MT_SW_APP — opis wydania

## Sniffer (duża przebudowa)

Sniffer pokazuje ruch w eterze, który normalnie by zniknął — wszystkie pakiety usłyszane przez węzeł, łącznie z ruchem broadcastowym, są przekazywane surowo do telefonu. Ta wersja łączy oba źródła (Radio i MQTT) na jednym ekranie i naprawia kilka realnych błędów w samym mechanizmie zbierania i deszyfrowania danych.

- **Jeden wspólny ekran Sniffer** w Ustawienia → Advanced (Radio + MQTT), tylko jedno źródło aktywne naraz.
- **Grupowanie duplikatów pakietów** — ten sam pakiet usłyszany wielokrotnie (przez kilka bramek MQTT albo kilka węzłów pośredniczących) pokazuje się jako jeden wiersz z pełną listą źródeł.
- **Naprawiony wybór "reprezentanta" duplikatu** — appka wybierała kopię z najdłuższą zdekodowaną trasą, zakładając, że to najpełniejszy obraz; w praktyce routing zalewowy w Meshtastic dokleja zbędne, redundantne skoki do dłuższych kopii, nie dodaje informacji. Teraz wybierana jest najkrótsza poprawnie zdekodowana kopia.
- **Kopiowanie pakietu obejmuje teraz wszystkie odebrane kopie naraz**, nie tylko jedną wybraną do wyświetlenia — przydatne przy diagnozowaniu rozbieżności między kopiami tego samego pakietu.
- **Naprawiony wybór kanału przy deszyfrowaniu** — appka próbowała znanych kluczy kanałów w kolejności ich konfiguracji, zamiast najpierw sprawdzić kanał, którego hash faktycznie zgadza się z pakietem. Przy więcej niż jednym skonfigurowanym kanale mogło się zdarzyć, że inny, niewłaściwy klucz "przypadkiem" rozszyfrowywał bajty na sensownie wyglądającą, ale całkowicie błędną treść, zanim appka doszła do właściwego klucza.
- **Naprawiona błędna kategoria "Nieznany"** dla pakietów, które musiała rozszyfrować sama appka (typowe w trybie Sniffer) — numer portu był odczytywany tylko z pakietów już rozszyfrowanych przez firmware, więc treść dekodowała się poprawnie, a kategoria i tak pokazywała "Nieznany".
- **Naprawione wykrywanie stanu snifera radiowego po ponownym połączeniu z tym samym węzłem** (np. po jego resecie) — appka pytała urządzenie o stan tylko wtedy, gdy zmienił się numer podłączonego węzła. Dodatkowo: jeśli urządzenie w ogóle nie odpowiada na to zapytanie (np. wgrane czyste, stockowe firmware), wybór teraz wraca na "Wyłączony" po przekroczeniu limitu czasu, zamiast zostawiać przełącznik w mylącym stanie "włączony".
- **Zapis i wczytywanie logu** do pliku (txt/JSON/CSV), licznik zapełnienia bufora w pasku górnym, podsumowanie telemetrii zasilania i statystyk węzła widoczne od razu, bez klikania w pakiet.
- **Sniffer MQTT działa teraz też na desktopie** — wcześniej była tam tylko atrapa połączenia.

## Diagnostyka na żądanie (OnDemand) — nowy ekran

Osobny ekran z widoku szczegółów węzła, pozwalający odpytać dowolny węzeł w zasięgu o bieżące statystyki na żądanie: 10 typów zapytań (statystyki węzła, ping, lista węzłów online, historia błędów routingu, liczniki portów, aktywność eteru i inne), odpowiedzi przychodzą na dedykowanym porcie protokołu.

## Mapa

Silnik mapy na Androidzie i desktopie korzysta teraz ze współdzielonej, natywnie renderowanej biblioteki MapLibre — desktop dostaje mapę "za darmo" z tego samego modułu, zamiast własnoręcznie pisanego renderera.

- Domyślnie włączone nakładki terenu (hillshade) i pogody (radar).
- Węzły na identycznej pozycji GPS są subtelnie rozstawione, żeby dały się rozdzielić po zoomie.
- Naprawione migotanie liczby w bąblu klastra, dostrojone klastrowanie, ostrzejsze kafelki rastrowe.

## Ustawienia desktopowe

- **Naprawiona brakująca mapa w spakowanej wersji (instalator MSI)** — działała normalnie przez `gradlew run`, ale w spakowanej wersji ProGuard usuwał klasy renderera wybierane przez refleksję, więc mapa nigdy nie rysowała pierwszej ramki.
- **Dodane natywne biblioteki macOS (ARM64)** do jara budowanego na Windowsie — wcześniej ten sam jar odpalony na Macu kończył się brakiem biblioteki graficznej i appka nie rysowała okna.
- Pasek szybkiego przewijania dla długich list, trzecia kolumna widoku węzła podąża teraz za wybranym węzłem.

## Bluetooth

- **Naprawiona zawodność ponownego łączenia z już sparowanym urządzeniem** (potwierdzone na Xiaomi/MIUI) — jedynym działającym obejściem było wcześniej ręczne odparowanie i sparowanie od nowa.
- **Naprawione niewykrywanie węzła po Bluetooth na desktopie (Windows)** — regresja z aktualizacji biblioteki BLE, przypięta z powrotem do wersji, która działa.

## Komunikator i bezpieczeństwo

- Zapis zdjęcia z podglądu na cały ekran w czacie.
- Wybór koloru węzła przy generowaniu klucza — appka miele losowe klucze lokalnie na telefonie, aż trafi kolor węzła zbliżony do wybranego.
- Ujednolicona kolorystyka wskaźników bezpieczeństwa i statusu połączenia węzła.

## Węzły

- Spolszczone nazwy ról urządzeń, nowe sekcje pomocy (Role, Bezpieczeństwo, Połączenie) w oknie pomocy listy węzłów.
- Automatyczne czyszczenie bazy węzłów (próg nieaktywności i częstotliwość sprawdzania, na Androidzie i desktopie).
- Naprawione gubienie/przestawianie znaków w polu wyszukiwania listy węzłów.

## Podziękowania

Za korektę tłumaczeń i część pomysłów na nowe funkcje odpowiada [cheaterenator](https://github.com/cheaterenator).

---

# 🇬🇧 MT_SW_APP — release notes

## Sniffer (major rework)

The Sniffer surfaces air traffic that would normally just vanish — every packet the node overhears, including broadcast traffic, is forwarded raw to the phone. This release combines both sources (Radio and MQTT) on one screen and fixes several real bugs in the collection and decryption pipeline itself.

- **One combined Sniffer screen** under Settings → Advanced (Radio + MQTT), only one source active at a time.
- **Duplicate-packet grouping** — the same packet heard multiple times (via several MQTT gateways or several relaying nodes) now shows as a single row with the full list of sources.
- **Fixed which copy gets picked as the "representative"** — the app used to pick the copy with the longest decoded route, assuming that meant the fullest picture; in practice Meshtastic's flood routing appends redundant hops to longer copies, it doesn't add information. It now picks the shortest successfully-decoded copy.
- **Copying a packet now includes every received copy at once**, not just the one chosen for display — useful for diagnosing discrepancies between copies of the same packet.
- **Fixed channel selection during decryption** — the app tried known channel keys in configuration order instead of first checking the channel whose hash actually matches the packet. With more than one channel configured, a wrong key could "successfully" decrypt bytes into plausible-looking but completely wrong content before the app ever reached the right key.
- **Fixed the wrong "Unknown" category** for packets the app had to decrypt itself (typical in Sniffer mode) — the port number was only ever read from packets firmware had already decrypted, so the content decoded fine but the category still showed "Unknown".
- **Fixed detecting the radio sniffer's state after reconnecting to the same node** (e.g. after a reset) — the app only re-asked when the connected node's number changed. On top of that, if the device never answers this query at all (e.g. reflashed to clean, stock firmware), the selection now falls back to Off after a timeout instead of leaving the toggle stuck looking "on".
- **Save and load a log** to a file (txt/JSON/CSV), a buffer-fill counter in the top bar, power and node-stats telemetry summaries visible immediately without tapping into a packet.
- **The MQTT sniffer now also works on desktop** — previously just a connection stub.

## On-demand diagnostics (OnDemand) — new screen

A dedicated screen from the node detail view, letting you query any node in range for current stats on demand: 10 query types (node stats, ping, nodes online, routing error history, port counters, air activity, and more), answered on a dedicated protocol port.

## Map

The map engine on Android and desktop now shares the same natively-rendered MapLibre library — desktop gets a map "for free" from the same module, instead of a hand-written renderer.

- Terrain (hillshade) and weather radar overlays on by default.
- Nodes at an identical GPS position are nudged slightly apart so they can be separated by zooming.
- Fixed a flickering cluster-bubble count, tuned clustering, sharper raster tiles.

## Desktop settings

- **Fixed the missing map in the packaged build (MSI installer)** — worked fine via `gradlew run`, but in the packaged build ProGuard stripped reflection-selected renderer classes, so the map never drew a first frame.
- **Added native macOS (ARM64) libraries** to the jar built on Windows — the same jar previously failed to draw a window at all on a Mac due to a missing graphics library.
- Fast-scroll sidebar for long lists, the node detail third column now follows the currently selected node.

## Bluetooth

- **Fixed unreliable reconnection to an already-paired device** (confirmed on Xiaomi/MIUI) — the only previous workaround was manually unpairing and re-pairing.
- **Fixed BLE device discovery not working on desktop (Windows)** — a regression from a BLE library update, pinned back to a working version.

## Messaging and security

- Save a photo from the full-screen chat viewer.
- Node color picker when generating a key — the app grinds random keys locally on the phone until it finds one whose resulting node color is close to the one chosen.
- Unified node security and connection-status indicator colors.

## Nodes

- Localized device role names, new help sections (Roles, Security, Connection) in the node list help sheet.
- Automatic node database cleanup (inactivity threshold and check frequency, on both Android and desktop).
- Fixed characters getting lost or reordered while typing in the node list search field.

## Credits

Translation corrections and some feature ideas courtesy of [cheaterenator](https://github.com/cheaterenator).
