# MT_SW_APP — osobisty fork Meshtastic-Android

Fork oficjalnej aplikacji [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android) rozwijany na potrzeby sieci mesh radiowej **Świętokrzyskie** (mt-sw.pl). Bazuje na architekturze KMP/Compose Multiplatform oryginału (warianty `fdroid`/`google`, moduł desktopowy) i dokłada zestaw lokalnych funkcji, poprawek i personalizacji, których nie ma w wersji upstream.

Stan roboczy — repo służy głównie do własnego użytku i testów z niewielką grupą osób, niekoniecznie buduje się na bieżąco.

## Zarządzanie węzłami i siecią mesh

- **Zdalne sterowanie GPIO** — na ekranie szczegółów węzła (moduł Remote Hardware) można wpisać numer pinu, appka sama liczy maskę bitową i wysyła `WRITE_GPIOS`/`READ_GPIOS` do zdalnego węzła. Przyciski aktywne tylko gdy klucze PKC z węzłem zostały wymienione.
- **Zdalne ulubione/ignorowanie węzłów** przez sieć LoRa (nie tylko lokalnie) — z prawdziwym potwierdzeniem doręczenia opartym o routing ACK z mesh, zamiast tylko zmiany po stronie telefonu.
- **Ręczne dodawanie kontaktu przez ID węzła** — zarówno lokalnie, jak i zdalnie, z ujednoliconym formatem `!a1b2c3d4` (hex) wszędzie w appce.
- **Pasywne zbieranie NeighborInfo** — log sąsiadów pokazuje teraz też podsłuchane rozgłoszenia innych węzłów, nie tylko odpowiedzi na własne zapytania; naprawiony też przypadek żądania Neighbor Info dla własnego, lokalnie podłączonego urządzenia (wcześniej nic nie zwracał).
- **Przyciski szybkich komend** (`/ping`, `/hello`, `/test`) na ekranie węzła — wysyłają wiadomość prywatną nawet do węzłów, których rola normalnie blokuje ręczne wiadomości; przydatne do szybkiego testowania nowych buildów firmware na urządzeniach w terenie.
- **Lista węzłów pośredniczących (relay) w dostarczeniu wiadomości** — dialog "Status doręczenia" pokazuje pełną listę nazw wszystkich węzłów biorących udział w retransmisji (nie tylko licznik ani jedną nazwę jak w oryginale), łącznie z poprawkami po stronie firmware, żeby te dane w ogóle docierały do appki.
- **Nazwa węzła pośredniczącego (relay) widoczna wprost na liście węzłów i w szczegółach węzła** — nie trzeba już otwierać dialogu "Status doręczenia", żeby zobaczyć przez kogo dany węzeł się łączy.
- Przywrócony ekran konfiguracji **Traffic Management** (w pewnym momencie usunięty w upstreamie).
- **Naprawiony nieaktualny czas ostatniej pozycji dla węzłów ze stałą (ręcznie wpisaną) lokalizacją** — firmware nie odświeża czasu przy retransmisji tej samej pozycji, appka pokazuje teraz realny czas ostatniego kontaktu z węzłem zamiast zamrożonej daty sprzed dni.
- **Ukryte nieaktualne role urządzenia** (REPEATER, ROUTER_CLIENT) na liście wyboru roli w konfiguracji urządzenia — firmware ich już nie wspiera, więc nie da się ich przez pomyłkę wybrać.
- **Spolszczone nazwy ról urządzeń** — appka wcześniej pokazywała surową, angielską nazwę techniczną roli (np. `ROUTER_LATE`) wszędzie: w ustawieniach, na liście węzłów, w szczegółach węzła i w telemetrii; teraz każda rola ma właściwą polską nazwę (np. "Router pomocniczy"), a opisy ról też odwołują się do nich po polsku zamiast po angielsku.
- **Nowa sekcja "Role" w oknie pomocy listy węzłów** — pełny opis wszystkich ról urządzenia (ikona, nazwa, znaczenie) dostępny bezpośrednio z listy węzłów.
- Poprawiony wygląd czasu działania (uptime) na liście węzłów — dodana ikonka odróżniająca go wizualnie od czasu ostatniego kontaktu.
- **Automatyczne czyszczenie bazy węzłów** — na ekranie "Wyczyść bazę węzłów" można włączyć automatyczne usuwanie nieaktywnych węzłów: suwak progu nieaktywności (1–90 dni) i suwak częstotliwości sprawdzania (1–30 dni). Działa zarówno na Androidzie (WorkManager), jak i na desktopie (własna pętla sprawdzająca co godzinę, z zapamiętanym czasem ostatniego uruchomienia, żeby przetrwać restart appki); domyślnie wyłączone, węzły ulubione i ignorowane nigdy nie są usuwane automatycznie.

## Mapa

Silnik mapy (Android i desktop) korzysta teraz ze współdzielonej, natywnie renderowanej biblioteki **MapLibre** — przyjętej z upstreamu zamiast wcześniejszego, własnoręcznie napisanego renderera (osmdroid na Androidzie, autorski renderer kafelków OSM na Compose Canvas na desktopie). Powody tej zmiany: wydajniejsze natywne renderowanie (GPU) i brak konieczności utrzymywania osobnego silnika mapy przy każdej synchronizacji z upstreamem.

Co to oznacza w praktyce:

- **Desktop ma teraz mapę "za darmo"** — wcześniej zakładka "Mapa" na desktopie była pustym placeholderem, potem dorobiona jako własny renderer od zera; teraz korzysta z tego samego współdzielonego komponentu co Android, więc funkcje typu import warstw GeoJSON/KML, pobieranie kafelków offline czy filtry mapy pochodzą już ze wspólnego modułu, a nie z osobnej implementacji tylko dla desktopu.
- **Site Planner** (symulacja zasięgu radiowego) na desktopie działa teraz przez przeglądarkę zamiast wcześniejszej integracji z wbudowaną przeglądarką Chromium (JCEF) pisaną specjalnie pod ten fork.
- Mini-mapa w szczegółach węzła, mapa trasy pozycji i główny ekran mapy korzystają z tego samego, wspólnego komponentu na obu platformach.
- Kliknięcie węzła na mapie desktopowej nadal otwiera najpierw listę węzłów, potem szczegóły — to zachowanie przetrwało przejście na nowy silnik mapy.
- **Domyślnie włączone nakładki terenu i pogody** — nakładka cieniowania rzeźby terenu (hillshade, przydatna do oceny zasięgu LoRa ograniczonego ukształtowaniem terenu) oraz radar pogodowy NOAA są teraz zaznaczone od razu po otwarciu mapy, zamiast wymagać ręcznego włączenia w warstwach.
- **Mniejsze plakietki węzłów z ogonkiem wskazującym dokładną pozycję** — plakietka jest teraz kompaktowa, z małym zaokrąglonym ogonkiem pod spodem wskazującym dokładny punkt GPS węzła, zamiast być wyśrodkowana na nim.
- **Węzły na tej samej pozycji GPS są od siebie subtelnie odsunięte** — kilka urządzeń zgłaszających identyczną (np. ustawioną na sztywno) lokalizację jest teraz rozstawionych o kilka metrów w rzeczywistości: niewidoczne przy oddaleniu, ale pozwalające zobaczyć i tapnąć każde z osobna po zbliżeniu, oraz rozbić taki klaster zoomem zamiast utykać na liście.
- **Mapa zawsze otwiera się dopasowana do wszystkich węzłów** — zamiast wracać do ostatnio zapamiętanej pozycji i przybliżenia.
- **Ostrzejsze kafelki map rastrowych (np. OSM)** — naprawiony błąd powodujący rozmycie przez błędny domyślny rozmiar kafelka.
- **Dostrojone klastrowanie węzłów na mapie** — małe grupki węzłów nie zlewają się już w jeden bąbel z liczbą; grupowanie zaczyna się dopiero przy realnie gęstym skupisku.

## Ustawienia desktopowe

- **Naprawiony import/eksport konfiguracji urządzenia** — wcześniej przycisk działał, ale nie tworzył żadnego pliku (błąd w parsowaniu ścieżki na Windowsie, cichy błąd bez informacji dla użytkownika).
- Przywrócony brakujący przełącznik **automatyczne ładowanie obrazków w czacie** (zgubiony przy jednym z merge'y z upstreamem).

## Ekran "Zdrowie sieci"

Nowa, szósta zakładka w dolnej nawigacji (między Węzłami a Mapą), której nie ma w oryginalnej appce:

- **7 kategorii metryk** per węzeł: zasilanie (bateria/napięcie/prąd), sygnał (SNR/RSSI/poziom szumu, z fallbackiem na liczbę przeskoków gdy brak bezpośrednich odczytów), sieć (kanał/eter), środowisko (temperatura/wilgotność/ciśnienie), **zasoby** (CPU/pamięć/flash/PSRAM hosta węzła — wymaga customowego firmware z rozszerzoną telemetrią), ruch (TX/RX/duplikaty/przekazane/uszkodzone) i sąsiedzi.
- **Widżet na pulpit Androida "Local Stats"** — pokazuje na żywo CPU/Flash/PSRAM hosta, z zachowaniem wartości między restartami appki.
- Lista węzłów z sortowaniem, przypinaniem ulubionych na górze, ukrywaniem pustych wpisów i wyszukiwarką; szczegóły każdej metryki jako wykres w oknie 24h/7d/30d.
- **Ekran "Podsumowanie"** — karty z rankingami top-3: najcichsze węzły, najlepszy sygnał, najwięcej wysłanych pozycji, fizycznie najbliższe węzły, najwięcej danych telemetrii, najwięcej wiadomości (tydzień/dziś) i inne — wszystkie poprawnie wykluczają lokalnie podłączone urządzenie z rankingów, żeby nie zaburzało wyników.
- Architektura danych: wszystko dekodowane na żywo z istniejącego logu zdarzeń mesh przy każdym odczycie ekranu — żadne dane nie są duplikowane w osobnej tabeli, więc statystyki są zawsze aktualne i nie zajmują dodatkowego miejsca w bazie.

## Diagnostyka na żądanie (OnDemand)

Osobny ekran dostępny z ekranu szczegółów węzła (Administracja → "Diagnostyka na żądanie"), niezależny od zdalnego sterowania GPIO. Pozwala odpytać dowolny węzeł w zasięgu o bieżące statystyki na żądanie, zamiast czekać na okresowe rozgłoszenia telemetrii.

- **10 typów zapytań**: statystyki węzła (bateria, czas pracy, CPU/heap/flash/PSRAM, liczniki floodu i nexthop, blokady limitem hopów), ping (RSSI/SNR), lista węzłów online, historia błędów routingu, liczniki użycia portów, aktywność eteru, log ostatnich wymian pakietów, historia średniego czasu odbioru, historia liczby odebranych pakietów oraz wersja firmware MT_SW.
- Odpowiedzi przychodzą na dedykowanym porcie protokołu (354) i są dekodowane na żywo z istniejącego logu zdarzeń mesh — ten sam wzorzec danych co ekran "Zdrowie sieci", nic nie jest dodatkowo zapisywane.
- **Wymaga customowego firmware z modułem OnDemand** — protokół zdefiniowany we własnym forku protobufs ([MT_SW_PROTOBUFS](https://github.com/MT-SW/MT_SW_PROTOBUFS)); na starszym lub oficjalnym firmware przyciski wysyłają zapytanie, ale węzeł na nie nie odpowiada.

## Sniffer

Tryb pokazujący ruch w eterze, który normalnie by zniknął — pakiety usłyszane przez węzeł podczas floodingu mesh, ale nieadresowane do niego, są teraz przekazywane surowo do telefonu zamiast po prostu odrzucane.

- **Przełącznik w Ustawienia → Advanced** ("Tryb sniffera"), tuż pod Panelem Debugowania.
- **Ekran "Sniffer Log"** — link pod przełącznikiem, pokazuje na żywo nadawcę/odbiorcę, kanał, liczbę przeskoków, RSSI/SNR oraz dane pakietu (zdekodowane, jeśli appka zna klucz kanału; w przeciwnym razie surowy hex zaszyfrowanej treści).
- **Działa wyłącznie lokalnie** — pokazuje tylko to, co fizycznie usłyszy radiem węzeł aktualnie podłączony do telefonu; włączenie na zdalnym węźle nic nie da z perspektywy tego telefonu, bo podsłuchane pakiety trafiają do urządzenia podłączonego do TAMTEGO węzła.
- **Wymaga customowego firmware z modułem sniffera** (własny fork protobufs, [MT_SW_PROTOBUFS](https://github.com/MT-SW/MT_SW_PROTOBUFS)) — na starszym lub oficjalnym firmware przełącznik nic nie zmienia.

## Komunikator

- **Zdjęcia w czacie przez link** — appka nie wysyła surowych bajtów zdjęcia przez LoRa (za mała przepustowość), tylko uploaduje je anonimowo na zewnętrzny serwer i wysyła sam link jako wiadomość tekstową; odbiorca widzi automatyczny podgląd. Przed wysyłką pojawia się dialog ostrzegający, że serwer hostingu jest publiczny.
- **Podgląd obrazków wklejonych jako link** — sterowany osobnym przełącznikiem w Ustawienia → Prywatność (domyślnie wyłączone), dostępny zarówno na Androidzie, jak i w wersji desktopowej.
- **Desktop: Enter = nowa linijka, Ctrl+Enter = wyślij** — zamiast wymuszonego wysyłania samym Enterem, zachowanie typowe dla komunikatorów na komputerze; na telefonie wysyłanie zostaje osobnym przyciskiem obok pola tekstowego.
- **Domyślne szablony w Szybkim Czacie (Quick Chat)** — appka wcześniej startowała z pustą listą szablonów wiadomości; teraz przy pierwszym uruchomieniu automatycznie wypełnia ją zestawem własnych komend sieciowych (np. `scyzoryk pomoc`, `scyzoryk test`, `scyzoryk range`, `scyzoryk pogoda`, `scyzoryk info`, `scyzoryk aktualnosci`).

## Bezpieczeństwo

- **Wybór koloru węzła przy generowaniu klucza** — na ekranie Zabezpieczenia, obok pola klucza prywatnego, dostępny jest wybór koloru z palety; appka miele losowe klucze X25519 lokalnie na telefonie (kilka równoległych wątków, z suwakiem tolerancji dopasowania) aż trafi kolor węzła zbliżony do wybranego. Ponieważ tolerancja dopuszcza pewien rozrzut, appka pokazuje obok siebie wybrany kolor i faktyczny wynik przed wpisaniem klucza — można go zaakceptować albo szukać dalej; klucz trafia do pola dopiero po potwierdzeniu, bez automatycznego zapisu. Wymaga customowego firmware wyprowadzającego numer węzła z klucza publicznego (od wersji 2.8) — na starszym firmware przycisk potwierdzenia jest wyszarzony i pokazuje komunikat o wymaganej aktualizacji zamiast mielić klucz, który i tak nie dałby oczekiwanego koloru po połączeniu z urządzeniem.

## Branding i personalizacja

- Własny `applicationId`, dzięki czemu appka instaluje się obok oryginalnej appki Meshtastic bez konfliktu (osobne dane, można mieć obie naraz).
- **Pełny rebranding wizualny** — appka i wersja desktopowa mają teraz własną tożsamość: nazwa MT_SW_APP wszędzie w interfejsie i powiadomieniach, autorska ikona (kontur województwa świętokrzyskiego + wordmark "MT_SW", w miejsce wcześniejszego tymczasowego oznaczenia "MS+"), oraz własna paleta kolorów (złoto/granat zamiast domyślnego zielonego brandingu Meshtastic) zastosowana w ikonie połączenia, tarczy podpisanego węzła i czasie ostatniego kontaktu w appce.
- Osobna ikona w zasobniku systemowym (tray) wersji desktopowej, spójna z resztą brandingu.
- **Ekran "O aplikacji"** wskazuje na repozytorium tego forka (nie oryginalnego projektu) i pokazuje jawny disclaimer "nieoficjalny fork, niezwiązany z Meshtastic LLC" — zgodnie z licencją GPL-3.0 i polityką znaku towarowego Meshtastic LLC.
- **Naprawiony zbyt ostry dźwięk powiadomień Alert (SOS)** — kanał alertów miał własny, dedykowany plik dźwiękowy odtwarzany w trybie alarmowym; appka używa teraz domyślnego dźwięku systemowego, tak jak pozostałe kanały powiadomień.
- **Kolorowa ikona statusu połączenia z urządzeniem** — złoty gdy połączony, biały podczas łączenia/ponownego łączenia, czerwony gdy rozłączony, żółty gdy urządzenie śpi; dodatkowo krótki błysk samej ikony i poświaty na zielono przy nadawaniu danych i na niebiesko przy odbiorze.
- Wersja desktopowa przemianowana z "Meshtastic Desktop" na tę samą nazwę co appka mobilna, z uzupełnioną sekcją Prywatności w ustawieniach (wcześniej niedostępną na desktopie mimo że logika już istniała).
- Rozpoznawanie niestandardowej edycji firmware używanej w sieci Świętokrzyskiej — appka pokazuje czytelną nazwę zamiast surowej wartości technicznej.
- Wygenerowany plik tłumaczeń PL uzupełniający ok. 1000 wcześniej brakujących stringów (appka była przetłumaczona na polski w ok. 43%).

## Status i zastrzeżenia

- To osobisty, roboczy fork — część zmian jest zweryfikowana buildem i przetestowana na urządzeniu, część czeka na potwierdzenie w terenie.
- Brak oficjalnych release'ów/tagów — zmiany trzymane na bieżąco na gałęzi `main`.
- Fork korzysta z tej samej licencji GPL-3.0 co projekt macierzysty.

---
*Bazuje na [meshtastic/Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android). Nieoficjalny, niezwiązany z Meshtastic LLC.*



____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________



# MT_SW_APP — personal Meshtastic-Android fork

A fork of the official [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android) app, developed for the **Świętokrzyskie** mesh radio network (mt-sw.pl). Builds on the upstream KMP/Compose Multiplatform architecture (`fdroid`/`google` flavors, desktop module) and adds a set of local features, fixes, and customizations not found in the upstream version.

Work in progress — this repo is mainly for personal use and testing with a small group of people; it doesn't necessarily build cleanly at all times.

## Node and mesh network management

- **Remote GPIO control** — on the node detail screen (Remote Hardware module) you can enter a pin number; the app computes the bitmask itself and sends `WRITE_GPIOS`/`READ_GPIOS` to the remote node. Buttons are only enabled once PKC keys have been exchanged with that node.
- **Remote favorite/ignore over the LoRa mesh** (not just locally) — with real delivery confirmation based on mesh routing ACKs, instead of only a local, phone-side change.
- **Manual contact add by node ID** — both locally and remotely, with a unified `!a1b2c3d4` (hex) format used everywhere in the app.
- **Passive NeighborInfo collection** — the neighbor log now also shows overheard broadcasts from other nodes, not just responses to your own requests; also fixes requesting Neighbor Info for your own, locally connected device (previously returned nothing).
- **Quick command buttons** (`/ping`, `/hello`, `/test`) on the node screen — send a private message even to nodes whose role normally hides the manual message option; useful for quickly testing new firmware builds on devices in the field.
- **Full list of relay nodes for message delivery** — the "Delivery status" dialog shows the full list of node names involved in relaying a message (not just a count or a single name like upstream), including firmware-side fixes so that data actually reaches the app.
- **Relay node name shown directly in the node list and node detail** — no need to open the "Delivery status" dialog anymore to see which node a given node relays through.
- Restored the **Traffic Management** configuration screen (removed from upstream at one point).
- **Fixed a stale last-position timestamp for nodes with a manually configured fixed position** — firmware doesn't refresh the timestamp when retransmitting the same position, so the app now shows the node's actual last-heard time instead of a frozen date from days ago.
- **Hid outdated device roles** (REPEATER, ROUTER_CLIENT) from the role picker in device configuration — firmware no longer supports them, so they can't be selected by mistake anymore.
- **Localized device role names** — the app used to show the raw, English technical role name (e.g. `ROUTER_LATE`) everywhere: settings, node list, node detail, and telemetry; every role now has a proper localized name (e.g. "Router pomocniczy" in Polish), and the role descriptions reference them the same way instead of the raw English name.
- **New "Roles" section in the node list help sheet** — a full reference of every device role (icon, name, meaning) available directly from the node list.
- Cleaned up the uptime display in the node list — added an icon to visually separate it from the last-heard time.
- **Automatic node-database cleanup** — the "Clean Node Database" screen now has a toggle for automatically removing inactive nodes, with sliders for the inactivity threshold (1–90 days) and how often the check runs (1–30 days). Works on both Android (WorkManager) and desktop (a lightweight hourly check loop with the last run persisted so timing survives app restarts); off by default, and favorited/ignored nodes are never auto-deleted.

## Map

The map engine (Android and desktop) now uses the shared, natively rendered **MapLibre** library — adopted from upstream in place of the earlier, hand-written renderer (osmdroid on Android, a custom OSM tile renderer on Compose Canvas on desktop). Reasons for the switch: faster native (GPU) rendering, and no longer having to maintain a separate map engine on every sync with upstream.

What this means in practice:

- **Desktop gets a map "for free" now** — the desktop "Map" tab used to be an empty placeholder, then a from-scratch custom renderer; it now uses the same shared component as Android, so features like GeoJSON/KML layer import, offline tile downloads, and map filters come from the shared module rather than a desktop-only implementation.
- **Site Planner** (coverage simulation) on desktop now runs through the browser instead of the earlier integration with an embedded Chromium browser (JCEF) built specifically for this fork.
- The node-detail mini-map, the position-track map, and the main map screen all use the same shared component on both platforms.
- Clicking a node on the desktop map still opens the node list first, then details — that behavior survived the switch to the new map engine.
- **Terrain and weather overlays enabled by default** — the hillshade overlay (useful for judging LoRa range limited by terrain) and the NOAA weather radar overlay are now checked as soon as the map opens, instead of requiring a manual toggle in the layers menu.
- **Smaller node chips with a tail pointing at the exact GPS position** — the chip badge is now more compact, with a small rounded tail underneath pointing at the node's precise coordinate instead of being centered over it.
- **Nodes reporting an identical GPS position are nudged slightly apart** — several devices reporting an identical (e.g. fixed) position are now spread a few metres apart in real space: invisible when zoomed out, but letting each one be seen and tapped individually once zoomed in, and letting such a cluster break apart by zooming instead of getting stuck.
- **Map always opens framed on all nodes** — instead of reopening at the last remembered position and zoom.
- **Sharper raster basemap tiles (e.g. OSM)** — fixed a bug causing blur from an incorrect default tile size.
- **Tuned node clustering on the map** — small groups of nodes no longer collapse into a single numbered bubble; clustering now only kicks in for a genuinely dense cluster.

## Desktop settings

- **Fixed device configuration import/export** — the button worked but silently failed to create any file (a URI-parsing bug on Windows, with no error shown to the user).
- Restored the missing **auto-load chat images** toggle (lost during an upstream merge).

## "Network Health" screen

A new, sixth tab in the bottom navigation (between Nodes and Map) that doesn't exist in the original app:

- **7 metric categories** per node: power (battery/voltage/current), signal (SNR/RSSI/noise floor, with a hop-count fallback when there are no direct readings), network (channel/air utilization), environment (temperature/humidity/pressure), **resources** (node host CPU/memory/flash/PSRAM — requires custom firmware with extended telemetry), traffic (TX/RX/duplicates/relayed/corrupted), and neighbors.
- **Android home-screen "Local Stats" widget** — shows live host CPU/Flash/PSRAM, persisted across app restarts.
- Node list with sorting, pinning favorites to the top, hiding empty entries, and search; each metric's detail view is a chart over a 24h/7d/30d window.
- **"Summary" screen** — cards with top-3 rankings: quietest nodes, best signal, most positions sent, physically closest nodes, most telemetry data, most messages (week/today), and more — all correctly exclude the locally connected device from the rankings so it doesn't skew results.
- Data architecture: everything is decoded live from the existing mesh event log every time the screen is read — nothing is duplicated into a separate table, so the stats are always current and take no extra database space.

## On-demand diagnostics (OnDemand)

A dedicated screen reachable from the node detail screen (Administration → "On-Demand Diagnostics"), separate from remote GPIO control. Lets you query any node in range for current stats on demand, instead of waiting for periodic telemetry broadcasts.

- **10 query types**: node stats (battery, uptime, CPU/heap/flash/PSRAM, flood and nexthop counters, hop-limit blocks), ping (RSSI/SNR), nodes online, routing error history, port usage counters, air activity, recent packet exchange log, average RX time history, RX packet count history, and the MT_SW firmware version.
- Responses arrive on a dedicated protocol port (354) and are decoded live from the existing mesh event log — the same data pattern as the "Network Health" screen, nothing is persisted separately.
- **Requires custom firmware with the OnDemand module** — the protocol is defined in a dedicated protobufs fork ([MT_SW_PROTOBUFS](https://github.com/MT-SW/MT_SW_PROTOBUFS)); on older or stock firmware the buttons send the request, but the node never responds.

## Sniffer

A mode that surfaces air traffic that would normally just vanish — packets the node overhears during normal mesh flooding but that aren't addressed to it are now forwarded raw to the phone instead of being dropped.

- **Toggle in Settings → Advanced** ("Sniffer mode"), right below the Debug Panel entry.
- **"Sniffer Log" screen** — a link below the toggle, showing sender/receiver, channel, hop count, RSSI/SNR, and the packet payload live (decoded when the app has the channel key, otherwise shown as raw encrypted hex).
- **Local only** — surfaces only what the node currently connected to the phone physically hears over the radio; enabling it on a remote node does nothing useful from this phone's perspective, since the sniffed traffic goes to whichever device is connected to THAT node.
- **Requires custom firmware with the sniffer module** (dedicated protobufs fork, [MT_SW_PROTOBUFS](https://github.com/MT-SW/MT_SW_PROTOBUFS)) — on older or stock firmware the toggle has no effect.

## Messaging

- **Photos in chat via link** — the app doesn't send raw photo bytes over LoRa (not enough bandwidth); instead it anonymously uploads the photo to an external server and sends just the link as a text message, with the recipient seeing an automatic preview. A confirmation dialog appears before sending, warning that the hosting server is public.
- **Preview for images pasted as links** — controlled by a separate toggle in Settings → Privacy (off by default), available on both Android and the desktop version.
- **Desktop: Enter = new line, Ctrl+Enter = send** — instead of forcing a send on plain Enter, matching the behavior people expect from desktop chat apps; on the phone, sending stays a separate button next to the text field.
- **Default Quick Chat templates** — the app's Quick Chat template list used to start out empty; now on first launch it's automatically seeded with a set of custom network commands (e.g. `scyzoryk pomoc`, `scyzoryk test`, `scyzoryk range`, `scyzoryk pogoda`, `scyzoryk info`, `scyzoryk aktualnosci`).

## Security

- **Node color picker when generating a key** — on the Security screen, next to the private key field, there's a color picker; the app grinds random X25519 keys locally on the phone (a few parallel threads, with a tolerance slider) until it finds one whose resulting node color is close enough to the chosen one. Since the tolerance allows some spread, the app shows the chosen color next to the actual result before the key is applied — you can accept it or search again; the key only goes into the field once confirmed, with no auto-save. Requires custom firmware that derives the node number from the public key (firmware 2.8+) — on older firmware the confirm button is grayed out and shows a message about the required update instead of grinding a key that wouldn't produce the expected color once connected to the device anyway.

## Branding and customization

- A custom `applicationId`, so the app installs side by side with the original Meshtastic app without conflicting (separate data, both can be installed at once).
- **Full visual rebrand** — the app and desktop build now have their own identity: the name MT_SW_APP everywhere in the UI and notifications, a custom icon (Świętokrzyskie voivodeship outline + "MT_SW" wordmark, replacing the earlier placeholder "MS+" mark), and a custom color palette (gold/navy instead of Meshtastic's default green branding) applied to the connection icon, the signed-node shield, and the last-heard time indicator throughout the app.
- A dedicated system-tray icon for the desktop build, matching the rest of the branding.
- The **"About" screen** now points to this fork's own repository (not the upstream project) and shows an explicit "unofficial fork, not affiliated with Meshtastic LLC" disclaimer — in line with the GPL-3.0 license and Meshtastic LLC's trademark policy.
- **Fixed an overly harsh Alert (SOS) notification sound** — the alert channel had its own dedicated sound file played in alarm mode; the app now uses the default system notification sound, matching every other notification channel.
- **Color-coded device connection status icon** — gold when connected, white while connecting/reconnecting, red when disconnected, yellow when the device is asleep; plus a brief flash of the icon itself and a matching glow, green when transmitting and blue when receiving data.
- The desktop build renamed from "Meshtastic Desktop" to match the mobile app's name, with a Privacy section added to its settings (previously missing on desktop even though the underlying logic already existed).
- Detection of the custom firmware edition used on the Świętokrzyskie network — the app shows a readable name instead of the raw technical value.
- A generated PL translation file filling in roughly 1,000 previously untranslated strings (the app was only about 43% translated into Polish).

## Status and caveats

- This is a personal, work-in-progress fork — some changes are build-verified and tested on-device, others are still awaiting confirmation in the field.
- No official releases/tags — changes are kept up to date directly on the `main` branch.
- The fork uses the same GPL-3.0 license as the upstream project.

---
*Based on [meshtastic/Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android). Unofficial, not affiliated with Meshtastic LLC.*