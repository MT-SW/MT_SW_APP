---
title: Funkcje MT_SW
parent: Przewodnik użytkownika
nav_order: 23
last_updated: 2026-10-18
description: Przegląd tego, co MT_SW_APP dodaje do oryginalnej aplikacji Meshtastic — zdalne sterowanie GPIO, zdrowie sieci, połączenia na mapie, Mesh Link Planer, diagnostyka na żądanie, Sniffer, panel debugowania z logami urządzenia, skalowanie aplikacji i więcej.
aliases:
  - mt-sw
  - gpio
  - zdalne-sterowanie
  - mt-sw-features
  - fork
  - funkcje
  - zdrowie-sieci
  - planer
---

# Funkcje MT_SW

MT_SW_APP to aplikacja oparta na oryginalnej aplikacji Meshtastic. Poza wszystkim, co robi oryginał, ma wiele własnych funkcji, które ułatwiają życie w sieci mesh — zwłaszcza w sieci świętokrzyskiej. Ta strona opisuje je po kolei i mówi, gdzie ich szukać.

> ℹ️ **Uwaga:** Część funkcji (Diagnostyka na żądanie, Sniffer radiowy, zasoby węzła, logi urządzenia przez Wi-Fi i USB) wymaga firmware MT_SW. Na zwykłym firmware aplikacja działa normalnie, ale te funkcje nie dostaną odpowiedzi.

## Zdalne zarządzanie

Aplikacja pozwala sterować innymi węzłami przez sieć mesh, bez dotykania urządzenia w terenie. Wszystko jest w szczegółach węzła.

- **Zdalne sterowanie GPIO** — w szczegółach węzła (moduł Remote Hardware) wpisujesz numer pinu, a aplikacja sama liczy maskę bitową i wysyła do zdalnego węzła polecenie włączenia („Włącz (1)”), wyłączenia („Wyłącz (0)”) albo odczytu pinu. Można tak zdalnie włączać przekaźniki, oświetlenie albo restartować sprzęt podłączony do węzła. Przyciski działają dopiero po wymianie kluczy z węzłem, bo polecenia są szyfrowane. W nagłówku karty GPIO jest ikona informacji z opisem krok po kroku, jak to skonfigurować.
- **Zdalne ulubione i ignorowanie** — węzeł możesz dodać do ulubionych albo go ignorować nie tylko na swoim telefonie, ale też w zdalnym węźle, przez sieć LoRa. Aplikacja czeka na prawdziwe potwierdzenie z sieci (routing ACK), więc wiesz, że zmiana dotarła.
- **Dodawanie nowych urządzeń do bazy** — nowy węzeł możesz dodać ręcznie, wpisując jego ID (np. `!a1b2c3d4`), do bazy własnego węzła albo zdalnie, do bazy innego węzła przez sieć mesh. Format ID jest w całej aplikacji taki sam, a aplikacja sprawdza wpisane ID.
- **Szybkie komendy** — przyciski `/ping`, `/hello` i `/test` wysyłają wiadomość prywatną nawet do węzłów, których rola zwykle blokuje ręczne wiadomości. Przydają się do szybkiego sprawdzenia węzła w terenie. Działają od firmware 2.8.0.
- **Sąsiedzi** — log sąsiadów pokazuje też podsłuchane rozgłoszenia innych węzłów, a nie tylko odpowiedzi na własne zapytania. W ustawieniach modułu „Info o sąsiadach” interwał wybierasz z listy godzin (6, 8, 10, 12, 18 i 24).
- **Nazwy przekaźników** — okno „Status doręczenia” pokazuje pełną listę nazw węzłów, które przekazały wiadomość. Nazwa przekaźnika jest też widoczna wprost na liście węzłów i w szczegółach węzła.
- **Trasa (traceroute)** — gdy ostatni węzeł przed nami nie wpisał się do trasy, a pasuje do niego dokładnie jeden znany węzeł, okno trasy pokazuje go jako „Prawdopodobnie: Nazwa” zamiast „Nieznany skok”.

> ⚠️ **Ważne:** Zdalne polecenia zmieniają prawdziwy sprzęt. Upewnij się, że sterujesz właściwym węzłem, zanim wyślesz polecenie GPIO.

## Zdrowie sieci

Zakładka **Zdrowie sieci** jest w dolnej nawigacji, między Węzłami a Mapą. Pokazuje w jednym miejscu, jak się mają wszystkie węzły.

- **Karta „Kondycja sieci”** — pierścień z procentem węzłów w dobrym stanie i liczbą węzłów w każdej ocenie oraz ile jest online. Kliknięcie oceny (np. „2 Źle”) pokazuje tylko węzły z tą oceną; każda zakładka pamięta swój filtr osobno.
- **Zakładki metryk** — zasilanie, sygnał, eter, środowisko, zasoby, ruch i sąsiedzi. Każdy węzeł ma kolorowy pasek, wartość i mały wykres trendu. W Sygnale główną wartością jest szum tła po korekcie LNA.
- **Kolory** — złoty to dobrze, czerwony średnio, fioletowy źle, biały bardzo źle, niebieski brak danych. Pod przyciskiem **(i)** u góry jest opis wszystkich kolorów i progów. Poziom baterii na liście węzłów ma te same kolory.
- **Wyszukiwanie i sortowanie** — pole „Szukaj węzła…”, sortowanie „Wg wartości” lub „Wg aktywności”, filtr „Tylko problemy”, ulubione na górze i ukrywanie węzłów bez danych.
- **Chowanie karty podsumowania** — przeciągnięcie w górę zwija całą kartę „Kondycja sieci”, żeby zostało więcej miejsca na listę; przeciągnięcie w dół ją rozwija. Można ją przesuwać, chwytając za samą kartę. Działa też na komputerze. Domyślnie chowanie jest zablokowane; przełącznik „Blokuj ukrywanie kondycji sieci przy przesuwaniu listy” jest w oknie **(i)**.
- **Szczegóły węzła** — wykresy w kilku widokach do wyboru: linia (z kolorowymi strefami), zegar, słupki i mapa cieplna; zakresy 1 h, 24 h, 2 dni, 7 dni, 2 tygodnie, 30 dni i „Wszystko”; pasek „Czas w każdym stanie”.
- **Podsumowanie sieci** — ikona listy u góry otwiera rankingi węzłów (najlepszy sygnał, najcichsze, najbliżej, najwięcej wiadomości i inne), wykresy kołowe i słupkowe oraz rozkład baterii i sygnału. Wynik można udostępnić albo zapisać jako PDF, HTML, Markdown, tekst lub CSV.
- **Sygnał WiFi urządzenia** — gdy firmware je wysyła, widać siłę WiFi, liczbę zerwań łącza i powód ostatniego zerwania (w Jakości sygnału i w Zdrowiu sieci).
- **Widżet** — na Androidzie można dodać na ekran główny widżet „Local Stats”. Zobacz [Widżet ekranu głównego](widget).

## Jakość sygnału i korekta LNA

- **Korekta LNA** — w Ustawienia → LoRa wpisujesz wzmocnienie LNA swojego urządzenia, a aplikacja pomniejsza wyświetlany szum i RSSI o tę wartość. W szczegółach obcego węzła jest osobne pole, które koryguje tylko szum tego węzła. Korekta jest liczona przy wyświetlaniu, więc surowe dane zostają bez zmian.
- **Ocena sygnału** — kolory takie jak w statusach połączenia (złoty, czerwony, fioletowy, biały). Ekran pomocy pokazuje rzeczywiste progi dla wybranego presetu.
- **Odwrócenie zasilania i prądu** — dwa przełączniki dla czujnika podłączonego odwrotnie: zamiana „zasilanie” z procentem baterii oraz odwrócenie znaku prądu. Ustawiasz je osobno dla każdego węzła w oknie informacji (ikona „i”). Zmieniają tylko wyświetlanie.
- **WiFi urządzenia** — osobna linia na wykresie, karty z siłą sygnału i zerwaniami oraz eksport do CSV przyciskiem „Zapisz WiFi”.

## Mapa

- **Połączenia między węzłami** — cienkie linie łączą węzły, które wymieniają pakiety, w kolorze jakości sygnału (SNR). Linie pochodzą z raportów sąsiadów, z traceroute z ostatniej doby i z węzłów słyszanych bezpośrednio. Włączają się w filtrach mapy („Pokaż połączenia między węzłami”); domyślnie są włączone.
- **Pokaż na mapie** — w szczegółach węzła, pod jego pozycją, jest przycisk **Pokaż na mapie**. Przenosi na główną mapę, przybliża do węzła i rysuje go na wierzchu, ponad innymi węzłami stojącymi w tym samym miejscu.
- **Trasa traceroute na mapie** — każdy skok jest osobną linią w kolorze jakości sygnału, ze strzałką i wartością SNR.
- **Węzły w tym samym miejscu** — są subtelnie odsunięte od siebie, więc po zbliżeniu można dotknąć każdy z osobna. Plakietki są małe, z ogonkiem wskazującym dokładną pozycję.
- **Mapy offline** — działają także dla OpenStreetMap, które jest domyślną mapą. Pobieraj małe obszary, bo serwer OSM nie lubi masowego pobierania.
- **Nakładki terenu i pogody** — cieniowanie rzeźby terenu i radar pogodowy są domyślnie włączone.
- **Płynność** — mapa przy dużej liczbie węzłów przewija się i powiększa płynniej, a mapa zawsze otwiera się dopasowana do wszystkich węzłów.
- **Komputer** — mapa działa także w wersji na komputer, z tymi samymi warstwami i filtrami co na telefonie.

Więcej o mapie: [Mapa i punkty](map-and-waypoints).

## Mesh Link Planer

Planer symuluje zasięg łącza radiowego między dwoma punktami **A** i **B**, w całości w aplikacji, na telefonie i na komputerze.

- Każdy punkt ustawisz z listy węzłów, kliknięciem w mapę, współrzędnymi albo z podłączonej stacji.
- Wysokość terenu pochodzi z modelu DEM Mapterhorn.
- Osobno dla każdej strony ustawiasz moc, zysk anteny, wysokość zawieszenia i tłumienie przewodu antenowego (ręcznie albo kreatorem z bazą kabli i złączy).
- Obsługuje pasma amatorskie od 169 MHz do 2,4 GHz oraz dowolną częstotliwość 20 MHz–20 GHz.
- Liczy model ITM (Longley-Rice), krzywiznę Ziemi i pierwszą strefę Fresnela, rysuje przekrój terenu i podaje bilans łącza oraz kąty i namiary anten.
- **Dokładny teren** — opcjonalnie pobiera z OpenStreetMap prawdziwe budynki i lasy i uwzględnia ich wysokość.
- **Pogoda na żądanie** — opcjonalnie uwzględnia warunki propagacji z bieżącej pogody (Open-Meteo).
- **Zasięg dookoła** — rysuje zasięg jako kolorowy raster na mapie, z suwakiem krycia.
- **Porównanie z pomiarem** — wynik można porównać z ostatnim zmierzonym SNR i RSSI.
- **Eksport** do PDF, CSV, KML, GeoJSON i PNG.

Planer otwiera przycisk **Planer** w szczegółach węzła, obok kompasu.

## Diagnostyka na żądanie

W szczegółach węzła (Administracja → „Diagnostyka na żądanie”) możesz zapytać dowolny węzeł w zasięgu o jego bieżące statystyki, zamiast czekać na okresowe rozgłoszenia. Jest 10 rodzajów zapytań, m.in. statystyki węzła (bateria, czas pracy, procesor, pamięć, WiFi), ping z RSSI i SNR, lista węzłów online, historia błędów routingu, liczniki portów, aktywność eteru i wersja firmware MT_SW. Ekran ma przegląd możliwości z licznikiem sprawdzonych zapytań, a wyniki są kolorowe (kafelki, wskaźniki, wykresy). Wszystkie odpowiedzi można udostępnić albo zapisać jako PDF, HTML, Markdown, tekst lub CSV. Ta funkcja wymaga firmware z modułem OnDemand.

## Sniffer

Ekran **Sniffer** (Ustawienia → Advanced) pokazuje na żywo ruch w sieci, także pakiety, których telefon nie potrafi rozszyfrować.

- **Dwa źródła w jednym ekranie** — Wyłączony, Radio albo MQTT (jedno naraz). Każde włączenie pyta o potwierdzenie i ostrzega, co może się zmienić. Sniffer MQTT działa też na komputerze.
- **Radio** — pokazuje wszystko, co usłyszał podłączony węzeł, także własny ruch. Wymaga firmware MT_SW z modułem snifera; bez niego przełącznik jest wyszarzony.
- **Log zostaje** po wyłączeniu snifera, aż sam go wyczyścisz koszem. Licznik w pasku pokazuje zapełnienie bufora (np. „1530/5000”).
- **Grupowanie duplikatów** — ten sam pakiet usłyszany wiele razy to jeden wiersz z listą bramek albo przekaźników. Kopiowanie pakietu kopiuje wszystkie kopie.
- **Automatyczne przewijanie** i **próba rozszyfrowania** znanymi kluczami kanałów — osobne przełączniki.
- **Zapis i wczytywanie logu** (txt, JSON, CSV), a po kliknięciu pakietu pełny podgląd jego treści.
- Karty pozycji i telemetrii pokazują czas zapisany przez nadawcę; niemożliwy czas jest oznaczony jako błędny.

## Panel debugowania

Panel debugowania (Ustawienia → Zaawansowane → Panel debugowania) ma w tej aplikacji dodatkową zakładkę. Zobacz też [Logi debugowania](debug-logs).

- **Logi urządzenia** — trzecia zakładka pokazuje na żywo logi wysyłane przez radio, te same, które widać na porcie szeregowym. Jest wyszukiwanie, kolory poziomów logów i eksport. Żeby logi się pojawiły, włącz „Logi debugowania” w ustawieniach zabezpieczeń urządzenia. Przez Bluetooth działa to z każdym firmware, a przez Wi-Fi i USB tylko z firmware MT_SW.
- Panel pokazuje też pakiety, których telefon nie potrafił rozszyfrować, a surowa treść jest dostępna jako hex.

## Komunikator

- **Zdjęcia przez link** — aplikacja nie wysyła zdjęcia przez LoRa, tylko wysyła link; odbiorca widzi podgląd. Przed wysłaniem jest ostrzeżenie, że serwer jest publiczny. Podgląd obrazków z linków jest domyślnie wyłączony i włącza się w Ustawienia → Prywatność. Zdjęcie na pełnym ekranie można zapisać na urządzeniu.
- **Długie wiadomości** — tekst, który nie mieści się w jednym pakiecie, jest dzielony na części z znacznikiem w rodzaju `[k9 2/4]` i składany po drugiej stronie.
- **Szybki czat** — przy pierwszym uruchomieniu ma gotowy zestaw szablonów.
- **Kolory statusów** — dostarczenie do sieci i potwierdzenie odbioru są złote, samo „dostarczono do sieci” na wiadomości prywatnej czerwone, a błąd wysyłania fioletowy.
- **Lista rozmów** — otwiera się szybciej i płynniej także przy dużej liczbie wiadomości.
- **Komputer** — Enter to nowa linia, Ctrl+Enter wysyła wiadomość.

## Lista węzłów i baza

- **Wyszukiwanie** — pole nad listą od razu filtruje tę samą listę, bez drugiego okna.
- **Pomoc listy węzłów** — sekcje o rolach, bezpieczeństwie, połączeniu i poziomie baterii z legendą ikon i kolorów.
- **Role węzłów po polsku** — nazwy i opisy ról urządzeń są przetłumaczone, a nieaktualne role są ukryte na liście wyboru.
- **Aktualny czas kontaktu** — węzły ze stałą pozycją pokazują prawdziwy czas ostatniego kontaktu.
- **Czyszczenie bazy węzłów** — na ekranie „Wyczyść bazę węzłów” można włączyć automatyczne usuwanie nieaktywnych węzłów (próg 1–90 dni, sprawdzanie co 1–30 dni), działa też na komputerze. Są przełączniki „Ignoruj datę” i „Tylko węzły z niezgodnym kluczem”.
- **Niesłyszane węzły** — węzeł, od którego właśnie odebrano pakiet przez radio, nie jest już oznaczony jako niesłyszany.

## Ustawienia i wygląd

- **Skalowanie aplikacji** — w ustawieniach aplikacji wybierasz rozmiar tekstu i elementów od 50% do 200%, niezależnie od ustawień telefonu. Domyślnie „Zgodnie z telefonem”. Działa też na komputerze.
- **Ustawienia LoRa** — pole „Przesunięcie częstotliwości (kHz)”, coding rate 4/5 dla presetów Narrow, pełna rozdzielczość częstotliwości nadpisanej, opis ostrzegawczy przy „Nadpisz cykl pracy” oraz przywrócony ekran Traffic Management.
- **Bezpieczeństwo** — kopia kluczy także na komputerze oraz wybór koloru węzła przy generowaniu klucza.
- **Wgrywanie firmware** — własny plik można wgrać także na urządzenie spoza listy, z ostrzeżeniami o ryzyku; odzyskiwanie urządzenia w trybie DFU działa też po własnym pliku.
- **Bluetooth** — poprawione ponowne łączenie z już sparowanym urządzeniem, wykrywanie węzła na Windowsie, a lista ostatnich urządzeń sieciowych pamięta 20 wpisów.
- **Powiadomienia o nowych wersjach** — aplikacja sprawdza na GitHubie, czy wyszło nowsze wydanie, i pokazuje powiadomienie z linkiem.
- **Zgłaszanie błędów i pomysłów** — pozycja „Zgłoś błąd lub pomysł” w ustawieniach prowadzi do naszego Discorda i grup.
- **Wygląd i marka** — własna nazwa, ikona i kolory (złoto i granat), kolorowa ikona stanu połączenia, instalacja obok oryginalnej aplikacji Meshtastic. Na Windowsie nową wersję instaluje się na starą bez odinstalowywania.
- **Komputer** — przeciągany pasek przewijania z widocznym torem na długich listach, trzecia kolumna podąża za wybranym węzłem, pozycja „Wprowadzenie” w ustawieniach.

## Pomoc i dokumentacja

Cała pomoc i dokumentacja w aplikacji są po polsku. Wielkość tekstu na stronach pomocy zmienisz przyciskami „A−” i „A+” u góry. Przy pierwszym uruchomieniu, po ekranie powitalnym, jest druga karta z krótkim opisem funkcji MT_SW; można ją zobaczyć ponownie w ustawieniach w pozycji „Wprowadzenie”. Zobacz [Pomoc i dokumentacja](help-and-docs).

## Powiązane tematy

- [Węzły](nodes) — lista węzłów i szczegóły węzła
- [Mapa i punkty](map-and-waypoints) — mapa, warstwy i punkty
- [Metryki węzła](node-metrics) — wykresy telemetrii, na których opiera się Zdrowie sieci
- [Pomoc i dokumentacja](help-and-docs) — jak korzystać z pomocy w aplikacji
