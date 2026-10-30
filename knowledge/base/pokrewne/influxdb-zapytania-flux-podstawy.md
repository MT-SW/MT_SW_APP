<!-- Baza wiedzy MT_SW: podstawy zapytań Flux w InfluxDB 2 (from, range, filter, operator |>). Tylko informacyjnie. -->
# InfluxDB 2: podstawy zapytań Flux

### Z jakich trzech elementów składa się podstawowe zapytanie Flux w InfluxDB 2?
Każde zapytanie Flux do InfluxDB potrzebuje trzech rzeczy: źródła danych, zakresu czasu i filtrów. Źródło definiuje funkcja from z parametrem bucket, np. from(bucket: "przyklad-bucket"). Zakres czasu ustawia range, np. range(start: -15m) dla ostatnich 15 minut. Filtry wybierają wiersze po wartościach kolumn, np. po _measurement albo _field, przez funkcję filter z predykatem. Operator |> przekazuje wynik jednego kroku do następnego, więc całość tworzy łańcuch: from, potem range, potem filter.
Słowa: flux, zapytanie flux, from, range, filter, bucket, zakres czasu, _measurement, _field, operator pipe, |>, podstawy flux, influxdb 2 zapytanie
Źródło: https://docs.influxdata.com/influxdb/v2/query-data/get-started/query-influxdb/index.md (sprawdzono 2026-10; oficjalna dokumentacja InfluxDB OSS v2)

### Dlaczego zapytanie Flux musi mieć zakres czasu i jak wygląda wzór z range?
Flux wymaga zakresu czasu przy zapytaniach o dane szeregów czasowych. Zapytania bez zakresu są bardzo kosztowne, więc Flux ich nie wykona. Zakres może być względny, np. -15m, albo bezwzględny znacznik czasu. Wzór z dokumentacji to from(bucket: "przyklad-bucket") |> range(start: -15m) |> filter(fn: (r) => r._measurement == "cpu" and r._field == "usage_system"). Przy domowym monitoringu zawsze zaczynaj od krótkiego zakresu, żeby zapytanie było szybkie.
Słowa: flux range wymagany, zapytanie bez zakresu, -15m, zakres względny, zakres bezwzględny, cpu usage_system, przykład flux, szybkie zapytanie, koszt zapytania, timeout flux
Źródło: https://docs.influxdata.com/influxdb/v2/query-data/get-started/query-influxdb/index.md (sprawdzono 2026-10; oficjalna dokumentacja InfluxDB OSS v2; zalecenie krótkiego zakresu z ogólnej wiedzy)
