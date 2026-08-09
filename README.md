# Meshtastic_S+ — osobisty fork Meshtastic-Android

Fork oficjalnej aplikacji [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android) rozwijany na potrzeby sieci mesh radiowej **Świętokrzyskie** (meshtastic-swietokrzyskie.pl). Bazuje na architekturze KMP/Compose Multiplatform oryginału (warianty `fdroid`/`google`, moduł desktopowy) i dokłada zestaw lokalnych funkcji, poprawek i personalizacji, których nie ma w wersji upstream.

Stan roboczy — repo służy głównie do własnego użytku i testów z niewielką grupą osób, niekoniecznie buduje się na bieżąco.

## Zarządzanie węzłami i siecią mesh

- **Zdalne sterowanie GPIO** — na ekranie szczegółów węzła (moduł Remote Hardware) można wpisać numer pinu, appka sama liczy maskę bitową i wysyła `WRITE_GPIOS`/`READ_GPIOS` do zdalnego węzła. Przyciski aktywne tylko gdy klucze PKC z węzłem zostały wymienione.
- **Zdalne ulubione/ignorowanie węzłów** przez sieć LoRa (nie tylko lokalnie) — z prawdziwym potwierdzeniem doręczenia opartym o routing ACK z mesh, zamiast tylko zmiany po stronie telefonu.
- **Ręczne dodawanie kontaktu przez ID węzła** — zarówno lokalnie, jak i zdalnie, z ujednoliconym formatem `!a1b2c3d4` (hex) wszędzie w appce.
- **Pasywne zbieranie NeighborInfo** — log sąsiadów pokazuje teraz też podsłuchane rozgłoszenia innych węzłów, nie tylko odpowiedzi na własne zapytania; naprawiony też przypadek żądania Neighbor Info dla własnego, lokalnie podłączonego urządzenia (wcześniej nic nie zwracał).
- **Przyciski szybkich komend** (`/ping`, `/hello`, `/test`) na ekranie węzła — wysyłają wiadomość prywatną nawet do węzłów, których rola normalnie blokuje ręczne wiadomości; przydatne do szybkiego testowania nowych buildów firmware na urządzeniach w terenie.
- **Lista węzłów pośredniczących (relay) w dostarczeniu wiadomości** — dialog "Status doręczenia" pokazuje pełną listę nazw wszystkich węzłów biorących udział w retransmisji (nie tylko licznik ani jedną nazwę jak w oryginale), łącznie z poprawkami po stronie firmware, żeby te dane w ogóle docierały do appki.
- Przywrócony ekran konfiguracji **Traffic Management** (w pewnym momencie usunięty w upstreamie).

## Ekran "Zdrowie sieci"

Nowa, szósta zakładka w dolnej nawigacji (między Węzłami a Mapą), której nie ma w oryginalnej appce:

- **6 kategorii metryk** per węzeł: zasilanie (bateria/napięcie/prąd), sygnał (SNR/RSSI/poziom szumu, z fallbackiem na liczbę przeskoków gdy brak bezpośrednich odczytów), sieć (kanał/eter), środowisko (temperatura/wilgotność/ciśnienie), ruch (TX/RX/duplikaty/przekazane/uszkodzone) i sąsiedzi.
- Lista węzłów z sortowaniem, przypinaniem ulubionych na górze, ukrywaniem pustych wpisów i wyszukiwarką; szczegóły każdej metryki jako wykres w oknie 24h/7d/30d.
- **Ekran "Podsumowanie"** — karty z rankingami top-3: najcichsze węzły, najlepszy sygnał, najwięcej wysłanych pozycji, fizycznie najbliższe węzły, najwięcej danych telemetrii, najwięcej wiadomości (tydzień/dziś) i inne — wszystkie poprawnie wykluczają lokalnie podłączone urządzenie z rankingów, żeby nie zaburzało wyników.
- Architektura danych: wszystko dekodowane na żywo z istniejącego logu zdarzeń mesh przy każdym odczycie ekranu — żadne dane nie są duplikowane w osobnej tabeli, więc statystyki są zawsze aktualne i nie zajmują dodatkowego miejsca w bazie.

## Komunikator

- **Zdjęcia w czacie przez link** — appka nie wysyła surowych bajtów zdjęcia przez LoRa (za mała przepustowość), tylko uploaduje je anonimowo na zewnętrzny serwer i wysyła sam link jako wiadomość tekstową; odbiorca widzi automatyczny podgląd. Przed wysyłką pojawia się dialog ostrzegający, że serwer hostingu jest publiczny.
- **Podgląd obrazków wklejonych jako link** — sterowany osobnym przełącznikiem w Ustawienia → Prywatność (domyślnie wyłączone), dostępny zarówno na Androidzie, jak i w wersji desktopowej.
- **Desktop: Enter = nowa linijka, Ctrl+Enter = wyślij** — zamiast wymuszonego wysyłania samym Enterem, zachowanie typowe dla komunikatorów na komputerze; na telefonie wysyłanie zostaje osobnym przyciskiem obok pola tekstowego.

## Mapa

- Domyślne centrum i przybliżenie mapy ustawione tak, żeby przy pierwszym uruchomieniu (zanim appka zdąży pobrać pozycje węzłów) nie pokazywała pustego oceanu, tylko sensowny punkt startowy; po załadowaniu węzłów mapa i tak wycentrowuje się na realnym obszarze sieci.
- Naprawiony efekt "przelotu przez ocean" (chwilowe pokazanie punktu 0,0 przed wyśrodkowaniem na właściwej pozycji) na głównym ekranie mapy.

## Branding i personalizacja

- Własny `applicationId`, dzięki czemu appka instaluje się obok oryginalnej appki Meshtastic bez konfliktu (osobne dane, można mieć obie naraz).
- Zmieniona nazwa i ikona głównego (trwałego) powiadomienia appki oraz ikona samej aplikacji.
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



# Meshtastic_S+ — personal Meshtastic-Android fork

A fork of the official [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android) app, developed for the **Świętokrzyskie** mesh radio network (meshtastic-swietokrzyskie.pl). Builds on the upstream KMP/Compose Multiplatform architecture (`fdroid`/`google` flavors, desktop module) and adds a set of local features, fixes, and customizations not found in the upstream version.

Work in progress — this repo is mainly for personal use and testing with a small group of people; it doesn't necessarily build cleanly at all times.

## Node and mesh network management

- **Remote GPIO control** — on the node detail screen (Remote Hardware module) you can enter a pin number; the app computes the bitmask itself and sends `WRITE_GPIOS`/`READ_GPIOS` to the remote node. Buttons are only enabled once PKC keys have been exchanged with that node.
- **Remote favorite/ignore over the LoRa mesh** (not just locally) — with real delivery confirmation based on mesh routing ACKs, instead of only a local, phone-side change.
- **Manual contact add by node ID** — both locally and remotely, with a unified `!a1b2c3d4` (hex) format used everywhere in the app.
- **Passive NeighborInfo collection** — the neighbor log now also shows overheard broadcasts from other nodes, not just responses to your own requests; also fixes requesting Neighbor Info for your own, locally connected device (previously returned nothing).
- **Quick command buttons** (`/ping`, `/hello`, `/test`) on the node screen — send a private message even to nodes whose role normally hides the manual message option; useful for quickly testing new firmware builds on devices in the field.
- **Full list of relay nodes for message delivery** — the "Delivery status" dialog shows the full list of node names involved in relaying a message (not just a count or a single name like upstream), including firmware-side fixes so that data actually reaches the app.
- Restored the **Traffic Management** configuration screen (removed from upstream at one point).

## "Network Health" screen

A new, sixth tab in the bottom navigation (between Nodes and Map) that doesn't exist in the original app:

- **6 metric categories** per node: power (battery/voltage/current), signal (SNR/RSSI/noise floor, with a hop-count fallback when there are no direct readings), network (channel/air utilization), environment (temperature/humidity/pressure), traffic (TX/RX/duplicates/relayed/corrupted), and neighbors.
- Node list with sorting, pinning favorites to the top, hiding empty entries, and search; each metric's detail view is a chart over a 24h/7d/30d window.
- **"Summary" screen** — cards with top-3 rankings: quietest nodes, best signal, most positions sent, physically closest nodes, most telemetry data, most messages (week/today), and more — all correctly exclude the locally connected device from the rankings so it doesn't skew results.
- Data architecture: everything is decoded live from the existing mesh event log every time the screen is read — nothing is duplicated into a separate table, so the stats are always current and take no extra database space.

## Messaging

- **Photos in chat via link** — the app doesn't send raw photo bytes over LoRa (not enough bandwidth); instead it anonymously uploads the photo to an external server and sends just the link as a text message, with the recipient seeing an automatic preview. A confirmation dialog appears before sending, warning that the hosting server is public.
- **Preview for images pasted as links** — controlled by a separate toggle in Settings → Privacy (off by default), available on both Android and the desktop version.
- **Desktop: Enter = new line, Ctrl+Enter = send** — instead of forcing a send on plain Enter, matching the behavior people expect from desktop chat apps; on the phone, sending stays a separate button next to the text field.

## Map

- Default map center and zoom set so that on first launch (before the app has fetched node positions) it doesn't show an empty ocean, just a sensible starting point; once nodes load, the map still re-centers on the actual network area.
- Fixed the "flying through the ocean" effect (briefly showing point 0,0 before centering on the real position) on the main map screen.

## Branding and customization

- A custom `applicationId`, so the app installs side by side with the original Meshtastic app without conflicting (separate data, both can be installed at once).
- Changed name and icon for the app's main (persistent) notification, plus a custom app icon.
- The desktop build renamed from "Meshtastic Desktop" to match the mobile app's name, with a Privacy section added to its settings (previously missing on desktop even though the underlying logic already existed).
- Detection of the custom firmware edition used on the Świętokrzyskie network — the app shows a readable name instead of the raw technical value.
- A generated PL translation file filling in roughly 1,000 previously untranslated strings (the app was only about 43% translated into Polish).

## Status and caveats

- This is a personal, work-in-progress fork — some changes are build-verified and tested on-device, others are still awaiting confirmation in the field.
- No official releases/tags — changes are kept up to date directly on the `main` branch.
- The fork uses the same GPL-3.0 license as the upstream project.

---
*Based on [meshtastic/Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android). Unofficial, not affiliated with Meshtastic LLC.*

