---
title: Widżet ekranu głównego
parent: Przewodnik użytkownika
nav_order: 20
last_updated: 2026-08-30
description: Dodaj widżet MT_SW na ekran główny, aby zerknąć na lokalne statystyki połączonego radia bez otwierania aplikacji.
aliases:
  - widget
  - home-screen-widget
  - local-stats-widget
---

# Widżet ekranu głównego

Na Androidzie MT_SW udostępnia **widżet** ekranu głównego, który na bieżąco pokazuje lokalne statystyki połączonego radia — bez potrzeby otwierania aplikacji.

## Co pokazuje

Widżet wyświetla bieżące lokalne statystyki **połączonego radia**:

- **Znacznik węzła** u góry, zawierający krótką nazwę radia w jego własnych kolorach
- **Bateria** — poziom naładowania baterii radia lub *Zasilanie*, gdy radio pracuje na zasilaniu zewnętrznym
- **ChUtil** — wykorzystanie kanału (jak bardzo zajęty jest kanał LoRa, w procentach)
- **AirUtil** — wykorzystanie czasu antenowego (jaka część limitu czasu nadawania jest zużywana przez Twoje radio)
- **Ruch** — pakiety wysłane / odebrane oraz zauważone duplikaty
- **Przekaźniki** — pakiety przekazane dalej i anulowane przekazania (pokazywane, gdy radio przekazuje pakiety)
- **Diagnostyka** — połączona linia zawierająca **Szum** (poziom szumu tła w dBm), **Błędne** (odebrane uszkodzone pakiety) i **Odrzucone** (pakiety odrzucone przez radio). Błędne i Odrzucone pojawiają się dopiero, gdy są większe od zera, więc spokojne radio może pokazywać samą wartość szumu
- **Sterta** — wolna i całkowita pamięć radia, przedstawiona jako pasek
- **Węzły** — ile węzłów jest online spośród wszystkich znanych
- **Czas pracy** — jak długo radio pracuje od ostatniego restartu, pokazany obok Węzłów
- **Zaktualizowano** — czas ostatniego odświeżenia statystyk, u dołu widżetu

Dotknij widżetu, aby otworzyć aplikację, lub użyj jego przycisku odświeżania, aby zażądać nowych statystyk.

> ℹ️ **Uwaga:** Wartości odzwierciedlają połączone radio. Jeśli radio się rozłączy, widżet zastępuje statystyki wierszem stanu — **Rozłączono**, **Łączenie** lub **Urządzenie uśpione**. Nie pozostawia na ekranie ostatnio znanych liczb.

## Dodawanie widżetu

1. Dotknij i przytrzymaj puste miejsce na ekranie głównym Androida.
2. Dotknij **Widżety**.
3. Przeciągnij widżet **MT_SW_APP** na ekran główny. Aplikacja zawiera jeden widżet, więc pozycja w wyborze nosi po prostu nazwę aplikacji.
4. W razie potrzeby zmień jego rozmiar — układ dostosowuje się do dostępnego miejsca.

> ℹ️ **Uwaga:** Widżet jest dostępny tylko na Androidzie. Nie ma go w kompilacjach na komputery ani iOS.

## Powiązane tematy

- [Metryki węzła](node-metrics) — pełna historia Jakości sygnału i Statystyk lokalnych w aplikacji
- [Połączenia](connections) — połącz się z radiem, aby widżet miał statystyki do pokazania
- [Odkrywanie lokalnej sieci mesh](discovery) — wykorzystanie kanału i czasu antenowego w całej sieci mesh
