---
title: Transport
parent: Przewodnik dewelopera
nav_order: 5
last_updated: 2026-10-21
description: Transporty BLE, USB serial i TCP za abstrakcją RadioTransport — który moduł jest właścicielem każdego z nich i jak dodać nowy.
aliases:
  - ble
  - serial
  - tcp
  - radio-transport
---

# Transport

Aplikacja komunikuje się z węzłem przez trzy transporty (BLE, USB serial i TCP) za jedną abstrakcją.

## Abstrakcja transportu

Warstwa transportu jest wyabstrahowana przez interfejsy zdefiniowane w `core:repository` (`RadioTransport`,
`RadioTransportFactory`, `RadioInterfaceService`). Konkretna implementacja każdego transportu znajduje się w
`core:network`: `BleRadioTransport` (BLE), `TcpRadioTransport` (TCP) i `SerialRadioTransport` (USB serial
na Androidzie), obok `MockRadioTransport` i `ReplayRadioTransport` do celów deweloperskich. `core:ble` dostarcza
niskopoziomowe prymitywy połączenia BLE (skanowanie, GATT, okablowanie Kable), na których opiera się
`BleRadioTransport` — sam nie implementuje `RadioTransport`. Dzięki temu aplikacja działa tak samo niezależnie od
typu połączenia.

```text
Feature UI → RadioController → RadioInterfaceService → RadioTransport (BLE | Serial | TCP)
```

Każdy adres transportu zaczyna się jednoznakowym `InterfaceId`: `x` BLE (akceptowane jest też `!`), `s`
port szeregowy, `t` TCP, `m` atrapa (mock), `r` odtwarzanie (replay) i `n` dla braku wybranego urządzenia. `RadioTransportFactory.isAddressValid` sprawdza ten
prefiks, zanim nastąpi próba połączenia.

## Bluetooth Low Energy (BLE)

**Moduł:** `core:network` (implementacja `BleRadioTransport`) zbudowany na `core:ble` (prymitywy połączenia BLE)  
**Platformy:** Android, Desktop (JVM przez Kable); kod kompiluje się dla iOS, ale żadna aplikacja iOS nie jest dostarczana

Podstawowy transport dla urządzeń mobilnych, dostępny także na desktopie:
- Wykrywanie usług GATT Meshtastic
- Odczyt/zapis pakietów protobuf oparty na charakterystykach
- Zarządzanie stanem połączenia i automatyczne ponowne łączenie
- Negocjacja MTU dla optymalnych rozmiarów pakietów

### Gdzie jest kod

- `BleRadioTransport` (`core:network`) — implementacja `RadioTransport` dla BLE
- `core:ble` — skanowanie BLE, połączenie i operacje GATT (`BleScanner`, `BluetoothRepository`,
  `KableBleConnection`), z których korzysta `BleRadioTransport`
- Skaner i połączenie Kable to współdzielony kod `commonMain`; `androidMain` i `jvmMain` zawierają `BluetoothRepository` każdej platformy, konfigurację Kable i ograniczanie częstości skanowania, a Android dodaje parowanie (bonding) i odświeżanie pamięci podręcznej GATT

## USB Serial

**Moduł:** `core:network`  
**Platformy:** Android (OTG), Desktop

Komunikacja szeregowa przez USB:
- Używa biblioteki `usb-serial-for-android` na Androidzie
- Bezpośredni dostęp do portu szeregowego na Desktopie (JVM)
- Tabela sond dla obsługiwanych identyfikatorów producenta/produktu USB
- Automatyczne wykrywanie po podłączeniu urządzenia USB

### Gdzie jest kod

- `SerialRadioTransport` (`core:network`, `androidMain`) — implementacja `RadioTransport` dla Androida
- `SerialTransport` (`core:network`, `jvmMain`) — wspólny transport portu szeregowego, na którym opiera się Desktop
- `DesktopRadioTransportFactory` (`desktopApp`) — tworzy `SerialTransport`, który używa jSerialComm, dla adresów szeregowych Desktopa

## TCP/IP

**Moduł:** `core:network`  
**Platformy:** Android, Desktop (iOS: kod się kompiluje, ale nie ma celu aplikacji iOS ani `RadioTransportFactory` — zobacz [Fabryka transportu](#transport-factory))

Transport sieciowy dla węzłów z Wi-Fi:
- Połączenie gniazda TCP z adresem IP węzła
- Domyślny port: 4403
- Węzły w sieci lokalnej znajdowane przez mDNS (`_meshtastic._tcp`): `NsdManager` na Androidzie, JmDNS na Desktopie
- Używany w programowaniu z meshtasticd i symulowanymi węzłami
- Dostępny, gdy BLE/USB jest niepraktyczne

## Fabryka transportu

Interfejs `RadioTransportFactory` abstrahuje tworzenie transportu:

```kotlin
interface RadioTransportFactory {
    val supportedDeviceTypes: List<DeviceType>

    /** Whether the virtual demo transports (`m` mock / `r` replay) may be offered and bound right now. */
    val mockTransportEnabled: StateFlow<Boolean>

    /** Whether this build ships a packet capture to replay, rather than degrading to plain mock. */
    val isReplayTransportAvailable: Boolean

    fun createTransport(address: String, service: RadioInterfaceService): RadioTransport
    fun isAddressValid(address: String?): Boolean
    fun toInterfaceAddress(interfaceId: InterfaceId, rest: String): String
}
```

`mockTransportEnabled` jest przepływem, a nie jednorazowym sprawdzeniem: tryb demo na Androidzie może zostać włączony
w trakcie sesji, a lista urządzeń musi to zauważyć. Każdy konsument musi czytać ten jeden przepływ — ścieżka widoczności i ścieżka dopuszczenia
`isAddressValid` muszą się zgadzać, inaczej pozycja demo pojawi się, a potem odmówi połączenia.

`BaseRadioTransportFactory` (`core:network`, `commonMain`) zawiera wspólną połowę: tworzy
`BleRadioTransport` dla adresów BLE, dopuszcza każdy typ adresu w `isAddressValid` i przekazuje każdy inny
adres do `createPlatformTransport` danej platformy. Na sprzęcie bez Bluetooth LE odrzuca adresy BLE,
a fabryka Androida odrzuca adresy szeregowe na sprzęcie bez hosta USB, więc zapisany adres przywrócony na takie urządzenie
jest zachowany, ale nigdy nie jest uzbrajany.

Implementacje specyficzne dla platform:
- **Android:** `AndroidRadioTransportFactory` (`core:network`, `androidMain`) obsługuje BLE + USB + TCP; tryb demo jest włączony w kompilacjach debug i w Firebase Test Lab
- **Desktop:** `DesktopRadioTransportFactory` (`desktopApp`) obsługuje BLE (Kable) + USB + TCP; tryb demo to funkcja kompilacji debug, a transport replay nie jest podłączony
- **iOS:** kod kompiluje się dla BLE i TCP, ale nie istnieje żadna implementacja `RadioTransportFactory` i nie jest dostarczana żadna aplikacja iOS

## Cykl życia połączenia

1. **Wykrywanie** — znajdowanie dostępnych węzłów (skan BLE / wykrycie USB / mDNS lub ręcznie wpisany adres TCP)
2. **Połączenie** — nawiązanie łącza z wybranym węzłem
3. **Handshake** — wymiana informacji o węźle i konfiguracji
4. **Aktywne** — normalna wymiana wiadomości
5. **Rozłączenie** — czyste zamknięcie lub odzyskiwanie po błędzie

Stan na poziomie aplikacji to `ConnectionState` (`Disconnected`, `Connecting`, `Connected`, `DeviceSleep`), odczytywany
przez `ConnectionStateProvider`. `MeshConnectionManager` zapisuje `Connecting`, `DeviceSleep` i
`Disconnected`, uzgadniając zdarzenia transportu z polityką uśpienia węzła, a `MeshConfigFlowManager`
zapisuje `Connected`, gdy handshake się zakończy. Kod UI obserwuje ten stan, nigdy
`RadioInterfaceService.connectionState`.

## Dodawanie nowego transportu

1. Zaimplementuj interfejs `RadioTransport`
2. Nadaj mu prefiks `InterfaceId`, dopuść go w `isAddressValid` i utwórz w `createPlatformTransport` danej platformy
3. Dodaj UI połączenia w `feature:connections`
4. Zaktualizuj powiązania DI dla platformy
