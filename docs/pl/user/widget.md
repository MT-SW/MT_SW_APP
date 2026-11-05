---
title: Widżet ekranu głównego
parent: Przewodnik użytkownika
nav_order: 20
last_updated: 2026-11-04
description: Dodaj widżety MT_SW na ekran główny, aby zerknąć na lokalne statystyki połączonego węzła albo śledzić jeden wybrany węzeł, bez otwierania aplikacji.
aliases:
  - widget
  - home-screen-widget
  - local-stats-widget
  - node-tracker-widget
---

# Widżet ekranu głównego

Na Androidzie MT_SW udostępnia dwa **widżety** ekranu głównego:

- **MT_SW_APP** pokazuje na bieżąco lokalne statystyki połączonego węzła.
- **Node Tracker** śledzi jeden wybrany węzeł: jego baterię, sygnał, kiedy był ostatnio słyszany, ile skoków dzieli go od Ciebie i jak daleko jest od połączonego węzła.

Możesz umieścić dowolną liczbę każdego z nich. Każdy Node Tracker pamięta swój węzeł.

## Statystyki lokalne

Widżet **MT_SW_APP** wyświetla bieżące lokalne statystyki **połączonego węzła**:

- **Znacznik węzła** z krótką nazwą węzła w jego własnych kolorach, a obok **bateria** — poziom naładowania lub *Zasilanie*, gdy węzeł pracuje na zasilaniu zewnętrznym
- **ChUtil** — wykorzystanie kanału (jak bardzo zajęty jest kanał LoRa, w procentach)
- **AirUtil** — procent czasu antenowego, który ten węzeł wykorzystał na nadawanie w ostatniej godzinie
- **Węzły** — ile węzłów jest online spośród wszystkich znanych
- **Czas pracy** — jak długo węzeł pracuje od ostatniego restartu
- **Wysłane** i **Odebrane** — liczniki pakietów węzła
- **Szum** — poziom szumu tła w dBm
- **Pamięć** — zajęta pamięć jako procent całości; przy rozszerzonych statystykach także **CPU**, **PSRAM** i **Flash** (to, co węzeł podaje)
- **Przekazane** — pakiety przekazane dalej dla innych
- **Błędne** — odebrane uszkodzone pakiety
- **Odrzucone** — pakiety odrzucone przez węzeł
- **Anulowane przekazania** — przekazania, które węzeł zaczął, a potem anulował
- **Zaktualizowano** — czas ostatniego odświeżenia statystyk, u dołu widżetu o wysokości trzech wierszy

Widżet pokazuje tyle z tych odczytów, ile mieści jego rozmiar, w tej kolejności, więc mały widżet zachowuje te, na które patrzy się najczęściej. Widżet 2×2 pokazuje węzeł, baterię, wykorzystanie i kilka pierwszych odczytów; przy szerokości czterech komórek odczyty układają się w dwie kolumny, a wyższy widżet dodaje kolejne wiersze, aż wszystkie odczyty są na ekranie.

Dotknij widżetu, aby otworzyć aplikację, lub użyj jego przycisku odświeżania, aby zażądać nowych statystyk.

> ℹ️ **Uwaga:** Wartości odzwierciedlają połączony węzeł. Jeśli węzeł się rozłączy, widżet zastępuje statystyki wierszem stanu: **Rozłączono**, **Łączenie** lub **Urządzenie uśpione**. Nie pozostawia na ekranie ostatnio znanych liczb.

## Node Tracker

Widżet **Node Tracker** pokazuje jeden węzeł z Twojej listy węzłów:

- **Znaczek** i **długą nazwę** węzła
- **Baterię**: ostatni zgłoszony poziom baterii węzła albo _Zasilany_, gdy węzeł jest na zasilaniu zewnętrznym
- **Ostatnio słyszany**: czas, kiedy Twój połączony węzeł ostatnio go słyszał, albo data, jeśli było to ponad dobę temu

Od wysokości dwóch wierszy dodaje, w tej kolejności, tyle odczytów, ile się zmieści (szeroki widżet niższy niż dwa wiersze pokazuje Skoki obok nazwy):

- **Skoki**: ile skoków dzieli węzeł od Ciebie albo _Bezpośrednio_
- **Odległość**: jak daleko węzeł jest od połączonego węzła. Pojawia się tylko przy połączeniu i gdy oba węzły mają pozycję
- **SNR** i **RSSI**: sygnał ostatniego pakietu węzła, pokazywany tylko wtedy, gdy węzeł jest słyszany bezpośrednio, a nie przez inne węzły lub MQTT

Przy szerokości czterech komórek odczyty układają się w dwie kolumny, więc przy domyślnym rozmiarze czcionki mieszczą się wszystkie w widżecie o dwóch wierszach. Większa czcionka odcina odczyty od końca tej listy, najpierw RSSI.

Gdy węzeł nie był słyszany w ciągu ostatnich dwóch godzin, widżet przyciemnia jego odczyty, żeby nie wyglądały na aktualne. Dotknij widżetu, aby otworzyć szczegóły tego węzła w aplikacji.

Po rozłączeniu z węzłem w aplikacji widżet zostawia na ekranie ostatnie odczyty śledzonego węzła, przyciemnione, aż do ponownego połączenia.

Jeśli Twoja lista węzłów nie zawiera wybranego węzła, bo go usunąłeś lub połączyłeś się z innym, widżet pokazuje **Nie znaleziono węzła**. Dotknij go, aby wybrać inny.

## Dodawanie widżetu

1. Dotknij i przytrzymaj puste miejsce na ekranie głównym Androida.
2. Dotknij **Widżety**, a potem znajdź **MT_SW_APP** na liście.
3. Przeciągnij widżet **MT_SW_APP** (statystyki lokalne) lub **Node Tracker** na ekran główny.
4. W przypadku Node Tracker wybierz węzeł do śledzenia. Ulubione są na początku listy, potem ostatnio słyszane węzły; wpisz tekst w polu wyszukiwania, aby filtrować po nazwie lub identyfikatorze węzła. Wyjście bez wyboru anuluje widżet.
5. W razie potrzeby zmień rozmiar — układ dostosowuje się do dostępnego miejsca.

Aby później śledzić inny węzeł, dotknij i przytrzymaj Node Tracker i wybierz ustawienia widżetu lub opcję ponownej konfiguracji. Dokładna nazwa zależy od Twojego launchera.

> ℹ️ **Uwaga:** Widżety są dostępne tylko na Androidzie. Nie ma ich w aplikacji na komputer.

## Powiązane tematy

- [Metryki węzła](node-metrics) — pełna historia Jakości sygnału i Statystyk lokalnych w aplikacji
- [Węzły](nodes) — ulubione, ostatnio słyszane i szczegóły węzła
- [Połączenia](connections) — połącz się z węzłem, aby widżety miały dane do pokazania
- [Odkrywanie lokalnej sieci mesh](discovery) — wykorzystanie kanału i czasu antenowego w całej sieci mesh
