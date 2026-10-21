---
title: Integracja TAK
parent: Przewodnik użytkownika
nav_order: 10
last_updated: 2026-10-21
description: Współpraca z ATAK i WinTAK — udostępnianie pozycji CoT, role TAK i konfiguracja wtyczki.
aliases:
  - tak
  - atak
  - team-awareness-kit
---

# Integracja TAK

MT_SW integruje się z ekosystemem Team Awareness Kit (TAK), umożliwiając współpracę radiów MT_SW z aplikacjami TAK, takimi jak ATAK i WinTAK.

## Przegląd

Moduł TAK pozwala węzłom MT_SW:
- Udostępniać dane o pozycji w formacie CoT (Cursor on Target) zgodnym z TAK
- Pojawiać się jako członkowie zespołu na mapach TAK
- Odbierać wiadomości TAK PLI (Position Location Information)

## Konfiguracja

### Wymagania wstępne

- Zainstalowany ATAK (Android Team Awareness Kit), iTAK lub WinTAK
- **Rola urządzenia** Twojego węzła (Konfiguracja urządzenia) ustawiona na **TAK** lub **TAK Tracker** — to sprawia, że
  moduł TAK w ogóle pojawia się w Konfiguracji modułów
- Firmware 2.8.0 lub nowsze w radiu. Wcześniejsze firmware przyjmuje zapis konfiguracji TAK i nigdy go
  nie przechowuje, więc aplikacja ukrywa pozycję modułu TAK poniżej tej wersji, nawet gdy rola jest ustawiona
  poprawnie. Zobacz [Aktualizacje firmware](firmware)

> ⚠️ **Ostrzeżenie:** Stara **wtyczka Meshtastic ATAK** nie jest już częścią tej ścieżki i nie może
> działać. Łączyła się przez międzyprocesowe API AIDL, które zostało usunięte w aplikacji 2.8.0; usługa
> mesh działa wyłącznie w obrębie procesu aplikacji. Nie instaluj jej. Współpraca działa przez własny lokalny
> serwer TAK aplikacji oraz Konwerter Mesh na CoT, opisane w następnych sekcjach, ze zwykłymi ATAK/iTAK/WinTAK.

### Konfiguracja

Przejdź do **Ustawienia → Konfiguracja modułów → TAK (ATAK)**. Własne ustawienia modułu to Twoja tożsamość TAK —
nie ma tu osobnego przełącznika włączania, ponieważ to ustawienie **Rola urządzenia** w Konfiguracji urządzenia
włącza TAK. Twój węzeł rozgłasza tę tożsamość, która pojawia się na mapach TAK.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Rola | Twoja rola operacyjna w zespole: Członek zespołu, Lider zespołu, Kwatera główna, Snajper, Medyk, Obserwator wysunięty, Operator radiotelefonu lub K9 | Domyślna (Członek zespołu) |
| Zespół | Twój kolor zespołu na mapie TAK, jeden z 14 standardowych kolorów zespołów TAK, np. Niebieski, Czerwony, Cyjan lub Zielony | Domyślny (Cyjan) |

Twój znak wywoławczy TAK nie jest osobnym ustawieniem. Firmware używa długiej nazwy Twojego węzła.

Kolory zespołów to standardowe kolory zespołów TAK, więc uzgodnij ze swoim zespołem TAK spójne przydziały do zespołów.

### Lokalny serwer TAK

Aplikacja może też uruchomić **lokalny serwer TAK**, aby ATAK na **tym samym telefonie** albo WinTAK na tym samym komputerze, na którym działa aplikacja na komputer, mogły łączyć się bezpośrednio, bez zdalnego serwera TAK. Serwer nasłuchuje wyłącznie na localhost (`127.0.0.1:8089`) i używa TLS z wzajemnym uwierzytelnianiem certyfikatami (mTLS), więc nie jest osiągalny z innych urządzeń w sieci.

Otwórz **Ustawienia → Zaawansowane → Serwer TAK**. To ustawienia aplikacji przechowywane na tym telefonie, dlatego są obok **Aktualizacja oprogramowania** i **Panel debugowania**, a nie w konfiguracji modułu TAK — i dlatego nie potrzebują ani roli TAK, ani firmware 2.8.0. W Androidzie 17 i nowszych włączenie serwera prosi o uprawnienie do sieci lokalnej, którego potrzebuje do otwarcia gniazda. Jeśli to uprawnienie zostanie odmówione na stałe, aplikacja wyłącza serwer z powrotem i proponuje otwarcie ustawień aplikacji:

![Ustawienia lokalnego serwera TAK z przełącznikiem włączania i opcją eksportu](../../assets/screenshots/tak_server_enabled.png)

- **Włącz lokalny serwer TAK** — uruchamia serwer mTLS tylko na pętli zwrotnej, na porcie **8089**, dla połączeń ATAK z tego samego urządzenia.
- **Stan** — pod przełącznikiem: *Wyłączony*, *Uruchamianie serwera TAK*, *Nasłuchuje na 127.0.0.1:8089 — czeka na ATAK*, liczba lokalnych połączeń klientów albo *Nie udało się uruchomić serwera TAK. Wyłącz go i włącz ponownie, aby spróbować jeszcze raz.*
- **Kanał sieci TAK** — wybiera, na którym kanale MT_SW wysyłany jest wychodzący ruch TAK (domyślnie: kanał podstawowy, indeks 0). Przychodzący ruch TAK jest przyjmowany z dowolnego kanału. Odpowiada analogicznemu ustawieniu w iOS i w starszej wtyczce ATAK.
- **Konwerter Mesh na CoT** — domyślnie wyłączony, pokazywany pod przełącznikiem serwera. Przy uruchomionym serwerze
  tworzy kontakt CoT dla każdego węzła w Twojej bazie węzłów, więc zwykłe
  węzły MT_SW pojawiają się na mapie ATAK jako kontakty. **To właśnie zastąpiło widoczność
  węzłów starej wtyczki** — bez tego pokazują się tylko węzły z rolą TAK. Gdy połączony węzeł ma firmware starsze niż 2.8.0, uwaga pod przełącznikiem mówi, że do ATAK przechodzą tylko pozycje i wiadomości czatu.
- **Eksportuj pakiet danych TAK** — generuje pakiet danych `.zip`, który ATAK może zaimportować, aby połączyć się z tym serwerem. To samo robi ikona udostępniania na górnym pasku.

## Role TAK

Węzły skonfigurowane z rolami związanymi z TAK zachowują się inaczej niż standardowi klienci:

| Rola | Opis |
|------|-------------|
| **TAK** | Dla węzła sparowanego z urządzeniem z ATAK. Wyłącza wiele zwykłych rozgłoszeń, aby dać pierwszeństwo strumieniowi pakietów CoT. |
| **TAK Tracker** | Automatycznie rozsyła TAK PLI w odstępach ustawionych w konfiguracji pozycji i wyłącza wiele zwykłych rozgłoszeń. Nadaje się do bezobsługowych beaconów pozycji, takich jak pojazdy i sprzęt. |

> 💡 **Wskazówka:** Użyj **TAK Tracker** dla urządzeń, które mają tylko raportować pozycję (np. radio zamontowane w pojeździe). Użyj **TAK** dla urządzeń, w których użytkownicy aktywnie uczestniczą w operacjach TAK.

### Format CoT (Cursor on Target)

Wiadomości TAK używają formatu XML Cursor on Target — wojskowego standardu wymiany danych o świadomości sytuacyjnej. MT_SW konwertuje swoje wewnętrzne wiadomości protobuf do formatu CoT podczas łączenia z systemami TAK, więc nie jest potrzebna ręczna konwersja formatu.

## Format transmisji (V1 / V2)

MT_SW obsługuje dwa formaty transmisji TAK, wybierane automatycznie na podstawie firmware podłączonego radia — nie jest potrzebna ręczna konfiguracja:

| Format | Zgodność | Funkcje |
|--------|--------------|----------|
| V1 (starszy) | Firmware 2.7.x i starsze | Surowe kodowanie protobuf na porcie 72. Obsługuje tylko udostępnianie pozycji (PLI) i czat (GeoChat) — kształty, znaczniki, trasy i inne typowane zdarzenia CoT są odrzucane |
| V2 (bieżący) | Firmware 2.8.0+ | Zwarte kodowanie skompresowane zstd na porcie 78. Dodaje kształty, znaczniki, trasy, statki powietrzne, casevac, sytuacje awaryjne i zadania jako typy CoT, oprócz wszystkiego, co obsługuje V1 |

Węzeł nadal przekazuje dalej starsze pakiety V1 z dawniejszych węzłów, nawet gdy sam działa w V2, więc sieci mesh z mieszanym firmware nadal działają.

## Użycie z ATAK

Po skonfigurowaniu:
- Węzły MT_SW pojawiają się jako znaczniki na mapie ATAK z etykietami znaków wywoławczych
- Wiadomości czatu mogą być mostkowane między siecią mesh a sieciami TAK
- Aktualizacje pozycji płyną dwukierunkowo między MT_SW a TAK
- Węzły TAK Tracker automatycznie rozsyłają PLI — ich pozycje pojawiają się na mapach ATAK bez żadnej konfiguracji po stronie ATAK
- Na Androidzie trasy odebrane z sieci mesh są też zapisywane jako pakiet danych (`.zip`) w **Pobrane**; zaimportuj go w ATAK, aby dodać trasę. Aktualizacja tej samej trasy nadpisuje jej plik, a nie dodaje kopii. Po ponownej instalacji aplikacji pierwsza aktualizacja trasy zapisuje jedną kopię ze zmienioną nazwą, a kolejne ją nadpisują. W Androidzie 9 i starszych plik trafia zamiast tego do własnego folderu aplikacji w `Android/data`. Aplikacja na komputer nie zapisuje plików tras

> ℹ️ **Uwaga:** Integracja TAK wymaga określonych ról węzłów. Standardowe węzły klienckie nie uczestniczą automatycznie w operacjach TAK — choć przy włączonym **Konwerter Mesh na CoT** nadal pojawiają się na mapie ATAK jako kontakty.

## Rozwiązywanie problemów

| Problem | Przyczyna | Rozwiązanie |
|---------|-------|----------|
| Węzeł nie pojawia się na mapie ATAK | Złe ustawienie roli urządzenia lub wyłączony Konwerter Mesh na CoT | Ustaw **Rola urządzenia** węzła na TAK lub TAK Tracker. Aby zwykłe węzły (bez roli TAK) się pojawiały, włącz też **Konwerter Mesh na CoT** w **Ustawienia → Zaawansowane → Serwer TAK** |
| Aktualizacje pozycji są nieaktualne | Utracony fix GPS lub zbyt długi interwał | Sprawdź stan GPS; skróć interwał transmisji pozycji w konfiguracji pozycji |
| ATAK pokazuje „disconnected” | Lokalny serwer TAK jest wyłączony lub ATAK wskazuje gdzie indziej | Sprawdź, czy **Włącz lokalny serwer TAK** jest włączone i czy ATAK łączy się z `127.0.0.1:8089` — w razie wątpliwości zaimportuj ponownie wyeksportowany pakiet danych |
| Kształty, znaczniki lub trasy nie są mostkowane | Węzeł wysyłający jest na starszym V1 (firmware 2.7.x lub starsze) | Zaktualizuj firmware węzła wysyłającego do 2.8.0+, aby uzyskać format transmisji V2 |
| Dane CoT nie płyną | Niezgodność kanału | Wszystkie węzły TAK muszą być na tym samym kanale ze zgodnym szyfrowaniem |

## Kwestie bezpieczeństwa

> 🔒 **Prywatność:** Dane TAK udostępniają Twoją pozycję i znak wywoławczy. Moduł TAK respektuje
> to samo szyfrowanie kanału co inne wiadomości MT_SW — w wrażliwych środowiskach używaj
> kanału z kluczem innym niż domyślny.

## Powiązane tematy

- [Ustawienia — Moduły i administracja](settings-module-admin) — konfiguracja modułu TAK
- [Węzły](nodes) — role TAK i TAK Tracker na liście węzłów
- [Mapa i punkty trasy](map-and-waypoints) — pozycje węzłów na mapie
