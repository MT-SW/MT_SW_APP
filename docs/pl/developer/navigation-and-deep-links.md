---
title: Nawigacja i deep linki
parent: Przewodnik dewelopera
nav_order: 4
last_updated: 2026-09-19
description: Jak współpracują typowane trasy Navigation 3 i DeepLinkRouter, obsługiwane URI deep linków oraz jak dodać nowy.
aliases:
  - deeplinks
  - navigation-3
  - routes
---

# Nawigacja i deep linki

Aplikacja używa **Navigation 3** z typowanymi, serializowalnymi trasami i scentralizowanym rozwiązywaniem deep linków.

## Architektura tras

Wszystkie trasy są zdefiniowane w `core/navigation/src/commonMain/kotlin/org/meshtastic/core/navigation/Routes.kt`.

### Hierarchia tras

```kotlin
interface Route : NavKey           // All routes implement NavKey
interface Graph : Route            // Graph roots for navigation hierarchies

@Serializable
sealed interface SettingsRoute : Route {
    @Serializable data class Settings(val destNum: Int? = null) : SettingsRoute, Graph
    @Serializable data object DeviceConfiguration : SettingsRoute
    @Serializable data object HelpDocs : SettingsRoute
    @Serializable data class HelpDocPage(val pageId: String) : SettingsRoute
    // ...
}
```

### Konwencje

- Trasy są `@Serializable` na potrzeby przywracania stanu
- Używaj `data object` dla tras bez parametrów
- Używaj `data class` dla tras z parametrami
- Grupuj powiązane trasy w `sealed interface`
- Punkty wejścia grafów implementują zarówno interfejs trasy, jak i `Graph`

## Router deep linków

`DeepLinkRouter` w `core/navigation` mapuje URI deep linków na typowane listy stosu wstecznego. Każdy obsługiwany link to
wzorzec `UriDeepLinkMatcher` z Navigation 3, którego symbole zastępcze dekodują się do pól docelowej trasy, opakowany w
`withBackStack`, aby zsyntetyzować rodziców.

Wzorce są zakotwiczone, więc ścieżka, której wzorce nie modelują, zwraca null zamiast wracać do korzenia
rodziny: `/firmware/anything-else` nie otwiera już ekranu firmware.

### Format URI

Obie formy są rozwiązywane przez ten sam `DeepLinkRouter`, więc dowolna ścieżka deep linka działa z każdym schematem:

```text
meshtastic://meshtastic/{path}
https://meshtastic.org/{path}       # App Link, android:autoVerify — also opens in-app on a real device/adb
```

`adb shell am start -a android.intent.action.VIEW -d "meshtastic://meshtastic/{path}"` to najszybszy sposób
wywołania dowolnej trasy z tabeli Obsługiwane deep linki z powłoki lub skryptu automatyzacji bez dotykania UI.

Aby forma `https` otwierała się w aplikacji, każdy segment ścieżki najwyższego poziomu musi być też zadeklarowany jako
`android:pathPrefix` w filtrze intencji `android:autoVerify` w `androidApp/src/main/AndroidManifest.xml` —
w przeciwnym razie link otworzy się w przeglądarce. Dodanie nowej trasy najwyższego poziomu wymaga więc trzech kroków: dodaj
segment do `DeepLinkRouter.topLevelPathSegments` (router odmawia obsługi segmentów spoza tego zbioru),
dodaj jego matcher do `DeepLinkRouter.matchers` i dodaj odpowiedni `pathPrefix` do manifestu.
`DeepLinkManifestConsistencyTest` (testy jednostkowe androidApp) sprawdza manifest względem tego zbioru, więc brakujący
wpis w manifeście powoduje błąd CI.

**Źródło prawdy:**

- Zawsze aktualna lista segmentów najwyższego poziomu to `topLevelPathSegments` w
  [`DeepLinkRouter`](https://github.com/meshtastic/Meshtastic-Android/blob/main/core/navigation/src/commonMain/kotlin/org/meshtastic/core/navigation/DeepLinkRouter.kt).
- Podścieżki znajdują się na liście `matchers` oraz w jej pomocniczych mapach (`settingsSubRoutes`, `nodeDetailSubRoutes`).
- KDoc na poziomie klasy w obiekcie `DeepLinkRouter` zawiera przykładowe mapowania, ale jest to ilustracja,
  a nie lista wyczerpująca.
- Wykonywalną specyfikacją jest
  [`DeepLinkRouterTest.kt`](https://github.com/meshtastic/Meshtastic-Android/blob/main/core/navigation/src/commonTest/kotlin/org/meshtastic/core/navigation/DeepLinkRouterTest.kt).
- Poniższa tabela to migawka do szybkiego wglądu — jeśli wygląda na nieaktualną, sprawdź te dwa pliki.

### Obsługiwane deep linki

| Ścieżka URI | Trasa | Uwagi |
|----------|-------|-------|
| `/connections` | `ConnectionsRoute.Connections(null)` | Ekran połączeń |
| `/connections?address={prefixedAddress}` | `ConnectionsRoute.Connections(address)` | Łączy z węzłem po potwierdzeniu przez użytkownika okna „Connect to this device?” — adres używa wewnętrznego formatu aplikacji z prefiksem transportu: `t192.168.1.1:4403` (TCP), `xAA:BB:CC:DD:EE:FF` (BLE), `s/dev/ttyUSB0` (port szeregowy). Przeznaczone dla skryptów/narzędzi AI sterujących aplikacją; kompilacja debug uruchomiona przez swój dostępny tylko z powłoki alias `org.meshtastic.app.AutomationLauncher` z dodatkiem intencji `skip_connect_confirm` lub z argumentem `--skip-connect-confirm` na desktopie stosuje adres, z którym została uruchomiona, bez okna dialogowego; linki, które przychodzą później, nadal pytają. |
| `/connections?address=n` | `ConnectionsRoute.Connections("n")` | Rozłącza bieżący węzeł zamiast się łączyć (`n` = wewnętrzny wartownik „nie wybrano urządzenia”), po tym samym potwierdzeniu. |
| `/wifi-provision` | `WifiProvisionRoute.WifiProvision(null)` | Ekran provisioningu Wi-Fi |
| `/wifi-provision?address={mac}` | `WifiProvisionRoute.WifiProvision(mac)` | Provisioning dla konkretnego MAC węzła |
| `/settings` | `SettingsRoute.Settings(null)` | Korzeń ustawień |
| `/settings/helpDocs` | `SettingsRoute.HelpDocs` | Przeglądarka dokumentacji |
| `/settings/helpDocs/{pageId}` | `SettingsRoute.HelpDocPage(pageId)` | Konkretna strona dokumentacji |
| `/settings/help-docs` | `SettingsRoute.HelpDocs` | Alias zgodności |
| `/discovery` | `DiscoveryRoute.DiscoveryGraph` | Punkt wejścia Local Mesh Discovery |
| `/settings/local-mesh-discovery/session/{sessionId}` | `DiscoveryRoute.DiscoverySummary(sessionId)` | Wynik sesji discovery |
| `/nodes` | `NodesRoute.Nodes` | Lista węzłów |
| `/nodes/{destNum}` | `NodesRoute.NodeDetail(destNum)` | Szczegóły węzła |
| `/nodes/{destNum}/{metric}` | np. `NodeDetailRoute.DeviceMetrics(destNum)` | Konkretna karta metryki węzła (`device-metrics`, `signal`, `power`, `traceroute`, `pax`, `neighbors`, ...) |
| `/messages` | `ContactsRoute.Contacts` | Lista rozmów |
| `/messages/{contactKey}` | `ContactsRoute.Messages(contactKey)` | Konkretna rozmowa |
| `/share?message={text}` | `ContactsRoute.Share(message)` | Kompozytor udostępniania do kontaktu |
| `/quickchat` | `ContactsRoute.QuickChat` | Wybór szybkiego czatu |
| `/map` | `MapRoute.Map(null)` | Widok mapy |
| `/map/{waypointId}` | `MapRoute.Map(waypointId)` | Mapa wycentrowana na waypoincie |
| `/channels` | `ChannelsRoute.Channels` | Lista kanałów |
| `/firmware` | `FirmwareRoute.FirmwareGraph` | Ekran firmware |
| `/firmware/update` | `FirmwareRoute.FirmwareUpdate` | Przepływ aktualizacji firmware |

Adresy trybu Demo zaczynają się od `m`. W kompilacji debug, albo po odblokowaniu ukrytych funkcji, `/connections?address=m` łączy z demonstracyjną siecią mesh, a `/connections?address=mshowcase` z siecią do zrzutów ekranu sklepu, której selektor urządzeń nigdy nie wyświetla. `MockScenario` w `:core:network` zawiera oba.

### Synteza stosu wstecznego

Deep linki syntetyzują pełny stos wsteczny, a nie tylko docelowy ekran:

```kotlin
// /settings/helpDocs/messages-and-channels produces:
listOf(
    SettingsRoute.Settings(null),
    SettingsRoute.HelpDocs,
    SettingsRoute.HelpDocPage("messages-and-channels"),
)
```

Dzięki temu użytkownik może poprawnie nawigować „w górę”.

## Dodawanie deep linka

1. Zdefiniuj typowaną trasę w `Routes.kt`.
2. Dodaj mapowanie w `DeepLinkRouter.settingsSubRoutes` (lub odpowiedniku dla innych grafów) oraz matcher w
   `DeepLinkRouter.matchers`, jeśli kształt ścieżki jest nowy.
3. Dodaj test w `DeepLinkRouterTest.kt`.
4. Zarejestruj wpis nawigacji w odpowiednim module funkcji.
5. Zaktualizuj ilustracyjną listę KDoc w obiekcie `DeepLinkRouter` (komentarz dokumentacyjny na poziomie klasy, a nie własny KDoc metody `route()`) oraz poprzedzającą tabelę — obie są migawkami do szybkiego wglądu, a nie źródłem prawdy. Autorytatywne miejsca znajdziesz na liście Źródło prawdy wcześniej na tej stronie.

## Rejestracja wpisów nawigacji

Każdy moduł funkcji dostarcza wpisy przez funkcję rozszerzającą:

```kotlin
fun EntryProviderScope<NavKey>.docsEntries(backStack: NavBackStack<NavKey>) {
    entry<SettingsRoute.HelpDocs> { DocsBrowserScreen(backStack) }
    entry<SettingsRoute.HelpDocPage> { route -> DocsPageRouteScreen(route.pageId, backStack) }
}
```

Są one wywoływane z kompozycji nawigacji ustawień.

## Testowanie

Routing deep linków jest testowany w:
```text
core/navigation/src/commonTest/kotlin/org/meshtastic/core/navigation/DeepLinkRouterTest.kt
```
