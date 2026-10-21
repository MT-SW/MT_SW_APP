---
title: Jak działa miernik sygnału
parent: Przewodnik użytkownika
nav_order: 15
last_updated: 2026-10-21
description: Jak miernik sygnału ocenia jakość na podstawie SNR względem presetu modemu LoRa — widmo rozproszone, presety i co naprawdę oznaczają paski.
aliases:
  - signal
  - signal-meter
  - snr
  - rssi
---

# Jak działa miernik sygnału

Miernik sygnału — paski lub kolor stanu obok węzła — jest obliczany inaczej niż paski w telefonie komórkowym czy routerze Wi-Fi. Ta strona wyjaśnia, co mierzy i dlaczego ten sam odczyt może znaczyć coś innego na innym presecie.

## RSSI i SNR

Za każdym razem, gdy radio LoRa odbiera wiadomość, podaje dwa pomiary:

- **RSSI (Received Signal Strength Indicator, wskaźnik siły odbieranego sygnału)** — surowa moc docierająca do anteny.
- **SNR (Signal-to-Noise Ratio, stosunek sygnału do szumu)** — jak bardzo sygnał wyróżnia się ponad szum tła.

> 💡 **Wskazówka:** Pomyśl o RSSI jako o tym, jak głośno mówi znajomy, a o SNR jako o tym, jak łatwo wyłowić jego głos z hałasu w pokoju. Znajomy krzyczący na koncercie rockowym może być głośny (wysokie RSSI), a mimo to niezrozumiały (zły SNR), podczas gdy szept w cichej bibliotece jest słaby (niskie RSSI), a mimo to doskonale wyraźny (świetny SNR).

## Dekodowanie poniżej poziomu szumu

Standardowe radia, takie jak FM czy Wi-Fi, tracą sygnał w trzasku, gdy szum tła jest głośniejszy od niego (ujemny SNR). Modulacja z widmem rozproszonym w LoRa pozwala radiu wydobyć sygnał z szumu nawet wtedy, gdy szum jest głośniejszy, więc ujemne wartości SNR są w sieciach Meshtastic częste i oczekiwane — na przykład −10 dB oznacza, że sygnał jest o 10 decybeli słabszy niż szum tła.

Każdy preset modemu ma limit SNR: najniższy SNR, przy którym ten preset jest jeszcze w stanie zdekodować wiadomość. Wolniejsze presety tolerują słabszy, bardziej zaszumiony sygnał (bardziej ujemny limit, większy zasięg); szybsze presety wymagają silniejszego sygnału (mniej ujemny limit, mniejszy zasięg).

## Ocena jakości sygnału

Aplikacja ocenia jakość sygnału (Brak, Zły, Dostateczny lub Dobry) na podstawie SNR względem limitu SNR aktywnego presetu. Gdy dostępne są zarówno RSSI, jak i odczyt poziomu szumu, ocenia również różnicę między nimi względem tego limitu i używa gorszej oceny. W przeciwnym razie używa samego SNR.

Ponieważ ocena jest względna wobec presetu, ten sam SNR jest oceniany inaczej na różnych presetach. SNR równy −16 dB jest oceniany jako Dobry na Long Fast (limit SNR −17,5 dB), ale jako Brak na Short Fast (limit SNR −7,5 dB). Niech `limit` oznacza limit SNR aktywnego presetu:

| Poziom | Paski | Kryteria | Znaczenie |
|-------|------|----------|---------|
| Dobry | Pełne | SNR powyżej `limit` | Wygodnie powyżej progu demodulacji — zdrowe połączenie. |
| Dostateczny | 3 | mniej niż 5,5 dB poniżej `limit` | Da się zdekodować, ale zbliża się do progu. |
| Zły | 2 | od 5,5 dB do 7,5 dB poniżej `limit` | Na granicy tego, co preset jest w stanie odzyskać. |
| Brak | 1 | więcej niż 7,5 dB poniżej `limit` | Daleko poniżej progu presetu; kolejne pakiety z tego węzła prawdopodobnie zostaną utracone. |

Ikona nigdy nie robi się pusta, więc licz paski uważnie: pojedynczy pasek oznacza Brak, a nie słabe, ale użyteczne łącze, a Dobry to pełny klin, a nie zestaw pasków. Szara ikona z trzema paskami z podpisem Nieznany to jeszcze inny stan — pakiet w ogóle nie niósł pomiaru SNR, co nie jest tym samym, co zmierzenie go i stwierdzenie, że jest zbyt słaby.

> ℹ️ **Uwaga:** Kolory SNR w traceroute i informacjach o sąsiadach również używają limitów aktywnego presetu. Tekst RSSI ma osobne kolory siły: zielony powyżej −115 dBm, żółty powyżej −120 dBm, pomarańczowy powyżej −126 dBm i czerwony przy −126 dBm lub mniej.

## Diagnozowanie lokalnych zakłóceń

Świetne RSSI w parze z oceną jednego lub dwóch pasków (Brak lub Zły) wskazuje na lokalne zakłócenia, a nie na odległość. Tani zasilacz, hałaśliwy komputer albo pobliski nadajnik mogą wytworzyć dość szumu, by zagłuszyć skądinąd silny sygnał.

## Gdzie pojawiają się informacje o sygnale

W aplikacji dane o sygnale pojawiają się w kilku miejscach:

- **Lista węzłów** — ikona pasków sygnału, pokazywana tylko dla węzłów, które Twój węzeł usłyszał bezpośrednio przez LoRa. Węzeł osiągany przez przekaźnik pokazuje zamiast tego liczbę skoków, ponieważ SNR zmierzony przez Twój węzeł opisuje ostatni skok, a nie całą ścieżkę
- **Szczegóły węzła** — SNR, RSSI i określenie jakości na karcie **Szczegóły** u góry ekranu
- **Traceroute** — jakość sygnału na każdym skoku dla każdego węzła przekaźnikowego
- **Jakość sygnału** — historyczne dane SNR i RSSI na wykresach metryk

![Wpis na liście węzłów pokazujący ocenę Dobry sygnał: SNR 12,5 dB, RSSI −42 dBm i zieloną ikonę siły sygnału](../../assets/screenshots/nodes_signal_info.png)

## Zobacz także

- [Węzły](nodes) — gdzie na liście węzłów pojawiają się paski sygnału
- [Metryki węzła](node-metrics) — historia SNR/RSSI i opis jakości sygnału dla każdego węzła
- [Ustawienia — Radio i użytkownik](settings-radio-user) — presety modemu i ich limity SNR
