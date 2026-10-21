---
title: Powiadomienia
parent: Przewodnik użytkownika
nav_order: 18
last_updated: 2026-10-21
description: Do czego służy każde powiadomienie MT_SW, jak wyciszyć jeden rodzaj bez pozostałych oraz co możesz zrobić z powiadomienia o wiadomości lub na zegarku.
aliases:
  - notifications
  - notification-channels
  - wear-os
  - smartwatch
---

# Powiadomienia

MT_SW wysyła powiadomienie, gdy coś w sieci mesh Cię potrzebuje, a aplikacja jest poza zasięgiem wzroku: wiadomość, nowy węzeł, niski poziom baterii lub komunikat z Twojego węzła. Każdy rodzaj jest osobną kategorią powiadomień Androida, więc możesz wyciszyć jeden i zachować pozostałe.

## Kategorie powiadomień

Aby zobaczyć kategorie, dotknij **Powiadomienia aplikacji** w sekcji **Informacje** w **Ustawieniach**. Otwiera to ustawienia powiadomień Androida dla aplikacji, gdzie kategorie są w trzech grupach.

| Grupa | Kategoria | Publikowane dla | Dotknięcie otwiera |
|---|---|---|---|
| Wiadomości | Powiadomienia o bezpośredniej wiadomości | Wiadomość wysłana bezpośrednio do Ciebie | Rozmowę |
| Wiadomości | Powiadomienia o wiadomościach rozgłoszeniowych | Wiadomość na jednym z Twoich kanałów | Kanał |
| Wiadomości | Powiadomienia o reakcjach | Reakcja emoji na wiadomość w jednej z Twoich rozmów | Rozmowę |
| Wiadomości | Powiadomienia punktów orientacyjnych | Punkt trasy udostępniony w sieci mesh lub przekroczenie geofence | Punkt trasy na mapie |
| Wiadomości | Powiadomienia alertowe | Krytyczny alert z węzła | Rozmowę |
| Mesh | Powiadomienia o nowych węzłach | Węzeł usłyszany po raz pierwszy | Szczegóły węzła |
| Mesh | Powiadomienia o zaproszeniach do sieci mesh | Zaproszenie do dołączenia do pobliskiej sieci mesh | Odkrywanie lokalnej sieci mesh |
| Mesh | Powiadomienia o niskim poziomie baterii (ulubione węzły) | Kończąca się bateria ulubionego węzła | Szczegóły węzła |
| Urządzenie | Powiadomienia o usługach | Połączenie z Twoim węzłem, gdy aplikacja działa w tle | Aplikację |
| Urządzenie | Powiadomienia o niskim poziomie baterii | Kończąca się bateria Twojego węzła | Szczegóły węzła |
| Urządzenie | Powiadomienia z radia | Komunikaty z Twojego węzła, takie jak prośby o weryfikację kluczy i ostrzeżenia bezpieczeństwa | Aplikację |
| Urządzenie | Powiadomienia o aktualizacjach i połączeniu | Aktualizacja firmware dla Twojego węzła lub problem z ponownym połączeniem | Aktualizację firmware lub Połączenia |

Domyślnie wiadomości bezpośrednie, alerty i komunikaty z Twojego węzła wyskakują na ekranie, zaproszenia do sieci mesh i powiadomienie usługi przychodzą bez dźwięku, a pozostałe wydają dźwięk. Android zachowuje wszystko, co zmienisz w kategorii, a aplikacja nie może tego cofnąć.

Powiadomienia alertowe odtwarzają dźwięk alarmu. Aby przepuszczać je także przez tryb Nie przeszkadzać, otwórz kategorię i zezwól jej na pomijanie trybu Nie przeszkadzać — to krok, który [Pierwsze kroki](onboarding#critical-alerts-permission) oferuje podczas konfiguracji.

Powiadomienie usługi pozostaje w panelu powiadomień, dopóki MT_SW jest połączone w tle. Pokazuje stan połączenia, a także postęp, gdy trwa aktualizacja firmware lub skan presetów.

> ℹ️ **Uwaga:** Aplikacja na komputer nie ma kategorii powiadomień. Zamiast tego włącza powiadomienia we własnych ustawieniach, opisanych w [Preferencje powiadomień](desktop#notification-preferences).

## Kiedy wiadomość wysyła powiadomienie

Jedno powiadomienie zbiera ostatnie wiadomości każdej rozmowy. To, czy nowa wiadomość do niego dołącza, zależy od tego, gdzie jesteś:

- **Czytasz tę rozmowę:** nic nie jest wysyłane, bo wiadomość jest już na ekranie.
- **W aplikacji, na innym ekranie:** powiadomienie aktualizuje się bez dźwięku.
- **Gdziekolwiek indziej:** powiadomienie aktualizuje się i alarmuje Cię tak, jak ustawiono jego kategorię.

Wyciszenie rozmowy lub kanału zatrzymuje jego powiadomienia, z wyjątkiem wiadomości, która wspomina o Tobie. Wyciszenie węzła zatrzymuje jego powiadomienia, nawet gdy wspomina o Tobie. Jak wyciszać, opisano w [Wiadomości i kanały](messages-and-channels). Krytyczny alert nadal Cię powiadamia, gdy czytasz jego rozmowę, i tylko wyciszenie go uciszy.

## Działania z powiadomienia

Powiadomienie o wiadomości ma trzy akcje:

- **Odpowiedz** wysyła wiadomość do tej rozmowy bez otwierania aplikacji. Powiadomienie następnie aktualizuje się, pokazując Twoją odpowiedź.
- **Oznacz jako przeczytane** zeruje licznik nieprzeczytanych w rozmowie i zamyka powiadomienie.
- **👍** reaguje na najnowszą wiadomość kciukiem w górę.

W Androidzie 11 i nowszym powiadomienie o wiadomości może też otworzyć się jako pływający dymek, opisany w [Dymki rozmów](messages-and-channels#conversation-bubbles).

## Na zegarku

Zegarek Wear OS sparowany z telefonem pokazuje każde powiadomienie MT_SW z wyjątkiem powiadomienia usługi, które Android zostawia na telefonie, ponieważ nigdy nie znika. Powiadomienie o wiadomości zachowuje swoje akcje na zegarku, a **Odpowiedz** może tam oferować sugerowane odpowiedzi. To ustawienia powiadomień samego zegarka decydują, czy MT_SW w ogóle się na nim pojawia.

## Zobacz także

- [Wiadomości i kanały](messages-and-channels) — wyciszanie rozmów i węzłów, dymki oraz stany wiadomości
- [Pierwsze kroki](onboarding) — kroki konfiguracji powiadomień i krytycznych alertów
- [Aktualizacje firmware](firmware) — ekran, który otwiera powiadomienie o aktualizacji
- [Odkrywanie lokalnej sieci mesh](discovery) — co oferuje zaproszenie do sieci mesh
