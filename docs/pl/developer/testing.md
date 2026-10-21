---
title: Testowanie
parent: Przewodnik dewelopera
nav_order: 7
last_updated: 2026-10-21
description: Strategia testowania projektu Meshtastic KMP — kategorie testów, potok zrzutów ekranu, profile bazowe i integracja z CI.
aliases:
  - tests
  - unit-tests
  - screenshot-tests
---

# Testowanie

Strategia i praktyki testowania projektu Meshtastic KMP.

## Kategorie testów

### Testy jednostkowe KMP (`commonTest`)

Wspólne testy pisane raz i uruchamiane na JVM oraz, w modułach deklarujących `withHostTest`, jako testy hosta Androida. `commonTest` kompiluje się także dla iOS, ale wykonywanie testów iOS jest wyłączone, więc żaden test iOS nie jest uruchamiany:

```shell
./gradlew allTests
```

- Testy logiki biznesowej
- Walidacja modeli danych
- Testy algorytmów wyszukiwania i rankingu
- Testy serializacji tras

### Testy hosta Androida

Testy specyficzne dla Androida, uruchamiane na JVM. W czystych modułach Android/JVM (takich jak `androidApp`) uruchamia je `test`; w modułach KMP deklarujących `withHostTest {}` uruchamia je `allTests` przez `testAndroidHostTest`:

```shell
./gradlew test       # pure Android/JVM modules
./gradlew allTests   # KMP modules
```

- Testy ViewModeli
- Testy repozytoriów z atrapami Room (fakes)
- Testy integracyjne specyficzne dla Androida

### Testy UI Compose

Framework testów UI Compose Multiplatform:

```kotlin
@Test
fun myScreenTest() = runComposeUiTest {
    setContent { MyScreen() }
    onNodeWithText("Expected").assertIsDisplayed()
}
```

Znajdują się w zestawach źródeł `commonTest` lub `jvmTest`.

### Testy zrzutów ekranu

Wykorzystują natywny framework testów zrzutów ekranu (layoutlib) z Android Gradle Plugin, podzielony na dwa moduły:

- **`:screenshot-tests`** — **bramka regresji wizualnej**. CI uruchamia na nim `validateDebugScreenshotTest`; zmiana kadrowania jednego z tych wzorców to realna różnica do przejrzenia. Zawiera atomowe komponenty o podwójnym przeznaczeniu.
- **`:docs-screenshots`** — **tylko generowanie**, *nie* jest walidowany w CI. Zawiera kompozycje kadrowane pod dokumentację, dopasowane do witryny z dokumentacją, więc zmiana kadru obrazu w dokumentacji nigdy nie poruszy bramki regresji.

```shell
./gradlew :screenshot-tests:updateDebugScreenshotTest    # record regression goldens
./gradlew :screenshot-tests:validateDebugScreenshotTest  # compare against goldens (CI gate)
./gradlew :docs-screenshots:updateDebugScreenshotTest    # record doc-framed composition images
./gradlew :screenshot-tests:copyDocsScreenshots          # copy doc images from BOTH modules into docs/assets
```

Renderowanie odbywa się na hoście przez layoutlib, więc wzorce nagrane lokalnie są tymi, względem których waliduje CI. `validate` dopuszcza próg różnicy obrazu 0.0005, ustawiony w `AndroidScreenshotConventionPlugin`, ponieważ porównanie co do piksela zawodzi dla niektórych wzorców. W `docs/assets/screenshots/README.md` opisano, do którego modułu należy nowy zrzut ekranu.

#### Zrzuty ekranu do sklepów

Zrzuty ekranu do wizytówek w sklepach (Play, F-Droid, IzzyOnDroid oraz wizytówka aplikacji desktopowej we Flathub) są robione z prawdziwych aplikacji, połączonych z ukrytą siecią pokazową trybu demo (`/connections?address=mshowcase`, `MockScenario.SHOWCASE` w `:core:network`), a nie rysowane. Każdy ekran jest osiągany przez swój deep link, więc przebieg nie zależy od języka interfejsu, a każde ujęcie jest zachowywane dopiero wtedy, gdy okno przestanie się zmieniać.

- **Android: `:store-screenshots`**, moduł testowy UiAutomator 2.4 skierowany na `:androidApp`. Dla każdej powierzchni, którą wysyła `fastlane supply`, ustawia rozmiar i gęstość ekranu, ponownie uruchamia aplikację debug przez jej dostępny tylko z powłoki alias `AutomationLauncher` z `skip_onboarding` i `skip_connect_confirm`, i zapisuje pięć zrzutów do wizytówki, pełnoekranowych, z paskiem stanu w trybie demo SystemUI, w `/data/local/tmp/store-screenshots/<flavor>` na urządzeniu.
- **Desktop: `store-screenshots/capture-desktop.sh`** uruchamia prawdziwą kompilację debug aplikacji desktopowej na wyświetlaczu Xvfb, po jednym uruchomieniu na ekran z deep linkiem tego ekranu, i zapisuje pięć zrzutów dla Flathub. Mapa wymaga renderera OpenGL z Skiko, a Skiko odrzuca każdy adapter GL o nazwie `llvmpipe` lub `virgl`, więc Mesa uruchamia GL przez zink nad lavapipe.

Na emulatorze lub urządzeniu, jeden wariant (flavor) naraz:

```shell
./gradlew :store-screenshots:connectedFdroidDebugAndroidTest
adb pull /data/local/tmp/store-screenshots/fdroid/. fastlane/metadata/android/en-US/images/
```

| Folder | Rozmiar | Okno | Wysyłane przez |
| --- | --- | --- | --- |
| `phoneScreenshots/` | 1080×1920 @400 dpi | kompaktowe: dolny pasek nawigacji | `fastlane supply` |
| `sevenInchScreenshots/` | 1080×1920 @288 dpi | średnie: szyna nawigacji, jeden panel | `fastlane supply` |
| `tenInchScreenshots/` | 2560×1440 @320 dpi | rozszerzone: szyna, lista obok szczegółów | `fastlane supply` |
| `desktopApp/packaging/linux/screenshots/` | 1280×800 | rozszerzone: szyna, lista obok szczegółów | Flathub, przez zasoby wydania, które wskazuje `metainfo.xml` |

`.github/workflows/store-screenshots.yml` uruchamia oba na hostowanych runnerach, każdy wariant Androida w osobnym zadaniu na własnym emulatorze przez `store-screenshots/capture-android.sh` (google dla wizytówki Play, fdroid dla zatwierdzonego drzewa), przy każdym wydaniu wewnętrznym, na żądanie oraz w pull requestach dotykających renderera lub sieci pokazowej. Potok wydania dołącza zrzuty do wydania, publikuje z nich wizytówkę Play w wydaniu produkcyjnym i otwiera samoscalający się PR, który zapisuje zestawy fdroid i desktop z powrotem tutaj (`RELEASE_PROCESS.md`).

### Profil bazowy (Baseline Profile) / wydajność startu

Moduł `:baselineprofile` (#5735) generuje [Baseline Profile](https://developer.android.com/topic/performance/baselineprofiles/overview) dla `:androidApp`, kompilując AOT gorące ścieżki startu, tak aby ART nie płacił kosztu JIT przy pierwszym uruchomieniu. Profiluje wariant `google` (wariant używany przez większość użytkowników), a wynik trafia do obu wariantów.

Generator Macrobenchmark (`BaselineProfileGenerator`) oraz benchmark przed/po (`StartupBenchmark`) znajdują się w `baselineprofile/src/main/kotlin/org/meshtastic/baselineprofile/`. Oba uruchamia się na urządzeniu/emulatorze:

```shell
./gradlew :androidApp:generateBaselineProfile                          # Generate the profile (commit the output)
./gradlew :baselineprofile:connectedGoogleBenchmarkReleaseAndroidTest  # Quantify the cold-start win
```

Wygenerowany profil jest scalany do `androidApp/src/main/generated/baselineProfiles/` (`mergeIntoMain` w `androidApp/build.gradle.kts`), więc kompilacje wydaniowe fdroid i google pakują go przez `androidx.profileinstaller`. `scheduled-baseline.yml` generuje go ponownie codziennie na emulatorze i otwiera pull request, gdy się zmieni.

> ℹ️ **Uwaga:** Ścieżka obejmuje tylko zimny start (uruchomienie → pierwsza klatka), ponieważ uprząż testowa nie łączy się z żadnym węzłem. Ekrany po połączeniu (lista węzłów, mapa, wątek wiadomości) nie są kompilowane AOT.

Rozszerzenie ścieżki poza zimny start wymaga, aby uprząż testowa łączyła się z węzłem, tak jak `:store-screenshots` łączy się z siecią pokazową trybu demo.

## Organizacja testów

```text
feature/my-feature/src/
├── commonTest/kotlin/org/meshtastic/feature/myfeature/
│   ├── MyBusinessLogicTest.kt
│   └── MyModelTest.kt
└── jvmTest/kotlin/org/meshtastic/feature/myfeature/
    └── MyDesktopSpecificTest.kt
```

## Wytyczne dotyczące testowania

### Należy

- Pisać testy w `commonTest`, gdy to możliwe (działają na JVM oraz jako test hosta Androida, jeśli moduł deklaruje `withHostTest`)
- Testować logikę biznesową niezależnie od UI
- Używać fake'ów/stubów zamiast mocków, gdzie to praktyczne
- Testować przypadki brzegowe: stany puste, stany błędów, wartości graniczne
- Testować routing deep linków w `DeepLinkRouterTest`
- Utrzymywać szybkie testy — bez sieci i bez operacji dyskowych I/O w testach jednostkowych

### Nie należy

- Nie testować zachowania frameworków (wnętrza Compose, zapytania Room)
- Nie tworzyć testów zależnych od innych modułów funkcji
- Nie używać `Thread.sleep` — używać testowych dispatcherów korutyn
- Nie polegać na kolejności wykonywania testów

## Uruchamianie testów

```shell
# All tests: allTests covers KMP modules, test covers Android/JVM-only modules; run both
./gradlew test allTests

# Specific module
./gradlew :feature:docs:allTests

# Code quality
./gradlew spotlessCheck detekt detektTypeResolved

# Full verification
./gradlew spotlessCheck detekt detektTypeResolved assembleDebug kmpSmokeCompile test allTests
```

## Integracja z CI

Testy uruchamiają się automatycznie przy:
- Pull requestach, których zmiany pasują do filtra ścieżek `android` lub `desktop` (`pull-request.yml`)
- Kolejce scalania (`merge-queue.yml`)
- Pushach do `main` (`main-check.yml`)

Każdy z nich wywołuje `reusable-check.yml`. Shardy testów kompilują się z `-PwarningsAsErrors=true`, więc
ostrzeżenie Kotlina, które przechodzi w lokalnej kompilacji, psuje CI, jeśli nie podasz tej samej flagi.

Zadania jednorunnerowe w `reusable-check.yml` działają na przypiętej etykiecie Ubuntu LTS x64 z JDK 25 i cache'owaniem Gradle. Dwa zadania używają macierzy: `test-shards` dzieli się na `shard-core`, `shard-feature` i `shard-app`, a `build-desktop` działa na macOS, Windows i Linux x64 oraz arm64, na etykietach wymienionych w jego własnej macierzy. Weryfikacja Flatpak to osobny workflow, nie zadanie tutaj. Przypięta etykieta arm Ubuntu LTS oraz runnery `ubuntu-slim` oparte na kontenerach obsługują lekkie zadania. Reguła czterech poziomów jest w `.skills/testing-ci/SKILL.md`.
