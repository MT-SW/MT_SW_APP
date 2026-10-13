---
title: Telemetria i czujniki
parent: Przewodnik użytkownika
nav_order: 9
last_updated: 2026-09-18
description: Dane z czujników w sieci mesh — obsługiwane czujniki środowiska, jakości powietrza i zasilania oraz poradniki konfiguracji i podglądu.
aliases:
  - sensors
  - environment
  - weather
  - power-metrics
---

# Telemetria i czujniki

Węzły mogą zbierać dane z czujników i udostępniać je w sieci mesh. Telemetria pozwala węzłom wyposażonym w czujniki rozsyłać informacje o środowisku, zasilaniu i stanie urządzenia, widoczne na ekranie szczegółów węzła i zapisywane w czasie.

## Telemetria urządzenia

Wszystkie węzły raportują podstawową telemetrię urządzenia:

| Wartość | Opis | Typowy zakres |
|--------|-------------|---------------|
| Poziom baterii | Procent naładowania | 0–100% |
| Napięcie | Napięcie baterii | 3,0–4,2 V (LiPo) |
| ChUtil | % lokalnego czasu antenowego w użyciu | 0–100% |
| AirUtil | % ostatniej godziny, który ten węzeł spędził na nadawaniu | 0–100% |
| Czas pracy | Sekundy od ostatniego uruchomienia | Różnie |

## Czujniki środowiska

Obsługiwane czujniki środowiskowe:

### Temperatura i wilgotność

| Czujnik | Temperatura | Wilgotność | Ciśnienie | Uwagi |
|--------|-------------|----------|----------|-------|
| BME280 | ✓ | ✓ | ✓ | Polecany, wszystko w jednym |
| BME680 | ✓ | ✓ | ✓ | Dodaje rezystancję gazu/IAQ |
| SHT31 | ✓ | ✓ | — | Wysoka dokładność |
| MCP9808 | ✓ | — | — | Precyzyjna temperatura |
| LPS22 | — | — | ✓ | Tylko ciśnienie |

### Jakość powietrza

| Czujnik | Wartość | Uwagi |
|--------|--------|-------|
| BME680 | Rezystancja gazu / IAQ | Lotne związki organiczne |
| PMSA003I | PM1.0, PM2.5, PM10 | Zobacz [Metryki jakości powietrza](#air-quality-metrics) |
| SEN55 | PM, temp., wilgotność | Czujnik wielofunkcyjny. Jego wskaźniki NOx i VOC są zapisywane i dołączane do eksportu CSV, ale nie są pokazywane jako karty ani wykresy |
| SEN6x | PM, CO₂, temp., wilgotność | Jego rejestr statusu jest zawsze eksportowany; gdy zgłasza usterkę (wentylator, RH&T, gaz, CO₂, HCHO lub PM), nazwy usterek pojawiają się na karcie Status czujnika oraz przy każdym wpisie dziennika jakości powietrza |

### Gleba

| Wartość | Jednostka | Uwagi |
|--------|------|-------|
| Temp. gleby | °C / °F | Raportowana razem z wilgotnością gleby przez sondy glebowe |
| Wilgotność gleby | % | Objętościowa zawartość wody |

Obie wartości pojawiają się jako karty informacyjne na ekranie szczegółów węzła, obok pozostałych odczytów środowiska.

Chemia z sond glebowych i sond jakości wody (pH, przewodność, zasolenie, NPK, tlen rozpuszczony, ORP, mętność, azotany, amon, zapotrzebowanie na tlen, natężenie promieniowania słonecznego) pojawia się w wierszu kart informacyjnych **Gleba i woda** (Soil & Water) na ekranie szczegółów węzła, gdy tylko węzeł zgłosi którąkolwiek z tych wartości, i jest dołączana do eksportu bazy węzłów. Nie ma dla nich jeszcze wykresu ani ekranu dziennika.

### Światło i UV

| Czujnik | Wartość |
|--------|--------|
| OPT3001 | Światło otoczenia (lux) |
| VEML7700 | Światło otoczenia (lux) |
| LTR390 | Indeks UV |

### Pogoda i inne odczyty

| Wartość | Jednostka | Gdzie się pojawia |
|--------|------|------------------|
| Prędkość wiatru | km/h lub mph | Karta i wykres. Czujniki podają metry na sekundę; aplikacja przelicza na jednostkę z Twoich ustawień, a wykres używa tej samej jednostki co karta |
| Kierunek wiatru, porywy i zastoje wiatru (lull) | stopnie, km/h lub mph | Wypisane przy każdym odczycie na ekranie Metryki środowiskowe; bez wykresu |
| Opady deszczu, ostatnia godzina i ostatnie 24 godziny | mm lub in | Wypisane przy każdym odczycie na ekranie Metryki środowiskowe; bez wykresu |
| Wyładowania, uderzenia w ostatniej godzinie i odległość burzy | liczba, km lub mi | Karta i wypisane przy każdym odczycie na ekranie Metryki środowiskowe; bez wykresu. Z detektora AS3935. Odległość burzy jest zawsze w km lub mi, ponieważ detektor podaje całe kilometry |
| Promieniowanie | µR/h | Karta i wykres |
| Waga | kg lub lb | Tylko karta — czujniki tensometryczne, np. waga ula |
| Odległość | mm lub in | Tylko karta — poziom wody, z czujnika odległości |
| Punkt rosy | °C lub °F | Tylko karta — obliczany z temperatury i wilgotności |
| Temperatura 1-Wire | °C lub °F | Karta i wykres, do ośmiu sond typu DS18B20 |
| Napięcie ADC | V | Karta i wykres, do ośmiu surowych kanałów analogowych |

## Metryki zasilania

Węzły z czujnikami zasilania serii INA mogą raportować:

| Wartość | Opis |
|--------|-------------|
| Napięcie | Odczyt napięcia dla każdego kanału |
| Prąd | Pobór prądu dla każdego kanału, w mA |

Ekran szczegółów węzła pokazuje karty tylko do odczytu dla kanałów od 1 do 3. Przyciskiem wykresu w wierszu **Metryki zasilania** otwierasz ekran wykresu, który wyświetla chip dla każdego kanału, który przysłał dane — do ośmiu — i rysuje wykres wybranego. Tam, w polu etykiety pod chipami, możesz nazwać kanał, np. Solar albo Bateria. Nie ma osobnego odczytu mocy w watach; aplikacja rysuje napięcie i prąd i nie wylicza z nich mocy.

Przydatne do monitorowania ładowania słonecznego lub stanu baterii w odległych węzłach.

## Konfiguracja telemetrii

1. Przejdź do **Ustawienia → Ustawienia modułu → Telemetria**.
2. Każda grupa wartości ma własny przełącznik włączenia i własny interwał:
   - **Metryka urządzenia** — bateria, napięcie, czas pracy, ChUtil i AirUtil. Jej przełącznik, **Wysyłaj telemetrię urządzenia**, pojawia się tylko w firmware 2.7.12 i nowszym; w starszym firmware możesz zmienić interwał, ale nie wyłączysz grupy
   - **Metryki środowiskowe** — temperatura, wilgotność, ciśnienie i inne odczyty z czujników
   - **Metryki jakości powietrza** — odczyty pyłów i CO₂
   - **Metryki zasilania** — odczyty napięcia i prądu dla każdego kanału

   Środowisko i zasilanie mają dodatkowy przełącznik pokazywania odczytów na własnym
   ekranie radia, a środowisko ma jeszcze jeden, który pokazuje tam temperatury w Fahrenheitach.

### Wybór interwału

To wartości nominalne, a nie sztywne harmonogramy. W przeciążonej sieci mesh firmware automatycznie
wydłuża interwały w zależności od liczby węzłów online, więc nie musisz
dobierać ich ręcznie do wielkości sieci. Wydłużaj je celowo tylko po to, by oszczędzać baterię.


## Metryki jakości powietrza

Węzły z czujnikami pyłów lub CO₂ raportują dane o jakości powietrza:

| Wartość | Jednostka | Opis |
|--------|------|-------------|
| PM1.0 | µg/m³ | Pył ultradrobny |
| PM2.5 | µg/m³ | Pył drobny |
| PM10 | µg/m³ | Pył gruby |
| CO₂ | ppm | Stężenie dwutlenku węgla |

Czujniki CO₂, takie jak SCD4x, raportują też własną temperaturę i wilgotność, które pojawiają się obok powyższych odczytów. Z historii PM2.5 aplikacja dodatkowo wylicza wartość **EPA NowCast AQI**.

Odczyt CO₂ jest oznaczony kolorem według poziomu (Dobrze → Duszno → Źle → Niebezpiecznie → Ewakuacja). Dokładne przedziały ppm, kolory i szczegóły AQI znajdziesz w [Metryki węzła — jakość powietrza](node-metrics#air-quality-metrics).

Dane o jakości powietrza można oglądać jako karty informacyjne na ekranie szczegółów węzła, na wykresie w czasie i eksportować do CSV.

## Podgląd telemetrii

1. Przejdź do **Węzły** i wybierz węzeł.
2. Sekcja **Telemetria** pokazuje wiersz dla każdego rodzaju wartości — urządzenie, środowisko, jakość powietrza, zasilanie i pozostałe — niezależnie od tego, czy węzeł już je zgłosił. Wiersz wypełnia się odczytami i zyskuje przycisk wykresu, gdy węzeł faktycznie wyśle ten rodzaj telemetrii. Pusty wiersz oznacza, że nic jeszcze nie dotarło, a nie że brakuje czujnika.
3. Przyciskiem wykresu w wierszu otwierasz historię danej wartości, gdzie możesz wybrać przedział czasu i wyeksportować odczyty do CSV.

![Ekran szczegółów węzła z otwartym menu działań wykresu telemetrii](../../assets/screenshots/node-metrics_telemetric_actions.png)

## Rozwiązywanie problemów

- **Brak danych środowiskowych?** Zdalny węzeł musi mieć podłączony fizyczny czujnik (np. BME280 na I2C). Telemetria urządzenia (bateria, czas pracy) jest zawsze dostępna, ale metryki środowiskowe wymagają sprzętu.
- **Nieaktualne odczyty?** Sprawdź interwał raportowania — bardzo długie interwały (7200 s i więcej) oznaczają rzadkie odświeżanie danych. Sprawdź też, czy zdalny węzeł nadal jest online.
- **Konflikt czujników na magistrali I2C?** Niektóre czujniki mają te same adresy I2C. Jeśli na jednej magistrali jest kilka czujników, sprawdź kolizje adresów w szeregowym wyjściu debugowania radia.

## Powiązane tematy

- [Metryki węzła](node-metrics) — podgląd telemetrii na ekranie szczegółów węzła
- [Ustawienia — Moduły i administracja](settings-module-admin) — konfiguracja modułu telemetrii
- [Jednostki i lokalizacja](units-and-locale) — jednostki wyświetlania temperatury i ciśnienia
