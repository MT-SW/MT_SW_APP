### Jaki rozmiar paczki zapisu InfluxDB zaleca dokumentacja?
Optymalny rozmiar paczki to 5000 linii line protocol. Strona nie podaje wskazówek opartych na czasie.

Słowa: influxdb, batch, paczka zapisu, 5000 linii, line protocol, optymalizacja
Źródło: https://docs.influxdata.com/influxdb/v2/write-data/best-practices/optimize-writes/ (sprawdzono 2026-10)

### Jak włączyć kompresję gzip zapisu w Telegrafie do InfluxDB?
W wtyczce `influxdb_v2` w Telegrafie trzeba ustawić `content_encoding = "gzip"`. Strona podaje benchmarki z przyspieszeniem nawet 5 razy przy skompresowanych danych.

Słowa: influxdb, telegraf, influxdb_v2, gzip, content_encoding, kompresja, przyspieszenie
Źródło: https://docs.influxdata.com/influxdb/v2/write-data/best-practices/optimize-writes/ (sprawdzono 2026-10)

### Jak wysłać gzip do endpointu zapisu InfluxDB v2?
Dla endpointu `/api/v2/write` trzeba skompresować treść i ustawić nagłówek `Content-Encoding` na `gzip`. Biblioteki klienta mają własne opcje kompresji.

Słowa: influxdb, api v2 write, Content-Encoding, gzip, nagłówek, endpoint
Źródło: https://docs.influxdata.com/influxdb/v2/write-data/best-practices/optimize-writes/ (sprawdzono 2026-10)

### Jaką precyzję czasu wybrać przy zapisie do InfluxDB?
Domyślna precyzja to nanosekundy. Jeśli dane nie są zbierane w rozdzielczości nanosekund, strona zaleca najgrubszą precyzję, która pasuje, bo poprawia to wydajność. Precyzję podaje się przy zapisie.

Słowa: influxdb, precyzja, nanosekundy, timestamp, wydajność, zapis
Źródło: https://docs.influxdata.com/influxdb/v2/write-data/best-practices/optimize-writes/ (sprawdzono 2026-10)
