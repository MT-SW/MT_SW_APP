---
title: Funkcje MT_SW
parent: Przewodnik użytkownika
nav_order: 23
last_updated: 2026-10-24
description: Przegląd tego, co MT_SW_APP dodaje do oryginalnej aplikacji Meshtastic — zdalne sterowanie GPIO, zdrowie sieci, połączenia na mapie, Mesh Link Planer, kalkulator anten, diagnostyka na żądanie, Sniffer, dodatkowa telemetria, panel debugowania z logami urządzenia, skalowanie aplikacji i więcej.
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

> ℹ️ **Uwaga:** Część funkcji (Diagnostyka na żądanie, Sniffer radiowy, dodatkowa telemetria, zasoby węzła, logi urządzenia przez Wi-Fi i USB) wymaga firmware MT_SW. Na zwykłym firmware aplikacja działa normalnie, ale te funkcje nie dostaną odpowiedzi.

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
- **Eksport** do PDF, CSV, KML, GeoJSON, JSON (wszystkie dane raportu: sekcje, profil terenu, punkty i komórki zasięgu) i PNG.

Planer otwiera przycisk **Planer** w szczegółach węzła, obok kompasu.

## Kalkulator anten

**Ustawienia → Zaawansowane → Funkcje MT_SW → Narzędzia → Kalkulator anten.** Liczy wymiary anteny dla dowolnej częstotliwości i od razu pokazuje jej rysunek z odcinkami A, B, C… i ich wartościami.

- **Typy anten:** ćwierćfalowa ground plane, dipol półfalowy, dipol pętlowy (z balunem λ/2 z kabla), pionowa 5/8 fali, J-pole, Slim Jim, kolinearna z kabla (COCO), Yagi-Uda (3–20 elementów, z poprawką na metalowy nośnik), Moxon, Biquad, helisa osiowa, antena płaska (patch na płytce) i odcinki kabla λ/4 i λ/2.
- **Materiał:** średnica drutu lub rurki i izolacja zmieniają długość elementów; przy antenach z kabla liczy się współczynnik skrócenia wybranego kabla, przy patchu przenikalność i grubość płytki.
- **Właściwości:** długość fali, użyty współczynnik skrócenia, impedancja, zysk, polaryzacja i wskazówki wykonania.
- **Tor antenowy:** moc nadajnika (domyślnie wzięta z Twojego radia, można ją zmienić albo wybrać limit pasma), kabel z listy (tłumienie liczone dla wybranej częstotliwości) albo własny, długość kabla, złącza i inne elementy, SWR i zysk anteny. Wynik: straty każdego elementu i razem, moc na antenie, EIRP i ERP, porównanie z limitem pasma (np. 500 mW ERP w 869,4–869,65 MHz) i wpływ strat na zasięg.

Wzory pochodzą ze sprawdzonych źródeł (Stutzman i Thiele, Rothammel/DL6WU, L. B. Cebik, J. D. Kraus, Balanis, Hammerstad, tabele producentów kabli). Wynik to bardzo dobry punkt wyjścia, ale każdą antenę i tak dostraja się miernikiem SWR albo NanoVNA. Asystent pod swoimi wyliczeniami anten daje przycisk do kalkulatora z tą samą częstotliwością i typem anteny.

Przycisk **Zapisz wynik…** na dole zapisuje dane wejściowe, wymiary, właściwości i wyniki toru antenowego jako PDF (raport tekstowy, bez rysunku), Markdown, TXT, CSV (sama tabela wymiarów) albo JSON.

Tor antenowy liczy się tak samo jak w Mesh Link Planerze: dowolna liczba złączy połączonych odcinkami kabla, z bazą kabli z kart katalogowych (tłumienie dla Twojej częstotliwości), gotowe zestawy i zestawienie strat każdej części. W sekcji **Moje anteny** zapisujesz policzoną antenę razem z torem, mocą, SWR i zyskiem pod własną nazwą, a później ją wczytujesz albo usuwasz. W Mesh Link Planerze, w sekcji nadajnika i anteny, możesz wybrać zapisaną antenę (ustawia moc, zysk i tor jednym dotknięciem) albo przycisk **Policz dokładnie w kalkulatorze**, który otwiera kalkulator na częstotliwości i mocy z planera. Pod wynikami kalkulator mówi, na której części toru tracisz najwięcej, ile dałoby skrócenie kabla, i potrafi porównać aktualny tor z zapisaną anteną (straty, EIRP, zasięg). Przycisk **Użyj w planerze** (z wyborem punktu A albo B) otwiera Mesh Link Planer z tą anteną — mocą, zyskiem i torem — ale tylko wtedy, gdy sam go klikniesz; nic nie przechodzi do planera samo.

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

## Dodatkowa telemetria (statystyki lokalne)

W **Ustawienia → Moduły → Telemetria** jest karta **Statystyki lokalne**. Działa od MT_SW_FIRMWARE v3; na starszej karta jest widoczna, ale nic w niej nie reaguje, a opis mówi, od której wersji to działa. Ma dwa przełączniki — **Dodatkowa telemetria** i **Rozszerzona dodatkowa telemetria** — i osobny interwał dla każdego. Pod każdym przełącznikiem jest opis, co trafia do pakietu: w dodatkowej telemetrii liczba węzłów, ruch, obciążenie kanału, czas pracy i poziom szumu, a w rozszerzonej zajęcie procesora i pamięci (RAM — heap, flash, PSRAM, w procentach) oraz siła sygnału Wi‑Fi.

- **Najkrótszy interwał to 2 godziny.**
- **Obie opcje zapisują się razem** zwykłym dolnym paskiem zapisu, razem z pozostałymi ustawieniami telemetrii. Aplikacja najpierw odczytuje obecny stan z węzła, więc zmiana jednej opcji nie rusza drugiej. Po zapisie pokazuje to, co węzeł naprawdę zapisał (może poprawić interwały); jeśli nie potwierdzi, dostajesz komunikat.
- Zapis nie wymaga restartu węzła.

## Panel debugowania

Panel debugowania (Ustawienia → Zaawansowane → Panel debugowania) ma w tej aplikacji dodatkową zakładkę. Zobacz też [Logi debugowania](debug-logs).

- **Zapisywanie logów** — w Funkcjach MT_SW, w sekcji **Logi**, jest przełącznik **Zbieranie logów**. Domyślnie jest wyłączony; po włączeniu zostaje włączony na stałe, także po ponownym uruchomieniu i po awarii, a aplikacja sama, bez żadnych przycisków, zapisuje na bieżąco wszystko, co loguje, od startu po zamknięcie: cykl życia, połączenie z radiem, asystenta, pamięć i obciążenie. Treści wiadomości, kluczy i pozycji nie zapisuje. Log składa się z kolejnych części po ok. 400 KB, każda z własnym nagłówkiem, więc można ją czytać osobno; najstarsze części są usuwane dopiero po przekroczeniu dużego łącznego limitu. Na telefonie logi same trafiają do folderu **Pobrane/MT_SW_logi** jako jeden plik zip ze spisem treści (odświeżany przy starcie, co kilka minut podczas pracy i przy przejściu aplikacji w tło), więc są dostępne także po awarii aplikacji; na komputerze leżą w folderze `.meshtastic/logs` w katalogu domowym, a obok przycisku jest otwarcie tego folderu. Niezależnie od przełącznika aplikacja zawsze zapisuje samą przyczynę awarii (ślad błędu i stan pamięci, bez żadnych innych danych), a na telefonie po awarii trafia ona do tego samego folderu. Gdy asystent nie może użyć układu graficznego, w logu jest też krótki raport o komputerze (system, Java, pamięć, karty graficzne).
- **Sąsiedzi na liście węzłów** — przy nazwie węzła (od lewej) jest mała szara ikona sieci z cyfrą: liczbą sąsiadów z ostatniej zapisanej listy tego węzła (pojawia się, gdy taka lista jest zapisana). Dotknięcie otwiera szczegóły sąsiadów.
- **Baza wiedzy (przeglądarka)** — w Funkcjach MT_SW jest pozycja „Baza wiedzy”: każdy może przeglądać całą bazę offline, bez asystenta. Jest wyszukiwanie (bez znaczenia polskich znaków), filtr poziomu (Wszystkie / Główne / Pokrewne / Offtop) i lista wpisów; dotknięcie wpisu rozwija treść i źródło. To te same pliki, z których korzysta asystent.
- **Baza wiedzy** — asystent ma własną, ogólną i stale rozbudowywaną bazę wiedzy, nie tylko o Meshtastic: elektronika, radio i propagacja fal, informatyka i systemy, druk 3D, praca na wysokości, zakupy i cła, życie codzienne, pierwsza pomoc, turystyka oraz sprzęt (także do zbudowania samemu). Z tematów sieci obejmuje: anteny i dopasowanie, filtry i wzmacniacze, role i ustawienia radia, presety LoRa, kanały i szyfrowanie, MQTT, moduły, wgrywanie i aktualizacja firmware (ESP32, nRF52), kompilacja, `userPrefs`, zasilanie i solary, budowa własnych urządzeń, Home Assistant i dlaczego sieć LoRa nie może być zbyt duża. Baza ma trzy poziomy: główny (nasza sieć i Meshtastic), „pokrewne” (IT, elektronika, Wi-Fi, krótkofalarstwo) i „offtop” (reszta życia). Każdy wpis ma podane źródło (adres i datę sprawdzenia). Na pytanie techniczne asystent sam dobiera pasujące wpisy, myśli nad nimi i odpowiada własnymi słowami; gdy bazie czegoś brakuje, mówi, że tego nie wie. Przy włączonym internecie asystenta wyniki z internetu są pokazane osobno, a sprzeczności z bazą zaznaczone. Tematy spoza sieci i radia (np. zdrowie, kemping, woda) leżą w osobnym katalogu `knowledge/base/offtop`. Baza to zwykłe pliki tekstowe `knowledge/base/*.md` w repozytorium (wpis to nagłówek `###` z pytaniem, treść, wiersz `Słowa:` z ukrytymi słowami kluczowymi i wiersz `Źródło:`), więc można je poprawiać i uzupełniać bez programowania.

- **Logi urządzenia** — trzecia zakładka pokazuje na żywo logi wysyłane przez radio, te same, które widać na porcie szeregowym. Jest wyszukiwanie, kolory poziomów logów i eksport. Żeby logi się pojawiły, włącz „Logi debugowania” w ustawieniach zabezpieczeń urządzenia. Przez Bluetooth działa to z każdym firmware, a przez Wi-Fi i USB tylko z firmware MT_SW.
- Panel pokazuje też pakiety, których telefon nie potrafił rozszyfrować, a surowa treść jest dostępna jako hex.

## Komunikator

- **Zdjęcia przez link** — aplikacja nie wysyła zdjęcia przez LoRa, tylko wysyła link; odbiorca widzi podgląd. Przed wysłaniem jest ostrzeżenie, że serwer jest publiczny. Podgląd obrazków z linków jest domyślnie wyłączony i włącza się w Ustawienia → Prywatność. Zdjęcie na pełnym ekranie można zapisać na urządzeniu.
- **Długie wiadomości** — tekst, który nie mieści się w jednym pakiecie, jest dzielony na części z znacznikiem w rodzaju `[k9 2/4]` i składany po drugiej stronie.
- **Szybki czat** — przy pierwszym uruchomieniu ma gotowy zestaw szablonów.
- **Kolory statusów** — dostarczenie do sieci i potwierdzenie odbioru są złote, samo „dostarczono do sieci” na wiadomości prywatnej czerwone, a błąd wysyłania fioletowy.
- **Lista rozmów** — otwiera się szybciej i płynniej także przy dużej liczbie wiadomości.
- **Komputer** — Enter to nowa linia, Ctrl+Enter wysyła wiadomość.

## Tłumacz i asystent offline

Aplikacja ma dwie opcjonalne funkcje AI, które działają **w całości na Twoim urządzeniu, bez internetu**: tłumacz i asystent. Obie są **domyślnie wyłączone** i włączasz je osobno, w Ustawienia → Zaawansowane → Funkcje MT_SW (na samej górze, nad aktualizacją oprogramowania) → „Tłumacz i asystent offline”. Możesz mieć włączonego tylko tłumacza, tylko asystenta albo oba.

**Prywatność.** Wszystko jest przetwarzane lokalnie. Po pobraniu modelu żadne teksty ani rozmowy nie są wysyłane do internetu. Internet jest potrzebny tylko do jednorazowego pobrania modelu.

**Modele.** Na liście są cztery modele: „Tylko tłumacz” oraz trzy poziomy asystenta (lekki, standardowy, mocny). Gemma 4 działa na silniku Google LiteRT-LM i używa układu graficznego (GPU), gdy urządzenie go obsługuje; w przeciwnym razie liczy procesor. Możesz mieć pobranych kilka naraz, każdy pobierasz i usuwasz osobno, a w jednym miejscu, „Model AI (asystent, tłumacz i dyktowanie)”, wybierasz jeden model do wszystkiego. W pamięci jest zawsze tylko ten jeden model. NLLB („Tylko tłumacz”) umie tylko tłumaczyć: gdy wybierzesz go przy włączonym asystencie, aplikacja ostrzeże, że to wyłączy asystenta i dyktowanie, i dopiero po potwierdzeniu wyłączy asystenta.

- **Tylko tłumacz — NLLB-200 600M.** Tłumaczy, ale nie ma asystenta ani dyktowania. Pobranie ok. 870 MB, pamięć RAM ok. 2,3 GB, zalecany telefon od 4 GB RAM.
- **Lekki — Gemma 4 E2B.** Tłumacz i asystent w jednym pliku. Pobranie ok. 2,6 GB, RAM ok. 3,5 GB, telefon od 6 GB RAM.
- **Standardowy — Gemma 4 E4B.** Tłumacz i asystent, lepsza jakość. Pobranie ok. 3,7 GB, RAM ok. 5 GB, urządzenie od 8 GB RAM. Nie tłumaczy nazw własnych, numerów ani adresów.
- **Mocny — Gemma 4 12B.** Tłumacz i asystent, najlepsze odpowiedzi asystenta. Pobranie ok. 6,9 GB, RAM ok. 9,5 GB, urządzenie od 16 GB RAM.

Na słabszych urządzeniach duże modele mogą spowalniać aplikację albo ją zamykać, dlatego przy każdym modelu są podane rozmiary i zalecana ilość pamięci.

**Karta na dolnym pasku.** Gdy włączony jest którykolwiek tryb, na dolnym pasku pojawia się karta „Tłumacz”. Gdy włączysz też asystenta, karta nazywa się „AI” i ma ikonę iskierek, a w środku są dwie osobne zakładki: tłumacz i asystent. Asystent działa tylko na tej karcie, a w kanałach i wiadomościach prywatnych jest wyłącznie tłumacz. Asystent wymaga modelu Gemma 4; sam NLLB tylko tłumaczy.

**Jak działa asystent.** Rozmowa wygląda jak w komunikatorze: Twoje wiadomości są po prawej, odpowiedzi po lewej, a na początku możesz stuknąć jedną z podpowiedzi. Przytrzymaj dymek, żeby skopiować tekst albo zadać to samo pytanie jeszcze raz; gdy odpowiedź się nie uda, pytanie wraca do pola, żeby je wysłać ponownie. Rozmowa może trwać bez końca: starsze wypowiedzi asystent zapamiętuje w krótkim streszczeniu, a odpowiedzi nie są ucinane. Piszesz pytanie, a odpowiedź pojawia się na bieżąco; przyciskiem „Zatrzymaj” przerwiesz ją w każdej chwili, a „Nowa rozmowa” zaczyna od nowa. Pierwsza odpowiedź (i pierwsze tłumaczenie) trwa dłużej, bo model jest wczytywany do pamięci. **Dane z aplikacji:** asystent może tylko *czytać* dane — własny węzeł, każdy znany węzeł sieci (wystarczy podać jego nazwę w pytaniu) wraz z całą zapisaną historią i tendencją (np. pamięć RAM/heap radia, procesor, bateria, zajętość kanału, sygnał), podsumowanie całej sieci, ustawienia radia (bez kluczy i haseł) i ostatnie wiadomości. Niczego sam nie przestawia i nie wysyła wiadomości. Pytaj np. „Jak zmienia się pamięć RAM?”. Pytania o skrajne wartości liczy aplikacja, nie model: „Który węzeł ma najniższe napięcie?”, „Który najdłużej pracuje?”, „Średnia temperatura”, „Szybkie podsumowanie węzłów” — dla napięcia, baterii, czasu pracy, obciążenia kanału, CPU, RAM, flash, PSRAM, Wi‑Fi, ruchu, sygnału, skoków, odległości, temperatury i innych. Odpowiedzi mogą być błędne — ważne rzeczy sprawdzaj. Rozmowy zapisują się **tylko na tym urządzeniu** i nigdzie nie są wysyłane. Ikona zegara w rogu otwiera historię: stuknij rozmowę, by ją kontynuować, albo ją usuń. „Nowa rozmowa” zaczyna od zera, a poprzednia zostaje w historii.

**Funkcja eksperymentalna.** Tłumacz i asystent są funkcjami eksperymentalnymi. Nie gwarantujemy poprawności ich działania ani odpowiedzi: mały model działający na urządzeniu może się pomylić, dlatego ważne informacje zawsze sprawdzaj.

**Jak asystent rozumie pytania.** Najpierw model krótko rozpoznaje, o co pytasz (np. liczba wiadomości, węzeł na mapie, ustawienie, pomoc z aplikacją, zwykła rozmowa), przy czym może wybrać tylko jedną ze znanych kategorii. Potem: dokładne rzeczy (liczby, wyszukiwanie, listy węzłów, trasy pakietów, stan połączenia, odczyty czujników) układa program w języku aplikacji, więc liczby się nie mylą; na pytania o aplikację asystent pokazuje, gdzie jest dane ustawienie, i cytuje pasujący fragment tej instrukcji (a „co nowego” — najnowsze wpisy z listy zmian), a na pytania o Twoją sieć na podstawie dobranych do pytania danych. Na każdy inny temat możesz z asystentem swobodnie rozmawiać. Zanim napisze odpowiedź, model chwilę po cichu się zastanawia: odpowiedzi są lepsze, choć trwa to kilka sekund dłużej.

**Asystent sam przechodzi tam, gdzie prosisz.** Gdy napiszesz np. „pokaż węzeł Łysica na mapie”, „pokaż najbliższy router”, „otwórz ustawienia LoRa”, „pokaż limit przeskoków” albo „czy mam zasięg do …”, aplikacja od razu otwiera mapę z zaznaczonym węzłem, właściwy ekran (ustawienie mruga na żółto) albo planer łącza. Prośba „napisz na kanale …, że …” otwiera rozmowę z gotowym tekstem w polu wiadomości, a **wysyłasz go sam**. Asystent nigdy nie zmienia ustawień i nigdy nie wysyła wiadomości. Pod odpowiedzią zostaje przycisk, żeby wrócić w to miejsce później.

**Test asystenta.** Wpisz w asystencie `/test` i wyślij: asystent sam zada sobie około 50 pytań testowych (z prawdziwą nazwą węzła i kanału z Twojej sieci) i pokaże pytania z odpowiedziami w rozmowie, a na końcu pełny raport z czasem odpowiedzi i użytym modelem, który można skopiować (przytrzymaj dymek). Na komputerze raport zapisuje się też w pliku `assistant-test-results.md`. Dzięki temu można porównać jakość odpowiedzi na różnych urządzeniach i modelach. W trakcie testu nic nie otwiera się samo; przerwiesz go przyciskiem „Zatrzymaj”.

**Kopiowanie rozmowy.** Nad rozmową z asystentem jest przycisk „Kopiuj całą rozmowę”: kopiuje wszystkie pytania i odpowiedzi (także nazwy przycisków przekierowań) do schowka, np. żeby wkleić je w wiadomości przy zgłaszaniu błędu. Obok jest „Zapisz rozmowę…”: zapisuje rozmowę do pliku w wybranym formacie — PDF (wygląda jak czat, najlepszy do pokazania komuś całości), Markdown, TXT, HTML albo JSON (dane w uporządkowanej formie, najłatwiejsze do czytania przez program). Pojedynczy dymek kopiujesz, przytrzymując go.

**Obliczenia.** Liczby asystent nie zgaduje: liczy je aplikacja. Zapytaj dowolnymi słowami, np. „ile cm ma dipol na 433 MHz”, „ile to 27 dBm w mW”, „jakie EIRP przy 22 dBm i antenie 6 dBi”, „jaka strefa Fresnela na 15 km”, „jak daleko sięga radio z masztu 40 m”, „jaki teoretyczny zasięg na Narrow Fast”, „ile trwa wysłanie wiadomości 100 znaków” albo „policz 12*7+3”. Gdy nie podasz częstotliwości, presetu albo mocy, asystent bierze je z Twojego radia albo z naszej sieci i to zaznacza. Na pytanie o słaby zasięg sprawdza ustawienia i odbiór Twojego radia i podpowiada, co poprawić.

**Tłumacz:**

- **Odebrane wiadomości** — w menu wiadomości jest „Przetłumacz wiadomość”. Oryginał zostaje, a tłumaczenie można przełączać z oryginałem.
- **Wysyłane wiadomości** — gdy w polu jest tekst, obok pola pojawia się przycisk „Przetłumacz przed wysłaniem”. Pokazuje oryginał i tłumaczenie obok siebie; możesz wysłać tłumaczenie, wstawić je do pola i poprawić albo anulować.
- **Osobny ekran** — karta „Tłumacz”: wpisujesz tekst, wybierasz języki (albo „Wykryj automatycznie”), tłumaczysz i kopiujesz wynik.
- Jeden model obsługuje wiele języków i dowolne pary między nimi, m.in. polski, angielski, niemiecki, czeski, słowacki, ukraiński, rosyjski, francuski i hiszpański.
- Tłumaczenie bywa niedokładne, zwłaszcza nazwy własne i żargon, więc zawsze traktuj je jako podpowiedź. Tekst jest tłumaczony zdanie po zdaniu. Jeśli tłumaczenie ma ponad 200 znaków, aplikacja ostrzega, że przez radio pójdzie w kilku częściach.

**Przyciski, wiedza o aplikacji i głos (asystent).** Asystent zna aplikację: ma wbudowaną wiedzę z tej dokumentacji i listy zmian i do każdego pytania dobiera tylko pasujący fragment. Pod odpowiedzią może pokazać **przyciski**: otwarcie ekranu, pokazanie ustawienia (żółte mruganie przez kilka sekund), pokazanie węzła na mapie, otwarcie planera łącza albo szkic wiadomości wpisany do pola. Gdy wprost prosisz o pokazanie lub otwarcie czegoś, aplikacja otwiera to od razu; w pozostałych przypadkach przycisk czeka na Twoje dotknięcie. Szkic wiadomości **nigdy nie jest wysyłany automatycznie**. Przytrzymaj odpowiedź, aby ją **odczytać na głos**; na telefonie przy polu pytania jest też przycisk „Dyktuj”. Nad polem czatu, gdy jest tekst do przetłumaczenia, pojawia się mały szary przycisk „Przetłumacz”.

**Internet dla asystenta (domyślnie wyłączony).** W ustawieniach asystenta jest jeden przełącznik internetu. Wyłączony = asystent działa w pełni offline. Włączony (po przeczytaniu ostrzeżenia, z czerwonym napisem, że jest włączony): asystent może wyszukać ogólne informacje w internecie, a aplikacja raz na dobę sprawdza w naszym repozytorium nowości o aplikacji (pobrane pliki są weryfikowane skrótem; gdy się nie uda, działa wiedza wbudowana). Wyszukiwarka widzi Twój adres IP i słowa zapytania. Tekst z internetu jest traktowany tylko jako informacja, nigdy jako polecenie, a pod odpowiedzią, która korzystała z internetu, pojawia się mała notka w kolorze aplikacji: „Odpowiedź z pomocą internetu”, co wyszukano i jakie były źródła. O tym, czy pytanie wymaga szukania w internecie, decyduje rozpoznanie pytania, a samo wyszukiwanie wykonuje aplikacja.

**Liczby i filtry (asystent).** Asystent potrafi policzyć wiadomości na kanale (dzisiaj od północy albo z ostatnich godzin, z podaniem kto pisze najwięcej), przefiltrować węzły (aktywne w ostatnich godzinach, ulubione, po sprzęcie, z niską baterią, z pozycją) przeczytać wiadomości prywatne z wybranym węzłem pokazać trasy pakietów (traceroute) i sąsiadów (kto kogo słyszy), stan połączenia, nieprzeczytane wiadomości, to czy Twoje wiadomości dotarły oraz waypointy. Okres możesz podać dowolnie (minuty, godziny, dni, tydzień, miesiąc, rok, dzisiaj, wczoraj). Takie odpowiedzi układa program w języku aplikacji. Liczby wylicza program, nie model, więc się nie mylą.

**Wyszukiwanie w wiadomościach (asystent).** Asystent znajdzie słowo lub frazę (np. w cudzysłowie) w wiadomościach na wszystkich kanałach, na jednym kanale albo w rozmowie prywatnej z wybranym węzłem; nie przeszkadza wielkość liter ani polskie znaki. Podaje liczbę trafień i na którym kanale padły.

**Pamięć asystenta (domyślnie wyłączona).** Asystent zapisuje sobie krótkie notatki o Tobie (kilka małych plików: kim jesteś, Twój sprzęt i sieć, jak lubisz rozmawiać, nad czym pracujesz) i pamięta je między rozmowami, dzięki czemu rozmowa jest bardziej naturalna. Notatki zostają tylko na urządzeniu. W ustawieniach asystenta możesz je zobaczyć, poprawić i usunąć, wyłączyć pamięć przełącznikiem oraz wpisać **własne instrukcje** (np. jak ma do Ciebie mówić), których asystent zawsze przestrzega. Nad rozmową asystent przypomina, że zapisuje notatki. Z włączoną pamięcią notatki pomagają też rozumieć pytania (np. który węzeł jest Twój w domu) i zapisują się także wtedy, gdy powiesz coś o sobie. W trakcie jednej rozmowy asystent pamięta całą rozmowę (starsze wymiany streszcza na bieżąco, najnowsze zna dosłownie) i ostatnio omawiany węzeł, więc możesz dopytać „a jaka jest tam bateria?” — to działa także przy wyłączonej pamięci.

**Czytanie i tłumaczenie całej rozmowy.** W menu z trzema kropkami w kanale lub rozmowie prywatnej są pozycje: „Czytaj widoczne wiadomości”, „Czytaj wszystkie wiadomości”, „Przetłumacz widoczne wiadomości” i „Przetłumacz wszystkie wiadomości” (oraz „Zatrzymaj czytanie” w trakcie czytania). „Widoczne” to te, które masz na ekranie; „wszystkie” to do 500 najnowszych. Własne wiadomości nie są tłumaczone, a przetłumaczone teksty można jak zwykle przełączać z powrotem na oryginał.

**Przycisk dodawania i udostępniania w górnej belce.** Domyślnie okrągły przycisk (import, udostępnianie, dodanie kontaktu) leży nad listą węzłów i rozmów. W „Funkcje MT_SW” możesz go przenieść do górnej belki, obok ikony informacji, żeby nie zasłaniał listy; wyłączenie przełącznika przywraca standardowe miejsce.

**Tłumacz – czytanie.** Przy przetłumaczonym tekście jest przycisk „Czytaj na głos” (i „Zatrzymaj czytanie”) obok kopiowania.

**Dyktowanie.** Przycisk „Dyktuj” w asystencie działa tak samo na telefonie i na komputerze, bez usług systemu: nagrywa mikrofon, a pobrany model Gemma (E2B lub E4B) zamienia mowę na tekst w całości na urządzeniu. Dotknij „Dyktuj”, mów (do około 30 sekund) i dotknij „Zatrzymaj i wstaw”. Telefon zapyta o zgodę na mikrofon przy pierwszym użyciu. Przycisk pojawia się dopiero, gdy pobrany jest model E2B lub E4B. Mikrofon (małe żółte kółko) jest też przy przycisku wysyłania w rozmowach (kanały i prywatne) oraz w tłumaczu. Pojawia się tylko wtedy, gdy asystent jest włączony i ma wybrany model, który słyszy (Gemma E2B lub E4B). W rozmowach i w asystencie do dyktowania służy model wybrany dla asystenta (żaden drugi model nie jest uruchamiany). W tłumaczu dyktowanie jest tylko wtedy, gdy model wybrany dla tłumacza sam słyszy (E2B lub E4B); przy najmniejszym modelu tłumacza (NLLB) przycisku nie ma. Opis jest też w ustawieniach AI. Przełącznik „Mikrofon słuchawki przy dyktowaniu” sprawia, że gdy podłączona jest słuchawka (Bluetooth, np. samochodowa, przewodowa lub USB), dyktowanie w całej aplikacji nagrywa z jej mikrofonu; bez słuchawki działa zwykły mikrofon.

**Jak długo pamiętać odebrane ramki.** W Ustawieniach (także w „Funkcjach MT_SW”) jest pole z liczbą dni (1, 2, 3, 7, 14, 30 lub 60), przez które aplikacja zachowuje odebrane ramki, z których liczone są statystyki i wykresy. Domyślnie jest to 60 dni. Starsze ramki są usuwane. Historia każdego urządzenia jest trzymana osobno.

**Wybór koloru węzła.** Okno wyboru koloru klucza pozwala wybrać dowolny kolor (kod, suwaki odcienia, nasycenia i jasności albo gotowe kolory oraz ostatnio użyte), wybrać tryb „Szybko” (do 20 s, kolor podobny) lub „Dokładnie” (do 3 min, kolor bardzo bliski), widzieć czas szukania, przerwać szukanie i porównać wybrany kolor z wynikiem. Klucz nie jest zapisywany automatycznie.

**Licencje modeli.** Modele nie są częścią aplikacji: pobierasz je z internetu i używasz na własną odpowiedzialność, zgodnie z ich licencjami.

- NLLB-200 (Meta) jest udostępniony na licencji **CC BY-NC 4.0**, czyli **wyłącznie do użytku niekomercyjnego**.
- Gemma 4 (Google) jest udostępniona na licencji **Apache 2.0**.
- Odpowiedzi modeli mogą być błędne lub zmyślone. Nie opieraj na nich decyzji dotyczących bezpieczeństwa.

## Lista węzłów i baza

- **Wyszukiwanie** — pole nad listą od razu filtruje tę samą listę, bez drugiego okna.
- **Pomoc listy węzłów** — sekcje o rolach, bezpieczeństwie, połączeniu i poziomie baterii z legendą ikon i kolorów.
- **Role węzłów po polsku** — nazwy i opisy ról urządzeń są przetłumaczone, a nieaktualne role są ukryte na liście wyboru.
- **Aktualny czas kontaktu** — węzły ze stałą pozycją pokazują prawdziwy czas ostatniego kontaktu.
- **Czyszczenie bazy węzłów** — na ekranie „Wyczyść bazę węzłów” można włączyć automatyczne usuwanie nieaktywnych węzłów (próg 1–90 dni, sprawdzanie co 1–30 dni), działa też na komputerze. Są przełączniki „Ignoruj datę” i „Tylko węzły z niezgodnym kluczem”.
- **Niesłyszane węzły** — węzeł, od którego właśnie odebrano pakiet przez radio, nie jest już oznaczony jako niesłyszany.

## Ustawienia i wygląd

- **Grupa „Funkcje MT_SW”** — w Ustawieniach jest jeden wpis „Funkcje MT_SW”, który otwiera osobny ekran z naszymi ustawieniami: przełącznik podglądu obrazków z linków w czacie, powiadomienia o nowych wersjach (dwa przełączniki) i układ listy węzłów. Niżej są skróty do ustawień, które mają swoje naturalne miejsce gdzie indziej: skalowanie aplikacji, motyw, kolor węzła (otwiera ustawienia bezpieczeństwa, a przycisk „Wybierz kolor” mruga), LoRa (korekta LNA i przesunięcie częstotliwości), czyszczenie bazy węzłów, Sniffer, statystyki lokalne (dodatkowa telemetria) i panel debugowania. Na końcu wpis „Pozostałe funkcje” opisuje funkcje przy węźle (korekta LNA węzła, odwrócony prąd i zasilanie, zdalne GPIO, diagnostyka na żądanie) i mówi, gdzie ich szukać. Wszystkie pozycje znajdzie też wyszukiwarka ustawień.
- **Skalowanie aplikacji** — w ustawieniach aplikacji wybierasz rozmiar tekstu i elementów od 50% do 200%, niezależnie od ustawień telefonu. Domyślnie „Zgodnie z systemem”. Działa też na komputerze.
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
