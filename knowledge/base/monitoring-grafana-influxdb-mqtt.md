<!-- Baza wiedzy MT_SW: monitoring telemetrii sieci (Grafana, InfluxDB, MQTT). Źródła w każdym wpisie. -->
# Monitoring telemetrii: Grafana, InfluxDB i MQTT

### Jak pokazać dane z węzłów Meshtastic na wykresach w Grafanie?
Grafana to platforma do budowania pulpitów (dashboardów) i przeglądania metryk, logów oraz śladów, niezależnie od tego, gdzie dane są przechowywane. Sama nie zbiera danych z radia, więc potrzebuje źródła danych. Najczęściej w domowym monitoringu to baza szeregów czasowych, np. InfluxDB albo Prometheus, a Grafana odczytuje ją i rysuje wykresy. Jeśli węzły wysyłają dane przez MQTT, to między węzłem a Grafaną stoi zwykle brokier MQTT i program, który zapisuje wiadomości do bazy. Taki łańcuch trzeba złożyć samodzielnie. Źródło opisuje Grafanę i jej wtyczki, ale nie opisuje konkretnego połączenia z Meshtastic, więc szczegóły zapisu (który temat MQTT, jaki format) sprawdź w konfiguracji MQTT własnego węzła i w swojej bazie. Wnioski o łańcuchu są z ogólnej wiedzy, nie ze źródła.
Słowa: grafana, dashboard, pulpit, wykresy, metryki, monitoring, mqtt, influx, prometheus, telemetria, wizualizacja, grafa
Źródło: https://grafana.com/docs/grafana/latest/ (sprawdzono 2026-10)

### Czym jest InfluxDB i jak się w nim połączyć?
InfluxDB to baza danych szeregów czasowych. Zbiera, przechowuje i przetwarza punkty danych oznaczone czasem, np. odczyty czujników, metryki serwera czy kursy. W wersji 2 dane trafiają do tzw. bucketu, czyli nazwanego miejsca w bazie, w którym może być wiele pomiarów. Dostęp do zapisu i odczytu daje token API, który określa uprawnienia. Interfejs webowy domyślnie działa pod adresem localhost na porcie 8086, gdy baza jest uruchomiona. Źródło nie opisuje pojęcia organizacji poza linkami w nawigacji, więc go tu nie rozwijam. Wersja 2.10 wymaga uwierzytelnienia tokenem. Dla domowego serwera przyjmij zasadę: osobny token tylko do zapisu z programu i osobny do odczytu w Grafanie.
Słowa: influxdb, influx, baza czasowa, time series, bucket, token, port 8086, organizacja, szeregi czasowe, zapis danych
Źródło: https://docs.influxdata.com/influxdb/v2/get-started/ (sprawdzono 2026-10)
