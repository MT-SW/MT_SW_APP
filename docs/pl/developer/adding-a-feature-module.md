---
title: Dodawanie modułu funkcji
parent: Przewodnik dewelopera
nav_order: 3
last_updated: 2026-10-21
description: Przewodnik krok po kroku tworzenia nowego modułu funkcji KMP — katalog modułu, skrypt budowania, DI, trasy, wpisy nawigacji i lista kontrolna.
aliases:
  - new-module
  - feature-module
  - module-guide
---

# Dodawanie modułu funkcji

Przewodnik krok po kroku tworzenia nowego modułu funkcji KMP w projekcie Meshtastic.

## Utwórz katalog modułu

```shell
mkdir -p feature/my-feature/src/{commonMain,commonTest,androidMain,jvmMain,iosMain}/kotlin/org/meshtastic/feature/myfeature
```

## Utwórz `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.meshtastic.kmp.feature)
}

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        namespace = "org.meshtastic.feature.myfeature"
        withHostTest { isIncludeAndroidResources = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.navigation)
            implementation(projects.core.resources)
            implementation(projects.core.ui)
            implementation(projects.core.di)
        }

        commonTest.dependencies {
            implementation(libs.compose.multiplatform.ui.test)
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}
```

`meshtastic.kmp.feature` deklaruje cel `jvm()` oraz `iosSimulatorArm64()` na komputerach, które potrafią uruchomić
Kotlin/Native. Dodaje zależności Compose, Navigation 3, Lifecycle i Koin oraz umieszcza `core:testing` w `commonTest`.
Jeśli nie ustawisz przestrzeni nazw, wyprowadza też przestrzeń nazw Androida ze ścieżki modułu, a dla ścieżki z myślnikiem,
takiej jak `:feature:my-feature`, nie jest to poprawna nazwa pakietu, więc ustaw `namespace` samodzielnie.

## Zarejestruj w `settings.gradle.kts`

Dodaj swój moduł do głównego bloku `include()`:

```kotlin
include(
    // ...existing modules...
    ":feature:my-feature",
)
```

## Utwórz moduł DI

`src/commonMain/kotlin/org/meshtastic/feature/myfeature/di/FeatureMyFeatureModule.kt`:

```kotlin
package org.meshtastic.feature.myfeature.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("org.meshtastic.feature.myfeature")
class FeatureMyFeatureModule
```

## Zarejestruj DI w aplikacji/Desktopie

Dodaj swój moduł do:
- `androidApp/src/main/kotlin/org/meshtastic/app/di/AppKoinModule.kt`
- `desktopApp/src/main/kotlin/org/meshtastic/desktop/di/DesktopKoinModule.kt`

## Dodaj trasy nawigacji

W `core/navigation/src/commonMain/kotlin/org/meshtastic/core/navigation/Routes.kt`:

```kotlin
@Serializable
sealed interface MyFeatureRoute : Route {
    @Serializable data object MyFeatureGraph : MyFeatureRoute, Graph
    @Serializable data object MyFeatureHome : MyFeatureRoute
}
```

Nowy zapieczętowany interfejs tras trzeba też zarejestrować w `MeshtasticNavSavedStateConfig`
(`core/navigation/.../NavigationConfig.kt`) przez `subclassesOfSealed<MyFeatureRoute>()`, bo inaczej zapisanie
stosu wstecz nie zserializuje jego tras. Dodaj egzemplarz każdej nowej trasy do `allRouteInstances` w
`NavigationConfigTest`, który sprawdza każdą trasę w obie strony przez zapisany stan.

## Utwórz wpisy nawigacji

`src/commonMain/kotlin/org/meshtastic/feature/myfeature/navigation/MyFeatureNavigation.kt`:

```kotlin
package org.meshtastic.feature.myfeature.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.meshtastic.core.navigation.MyFeatureRoute

fun EntryProviderScope<NavKey>.myFeatureGraph(backStack: NavBackStack<NavKey>) {
    entry<MyFeatureRoute.MyFeatureGraph> {
        MyFeatureScreen(onNavigateUp = { backStack.removeLastOrNull() })
    }
    entry<MyFeatureRoute.MyFeatureHome> {
        MyFeatureScreen(onNavigateUp = { backStack.removeLastOrNull() })
    }
}
```

Zarówno wartownik grafu (`MyFeatureRoute.MyFeatureGraph`), jak i główny ekran (`MyFeatureRoute.MyFeatureHome`)
prowadzą do tego samego kompozytu, więc funkcja jest osiągalna zarówno przez wypchnięcie na najwyższym poziomie, jak i przez
wypchnięcie grafu deep linka — według tego samego wzorca, którego używają `feature:wifi-provision` i `feature:firmware`.

Następnie podepnij ją: wywołaj `myFeatureGraph(backStack)` z bloku `entryProvider<NavKey> { }` w
`Main.kt` aplikacji `androidApp` oraz z `desktopNavGraph` w `DesktopNavigation.kt` aplikacji `desktopApp`, obok funkcji wpisów
innych modułów funkcji. Zobacz [Rejestracja wpisów nawigacji](navigation-and-deep-links#navigation-entry-registration).

## Wytyczne dotyczące zestawów źródeł

| Zestaw źródeł | Zawiera |
|-----------|----------|
| `commonMain` | Modele, ViewModele, współdzielone UI, moduł DI, nawigację |
| `androidMain` | Implementacje specyficzne dla Androida (np. API platformy) |
| `jvmMain` | Implementacje specyficzne dla desktopu |
| `iosMain` | Implementacje specyficzne dla iOS |
| `commonTest` | Współdzielone testy jednostkowe |

## Oczekiwania dotyczące testów

Każdy moduł funkcji powinien mieć:
- Testy jednostkowe w `commonTest` dla logiki biznesowej
- Testy UI z użyciem `compose-multiplatform-ui-test` tam, gdzie to stosowne
- Brak zależności testowej od innych modułów funkcji

## Lista kontrolna

- [ ] Katalog modułu utworzony
- [ ] `build.gradle.kts` z poprawnymi wtyczkami i zależnościami
- [ ] Dodany do `settings.gradle.kts`
- [ ] Dodany do `ALL_MODULES_FULL` w `build-logic/convention/src/main/kotlin/RootConventionPlugin.kt`; `python3 scripts/check-module-list.py` kończy się błędem, gdy moduł niewyłączony brakuje na tej liście, gdy wpis nie występuje w `include` w `settings.gradle.kts` albo gdy moduł wyłączony zostanie dodany ponownie, a moduł niewyłączony, którego na niej brakuje, jest pomijany w agregacji Dokka i Kover oraz w `kmpSmokeCompile`
- [ ] Dodany do shardu testów w `.github/workflows/reusable-check.yml` (`shard-feature` dla modułu funkcji): jego zadanie `:feature:my-feature:allTests` w `tasks` i jego `koverXmlReport` w `kover`; `python3 scripts/check-test-shards.py` kończy się błędem dla modułu bez zadania testowego w żadnym shardzie, chyba że moduł jest wymieniony w zbiorach `COVERED_ELSEWHERE` lub `NO_TESTS_YET` tego skryptu
- [ ] Jeśli moduł jest widoczny dla użytkownika: strona w `docs/en/user/` oraz wpis `MODULE_TO_DOCS` dla niego w `scripts/check-doc-coverage.js`; sprawdzenie kończy się błędem, gdy brakuje strony wymienionego modułu, a moduł bez wpisu nie wymaga strony
- [ ] Moduł DI utworzony z `@ComponentScan`
- [ ] Moduł DI zarejestrowany w korzeniach aplikacji i desktopu
- [ ] Trasy dodane do `Routes.kt`, zapieczętowany interfejs zarejestrowany w `NavigationConfig.kt`, a egzemplarz każdej trasy dodany w `NavigationConfigTest`
- [ ] Wpisy nawigacji zarejestrowane
- [ ] `./gradlew kmpSmokeCompile` przechodzi
- [ ] `./gradlew :feature:my-feature:allTests` przechodzi
