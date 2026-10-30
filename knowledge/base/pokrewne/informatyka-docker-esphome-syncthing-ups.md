<!-- Baza wiedzy MT_SW: narzędzia informatyczne w domowym serwerze i węźle. Tylko informacyjnie. -->
# Informatyka: ESPHome, Syncthing, UPS (NUT)

### Jak zaprogramować ESP32 lub ESP8266 przez ESPHome?
ESPHome to projekt firmware dla mikrokontrolerów ESP. Każde urządzenie konfiguruje się plikiem YAML. Pierwsze wgranie wymaga kabla USB z transmisją danych. W instalatorze (Device Builder) edytujesz konfigurację i klikasz INSTALL, a przeglądarka musi obsługiwać WebSerial, czyli np. Chrome, Edge albo Firefox. Ręcznie można wgrać plik firmware.bin z katalogu wyjściowego narzędziem ESPHome Web albo esptool. Źródło nie opisuje szczegółowo aktualizacji OTA ani wymagań połączenia z Home Assistant dla wgrywania, więc te sprawy sprawdź w ich dokumentacji. Wniosek, że aktualizacje przychodzą jako powiadomienia Home Assistant, jest z ogólnej wiedzy, nie ze źródła.
Słowa: esphome, esp32, esp8266, yaml, firmware, wgrywanie, usb, webserial, esptool, firmware.bin, device builder, install, aktualizacja ota, home assistant
Źródło: https://esphome.io/guides/faq.html (sprawdzono 2026-10)

### Czym jest Syncthing i czy wymaga serwera w chmurze?
Syncthing to program do ciągłej synchronizacji plików między dwoma lub większą liczbą komputerów, działający w czasie rzeczywistym. Według strony dane nie są przechowywane nigdzie poza Twoimi urządzeniami, a nie ma centralnego serwera, który mógłby zostać przejęty. Urządzenia łączą się bezpośrednio, ale tylko z tymi, które sam dopuściłeś. Każda maszyna ma swój identyfikator. Synchronizacja działa w sieci lokalnej i przez internet. Gdy nie ma przekierowania portów, pomaga UPnP. Źródło nie podaje, jak działa to w szczegółach przy NAT, więc sprawdź to w swojej sieci. Dla rodziny plików między telefonem a serwerem w domu to rozsądny wybór, ale nie zastępuje kopii zapasowej.
Słowa: syncthing, synchronizacja plików, peer-to-peer, bez chmury, udostępnianie plików, UPnP, identyfikator urządzenia, sync, backup, lokalna sieć
Źródło: https://syncthing.net/ (sprawdzono 2026-10)

### Czym jest UPS i jak Network UPS Tools (NUT) pilnuje wyłączenia zasilania?
UPS to zasilacz awaryjny, który podtrzymuje zasilanie przy zaniku prądu. Network UPS Tools (NUT) to zestaw narzędzi do monitorowania i zarządzania UPS-ami. Składa się z sterowników (drivers) dla różnych typów połączeń, np. szeregowego, USB czy SNMP, oraz z części sieciowej i klienta monitorującego. Strona spisu treści opisuje też automatyczne wyłączanie przy niskim poziomie baterii oraz harmonogramy zdarzeń. Nie opisuje w tym źródle, co dokładnie robią poszczególne programy serwera i klienta, więc nazw ich ról nie podaję. Wiedz, jak skonfigurować wyłączanie na własnym serwerze, szukaj w pełnej instrukcji NUT.
Słowa: ups, zasilacz awaryjny, nut, network ups tools, sterowniki, monitorowanie, zanik prądu, bateria, automatyczne wyłączenie, usb ups, snmp, awaria prądu
Źródło: https://networkupstools.org/docs/user-manual.chunked/index.html (sprawdzono 2026-10; źródło to spis treści, szczegóły w pełnej instrukcji)
