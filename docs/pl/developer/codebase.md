---
title: Baza kodu
parent: Przewodnik dewelopera
nav_order: 2
last_updated: 2026-10-21
description: Układ repozytorium, przestrzenie nazw pakietów i system budowania Gradle — wtyczki konwencji, warianty kompilacji i kluczowe zadania.
aliases:
  - repository-layout
  - project-structure
  - source-code
---

# Baza kodu

Układ repozytorium, konwencje przestrzeni nazw i przegląd systemu budowania.

## Struktura repozytorium

```text
Meshtastic-Android/
├── androidApp/             # Android application module
│   ├── src/main/           # Shared Android code
│   ├── src/google/         # Google Play flavor (proprietary Google integrations — Gemini, Maps, Play services)
│   └── src/fdroid/         # F-Droid flavor (FOSS-only)
├── desktopApp/             # Desktop JVM application
├── feature/                # Feature modules (KMP, except widget)
│   ├── intro/
│   ├── messaging/
│   ├── connections/
│   ├── map/
│   ├── map-maplibre/
│   ├── map-terrain/
│   ├── coverage/
│   ├── node/
│   ├── settings/
│   ├── firmware/
│   ├── docs/
│   ├── wifi-provision/
│   ├── widget/             # Android library
│   └── discovery/
├── core/                   # Core infrastructure modules (KMP, except barcode and nfc)
│   ├── barcode/            # Android library
│   ├── ble/
│   ├── common/
│   ├── data/
│   ├── database/
│   ├── datastore/
│   ├── di/
│   ├── domain/
│   ├── konsist/
│   ├── model/
│   ├── navigation/
│   ├── network/
│   ├── nfc/                # Android library
│   ├── prefs/
│   ├── repository/
│   ├── resources/
│   ├── service/
│   ├── takserver/
│   ├── testing/
│   └── ui/
├── baselineprofile/        # Baseline Profile generation for :androidApp
├── screenshot-tests/       # Compose Preview screenshot tests (visual-regression gate)
├── docs-screenshots/       # Doc-framed composition screenshots (generate-only, not CI-gated)
├── store-screenshots/      # Store-listing screenshots captured from the real apps (instrumented)
├── schema-strings/         # JVM generator for schema_strings.xml from the protobufs field metadata
├── build-logic/            # Convention plugins and build helpers
│   ├── convention/
│   └── settings-plugin/    # meshtastic.develocity settings plugin
├── docs/                   # Documentation source (markdown)
│   └── en/                 # English source; other locales live under docs/<locale>/user/
│       ├── user/
│       └── developer/
├── gradle/                 # Gradle wrapper and version catalog
│   └── libs.versions.toml
├── specs/                  # Feature specifications
└── .github/workflows/      # CI/CD workflows
```

## Konwencja przestrzeni nazw

Wszystkie pakiety Kotlin mają wzorzec:
```text
org.meshtastic.{layer}.{module}.{subpackage}
```

Przykłady:
- `org.meshtastic.core.navigation` — moduł nawigacji core
- `org.meshtastic.feature.docs.ui` — pakiet UI funkcji docs
- `org.meshtastic.app.di` — konfiguracja DI aplikacji

## System budowania

### Gradle Kotlin DSL

Wszystkie pliki budowania używają Kotlin DSL (`.gradle.kts`). Konfiguracja:

- **Katalog wersji:** `gradle/libs.versions.toml`
- **Wtyczki konwencji:** `build-logic/convention/`
- **Ustawienia:** `settings.gradle.kts`

### Wtyczki konwencji

Znajdują się w `build-logic/convention/src/main/kotlin/`. Pełny zestaw jest zarejestrowany w
`build-logic/convention/build.gradle.kts`; poniżej te, które build modułu stosuje najczęściej:

| Wtyczka | Przeznaczenie |
|--------|---------|
| `meshtastic.kmp.feature` | Konfiguracja modułu funkcji: stosuje `meshtastic.kmp.library`, `meshtastic.kmp.library.compose` i `meshtastic.koin` oraz dodaje zależności Compose, Navigation 3, Lifecycle i Koin potrzebne każdej funkcji |
| `meshtastic.kmp.library` | Współdzielony moduł biblioteki KMP |
| `meshtastic.kmp.library.compose` | Biblioteka KMP, która dostarcza także UI w Compose |
| `meshtastic.kmp.jvm.android` | Dodaje zestaw źródeł `jvmAndroidMain` współdzielony przez cele Android i desktopowy JVM |
| `meshtastic.koin` | Koin Annotations + wtyczka kompilatora K2 |
| `meshtastic.kotlinx.serialization` | Konfiguracja wtyczki serializacji |
| `meshtastic.android.room` | Konfiguracja Room KMP i lokalizacja schematu |
| `meshtastic.android.screenshot` | Testowanie zrzutów ekranu Compose Preview |

Pozostałe obejmują warianty aplikacji i bibliotek, lint, detekt, spotless, Dokka, Kover,
AboutLibraries, analitykę, sekrety, zadania dokumentacji i zbiorczy root — przeczytaj blok `register(…)`,
zamiast zakładać, że dana wtyczka istnieje lub nie istnieje.

### Warianty kompilacji (Android)

| Flavor | Opis |
|--------|-------------|
| `google` | Dystrybucja Google Play; zawiera własnościowe API |
| `fdroid` | Dystrybucja F-Droid; tylko zależności FOSS |

### Kluczowe zadania Gradle

```shell
# Compile every KMP module's commonMain metadata, JVM main, and iosSimulatorArm64 main and test sources (excludes :desktopApp), and assemble the device-test APKs
./gradlew kmpSmokeCompile

# Run all tests: allTests covers KMP modules, test covers Android/JVM-only modules; run both
./gradlew test allTests

# Code quality
./gradlew spotlessCheck detekt detektTypeResolved

# Android build
./gradlew assembleGoogleDebug assembleFdroidDebug

# Desktop run
./gradlew :desktopApp:run

# Desktop native installers for the current OS (DMG / MSI+EXE / DEB+RPM+AppImage)
./gradlew :desktopApp:packageReleaseDistributionForCurrentOS

# API reference (Dokka HTML → build/dokka/html)
./gradlew :dokkaGeneratePublicationHtml
```

## Najważniejsze pozycje katalogu wersji

Kluczowe zależności w `gradle/libs.versions.toml`:

| Kategoria | Biblioteka |
|----------|---------|
| Compose | Compose Multiplatform (JetBrains) |
| Nawigacja | Navigation 3 |
| DI | Koin (adnotacje) |
| Serializacja | kotlinx.serialization |
| Baza danych | Room KMP |
| Sieć | Ktor |
| Markdown | multiplatform-markdown-renderer |
| BLE | Kable |
| Port szeregowy | usb-serial-for-android (Android), jSerialComm (Desktop) |
| Testowanie | kotlin-test, Kotest, Mokkery, Turbine, Compose Multiplatform UI test |
