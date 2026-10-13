---
title: Przewodnik dewelopera
layout: default
nav_order: 2
has_children: true
---

# Przewodnik dewelopera

Dokumentacja techniczna dla osób współtworzących aplikację MT_SW_APP na Androida i komputery stacjonarne.

## Zanim otworzysz PR

Rzeczy, na których potykają się nowi współtwórcy — sprawdź je, zanim poprosisz o review:

- **Formatowanie przechodzi** — uruchom `./gradlew spotlessApply`, aby sformatować automatycznie, a potem zweryfikuj poleceniem `spotlessCheck`
- **Detekt przechodzi** — uruchom `./gradlew detekt detektTypeResolved` i napraw wszystkie zgłoszone problemy; drugie zadanie uruchamia reguły wymagające classpath kompilacji
- **Wszystkie testy przechodzą** — uruchom `./gradlew test allTests` (potrzebne są oba: `test` obejmuje moduły tylko na Androida, `allTests` obejmuje KMP)
- **Testy zrzutów ekranu przechodzą** — jeśli dotykałeś/aś jakiegokolwiek UI w Compose, uruchom `./gradlew :screenshot-tests:validateDebugScreenshotTest` i w razie potrzeby zaktualizuj obrazy referencyjne
- **Protobufy to zewnętrzna zależność** — modele protobuf pochodzą z artefaktu Maven `org.meshtastic:protobufs` (przypiętego w `gradle/libs.versions.toml`); zmieniaj protobufy upstream i podbij wersję, nigdy nie edytuj wygenerowanego kodu lokalnie
- **Dokumentacja zaktualizowana** — jeśli zmieniłeś/aś widoczne dla użytkownika UI, zaktualizuj odpowiednią stronę w `docs/en/user/`
- **Podglądy zaktualizowane** — jeśli zmieniłeś/aś kompozyty UI, zaktualizuj odpowiedni plik `*Previews.kt` oraz wzorce testów zrzutów ekranu
- **Nazewnictwo gałęzi** — nazwy gałęzi muszą zaczynać się od `feat/`, `fix/`, `chore/`, `docs/`, `build/`, `ci/`, `refactor/`, `test/` lub `deps/`

## Co nowego dla deweloperów

<!-- DEV_WHATS_NEW_START -->
<!-- Add new entries at the top. Format:
**Month YYYY** — [Page or area](relative/path) — One sentence on what changed architecturally or procedurally.
Keep the last 5–8 entries and trim older ones from the bottom.
-->

**Wrzesień 2026** — [Nawigacja i deep linki](developer/navigation-and-deep-links) — Deep linki są kierowane przez `UriDeepLinkMatcher` z Navigation 3 zamiast ręcznie pisanego bloku `when`, a wzorce są zakotwiczone, więc nieopisana końcówka ścieżki nie otwiera już korzenia danej rodziny.

**Wrzesień 2026** — [Pomiary i formatowanie](developer/measurement) — `NumberFormatter.format` stosuje ustawienia regionalne systemu; `formatInvariant` to wersja ze stałą kropką, przeznaczona dla danych, które parsuje inny system.

**Wrzesień 2026** — [Styl dokumentacji](developer/documentation-style) — Sekcja 11 standardów projektowych Meshtastic jest teraz przewodnikiem stylu dla `docs/en/`; ta strona zachowuje tylko mechanikę repozytorium, formę admonicji w rendererze w aplikacji oraz reguły prozy, których standardy nie rozstrzygają.

**Sierpień 2026** — [Styl dokumentacji](developer/documentation-style) — Nowa strona: wewnętrzny przewodnik stylu prozy w `docs/en/`, z identyfikatorami reguł, listą słownictwa projektu i uzasadnieniem każdej konwencji.

**Sierpień 2026** — Źródła kafelków map to jeden wspólny katalog w `feature/map` (`MapTileCatalogue`, `RasterTileSpec`), więc oba flavory rysują te same rastrowe mapy bazowe i nakładki z jednej definicji.

**Sierpień 2026** — Obie mapy rysują własną ikonę zaimportowanego obiektu i nakładają obraz KMZ `GroundOverlay` w jego `LatLonBox` (łącznie z obrotem) — MapLibre przez czworokąt `ImageSource`, Google przez `GroundOverlayOptions` (#3786).

**Sierpień 2026** — Pobieranie paczek map offline jest uzależnione od `offlineMapsSupported`, ponieważ API offline MapLibre kompiluje się na Desktopie, ale nic tam po cichu nie pobiera.

**Sierpień 2026** — Nowy moduł `feature/map-maplibre`: flavor F-Droid i Desktop renderują teraz wszystkie powierzchnie map (główną mapę, ślad węzła, traceroute, discovery, wbudowaną minimapę) przez `maplibre-compose` z jednego modułu multiplatformowego, a `osmdroid` zniknął. Wspólne reguły, na które muszą się zgadzać oba renderery, znajdują się w klasach polityk w `feature/map`.

<!-- DEV_WHATS_NEW_END -->
