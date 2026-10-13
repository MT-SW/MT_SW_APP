---
title: Współtworzenie
parent: Przewodnik dewelopera
nav_order: 8
last_updated: 2026-09-28
description: Nazewnictwo gałęzi, styl commitów, przebieg PR i bramki weryfikacyjne, które zmiana musi przejść przed scaleniem.
aliases:
  - contributing
  - pull-request
  - branch-naming
---

# Współtworzenie

Wytyczne dotyczące współtworzenia projektu Meshtastic Android/Desktop (bazy kodu KMP, która kompiluje się także dla iOS).

## Nazewnictwo gałęzi

Gałęzie używają prefiksów w stylu conventional commits:

| Prefiks | Do czego |
|--------|---------|
| `feat/<scope>` | Nowe zachowanie widoczne dla użytkownika |
| `fix/<scope>` | Poprawki błędów |
| `refactor/<scope>` | Zmiany struktury kodu |
| `chore/<scope>` | Narzędzia, zależności, CI, porządki |
| `docs/<scope>` | Wyłącznie dokumentacja |
| `build/<scope>` | Zmiany systemu budowania |
| `ci/<scope>` | Zmiany workflow CI |
| `test/<scope>` | Dodawanie lub poprawianie testów |
| `deps/<scope>` | Aktualizacje zależności |

Prefiksy specyfikacji oparte na znaczniku czasu (`YYYYMMDD-HHMMSS-feature-name`, tworzone przez `/speckit.git.feature`) są również dozwolone dla prac prowadzonych według specyfikacji.

Przykłady:
- `feat/desktop-ble-transport`
- `fix/bluetooth-reconnect`
- `20260601-074653-air-quality-telemetry`

## Przebieg pracy

1. **Zrób fork** repozytorium (zewnętrzni współtwórcy) lub utwórz gałąź (opiekunowie).
2. **Zaimplementuj** zmiany zgodnie z wytycznymi architektury.
3. **Przetestuj** lokalnie: `./gradlew spotlessCheck detekt detektTypeResolved kmpSmokeCompile test allTests`
4. **Zrób commit** z jasnymi, opisowymi komunikatami.
5. **Wypchnij** zmiany i otwórz pull request.

## Komunikaty commitów

Stosuj styl conventional commits:
```text
feat(docs): add in-app documentation browser
fix(ble): handle reconnection timeout
refactor(navigation): migrate to typed routes
test(search): add keyword ranking tests
```

## Lista kontrolna pull requesta

Przed wysłaniem:
- [ ] Kod kompiluje się na wszystkich celach: `./gradlew kmpSmokeCompile`
- [ ] Wszystkie testy przechodzą: `./gradlew allTests`
- [ ] Styl kodu przechodzi: `./gradlew spotlessCheck`
- [ ] Analiza statyczna przechodzi: `./gradlew detekt detektTypeResolved`
- [ ] Nowy kod ma odpowiednie pokrycie testami
- [ ] Brak importów `android.*` w `commonMain`
- [ ] Moduły Koin zarejestrowane, jeśli dodano nowe DI
- [ ] Trasy dodane do `Routes.kt`, jeśli wprowadzono nową nawigację
- [ ] Dokumentacja zaktualizowana, jeśli zmienia się zachowanie widoczne dla użytkownika

## Styl kodu

- **Formatowanie:** wymuszane przez Spotless (reguły KtLint)
- **Analiza statyczna:** Detekt z konfiguracją specyficzną dla projektu
- **Importy:** bez importów z wildcardem; porządkowane automatycznie przez Spotless
- **Długość linii:** maksymalnie 120 znaków

Uruchom formatowanie:
```shell
./gradlew spotlessApply
```

## Zasady architektury

- Moduły funkcji nie mogą zależeć od innych modułów funkcji
- `commonMain` nie może zawierać `android.*`, `java.io.*` ani importów specyficznych dla platformy
- Dla złożonych zachowań platformowych preferuj interfejs + DI zamiast `expect`/`actual`
- Wszystkie trasy nawigacji muszą być `@Serializable` i zdefiniowane w `Routes.kt`
- Do wstrzykiwania zależności używaj adnotacji Koin (`@Single`, `@Factory`, `@Module`)

## Weryfikacja

Pełna weryfikacja przed scaleniem:
```shell
./gradlew spotlessCheck detekt detektTypeResolved kmpSmokeCompile test allTests
```

Przy zmianach dotyczących dokumentacji uruchom dodatkowo:
```shell
./gradlew generateDocsBundle validateDocsBundle
```

Proza w `docs/en/` stosuje się do Sekcji 11 [Meshtastic Client Design Standards](https://github.com/meshtastic/design/tree/master/standards) — zobacz [Styl dokumentacji](documentation-style), aby poznać zasady specyficzne dla tego repozytorium.

## Uzyskiwanie pomocy

- [Meshtastic Discord](https://discord.gg/meshtastic) — kanał `#app-development`
- GitHub Issues — zgłoszenia błędów i propozycje funkcji
- GitHub Discussions — pytania i pomysły
