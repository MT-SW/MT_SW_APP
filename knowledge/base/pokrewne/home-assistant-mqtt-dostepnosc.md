<!-- Baza wiedzy MT_SW: dostępność (availability) urządzeń MQTT w Home Assistant, czyli temat online i offline. Tylko informacyjnie. -->
# Home Assistant: dostępność urządzeń MQTT

### Jak Home Assistant pokazuje, że urządzenie MQTT jest online albo offline?
Dostępność (availability) urządzenia MQTT określa temat availability_topic, który Home Assistant subskrybuje. Domyślna wartość gotowości to online, a niedostępności to offline. Jeśli wpiszesz własne wartości, zmieniają się one przez payload_available i payload_not_available. Opcja availability_mode działa tylko przy liście availability i ma domyślnie wartość latest, czyli liczy się ostatnia wiadomość z dowolnego tematu. Tryb all wymaga gotowości na każdym temacie, a any wystarczy jednego. Dostępność to czy urządzenie odpowiada, a stan to wartość z tematu stanu, np. ON, OFF albo odczyt czujnika. Gdy urządzenie jest niedostępne, jego stan nie nadaje się do odczytu. Wniosek, że lista tematów pomaga przy wielu źródłach, to z ogólnej wiedzy.
Słowa: home assistant availability, dostępność mqtt, online, offline, availability_topic, payload_available, payload_not_available, availability_mode, latest, all, any, niedostępny, czujnik niedostępny, stan vs dostępność
Źródło: https://www.home-assistant.io/integrations/mqtt/ (sprawdzono 2026-10; oficjalna dokumentacja Home Assistant; wniosek o wielu tematach z ogólnej wiedzy)

