---
title: Funkcje MT_SW
parent: Przewodnik użytkownika
nav_order: 23
last_updated: 2026-10-18
description: Przegląd tego, co MT_SW_APP dodaje do oryginalnej aplikacji Meshtastic — zdalne sterowanie GPIO, zdrowie sieci, połączenia na mapie, Mesh Link Planer, diagnostyka na żądanie, Sniffer, skalowanie aplikacji i więcej.
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

MT_SW_APP to aplikacja oparta na oryginalnej aplikacji Meshtastic. Poza wszystkim, co robi oryginał, ma kilka własnych funkcji, które ułatwiają życie w sieci mesh — zwłaszcza w sieci świętokrzyskiej. Ta strona krótko je opisuje i mówi, gdzie ich szukać.

> ℹ️ **Uwaga:** Część funkcji (Diagnostyka na żądanie, zasoby węzła) wymaga firmware z modułem OnDemand. Na zwykłym firmware aplikacja działa normalnie, ale te funkcje nie dostaną odpowiedzi.

## Zdalne zarządzanie

Aplikacja pozwala sterować innymi węzłami przez sieć mesh, bez dotykania urządzenia w terenie. Wszystko jest w szczegółach węzła.

- **Zdalne sterowanie GPIO** — w szczegółach węzła (moduł Remote Hardware) wpisujesz numer pinu, a aplikacja sama liczy maskę bitową i wysyła do zdalnego węzła polecenie włączenia, wyłączenia albo odczytu pinu. Można w ten sposób zdalnie włączać przekaźniki, oświetlenie albo restartować sprzęt podłączony do węzła. Przyciski działają dopiero po wymianie kluczy z węzłem, bo polecenia są szyfrowane. W nagłówku karty GPIO jest ikona informacji z opisem.
- **Zdalne ulubione i ignorowanie** — węzeł możesz dodać do ulubionych albo go ignorować nie tylko na swoim telefonie, ale też w zdalnym węźle, przez sieć LoRa. Aplikacja czeka na prawdziwe potwierdzenie z sieci (routing ACK), więc wiesz, że zmiana dotarła.
- **Szybkie komendy** — przyciski `/ping`, `/hello` i `/test` wysyłają wiadomość prywatną nawet do węzłów, których rola zwykle blokuje ręczne wiadomości. Przydają się do szybkiego sprawdzenia węzła w terenie.
- **Dodawanie nowych urządzeń do bazy** — nowy węzeł możesz dodać ręcznie, wpisując jego ID (np. `!a1b2c3d4`), do bazy własnego węzła albo zdalnie, do bazy innego węzła przez sieć mesh. Format ID jest w całej aplikacji taki sam.

> ⚠️ **Ważne:** Zdalne polecenia zmieniają prawdziwy sprzęt. Upewnij się, że sterujesz właściwym węzłem, zanim wyślesz polecenie GPIO.

## Zdrowie sieci

Zakładka **Zdrowie sieci** jest w dolnej nawigacji, między Węzłami a Mapą. Pokazuje w jednym miejscu, jak się mają wszystkie węzły.

- **Karta „Kondycja sieci”** — pierścień z procentem węzłów w dobrym stanie i liczbą węzłów w każdej ocenie. Kliknięcie oceny (np. „2 Źle”) pokazuje tylko węzły z tą oceną.
- **Zakładki metryk** — zasilanie, sygnał, eter, środowisko, ruch, zasoby i sąsiedzi. Każdy węzeł ma kolorowy pasek, wartość i mały wykres trendu.
- **Kolory** — złoty to dobrze, czerwony średnio, fioletowy źle, biały bardzo źle, niebieski brak danych. Pod przyciskiem **(i)** u góry jest opis wszystkich kolorów i progów.
- **Wyszukiwanie i sortowanie** — pole „Szukaj węzła…”, sortowanie „Wg wartości” lub „Wg aktywności”, filtr „Tylko problemy”, ulubione na górze i ukrywanie węzłów bez danych.
- **Chowanie karty podsumowania** — przeciągnięcie palcem w górę zwija całą kartę „Kondycja sieci”, żeby zostało więcej miejsca na listę; przeciągnięcie w dół ją rozwija. Domyślnie to chowanie jest zablokowane. Przełącznik „Blokuj ukrywanie kondycji sieci przy przesuwaniu listy” jest w oknie **(i)**.
- **Podsumowanie** — ikona listy u góry otwiera ranking węzłów (najlepszy sygnał, najcichsze, najwięcej wiadomości i inne) z wykresami. Wynik można zapisać jako PDF, HTML, Markdown, tekst albo CSV.
- **Widżet** — na Androidzie można dodać na ekran główny widżet „Local Stats”. Zobacz [Widżet ekranu głównego](widget).

## Mapa

- **Połączenia między węzłami** — cienkie linie łączą węzły, które wymieniają pakiety, w kolorze jakości sygnału (SNR). Linie pochodzą z raportów sąsiadów, z traceroute z ostatniej doby i z węzłów słyszanych bezpośrednio. Włączają się i wyłączają w filtrach mapy („Pokaż połączenia między węzłami”); domyślnie są włączone.
- **Pokaż na mapie** — w szczegółach węzła, pod jego pozycją, jest przycisk **Pokaż na mapie**. Przenosi na główną mapę, przybliża do węzła i rysuje go na wierzchu, ponad innymi węzłami stojącymi w tym samym miejscu.
- **Trasa traceroute na mapie** — każdy skok jest osobną linią w kolorze jakości sygnału, ze strzałką i wartością SNR.
- **Mapy offline** — działają także dla OpenStreetMap. Pobieraj małe obszary, bo serwer OSM nie lubi masowego pobierania.
- **Nakładki terenu i pogody** — cieniowanie rzeźby terenu i radar pogodowy są domyślnie włączone.

Więcej o mapie: [Mapa i punkty](map-and-waypoints).

## Mesh Link Planer

Planer symuluje zasięg łącza radiowego między dwoma punktami **A** i **B**, w całości w aplikacji, na telefonie i na komputerze.

- Każdy punkt ustawisz z listy węzłów, kliknięciem w mapę, współrzędnymi albo z podłączonej stacji.
- Wysokość terenu pochodzi z modelu DEM Mapterhorn.
- Osobno dla każdej strony ustawiasz moc, zysk anteny, wysokość zawieszenia i tłumienie przewodu antenowego (ręcznie albo kreatorem z bazą kabli i złączy).
- Obsługuje pasma amatorskie od 169 MHz do 2,4 GHz oraz dowolną częstotliwość 20 MHz–20 GHz.
- Liczy model ITM (Longley-Rice), krzywiznę Ziemi i pierwszą strefę Fresnela, rysuje przekrój terenu i podaje bilans łącza.

Planer otwiera przycisk **Planer** w szczegółach węzła, obok kompasu.

## Diagnostyka na żądanie

W szczegółach węzła (Administracja → „Diagnostyka na żądanie”) możesz zapytać dowolny węzeł w zasięgu o jego bieżące statystyki, zamiast czekać na okresowe rozgłoszenia. Jest 10 rodzajów zapytań, m.in. statystyki węzła (bateria, czas pracy, procesor i pamięć), ping z RSSI i SNR, lista węzłów online, historia błędów routingu i aktywność eteru. Wyniki można zapisać i udostępnić. Ta funkcja wymaga firmware z modułem OnDemand.

## Sniffer

Ekran **Sniffer** (Ustawienia → Advanced) pokazuje na żywo ruch w sieci, także pakiety, których telefon nie potrafi rozszyfrować. Zapamiętuje ramki po wyłączeniu radia, grupuje duplikaty, ma automatyczne przewijanie i próbę rozszyfrowania oraz zapis i wczytywanie logu.

## Komunikator

- **Zdjęcia przez link** — aplikacja nie wysyła zdjęcia przez LoRa, tylko wysyła link; odbiorca widzi podgląd. Podgląd obrazków z linków jest domyślnie wyłączony i włącza się w Ustawienia → Prywatność.
- **Długie wiadomości** — tekst, który nie mieści się w jednym pakiecie, jest dzielony na części i składany po drugiej stronie.
- **Szybki czat** — przy pierwszym uruchomieniu ma gotowy zestaw szablonów.
- **Komputer** — Enter to nowa linia, Ctrl+Enter wysyła wiadomość.

## Ustawienia i wygląd

- **Skalowanie aplikacji** — w ustawieniach aplikacji wybierasz rozmiar tekstu i elementów od 50% do 200%, niezależnie od ustawień telefonu. Domyślnie „Zgodnie z telefonem”. Przydatne, gdy telefon ma powiększoną czcionkę albo rozmiar ekranu, a ekrany wyglądają za duże. Działa też na komputerze.
- **Korekta LNA** — w Ustawienia → LoRa możesz wpisać wzmocnienie LNA, a aplikacja skoryguje wyświetlany szum i RSSI. Surowe dane zostają bez zmian.
- **Odwrócenie prądu i baterii** — dla czujnika zasilania podłączonego odwrotnie.
- **Role węzłów po polsku** — nazwy i opisy ról urządzeń są przetłumaczone.
- **Zgłaszanie błędów i pomysłów** — pozycja „Zgłoś błąd lub pomysł” w ustawieniach prowadzi do naszego Discorda i grup.

## Pomoc i dokumentacja

Cała pomoc i dokumentacja w aplikacji są po polsku. Wielkość tekstu na stronach pomocy zmienisz przyciskami „A−” i „A+” u góry. Zobacz [Pomoc i dokumentacja](help-and-docs).

## Powiązane tematy

- [Węzły](nodes) — lista węzłów i szczegóły węzła
- [Mapa i punkty](map-and-waypoints) — mapa, warstwy i punkty
- [Metryki węzła](node-metrics) — wykresy telemetrii, na których opiera się Zdrowie sieci
- [Pomoc i dokumentacja](help-and-docs) — jak korzystać z pomocy w aplikacji
