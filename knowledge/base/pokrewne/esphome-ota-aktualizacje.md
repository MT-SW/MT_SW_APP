<!-- Baza wiedzy MT_SW: aktualizacje OTA w ESPHome (hasło, porty, protokół, tryb awaryjny). Tylko informacyjnie. -->
# ESPHome: aktualizacje OTA

### Jak działa aktualizacja OTA w ESPHome: hasło, porty i protokół?
OTA w ESPHome pozwala wgrać nowy firmware na urządzenie przez sieć, bez kabla, przez Wi-Fi, Ethernet lub Thread. Z tej drogi korzysta zarówno panel ESPHome, jak i wiersz poleceń. Opcja password ustawia hasło do aktualizacji. Domyślne porty to 3232 dla ESP32, 8266 dla ESP8266, 2040 dla RP2040 i 8892 dla układów Beken. Opcja version domyślnie ma wartość 2, którą dokumentacja opisuje jako bardziej stabilną. Powrót do starego ESPHome wymaga wcześniejszej aktualizacji wersją 1. Komponent OTA sam włącza tryb awaryjny (safe mode), który pomaga odzyskać urządzenie, gdy aktualizacja się nie uda. Można go wyłączyć.
Słowa: esphome ota, aktualizacja przez wifi, hasło ota, password, port 3232, port 8266, port 2040, protokół ota, wersja 2, safe mode, tryb awaryjny, zdalne wgrywanie firmware, esphome upload
Źródło: https://esphome.io/components/ota/esphome.html (sprawdzono 2026-10; oficjalna dokumentacja ESPHome)

### Dlaczego ESPHome zaleca szyfrowanie zamiast hasła przy OTA?
Dokumentacja ESPHome nie zaleca hasła OTA dla nowych urządzeń. Zamiast tego zaleca opcję encryption. Obu nie wolno łączyć, poza krótkim okresem migracji z allow_plaintext_upload. Ta różnica ma znaczenie dla bezpieczeństwa w domowej sieci, bo hasło w konfiguracji to nadal tylko hasło. Jeśli masz stare urządzenie z hasłem OTA, warto je przenieść na szyfrowanie zgodnie z dokumentacją. Konkretne kroki migracji nie są w źródle opisane, więc sprawdź je na stronie ESPHome przed zmianą.
Słowa: esphome encryption, szyfrowanie ota, hasło ota vs szyfrowanie, allow_plaintext_upload, migracja ota, bezpieczeństwo esphome, esp, nie łącz hasła z szyfrowaniem
Źródło: https://esphome.io/components/ota/esphome.html (sprawdzono 2026-10; oficjalna dokumentacja ESPHome; kroki migracji nie są w otwartej części strony)
