---
title: Trwałe przechowywanie danych
parent: Przewodnik dewelopera
nav_order: 6
last_updated: 2026-10-21
description: Trzy warstwy trwałego przechowywania danych w aplikacji — Room, DataStore i core:prefs — oraz kiedy współtwórca powinien używać każdej z nich.
aliases:
  - room
  - database
  - datastore
  - prefs
---

# Trwałe przechowywanie danych

Trzy warstwy trwałego przechowywania danych w aplikacji — Room, DataStore i `core:prefs` — oraz kiedy współtwórca powinien używać każdej z nich.

## Baza danych Room KMP

**Moduł:** `core:database`

Główny magazyn danych strukturalnych:
- Informacje o węzłach i ich historia
- Historia wiadomości
- Punkty trasy (waypointy)
- Dane telemetryczne
- Konfiguracja zestawu kanałów (nazwy kanałów i konfiguracja LoRa)

### Kluczowe informacje

- Używa Room KMP dla zgodności między platformami
- Migracje są zarządzane przez wbudowany system migracji Room
- Interfejsy DAO znajdują się w `core:database`
- Warstwa repozytoriów w `core:repository` udostępnia publiczne API
- Pełnotekstowe wyszukiwanie wiadomości opiera się na tabeli treści FTS5 (`PacketFts`) nad `Packet`, utrzymywanej w synchronizacji przez wyzwalacze zarządzane przez Room
- `DatabaseManager` trzyma jedną bazę Room na każde podłączone urządzenie, z wypychaniem najdawniej używanych (LRU)
- `DatabaseBackupService` eksportuje bazę urządzenia do pliku wskazanego przez użytkownika oraz przygotowuje zweryfikowane przywrócenie, które zastępuje bazę przy jej następnym otwarciu; przywrócenie nigdy nie jest stosowane do otwartej bazy. `DatabaseBackup.SCHEMA_VERSION` musi być równe wersji z `@Database`, co sprawdza test `jvmTest`

### Co jest przechowywane w Room

| Encja | Opis |
|--------|-------------|
| `NodeEntity` | Wszystkie znane węzły sieci mesh i ich metadane |
| `MyNodeEntity` | Informacje o lokalnym węźle |
| `Packet` | Historia wiadomości (kanałowych i bezpośrednich), alerty, punkty trasy i stan węzłów, czyli porty z `PERSISTED_DATA_PORT_NUMBERS` |
| `PacketFts` | Wirtualna tabela FTS5 odzwierciedlająca `Packet.messageText` na potrzeby pełnotekstowego wyszukiwania wiadomości (wyzwalacze INSERT/UPDATE/DELETE zarządzane przez Room utrzymują ją w synchronizacji) |
| `ContactSettings` | Ustawienia każdej rozmowy: wyciszenie, pozycja odczytu, pominięcie filtra, niewysłany szkic, przypięcie i etykieta wycofanego kanału |
| `ReactionEntity` | Reakcje emoji na wiadomości |
| `MeshLog` | Surowe logi protokołu mesh; historia telemetrii i inne logi poszczególnych węzłów są z niego odczytywane według portu |
| `MetadataEntity` | Metadane urządzenia (wersja firmware, model sprzętu) |
| `ChannelSetEntity` | Zestaw kanałów podłączonego węzła (nazwy kanałów i konfiguracja LoRa), jeden wiersz na urządzenie |
| `QuickChatAction` | Skonfigurowane przez użytkownika wiadomości szybkiego czatu |
| `DeviceHardwareEntity` | Buforowany katalog sprzętu urządzeń |
| `FirmwareReleaseEntity` | Buforowane informacje o wydaniach firmware |
| `TracerouteNodePositionEntity` | Dane pozycji przeskoków traceroute |
| `DiscoverySessionEntity` | Sesja skanowania Local Mesh Discovery (znacznik czasu, zeskanowane presety, preset domowy) |
| `DiscoveryPresetResultEntity` | Wynik dla pojedynczego presetu w sesji discovery |
| `DiscoveredNodeEntity` | Węzły znalezione podczas skanowania presetu discovery |
| `DeviceLinkEntity` | Buforowane linki urządzeń `msh.to` z API Meshtastic |
| `EventFirmwareEditionEntity` | Rekordy wyświetlania firmware wydarzeń, buforowane z API Meshtastic (`/resource/eventFirmware`) |
| `BootloaderOtaQuirksCacheEntity` | Jednowierszowa pamięć podręczna katalogu osobliwości bootloadera/OTA dla nRF52 (`/resource/bootloaderOtaQuirks`), przechowywana jako jedna zserializowana koperta |
| `MaintenanceUf2CacheEntity` | Jednowierszowa pamięć podręczna manifestu maintenance-UF2 (`/resource/maintenanceUf2`), przechowywana jako jedna zserializowana koperta |
| `MergeMarkerEntity` | Oznacza zakończone scalenie `DatabaseMerger`, aby ponowne uruchomienie przy następnym połączeniu pominęło je zamiast duplikować wiersze |

> ℹ️ **Uwaga:** Punkty trasy są przechowywane w encji `Packet` (pole `port_num` odróżnia typy pakietów), wraz z indeksem `channel` zapisującym, z którego kanału korzystał każdy pakiet. Telemetria nie jest pakietem `Packet`: `MeshLogRepository.getTelemetryFrom` odczytuje ją z `MeshLog`. *Konfiguracja* kanałów (nazwy i ustawienia LoRa) znajduje się osobno, w `ChannelSetEntity`.

## Preferencje DataStore

**Moduł:** `core:datastore`

Dla lekkich preferencji klucz-wartość:
- Konfiguracja lokalnego węzła (`LocalConfig`)
- Konfiguracja modułów (`ModuleConfig`)
- Statystyki lokalne
- Ostatnio używane adresy połączonych węzłów
- Zamknięte ostrzeżenia o bootloaderze, osobno dla każdego adresu urządzenia
- Oczekujące odzyskiwanie firmware, po aktualizacji, która mogła zostawić urządzenie w bootloaderze

## Core Prefs

**Moduł:** `core:prefs`

Abstrakcja preferencji wyższego poziomu:
- Ustawienia widoczne dla użytkownika
- Konfiguracja zachowania aplikacji
- Przełączniki funkcji

## Co dokumentacja celowo pomija

Moduł `feature:docs` **nie** używa Room ani żadnej trwałej bazy danych. Dokumentacja jest dostarczana jako zasoby czasu kompilacji, wersjonowane razem z binarką aplikacji, dzięki czemu działa w pełni offline, jest zastępowana przy każdej aktualizacji i nie wymaga strategii migracji. Jedyne, co zapisuje, to plik pamięci podręcznej stron tłumaczonych maszynowo we flavorze `google` (`DocTranslationCache`, w katalogu pamięci podręcznej aplikacji). Każdy wpis ma klucz złożony ze strony, języka i skrótu angielskiego źródła, więc zmieniona strona nie trafia w pamięć podręczną, a sama pamięć jest ograniczona do 50 MB. Opcjonalny stan UX (na przykład ostatnio oglądana strona) mógłby być przechowywany w `core:prefs`, ale nie jest częścią modelu danych dokumentacji.

## Dobre praktyki

- Używaj Room dla danych strukturalnych, o które można pytać i które zmieniają się w czasie działania
- Używaj DataStore dla prostych preferencji i stanu
- Używaj dołączonych zasobów/assetów dla treści statycznych
- Nigdy nie przechowuj danych wrażliwych (kluczy, haseł) w zwykłych tabelach Room
- Zawsze dostarczaj migracje przy zmianach schematu
