<!-- Baza wiedzy MT_SW: typy tokenów API w InfluxDB 2 i haszowanie tokenów. Tylko informacyjnie. -->
# InfluxDB 2: typy tokenów API

### Jakie są typy tokenów API w InfluxDB 2: operator, all access i read/write?
Token operatora daje pełny dostęp do odczytu i zapisu do wszystkich organizacji i ich zasobów w InfluxDB OSS 2.x. Niektóre operacje, np. pobranie konfiguracji serwera, wymagają uprawnień operatora. Token All Access daje pełny dostęp do wszystkich zasobów w jednej organizacji. Token Read/Write daje odczyt, zapis lub oba, ale tylko do wybranych bucketów w organizacji. Dokumentacja zaleca, żeby do zarządzania InfluxDB tworzyć token All Access dla każdej organizacji, a nie używać tokenu operatora, bo ten może przypadkowo zadziałać na innej organizacji. Do zapisu danych z Telegrafa lub Home Assistant wystarcza token Read/Write ograniczony do jednego bucketu.
Słowa: influxdb token, token api, operator, all access, read write, token zapisu, bucket, uprawnienia, token telegraf, token grafana, organizacja, dostęp, zabezpieczenia
Źródło: https://docs.influxdata.com/influxdb/v2/admin/tokens/index.md (sprawdzono 2026-10; oficjalna dokumentacja InfluxDB OSS v2; zalecenie tokenów Read/Write dla Telegrafa to wniosek z ogólnej wiedzy)

### Czy tokeny InfluxDB 2 są przechowywane jawnie i co zmienia haszowanie od wersji 2.9?
Od InfluxDB OSS 2.9.0 haszowanie tokenów jest domyślnie włączone. W wersjach 2.8.x było opcjonalne, a w 2.7 i starszych niedostępne. Tokeny trzymane są na dysku jako skróty, więc kopia pliku bazy nie ujawnia działających tokenów. Przy pierwszym uruchomieniu z haszowaniem istniejące tokeny są migrowane do postaci zahaszowanej, a oryginalnych ciągów nie da się już odczytać. Dlatego przed aktualizacją do 2.9.0 zapisz tokeny, których potrzebujesz, w tym token operatora. Zmiany uprawnień po utworzeniu tokenu nie da się zrobić, więc przy błędzie trzeba utworzyć nowy.
Słowa: haszowanie tokenów, token hashing, influxdb 2.9, aktualizacja influxdb, migracja tokenów, kopia tokenu, token zapisany, skrót tokenu, bezpieczeństwo bazy, utrata tokenu, aktualizacja przed
Źródło: https://docs.influxdata.com/influxdb/v2/admin/tokens/index.md (sprawdzono 2026-10; oficjalna dokumentacja InfluxDB OSS v2)
