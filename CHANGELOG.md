# Changelog / Lista zmian

Dwujęzyczny plik zmian: najpierw **polska** część, niżej **angielska** (oryginał).
A bilingual changelog: the **Polish** part comes first, the **English** original below it.

* [Polski](#polski) — zmiany forka MT_SW oraz tłumaczenie wydań z upstreamu
* [English](#english) — MT_SW fork changes and the upstream releases (original text)

---

# Polski

Ten plik opisuje zmiany wprowadzone w aplikacji MT_SW_APP. Sekcja „Zmiany forka MT_SW” jest prowadzona ręcznie. Sekcja „Wydania z upstreamu” to tłumaczenie na polski oryginalnych notatek wydań projektu [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android) (opisy zmian przetłumaczone, tytuły zmian, autorzy i linki bez zmian). Kolejne wydania z upstreamu automat dopisuje tylko w części angielskiej, więc polską wersję trzeba uzupełnić ręcznie. Pełna historia: [Wydania na GitHubie](https://github.com/meshtastic/Meshtastic-Android/releases).

## Zmiany forka MT_SW

Wpisy niżej opisują wyłącznie to, co ten fork dokłada do upstreamu, od początku (27 lipca 2026) do dziś, w kolejności chronologicznej. Sekcje `[Unreleased]` i `[x.y.z]` dalej w pliku pochodzą z upstreamu. Opis funkcji: [README.md](README.md).

### Październik 2026

**Mesh Link Planer (natywny planer zasięgu, 1–2.10)**
* Natywny planer zamiast zewnętrznego Site Plannera, w całości w aplikacji (Android i desktop): dwa niezależne punkty A/B (lista węzłów, mapa, współrzędne, stacja), teren Mapterhorn, ITM/Longley-Rice, strefa Fresnela, wykres profilu, bilans łącza, namiary i kąty anten, porównanie z ostatnim pomiarem, zasięg dookólny, eksport PDF/CSV/KML/GeoJSON/PNG, okna informacji „i”, podziękowania i licencje.
* Kreator kabli i złączy (baza z kart katalogowych, złącza bez kabla, np. przejściówki SMA→N), wszystkie pasma od 169 MHz do 2,4 GHz i dowolna częstotliwość; wybór pasma ustawia częstotliwość i szerokość kanału (2,4 GHz jako szeroki LoRa, pierwszy slot dla 433 i 470 MHz).
* Domyślnie preset Narrow Fast i częstotliwość 869,44165 MHz; presety bez ręcznego BW i SF; zmiana presetu ustawia jego domyślną częstotliwość.
* Zasięg na mapie jako gęsty raster z płynną skalą kolorów (styl MeshMap Planner), suwak krycia, lista warstw u góry z przewijaniem, licznik warstw na ikonie, limit 9 warstw, warstwy zasięgu nie są zapamiętywane po restarcie. Nazwa „Mesh Link Planer (by MT_SW)”.
* Opcjonalna pogoda na żywo (Open-Meteo): współczynnik k, refrakcja, ducting.
* Przeszkody: presety zabudowy/roślinności (domyślnie) albo opcjonalne **dokładne odwzorowanie terenu z OpenStreetMap** (budynki i lasy przez Overpass API, na trasie i w zasięgu do wybranego promienia od środka), z ostrzeżeniem o większym zużyciu zasobów i dłuższym liczeniu; przy błędzie pobierania powrót do presetu.
* Menu zasięgu danych o terenie (5 / 10 / 15 / 30 / 50 / 100 km, domyślnie 30 km); pobieranie z OpenStreetMap jest czytane kawałkami z limitem rozmiaru, a brak pamięci jest łapany, więc gęsty obszar nie wywala już aplikacji (także na emulatorze z małą pamięcią), tylko pokazuje komunikat „za dużo danych, zmniejsz zasięg” i wraca do ustawienia wstępnego przeszkód, także podczas liczenia zasięgu.
* Naprawa „zabrakło pamięci” przy dokładnym odwzorowaniu terenu: limit pamięci serwera Overpass w zapytaniu (wcześniej błędnie obniżony do 16 MB) wrócił do bezpiecznej wartości, więc serwer nie odmawia już nawet dla małego obszaru; odmowa serwera („out of memory”, przeciążenie, HTTP 429/504) ma teraz osobny komunikat zamiast „urządzeniu zabrakło pamięci”, z krótkim powodem podanym przez serwer; przy odmowie planer próbuje zapasowych serwerów Overpass.
* Zasięg danych o terenie powyżej 10 km (50 i 100 km): obszar jest pobierany kawałkami (kafelki po ok. 30 km, jedno zapytanie naraz) zamiast jednej ogromnej odpowiedzi, której nie udźwignąłby serwer ani urządzenie; kafelek zbyt ciężki dla serwera jest dzielony na cztery, zajęty serwer jest cierpliwie czekany, obiekty na granicach kafelków liczą się raz, punkty obrysów są przerzedzane (od 25 m, mocniej im większy zasięg, a drobne lasy poniżej tej skali pomijane), żeby oszczędzić pamięć, a ostatni działający serwer jest zapamiętywany. Zapytanie o tereny używa dokładnych dopasowań (`landuse=forest`, `residential` itd.) zamiast wyrażenia regularnego, więc serwer nie czyta już wszystkich pól i łąk w kafelku i odpowiada znacznie szybciej. Całe pobieranie ma limit 12 minut i kończy się całością albo błędem (bez „połowicznych” map). Postęp (fragment x z y) widać pod paskiem zasięgu, a długie pobieranie nie blokuje już zwykłego liczenia łącza A–B. Zapytania do Overpass mają własne limity (90 s oczekiwania na dane zamiast 30 s i bez automatycznego ponawiania, które powtarzało ciężkie zapytania); przekroczenie czasu oznacza „kafelek za ciężki” i dzieli go na mniejsze, a chwilowy zanik sieci jest ponawiany. Zapytanie o przeszkody na trasie łącza jest dzielone na lżejsze części (lasy, potem budynki przy każdym końcu) z limitem serwera 60 s i jedną ponowną próbą, bo jedno ciężkie zapytanie o długie łącze przekraczało limit czasu serwera; najmniejszy kafelek zasięgu, na którym serwer przekroczył czas, jest ponawiany po krótkim oczekiwaniu.
* Komunikat pod ustawieniami dokładnego terenu podaje teraz wybrany zasięg danych zamiast stałych „30 km”.
* Okno traceroute: gdy trasy nie da się pokazać na mapie, jest jeden przycisk OK (wcześniej obok pojawiał się drugi); opisy trasy na mapie mają cieńszą obwódkę i większą czcionkę.

**Sniffer, klucze, coding rate (8.10)**
* Sniffer: po ponownym połączeniu stan jest odczytywany od nowa — aplikacja nie pokazuje już starej odpowiedzi z poprzedniego połączenia i dopytuje radio kilka razy, aż odpowie.
* Desktop: w ustawieniach Zabezpieczenia są teraz przyciski „Kopia kluczy”, „Przywróć klucze” i „Usuń kopię kluczy” — tak samo jak w aplikacji na telefon.
* Coding rate: dla presetów Narrow (Fast/Slow) i Tiny Slow, które domyślnie mają 4/6, na liście dostępne jest także 4/5.
* Budowanie na GitHubie (Actions → „Build Binaries (Manual)”): każdy system i format wybierasz osobno — Windows (.msi, .exe, .jar), Linux (.deb, .rpm, .AppImage, .jar), macOS (.dmg, .jar) i Android (APK).

**Mapa**
* Płynne przewijanie i powiększanie przy dużej liczbie węzłów: usunięte z danych mapy pola „ostatnio słyszany”/„online”, węzły odświeżane tylko przy zmianie pozycji, nazwy, ulubionego i ignorowanego, błysk na własnym małym źródle w 15 krokach, stabilny obszar widoku podczas przesuwania, pamięć podręczna obrazków plakietek, brak niewidocznych obrysów w warstwie zasięgu.
* **Trasa traceroute na mapie z siłą sygnału:** przycisk „Pokaż na mapie” tylko gdy wszystkie węzły trasy mają lokalizację (inaczej okno tłumaczy dlaczego); osobna cienka linia na skok w kolorze jakości sygnału, ze strzałką na końcu i SNR w dB wzdłuż linii, kończąca się tuż przed węzłem (węzły jako punkty pod plakietkami), oba kierunki symetrycznie obok siebie; legenda jakości; świeżo otrzymany wynik traceroute od razu ma wartości SNR (wcześniej linie były szare do otwarcia z zapisanych tras).

**Sieć**
* Lista ostatnio używanych urządzeń sieciowych (WiFi/TCP): 20 wpisów zamiast 3.

**Opisy i nazewnictwo (4.10)**
* **Opis przy sterowaniu GPIO pod ikoną informacji:** nagłówek karty GPIO w szczegółach węzła ma ikonę „i”, która otwiera krótką instrukcję krok po kroku — moduł Zdalny sprzęt i dostępne piny na węźle docelowym, kanał „gpio” na pozycji 1 z tym samym kluczem na obu urządzeniach, znaczenie przycisków 1 / 0 / Odczyt.
* **Jednolita nazwa „Diagnostyka na żądanie”** w całej aplikacji (sniffer, nazwa portu w pakietach, opisy) zamiast „OnDemand”.

**Węzły, czyszczenie bazy i scalenie z upstreamem (3.10)**
* **Czyszczenie węzłów przez Bluetooth działa do końca:** polecenia „usuń węzeł” do radia są wysyłane po kolei, z krótką przerwą i ponawiane, gdy łącze chwilowo ich nie przyjmuje (Bluetooth przyjmuje naraz tylko kilka zapisów, więc wcześniej przy setkach węzłów większość poleceń była po cichu odrzucana, a radio oddawało te węzły po ponownym połączeniu). Wysyłanie kończy się nawet po wyjściu z ekranu; gdy radio nie jest połączone, czyszczone jest tylko to, co w aplikacji. Przez WiFi działało dobrze już wcześniej.
* **Czyszczenie wszystkich nieznanych węzłów bez względu na datę:** po włączeniu „Wyczyść tylko nieznane węzły” pojawia się przełącznik „Ignoruj datę: wszystkie nieznane węzły bez klucza” (węzły, które się nie przedstawiły i dla których nie ma klucza; ulubione i ignorowane zawsze zostają).
* **Czyszczenie węzłów z niezgodnym kluczem:** nowy przełącznik „Tylko węzły z niezgodnym kluczem (usuwane z aplikacji i z radia)” na ekranie czyszczenia — lista pokazuje wszystkie węzły ze stanem „niezgodny klucz” bez względu na wiek (ulubione, ignorowane i własny węzeł zostają), a czyszczenie usuwa je z bazy aplikacji i z radia.
* **Czyszczenie samej bazy węzłów w aplikacji** (bez kontaktu z radiem, działa też bez połączenia): przycisk na dole ekranu czyszczenia z potwierdzeniem i przełącznikiem „Zachowaj ulubione węzły”; po jego wyłączeniu lista idzie do zera. Podłączone radio wyśle swoje węzły ponownie przy następnej synchronizacji.
* **Płynniejszy pasek szybkiego przewijania (desktop):** nie przelicza już całego ekranu przy każdej klatce przewijania, suwak sunie razem z listą (zamiast skakać co element), a przeciąganie przesuwa listę ciągle, w pikselach.
* **Scalenie z upstreamem (meshtastic/Meshtastic-Android):** protobufy 2.8.1 i nowe zależności, nowe funkcje upstreamu (m.in. zdalny terminal, powiadomienia o reakcjach, stronicowane logi, `SendMessageOutcome`), nowe teksty przetłumaczone na polski; nasze tłumaczenia, branding, planer i pasek przewijania zostały zachowane. Polecenie formatowania czasu i rozmiarów przeszło na nowe funkcje upstreamu.

**Dokumentacja**
* README i CHANGELOG uzupełnione o całą historię forka.

### Wrzesień 2026

**Jakość sygnału, LNA, logi (29–30.09, 1.10)**
* Korekta wzmocnienia LNA dla szumu i RSSI (pole w LoRa pod mocą radia i w oknie „Jakość sygnału”, osobne pole dla obcych węzłów w szczegółach węzła, wykresy, Zdrowie sieci, średni szum w podsumowaniu).
* Ocena SNR względem presetu (Narrow −3/−7/−12 dB, Lite −5/−10/−15 dB), RSSI może tylko obniżyć ocenę; kolory jak w statusach połączenia (dobry złoty, wystarczający czerwony, słaby fioletowy, brak biały/czarny zależnie od motywu); opis z progami zależnymi od presetu; kolorowanie SNR w szczegółach pakietów sąsiadów i w traceroute.
* Kolory statusów wiadomości (dostarczono, potwierdzono, błąd, tarcza podpisu na złoto).
* Logi urządzenia w panelu debug (na żywo, wyszukiwanie, eksport; Wi-Fi i USB tylko z firmware MT_SW), czas z ramki w snifferze, przycisk „Wymuś zatrzymanie aplikacji”.
* Desktop: naprawione puste listy rozwijane w wersji spakowanej (ProGuard i wartości enum).

**Sniffer i OnDemand (5–26.09)**
* Diagnostyka na żądanie (OnDemand, port 354): 10 typów zapytań; naprawione gubienie zapytań do lokalnie podłączonego węzła.
* Sniffer radiowy i MQTT na jednym ekranie z wyborem źródła, potwierdzenie przed włączeniem, pokazywanie ruchu broadcastowego i własnych pakietów, dekodowanie po kliknięciu, kopiowanie, automatyczne przewijanie z przyciskiem live/pauza i licznikiem nowych pakietów, krótkie nazwy węzłów, znaczniki czasu co do sekundy.
* Przejście snifera na oficjalne protobufy przez OnDemand (port 354); wykrywanie wsparcia po wersji firmware (próg 2) oraz po zgłoszeniu modułu.
* Grupowanie duplikatów z listą odbiorów, zapis i wczytywanie logu (txt/JSON/CSV), bufor 5000 pakietów z licznikiem i konfigurowalną polityką przepełnienia, przeprojektowane karty pakietów (kategorie i kolory), filtr kanału 0, podsumowania telemetrii (uptime, prąd, zajętość kanału i eteru, CPU i pamięć hosta, power metrics, local stats).
* Poprawki: pakiety nieodszyfrowalne trafiają do logu, wybór kanału przez hash, kategoria „Nieznany”, znikanie treści po grupowaniu, trasa powrotna traceroute (SNR), podwójne wpisy przy własnym traceroute, stan snifera po ponownym połączeniu i przy wejściu na ekran, MQTT na desktopie, limit 15 s połączenia MQTT.

**Komunikator**
* Długie wiadomości dzielone na części i składane na bieżąco po odebraniu (podgląd na liście kontaktów, status wysyłania i odbierania zależny od kierunku) (24–25.09).
* Zdjęcia przez link, zapis zdjęcia z podglądu, desktop: Enter = nowa linia, Ctrl+Enter = wysłanie; domyślne szablony Szybkiego Czatu zapisywane per baza.

**Bezpieczeństwo i węzły**
* Wybór koloru węzła przy generowaniu klucza (lokalne szukanie klucza X25519) (2.09).
* Automatyczne czyszczenie bazy węzłów (Android: WorkManager, desktop: pętla godzinowa) (2.09).
* Spolszczone role urządzeń i sekcja „Role” w pomocy; nowe sekcje „Bezpieczeństwo” i „Połączenie” w pomocy listy węzłów (13.09).
* Ujednolicone kolory wskaźników bezpieczeństwa węzła (złoty = zaufanie, czerwony = niezgodność klucza).

**Mapa i desktop**
* Domyślna mapa OpenStreetMap, mniejsze plakietki z ogonkiem, rozsunięcie węzłów o identycznej pozycji, dostrojone klastrowanie, ostrzejsze kafelki rastrowe, mapa zawsze dopasowana do wszystkich węzłów, naprawiona migająca liczba w klastrze, mniejsze przybliżenie mini-mapy w szczegółach węzła, naprawiony padający selektor zakresu czasu.
* Desktop: trzecia kolumna podąża za wybranym węzłem, pasek szybkiego przewijania, naprawiona mapa w spakowanym MSI (ProGuard/LWJGL), biblioteki macOS ARM64, wykrywanie Bluetooth na Windows.
* Naprawione ponowne łączenie Bluetooth z już sparowanym urządzeniem (Xiaomi/MIUI) (8.09). Rozmrożony panel Advanced podczas przełączania Snifera (7.09).
* Odczyt `local_stats_extended` jako natywnego pola telemetrii po zmianie protobufów (7.09).

**Utrzymanie**
* Synchronizacje z upstreamem (1.09, 13.09, 20.09) wraz z aktualizacją protobufów (2.8.0.x); uzupełnione brakujące tłumaczenia PL (w tym 320 wpisów schematu); poprawki formatowania i statycznej analizy.

### Sierpień 2026

* **Zdrowie sieci** (1–2.08): nowa zakładka z metrykami zasilania, sygnału, eteru, środowiska, ruchu i sąsiadów; podsumowanie z rankingami; dane dekodowane na żywo z logu mesh. W sierpniu doszła zakładka **Zasoby** (CPU/pamięć/flash/PSRAM) i widżet „Local Stats”.
* **Nazwa węzła pośredniczącego (relay)** na liście węzłów i w szczegółach oraz pełna lista relayów w statusie doręczenia (do 10.08, 15.08).
* Rozpoznawanie edycji firmware MT-SW (5.08), własny `applicationId` – aplikacja instaluje się obok oryginału (2.08).
* Przyciski szybkich komend `/ping` `/hello` `/test` (3.08), domyślne szablony Szybkiego Czatu (24.08), uptime z ikoną (2 i 10.08).
* Ukryte przestarzałe role (REPEATER, ROUTER_CLIENT), naprawiona mapa śladu pozycji startująca w oceanie, import/eksport konfiguracji na desktopie, własna mapa desktopowa (później zastąpiona MapLibre).
* **Rebranding (27–29.08):** MT_SW_APP, autorska ikona (kontur województwa i „MT_SW”), paleta złoto/granat, ekran „O aplikacji” z zastrzeżeniem o nieoficjalnym forku, nazwa wersji desktopowej, ikona w zasobniku.
* Synchronizacje z upstreamem (5.08, 10.08, 15.08, 16.08, 29.08); przejście na wspólny silnik mapy MapLibre (Android i desktop), domyślnie włączone nakładki cieniowania terenu i radaru pogody.

### Lipiec 2026 — start forka

* Przywrócony ekran Traffic Management (27.07).
* Zawsze widoczna lista sparowanych urządzeń Bluetooth; widoczność Remote Hardware; zdalne ulubione/ignorowanie przez sieć LoRa z potwierdzeniem routing ACK (27.07).
* Ręczne dodawanie kontaktu po ID (lokalnie i zdalnie, format `!a1b2c3d4`) i zdalne sterowanie GPIO (29.07).
* Ostrzeżenia o paśmie wąskim i o wyjściu poza region świętokrzyski; pasywny NeighborInfo z naprawami (30.07).
* Podgląd obrazków z linków w czacie (domyślnie wyłączony); pełny plik tłumaczeń PL (ok. 1000 tekstów) (30.07).

## Wydania z upstreamu (tłumaczenie)

## [2.8.2] - 2026-09-26

### 🏗️ Funkcje
* perf(ui): renderowanie kodów QR w gęstości wyświetlacza zamiast stałych 960px by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6793
* feat: zgłaszanie zabić procesu przez limiter pamięci Androida 17 za pomocą ApplicationExitInfo by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6792
* feat(privacy): ochrona wrażliwych treści interfejsu przed usługami dostępności niebędącymi narzędziami by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6794
* feat(hardware): pobieranie z API specyfiki OTA bootloadera, zasilane z dołączonego zasobu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6802
* feat(hardware): pobieranie z API manifestu UF2 konserwacji, z przypiętym skrótem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6803
* feat(messaging): przesunięcie do odpowiedzi, reakcje podwójnym dotknięciem, separatory dni by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6850
* feat(messaging): szkice dla każdego kontaktu i cichsze pole wpisywania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6851
* feat(messaging): nazwa nadawcy na przycisku przejścia do najnowszych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6856
* feat(notifications): udostępnianie rozmów jako dymków by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6858
* feat(messaging): oznaczanie jako nieprzeczytane, akcje przesunięcia wiersza i przypięte rozmowy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6857
* feat(messaging): zamykanie paska szybkich reakcji po dotknięciu poza nim by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6865
* feat(settings): wysyłanie opcjonalnej nazwy ham long_name razem ze znakiem wywoławczym by @vidplace7 in https://github.com/meshtastic/Meshtastic-Android/pull/6875
* feat(appfunctions): uzgadnianie stanu systemu zamiast zapisu na ślepo by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6908
* feat(units): określanie jednostek na podstawie regionu urządzenia, nowe ustawienie Jednostki, renderowanie przez ICU by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6916
* feat(connections): powiadomienie o wyłączonej transmisji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6945
* feat(agents): dodanie umiejętności run-meshtastic-android ze sterownikami desktop i emulatora by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6955
* feat(map): osobny suwak przezroczystości dla każdej warstwy mapy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6958
* feat(map): filtrowanie mapy według roli węzła i sposobu, w jaki węzeł został usłyszany by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6959
* feat(settings): dostosowanie edytora konfiguracji Mesh Beacon do design#140 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6963
* feat(discovery): pomijanie zaproszeń beacon do kanałów, które radio już ma by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6964
* feat(network): konfiguracja silników klienta HTTP i dodanie nagłówków User-Agent dla platform by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7008
* feat(ui): uwzględnianie poziomu szumów w ocenie jakości sygnału (design#15) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7009
* feat(map): baner offline i automatyczne przełączanie na mapę Google przy utracie łączności by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6998
* feat(node): skrót do statusu aktualizacji w menu lokalnego węzła by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7047
* feat: oznaczanie węzłów niesłyszanych od zmiany konfiguracji LoRa by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7055
* chore: ujawnienie połączenia z urządzeniem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7068
* feat(node): selektor przedziału czasu zwija się do menu zamiast być ściskany by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7097
* perf: okno cache dla często używanych list oraz modernizacja nav3 i adaptive by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7093
* feat(docs): prawdziwy pasek wyszukiwania M3 w wyszukiwarce dokumentacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7098
* feat(nfc): udostępnianie kontaktu lub kanału przez zbliżenie telefonów, bez tagu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7124
* feat(ui): udostępnianie linku z okna udostępniania i opis działania okna by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7126
* feat(node): wskaźniki podpisu i weryfikacji zamiast kłódki PKI by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7117
* fix(node): zachowanie klucza publicznego kontaktu, gdy nadejdzie inny by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7118
* feat(settings): wyświetlanie informacji o licencji na ekranie O aplikacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7191
* feat(map): ograniczenie przybliżenia przy dopasowywaniu granic i hardware bitmaps dla obrazów stylu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7245
* refactor(navigation): obsługa deep linków przez navigation3 UriDeepLinkMatcher by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7246
* refactor(settings): odczyt granic pól liczbowych z rejestru pól protobufs by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7242
* feat(telemetry): wyświetlanie metryk wyładowań atmosferycznych, statusu PM i wilgotności gleby by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7237
* feat(node): sprzęt producentów w znaczku urządzenia klasyfikowany między wspieranym a społecznościowym by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7263
* feat(settings): ograniczanie pól konfiguracji według wersji firmware ze schematu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7262
* feat(settings): ukrywanie ustawień modułów, które węzeł zgłasza jako niewkompilowane by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7272
* feat(mqtt): wyświetlanie odrzuconej przez brokera subskrypcji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7290
* feat(settings): sterowanie bramkami modułów ze schematu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7296
* feat(settings): etykiety list wyboru enum ze schematu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7298
* feat(settings): odczyt etykiet i tekstów pomocniczych, które schemat już zawiera by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7302
* feat(beacon): uwzględnianie ogłaszanego slotu częstotliwości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7305
* feat(settings): generowanie prefiksów kluczy enum używanych w ciągach schematu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7306
* feat(search): jeden pasek wyszukiwania M3, wyszukiwanie ustawień i status węzła w wynikach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7303
* feat(settings): wyświetlanie jednostki zadeklarowanej w schemacie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7304
* feat(connections): obsługa sprzętu bez Bluetooth dla Android XR by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7318
* feat(connections): obsługa sprzętu bez hosta USB dla Android XR by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7322
* refactor(connections): osobna sekcja dla trybu Demo by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7326
* feat(messaging): informacja, czy potwierdzenie doręczenia zostało udowodnione by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7284
* feat(messaging): zapisywanie dowodu podpisu i potwierdzenia przy reakcjach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7339
### 🖥️ Desktop
* fix(desktop): wyłączenie powiadomień macOS, gdy proces nie ma pakietu aplikacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6876
* fix(desktop): sprawdzanie ścieżki pakietu, a nie identyfikatora, przed powiadomieniem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6885
* fix(desktop): użycie standardowego identyfikatora licencji SPDX w pakiecie RPM by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/7043
* fix(desktop): przypięcie zrzutów ekranu Flathub do commita, który przetrwa squash by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7243
* fix(desktop): zachowanie metod upcall FFI MapLibre w ProGuard by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7287
* refactor(model): jednorazowe parsowanie adresu wybranego urządzenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7328
### 🛠️ Poprawki
* fix(navigation): czyszczenie pamięci podręcznej odtwarzania deep linków po zastosowaniu do stosu wstecznego by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6796
* fix(ui): informacja zwrotna, gdy import kontaktu lub kanału nadejdzie podczas rozłączenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6798
* fix(database): przebudowa indeksu FTS pakietów po ponownym utworzeniu tabeli w schemacie 52 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6808
* fix(map): podniesienie android-maps-utils do 5.1.1, aby import KML działał z xmlutil 1.0.x by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6811
* fix(database): limit czasu zajętości dla każdego połączenia SQLite by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6809
* fix(firmware): wyświetlanie oczekiwania na wymazanie i ponowień wysyłania podczas Legacy DFU by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6812
* fix(map): parsowanie archiwów KMZ w rendererze nakładek mapy F-Droid by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6834
* fix(firmware): mapowanie wariantów SoftDevice dla nowych płytek nrf52840 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6843
* fix(navigation): aktywny stos wsteczny nigdy nie może się opróżnić pod NavDisplay by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6844
* fix(metrics): usunięcie awarii underflow przy przywracaniu canvas w Vico by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6847
* fix(ui): ograniczenie wysokości zawartości paneli w adaptacyjnym trójpanelowym scaffoldzie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6845
* fix(connection): wydłużanie limitów czasu handshake BLE, dopóki trwa postęp konfiguracji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6848
* fix(map): odporność na wyścig aktualizacji dostawcy WebView w Site Planner by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6846
* fix(mqtt): tolerowanie ładunku typu obiekt w wiadomościach MQTT JSON by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6839
* fix(messaging): czyszczenie i wyciszanie powiadomień dla rozmowy widocznej na ekranie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6849
* fix(messaging): akcje przesunięcia wiersza wywoływane raz na przesunięcie, a snackbar może zniknąć by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6863
* Ustabilizowanie testu stanu jednostek lokalizacji w trakcie sesji by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6870
* Obniżenie poziomu powtarzających się logów odrzucenia heartbeat offline do Debug by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6872
* Użycie sond tylko skanujących po długotrwałych niepowodzeniach ponownego połączenia sparowanego BLE by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6871
* fix(permissions): odzyskiwanie po pominiętym, odmówionym lub cofniętym uprawnieniu zamiast ślepego zaułka by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6880
* fix(node): przeliczanie temperatury przed oznaczeniem jej jako °F i wyświetlanie wiatru w km/h by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6925
* fix(settings): usunięcie pojedynczych skalarów celu beacon zarezerwowanych przez protobufs by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6942
* fix(node): przewijanie okien szczegółów historii traceroute/logów by @beecho01 in https://github.com/meshtastic/Meshtastic-Android/pull/6708
* fix(ui): wyświetlanie ról urządzeń po nazwie i koniec z podwajaniem dwóch znaków procenta by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6962
* fix(map): bramkowanie edytora punktów trasy MapLibre przez isModifiableBy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6969
* fix(ai): ograniczenie wiadomości asystenta do tego, co ścieżka wysyłania faktycznie zakoduje by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6970
* fix(firmware): ukrycie konserwacji USB tam, gdzie platforma nie może jej uruchomić by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6974
* fix(map): ustalenie zapisanej mapy bazowej przed pierwszym renderowaniem mapy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6978
* fix(network): wskazanie bazowego URL API na host apiv2 oparty na R2 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7004
* fix(settings): lista celów rozgłaszania beacon ma minimum jeden wiersz by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7010
* fix(settings): ponowne ustalenie numeru węzła, gdy pierwsze ustawienie regionu zmienia numerację radia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7021
* fix(connections): ograniczenie ostrzeżeń o regionie do aktywnego połączenia by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/7015
* fix(mqtt): obniżenie poziomu logów dekodowania nieparsowalnych ładunków z error do warn by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7040
* fix(node): blokowanie wiadomości bezpośrednich bez posiadanego klucza publicznego by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7050
* fix(takserver): pomijanie skompresowanych pakietów TAK v1, aby zapobiec duplikatom kontaktów by @texaskst in https://github.com/meshtastic/Meshtastic-Android/pull/7020
* fix(firmware): przeniesienie pobierania artefaktów firmware na hosty R2 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7073
* fix(settings): pobieranie konfiguracji Mesh Beacon z węzłów zdalnej administracji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7077
* fix(messaging): stabilna sesja IME pola wiadomości podczas zmian na liście węzłów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7079
* fix(network): nigdy nie wysyłaj nonce heartbeat 1, czyli wyzwalacza pingu NodeInfo w firmware by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7078
* fix(lora): poprawne progi SNR presetów i kolory sygnału by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7110
* fix(settings): zezwolenie na wszystkie dostępne spread factory by @gargomoma in https://github.com/meshtastic/Meshtastic-Android/pull/7119
* fix(nfc): zapis na tagach, które nigdy nie były sformatowane w NDEF by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7123
* fix(node): koniec z przedstawianiem węzłów tylko z MQTT jako niesłyszanych w bieżącej sieci LoRa by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7138
* fix(node): przywrócenie selektora przedziału czasu jako segmentowanego wiersza by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7142
* fix(messaging): zachowanie klawisza Enter w polu wiadomości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7155
* fix(storeforward): deduplikacja powtórki routera według original_id by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7158
* fix(service): utrzymanie procesu przy życiu i w stanie czuwania podczas aktualizacji firmware i skanowania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7159
* fix(messaging): powiązanie konwersacji z tożsamością kanału, a nie z indeksem slotu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7162
* fix(ui): usunięcie okien pamięci podręcznej leniwych list, które powodują awarię w zakresie lookahead by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7185
* fix(settings): informacja o nieodwracalności lockdown w oknie włączania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7171
* fix(network): wskazanie bazowego adresu URL API na host produkcyjny by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7197
* fix(discovery): zapis dwell i jego nadrzędnej kontroli w jednej transakcji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7204
* fix(map): zachowanie 300 ms płynnego przejścia przy korektach kamery MapLibre by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7240
* fix(ble): zastąpienie przestarzałego preConflate przez bufferCapacity by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7236
* fix(model): uwzględnienie 0xAE w haszu kanału AEAD by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7239
* fix(model): dopasowanie presetu pierwszej konfiguracji dla USA do ekranu urządzenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7244
* fix(settings): nazywanie eksportów konfiguracji według długiej nazwy, a nie krótkiej by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7248
* fix(nodes): filtr bezpośrednich nie zwraca już węzłów MQTT by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7256
* fix(mqtt): przełącznik TLS pokazuje i ustawia zapisaną flagę dla publicznego brokera by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7255
* fix(connection): podawanie transportu w raportach o zawieszeniu handshake by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7276
* fix(mqtt): subskrypcja z opcjami dozwolonymi przez wynegocjowaną wersję protokołu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7273
* fix(analytics): raporty o awariach zawierają radio, które je wywołało, prawdziwą ramkę przyczyny i limit by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7274
* fix(settings): zapis czasu trwania powiadomienia zewnętrznego w milisekundach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7307
* fix(node): widoczne liczniki węzłów podczas wyszukiwania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7311
* fix(ble): nie uzbrajaj transportu BLE na sprzęcie bez Bluetooth by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7321
* fix(connections): odrzucanie adresów szeregowych bez USB host by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7327
* fix(service): nieaktualny zapisany adres nie nadpisuje już nowszego wyboru by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7329
* fix(service): działanie na pierwszym planie tylko dla adresu, z którym można się połączyć by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7330
* fix(messaging): czytelniejsze kontrolki filtra wiadomości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7324
* fix(notifications): adaptacyjna ikona dla dymków by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7336
* fix(position): brak współrzędnych w żądaniu pozycji, gdy ich nie mamy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7361
### 📝 Inne zmiany
* refactor(settings): edycja komunikatu statusu na ekranie użytkownika by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6951
* refactor(map): wycofanie obejść maps-utils, które po poprawkach w wersji 5.1 stały się zbędne by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7046
* refactor(prefs): filtry każdego ekranu w jednym obiekcie stanu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7120
* refactor(appfunctions): migracja do AppFunctionServiceEntryPoint by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7340

## Nowi współtwórcy
* @azchohfi wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6864
* @gargomoma wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/7119

## [2.8.1] - 2026-08-20

### 🏗️ Funkcje
* feat(settings): dodanie regionów LoRa 2.8 amatorskich oraz EU Lite/Narrow by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6548
* feat(takserver): udostępnianie węzłów mesh w ATAK jako kontaktów CoT (mesh-to-CoT) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6554
* feat: wyróżnienie zapisanych szacunków zasięgu w arkuszu warstw mapy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6578
* Ujednolicenie sformułowań statusu wiadomości w Androidzie by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6133
* feat(settings): eksport bazy węzłów do JSON by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6610
* feat(tak): wyświetlanie statusu lokalnego serwera by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6599
* feat(messaging): opcjonalne pełne znaczniki czasu wiadomości by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6639
* feat(settings): ostrzeżenie przed włączeniem trybu licencjonowanego (ham) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6632
* perf(startup): przeniesienie inicjalizacji WorkManager i AppFunctions poza główny wątek by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6648
* feat(analytics): raportowanie kluczowych interakcji użytkownika jako nazwanych akcji RUM by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6654
* feat(node): oznaczanie niekompletnych węzłów odznaką i domyślne ich wyświetlanie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6673
* refactor(maps): współdzielenie własnych dostawców kafelków by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6641
* feat(demo): tryb Demo dostępny i wypełniony danymi w wersjach release by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6691
* feat(lora): respektowanie zamiaru przypiętego presetu ogłaszanego dla UNSET by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6711
* feat(icons): dodanie ikon Material dla własnych SF Symbols i poprawka ikon telemetrii gleby by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6722
* feat(settings): dodanie ekranu About z karuzelą sprzętu i zmiana nazwy ekranu licencji na Acknowledgements by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6721
* feat(firmware): dodanie factory erase dla nRF52/RP2040 i aktualizacji bootloadera OTAFIX przez USB by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6526
* feat(node): oznaczenie jakości SNR w wierszu sygnału w szczegółach węzła by @beecho01 in https://github.com/meshtastic/Meshtastic-Android/pull/6703
* fix(database): podniesienie domyślnego limitu pamięci podręcznej i ostrzeżenie przed usunięciem danych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6742
* feat(tak): dodanie ustawienia kanału TAK Mesh dla ruchu wychodzącego TAK by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6770
### 🖥️ Desktop
* fix(desktop): koniec z SIGSEGV przy każdym powiadomieniu w Linuksie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6543
* fix(desktop): zwolnienie GError i libnotify przy zamykaniu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6544
* fix(desktop): przywrócenie skanowania i łączenia BLE w spakowanych wersjach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6558
* fix(service): koniec z blokującym getString na ścieżkach powiadomień dostępnych z Dispatchers.Default by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6668
* fix(messaging): rozróżnianie ID pakietów w zakresie nadawcy by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6624
* fix(connections): ukrycie wpisu powtórki demo, gdy brak jej zasobu z nagraniem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6692
* fix(lifecycle): wzmocnienie dopuszczania pakietów i własności transportu by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6716
### 🛠️ Poprawki
* fix(ui): prawdziwa semantyka obecności dla rx_snr od początku do końca by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6523
* fix(debug): przywrócenie adnotacji hex ID węzłów zepsutych przez migrację na Wire by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6532
* fix(nodes): wyciszenie alertów podczas początkowej synchronizacji bazy danych by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6538
* fix(ble): wstrzymanie odpytywania RSSI w tle by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6539
* fix(database): odzyskiwanie zablokowanych pul obserwatorów by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6540
* fix(docs): publikowanie tylko zrzutów ekranu, do których odwołuje się zsynchronizowana strona by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6557
* fix(icons): rozróżnienie ikon launchera debug google/fdroid by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6564
* fix(tak): ponowne kodowanie dołączonych certyfikatów .p12 starszymi algorytmami PKCS#12 dla zgodności z ATAK na Androidzie ≤ 9 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6569
* fix: zgodność mapy/kamery w Site Planner z iOS by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6577
* fix(service): odblokowanie potoku przychodzącego za zombie w stanie nieaktualnego Connected by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6587
* fix(map): ograniczenie śledzenia lokalizacji do widocznego cyklu życia by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6602
* fix(konsist): normalizacja ścieżek źródeł w Windows by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6604
* fix(map): ponowne użycie elementów klastrów między rekompozycjami kamery by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6605
* fix(fdroid): dostarczanie GeoPackage SQLite zgodnego z 16 KB by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6606
* fix(tak): bramkowanie pakietów V2 według znanego firmware by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6600
* fix(messaging): oczekiwanie na ACK routingu udostępnionego kontaktu by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6603
* fix(build): podpisywanie wersji debug wspólnym keystore zapisanym w repozytorium by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6615
* fix(ci): odblokowanie merge queue na Gradle 9.6.1 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6636
* fix(ui): dodanie kontekstu MQTT do etykiet przełączników uplink/downlink kanału by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6614
* fix(i18n): lokalizacja ciągów selektora emoji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6628
* fix(database): atomowe usuwanie węzłów i metadanych by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6623
* fix(ci): synchronizacja dystrybucji Gradle dla Flatpak by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6625
* fix(mqtt): izoluj nakładające się sesje klientów by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6616
* fix(data): zapobiegaj czyszczeniu tabeli MeshLog przez retencję logów "1 godzina" by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6635
* fix(node): wykresy metryk środowiskowych w jednostkach wybranych przez użytkownika by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6634
* fix(messaging): osieroceniowe wiadomości "Wysyłanie..." kończą się teraz błędem z możliwością ponowienia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6630
* fix(ui): selektor emoji nie ucina już tekstu przy dużych skalach czcionki by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6631
* fix(service): poczekaj na zapisane urządzenie przed ponownym połączeniem po uruchomieniu systemu by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6617
* fix(node): zatrzymaj aktualizacje kompasu w tle by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6620
* fix(service): wczytuj zapisaną politykę czyszczenia MeshLog by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6621
* fix(usb): waliduj wywołania zwrotne uprawnień by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6622
* fix(settings): odświeżaj opóźnione zdalne klucze publiczne by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6638
* fix(ui): formatowanie jednostek zależne od ustawień regionalnych i skalowanie czcionki w widoku wiadomości (audyt projektu) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6629
* fix(android): zachowuj udostępniany tekst w deep linkach by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6619
* fix(test): odizoluj testy jednostkowe androidApp od produkcyjnej klasy Application by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6644
* fix(settings): pokazuj REDACTED dla zatajonego klucza prywatnego zdalnego węzła by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6649
* fix(ui): pokazuj w ikonie połączenia na pasku nawigacji ponowne połączenia wymuszone przez watchdog by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6655
* fix(ble): włącz Kable preConflate, aby zapobiec ANR w wywołaniach zwrotnych skanowania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6652
* fix(ui): przywróć wysokość dolnej osi czasu przy dużych skalach czcionki by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6656
* fix(telemetry): skieruj temperaturę 1-Wire na pola dla poszczególnych kanałów, obsłuż napięcie ADC by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6653
* fix(analytics): obejmij lokalne wgrania firmware'u z pliku i raportuj message_send na pierwszym planie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6660
* fix(database): ogranicz czas wykonania withDb, aby jedno zawieszone wywołanie nie blokowało wszystkich zapisów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6661
* fix(konsist): zakotwicz reguły ścieżek w skanowanym katalogu głównym, aby strażnik commonMain nie przechodził pozornie w worktree agentów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6665
* fix(database): odzyskiwanie po cichym zawieszeniu puli połączeń Room (#6608) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6658
* fix(settings): przywracaj kanały z profili urządzeń by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6618
* fix(map): otwieraj Site Planner dla wybranego węzła by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6640
* fix(map): zezwalaj na warstwy z ogólnymi typami MIME by @ayysasha in https://github.com/meshtastic/Meshtastic-Android/pull/6663
* fix(ui): przenieś branding firmware'u wydarzeń z paska aplikacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6676
* fix(lora): waliduj opcje szerokości pasma 2,4 GHz by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6529
* fix(build): śledź rzeczywistą wersję compose-multiplatform we wymuszonym rozwiązywaniu zależności flatpak arm64 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6694
* fix(build): zadeklaruj jawnie navigationevent-compose, naprawiając kompilację metadanych Dokka by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6698
* fix(notifications): zapobiegaj powtarzającym się alertom o chronionej pozycji by @ayysasha in https://github.com/meshtastic/Meshtastic-Android/pull/6700
* fix(lora): zachowaj celowo przypięty preset przy świeżej konfiguracji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6710
* fix(admin): zachowaj odświeżanie sesji przy opóźnieniach wielu przeskoków by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6718
* fix(discovery): przywracaj stan radia po przerwanych skanowaniach by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6717
* fix(network): poprawnie obsługuj adresy IPv6 w mDNS, odświeżaj przy ponownym ogłoszeniu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6729
* fix(ui): żądaj korekcji błędów HIGH dla generowanych kodów QR by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6730
* fix(barcode): zawęź skaner F-Droid wyłącznie do kodów QR by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6731
* fix(ai): odczytuj rzeczywiste źródło inferencji zamiast wpisywać je na stałe by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6732
* fix(firmware): dodaj zastępczy painter do obrazu sprzętu urządzenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6733
* fix(map): przywróć natywny styl osmbonuspack, napraw lukę w skalowaniu kafelków by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6735
* fix(takserver): podłącz logger TAKPacket-SDK, pokazuj usunięcie uwag (remarks) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6736
* fix(node): potwierdzaj przed wyczyszczeniem śladu pozycji węzła by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6745
* fix(takserver): przekieruj autotest TAK przez rzeczywistą ścieżkę dyspozytora v1/v2 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6746
* fix(map): ogranicz klastrowanie Google Maps do widocznego obszaru powyżej 1000 węzłów by @beecho01 in https://github.com/meshtastic/Meshtastic-Android/pull/6739
* fix(node): wyszukiwanie na liście węzłów nie rozróżnia wielkości liter dla nazw spoza ASCII by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6753
* fix(ble): odświeżaj nieaktualną pamięć podręczną GATT podczas zwykłych ponownych połączeń by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6744
* fix(connections): otwieraj wybór regionu bezpośrednio z karty regionu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6743
* fix(tak): zduplikowane kontakty ATAK w v1 GeoChat + komunikat o przejściu na v1 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6759
* fix(connections): uzależnij ręczne i ostatnie połączenia TCP od ACCESS_LOCAL_NETWORK by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6766
* fix(service): wyjaśniaj zamiast zawieszać się, gdy ponownemu połączeniu TCP brakuje dostępu do sieci lokalnej by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6767
* fix(firmware): skieruj samoaktualizację bootloadera OTAFIX do własnego forka meshtastic by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6769
* fix(firmware): zmapuj T_ECHO_CARD (136) na SoftDevice 6.1.1 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6776
* fix: wyznaczaj jednostkę temperatury z preferencji temperatury w ustawieniach regionalnych, nie z systemu miar odległości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6775
* fix(ci): podbij dołączoną dystrybucję Gradle w verify-flatpak do 9.7.1, aby zgadzała się z wrapperem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6782
* fix(dfu): poprawnie wycofuj się, gdy Android ogranicza uruchamianie skanowania BLE by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6784
### 📝 Inne zmiany
* refactor(compose): zastosuj SideEffect z kluczem i usuń zbędne efekty zapisu zwrotnego konfiguracji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6684
* refactor(firmware): usuń duplikację pętli kopiowania bajtów przy pobieraniu w Android/JVM by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6737

## Nowi współtwórcy
* @clayburn wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6531
* @simulationstation wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6602
* @ayysasha wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6663
* @beecho01 wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6703

## [2.8.0] - 2026-08-01

### 🏗️ Funkcje
* feat(export): dodaj kolumny hop start i węzła przekazującego do eksportu CSV by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5822
* feat(mqtt): dodaj sterowanie lokalnym progiem odcięcia proxy MQTT w telefonie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5823
* feat(node): pokaż chip z krótką nazwą naszego węzła w zakładce Węzły by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5820
* feat(settings): dodaj zdalną akcję administracyjną "Ustaw czas" by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5821
* feat(network): transport odtwarzania przechwyconego ruchu na urządzeniu + fuzzing i utwardzanie przyjmowania danych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5846
* perf(node): dodaj stabilne klucze i contentType do list wykresów telemetrii by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5869
* feat(connections): wyświetlaj tylko urządzenia BLE widoczne podczas skanowania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5877
* feat(ui): użyj progów SNR względnych do presetu modemu przy ocenie jakości sygnału by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5903
* feat(firmware): dodaj odnośnik do bootloadera OTAFIX na ekranie powodzenia wolnego DFU by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5917
* feat(node): dodaj eksport GPX do ekranu dziennika pozycji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5919
* feat: metadane firmware'u wydarzeń działające offline (schemat JSON + dołączony zasób) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5920
* feat(firmware): steruj brandingiem firmware'u wydarzeń z dołączonych metadanych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5929
* feat(lora): obsłuż mapę zgodności region→preset oraz presety TINY (protobufs #951) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5834
* feat(lockdown): tryb blokady firmware'u (aprowizacja / odblokowanie / zablokuj teraz) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5939
* feat(lora): uzależnij mapę region→preset i presety TINY od możliwości firmware'u by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5941
* feat(security): pokaż podpisywanie pakietów XEdDSA w interfejsie węzłów i wiadomości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5976
* fix(security): zrób tarczę podpisu XEdDSA zieloną i wyraźną by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5980
* Zapobiegaj uruchamianiu Range Test na publicznym/domyślnym kanale by @dubsector in https://github.com/meshtastic/Meshtastic-Android/pull/5986
* feat(network): przenieś TcpTransport na ktor-network (commonMain) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5995
* feat(ui): czytelność StatusSurface zgodna z AA + dopracowanie podpisów/transportu w szczegółach węzła by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5985
* feat: zapis tagów NFC dla udostępnianych kontaktów i kanałów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6030
* feat: geofencing waypointów (edytor, nakładki mapy, silnik alertów)  by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6014
* feat(lora): ustaw domyślnie dla regionu US preset LongTurbo by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6009
* feat(connections): dodaj deep link uruchamiający połączenie według adresu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6036
* feat(desktop): dodaj zrzuty ekranu Flathub do metainfo.xml by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6042
* feat(testing): extra intentu skip_onboarding tylko do debugowania, dla narzędzi AI/CI by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6044
* feat(discovery): pokazuj odebrane zaproszenia Mesh Beacon by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6043
* feat(firmware): starszy BLE DFU dla nRF52 — poprawki dla standardowego bootloadera + odzyskiwanie utkniętych urządzeń by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6041
* feat(settings): podłącz is_unmessagable/is_licensed do eksportu/importu DeviceProfile by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6065
* Stylizuj nakładki GeoJSON według simplestyle-spec (wypełnienie/obrys) by @garthvh in https://github.com/meshtastic/Meshtastic-Android/pull/6088
* feat(discovery): klient Mesh Beacon z parytetem z iOS 014-mesh-beacons by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6097
* feat(messaging): tłumacz wiadomości czatu w miejscu za pomocą ML Kit na urządzeniu (tylko wariant google) by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/6103
* feat(database): ujednolić bazę danych urządzenia dla wszystkich transportów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6096
* feat: oblicz AQI EPA NowCast na podstawie historii PM2.5 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6102
* Dodaj bezpieczne tworzenie kopii zapasowej, przywracanie i usuwanie kluczy dla konfiguracji zabezpieczeń by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6105
* feat(messaging): @wzmianka z deep-linkiem do szczegółów węzła (#6098) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6108
* feat(node): opisz kanały zasilania i popraw skalę osi ciśnienia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6111
* Pokaż tekst statusu wiadomości wychodzącej by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6121
* feat(map): integracja zasięgu Site Planner — import i szacowanie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6136
* feat(node): pokaż temperaturę i wilgotność z czujnika CO₂ na stronie Jakość powietrza by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6143
* feat(node): histogram węzłów według odległości w skokach (#5745) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6146
* feat(map): zgodność warstw mapy z F-Droid — wspólny interfejs i logika warstw we wspólnym kodzie źródłowym (#6138) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6148
* Awansuj temperatury z CO2 do podsumowania, jeśli nie ma innej temperatury. by @DaneEvans in https://github.com/meshtastic/Meshtastic-Android/pull/6153
* feat(firmware): potwierdzaj lokalne pliki firmware przed wgraniem by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6083
* feat(messaging): podziel Rozmowy na zwijane sekcje Kanały/Wiadomości prywatne by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6169
* perf(mqtt): odrzucaj pozbawione ładunku pakiety downlink klienta proxy przed przekazaniem dalej by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/6171
* feat(map): wysyłaj punkt trasy jako wiadomość prywatną lub na wybrany kanał by @joeyleake in https://github.com/meshtastic/Meshtastic-Android/pull/6218
* feat(firmware): kanał podglądu nightly za odblokowaniem ukrytych funkcji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6225
* perf(r8): włącz optymalizację w buildach release (bez obfuskacji) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6275
* feat(analytics): przywróć śledzenie widoków RUM w Nav3 (wariant Google) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6281
* perf(analytics): dopasuj monitoring Datadog RUM do iOS, usuń nieużywaną zależność timber by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6278
* Dostosuj okna importu i udostępniania kontaktów przez NFC do Design Standards v1.4 by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/6332
* feat(settings): dodaj politykę autentyczności pakietów by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6178
* feat(messaging): przeprojektuj dymki wiadomości zgodnie z wzorcem rozmowy M3 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6368
* feat(notifications): dostosuj powiadomienia o wiadomościach do najlepszych praktyk Androida by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6371
* feat(node): grupuj powiązane karty metryk w pionowe kolumny by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6431
* feat(node): pokaż AQI na wykresie i w tabeli jakości powietrza by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6434
* feat(event): logo DEF CON 34 i pełna paleta marki w motywie wydarzenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6483
### 🖥️ Desktop
* fix(data): nieaktualne pamięci podręczne firmware/sprzętu — nie anuluj wolnych odświeżeń API, usuwaj wycofane wydania, zasilaj z nowszych paczek by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6060
* fix(geofence): ogranicz alerty o przekroczeniu do twórcy, dodaj zgodę per geofence by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6117
* fix(ble): odśwież pamięć podręczną GATT przy zmianach profilu ESP32 OTA by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6166
* Dodaj powiadomienie o aktualizacji firmware by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6309
* fix: spraw, by NotificationManager.dispatch czekał na wysłanie przez platformę by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6326
* fix(database): spraw, by aktualizacje bazy danych były atomowe przy przełączaniu urządzeń by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6256
* fix(ci): odłóż rozwiązywanie toolchainu pakowania desktop by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6401
* fix(settings): zachowaj sesję konfiguracji podczas nawigacji by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6449
* fix(event): respektuj wyłączone zdarzenia węzłów, ogranicz adresy URL marki, obserwuj manifest by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6499
### 🛠️ Poprawki
* fix(mqtt): spraw, by identyfikator klienta MQTT był unikalny dla każdego połączenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5755
* fix(ble): wzmocnij cykl życia połączenia BLE by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5795
* fix(build): odizoluj ML Kit GenAI do wariantu Google (napraw rb-check F-Droid) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5824
* fix(notifications): otwieraj szczegóły węzła po dotknięciu powiadomienia 'New Node Seen' by @LesterCheng in https://github.com/meshtastic/Meshtastic-Android/pull/5752
* fix(appfunctions): zachowaj konstruktory fabryk dokumentów AppSearch w trybie R8 full by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5829
* fix(service): rozwiąż wyścig przy uruchamianiu wybranego urządzenia by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5828
* fix(database): odłóż uzupełnianie FTS przy zimnym starcie i wymuś pulę z jednym połączeniem by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5841
* fix(ble): ponów połączenie, gdy parowanie zostanie przerwane by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5849
* fix(desktop): zakończ proces przy wyjściu; zamknij aplikację przy zamknięciu okna, gdy nie ma zasobnika by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5858
* fix(settings): awaria przy otwieraniu ekranu konfiguracji radia Position by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5862
* fix(settings): ogranicz konfigurację Traffic Management do firmware v2.8.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5864
* fix(network): ponawiaj przejściowe błędy połączenia/IO z api.meshtastic.org by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5870
* fix(ui): rozpoznawaj VPN i wszystkie sieci przy sprawdzaniu dostępności skanowania sieci by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5882
* fix(data): oddziel limity czasu odświeżania od zapisu w Room by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5881
* fix(service): odzyskuj zawieszone uzgadnianie WiFi/TCP przez przełączenie aktywnego transportu by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5856
* fix(ui): zapobiegaj duplikatom kluczy LazyColumn w logach metryk węzła by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5890
* fix(network): zachowaj backoff ponownego łączenia TCP przy krótkich sesjach by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5893
* fix(connections): skoordynuj cykl życia skanowania BLE i TCP by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5887
* fix(ui): pokazuj baner niedostępności Wi-Fi tylko podczas aktywnego skanowania sieci by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5892
* fix(network): przejdź na mqtt-client 0.4.0 (poprawka TLS dla adresów IP) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5895
* fix(ble): wymagaj świeżej reklamy przy automatycznym ponownym łączeniu by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5912
* fix(firmware): wzmocnij ścieżki aktualizacji ESP32 OTA i nRF DFU (zweryfikowane sprzętowo) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5915
* fix(firmware): seria porządków P3 dla OTA/DFU z audytu #5915 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5916
* fix(firmware): renderuj maskotkę chirpy przez painterResource w oknie aktualizacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5925
* fix(usb): dodaj odzyskiwanie obecności portu szeregowego po ponownym podłączeniu USB by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5923
* fix(data): zapisuj konfigurację modułu TAK by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5933
* fix(usb): wycisz oczekiwane ostrzeżenia o zamknięciu portu szeregowego by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5932
* refactor(connections): wyprowadzaj DeviceType z InterfaceId by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5935
* fix(usb): zgłaszaj odmowę uprawnień jako trwałe rozłączenie by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5943
* refactor(ble): spraw, by awaryjne połączenie Kable było jawnie ograniczone by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5944
* refactor(connections): pokazuj jeden panel aktywnego transportu by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5956
* fix(ble): przywróć ograniczone awaryjne ponowne łączenie ze sparowanym urządzeniem by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5960
* fix(docs): zachowaj #anchor przy przepisywaniu linków do sąsiednich stron dla Docusaurus by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5963
* fix(ble): ogranicz czas oczekiwania na parowanie w Androidzie by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5967
* fix(ble): unikaj zdublowanych ponowień parowania po nieudanym parowaniu by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5969
* fix(ble): zatrzymaj łączenie transportu po nieudanym parowaniu by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5973
* fix(ble): szybko kończ parowanie z błędem, gdy odpytany stan zwraca none by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5982
* fix(car): podłącz powiadomienia i alarmy, napraw awarię TabTemplate, przypnij car-app do wersji stabilnej by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5997
* fix(qr): serializuj zapisy importu kanałów by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5999
* fix(ui): zapobiegaj zawijaniu znacznika sygnału węzła; przywróć rozciąganie na pełną szerokość by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6007
* fix(car): wycisz INVISIBLE_MEMBER w CarScreensTest dla builda fdroid by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6010
* fix(docs): zapobiegaj modyfikowaniu śledzonych zrzutów ekranu dokumentacji przez buildy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6012
* fix(qr): zachowaj przychodzące kanały przy dodawaniu z kodu QR by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6013
* fix(car): wiadomości w samochodzie tylko przez powiadomienia w produkcji; szablonowe za flagą by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6015
* fix(firmware): napraw aktualizację firmware nRF przez USB i ponowne połączenie po aktualizacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6018
* fix(ble): obsłuż niepowodzenie rejestracji skanowania by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6019
* fix(discovery): pokaż powód wyłączenia pod przyciskiem Start Analysis by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6027
* fix(qr): ustabilizuj cykl życia skanera i importy by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6040
* fix(connections): oznacz przycisk karty łączenia jako "Stop Connecting" by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6046
* fix(messages): odświeżaj symbole zastępcze kanałów po aktualizacjach by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6054
* fix(qr): filtruj zduplikowane importy ADD by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6056
* fix(ci): zmień nazwę skip_author na ignore_usernames w .coderabbit.yaml by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6069
* fix(qr): niezawodnie stosuj zamiany kanałów by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6072
* fix(logs): zezwól na dostęp do logów DebugPanel przy braku połączenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6074
* fix: zabezpiecz przed wrogimi wynikami mesh-fuzz (awaria, GC-thrash, nieograniczony wzrost, OOM) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6093
* fix: usuń znacznik liczby satelitów z wiersza metryk listy węzłów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6100
* fix: wyprowadzaj jednostki interfejsu telefonu z ustawień regionalnych systemu, a nie z konfiguracji wyświetlacza radia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6101
* fix(ui): dopasuj podział AdaptiveTwoPane do progu adaptive directive by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6112
* fix(settings): stosuj ręczne zapisy kanałów w kolejności by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6077
* fix(settings): generuj świeży PSK dla nazwanych kanałów ręcznych by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6076
* fix(discovery): pozwól użytkownikom kanału domyślnego rozpocząć skanowanie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6120
* fix(discovery): wysyłaj świeże LoRaConfig przy przełączaniu na preset beacona by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6135
* fix(runtime): wzmocnij profil BLE, konfigurację OTA i dostęp do bazy danych by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6126
* fix(messaging): użyj zapisanego contact_key, by uniknąć awarii z powodu zduplikowanego klucza LazyColumn (#6131) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6142
* fix(metrics): legenda wykresu jakości powietrza podąża za wyświetlanymi danymi, a nie za wyborem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6145
* fix(firmware): Zachowanie dostępności wykrywania dołączonych wydań by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6082
* fix(dfu): Wykrywanie starszych bootloaderów przed bezpiecznym wariantem zapasowym by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6079
* fix(firmware): Czyste ponawianie połączeń z usługą OTA ESP32 by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6081
* fix(database): odzyskiwanie odczytów metadanych firmware po rotacji puli bazy danych by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6177
* fix: poprawki uwag z audytu wydania 2.8.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6189
* fix: wyciszenie węzła ma pierwszeństwo przed @wzmiankami by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6190
* fix: deduplikacja list węzłów, aby zapobiec awarii LazyColumn przez zduplikowane klucze by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6193
* fix(discovery): przywracanie domowej konfiguracji radia po przerwanym skanowaniu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6192
* fix(service): kierowanie szybkich odpowiedzi z powiadomień przez potok wysyłania; usunięcie nieużywanej wtyczki parcelize by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6196
* fix(firmware): nigdy nie oferuj wydania alpha starszego niż bieżące stabilne by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6198
* fix(settings): zachowanie dostępności Panelu debugowania podczas rozłączenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6199
* fix(database): wymuszenie puli z jednym połączeniem Room na wszystkich platformach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6202
* fix(dfu): wzmocnienie odzyskiwania Legacy nRF52 po zatrzymaniach BLE by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6201
* fix(dfu): klasyfikacja rozłączeń po nieudanych zapisach Legacy by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6209
* fix(database): migracja tożsamości urządzenia przy renumeracji w firmware 2.8 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6228
* fix(database): ponawianie ukończonych scaleń między transportami jest idempotentne by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6231
* fix(database): kierowanie wszystkich jednorazowych zapisów do bazy przez barierę opróżniania scaleń by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6233
* fix(database): wstrzykiwany DiscoveryDao podąża za aktywną bazą danych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6236
* fix(agents): datadog-rum-investigator blokowany przez prefiks montowania UUID w Datadog MCP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6241
* Fix: dodano nawigację wstecz na ekranie Czyszczenia bazy węzłów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6264
* fix: stabilizacja tożsamości węzłów na mapie traceroute i poprawa wyrównania metryk w logu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6270
* fix(connections): wyświetlanie długich nazw urządzeń w menedżerze połączeń (#5808) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6271
* fix: przyjęcie progów AQI PM2.5 EPA z 2024 roku dla spójności między platformami by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6273
* fix(analytics): przywrócenie śledzenia sieci Datadog RUM (google) i ujednolicenie nazwy usługi by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6280
* fix(connections): użycie przycisku segmentowego do wyboru transportu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6285
* fix: wyciszenie fałszywie pozytywnego ostrzeżenia lint Instantiatable dla ExtensionAppFunctionService by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6293
* fix: etykieta „Import/Export" na FAB udostępniania kanału i zapowiedź Zamknij po rozwinięciu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6303
* fix: normalizacja klatek kamery do skanowania QR w F-Droid by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6319
* fix: zachowanie trybu dodawania w starszych adresach URL kanałów by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6320
* fix: ujednolicenie kontraktu udostępniania kanału przez QR by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6325
* fix: dane z pamięci podręcznej nadal działają, gdy api.meshtastic.org jest niedostępne by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6335
* Fix: dosłowne ukośniki odwrotne w tekstach z ucieczką apostrofów by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/6336
* fix(map): możliwość edycji/usuwania własnego zablokowanego waypointu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6344
* fix(waypoint): odrzucanie modyfikacji zapisanego zablokowanego waypointu przez osobę niebędącą właścicielem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6348
* fix(nodes): odświeżanie odległości po zmianie jednostek wyświetlania by @CatSu-OSM in https://github.com/meshtastic/Meshtastic-Android/pull/6351
* fix(map): zachowanie położenia kamery przy przełączaniu kart by @CatSu-OSM in https://github.com/meshtastic/Meshtastic-Android/pull/6352
* fix(database): zmniejszenie obciążenia ścieżki zapisu przez jednorazowe odczyty by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6255
* fix(nav): rejestracja /wifi-provision i /discovery jako App Links https by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6365
* fix(node): koniec ze ściskaniem wartości końcowych kart metryk do jednego znaku w wierszu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6374
* fix(node-metrics): zastosowanie pełnego stylu typografii zamiast samego fontSize by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6375
* fix(node): uwzględnianie jednostek wyświetlania dla prędkości względem ziemi w pozycji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6376
* fix(node): uzgadnianie nieaktualnych powtórzeń tożsamości po renumeracji by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6259
* fix(ui): dodano poziomy margines dla elementu listy flag pozycji by @dzmpr in https://github.com/meshtastic/Meshtastic-Android/pull/6369
* fix(messaging): zmniejszenie rekompozycji edytora i zacięć podczas pisania by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6318
* fix(ble): wzmocnienie sesji ponownego łączenia i cyklu życia skanowania w Androidzie by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6384
* fix(settings): koniec z gubieniem odpowiedzi konfiguracji administratora (zawieszenie na 0%, brakujące zdalne kanały) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6391
* fix(hardware): ograniczenie częstotliwości ponownych odświeżeń katalogu przy chybieniach pamięci podręcznej by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6399
* fix(firmware): dekodowanie manifestów niezależnie od typu zawartości by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6400
* fix(firmware): nie traktuj nieparsowalnej wersji jako „zbyt starej" (#3726) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6408
* fix(channels): przechowywanie zestawu kanałów osobno dla każdego urządzenia, aby uniknąć duplikatów między urządzeniami (#4623) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6409
* fix(android): unikanie przetrzymywania aktywności w fabryce singletona Coil by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6403
* fix(messaging): użycie czasu mesh do znaczników czasu wiadomości i grupowania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6422
* fix(strings): poprawka angielskiej etykiety podsieci by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6425
* fix(android): unikanie zawieszeń zasobów w statusie połączenia by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6398
* fix(node): wyświetlanie liczby węzłów bez obcinania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6433
* fix(model): uzależnienie konfiguracji modułu TAK od firmware 2.8.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6435
* fix(data): raporty zgrubnej pozycji nie nadpisują dokładnych współrzędnych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6432
* fix(car): usunięcie CarAppService z produkcyjnych manifestów, aby spełnić wymagania przeglądu car-app w Play by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6438
* fix(map): konfiguracja user agenta OSMdroid przy starcie F-Droid by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6448
* fix(strings): usunięcie zbędnego „to" w opisie udostępniania lokalizacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6463
* fix(mqtt): ograniczenie zaufania do CA użytkownika do połączenia MQTT by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6464
* fix(logging): koniec z zgłaszaniem oczekiwanych sytuacji jako awarii by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6470
* fix(map): koniec z zgłaszaniem anulowanych ładowań warstw jako błędów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6468
* fix(network): ponowna synchronizacja ramkowania strumienia i koniec ze zgłaszaniem oczekiwanych rozłączeń jako błędów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6469
* fix(notifications): zabezpieczenie przed pustymi etykietami skrótów konwersacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6472
* fix(service): gwarancja osiągnięcia startForeground lub samoczynnego zatrzymania MeshService by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6471
* fix(node): ujednoznacznienie kluczy pakietów w logu sygnału by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6492
* fix(mqtt): logowanie błędów klienta bez throwable na poziomie warn by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6493
* fix(connections): deduplikacja kluczy list ostatnich TCP i reakcji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6494
* fix(map): lokalne usuwanie dowolnego waypointu, zablokowanego lub nie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6497
* fix(model): jawna obecność rssi dla protobufs 2.7.26.138 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6498
* fix(connections): wysyłanie set_time_only przy MyNodeInfo zamiast onNodeDbReady by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6503
* fix(ui): koniec z odrzucaniem zmierzonych zerowych odczytów czujników i RSSI by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6507
* fix(mqtt): koniec z zgłaszaniem awarii transportu jako odrzucenia poświadczeń by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6506
### 📝 Inne zmiany
* refactor(takserver): ujednolicenie potoku TAK SDK, usunięcie zbędnych zależności zstd/xpp3 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5871
* refactor(settings): usunięcie konfiguracji modułu Traffic Management by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5878
* refactor(firmware): deduplikacja transportu BLE/DFU OTA i powtarzalnego kodu obsługi by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5918
* refactor(data): konsolidacja ładowania dołączonych zasobów w BundledAssetReader by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5921
* refactor(core:ui): usunięcie zbędnego SinglePaneSceneStrategy z NavDisplay by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5934
* refactor: usunięcie dwóch przesadnie rozbudowanych szwów (enum + stdlib Base64) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5945
* refactor(ui): migracja dialogu MapView do Compose M3 i usunięcie starszej zależności material by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5988
* refactor(settings): zastąpienie SimpleDateFormat przez kotlinx-datetime by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5992
* refactor(car): usunięcie nieużywanego duplikatu FuzzyNodeNameResolver by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5994
* refactor(qr): atomowe stosowanie importu kanałów przez transakcję edit-settings by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6170
* refactor(di): usunięcie atrapy ApiService dla F-Droid, użycie prawdziwego klienta API w obu wariantach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6226
* refactor(map): przejście na standardowe klastrowanie maps-compose 8.4.0, usunięcie obejścia z własnym rendererem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6301
* refactor(map): migracja własnych nakładek do warstwy danych maps-utils 5.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6304
* refactor(ui): usunięcie przyciemnienia StatusSurface za chipami w kolorach statusu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6367
* perf(docs): pomijanie Dokka, gdy nie zmieniły się źródła API, usunięcie modułów testowych z agregacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6412
* fix(desktop): przywrócenie wczesnego przypięcia javaHome JBR dla pakowania ProGuard by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6414
* Porządkowanie menu konfiguracji by @pdxlocations in https://github.com/meshtastic/Meshtastic-Android/pull/6478
* chore(deps): aktualizacja org.meshtastic:mqtt-client do v0.8.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6508

## Nowi współtwórcy
* @LesterCheng wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/5752
* @dubsector wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/5986
* @garthvh wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6088
* @coderabbitai[bot] wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6180
* @madeofstown wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6210
* @joeyleake wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6218
* @sashko wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6315
* @CatSu-OSM wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6351
* @dzmpr wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6369
* @pdxlocations wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/6478

## [2.7.14] - 2026-06-03

### 🏗️ Funkcje
* refactor(ble): Scentralizowanie logiki BLE w module core by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4550
* feat(ble): Dodanie obsługi charakterystyki `FromRadioSync` by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4609
* feat(widget): Dodanie widżetu Glance ze statystykami lokalnymi by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4642
* chore(deps): aktualizacja zależności, by skorzystać z nowych funkcji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4658
* feat(maps): Ulepszenia Google Maps dla sieciowych i offline'owych źródeł kafelków by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4664
* feat: Ulepszona obsługa edge-to-edge i wycięć wyświetlacza by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4669
* feat: nadchodzące wsparcie dla konfiguracji tak i trafficmanagement oraz sprzętu urządzeń by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4671
* feat: przebudowa ustawień by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4678
* feat: przebudowa ustawień część 2, abstrakcja domeny i usecase, testy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4680
* feat: rozdzielenie usługi by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4685
* refactor: migracja :core:database do Room Kotlin Multiplatform by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4702
* refactor(ble): usprawnienie cyklu życia połączenia i zwiększenie niezawodności OTA by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4721
* refactor: migracja preferencji do DataStore i odseparowanie core:domain dla KMP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4731
* refactor: migracja modułów core do Kotlin Multiplatform i konsolidac… by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4735
* feat: Migracja projektu do architektury Kotlin Multiplatform (KMP) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4738
* refactor: migracja z Hilt do Koin i rozbudowa wspólnych modułów KMP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4746
* refactor: migracja core UI i funkcji do KMP, przejście na Navigation 3 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4750
* feat: wprowadzenie celu Desktop i rozbudowa architektury Kotlin Multiplatform (KMP) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4761
* build(desktop): włączenie ProGuard dla kompilacji release by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4772
* feat(desktop): implementacja automatycznego łączenia DI i walidacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4782
* feat(desktop): rozszerzenie obsługiwanych natywnych formatów dystrybucji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4783
* feat: Ukończenie wydzielania ViewModeli i aktualizacja dokumentacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4817
* refactor: Zastąpienie Nordic backendem Kable dla Desktop i Androida z obsługą BLE by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4818
* feat: Integracja zarządzania powiadomieniami i preferencji na wszystkich platformach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4819
* feat: wydzielenie usługi by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4828
* feat: logika budowania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4829
* feat: Transport USB serial dla Desktop by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4836
* Dodanie filtra "Wyklucz MQTT" do widoku węzłów. by @VictorioBerra in https://github.com/meshtastic/Meshtastic-Android/pull/4825
* feat: mqtt by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4841
* feat: Integracja Mokkery i Turbine z frameworkiem testowym KMP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4845
* feat: Ukończenie odchudzania modułu app i wydzielenia modułów funkcji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4844
* feat: Zwiększenie pokrycia testami  by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4847
* feat: Implementacja KMP ServiceDiscovery dla urządzeń TCP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4854
* feat: Dodanie obsługi URI, importu i generowania kodów QR w KMP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4856
* feat: Migracja panelu debugowania do KMP i aktualizacja dokumentacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4859
* feat: Migracja do Room 3.0 i aktualizacja powiązanej dokumentacji oraz tracków by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4865
* feat: Implementacja wsparcia dla iOS i ujednolicenie infrastruktury Compose Multiplatform by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4876
* Dodanie implementacji InlineMap dla kompilacji F-Droid by @theKorzh in https://github.com/meshtastic/Meshtastic-Android/pull/4877
* refactor(desktop): usunięcie natywnego MenuBar z głównego okna by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4888
* feat: Migracja warstwy sieciowej do Ktor i rozszerzenie wsparcia multiplatform by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4890
* refactor: adaptacyjne komponenty UI dla Navigation 3 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4891
* feat: Integracja AlertHost z aplikacją desktopową i dodanie testów UI by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4893
* feat: implementacja globalnego SnackbarManager i konsolidacja wspólnej konfiguracji UI by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4909
* feat: implementacja ujednoliconego routingu deep linków dla Kotlin Multiplatform by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4910
* refactor: transport BLE i UI na potrzeby ujednolicenia Kotlin Multiplatform by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4911
* Refaktoryzacja zarządzania warstwami mapy i infrastruktury nawigacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4921
* feat: migracja do API Material 3 Expressive by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4934
* Refaktoryzacja architektury nav3 i ulepszenie układów adaptacyjnych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4944
* feat(tak): wprowadzenie wbudowanego lokalnego serwera TAK i integracji z siecią mesh by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4951
* feat(analytics): rozbudowa integracji DataDog RUM i dopasowanie do parytetu z iOS by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4970
* feat(wifi): wprowadzenie provisioningu WiFi przez BLE dla urządzeń zgodnych z nymea by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4968
* feat(wifi-provision): dodanie brandingu mPWRD-OS i banera z zastrzeżeniem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4978
* feat(charts): przyjęcie najlepszych praktyk Vico, dodanie danych z czujników i migracja TracerouteLog by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5026
* refactor(icons): migracja do własnych plików XML VectorDrawable przez MeshtasticIcons by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5030
* feat(messaging): dodanie akcji Wyślij IME do pola wpisywania wiadomości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5047
* feat(metrics): przeprojektowanie dziennika pozycji z SelectableMetricCard i dodanie eksportu CSV do wszystkich ekranów metryk by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5062
* feat(core/ui): dodanie safeLaunch, UiState, uprawnień KMP i modernizacja cyklu życia CMP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5118
* feat(desktop): dodanie entitlements i podłączenie MeshConnectionManager do orkiestratora by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5127
* feat(environment): dodanie obsługi wyświetlania wielu termometrów 1-Wire (DS18B20) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5130
* feat: dodanie motywu wysokiego kontrastu z dostępnymi dymkami wiadomości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5135
* feat(mqtt): migracja do MQTTastic-Client-KMP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5165
* feat(mqtt): przyjęcie mqttastic-client-kmp 0.2.0 — powody rozłączenia + Test połączenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5181
* feat(firmware): obsługa nRF52 BLE Legacy DFU by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5209
* feat(service): wysyłanie grzecznego ToRadio(disconnect=true) przed zamknięciem transportu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5210
* feat(node): płynniejsza obsługa zdalnej administracji dzięki śledzeniu sesji dla każdego węzła by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5217
* fix(ble): odblokowanie ponownego łączenia + audyt kable (logowanie, priorytet, backoff, StateFlow) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5222
* feat: Ulepszenie stanu powodzenia provisioningu WiFi mPWRD-os i komponentów UI by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5225
* feat(messaging): dodanie punktów wejścia do ustawień filtra by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5229
* feat(messaging): wysyłanie wiadomości po naciśnięciu Enter by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5246
* feat(desktop): natywne powiadomienia systemowe przez libnotify/osascript/PowerShell by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5253
* feat(auto): włączenie powiadomień o wiadomościach w Android Auto by @riddlemd in https://github.com/meshtastic/Meshtastic-Android/pull/5265
* fix: aktualizacja metadanych katalogu emoji i usprawnienie synchronizacji selektora by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5292
* fix: aktualizacja ikony powiadomień by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5293
* feat(connections): sortowanie połączeń i ranking pustych kanałów w konwersacjach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5295
* fix(connections): poprawa niezawodności skanowania BLE i cyklu życia UI by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5329
* feat: easter egg firmware'u eventowego z ambientowym brandingiem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5354
* feat: dopasowanie motywu do Design Standards v1.3, usunięcie ustawienia kontrastu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5355
* feat(desktop): naprawa powiadomień na macOS, nowe ikony desktopowe by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5403
* Aktualizacja intencji powiadomień i formatu URI deep linków by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5408
* fix: doprecyzowanie dokładności pozycji jako ± promień  by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5428
* feat: integracja protokołu TAK v2 z kompresją zstd i pełną obsługą typów CoT by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/5434
* feat(flatpak): odtwarzanie standardowych nazw plików maven z lokalnej pamięci podręcznej Gradle by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5538
* fix: użycie jednorazowych powiadomień o niskim poziomie baterii by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5550
* feat: dopasowanie menu kontekstowego listy węzłów do kanonicznej kolejności 6 pozycji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5548
* feat: włączenie puli połączeń WAL dla równoległych odczytów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5372
* feat: przełączanie gęstości listy węzłów z kompaktowym układem i przełącznikami pól by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5444
* feat(ai): ulepszenie AI na urządzeniu Chirpy dzięki właściwym API, UX pobierania i strumieniowaniu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5579
* feat: zastąpienie tekstowego pola szerokości pasma LoRa ograniczoną listą rozwijaną by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5687
* feat: Zapisywanie niewysłanej wiadomości czatu jako wersji roboczej by @Copilot in https://github.com/meshtastic/Meshtastic-Android/pull/5686
### 🖥️ Desktop
* fix(desktop): zachowaj pakiet Vico, aby zapobiec błędom weryfikacji bytecode by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5424
### 🛠️ Poprawki
* fix(strings): zastąp formy liczby mnogiej by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4596
* fix: zastąp ciąg fdroid map_style_selection by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4598
* refactor(test): wprowadź MeshTestApplication dla niezawodnego testowania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4602
* fix: spotless by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4604
* feat(build): wdróż skanowanie kodów kreskowych zależne od flavoru oraz ulepszenia kompilacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4611
* fix(qr): dodaj kanały jako klucz bloku remember, aby naprawić wyścig przy dodawaniu kanału… by @nreisbeck in https://github.com/meshtastic/Meshtastic-Android/pull/4607
* chore(ble): dodaj reguły Proguard dla biblioteki Nordic BLE by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4618
* ci(release): użyj symboli wieloznacznych dla ścieżek APK w workflow wydania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4619
* chore(ci): użyj symbolu wieloznacznego dla ścieżek APK w workflow wydania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4622
* chore(ci): doprecyzuj filtrowanie zadań analityki i usprawnij debugowanie wydań by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4624
* Poprawka/splits by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4626
* Dostosuj konstruktor MapView w FDroid do wersji Google (Issue #4576) by @ujade in https://github.com/meshtastic/Meshtastic-Android/pull/4630
* refactor(analytics): zmniejsz zakres śledzenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4649
* fix(map): uprawnienia lokalizacji, widoczność przycisku i stuknięcia w breadcrumb by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4651
* fix(strings): popraw wielkość liter w słowie Ham by @alecperkins in https://github.com/meshtastic/Meshtastic-Android/pull/4620
* ci: rozdziel atestacje artefaktów Google i zapewnij przesyłanie do F-Droid by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4665
* fix: zastąp strings.xml zasobem app_name by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4666
* Wyłącz generate_release_notes w workflow wydania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4668
* fix: drobne poprawki interfejsu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4696
* refactor: uprość śledzenie traceroute i ujednolić logikę przycisku z czasem odnowienia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4699
* feat: dodaj „Oznacz wszystkie jako przeczytane” oraz wskaźniki liczby nieprzeczytanych wiadomości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4720
* fix(widget): zapewnij aktualizacje widżetu lokalnych statystyk by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4722
* refactor(ble): zwiększ domyślny limit czasu profilowania BLE by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4728
* refactor: ulepsz zabezpieczenie przed zawieszeniem handshake i rozszerz pokrycie na Etap 2 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4730
* build(ci): zoptymalizuj workflow wydania i zaktualizuj konfigurację Room by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4775
* Wyłącz ProGuard dla wydania desktop i dodaj ikonę aplikacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4776
* fix(ble): zaimplementuj skanowanie niesparowanych urządzeń w interfejsie typowych połączeń by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4779
* fix: napraw zacięcia animacji i zaktualizuj zależności dla stabilności by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4784
* build(desktop): dołącz moduł `java.net.http` do natywnej dystrybucji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4787
* build: usuń PKG z celów dystrybucji desktop by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4788
* build: zaktualizuj ikony aplikacji desktop, wersjonowanie i konfigurację pakowania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4789
* refactor(settings): usprawnij obsługę węzła docelowego w RadioConfigViewModel by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4790
* feat(desktop): dodaj wysyłanie klawiszem Enter w wiadomościach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4793
* feat: ulepsz nawigację po mapie i obsługę punktów trasy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4814
* build: napraw generowanie licencji i zadania budowania analityki by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4820
* fix: napraw awarie i problemy z filtrem debugowania w Metrics i MapView by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4824
* fix(map, settings): zezwól na puste ID i wprowadź limit czasu żądania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4851
* docs: ujednolić zarządzanie kanałami powiadomień i przenieść testy jednostkowe by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4867
* fix: wdróż logikę ponownego łączenia i ustabilizuj przebieg połączenia BLE by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4870
* fix: zaktualizuj funkcję wiadomości o klucze elementów kontaktów i limity MQTT by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4871
* fix: określ jetbrains w gradle-daemon-jvm.properties by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4872
* fix(settings): usuń zbędną opcję regex w DebugViewModel by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4881
* refactor(service): zaktualizuj formatowanie ciągów w powiadomieniu o lokalnych statystykach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4885
* refactor(messaging): napraw wyprowadzanie klucza kontaktu w ContactsViewModel by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4887
* feat: optymistycznie zapisuj lokalne konfiguracje i kanały by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4898
* refactor(di): określ katalog pamięci podręcznej dysku dla ImageLoader by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4899
* refactor: bezpieczeństwo null, aktualizacja bibliotek daty/czasu i migracja testów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4900
* refactor: usuń demoscenario i zwiększ stabilność połączenia BLE by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4914
* refactor(ui): usuń etykiety z elementów zestawu nawigacji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4924
* build: włącz flagę kompilatora `-Xjvm-default=all` by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4929
* fix(ci): zaktualizuj odwołanie do wyjścia APP_VERSION_NAME w workflow by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4935
* fix(strings): popraw opis klucza publicznego by @Klavionik in https://github.com/meshtastic/Meshtastic-Android/pull/4957
* feat: wdróż transfer plików XModem i zwiększ niezawodność połączenia BLE by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4959
* Przebuduj nawigację na trasę NodeDetail i napraw ustawienia radia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4960
* Przebuduj i ujednolić logikę aktualizacji firmware na wszystkich platformach by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4966
* fix: popraw routing wiadomości PKI i napraw wyścig przy migracji bazy danych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4996
* fix: wyznacz poprawny klucz publiczny węzła w sendSharedContact i favoriteNode by @Copilot in https://github.com/meshtastic/Meshtastic-Android/pull/5005
* fix: napraw błędy w podsystemach połączenia, PKI, administracji, przepływu pakietów i stabilności by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5011
* fix(tak): napraw częste rozłączenia klienta TAK by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5015
* fix(service): napraw awarię MeshService spowodowaną zachłanną inicjalizacją kanału powiadomień by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5034
* style: zaktualizuj wektory drawable ic_no_cell i ic_place by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5040
* fix(build): zapobiegaj usuwaniu zasobów wydania fdroid przez transformację zasobów DataDog by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5044
* fix(icons): zastąp pathData konturowe (FILL=0) wypełnionymi (FILL=1) z oryginalnych Material Symbols by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5056
* fix(charts): wynieś rememberVicoZoomState ponad warstwy vararg, aby zapobiec ClassCastException by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5060
* fix(ui): dodaj brakujące adnotacje @ParameterName w deklaracjach actual rememberReadTextFromUri by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5072
* fix(settings): ukryj konfigurację Status Message do firmware w wersji 2.8.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5070
* fix(transport): audyt Kable BLE oraz poprawki bezpieczeństwa wątków, MQTT i logowania we wszystkich warstwach transportu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5071
* fix(build): usuń Compose BOM, aby rozwiązać konflikt z compileSdk 37 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5088
* fix(connections): pokazuj nazwę urządzenia podczas łączenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5085
* fix(build): dodaj jawną zależność compose-multiplatform-animation by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5095
* fix(nav): przywróć działającą nawigację do mapy traceroute by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5104
* fix(build): przebuduj reguły R8 i uprość konwencje build-logic (DRY) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5109
* fix(proguard): wyłącz shrinking dla klas animacji Compose  by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5116
* fix(icons): audyt i korekta regresji migracji ikon z #5030 #5040 #5056 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5136
* fix: dostosuj handshake połączenia BLE do oczekiwań protokołu firmware by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5141
* fix(app): dodaj reguły keep R8 dla Compose animation/runtime/ui by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5146
* perf(messaging): grupuj wyszukiwanie węzłów i odpowiedzi podczas wczytywania wiadomości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5149
* fix(app): wyłącz optymalizację R8, aby naprawić zawieszanie animacji Compose by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5150
* fix(node): nie twórz ponownie CartesianChartModelProducer z Vico przy zmianie kanału by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5160
* refactor: użyj wstrzykiwanych ioDispatcher i ApplicationCoroutineScope by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5167
* fix: zredaguj sekrety proto w MeshLog i scentralizuj reguły keep dla Compose by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5166
* fix(ui): stabilne klucze LazyColumn, role semantyczne i opisy zawartości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5168
* fix(ui): dokończ role dostępności i etykiety akcji dla klikalnych powierzchni by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5170
* fix(widget): steruj aktualizacjami przez obserwator stanu z debouncingiem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5185
* fix(transport): popraw odporność ponownego łączenia i handshake dla BLE / TCP / USB by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5196
* fix(fdroid): zapobiegaj awarii NotImplementedError przy pobieraniu wydania firmware by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5197
* fix(compass): nie pozwól, by przybliżone pozycje z sieci nadpisywały pozycje GPS by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5200
* fix(canned-messages): włącz wielowierszową edycję tekstu dla długich list wiadomości by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5203
* fix(settings): przywróć działanie przycisków Import/Export w #4913 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5204
* refactor: wyeliminuj bibliotekę uprawnień Accompanist by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5211
* fix: napraw połączenie proxy MQTT i błędy testów sondujących by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5215
* fix(ble): zapewnij wykonanie czyszczenia GATT w NonCancellable przy anulowaniu by @jdogg172 in https://github.com/meshtastic/Meshtastic-Android/pull/5207
* fix(ble): wyścigi przy czyszczeniu wykryte podczas przeglądu #5207 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5221
* fix(ui): spraw, aby przyciski stopki rozwijały się w dół by @zt64 in https://github.com/meshtastic/Meshtastic-Android/pull/5226
* fix(desktop): wycisz ostrzeżenia ProGuard dla Vico ColorScale by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5232
* fix(desktop): napraw awarię wydania dzięki poprawnym regułom ProGuard by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5236
* fix(crashlytics): napraw problemy z awariami w becie 2.7.14 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5245
* fix: napraw najczęstsze problemy z Crashlytics w wydaniu beta 29320633 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5278
* fix: zachowywanie przełączania języka i poprawne mapowanie ustawień regionalnych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5287
* fix: zapewnienie, że snackbar uwzględnia bezpieczne marginesy rysowania nad modyfikatorami hosta by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5290
* fix(ui): ujednolicenie stanu aktywności przycisków Anuluj i Wyślij by @elagin in https://github.com/meshtastic/Meshtastic-Android/pull/5284
* fix(data): domyślne wyłączenie powiadomień o nowych węzłach dla firmware'u wydarzeń by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5323
* fix(network): naprawa pustego adresu MQTT i wymuszenie TLS na serwerze domyślnym by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5333
* fix(mqtt): wzmocnienie wymuszania TLS, dodanie zaufania do certyfikatów CA użytkownika i lepsza diagnostyka błędów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5365
* fix: ograniczanie przyszłych znaczników czasu lastHeard do bieżącego czasu podczas przyjmowania danych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5418
* revert: Aktualizacja ustawień ponawiania w gradle-wrapper.properties by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5430
* fix: aktualizacja zrzutów ekranu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5435
* fix(database): odporniejsza logika ponawiania withDb na różne komunikaty o zamknięciu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5474
* fix(settings): dodanie walidacji danych wejściowych dla PIN-u BLE, modemu LoRa i oświetlenia otoczenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5477
* fix(nav): numer węzła zdalnej administracji + konsolidacja i usprawnienia Nav3 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5478
* fix(database): aktualizacja adnotacji @Relation dla Room 3.0.0-alpha05 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5507
* fix: zapobieganie zawieszaniu się szczegółów węzła, gdy API sprzętu urządzenia jest nieosiągalne by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5514
* fix(settings): zdalna administracja zawsze pokazywała konfigurację lokalnego węzła by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5560
* fix: ukrywanie wskaźnika baterii, gdy poziom wynosi 0 (nigdy nie zgłoszony) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5595
* fix: spójna szerokość kolumn w kompaktowych elementach listy węzłów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5596
* fix(emoji): włączenie androidResources dla core:ui, aby spakować emoji-data.json by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5597
* fix(proto): usunięcie TakTalkMessage i TakTalkRoomData z generowania kodu Wire by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5624
* fix(database): ustabilizowanie niestabilnego testu DatabaseManagerWithDbRetryTest by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5635
* fix(ble): zatrzymywanie skanowania BLE w tle i obniżanie priorytetu połączenia by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5644
* fix: usunięcie wpisu Android Auto z manifestu powodującego odrzucenie w Sklepie Play by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5662
* fix(takserver): emitowanie punktu kontaktowego *:-1:stcp, aby kierowane TAK-Talk/GeoChat były trasowane przez sieć mesh by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/5661
* fix(takserver): odrzucanie CoT dostarczonych przez sieć mesh więcej niż raz by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/5667
* fix: naprawa najczęstszych awarii z Crashlytics w becie 2.7.14 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5672
* fix(flatpak): pobieranie metadanych desktop z katalogu pakietowania w repozytorium by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5673
* Zmiana nazwy aplikacji Desktop na 'Meshtastic Desktop' by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5677
* fix: naprawa najczęstszych awarii i błędów niekrytycznych z Crashlytics dla kompilacji 29320984 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5684
* fix: natychmiastowe wyświetlanie nakładki ładowania dla podekranów zdalnej konfiguracji by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5694
* fix(node): przywracanie właścicieli drzewa widoków przy usuwaniu mapy, aby wyskakujące okna listy węzłów nie były niewidoczne by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5699
* fix(firmware): pokazywanie stanu błędu po wyczerpaniu prób połączenia BLE OTA by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5700
* fix(map): zastąpienie MarkerComposable bitmapami renderowanymi na Canvas by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5702
* fix(map): usunięcie ręcznych obejść ViewTree lifecycle owner by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5704
* fix(map): ograniczenie zasięgu ViewTreeLifecycleOwner renderera klastrów do widoku hosta mapy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5708
* fix(map): inicjalizacja Maps SDK przed budowaniem deskryptorów bitmap znaczników by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5709
* fix(map): wyeliminowanie FATAL renderera klastrów i wzmocnienie ścieżek czarnej mapy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5715
* fix(map): powrót do idiomatycznej dla biblioteki inicjalizacji Maps SDK po stronie aplikacji, naprawa awarii mapy wbudowanej by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5719
* fix(map): renderowanie znaczników klastrów w odpowiednim zakresie, aby usunąć FATAL ClusterRenderer by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5723
* fix(map): zastosowanie wtyczki kompilatora kotlinx-serialization w androidApp by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5726
* fix(map): utrzymanie widoczności ikony kompasu podczas podążania za kierunkiem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5728
### 📝 Inne zmiany
* refactor(ui): zasoby Compose, warstwa domeny by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4628
* Dodanie ikon metody transportu dla każdej wiadomości w nowym formacie wiadomości by @Kealper in https://github.com/meshtastic/Meshtastic-Android/pull/4643
* build: warunkowe stosowanie zależności testów instrumentalnych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4698
* docs: podsumowanie postępów migracji KMP i decyzji architektonicznych by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4770
* ci(release): przekazywanie wersji aplikacji do kompilacji desktop przez zmienną środowiskową by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4774
* ai: Ustanowienie dokumentacji conductor i ram zarządzania by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4780
* fix: naprawa błędnego wywołania getChannelUrl() powodującego utratę flagi "add" i un… by @skobkin in https://github.com/meshtastic/Meshtastic-Android/pull/4809
* chore: Rozszerzenie raportowania pokrycia w CI i dodanie workflow dla gałęzi main by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4873
* build(desktop): włączenie minifikacji ProGuard i tree-shakingu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4904
* build: aktualizacja Compose Multiplatform i migracja zależności lifecycle by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4932
* chore: ujednolicenie zasobów i aktualizacja dokumentacji dla Navigation 3 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4961
* feat(settings): dodanie obsługi DNS i naprawa przełącznika protokołu UDP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5013
* fix: użycie etykiet z payloadu w pr_enforce_labels.yml, aby uniknąć ograniczania liczby żądań by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5018
* fix: zawężenie wyzwalacza labelera, aby ograniczyć limitowanie żądań, i poprawka literówki bugfix by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5020
* test(prefs): migracja testów DataStore z androidHostTest do commonTest by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5092
* fix(resources): dodanie resourcePrefix do modułów KMP i widgetów, zmiana nazw zasobów z prefiksem by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5111
* fix(charts): zastosowanie poprawek z audytu dobrych praktyk Vico 3.1.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5138
* refactor(di): przyjęcie @KoinApplication z API wtyczki kompilatora startKoin<T>() by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5152
* test: migracja MigrationTest do runTest i dodanie brakujących fake'ów repozytoriów by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5171
* refactor: skonsolidowanie formatowania metryk przez MetricFormatter by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5169
* chore(r8): usunięcie zbędnych reguł keep pokrytych przez reguły konsumenckie by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5172
* Revert "diag(r8): disable minify for release builds (animation-freeze diagnostic)" by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5176
* Poprawka akcji usuwania w szczegółach węzła, aby zachować przepływ potwierdzenia by @Copilot in https://github.com/meshtastic/Meshtastic-Android/pull/5192
* Zmiana domyślnego ContrastLevel z STANDARD na MEDIUM by @somenice in https://github.com/meshtastic/Meshtastic-Android/pull/5325
* Wydzielenie ustawień wyświetlania listy węzłów do osobnego ekranu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5580
* Aktualizacja takpacket-sdk do wersji 0.3.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5621
* repo: Dodanie plików SVG ikon Meshtastic Desktop by @vidplace7 in https://github.com/meshtastic/Meshtastic-Android/pull/5623
* Rozszerzenie obsługi TAKTALK o obsługę wiadomości i pokoi, aktualizacja SDK do v0.3.2 by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/5634
* Revert "feat: replace LoRa bandwidth text input with constrained dropdown" by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5691

## Nowi współtwórcy
* @nreisbeck wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/4607
* @ujade wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/4630
* @alecperkins wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/4620
* @skobkin wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/4809
* @VictorioBerra wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/4825
* @theKorzh wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/4877
* @Klavionik wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/4957
* @jdogg172 wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/5207
* @zt64 wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/5226
* @riddlemd wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/5265
* @elagin wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/5284
* @somenice wnieśli swój pierwszy wkład w https://github.com/meshtastic/Meshtastic-Android/pull/5325

---

# English

All notable changes to this project will be documented in this file.
The `[Unreleased]` section is refreshed by running the `Update Changelog` workflow by hand;
a production release stamps its own section here automatically.
See [GitHub Releases](https://github.com/meshtastic/Meshtastic-Android/releases) for the full history.
The "MT_SW" sections below are maintained by hand and list only this fork's own changes; the upstream sections are generated.

## MT_SW fork changes

The entries below describe only what this fork adds on top of upstream, from the very start (27 July 2026) to today, in chronological order. The `[Unreleased]` and `[x.y.z]` sections further down come from upstream. Feature details: [README.md](README.md).

### October 2026

**Mesh Link Planner (native coverage planner, 1–2 Oct)**
* A native planner replaces the external Site Planner, entirely inside the app (Android and desktop): two independent points A/B (node list, map, coordinates, own station), Mapterhorn terrain, ITM/Longley-Rice, Fresnel zone, profile chart, link budget, antenna bearings and angles, comparison with the last measurement, omnidirectional coverage, export to PDF/CSV/KML/GeoJSON/PNG, "i" info dialogs, credits and licences.
* Cable and connector wizard (datasheet-based database, connectors without a cable, e.g. SMA→N adapters), every band from 169 MHz to 2.4 GHz plus any frequency; picking a band sets the frequency and channel width (2.4 GHz as wide LoRa, first slot for 433 and 470 MHz).
* Default Narrow Fast preset and 869.44165 MHz; presets without manual BW and SF; changing the preset sets its default frequency.
* Coverage on the map as a dense raster with a smooth colour scale (MeshMap Planner style), an opacity slider, a layer list on top with scrolling, a layer counter on the icon, a limit of 9 layers; coverage layers are not remembered after a restart. Named "Mesh Link Planer (by MT_SW)".
* Optional live weather (Open-Meteo): k-factor, refractivity, ducting.
* Obstacles: clutter presets (default) or the optional **detailed terrain from OpenStreetMap** (buildings and forests via the Overpass API, along the path and in coverage within the selected radius of the centre), with a warning about higher resource use and longer calculation; falls back to the preset if the download fails.
* Terrain data range menu (5 / 10 / 15 / 30 / 50 / 100 km, 30 km by default); the OpenStreetMap download is read in chunks with a size limit and out-of-memory is caught, so a dense area no longer crashes the app (an emulator with little memory included) but shows "too much data, reduce the range" and falls back to the obstacles preset, also during the coverage calculation.
* Fix for "out of memory" with detailed terrain: the Overpass server memory cap written into the query (wrongly lowered to 16 MB earlier) is back to a safe value, so the server no longer refuses even a small area; a server refusal ("out of memory", overload, HTTP 429/504) now has its own message instead of "the device ran out of memory", with the short reason given by the server; on a refusal the planner tries backup Overpass servers.
* Terrain data range above 10 km (50 and 100 km): the area is downloaded in pieces (tiles of about 30 km, one request at a time) instead of one huge answer that neither the server nor the device could handle; a tile the server finds too heavy is split into four, a busy server is waited for, elements on tile borders are counted once, outline points are thinned (from 25 m, more the larger the range, and tiny woods below that scale skipped) to save memory, and the last working server is remembered. The terrain query uses exact matches (`landuse=forest`, `residential` etc.) instead of a regular expression, so the server no longer reads every field and meadow in a tile and answers much faster. The whole download has a 12-minute limit and ends either complete or with an error (no half-maps). The progress (piece x of y) is shown under the coverage bar, and a long download no longer blocks the ordinary A–B link calculation. Overpass requests have their own limits (90 s wait for data instead of 30 s and no automatic repeats, which re-sent heavy queries); a timeout now means "tile too heavy" and splits it, and a brief network drop is retried. The obstacle query for a link path is split into lighter parts (woods, then the buildings at each end) with a 60 s server limit and one retry, because a single heavy query for a long link ran into the server time limit; the smallest coverage tile that timed out is retried after a short wait.
* The note under the detailed terrain settings now shows the selected data range instead of a fixed “30 km”.
* Traceroute dialog: when the route cannot be shown on the map there is a single OK button (before, a second one appeared next to it); traceroute labels on the map have a thinner outline and a larger font.

**Sniffer, keys, coding rate (Oct 8)**
* Sniffer: the state is read again after every reconnect — the app no longer shows an old answer from the previous connection and asks the radio several times until it answers.
* Desktop: the Security settings now have "Back up keys", "Restore keys" and "Delete key backup" buttons, the same as the phone app.
* Coding rate: the Narrow (Fast/Slow) and Tiny Slow presets, which default to 4/6, now also offer 4/5.
* GitHub builds (Actions → "Build Binaries (Manual)"): every system and format is picked separately — Windows (.msi, .exe, .jar), Linux (.deb, .rpm, .AppImage, .jar), macOS (.dmg, .jar) and Android (APK).

**Map**
* Smooth panning and zooming with many nodes: the "last heard"/"online" fields removed from map data, nodes refreshed only when position, name, favorite or ignored state change, pulse on its own small source in 15 steps, a stable view area while panning, cached chip images, no invisible outlines in the coverage layer.
* **Traceroute on the map with signal strength:** the "View on map" button only when every node on the route has a position (otherwise the dialog explains why); one thin line per hop in the colour of its signal quality, with an arrowhead at its end and the SNR in dB along the line, stopping just short of the node (nodes as dots under their chips), both directions symmetrically side by side; quality legend; a freshly received traceroute now has its SNR values straight away (previously the lines were grey until opened from the saved traces).

**Network**
* Recently used network (WiFi/TCP) devices: 20 entries instead of 3.

**Descriptions and naming (4 Oct)**
* **GPIO control guide behind an info icon:** the GPIO card header on the node detail screen now has an info icon that opens a short step-by-step guide — the Remote Hardware module and available pins on the target node, a "gpio" channel at position 1 with the same key on both devices, and what the 1 / 0 / Read buttons do.
* **One name, "On-Demand Diagnostics",** across the app (sniffer, packet port name, descriptions) instead of "OnDemand".

**Nodes, database cleanup and the upstream merge (3 Oct)**
* **Node cleanup over Bluetooth now completes:** the "remove node" commands to the radio are sent one by one with a short gap and retried when the link briefly refuses them (Bluetooth accepts only a few writes at a time, so with hundreds of nodes most commands used to be silently dropped and the radio handed those nodes back after reconnecting). Sending runs to the end even if you leave the screen; when the radio is not connected only the app is cleaned. It already worked over WiFi.
* **Clean all unknown nodes regardless of date:** with "Clean up only unknown nodes" on, a new switch "Ignore the date: all unknown nodes without a key" appears (nodes that never introduced themselves and that we hold no key for; favorites and ignored nodes are always kept).
* **Clean nodes with a mismatched key:** a new switch "Only nodes with a mismatched key (removed from the app and the radio)" on the cleanup screen — the list shows every node in the "key mismatch" state regardless of age (favorites, ignored nodes and our own node are kept), and cleaning removes them from the app database and from the radio.
* **Clear only the app's own node database** (no contact with the radio, works while disconnected): a button at the bottom of the cleanup screen with a confirmation and a "Keep favorite nodes" switch; turn it off to empty the list completely. A connected radio sends its nodes again on the next sync.
* **Smoother fast-scroll strip (desktop):** it no longer recomposes the whole screen on every scroll frame, the thumb glides with the list (instead of stepping item by item), and dragging moves the list continuously in pixels.
* **Upstream merge (meshtastic/Meshtastic-Android):** protobufs 2.8.1 and new dependencies, new upstream features (including the remote shell, reaction notifications, paged logs, `SendMessageOutcome`), new strings translated to Polish; our translations, branding, planner and scroll strip were kept. Time and size formatting moved to the new upstream helpers.

**Documentation**
* README and CHANGELOG completed with the whole history of the fork.

### September 2026

**Signal quality, LNA, logs (29–30 Sep, 1 Oct)**
* LNA gain correction for noise floor and RSSI (field in LoRa under the radio power and in the "Signal quality" dialog, a separate field for foreign nodes in node details, charts, Network Health, average noise in the summary).
* SNR rating relative to the preset (Narrow −3/−7/−12 dB, Lite −5/−10/−15 dB), RSSI can only lower the rating; colours as in the connection statuses (good gold, sufficient red, weak purple, none white/black depending on the theme); description with preset-dependent thresholds; SNR colouring in neighbor packet details and in traceroute.
* Message status colours (delivered, acknowledged, error, signature shield in gold).
* Device logs in the debug panel (live, search, export; Wi-Fi and USB only with MT_SW firmware), the sender's frame time in the sniffer, a "Force stop app" button.
* Desktop: fixed empty dropdown lists in the packaged build (ProGuard and enum values).

**Sniffer and OnDemand (5–26 Sep)**
* On-demand diagnostics (OnDemand, port 354): 10 query types; fixed requests to the locally connected node being dropped.
* Radio and MQTT Sniffer on one screen with a source selector, confirmation before enabling, showing broadcast traffic and own packets, decoding on click, copying, auto-scroll with a live/pause button and a new-packet counter, short node names, second-precision timestamps.
* Sniffer moved to the official protobufs over OnDemand (port 354); support detected by firmware version (threshold 2) and by the advertised module.
* Duplicate grouping with a list of receipts, saving and loading the log (txt/JSON/CSV), a 5000-packet buffer with a counter and a configurable overflow policy, redesigned packet cards (categories and colours), a channel-0 filter, telemetry summaries (uptime, current, channel and air utilisation, host CPU and memory, power metrics, local stats).
* Fixes: undecryptable packets reach the log, channel choice by hash, the "Unknown" category, content vanishing after grouping, traceroute return path (SNR), double entries for own traceroute, sniffer state after reconnecting and on re-entering the screen, MQTT on desktop, a 15 s MQTT connect timeout.

**Messaging**
* Long messages are split into parts and reassembled live on receipt (contact list preview, direction-aware sending and receiving status) (24–25 Sep).
* Photos by link, saving a photo from the preview, desktop: Enter = new line, Ctrl+Enter = send; default Quick Chat templates seeded per database.

**Security and nodes**
* Node colour picker when generating a key (local X25519 key search) (2 Sep).
* Automatic node-database cleanup (Android: WorkManager, desktop: hourly loop) (2 Sep).
* Localized device roles and a "Roles" help section; new "Security" and "Connection" sections in the node list help (13 Sep).
* Unified node security indicator colours (gold = trust, red = key mismatch).

**Map and desktop**
* OpenStreetMap as the default map, smaller chips with a tail, spreading of nodes at an identical position, tuned clustering, sharper raster tiles, the map always framed on all nodes, fixed flickering count in the cluster, a smaller mini-map zoom in node details, fixed crashing time-range selector.
* Desktop: the third column follows the selected node, a quick-scroll bar, fixed map in the packaged MSI (ProGuard/LWJGL), macOS ARM64 libraries, Bluetooth discovery on Windows.
* Fixed Bluetooth reconnection to an already-paired device (Xiaomi/MIUI) (8 Sep). Unfrozen Advanced settings panel while toggling the Sniffer (7 Sep).
* `local_stats_extended` read as a native telemetry field after the protobufs change (7 Sep).

**Maintenance**
* Upstream syncs (1 Sep, 13 Sep, 20 Sep) together with protobufs updates (2.8.0.x); missing PL translations completed (including 320 schema entries); formatting and static-analysis fixes.

### August 2026

* **Network Health** (1–2 Aug): a new tab with power, signal, airtime, environment, traffic and neighbor metrics; a summary with rankings; data decoded live from the mesh log. A **Resources** tab (CPU/memory/flash/PSRAM) and a "Local Stats" widget followed in August.
* **Relay node name** in the node list and node details, and the full list of relays in the delivery status (until 10 Aug, 15 Aug).
* MT-SW firmware edition detection (5 Aug), own `applicationId` so the app installs next to the original (2 Aug).
* `/ping` `/hello` `/test` quick command buttons (3 Aug), default Quick Chat templates (24 Aug), uptime with an icon (2 and 10 Aug).
* Hidden obsolete roles (REPEATER, ROUTER_CLIENT), fixed position-track map starting in the ocean, config import/export on desktop, a custom desktop map (later replaced by MapLibre).
* **Rebranding (27–29 Aug):** MT_SW_APP, custom icon (province outline and "MT_SW"), gold/navy palette, an "About" screen with the unofficial-fork disclaimer, desktop app name, tray icon.
* Upstream syncs (5 Aug, 10 Aug, 15 Aug, 16 Aug, 29 Aug); switch to the shared MapLibre map engine (Android and desktop), hillshade and weather radar overlays on by default.

### July 2026 — start of the fork

* Restored the Traffic Management screen (27 Jul).
* An always visible list of paired Bluetooth devices; Remote Hardware visibility; remote favorite/ignore over the LoRa mesh with routing-ACK confirmation (27 Jul).
* Manual contact add by ID (local and remote, `!a1b2c3d4` format) and remote GPIO control (29 Jul).
* Narrow-band and out-of-region (Świętokrzyskie) warnings; passive NeighborInfo with fixes (30 Jul).
* Image preview from links in chat (off by default); a full PL translation file (about 1000 strings) (30 Jul).

<!-- UNRELEASED_START -->
## [Unreleased]

### Internal (v2.8.3-internal.7)
Changes since [`v2.8.2`](https://github.com/meshtastic/Meshtastic-Android/releases/tag/v2.8.2):

#### 🏗️ Features
* feat(network): a hidden showcase scenario for Demo Mode by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7377
* feat(app): shell-only debug launch switches for onboarding and the trust dialog by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7375
* feat(settings): raise the coding rate over a modem preset by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7405
* feat(firmware): show installed vs latest bootloader before an upgrade by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7409
* feat(auto): notification messaging on Android Auto by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7417
* feat(map-maplibre): start with collapsed attribution strip on seconda… by @Tha14 in https://github.com/meshtastic/Meshtastic-Android/pull/7424
* perf(store-screenshots): wait for the map to draw instead of a fixed 45 seconds by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7501
* feat(desktop): add draggable scrollbars to node and message lists by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7506
* feat(notifications): post reactions on their own channel by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7507
* feat(admin): add Reboot into DFU mode admin action for nRF52 nodes by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7504
* feat(remote-shell): mesh terminal for the firmware DMShell module by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6862
#### 🖥️ Desktop
* fix(mqtt): remove noop mqtt to allow the mqtt proxy to work on the desktop builds by @Tha14 in https://github.com/meshtastic/Meshtastic-Android/pull/7400
* fix(notifications): put every notification on its own channel and tap target by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7415
* fix(database): delete old mesh logs in bounded batches by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7423
* fix: keep WiFi credentials, addresses and coordinates out of app logs by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7419
* fix: data correctness fixes from the Android audit by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7429
* fix(node): show the real traceroute map on desktop by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7425
* refactor: keep one copy of shared code across platforms by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7432
* fix(ui): localize UI strings, fix stale effect captures, one EmptyState by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7430
#### 🛠️ Fixes
* fix(map): keep the node track map responsive on long tracks by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7373
* fix(konsist): anchor the scanned-source inputs at the source roots by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7382
* fix(map): frame the mesh clear of the map's own controls by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7385
* fix(node): drop the filter bar's own background inside the list header by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7386
* fix(appfunctions): index functions where AppSearch has no dynamic schema by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7402
* fix(ui): split the link colour per mode by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7406
* fix(settings): stop profile import dropping Mesh Beacon settings by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7416
* fix(database): never publish a replacement pool while the write lock is held by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7418
* fix(prefs): recover corrupt Android prefs files and make toggles atomic by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7420
* fix(navigation): keep each tab's state across tab switches by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7421
* fix(discovery): keep an unheard node's SNR distinct from 0 dB by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7422
* fix(service): correct phone position units and omit missing readings by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7437
* fix(wifi-provision): release the BLE peripheral on retry and on leaving by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7453
* fix(map-maplibre): clear warnings and collect state with lifecycle by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7460
* fix(takserver): log a route export that failed to write by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7459
* fix(node): leave positions without a fix out of the GPX track by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7457
* fix(service): stop the inbound pipeline waiting on node writes by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7464
* fix(app): check a shared map file before importing it by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7463
* fix(ui): show byte sizes in decimal units, formatted for the locale by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7469
* fix(app): stop cleanly on devices the bundled SQLite can't run on by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7470
* fix(service): keep the last-heard write off the inbound lock by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7474
* fix(takserver): keep one route file across reinstalls by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7475
* fix(ui): show transfer rates and file limits in decimal units by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7476
* fix(ble): keep device addresses out of logs by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7478
* fix(ble): export the bond wait receiver so bond broadcasts reach it by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7484
* fix(firmware): show the percent while a maintenance UF2 downloads by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7485
* fix(messaging): let a pinned conversation be unpinned by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7492
* fix(app): restore the Apache HTTP legacy library for Google Maps by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7499
* fix(metrics): break power chart lines across gaps in readings by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7505
* fix(analytics): drop the package from RUM view names by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7525
* fix(node): keep verified contacts verified by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7531
* fix(settings): stop calling Balanced packet authenticity recommended by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7535
#### 📝 Other Changes
* refactor(data): page the log export, remove dedupe leftovers by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7449
* refactor(map): share the Web Mercator projection with node clustering by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7483
* refactor(ui): share the remote shell's keyboard sink from core/ui by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7526

## New Contributors
* @Tha14 made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/7400
<!-- UNRELEASED_END -->

<!-- RELEASED_START -->

## [2.8.2] - 2026-09-26

### 🏗️ Features
* perf(ui): render QR codes at display density instead of fixed 960px by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6793
* feat: report Android 17 memory-limiter kills via ApplicationExitInfo by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6792
* feat(privacy): shield sensitive UI content from non-tool accessibility services by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6794
* feat(hardware): fetch bootloader OTA quirks from the API, seeded from the bundled asset by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6802
* feat(hardware): fetch the maintenance UF2 manifest from the API, digest-pinned by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6803
* feat(messaging): swipe to reply, double-tap reactions, day separators by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6850
* feat(messaging): per-contact drafts and a quieter composer by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6851
* feat(messaging): name the sender on the jump-to-latest control by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6856
* feat(notifications): offer conversations as bubbles by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6858
* feat(messaging): mark unread, swipe row actions, and pinned conversations by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6857
* feat(messaging): close the quick reaction bar on a tap outside it by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6865
* feat(settings): send the optional ham long_name alongside the call sign by @vidplace7 in https://github.com/meshtastic/Meshtastic-Android/pull/6875
* feat(appfunctions): reconcile system state instead of blind-writing it by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6908
* feat(units): resolve units from the device region, add a Units setting, render through ICU by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6916
* feat(connections): notice when transmit is disabled by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6945
* feat(agents): add the run-meshtastic-android skill with desktop and emulator drivers by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6955
* feat(map): give every map layer its own opacity slider by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6958
* feat(map): filter the map by node role and by how a node was heard by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6959
* feat(settings): align the Mesh Beacon config editor with design#140 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6963
* feat(discovery): suppress beacon invitations for channels the radio already has by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6964
* feat(network): configure HTTP client engines and add platform User-Agent headers by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7008
* feat(ui): blend noise floor into signal quality rating (design#15) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7009
* feat(map): offline banner and Google basemap auto-fallback on connectivity loss by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6998
* feat(node): add an update-status shortcut to the local node menu by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7047
* feat: mark nodes not heard since the LoRa config changed by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7055
* chore: device link disclosure by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7068
* feat(node): overflow the time frame selector instead of crushing it by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7097
* perf: cache-window the hot lists, and modernize the nav3 and adaptive wiring by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7093
* feat(docs): use the real M3 search bar for doc search by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7098
* feat(nfc): share a contact or channel by tapping phones, no tag needed by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7124
* feat(ui): share the link from the share dialog, and say what the dialog does by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7126
* feat(node): signed and verified indicators in place of the PKI lock by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7117
* fix(node): keep a contact's public key when a different one arrives by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7118
* feat(settings): show the license notice on the About screen by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7191
* feat(map): clamp zoom when framing bounds, and let style images use hardware bitmaps by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7245
* refactor(navigation): route deep links through navigation3 UriDeepLinkMatcher by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7246
* refactor(settings): read numeric field bounds from the protobufs field registry by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7242
* feat(telemetry): surface lightning, PM status and soil-water metrics by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7237
* feat(node): rank maker hardware between supported and community in the device badge by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7263
* feat(settings): gate config fields on the schema's firmware versions by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7262
* feat(settings): hide module settings the node reports compiled out by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7272
* feat(mqtt): surface a refused subscription from the broker by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7290
* feat(settings): drive the module gates from the schema by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7296
* feat(settings): label enum pickers from the schema by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7298
* feat(settings): read the labels and helper text the schema already has by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7302
* feat(beacon): honour the advertised frequency slot by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7305
* feat(settings): emit the enum key prefixes the schema strings use by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7306
* feat(search): one M3 search bar, settings search, and node status in search by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7303
* feat(settings): show the unit the schema declares by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7304
* feat(connections): handle hardware without Bluetooth for Android XR by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7318
* feat(connections): handle hardware without USB host for Android XR by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7322
* refactor(connections): give Demo Mode its own section by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7326
* feat(messaging): show whether a delivery receipt was proven by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7284
* feat(messaging): record signing and ack proof on reactions by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7339
### 🖥️ Desktop
* fix(desktop): disable macOS notifications when the process has no app bundle by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6876
* fix(desktop): test the bundle path, not the identifier, before notifying by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6885
* fix(desktop): use standard SPDX license identifier for RPM packaging by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/7043
* fix(desktop): pin the Flathub screenshots to a commit that survives the squash by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7243
* fix(desktop): keep MapLibre's FFI upcall methods through ProGuard by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7287
* refactor(model): parse the selected device address once by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7328
### 🛠️ Fixes
* fix(navigation): clear deep-link replay cache once applied to the backstack by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6796
* fix(ui): give feedback when a contact or channel import arrives while disconnected by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6798
* fix(database): rebuild the packet FTS index after the schema-52 table recreation by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6808
* fix(map): raise android-maps-utils to 5.1.1 so KML import survives xmlutil 1.0.x by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6811
* fix(database): give every SQLite connection a busy timeout by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6809
* fix(firmware): show the erase wait and upload retries during Legacy DFU by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6812
* fix(map): parse KMZ archives in the F-Droid map overlay renderer by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6834
* fix(firmware): map SoftDevice variants for the new nrf52840 boards by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6843
* fix(navigation): never let the active backstack empty under NavDisplay by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6844
* fix(metrics): eliminate the Vico canvas restore underflow crash by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6847
* fix(ui): bound pane content height under the adaptive three-pane scaffold by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6845
* fix(connection): extend BLE handshake deadlines while config progress flows by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6848
* fix(map): survive the WebView provider update race in Site Planner by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6846
* fix(mqtt): tolerate object-typed payload in MQTT JSON messages by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6839
* fix(messaging): clear and suppress notifications for the conversation on screen by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6849
* fix(messaging): fire swipe row actions once per swipe, and let the snackbar go by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6863
* Stabilize Mid-Session Locale Unit State Test by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6870
* Demote Repeated Offline Heartbeat Rejection Logs to Debug by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6872
* Use Scan-Only Probes After Prolonged Bonded BLE Reconnect Failures by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6871
* fix(permissions): recover from a skipped, denied, or revoked permission instead of dead-ending by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6880
* fix(node): convert temperature before labelling it °F, and show wind in km/h by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6925
* fix(settings): drop the beacon single-target scalars protobufs reserved by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6942
* fix(node): scroll traceroute/log history detail popups by @beecho01 in https://github.com/meshtastic/Meshtastic-Android/pull/6708
* fix(ui): show device roles by name, and stop doubling two percent signs by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6962
* fix(map): gate the MapLibre waypoint editor on isModifiableBy by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6969
* fix(ai): bound assistant messages at what the send path will actually encode by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6970
* fix(firmware): hide USB maintenance where the platform cannot run it by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6974
* fix(map): resolve the persisted basemap before the map first renders by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6978
* fix(network): point the API base URL at the R2-backed apiv2 host by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7004
* fix(settings): floor the beacon broadcast-target list at one row by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7010
* fix(settings): re-learn the node number when the first region set renumbers the radio by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7021
* fix(connections): scope region warnings to the active connection by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/7015
* fix(mqtt): downgrade unparseable-payload decode logs from error to warn by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7040
* fix(node): gate direct messages on holding a public key by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7050
* fix(takserver): skip compressed v1 TAK packets to stop duplicate contacts by @texaskst in https://github.com/meshtastic/Meshtastic-Android/pull/7020
* fix(firmware): move firmware artifact fetching onto the R2 hosts by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7073
* fix(settings): fetch Mesh Beacon config from remote-admin nodes by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7077
* fix(messaging): keep the composer IME session stable while the node list changes by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7079
* fix(network): never send heartbeat nonce 1, the firmware NodeInfo-ping trigger by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7078
* fix(lora): correct preset SNR floors and signal colors by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7110
* fix(settings): allow all available spread factors by @gargomoma in https://github.com/meshtastic/Meshtastic-Android/pull/7119
* fix(nfc): write to tags that have never been NDEF-formatted by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7123
* fix(node): stop presenting MQTT-only nodes as unheard on the current LoRa by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7138
* fix(node): revert the time frame selector to a segmented row by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7142
* fix(messaging): keep the Enter key in the message composer by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7155
* fix(storeforward): dedupe a router replay by original_id by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7158
* fix(service): keep the process alive and awake through firmware updates and scans by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7159
* fix(messaging): key conversations to channel identity, not slot index by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7162
* fix(ui): drop the lazy list cache windows that crash inside a lookahead scope by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7185
* fix(settings): state lockdown's irreversibility in the enable dialog by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7171
* fix(network): point the API base URL at the production host by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7197
* fix(discovery): write a dwell and its parent check in one transaction by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7204
* fix(map): keep the 300 ms ease on MapLibre camera nudges by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7240
* fix(ble): replace deprecated preConflate with bufferCapacity by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7236
* fix(model): fold 0xAE into the hash of an AEAD channel by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7239
* fix(model): align the US first-setup preset with the device screen by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7244
* fix(settings): name config exports after the long name, not the short name by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7248
* fix(nodes): the direct filter no longer returns MQTT nodes by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7256
* fix(mqtt): TLS switch shows and sets the stored flag for the public broker by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7255
* fix(connection): name the transport in handshake stall reports by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7276
* fix(mqtt): subscribe with the options the negotiated protocol version allows by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7273
* fix(analytics): give crash reports the radio that produced them, a real blame frame, and a ceiling by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7274
* fix(settings): write the external notification duration in milliseconds by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7307
* fix(node): keep the node counts visible while searching by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7311
* fix(ble): don't arm a BLE transport on hardware without Bluetooth by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7321
* fix(connections): refuse serial addresses without USB host by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7327
* fix(service): stop a stale saved address overwriting a newer selection by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7329
* fix(service): stay foreground only for an address that can connect by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7330
* fix(messaging): clarify the message filter controls by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7324
* fix(notifications): give bubbles an adaptive icon by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7336
* fix(position): send no coordinates with a position request when we have none by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7361
### 📝 Other Changes
* refactor(settings): edit the status message on the user screen by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6951
* refactor(map): retire the maps-utils workarounds its 5.1 fixes made stale by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7046
* refactor(prefs): keep each surface's filters in one state object by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7120
* refactor(appfunctions): migrate to AppFunctionServiceEntryPoint by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/7340

## New Contributors
* @azchohfi made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6864
* @gargomoma made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/7119


## [2.8.1] - 2026-08-20

### 🏗️ Features
* feat(settings): add 2.8 amateur and EU Lite/Narrow LoRa regions by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6548
* feat(takserver): surface mesh nodes to ATAK as CoT contacts (mesh-to-CoT) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6554
* feat: distinguish saved coverage estimates in the map layers sheet by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6578
* Align Android message status wording by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6133
* feat(settings): export the node database as JSON by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6610
* feat(tak): show local server status by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6599
* feat(messaging): add optional full message timestamps by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6639
* feat(settings): warn before enabling licensed (ham) mode by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6632
* perf(startup): move WorkManager and AppFunctions init off the main thread by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6648
* feat(analytics): report key user interactions as named RUM actions by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6654
* feat(node): surface incomplete nodes with a badge and show them by default by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6673
* refactor(maps): share custom tile providers by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6641
* feat(demo): make Demo Mode reachable and populated in release builds by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6691
* feat(lora): honor a pinned-preset intent advertised for UNSET by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6711
* feat(icons): add Material icons for custom SF Symbols and fix soil telemetry icons by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6722
* feat(settings): add About screen with hardware carousel and rename license screen to Acknowledgements by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6721
* feat(firmware): add nRF52/RP2040 factory erase and OTAFIX bootloader upgrade over USB by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6526
* feat(node): label SNR quality on the Node Details signal row by @beecho01 in https://github.com/meshtastic/Meshtastic-Android/pull/6703
* fix(database): raise default cache limit and warn before eviction by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6742
* feat(tak): add TAK Mesh Channel setting for outbound TAK traffic by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6770
### 🖥️ Desktop
* fix(desktop): stop SIGSEGV on every Linux notification by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6543
* fix(desktop): free the GError and release libnotify on shutdown by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6544
* fix(desktop): restore BLE scanning and connecting in packaged builds by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6558
* fix(service): stop blocking getString on Dispatchers.Default-reachable notification paths by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6668
* fix(messaging): disambiguate sender-scoped packet IDs by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6624
* fix(connections): hide the replay demo entry when its capture asset is absent by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6692
* fix(lifecycle): harden packet admission and transport ownership by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6716
### 🛠️ Fixes
* fix(ui): give rx_snr real presence semantics end to end by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6523
* fix(debug): restore node ID hex annotations broken by the Wire migration by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6532
* fix(nodes): suppress alerts during initial database sync by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6538
* fix(ble): pause background RSSI polling by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6539
* fix(database): recover wedged observer pools by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6540
* fix(docs): publish only screenshots a synced page references by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6557
* fix(icons): differentiate google/fdroid debug launcher icons by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6564
* fix(tak): re-encode bundled .p12 certs with legacy PKCS#12 algorithms for Android ≤ 9 ATAK compatibility by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6569
* fix: Site Planner map/camera parity with iOS by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6577
* fix(service): unwedge the inbound pipeline behind stale-Connected zombies by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6587
* fix(map): scope location tracking to the visible lifecycle by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6602
* fix(konsist): normalize source paths on Windows by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6604
* fix(map): reuse cluster items across camera recompositions by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6605
* fix(fdroid): ship 16 KB-compatible GeoPackage SQLite by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6606
* fix(tak): gate V2 packets on known firmware by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6600
* fix(messaging): wait for shared-contact routing ACK by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6603
* fix(build): sign debug builds with a shared checked-in keystore by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6615
* fix(ci): unbreak the merge queue on Gradle 9.6.1 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6636
* fix(ui): add MQTT context to channel uplink/downlink toggle labels by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6614
* fix(i18n): localize emoji picker strings by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6628
* fix(database): delete nodes and metadata atomically by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6623
* fix(ci): sync Flatpak Gradle distribution by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6625
* fix(mqtt): isolate overlapping client sessions by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6616
* fix(data): stop "1 hour" log retention from wiping the MeshLog table by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6635
* fix(node): chart environment metrics in the user's display units by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6634
* fix(messaging): time out orphaned "Sending..." messages into a retryable failure by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6630
* fix(ui): stop emoji picker clipping text at large font scales by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6631
* fix(service): await persisted device before boot reconnect by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6617
* fix(node): stop compass updates while backgrounded by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6620
* fix(service): load persisted MeshLog cleanup policy by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6621
* fix(usb): validate permission callbacks by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6622
* fix(settings): refresh delayed remote public keys by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6638
* fix(ui): locale-aware unit formatting and message-view font scaling (design audit) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6629
* fix(android): preserve shared text in deep links by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6619
* fix(test): isolate androidApp unit tests from the production Application by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6644
* fix(settings): show REDACTED for a remote node's withheld private key by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6649
* fix(ui): surface watchdog-forced reconnects in the nav connection icon by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6655
* fix(ble): enable Kable preConflate to prevent scan-callback ANRs by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6652
* fix(ui): restore bottom time-axis height at large font scales by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6656
* fix(telemetry): repoint 1-Wire temperature at per-channel fields, adopt ADC voltage by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6653
* fix(analytics): cover local firmware sideloads and report message_send in the foreground by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6660
* fix(database): bound withDb execution so one wedged callback can't stall every write by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6661
* fix(konsist): anchor path rules to the scanned checkout root so the commonMain guard isn't vacuously green in agent worktrees by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6665
* fix(database): recover from Room's silent connection-pool wedge (#6608) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6658
* fix(settings): restore channels from device profiles by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6618
* fix(map): open Site Planner for the selected node by @simulationstation in https://github.com/meshtastic/Meshtastic-Android/pull/6640
* fix(map): allow layers with generic MIME types by @ayysasha in https://github.com/meshtastic/Meshtastic-Android/pull/6663
* fix(ui): move event firmware branding off the app bar by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6676
* fix(lora): validate 2.4 GHz bandwidth options by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6529
* fix(build): track compose-multiplatform's actual version in the flatpak arm64 force-resolve by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6694
* fix(build): declare navigationevent-compose explicitly, fixing Dokka's metadata compile by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6698
* fix(notifications): stop repeated protected position alerts by @ayysasha in https://github.com/meshtastic/Meshtastic-Android/pull/6700
* fix(lora): keep a deliberately pinned preset at fresh setup by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6710
* fix(admin): retain session refresh across multi-hop latency by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6718
* fix(discovery): restore radio state after interrupted scans by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6717
* fix(network): handle IPv6 mDNS addresses correctly, refresh on re-announce by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6729
* fix(ui): request HIGH error correction for generated QR codes by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6730
* fix(barcode): narrow the F-Droid scanner to QR-only by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6731
* fix(ai): read the real inference source instead of hardcoding it by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6732
* fix(firmware): add fallback painter to device hardware image by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6733
* fix(map): restore osmbonuspack native styling, fix tile-scaling gap by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6735
* fix(takserver): wire TAKPacket-SDK's logger, surface remarks-stripped by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6736
* fix(node): confirm before clearing a node's position track by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6745
* fix(takserver): route TAK self-test through the real v1/v2 dispatch path by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6746
* fix(map): scope Google Maps clustering to the viewport above 1000 nodes by @beecho01 in https://github.com/meshtastic/Meshtastic-Android/pull/6739
* fix(node): make node-list search case-insensitive for non-ASCII names by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6753
* fix(ble): refresh a stale GATT cache during ordinary reconnects by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6744
* fix(connections): open the region picker directly from the region card by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6743
* fix(tak): duplicate ATAK contacts on v1 GeoChat + surface v1-fallback notice by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6759
* fix(connections): gate manual + recent TCP connects on ACCESS_LOCAL_NETWORK by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6766
* fix(service): explain, don't hang, when a TCP reconnect lacks local-network access by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6767
* fix(firmware): point OTAFIX bootloader self-update at meshtastic's own fork by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6769
* fix(firmware): map T_ECHO_CARD (136) to SoftDevice 6.1.1 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6776
* fix: derive temperature unit from locale temperature preference, not distance system by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6775
* fix(ci): bump verify-flatpak's vendored Gradle dist to 9.7.1 to match the wrapper by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6782
* fix(dfu): back off correctly when Android throttles BLE scan-starts by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6784
### 📝 Other Changes
* refactor(compose): adopt keyed SideEffect and drop redundant config write-back effects by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6684
* refactor(firmware): dedupe the Android/JVM download byte-copy loop by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6737

## New Contributors
* @clayburn made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6531
* @simulationstation made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6602
* @ayysasha made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6663
* @beecho01 made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6703


## [2.8.0] - 2026-08-01

### 🏗️ Features
* feat(export): add hop start and relay node columns to CSV export by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5822
* feat(mqtt): add phone-local MQTT proxy cutoff control by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5823
* feat(node): show our node shortname chip on the Nodes tab by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5820
* feat(settings): add remote "Set time" admin action by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5821
* feat(network): on-device capture-replay transport + ingestion fuzzing/hardening by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5846
* perf(node): add stable keys and contentType to telemetry chart lists by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5869
* feat(connections): list only BLE devices visible via scan by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5877
* feat(ui): use modem-preset-relative SNR thresholds for signal quality by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5903
* feat(firmware): link OTAFIX bootloader from slow-DFU success screen by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5917
* feat(node): add GPX export to position log screen by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5919
* feat: offline-first event firmware metadata (JSON schema + bundled asset) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5920
* feat(firmware): drive event firmware branding from bundled metadata by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5929
* feat(lora): consume region→preset compatibility map + TINY presets (protobufs #951) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5834
* feat(lockdown): firmware lockdown mode (provision / unlock / lock-now) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5939
* feat(lora): gate region→preset map + TINY presets on firmware capability by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5941
* feat(security): surface XEdDSA packet signing in node & messaging UI by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5976
* fix(security): make XEdDSA signing shield green & prominent by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5980
* Prevent Range Test from running on public/default channel by @dubsector in https://github.com/meshtastic/Meshtastic-Android/pull/5986
* feat(network): migrate TcpTransport to ktor-network (commonMain) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5995
* feat(ui): StatusSurface AA legibility + node-details signing/transport polish by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5985
* feat: NFC tag writing for shared contacts and channels by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6030
* feat: Waypoint geofences (editor, map overlays, alert engine)  by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6014
* feat(lora): default US region to LongTurbo preset by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6009
* feat(connections): add deep link to trigger a connection by address by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6036
* feat(desktop): add Flathub screenshots to metainfo.xml by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6042
* feat(testing): debug-only skip_onboarding intent extra for AI/CI tooling by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6044
* feat(discovery): surface received Mesh Beacon invitations by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6043
* feat(firmware): nRF52 legacy BLE DFU — stock-bootloader fixes + stranded-device recovery by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6041
* feat(settings): wire is_unmessagable/is_licensed into DeviceProfile export/import by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6065
* Style GeoJSON overlays from simplestyle-spec (fill/stroke) by @garthvh in https://github.com/meshtastic/Meshtastic-Android/pull/6088
* feat(discovery): Mesh Beacon client with iOS 014-mesh-beacons parity by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6097
* feat(messaging): translate chat messages in-place with on-device ML Kit (google flavor only) by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/6103
* feat(database): unify device database across transports by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6096
* feat: compute EPA NowCast AQI from PM2.5 history by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6102
* Add secure key backup/restore/delete for security config by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6105
* feat(messaging): @mention with deep-link to node detail (#6098) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6108
* feat(node): label power channels and fix pressure axis scale by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6111
* Show outgoing message status text by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6121
* feat(map): Site Planner coverage integration — import + estimate by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6136
* feat(node): show CO₂ sensor temperature & humidity on Air Quality page by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6143
* feat(node): histogram of nodes per hop distance (#5745) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6146
* feat(map): F-Droid map-layer parity — share layer UI + logic in common source (#6138) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6148
* Promote CO2 temperatures to summary if no other temp is present. by @DaneEvans in https://github.com/meshtastic/Meshtastic-Android/pull/6153
* feat(firmware): Confirm local firmware files before flashing by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6083
* feat(messaging): split Conversations into collapsible Channels/DM sections by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6169
* perf(mqtt): drop payload-less client-proxy downlinks before forwarding by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/6171
* feat(map): send a waypoint as a DM or to a specific channel by @joeyleake in https://github.com/meshtastic/Meshtastic-Android/pull/6218
* feat(firmware): nightly preview channel behind the hidden-features unlock by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6225
* perf(r8): enable optimization for release builds (keep unobfuscated) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6275
* feat(analytics): restore RUM view tracking on Nav3 (Google flavor) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6281
* perf(analytics): match iOS Datadog RUM monitoring, drop dead timber dep by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6278
* Align NFC contact import and share dialogs with Design Standards v1.4 by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/6332
* feat(settings): add packet authenticity policy by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6178
* feat(messaging): redesign message bubbles toward the M3 conversation pattern by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6368
* feat(notifications): align message notifications with Android best practices by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6371
* feat(node): group related metric cards into vertical columns by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6431
* feat(node): show AQI in the air quality graph and table by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6434
* feat(event): DEF CON 34 logo and full brand palette in event theming by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6483
### 🖥️ Desktop
* fix(data): stale firmware/hardware caches — stop cancelling slow API refreshes, prune pulled releases, seed from newer bundles by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6060
* fix(geofence): restrict crossing alerts to creator, add per-geofence opt-in by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6117
* fix(ble): refresh GATT cache around ESP32 OTA profile changes by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6166
* Add firmware update notice by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6309
* fix: make NotificationManager.dispatch await the platform send by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6326
* fix(database): make DB updates atomic across device switches by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6256
* fix(ci): defer desktop packaging toolchain resolution by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6401
* fix(settings): retain config session across navigation by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6449
* fix(event): honor disabled node events, gate brand URLs, observe the manifest by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6499
### 🛠️ Fixes
* fix(mqtt): make the MQTT client-id unique per connection by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5755
* fix(ble): Harden BLE connection lifecycle by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5795
* fix(build): isolate ML Kit GenAI to the Google flavor (fix F-Droid rb-check) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5824
* fix(notifications): open node detail when tapping 'New Node Seen' notification by @LesterCheng in https://github.com/meshtastic/Meshtastic-Android/pull/5752
* fix(appfunctions): keep AppSearch document-factory constructors under R8 full mode by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5829
* fix(service): resolve selected-device startup race by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5828
* fix(database): defer FTS backfill on cold start and enforce single-connection pool by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5841
* fix(ble): retrigger connection when bonding is interrupted by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5849
* fix(desktop): terminate process on exit; quit on close when no tray by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5858
* fix(settings): crash opening Position radio-config screen by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5862
* fix(settings): gate Traffic Management config at firmware v2.8.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5864
* fix(network): retry transient connection/IO failures to api.meshtastic.org by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5870
* fix(ui): recognize VPN and all networks for network scan availability by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5882
* fix(data): separate refresh timeouts from Room persistence by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5881
* fix(service): recover stalled WiFi/TCP handshakes by cycling active transport by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5856
* fix(ui): prevent duplicate LazyColumn keys in node metrics logs by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5890
* fix(network): preserve TCP reconnect backoff on short sessions by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5893
* fix(connections): coordinate BLE and TCP scan lifecycle by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5887
* fix(ui): show Wi-Fi unavailable banner only during active network scan by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5892
* fix(network): migrate to mqtt-client 0.4.0 (IP-literal TLS fix) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5895
* fix(ble): require fresh advertisement for auto-reconnect by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5912
* fix(firmware): harden ESP32 OTA + nRF DFU update paths (hardware-validated) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5915
* fix(firmware): batch of P3 OTA/DFU cleanups from the #5915 audit by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5916
* fix(firmware): render chirpy mascot via painterResource in update dialog by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5925
* fix(usb): Add serial presence recovery for USB replug by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5923
* fix(data): Persist TAK module config by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5933
* fix(usb): Suppress expected serial close warnings by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5932
* refactor(connections): Derive DeviceType from InterfaceId by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5935
* fix(usb): Surface permission denial as permanent disconnect by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5943
* refactor(ble): Make Kable connect fallback explicitly bounded by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5944
* refactor(connections): Show one active transport pane by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5956
* fix(ble): Restore bounded bonded reconnect fallback by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5960
* fix(docs): preserve #anchor when rewriting sibling links for Docusaurus by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5963
* fix(ble): Bound Android bonding wait by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5967
* fix(ble): Avoid duplicate bonding retries after pairing failure by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5969
* fix(ble): Stop transport connect after failed bonding by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5973
* fix(ble): Fail bonding promptly when polled state returns none by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5982
* fix(car): wire notifications & emergency, fix TabTemplate crash, pin car-app to stable by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5997
* fix(qr): Serialize channel import writes by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/5999
* fix(ui): stop node signal pill from wrapping; restore full-width spread by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6007
* fix(car): suppress INVISIBLE_MEMBER in CarScreensTest for fdroid build by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6010
* fix(docs): stop builds from churning tracked docs screenshots by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6012
* fix(qr): Preserve incoming channels when adding from QR by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6013
* fix(car): notification-only car messaging for production; park templated behind flag by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6015
* fix(firmware): repair nRF USB firmware update and post-update reconnect by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6018
* fix(ble): Handle scan registration failure by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6019
* fix(discovery): show disabled reason below Start Analysis button by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6027
* fix(qr): Stabilize scanner lifecycle and imports by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6040
* fix(connections): label the connecting-card button "Stop Connecting" by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6046
* fix(messages): Refresh channel placeholders after updates by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6054
* fix(qr): Filter duplicate ADD imports by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6056
* fix(ci): rename skip_author to ignore_usernames in .coderabbit.yaml by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6069
* fix(qr): Apply channel replacements reliably by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6072
* fix(logs): Allow access to DebugPanel Logs while disconnected by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6074
* fix: harden against adversarial mesh-fuzz findings (crash, GC-thrash, unbounded growth, OOM) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6093
* fix: remove satellite-count chip from node-list metrics row by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6100
* fix: derive phone-UI units from OS locale, not radio display config by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6101
* fix(ui): align AdaptiveTwoPane split to the adaptive directive breakpoint by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6112
* fix(settings): Apply manual channel writes in order by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6077
* fix(settings): Generate fresh PSK for named manual channels by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6076
* fix(discovery): let default-channel users start a scan by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6120
* fix(discovery): ship a fresh LoRaConfig when switching to a beacon's preset by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6135
* fix(runtime): harden BLE profile, OTA setup, and DB access by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6126
* fix(messaging): use stored contact_key to avoid duplicate LazyColumn key crash (#6131) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6142
* fix(metrics): air-quality chart legend follows plotted data, not selection by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6145
* fix(firmware): Keep bundled release discovery available by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6082
* fix(dfu): Detect legacy bootloaders before secure fallback by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6079
* fix(firmware): Retry ESP32 OTA service connections cleanly by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6081
* fix(database): recover firmware metadata reads after DB pool churn by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6177
* fix: address 2.8.0 release-audit findings by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6189
* fix: keep node-mute authoritative over @mentions by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6190
* fix: dedup node lists to prevent LazyColumn duplicate-key crash by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6193
* fix(discovery): restore radio home config after an interrupted scan by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6192
* fix(service): route notification quick-replies through the send pipeline; drop dead parcelize plugin by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6196
* fix(firmware): never offer an alpha release older than current stable by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6198
* fix(settings): keep the Debug Panel accessible while disconnected by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6199
* fix(database): force Room single-connection pool on all platforms by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6202
* fix(dfu): harden Legacy nRF52 recovery after BLE stalls by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6201
* fix(dfu): classify disconnects from failed Legacy writes by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6209
* fix(database): migrate device identity across firmware 2.8 renumbering by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6228
* fix(database): make completed cross-transport merges retry-idempotent by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6231
* fix(database): route all one-shot DB writes through the merge drain barrier by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6233
* fix(database): make the injected DiscoveryDao follow the active database by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6236
* fix(agents): datadog-rum-investigator blocked by Datadog MCP UUID mount prefix by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6241
* Fix: add back navigation to Clean Node Database screen by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6264
* fix: stabilize traceroute map node identity & fix log metric alignment by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6270
* fix(connections): show device long names in the connection manager (#5808) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6271
* fix: adopt 2024 EPA PM2.5 AQI breakpoints for cross-platform alignment by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6273
* fix(analytics): restore Datadog RUM network tracking (google) & unify service name by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6280
* fix(connections): use a segmented button for transport selection by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6285
* fix: silence false-positive Instantiatable lint on ExtensionAppFunctionService by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6293
* fix: label channel share FAB "Import/Export" and announce Close when expanded by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6303
* fix: normalize F-Droid QR camera frames by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6319
* fix: preserve add mode in legacy channel URLs by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6320
* fix: align channel QR share contract by @RCGV1 in https://github.com/meshtastic/Meshtastic-Android/pull/6325
* fix: keep cached data flowing when api.meshtastic.org is down by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6335
* Fix literal backslashes in strings with escaped apostrophes by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/6336
* fix(map): allow editing/deleting your own locked waypoint by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6344
* fix(waypoint): reject non-owner modification of a stored locked waypoint by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6348
* fix(nodes): refresh distance when display units change by @CatSu-OSM in https://github.com/meshtastic/Meshtastic-Android/pull/6351
* fix(map): retain camera across tab navigation by @CatSu-OSM in https://github.com/meshtastic/Meshtastic-Android/pull/6352
* fix(database): reduce write-lane pressure from one-shot reads by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6255
* fix(nav): register /wifi-provision and /discovery as https App Links by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6365
* fix(node): stop metric-card trailing values crushing to one-char-per-line by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6374
* fix(node-metrics): apply full typography style instead of fontSize-only by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6375
* fix(node): respect display units for position ground speed by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6376
* fix(node): reconcile stale identity replays after renumbering by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6259
* fix(ui): add horizontal padding for position flags list item by @dzmpr in https://github.com/meshtastic/Meshtastic-Android/pull/6369
* fix(messaging): reduce composer recomposition and typing jank by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6318
* fix(ble): harden reconnect sessions and Android scan lifecycle by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6384
* fix(settings): stop dropping admin config responses (0% stall, missing remote channels) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6391
* fix(hardware): throttle repeated catalog refreshes for cache misses by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6399
* fix(firmware): decode manifests independent of content type by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6400
* fix(firmware): don't treat unparseable version as "too old" (#3726) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6408
* fix(channels): store channel set per-device to stop cross-device duplicates (#4623) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6409
* fix(android): avoid retaining activity in Coil singleton factory by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6403
* fix(messaging): use mesh time for message timestamps and grouping by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6422
* fix(strings): correct English subnet label by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6425
* fix(android): avoid resource stalls in connection status by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6398
* fix(node): show node counts without truncation by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6433
* fix(model): gate TAK module config on firmware 2.8.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6435
* fix(data): don't let coarse position reports overwrite precise coordinates by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6432
* fix(car): drop CarAppService from production manifests to satisfy Play car-app review by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6438
* fix(map): configure OSMdroid user agent at F-Droid startup by @jeremiah-k in https://github.com/meshtastic/Meshtastic-Android/pull/6448
* fix(strings): remove stray "to" in share location description by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6463
* fix(mqtt): scope user-CA trust to the MQTT connection by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6464
* fix(logging): stop reporting expected conditions as crashes by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6470
* fix(map): stop reporting cancelled layer loads as errors by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6468
* fix(network): resync stream framing and stop reporting expected disconnects as errors by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6469
* fix(notifications): guard blank conversation-shortcut labels by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6472
* fix(service): guarantee startForeground is reached or MeshService stops itself by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6471
* fix(node): disambiguate signal-log packet keys by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6492
* fix(mqtt): log throwable-less client errors at warn by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6493
* fix(connections): dedupe recent-TCP and reaction list keys by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6494
* fix(map): allow local deletion of any waypoint, locked or not by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6497
* fix(model): rssi explicit presence for protobufs 2.7.26.138 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6498
* fix(connections): send set_time_only at MyNodeInfo instead of onNodeDbReady by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6503
* fix(ui): stop discarding measured-zero sensor and RSSI readings by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6507
* fix(mqtt): stop reporting transport failures as credential rejections by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6506
### 📝 Other Changes
* refactor(takserver): commonize TAK SDK pipeline, drop redundant zstd/xpp3 deps by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5871
* refactor(settings): remove Traffic Management module config by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5878
* refactor(firmware): dedupe BLE/DFU OTA transport + handler boilerplate by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5918
* refactor(data): consolidate bundled-asset loading behind BundledAssetReader by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5921
* refactor(core:ui): drop redundant SinglePaneSceneStrategy from NavDisplay by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5934
* refactor: drop two over-engineered seams (enum + stdlib Base64) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5945
* refactor(ui): migrate MapView dialog to Compose M3 + drop legacy material dependency by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5988
* refactor(settings): replace SimpleDateFormat with kotlinx-datetime by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5992
* refactor(car): drop dead FuzzyNodeNameResolver duplicate by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5994
* refactor(qr): apply channel imports atomically via edit-settings transaction by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6170
* refactor(di): drop the F-Droid ApiService stub, use the real API client in both flavors by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6226
* refactor(map): adopt maps-compose 8.4.0 stock clustering, drop custom renderer workaround by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6301
* refactor(map): migrate custom overlays to the maps-utils 5.0 data layer by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6304
* refactor(ui): remove StatusSurface scrim behind status-colored chips by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6367
* perf(docs): skip Dokka when no API sources changed, drop test modules from aggregation by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6412
* fix(desktop): restore eager JBR javaHome pin for ProGuard packaging by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6414
* Clean up configuration menus by @pdxlocations in https://github.com/meshtastic/Meshtastic-Android/pull/6478
* chore(deps): update org.meshtastic:mqtt-client to v0.8.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/6508

## New Contributors
* @LesterCheng made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/5752
* @dubsector made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/5986
* @garthvh made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6088
* @coderabbitai[bot] made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6180
* @madeofstown made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6210
* @joeyleake made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6218
* @sashko made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6315
* @CatSu-OSM made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6351
* @dzmpr made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6369
* @pdxlocations made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/6478


## [2.7.14] - 2026-06-03

### 🏗️ Features
* refactor(ble): Centralize BLE logic into a core module by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4550
* feat(ble): Add support for `FromRadioSync` characteristic by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4609
* feat(widget): Add Local Stats glance widget by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4642
* chore(deps): bump deps to take advantage of new functionality by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4658
* feat(maps): Google maps improvements for network and offline tilesources by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4664
* feat: Improve edge-to-edge and display cutout handling by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4669
* feat: upcoming support for tak and trafficmanagement configs, device hw by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4671
* feat: settings rework by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4678
* feat: settings rework part 2, domain and usecase abstraction, tests by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4680
* feat: service decoupling by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4685
* refactor: migrate :core:database to Room Kotlin Multiplatform by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4702
* refactor(ble): improve connection lifecycle and enhance OTA reliability by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4721
* refactor: migrate preferences to DataStore and decouple core:domain for KMP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4731
* refactor: migrate core modules to Kotlin Multiplatform and consolidat… by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4735
* feat: Migrate project to Kotlin Multiplatform (KMP) architecture by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4738
* refactor: migrate from Hilt to Koin and expand KMP common modules by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4746
* refactor: migrate core UI and features to KMP, adopt Navigation 3 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4750
* feat: introduce Desktop target and expand Kotlin Multiplatform (KMP) architecture by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4761
* build(desktop): enable ProGuard for release builds by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4772
* feat(desktop): implement DI auto-wiring and validation by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4782
* feat(desktop): expand supported native distribution formats by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4783
* feat: Complete ViewModel extraction and update documentation by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4817
* refactor: Replace Nordic, use Kable backend for Desktop and Android with BLE support by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4818
* feat: Integrate notification management and preferences across platforms by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4819
* feat: service extraction by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4828
* feat: build logic by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4829
* feat: Desktop USB serial transport by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4836
* Add "Exclude MQTT" filter to Nodes view. by @VictorioBerra in https://github.com/meshtastic/Meshtastic-Android/pull/4825
* feat: mqtt by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4841
* feat: Integrate Mokkery and Turbine into KMP testing framework by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4845
* feat: Complete app module thinning and feature module extraction by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4844
* feat: Enhance test coverage  by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4847
* feat: Implement KMP ServiceDiscovery for TCP devices by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4854
* feat: Add KMP URI handling, import, and QR code generation support by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4856
* feat: KMP Debug Panel Migration and Update Documentation by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4859
* feat: Migrate to Room 3.0 and update related documentation and tracks by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4865
* feat: Implement iOS support and unify Compose Multiplatform infrastructure by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4876
* Add InlineMap implementation for F-Droid build by @theKorzh in https://github.com/meshtastic/Meshtastic-Android/pull/4877
* refactor(desktop): remove native MenuBar from main window by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4888
* feat: Migrate networking to Ktor and enhance multiplatform support by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4890
* refactor: adaptive UI components for Navigation 3 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4891
* feat: Integrate AlertHost into desktop application and add UI tests by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4893
* feat: implement global SnackbarManager and consolidate common UI setup by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4909
* feat: implement unified deep link routing for Kotlin Multiplatform by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4910
* refactor: BLE transport and UI for Kotlin Multiplatform unification by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4911
* Refactor map layer management and navigation infrastructure by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4921
* feat: migrate to Material 3 Expressive APIs by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4934
* Refactor nav3 architecture and enhance adaptive layouts by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4944
* feat(tak): introduce built-in Local TAK Server and mesh integration by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4951
* feat(analytics): expand DataDog RUM integration and align with iOS parity by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4970
* feat(wifi): introduce BLE-based WiFi provisioning for nymea-compatible devices by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4968
* feat(wifi-provision): add mPWRD-OS branding and disclaimer banner by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4978
* feat(charts): adopt Vico best practices, add sensor data, and migrate TracerouteLog by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5026
* refactor(icons): migrate to self-hosted VectorDrawable XMLs via MeshtasticIcons by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5030
* feat(messaging): add IME Send action to message input by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5047
* feat(metrics): redesign position log with SelectableMetricCard and add CSV export to all metrics screens by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5062
* feat(core/ui): add safeLaunch, UiState, KMP permissions, and CMP lifecycle modernization by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5118
* feat(desktop): add entitlements and wire MeshConnectionManager into orchestrator by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5127
* feat(environment): add 1-Wire multi-thermometer (DS18B20) display support by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5130
* feat: add high-contrast theme with accessible message bubbles by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5135
* feat(mqtt): migrate to MQTTastic-Client-KMP by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5165
* feat(mqtt): adopt mqttastic-client-kmp 0.2.0 — disconnect reasons + Test Connection by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5181
* feat(firmware): nRF52 BLE Legacy DFU support by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5209
* feat(service): send polite ToRadio(disconnect=true) before transport close by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5210
* feat(node): smoother remote-admin UX with per-node session tracking by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5217
* fix(ble): unblock reconnect + kable audit (logging, priority, backoff, StateFlow) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5222
* feat: Enhance mPWRD-os WiFi provisioning success state and UI components by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5225
* feat(messaging): add entry points for filter settings by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5229
* feat(messaging): send message on Enter keypress by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5246
* feat(desktop): native OS notifications via libnotify/osascript/PowerShell by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5253
* feat(auto): enable Android Auto messaging notifications by @riddlemd in https://github.com/meshtastic/Meshtastic-Android/pull/5265
* fix: update emoji catalog metadata and improve picker synchronization by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5292
* fix: update notification icon by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5293
* feat(connections): connection sorting & conversation empty channel ranking by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5295
* fix(connections): improve BLE scan reliability and UI lifecycle by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5329
* feat: event firmware easter egg with ambient branding by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5354
* feat: align theme with Design Standards v1.3, remove contrast setting by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5355
* feat(desktop): fix mac notifications, new desktop icons by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5403
* Update notification intents and deep link URI format by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5408
* fix: clarify position precision as ± radius  by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5428
* feat: TAK v2 protocol integration with zstd compression and full CoT type support by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/5434
* feat(flatpak): reconstruct standard maven filenames from local Gradle cache by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5538
* fix: use single-shot low battery notifications by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5550
* feat: align node list context menu to canonical 6-item order by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5548
* feat: enable WAL connection pool for parallel reads by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5372
* feat: node list density switching with compact layout and field toggles by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5444
* feat(ai): upgrade Chirpy on-device AI with proper APIs, download UX, and streaming by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5579
* feat: replace LoRa bandwidth text input with constrained dropdown by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5687
* feat: Save unsent chat message as draft by @Copilot in https://github.com/meshtastic/Meshtastic-Android/pull/5686
### 🖥️ Desktop
* fix(desktop): keep Vico package to prevent bytecode verification errors by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5424
### 🛠️ Fixes
* fix(strings): replace plurals by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4596
* fix: replace fdroid map_style_selection string by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4598
* refactor(test): Introduce MeshTestApplication for robust testing by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4602
* fix: spotless by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4604
* feat(build): Implement flavor-specific barcode scanning and build improvements by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4611
* fix(qr): add channels as key to remember block to fix add-channel rac… by @nreisbeck in https://github.com/meshtastic/Meshtastic-Android/pull/4607
* chore(ble): Add Proguard rules for Nordic BLE library by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4618
* ci(release): Use wildcards for APK paths in release workflow by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4619
* chore(ci): Use wildcard for APK paths in release workflow by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4622
* chore(ci): Refine analytics task filtering and improve release debugging by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4624
* Fix/splits by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4626
* Align FDroid MapView constructor with Google version (Issue #4576) by @ujade in https://github.com/meshtastic/Meshtastic-Android/pull/4630
* refactor(analytics): reduce tracking footprint by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4649
* fix(map): location perms and button visibility, breadcrumb taps by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4651
* fix(strings): Correct capitalization of Ham by @alecperkins in https://github.com/meshtastic/Meshtastic-Android/pull/4620
* ci: Split Google artifact attestations and ensure F-Droid uploads by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4665
* fix: Replace strings.xml with app_name resource by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4666
* Disable generate_release_notes in release workflow by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4668
* fix: ui tweaks by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4696
* refactor: simplify traceroute tracking and unify cooldown button logic by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4699
* feat: Add "Mark all as read" and unread message count indicators by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4720
* fix(widget): ensure local stats widget gets updates by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4722
* refactor(ble): increase default timeout for BLE profiling by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4728
* refactor: enhance handshake stall guard and extend coverage to Stage 2 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4730
* build(ci): optimize release workflow and update Room configuration by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4775
* Disable ProGuard for desktop release and add application icon by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4776
* fix(ble): implement scanning for unbonded devices in common connections ui by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4779
* fix: fix animation stalls and update dependencies for stability by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4784
* build(desktop): include `java.net.http` module in native distribution by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4787
* build: remove PKG from desktop distribution targets by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4788
* build: Update desktop app icons, versioning, and packaging configuration by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4789
* refactor(settings): improve destination node handling in RadioConfigViewModel by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4790
* feat(desktop): add enter-to-send functionality in messaging by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4793
* feat: enhance map navigation and waypoint handling by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4814
* build: fix license generation and analytics build tasks by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4820
* fix: resolve crashes and debug filter issues in Metrics and MapView by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4824
* fix(map, settings): allow null IDs and implement request timeout by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4851
* docs: Unify notification channel management and migrate unit tests by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4867
* fix: Implement reconnection logic and stabilize BLE connection flow by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4870
* fix: Update messaging feature with contact item keys and MQTT limits by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4871
* fix: specify jetbrains in gradle-daemon-jvm.properties by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4872
* fix(settings): remove redundant regex option in DebugViewModel by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4881
* refactor(service): update string formatting for local stats notif by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4885
* refactor(messaging): fix contact key derivation in ContactsViewModel by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4887
* feat: optimistically persist local configs and channels by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4898
* refactor(di): specify disk cache directory for ImageLoader by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4899
* refactor: null safety, update date/time libraries, and migrate tests by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4900
* refactor: remove demoscenario and enhance BLE connection stability by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4914
* refactor(ui): remove labels from navigation suite items by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4924
* build: enable `-Xjvm-default=all` compiler flag by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4929
* fix(ci): update APP_VERSION_NAME output reference in workflows by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4935
* fix(strings): Fix public key description by @Klavionik in https://github.com/meshtastic/Meshtastic-Android/pull/4957
* feat: implement XModem file transfers and enhance BLE connection robustness by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4959
* Refactor navigation to use NodeDetail route and fix radio settings by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4960
* Refactor and unify firmware update logic across platforms by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4966
* fix: improve PKI message routing and resolve database migration racecondition by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4996
* fix: resolve correct node public key in sendSharedContact and favoriteNode by @Copilot in https://github.com/meshtastic/Meshtastic-Android/pull/5005
* fix: resolve bugs across connection, PKI, admin, packet flow, and stability subsystems by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5011
* fix(tak): resolve frequent TAK client disconnections by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5015
* fix(service): resolve MeshService crash from eager notification channel init by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5034
* style: update ic_no_cell and ic_place vector drawables by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5040
* fix(build): prevent DataDog asset transform from stripping fdroid release assets by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5044
* fix(icons): replace outline (FILL=0) pathData with filled (FILL=1) from upstream Material Symbols by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5056
* fix(charts): hoist rememberVicoZoomState above vararg layers to prevent ClassCastException by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5060
* fix(ui): add missing @ParameterName annotations on actual rememberReadTextFromUri declarations by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5072
* fix(settings): hide Status Message config until firmware v2.8.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5070
* fix(transport): Kable BLE audit + thread-safety, MQTT, and logging fixes across transport layers by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5071
* fix(build): remove Compose BOM to resolve compileSdk 37 conflict by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5088
* fix(connections): show device name during connecting state by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5085
* fix(build): add explicit compose-multiplatform-animation dependency by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5095
* fix(nav): restore broken traceroute map navigation by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5104
* fix(build): overhaul R8 rules and DRY up build-logic conventions by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5109
* fix(proguard): disable shrinking for Compose animation classes  by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5116
* fix(icons): audit and correct icon migration regressions from #5030 #5040 #5056 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5136
* fix: align BLE connection handshake with firmware protocol expectations by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5141
* fix(app): add R8 keep rules for Compose animation/runtime/ui by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5146
* perf(messaging): batch node + reply lookups in message loading by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5149
* fix(app): disable R8 optimization to fix Compose animation freeze by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5150
* fix(node): don't recreate Vico CartesianChartModelProducer on channel switch by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5160
* refactor: use injected ioDispatcher and ApplicationCoroutineScope by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5167
* fix: redact MeshLog proto secrets and centralize Compose keep-rules by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5166
* fix(ui): stable LazyColumn keys, semantic roles, and content descriptions by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5168
* fix(ui): finish accessibility roles and action labels for clickable surfaces by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5170
* fix(widget): drive updates via debounced state observer by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5185
* fix(transport): improve BLE / TCP / USB reconnect and handshake resilience by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5196
* fix(fdroid): prevent NotImplementedError crash on firmware release fetch by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5197
* fix(compass): stop coarse network fixes from clobbering GPS fixes by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5200
* fix(canned-messages): enable multiline text editing for long message lists by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5203
* fix(settings): restore Import/Export button functionality in #4913 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5204
* refactor: eliminate Accompanist permissions library by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5211
* fix: MQTT proxy connection and probe test failures by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5215
* fix(ble): ensure GATT cleanup runs under NonCancellable on cancellation by @jdogg172 in https://github.com/meshtastic/Meshtastic-Android/pull/5207
* fix(ble): cleanup races discovered while reviewing #5207 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5221
* fix(ui): make footer buttons expand downwards by @zt64 in https://github.com/meshtastic/Meshtastic-Android/pull/5226
* fix(desktop): suppress Vico ColorScale ProGuard warnings by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5232
* fix(desktop): unbreak release crash via correct ProGuard rules by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5236
* fix(crashlytics): resolve beta 2.7.14 crash issues by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5245
* fix: Resolve top Crashlytics issues for 29320633 beta release by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5278
* fix: persist language switching and correctly map locales by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5287
* fix: ensure snackbar respects safe drawing padding over host modifiers by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5290
* fix(ui): align Cancel and Send enabled state by @elagin in https://github.com/meshtastic/Meshtastic-Android/pull/5284
* fix(data): default new-node notifications off for event firmware by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5323
* fix(network): resolve empty MQTT address and enforce TLS on default server by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5333
* fix(mqtt): harden TLS enforcement, add user CA trust, and improve error diagnostics by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5365
* fix: clamp future lastHeard timestamps to current time on ingestion by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5418
* revert: Update retry settings in gradle-wrapper.properties by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5430
* fix: update screenshots by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5435
* fix(database): make withDb retry logic resilient to varying close messages by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5474
* fix(settings): add input validation for BLE PIN, LoRa modem, and ambient lighting by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5477
* fix(nav): remote admin nodenum + Nav3 consolidation and improvements by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5478
* fix(database): update @Relation annotations for Room 3.0.0-alpha05 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5507
* fix: prevent node details hang when device hardware API is unreachable by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5514
* fix(settings): remote admin always showed local node config by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5560
* fix: hide battery indicator when level is 0 (never reported) by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5595
* fix: consistent column width for compact node list items by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5596
* fix(emoji): enable androidResources for core:ui to package emoji-data.json by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5597
* fix(proto): prune TakTalkMessage and TakTalkRoomData from Wire codegen by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5624
* fix(database): stabilize flaky DatabaseManagerWithDbRetryTest by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5635
* fix(ble): stop BLE scan on background and downgrade connection priority by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5644
* fix: remove Android Auto manifest entry causing Play Store rejection by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5662
* fix(takserver): emit *:-1:stcp contact endpoint so directed TAK-Talk/GeoChat routes over the mesh by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/5661
* fix(takserver): drop CoT the mesh delivers more than once by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/5667
* fix: address top Crashlytics crashes in beta 2.7.14 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5672
* fix(flatpak): source desktop metadata from in-repo packaging dir by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5673
* Rename Desktop application to 'Meshtastic Desktop' by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5677
* fix: address top Crashlytics crashes and non-fatals for build 29320984 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5684
* fix: show loading overlay immediately for remote config sub-screens by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5694
* fix(node): restore view-tree owners on map dispose so node-list popups aren't invisible by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5699
* fix(firmware): surface error state when BLE OTA connection attempts are exhausted by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5700
* fix(map): replace MarkerComposable with Canvas-rendered bitmaps by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5702
* fix(map): remove manual ViewTree lifecycle owner workarounds by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5704
* fix(map): scope cluster-renderer ViewTreeLifecycleOwner to map host view by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5708
* fix(map): initialize Maps SDK before building marker bitmap descriptors by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5709
* fix(map): eliminate cluster-renderer FATAL and harden black-map paths by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5715
* fix(map): revert app-side Maps SDK init to library-idiomatic, fix inline-map crash by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5719
* fix(map): render cluster markers in-scope to kill ClusterRenderer FATAL by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5723
* fix(map): apply kotlinx-serialization compiler plugin to androidApp by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5726
* fix(map): keep compass icon visible while following bearing by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5728
### 📝 Other Changes
* refactor(ui): compose resources, domain layer by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4628
* Add per-message transport method icons for new message format by @Kealper in https://github.com/meshtastic/Meshtastic-Android/pull/4643
* build: apply instrumented test dependencies conditionally by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4698
* docs: summarize KMP migration progress and architectural decisions by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4770
* ci(release): pass app version to desktop build via environment variable by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4774
* ai: Establish conductor documentation and governance framework by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4780
* fix: fix wrong getChannelUrl() call causing loss of "add" flag and un… by @skobkin in https://github.com/meshtastic/Meshtastic-Android/pull/4809
* chore: Enhance CI coverage reporting and add main branch workflow by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4873
* build(desktop): enable ProGuard minification and tree-shaking by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4904
* build: update Compose Multiplatform and migrate lifecycle dependencies by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4932
* chore: standardize resources and update documentation for Navigation 3 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/4961
* feat(settings): add DNS support and fix UDP protocol toggle by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5013
* fix: use payload labels in pr_enforce_labels.yml to avoid rate limiting by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5018
* fix: scope labeler trigger to reduce rate limiting and fix bugfix typo by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5020
* test(prefs): migrate DataStore tests from androidHostTest to commonTest by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5092
* fix(resources): add resourcePrefix to KMP + widget modules, rename prefixed resources by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5111
* fix(charts): apply Vico 3.1.0 best-practice audit fixes by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5138
* refactor(di): adopt @KoinApplication with startKoin<T>() compiler plugin API by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5152
* test: migrate MigrationTest to runTest and add missing repository fakes by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5171
* refactor: consolidate metric formatting through MetricFormatter by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5169
* chore(r8): remove redundant keep rules covered by consumer rules by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5172
* Revert "diag(r8): disable minify for release builds (animation-freeze diagnostic)" by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5176
* Fix node-details remove action to preserve confirmation flow by @Copilot in https://github.com/meshtastic/Meshtastic-Android/pull/5192
* Change default ContrastLevel from STANDARD to MEDIUM by @somenice in https://github.com/meshtastic/Meshtastic-Android/pull/5325
* Extract node list display settings to dedicated screen by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5580
* Upgrade takpacket-sdk to version 0.3.0 by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5621
* repo: Add Meshtastic Desktop icon SVGs by @vidplace7 in https://github.com/meshtastic/Meshtastic-Android/pull/5623
* Enhance TAKTALK support with message and room handling, update SDK to v0.3.2 by @thebentern in https://github.com/meshtastic/Meshtastic-Android/pull/5634
* Revert "feat: replace LoRa bandwidth text input with constrained dropdown" by @jamesarich in https://github.com/meshtastic/Meshtastic-Android/pull/5691

## New Contributors
* @nreisbeck made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/4607
* @ujade made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/4630
* @alecperkins made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/4620
* @skobkin made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/4809
* @VictorioBerra made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/4825
* @theKorzh made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/4877
* @Klavionik made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/4957
* @jdogg172 made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/5207
* @zt64 made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/5226
* @riddlemd made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/5265
* @elagin made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/5284
* @somenice made their first contribution in https://github.com/meshtastic/Meshtastic-Android/pull/5325

<!-- RELEASED_END -->
