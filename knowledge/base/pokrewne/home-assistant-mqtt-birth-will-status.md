### Do czego służy wiadomość birth w integracji MQTT Home Assistant?
Wiadomość birth jest wysyłana po starcie usługi MQTT w Home Assistant, żeby poinformować inne klienty, że usługa działa. Domyślnie jest publikowana na temacie `homeassistant/status`.

Słowa: home assistant, mqtt, birth message, homeassistant/status, start usługi, online
Źródło: https://www.home-assistant.io/integrations/mqtt/#birth-and-will-messages (sprawdzono 2026-10)

### Co wysyła wiadomość last will w MQTT Home Assistant i kiedy?
Last will informuje inne klienty o rozłączeniu. Domyślnie wysyłane jest `offline` na `homeassistant/status`. Wysyłana jest zarówno przy czystym rozłączeniu, np. przy wyłączaniu, jak i przy nieczystym, np. przy awarii lub utracie sieci.

Słowa: home assistant, mqtt, last will, offline, rozłączenie, awaria, homeassistant/status
Źródło: https://www.home-assistant.io/integrations/mqtt/#birth-and-will-messages (sprawdzono 2026-10)

### Jak zmienić wiadomości birth i will w MQTT Home Assistant?
Obie wiadomości można zmienić albo wyłączyć w interfejsie: wybierz Configure na stronie integracji, potem Re-configure MQTT, a następnie Next.

Słowa: home assistant, mqtt, birth, will, konfiguracja, Re-configure MQTT, interfejs
Źródło: https://www.home-assistant.io/integrations/mqtt/#birth-and-will-messages (sprawdzono 2026-10)
