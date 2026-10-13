---
title: Transport
parent: Przewodnik dewelopera
nav_order: 5
last_updated: 2026-08-29
description: Transporty BLE, USB serial i TCP za abstrakcją RadioTransport — który moduł jest właścicielem każdego z nich i jak dodać nowy.
aliases:
  - ble
  - serial
  - tcp
  - radio-transport
---

# Transport

Aplikacja komunikuje się z węzłem przez trzy transporty — BLE, USB serial i TCP — za jedną abstrakcją.

## Abstrakcja transportu

Warstwa transportu jest wyabstrahowana przez interfejsy zdefiniowane w `core:repository` (`RadioTransport`,
`RadioTransportFactory`, `RadioInterfaceService`). Konkretna implementacja każdego transportu znajduje się w
`core:network`: `BleRadioTransport` (BLE), `TcpRadioTransport` (TCP) i `SerialRadioTransport` (USB serial
na Androidzie), obok `MockRadioTransport` i `ReplayRadioTransport` do celów deweloperskich. `core:ble` dostarcza
niskopoziomowe prymitywy połączenia BLE (skanowanie, GATT, okablowanie Kable), na których opiera się
`BleRadioTransport` — sam nie implementuje `RadioTransport`. Dzięki temu aplikacja działa tak samo niezależnie od
typu połączenia.

```text
App ← RadioController → Transport (BLE | Serial | TCP)
```

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
- Implementacje specyficzne dla platform w `androidMain` i `jvmMain` (Kable)

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
- `DesktopRadioTransportFactory` (`desktopApp`) — spina połączenie szeregowe Desktopa oparte na jSerialComm

## TCP/IP

**Moduł:** `core:network`  
**Platformy:** Android, Desktop (iOS: kod się kompiluje, ale nie ma celu aplikacji iOS ani `RadioTransportFactory` — zobacz [Fabryka transportu](#transport-factory))

Transport sieciowy dla węzłów z Wi-Fi:
- Połączenie gniazda TCP z adresem IP węzła
- Domyślny port: 4403
- Używany w programowaniu z symulowanymi węzłami
- Dostępny, gdy BLE/USB jest niepraktyczne

## Fabryka transportu

Interfejs `RadioTransportFactory` abstrahuje tworzenie transportu:

```kotlin
interface RadioTransportFactory {
    val supportedDeviceTypes: List<DeviceType>

    /** Whether the virtual demo transports (`m` mock / `r` replay) may be offered right now. */
    val mockTransportEnabled: StateFlow<Boolean>

    /** Whether this build ships a packet capture to replay, rather than degrading to plain mock. */
    val isReplayTransportAvailable: Boolean

    fun createTransport(address: String, service: RadioInterfaceService): RadioTransport
    fun isAddressValid(address: String?): Boolean
    fun toInterfaceAddress(interfaceId: InterfaceId, rest: String): String
}
```

`mockTransportEnabled` jest przepływem, a nie jednorazowym sprawdzeniem: gest trybu demo na Androidzie (pięć stuknięć
w wiersz wersji aplikacji w Ustawieniach) odblokowuje transporty demo w trakcie sesji, a lista urządzeń
musi to zauważyć. Każdy konsument musi czytać ten jeden przepływ — ścieżka widoczności i ścieżka dopuszczenia
`isAddressValid` muszą się zgadzać, inaczej pozycja demo pojawi się, a potem odmówi połączenia.

Implementacje specyficzne dla platform:
- **Android:** obsługuje BLE + USB + TCP
- **Desktop:** obsługuje BLE (Kable) + USB + TCP
- **iOS:** kod kompiluje się dla BLE i TCP, ale nie istnieje żadna implementacja `RadioTransportFactory` i nie jest dostarczana żadna aplikacja iOS

## Cykl życia połączenia

1. **Wykrywanie** — skanowanie dostępnych węzłów (skan BLE / wykrycie USB / ręczne TCP)
2. **Połączenie** — nawiązanie łącza z wybranym węzłem
3. **Handshake** — wymiana informacji o węźle i konfiguracji
4. **Aktywne** — normalna wymiana wiadomości
5. **Rozłączenie** — czyste zamknięcie lub odzyskiwanie po błędzie

## Dodawanie nowego transportu

1. Zaimplementuj interfejs `RadioTransport`
2. Zarejestruj w `RadioTransportFactory` specyficznej dla platformy
3. Dodaj UI połączenia w `feature:connections`
4. Zaktualizuj powiązania DI dla platformy
