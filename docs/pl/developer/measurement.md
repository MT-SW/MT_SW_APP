---
title: Pomiary i formatowanie
parent: Przewodnik dewelopera
nav_order: 9
last_updated: 2026-10-21
description: Jak MetricFormatter i NumberFormatter formatują pomiary oraz jak aplikacja wybiera jednostki metryczne lub imperialne na podstawie ustawień regionalnych i preferencji użytkownika.
aliases:
  - measurement
  - metric-formatter
  - number-formatter
---

# Pomiary i formatowanie

Jak aplikacja Meshtastic Android/KMP formatuje liczby, jednostki i wartości zależne od ustawień regionalnych.

## Przegląd

Wszystkie dane pomiarowe przesyłane przez węzły Meshtastic używają **jednostek metrycznych** (metry, °C, hPa, m/s itd.). Aplikacja przelicza i formatuje te wartości do wyświetlenia za pomocą tych narzędzi:

| Narzędzie | Lokalizacja | Przeznaczenie |
|---|---|---|
| `MetricFormatter` | `core/common/.../util/MetricFormatter.kt` | Przelicza i formatuje wielkości fizyczne (temperatura, ciśnienie, prędkość itd.) |
| `NumberFormatter` | `core/common/.../util/NumberFormatter.kt` | Formatowanie dziesiętne: `format` podąża za ustawieniami regionalnymi systemu, `formatInvariant` zachowuje stały separator kropkę |
| `formatMeasure` | `core/common/.../util/MeasureFormatting.kt` | Łączy stały angielski symbol jednostki (`MeasureUnitKind`) z liczbą sformatowaną regionalnie, dla jednostek, które się przeliczają |
| `toDistanceString` i pokrewne | `core/model/.../util/DistanceExtensions.kt` | Teksty odległości, wysokości, prędkości i małych odległości, renderowane przez CLDR tam, gdzie platforma potrafi, a ręcznie tam, gdzie nie |

Narzędzia z `core/common` znajdują się w `org.meshtastic.core.common.util` i kompilują się dla każdego celu KMP (Android, Desktop, iOS). iOS jest celem tylko do kompilacji: jego formatowanie regionalne wraca do postaci niezmiennej.

## API MetricFormatter

`MetricFormatter` to `object` Kotlina z czystymi funkcjami dla każdego typu pomiaru:

```kotlin
object MetricFormatter {
    fun degreeSymbol(isFahrenheit: Boolean): String
    fun temperature(celsius: Float, isFahrenheit: Boolean): String
    fun voltage(volts: Float, decimalPlaces: Int = 2): String
    fun current(milliAmps: Float, decimalPlaces: Int = 1): String
    fun percent(value: Float, decimalPlaces: Int = 1): String
    fun percent(value: Int): String
    fun humidity(value: Float): String
    fun pressure(hPa: Float, decimalPlaces: Int = 1): String
    fun snr(value: Float?, decimalPlaces: Int = 1): String
    fun rssi(value: Int?): String
    fun windSpeed(metersPerSecond: Float, isImperial: Boolean, decimalPlaces: Int = 1): String
    fun rainfall(millimeters: Float, isImperial: Boolean, decimalPlaces: Int = 1): String
    fun weight(kilograms: Float, isImperial: Boolean, decimalPlaces: Int = 2): String
}
```

`snr` i `rssi` przyjmują odczyt dopuszczający null i renderują symbol zastępczy `—`, gdy go brak. 0 dB i 0 dBm to prawdziwe odczyty, więc nigdy nie zastępują brakującego.

### Użycie

Wyniki pokazane dla ustawień regionalnych `en-US`:

```kotlin
// Temperature — Fahrenheit conversion is handled automatically
MetricFormatter.temperature(22.5f, isFahrenheit = true)  // "72.5°F"
MetricFormatter.temperature(22.5f, isFahrenheit = false)  // "22.5°C"

// Signal metrics
MetricFormatter.snr(-5.2f)    // "-5.2 dB"
MetricFormatter.rssi(-97)     // "-97 dBm"

// Environment
MetricFormatter.pressure(1013.25f)  // "1,013.3 hPa"
MetricFormatter.humidity(65.0f)     // "65%"
MetricFormatter.windSpeed(3.7f, isImperial = false)  // "13.3 km/h"
MetricFormatter.windSpeed(3.7f, isImperial = true)   // "8.3 mph"
MetricFormatter.rainfall(12.3f, isImperial = false)  // "12.3 mm"
MetricFormatter.rainfall(12.3f, isImperial = true)   // "0.5 in"

// Power
MetricFormatter.voltage(3.95f)      // "3.95 V"
MetricFormatter.current(125.0f)     // "125.0 mA"
```

## NumberFormatter

`NumberFormatter` ma dwie połówki, a wybór niewłaściwej to błąd, którego należy unikać:

```kotlin
object NumberFormatter {
    fun format(value: Double, decimalPlaces: Int): String           // follows the OS locale
    fun format(value: Float, decimalPlaces: Int): String            // follows the OS locale
    fun formatInvariant(value: Double, decimalPlaces: Int): String  // fixed dot separator
    fun formatInvariant(value: Float, decimalPlaces: Int): String   // fixed dot separator
    fun parseDecimalOrNull(text: String): Double?                   // reads a comma or dot decimal
}
```

**Używaj `format` do wszystkiego, co czyta człowiek.** Deleguje do własnego formatera liczb platformy
przez `expect`/`actual` (`java.text.NumberFormat` na JVM i Androidzie), ze znakami dziesiętnym i grupowania
właściwymi dla ustawień regionalnych, dzięki czemu liczba czyta się tak, jak dany użytkownik zapisuje liczby. `NaN` i nieskończoność są renderowane jako `—`.

**Używaj `formatInvariant` do wszystkiego, co parsuje maszyna** — ładunku CoT czytanego przez innego klienta lub
wartości zapisanej i odczytanej z powrotem. Lokalizowanie ich zamienia `1.5` w `1,5` i psuje czytnik.
Zaokrągla połówki od zera, aby odpowiadać ścieżce regionalnej, co ma znaczenie, ponieważ firmware raportuje SNR
w krokach co ćwierć dB, a remisy są rutyną.

`parseDecimalOrNull` to odpowiednik po stronie wejścia: przyjmuje liczbę wpisaną z separatorem dziesiętnym
ustawień regionalnych klawiatury, np. `48,21`, którą `String.toDoubleOrNull` odrzuca.

## Przeliczanie jednostek

Cztery pomiary z `MetricFormatter` są przeliczane z metrycznych do wyświetlenia, każdy sterowany flagą logiczną, którą wywołujący bierze z `LocaleUnitsProvider`:

| Pomiar | Flaga | Źródło | Przeliczenie |
|---|---|---|---|
| `temperature` | `isFahrenheit` | `LocaleUnitsProvider.temperatureUnit` | `°F = °C × 1.8 + 32` |
| `windSpeed` | `isImperial` | `LocaleUnitsProvider.measurementSystem` | m/s × 3.6 → km/h, lub × 2.23694 → mph |
| `rainfall` | `isImperial` | `LocaleUnitsProvider.measurementSystem` | mm ÷ 25.4 → in |
| `weight` | `isImperial` | `LocaleUnitsProvider.measurementSystem` | kg × 2.20462 → lb |

Rozwiązanie po stronie systemu operacyjnego za tymi flagami to dwie osobne funkcje w `core/common/.../util/MeasurementSystem.kt`, `getSystemMeasurementSystem()` i `getSystemTemperatureUnit()`. Są celowo oddzielne: niektóre ustawienia regionalne mieszają systemy (Wielka Brytania używa mil dla odległości, ale stopni Celsjusza dla temperatury), więc temperatura nigdy nie może być wyprowadzana z jednostki odległości. Na Androidzie `getSystemTemperatureUnit()` deleguje do `androidx.core.text.util.LocalePreferences`, które rozwiązuje dane regionalne CLDR i respektuje nadpisanie temperatury w preferencjach regionalnych Androida 14+.

Wybór w aplikacji **Jednostki** (`UnitsOverride`, zapisany w `UiPrefs`) jest wczytywany przez `LocaleUnitsProvider`, który jest jedynym miejscem, z którego kod wyświetlania pobiera jednostki. Reguła Konsist (`MeasurementSystemSourceTest`) trzyma bezpośrednie odczyty rozwiązania systemowego poza resztą bazy kodu, ponieważ bezpośredni odczyt podąża za ustawieniami regionalnymi, ale ignoruje to ustawienie. Wymuszony system niesie ze sobą swoją temperaturę (metryczny → °C, imperialny → °F), nadpisując nawet jawną regionalną preferencję temperatury systemu.

`getSystemMeasurementSystem()` rozwiązuje ustawienia regionalne w tej kolejności (temperatura jest osobna: jak opisano wcześniej, `getSystemTemperatureUnit()` czyta regionalną preferencję temperatury przez `LocalePreferences`, dzieli tylko uzupełnianie regionu i w ostateczności używa Celsjusza):

1. Rozszerzenie Unicode `ms` (preferencja systemu miar z Androida 16+) wygrywa bezwarunkowo.
2. Ustawienie regionalne bez regionu — selektor języka w aplikacji oferuje same tagi, np. `en` — bierze region z konfiguracji systemu, zamiast pozwalać ICU zgadywać go z języka.
3. Wszystko, co nadal nie jest sklasyfikowane, wraca do metrycznego, nigdy do imperialnego.

Implementacje Android i Desktop współdzielą tabelę regionów i czytnik nadpisania w `commonMain`, więc oba klienty nie mogą się różnić co do tych samych ustawień regionalnych.

Wszystko inne, co formatuje `MetricFormatter` (napięcie, prąd, ciśnienie, SNR, RSSI, wilgotność, procent) wyświetla się w natywnych jednostkach metrycznych. Strona dla użytkowników [Jednostki i ustawienia regionalne](../user/units-and-locale) wyjaśnia, co widzą użytkownicy końcowi.

## Odległość, wysokość i prędkość

Długości i prędkości przechodzą przez funkcje rozszerzające w `core/model/.../util/DistanceExtensions.kt`, które biorą `MeasurementSystem` z `LocaleUnitsProvider`:

| Funkcja | Renderuje |
|---|---|
| `Int.toDistanceString` | Odległość w metrach, jako m lub km, albo ft lub mi, zależnie od wielkości |
| `Int.toElevationString` | Wysokość w pełnych metrach lub stopach przy dowolnej wielkości |
| `Float.toSpeedString` | Prędkość w m/s, jako km/h lub mph |
| `Float.toSmallDistanceString` | Milimetry, jako mm lub cale |
| `Float.toStormDistanceString` | Pełne kilometry albo mile z jednym miejscem po przecinku |

Każda z nich najpierw pyta funkcje `expect` w `LocalizedUnitFormatting.kt` (`formatLengthLocalized`, `formatElevationLocalized`, `formatSpeedLocalized`, `formatRainfallLocalized`), które renderują przez dane ICU platformy, z cyframi, symbolem i odstępami według CLDR. Zwracają null tam, gdzie platforma nie potrafi odpowiedzieć: na Desktopie i iOS, na wersjach Androida bez potrzebnego im API ICU oraz dla wartości nieskończonej lub NaN. Wywołujący renderuje wtedy własną wersję zapasową przez `formatMeasure`. Żadna z nich nie używa zastosowania `road` z CLDR, którego zaokrąglanie pokazałoby węzeł oddalony o 87 m jako 90 m.

## Dodawanie nowego typu pomiaru

Aby dodać nowy formater pomiaru:

1. **Dodaj funkcję do `MetricFormatter`** w `core/common/src/commonMain/kotlin/org/meshtastic/core/common/util/MetricFormatter.kt`. Przyjmij wartość w jednostce, którą wysyła węzeł; na przykład pole protobuf `EnvironmentMetrics.radiation` jest w µR/h:

   ```kotlin
   fun radiation(microRoentgensPerHour: Float, decimalPlaces: Int = 2): String =
       "${NumberFormatter.format(microRoentgensPerHour, decimalPlaces)} µR/h"
   ```

2. **Dodaj testy** w `core/common/src/commonTest/`:

   ```kotlin
   @Test
   fun radiationFormatting() {
       assertEquals("0.15 µR/h", MetricFormatter.radiation(0.15f))
       assertEquals("1.23 µR/h", MetricFormatter.radiation(1.234f))
   }
   ```

3. **Użyj w UI** — wywołaj z dowolnego composable lub ViewModelu w `commonMain`:

   ```kotlin
   envMetrics.radiation?.let { Text(text = MetricFormatter.radiation(it)) }
   ```

4. **Uruchom weryfikację**:

   ```shell
   ./gradlew :core:common:allTests
   ```

## DateFormatter

Formatowanie daty i godziny używa `expect object` `DateFormatter` z implementacjami `actual` specyficznymi dla platform. Formaty na Androidzie, wszystkie w ustawieniach regionalnych urządzenia:

| Funkcja | Format |
|---|---|
| `formatRelativeTime()` | Skrócony czas względny z dokładnością do minuty, z `DateUtils.getRelativeTimeSpanString` |
| `formatDateTime()` | Data i godzina ze skróconą nazwą miesiąca, z `DateUtils.formatDateTime` |
| `formatShortDate()` | Krótka godzina w ciągu ostatnich 24 godzin, w przeciwnym razie krótka data |
| `formatTime()` | Krótka godzina |
| `formatTimeWithSeconds()` | Średnia godzina, z sekundami |
| `formatDate()` | Krótka data |
| `formatDateTimeShort()` | Krótka data i średnia godzina |

W przeciwieństwie do `MetricFormatter`, `DateFormatter` jest zadeklarowany z `expect`/`actual` (`expect object` w `commonMain`, `actual object` na platformę), ponieważ formatowanie dat z natury zależy od platformowych API ustawień regionalnych. Desktop używa formaterów `java.time`; wersja zastępcza na iOS zwraca puste ciągi.

## Decyzje projektowe

| Decyzja | Uzasadnienie |
|---|---|
| Formatowanie wyświetlania podąża za ustawieniami regionalnymi systemu | Liczba pokazana użytkownikowi powinna czytać się tak, jak on zapisuje liczby |
| `formatInvariant` zachowuje stałą `.` dla wartości czytanych maszynowo | Ładunki interoperacyjne i ponownie parsowane wartości psują się po zlokalizowaniu |
| `MetricFormatter` przelicza tylko temperaturę, prędkość wiatru, opady i wagę; długości i prędkości są przeliczane w `DistanceExtensions.kt` | Pozostałe jednostki metryczne są powszechnie zrozumiałe w swojej natywnej formie |
| Wzorzec singletona `object` | Bezstanowe narzędzie — brak potrzeby zarządzania instancjami |

## Powiązane

- **Dokumentacja dla użytkowników**: [Jednostki i ustawienia regionalne](../user/units-and-locale) wyjaśnia, co widzą użytkownicy końcowi
- **Kod źródłowy**: `core/common/src/commonMain/kotlin/org/meshtastic/core/common/util/MetricFormatter.kt`
- **Testy**: `core/common/src/commonTest/kotlin/org/meshtastic/core/common/util/MetricFormatterTest.kt`
