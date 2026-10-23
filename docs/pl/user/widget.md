---
title: Widżet ekranu głównego
parent: Przewodnik użytkownika
nav_order: 20
last_updated: 2026-10-21
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

- **Znacznik węzła** u góry, zawierający krótką nazwę węzła w jego własnych kolorach
- **Bateria** — poziom naładowania baterii węzła lub *Zasilanie*, gdy węzeł pracuje na zasilaniu zewnętrznym
- **ChUtil** — wykorzystanie kanału (jak bardzo zajęty jest kanał LoRa, w procentach)
- **AirUtil** — procent czasu antenowego, który ten węzeł wykorzystał na nadawanie w ostatniej godzinie
- **Ruch** — pakiety wysłane / odebrane oraz zauważone duplikaty
- **Przekaźniki** — pakiety przekazane dalej i anulowane przekazania (pokazywane, gdy węzeł przekazuje pakiety)
- **Diagnostyka** — połączona linia zawierająca **Szum** (poziom szumu tła w dBm), **Błędne** (odebrane uszkodzone pakiety) i **Odrzucone** (pakiety odrzucone przez węzeł). Błędne i Odrzucone pojawiają się dopiero, gdy są większe od zera, więc spokojne radio może pokazywać samą wartość szumu
- **Sterta** — wolna i całkowita pamięć węzła, przedstawiona jako pasek
- **Węzły** — ile węzłów jest online spośród wszystkich znanych
- **Czas pracy** — jak długo węzeł pracuje od ostatniego restartu, pokazany obok Węzłów
- **Zaktualizowano** — czas ostatniego odświeżenia statystyk, u dołu widżetu

Dotknij widżetu, aby otworzyć aplikację, lub użyj jego przycisku odświeżania, aby zażądać nowych statystyk.

> ℹ️ **Uwaga:** Wartości odzwierciedlają połączony węzeł. Jeśli węzeł się rozłączy, widżet zastępuje statystyki wierszem stanu: **Rozłączono**, **Łączenie** lub **Urządzenie uśpione**. Nie pozostawia na ekranie ostatnio znanych liczb.

## Node Tracker

Widżet **Node Tracker** pokazuje jeden węzeł z Twojej listy węzłów:

- **Znaczek** i **długą nazwę** węzła
- **Baterię**: ostatni zgłoszony poziom baterii węzła albo _Zasilany_, gdy węzeł jest na zasilaniu zewnętrznym
- **Ostatnio słyszany**: czas, kiedy Twój połączony węzeł ostatnio go słyszał, albo data, jeśli było to ponad dobę temu

Poszerz widżet, aby dodać drugą kolumnę:

- **SNR** i **RSSI**: sygnał ostatniego pakietu węzła, pokazywany tylko wtedy, gdy węzeł jest słyszany bezpośrednio, a nie przez inne węzły lub MQTT
- **Skoki**: ile skoków dzieli węzeł od Ciebie albo _Bezpośrednio_
- **Odległość**: jak daleko węzeł jest od połączonego węzła. Pojawia się tylko przy połączeniu i gdy oba węzły mają pozycję

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
