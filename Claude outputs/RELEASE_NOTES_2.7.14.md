# v2.7.14

## Polski

### Nowości

**Zapis zdjęć na własnym urządzeniu.** Pełnoekranowy podgląd zdjęcia z czatu
ma teraz przycisk „Zapisz". Otwiera natywne okienko wyboru folderu — tak jak
przy wysyłaniu pliku — i zapisuje zdjęcie tam, gdzie wskażesz, na telefonie
albo komputerze. Nic nie trafia na serwer Home Assistant. Przeglądarki bez
wsparcia dla tego okienka (Firefox, Safari) używają zwykłego pobierania do
domyślnego folderu Pobrane.

**Sniffer MQTT — drugie, niezależne źródło logu obok radiowego.** Integracja
może teraz łączyć się bezpośrednio z brokerem MQTT, którego używa moduł MQTT
Twojej bramki, i widzieć cały ruch protobuf na tym brokerze/kanale — nie
tylko to, co odbierze własne radio. Wpisy z obu źródeł trafiają do jednego,
wspólnego logu w panelu Sniffer, oznaczone skąd pochodzą (Radio / MQTT).

W przeciwieństwie do sniffera radiowego (jego stan żyje w pamięci RAM radia,
więc zawsze wyłącza się po restarcie), sniffer MQTT działa w całości po
stronie Home Assistant i pamięta swoje ustawienie na stałe — włącza się
niezależnym przełącznikiem w panelu albo w opcjach integracji.

Ruch MQTT jest zaszyfrowany kluczem kanału, więc integracja teraz sama go
odszyfrowuje (ten sam, standardowy dla całego ekosystemu Meshtastic algorytm
co w każdym innym kliencie). Dopasowanie po nazwie kanału działa pewnie, gdy
kanał ma ustawioną własną nazwę; przy jednym kanale domyślnym (bez własnej
nazwy) też powinno zadziałać. Warto to sprawdzić jako pierwsze po aktualizacji.

### Zmiany techniczne

- Nowy moduł `aiomeshtastic/mesh_crypto.py` — rozwijanie PSK kanału do klucza
  AES i deszyfrowanie AES-CTR pakietów z MQTT.
- Nowy moduł `mqtt_sniffer.py` — niezależny klient MQTT subskrybujący
  `{root}/2/e/#`, zasilający istniejący `SnifferLog`.
- `sniffer.py`: wpisy logu mają teraz pole `source` (`radio`/`mqtt`).
- Nowa opcja integracji: sniffer MQTT (włącz/wyłącz, trwałe ustawienie).
- Nowa komenda WebSocket panelu: `sniffer_mqtt_set`.
- Panel Sniffer: osobny przełącznik i wskaźnik połączenia dla MQTT, kolumna
  źródła w logu.

---

## English

### What's new

**Save chat photos to your own device.** The full-screen photo viewer now
has a Save button. It opens a native folder picker — the same kind used
when sending a file — and saves the photo wherever you choose, on your
phone or computer. Nothing is written to Home Assistant's own server
storage. Browsers without support for that picker (Firefox, Safari) fall
back to a regular browser download into the default Downloads folder.

**MQTT sniffer — a second, independent log source alongside the radio one.**
The integration can now connect directly to the MQTT broker your gateway's
own MQTT module uses, and see all protobuf traffic on that broker/channel —
not just what your own radio receives. Entries from both sources feed the
same combined log in the Sniffer panel, tagged with where they came from
(Radio / MQTT).

Unlike the radio sniffer (its state lives in the radio's RAM, so it's
always off after a reboot), the MQTT sniffer runs entirely on the Home
Assistant side and remembers its setting permanently — toggle it
independently from the panel or from the integration's options.

MQTT traffic is encrypted with the channel key, so the integration now
decrypts it itself (the same standard algorithm used across the whole
Meshtastic ecosystem). Matching by channel name works reliably when the
channel has an explicit name set; with a single default channel (no custom
name) it should also work. Worth checking first after updating.

### Technical changes

- New module `aiomeshtastic/mesh_crypto.py` — channel PSK expansion to an
  AES key and AES-CTR decryption of MQTT packets.
- New module `mqtt_sniffer.py` — independent MQTT client subscribing to
  `{root}/2/e/#`, feeding the existing `SnifferLog`.
- `sniffer.py`: log entries now carry a `source` field (`radio`/`mqtt`).
- New integration option: MQTT sniffer (enable/disable, persistent).
- New panel WebSocket command: `sniffer_mqtt_set`.
- Sniffer panel: separate toggle and connection indicator for MQTT, source
  column in the log.
