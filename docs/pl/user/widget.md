---
title: Widżet ekranu głównego
parent: Przewodnik użytkownika
nav_order: 20
last_updated: 2026-11-16
description: Dodaj na ekran główny Androida widżety Statystyki MT_SW i SOS, aby widzieć swój węzeł i sieć jednym spojrzeniem albo wysłać alarm kilkoma szybkimi dotknięciami.
aliases:
  - widget
  - home-screen-widget
  - mesh-status-widget
  - sos-widget
---

# Widżet ekranu głównego

Na Androidzie MT_SW udostępnia dwa **widżety** ekranu głównego:

- **Statystyki MT_SW** pokazuje te same statystyki co stałe powiadomienie: stan całej sieci mesh albo jednego śledzonego węzła.
- **SOS** wysyła alarm po kilku szybkich dotknięciach pod rząd.

Widżet statystyk pokazuje odczyty, które aplikacja już zapisała, więc działa bez połączenia z radiem i nigdy niczego nie wysyła przez sieć mesh.

## Statystyki MT_SW

Widżet pokazuje węzły online, nieprzeczytane wiadomości, baterię, czas pracy, obciążenie kanału, ruch i resztę, co zgłasza radio. Co pokazuje (całą sieć mesh albo jeden wybrany węzeł), ustawiasz w wyświetlaniu adaptacyjnym na ekranie **Połączenia**. Gdy śledzi całą sieć, co minutę przełącza się między węzłami.

## SOS

Alarm wysyła się kilkoma szybkimi dotknięciami widżetu pod rząd (odstęp między dotknięciami najwyżej 2,5 s). Przytrzymanie nie działa, bo launcher wtedy zaczyna edycję widżetu. Liczbę dotknięć ustawisz w ekranie **SOS** w sekcji „Widżet na ekranie głównym”: od 3 (domyślnie 3), żeby kieszeń nie wysłała alarmu przypadkiem. W trakcie widżet pokazuje „Dotknij jeszcze N”, a po wysłaniu „Wysłano”; dotknięcie wtedy otwiera ekran SOS z odpowiedziami. Wiadomość, kanały i węzły opisuje [SOS](mt-sw-features). Na iPhonie/iPadzie ostatnie dotknięcie otwiera aplikację, która wysyła alarm.

## Dodawanie widżetu

1. Przytrzymaj pusty obszar ekranu głównego Androida.
2. Dotknij **Widżety** i znajdź na liście **MT_SW**.
3. Przeciągnij **Statystyki MT_SW** (4x2) albo **SOS** na ekran główny.
4. W razie potrzeby zmień rozmiar — układ dopasowuje się do dostępnego miejsca.

> ℹ️ **Uwaga:** Widżety są tylko na Androidzie. Na iPhonie i iPadzie statystyki to widżet dodawany przyciskiem **+** na ekranie głównym, a na komputerze są w ikonie w zasobniku i w oknie głównym.

## Powiązane tematy

- [Węzły](nodes) — ulubione, ostatnio słyszane i szczegóły węzła
- [Połączenia](connections) — połącz się z węzłem, żeby widżet miał dane
- [Lokalne odkrywanie sieci](discovery) — obciążenie kanału i czasu antenowego w całej sieci
