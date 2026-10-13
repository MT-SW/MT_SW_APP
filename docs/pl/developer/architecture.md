---
title: Architektura
parent: Przewodnik dewelopera
nav_order: 1
last_updated: 2026-09-29
description: Jak aplikacje na Androida i komputery dzielą się na androidApp/desktopApp, moduły funkcji i moduły core oraz jak sterowanie radiem i nawigacja są w nich warstwowane.
aliases:
  - layers
  - module-architecture
  - kmp
  - radio-control
---

# Architektura

Aplikacje MT_SW_APP na Androida i komputery stacjonarne mają modularną architekturę Kotlin Multiplatform (KMP) z wyraźnymi granicami warstw. iOS jest celem służącym wyłącznie do walidacji kompilacji; żadna aplikacja na iOS nie jest wydawana.

## Przegląd warstw

```text
┌─────────────────────────────────────────────┐
│          androidApp / desktopApp            │  Platform entry points
├─────────────────────────────────────────────┤
│              feature/* modules               │  UI + Business Logic
├─────────────────────────────────────────────┤
│                core/* modules                │  Shared infrastructure
├─────────────────────────────────────────────┤
│           Platform (Android/JVM/iOS)         │  OS-specific bindings
└─────────────────────────────────────────────┘
```

## Kategorie modułów

### `androidApp/` — aplikacja na Androida

Punkt wejścia aplikacji na Androida:
- Definicje Activity, Application i Manifestu
- Kompozycja modułów DI Koin (`AppKoinModule`)
- Powiązania specyficzne dla flavorów (`google/`, `fdroid/`)
- Integracje tylko dla Androida (widżety, usługi)

### `desktopApp/` — aplikacja desktopowa JVM

Punkt wejścia aplikacji desktopowej (Linux/macOS/Windows):
- Zarządzanie oknami Compose Desktop
- DI specyficzne dla desktopu (`DesktopKoinModule`)
- Zaślepki platformy dla możliwości dostępnych tylko na Androidzie
- `DesktopRadioTransportFactory` i transport szeregowy oparty na jSerialComm; implementacje transportu BLE i TCP, które ta fabryka podpina, to kod współdzielony — znajdują się w `core:network`, zbudowane na prymitywach BLE z `core:ble` — a nie należą do desktopApp

### `feature/*` — moduły funkcji

Każdy moduł `feature/` odpowiada za pionowy wycinek funkcjonalności:

| Moduł | Odpowiedzialność |
|--------|---------------|
| `feature:intro` | Przepływ wprowadzający/powitalny |
| `feature:messaging` | Wiadomości, kanały, kontakty, szybki czat |
| `feature:connections` | Zarządzanie połączeniami Bluetooth/USB/TCP |
| `feature:map` | Wyświetlanie mapy, waypointy — współdzielony stan, polityki i edytor waypointów |
| `feature:map-maplibre` | Powierzchnie map MapLibre — używane przez flavor `fdroid` i Desktop; flavor `google` używa zamiast tego Google Maps. Definicje źródeł kafelków i edytor własnych źródeł są w `feature:map`, więc oba renderery je współdzielą |
| `feature:map-terrain` | Teren offline: dekodowanie wysokości, cieniowanie wzniesień i warstwice, współdzielone przez oba flavory map |
| `feature:node` | Lista węzłów, szczegóły węzła, metryki |
| `feature:settings` | Wszystkie ekrany konfiguracji |
| `feature:firmware` | Przepływ aktualizacji firmware |
| `feature:docs` | Przeglądarka dokumentacji w aplikacji |
| `feature:wifi-provision` | Provisioning Wi-Fi |
| `feature:widget` | Widżety ekranu głównego Androida |
| `feature:discovery` | Wykrywanie sieci mesh |

Moduły funkcji:
- Używają wtyczki konwencji `meshtastic.kmp.feature`
- Zależą od modułów `core`, a nie od innych modułów `feature`; jedyny wyjątek to `feature:map-maplibre`, który bazuje na `feature:map` i `feature:map-terrain`
- Posiadają własne wpisy nawigacji i rejestracje DI
- Zawierają implementacje specyficzne dla platform w `androidMain`/`jvmMain`/`iosMain`

### `core/*` — moduły core

Współdzielona infrastruktura używana przez wszystkie funkcje:

| Moduł | Odpowiedzialność |
|--------|---------------|
| `core:common` | Narzędzia, rozszerzenia, konfiguracja kompilacji |
| `core:navigation` | Trasy, deep linki, Navigation 3 |
| `core:ui` | Współdzielone komponenty Compose, ikony, motyw |
| `core:resources` | Współdzielone zasoby tekstowe |
| `core:model` | Modele domeny |
| `core:data` | Abstrakcje warstwy danych |
| `core:domain` | Przypadki użycia / logika biznesowa |
| `core:database` | Baza danych Room KMP |
| `core:datastore` | Preferencje DataStore |
| `core:prefs` | Preferencje aplikacji |
| `core:repository` | Interfejsy repozytoriów |
| `core:service` | Warstwa usługi mesh |
| `core:di` | Narzędzia DI |
| `core:network` | HTTP/port szeregowy/transport |
| `core:ble` | Abstrakcje Bluetooth LE |
| `core:barcode` | Skanowanie kodów QR / kreskowych (kody QR udostępniania kanałów) |
| `core:nfc` | Obsługa odczytu/zapisu NFC |
| `core:takserver` | Integracja z wbudowanym serwerem TAK |
| `core:testing` | Narzędzia testowe |
| `core:konsist` | Testy architektury/konwencji Konsist |

Modele protobuf pochodzą z zewnętrznego artefaktu Maven `org.meshtastic:protobufs` (przypiętego w `gradle/libs.versions.toml`).

## Zestawy źródeł KMP

Każdy moduł używa standardowej hierarchii zestawów źródeł KMP:

```text
src/
├── commonMain/         ← Shared code (all platforms)
├── commonTest/         ← Shared tests
├── androidMain/        ← Android-specific
├── jvmMain/            ← Desktop JVM-specific
├── jvmAndroidMain/     ← Shared by Android and desktop JVM
├── iosMain/            ← iOS-specific
├── nativeMain/         ← Kotlin/Native code, built for the one iOS target (iosSimulatorArm64)
├── jvmTest/            ← Desktop test host
├── androidHostTest/    ← Android host (JVM) unit tests
└── androidDeviceTest/  ← Instrumented tests (core:database, core:model)
```

`jvmAndroidMain` istnieje tylko w modułach, które stosują `meshtastic.kmp.jvm.android`. `nativeMain`,
`androidHostTest` (moduły wywołujące `withHostTest`) i `androidDeviceTest` istnieją tylko w
modułach, które ich potrzebują.

**Złote zasady:**
- Żadnych importów `android.*` w `commonMain`
- Kod specyficzny dla platformy trafia do odpowiedniego zestawu źródeł
- Dla złożonych zachowań preferuj interfejsy + DI zamiast `expect`/`actual`
- Używaj `expect`/`actual` tylko dla prostych deklaracji

## Wstrzykiwanie zależności

Projekt używa **Koin** z przetwarzaniem adnotacji:
- Adnotacje `@Module`, `@Single`, `@Factory`
- `@ComponentScan` do automatycznej rejestracji
- Moduły funkcji eksportują własną klasę `Feature*Module`
- Aplikacja/Desktop składają wszystkie moduły w swojej głównej konfiguracji DI

## Sterowanie radiem

Funkcje wydają polecenia radiu przez `RadioController` (`core:repository`), złożenie czterech
wyspecjalizowanych pod-interfejsów, dzięki czemu wywołujący mogą zależeć tylko od potrzebnego im wycinka:

| Pod-interfejs | Odpowiedzialność |
|---------------|---------------|
| `AdminController` | Konfiguracja, kanały, właściciel, cykl życia urządzenia, transakcje `editSettings { }` |
| `MessagingController` | Wysyłanie pakietów, reakcje, udostępniane kontakty |
| `NodeController` | Ulubione, ignorowanie, wyciszanie, usuwanie węzłów |
| `QueryController` | Telemetria, traceroute, zapytania o pozycję/informacje o użytkowniku |

`RadioControllerImpl` (`core:service`) jest wewnątrzprocesowym korzeniem kompozycji dla wszystkich
celów (Desktop, iOS, jednoprocesowy Android). Składa cztery pod-kontrolery za pomocą delegacji
interfejsów Kotlina i dodaje zagadnienia przekrojowe (stan połączenia, identyfikator pakietu, lokalizacja,
przełączanie adresu urządzenia). Polecenia to bezpośrednie wywołania suspend; zapisy administracyjne są typu
fire-and-forget, ponieważ źródłem prawdy jest urządzenie (lokalne przechowywanie to optymistyczny cache).
Warstwowy kształt odzwierciedla projekt `AdminApi`/`TelemetryApi` z [meshtastic-sdk](https://github.com/meshtastic/meshtastic-sdk),
aby ułatwić przyszłą migrację do SDK.

## Repozytorium usługi

`ServiceRepository` jest reaktywnym mostem między usługą mesh a wszystkimi warstwami funkcji/UI.
Jest rozłożone na wyspecjalizowane interfejsy dostawców zgodnie z zasadą segregacji interfejsów:

| Interfejs | Odpowiedzialność |
|-----------|---------------|
| `ConnectionStateProvider` | Tylko do odczytu `connectionState: StateFlow<ConnectionState>` |
| `TracerouteResponseProvider` | Stan odpowiedzi traceroute + czyszczenie |
| `NeighborInfoResponseProvider` | Stan odpowiedzi neighbor info + czyszczenie |
| `ServiceStateWriter` | Strona zapisu dla handlerów (set*, emit*, clear*) |

`ServiceRepository` rozszerza wszystkie cztery interfejsy — konsumenci wstrzykują najwęższy interfejs,
jakiego faktycznie potrzebują. Na przykład `ContactsViewModel` wstrzykuje tylko `ConnectionStateProvider`
zamiast całego `ServiceRepository`, co zapobiega przypadkowemu dostępowi do operacji zapisu
z kodu UI. `RadioController` również rozszerza `ConnectionStateProvider`, więc VM-y, które już
wstrzykują pod-interfejs kontrolera, mogą odczytywać stan połączenia bez osobnej zależności.

## Nawigacja

Nawigacja używa **Navigation 3** z typowanymi trasami:
- Wszystkie trasy są zdefiniowane w `core/navigation/Routes.kt`
- Trasy to klasy/obiekty danych z adnotacją `@Serializable`
- Deep linki są rozwiązywane przez `DeepLinkRouter`
- Każda funkcja rejestruje własne wpisy nawigacji

Szczegóły w [Nawigacja i deep linki](navigation-and-deep-links).
