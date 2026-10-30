<p align="center">
  <img src="https://raw.githubusercontent.com/MT-SW/MT_SW_INTEGRATION/main/custom_components/meshtastic/brand/icon.png" width="140" alt="MT_SW" />
</p>

# MT_SW Firmware

Custom firmware fork for the **Świętokrzyskie (MT_SW)** LoRa mesh network, based on the official [meshtastic/firmware](https://github.com/meshtastic/firmware) (version 2.8.2).

🌐 [mt-sw.pl](https://mt-sw.pl)

👥 Autorzy / Authors: **Spontaniczne Majsterkowanie** & **cheaterenator** — wspólny projekt / joint project

---

## 🇵🇱 Polski

### Czym jest ten fork

To zmodyfikowana wersja oficjalnego firmware'u Meshtastic, dostosowana do potrzeb regionalnej sieci mesh MT_SW (Świętokrzyskie). Zmiany obejmują rozszerzoną telemetrię, poprawki prywatności w MQTT, dodatkowe komendy czatu, nowe warianty sprzętowe, diagnostykę radia oraz szereg poprawek stabilności.

### Co zmienia ten fork względem oficjalnego Meshtastic

#### 📊 Telemetria i statystyki
- **Statystyki `local_stats` po sieci mesh** — wysyłane automatycznie zaraz po standardowej telemetrii urządzenia, z tym samym interwałem co ona (domyślnie ustawiane w `userPrefs.jsonc`, np. co 2 godziny + naturalna kompensacja zależna od ruchu na sieci). W oficjalnym firmware `local_stats` trafia tylko do telefonu, nie na mesh.
- **Nowy pakiet `local_stats_extended`** — dodatkowy typ telemetrii wysyłany tuż po `local_stats`, zawierający zużycie CPU (%), użycie flash (used/total) oraz RAM/PSRAM (free/total). CPU liczone jest z własnego licznika czasu zajętości pętli głównej (`startBusy()`/`endBusy()` w `main.cpp`), uśrednianego z ostatnich 60 sekund.
- **Jakość i przyczyna rozłączenia WiFi** — w `local_stats_extended` oraz w odpowiedziach OnDemand raportowany jest poziom sygnału WiFi i przyczyna ostatniego rozłączenia.
- **Węzeł nie wysyła czasu, jeśli nie jest on poprawny** — dotyczy pakietów telemetrii i pozycji (zapobiega rozsyłaniu błędnych znaczników czasu).
- **Interwały telemetrii nie są już wymuszane do minimum roli** — usunięto (zakomentowano) mechanizm w `NodeDB.cpp`, który normalnie zaokrąglał skonfigurowane interwały telemetrii do minimalnej wartości zależnej od roli urządzenia na kanale domyślnym. Pozwala to np. na interwał 2h dla telemetrii urządzenia bez nadpisywania go przez firmware.
- Domyślny interwał telemetrii urządzenia podniesiony do 2h, środowiskowej do 1h, pozycji do 12h (konfigurowalne w `userPrefs.jsonc`).
- Limity pamięci na rekordy transmisji (offline message records) podwojone przy większych układach flash (np. 250→500 dla flash ≥15MB).

#### 🔒 Prywatność i MQTT
- **Wiadomości prywatne (DM) nigdy nie trafiają na MQTT** — dodane zabezpieczenie w `Router.cpp` i `MQTT.cpp`: pakiety adresowane do konkretnego węzła (nie broadcast) są odrzucane przed uplinkiem do brokera, niezależnie od ustawień kanału. Bez tego DM odebrany i rozszyfrowany lokalnie przez węzeł pełniący rolę bramki mógł trafić do brokera w postaci jawnej.
- **Pakiety przechodzące przez MQTT nie są flagowane jako `via_mqtt`** — zmieniono `p->via_mqtt = true` na `false` w `MQTT.cpp`, więc pakiety z brokera nie różnią się (z punktu widzenia flagi) od tych odebranych bezpośrednio przez radio.
- Domyślnie `USERPREFS_CONFIG_LORA_IGNORE_MQTT` ustawione na `false` (węzeł nie ignoruje ruchu MQTT).

#### 📍 Lokalizacja
- **Zezwolenie na pełną precyzję pozycji** — `MAX_POSITION_PRECISION_PUBLIC_KEY` podniesione z 15 do 32 bitów, a zakres akceptowanej precyzji w raportach mapowych MQTT rozszerzony z 12–15 do 12–40. Dotyczy to zarówno kanału publicznego, jak i raportowania przez MQTT/mapę.
- **Poprawka precyzji pozycji w Map Report** — zbyt duża rozdzielczość pozycji powodowała przekroczenie zakresu współrzędnych; została poprawiona.
- Domyślna precyzja pozycji w Traffic Management (`default_traffic_mgmt_position_precision_bits`) podniesiona z 19 do 32 bitów.

#### 💬 Komendy czatu i odpowiedzi automatyczne
- **ReplyBot włączony** (`MESHTASTIC_EXCLUDE_REPLYBOT` z 1 na 0) z osobnymi, spersonalizowanymi odpowiedziami dla trzech komend:
  - `/ping` — odpowiedź "pong!" wraz z diagnostyką (liczba skoków, RSSI, SNR)
  - `/test` — odpowiedź po polsku z odnośnikiem do komendy "scyzoryk pomoc", bez diagnostyki sygnału
  - `/hello` — powitanie w języku polskim z linkiem do mt-sw.pl, bez diagnostyki sygnału
- **Nowy moduł `SignalReplyModule`** — odpowiada na wiadomość `!!Test!!`/`!!test!!` informacją o liczbie przeskoków (dla wiadomości pośrednich) albo RSSI/SNR (dla wiadomości bezpośrednich).
- Remote Hardware moduł włączony (`MESHTASTIC_EXCLUDE_REMOTEHARDWARE` z 1 na 0).

#### 📡 Sieć mesh
- **Priorytet LoRa w routingu MQTT/UDP** — poprawiono routing tak, aby pierwszeństwo miała transmisja przez LoRa.
- **Poprawka raportowania NeighborInfo** — węzły bez poprawnej liczby skoków nie są już ignorowane, gdy są odbierane na innym kanale.
- **NeighborInfo może być rozsyłane na kanale publicznym** — usunięto ograniczenie w `NeighborInfoModule.cpp`, które normalnie blokowało transmisję sąsiedztwa poza kanałem/slotem domyślnym.
- **Poprawka przekazywania informacji o węźle-przekaźniku (`relay_node`)** — w `RadioLibInterface.cpp` i `ReliableRouter.cpp`: wcześniej `relay_node` był odrzucany przy `hop_start == 0`, co gubiło informację o przekaźniku dla wielu pakietów ACK (w tym implicit ACK). Teraz `relay_node` jest zawsze ufany bezpośrednio, a implicit ACK przekazuje go dalej do PhoneAPI/aplikacji — dzięki temu apka wie, który węzeł przekazał wysłaną wiadomość.
- **OnDemand — węzły online** — pełna lista węzłów jest teraz zwracana w kilku pakietach (zamiast jednego, który się nie mieścił).
- Skrócone limity częstotliwości żądań traceroute z 30 do 10 sekund (komunikat o limicie przetłumaczony na polski).
- `HOP_RELIABLE` (domyślna liczba skoków dla niezawodnych wiadomości) podniesione z 3 do 7.
- Dodano eksperymentalny fallback dekodowania dla UDP multicast (`HAS_UDP_MULTICAST`), próbujący domyślnych presetów/PSK gdy standardowe dopasowanie kanału zawiedzie.

#### 📻 Radio i częstotliwość
- **Diagnostyka magistrali SPI i automatyczny restart radia w locie** — gdy radio się zawiesi, firmware wykrywa to i restartuje je bez restartu całego urządzenia.
- **Błędy radia i magistrali SPI w logach** — dodano ich zapis w logach, co ułatwia szukanie przyczyn problemów.
- **Domyślny preset NARROW_FAST** — świeże urządzenie (bez ustawień w `userPrefs.jsonc`) startuje z presetem Narrow Fast zamiast Long Fast (`Channels.cpp`), dzięki czemu pasuje do regionu EU_N_868. Region nadal trzeba wybrać w aplikacji albo ustawić w `userPrefs.jsonc`.
- **Odchyłka częstotliwości z `userPrefs.jsonc`** — `USERPREFS_LORACONFIG_FREQUENCY_OFFSET` (w MHz) koryguje błąd kwarcu radia i jest stosowana przy każdym uruchomieniu.

#### 🔋 Zasilanie i stabilność
- **Poprawka `UptimeClock`** — naprawiono błędne liczenie czasu pracy (skoki o 49,7 dnia do przodu).
- **Auto-restart** — nowy wątek `AutoRebootThread`, domyślnie restartuje urządzenie co 30 dni pracy (konfigurowalne przez `AUTO_REBOOT_INTERVAL_SECS` w `platformio.ini`, maksymalnie ~49 dni ze względu na przewijanie licznika `millis()`).
- **Mniejsze zużycie RAM dla ESP32-C3** — ograniczono zużycie pamięci RAM na tej platformie.
- **Moduł testu watchdoga** (`WatchdogTestThread`, domyślnie wyłączony) — pozwala celowo zawiesić firmware na określony czas, by zweryfikować że sprzętowy watchdog rzeczywiście resetuje urządzenie i zmierzyć jak długo to trwa. Włączany flagą kompilacji `WATCHDOG_TEST_ENABLED=1`.
- Krzywa napięcia baterii (`OCV_ARRAY`) dostrojona do rzeczywistych ogniw używanych w projekcie.
- Tryb parowania Bluetooth ustawiony na stały PIN (`FIXED_PIN`) również dla urządzeń z ekranem (wcześniej: losowy PIN dla urządzeń z ekranem).

#### 🖥️ Platformy i narzędzia budowania
- **BaseUI dla Windows native** — interfejs BaseUI działa także w wersji natywnej na Windows.
- **Wyłączona biblioteka JSON dla OpenWrt native** — upraszcza budowanie na OpenWrt.
- **Zadania (tasks) OTA** — budują pełny obraz (full image) z partycją OTA.

#### 🔌 Nowe warianty sprzętowe
- **`rak_killer_v2`** — SuperMini nRF52840 + moduł Heltec HT-RA62 na dedykowanej płytce DIY.
- **`ikoka_s3`**, **`ikoka_s3_433`** i **`ikoka_s3_e22p`** — warianty na bazie Seeed XIAO S3, m.in. z modułem eByte E22P-868M30S.
- **`ikoka_c6`**, **`ikoka_c6_433`** i **`ikoka_c6_e22p`** — nowe warianty dla ESP32-C6.
- **`elecrow-adv1-43-50-70-v15-tft`** — Elecrow CrowPanel Advance 4.3"/5.0"/7.0" w wersji płyty 1.4/1.5 (sterownik STC8H1K28, radio SX1262 z CS na GPIO8). Dla starszych płyt (1.0/1.1) nadal służy `elecrow-adv1-43-50-70-tft`.

### Autorzy
- **Spontaniczne Majsterkowanie** — integracja z siecią MT_SW, konfiguracja regionalna, dokumentacja
- **cheaterenator** — rozwój customowego firmware (ReplyBot, telemetria rozszerzona, poprawki relay_node/MQTT, diagnostyka radia, nowe warianty sprzętowe)

### Budowanie

Standardowo przez PlatformIO:
```bash
pio run -e <nazwa-środowiska>
```
np. `pio run -e rak_killer_v2` albo `pio run -e ikoka_c6`. Pełna lista środowisk w `platformio.ini` oraz w folderach `variants/`.

### Licencja, nazwa i cudze treści

Kod jest otwarty, na licencji GNU GPL v3 — możesz go używać, zmieniać i udostępniać dalej na jej zasadach. Prosimy o dobre praktyki:

- zmienioną lub cudzą wersję programu nazwij inaczej i zaznacz, że nie pochodzi od nas;
- nie przedstawiaj swojego produktu, strony ani grupy jako oficjalnego projektu MT_SW, jeśli nim nie jest;
- nasze teksty, zrzuty ekranu i dokumentację możesz cytować z podaniem autora i źródła;
- jeśli chcesz napisać o projekcie lub sieci, odezwij się do nas wcześniej — pomożemy sprawdzić fakty; prosimy nie publikować prywatnych danych osób tworzących projekt.

Nie odpowiadamy za informacje, artykuły, filmy, strony ani wypowiedzi osób i podmiotów trzecich, w tym za błędne, nieaktualne lub niezwiązane z nami fakty. Odcinamy się od stron, struktur, grup i osób, które piszą lub mówią o nas bez kontaktu z nami i bez udziału w naszej społeczności i sieci. Rzetelne informacje o projekcie są w naszych własnych kanałach i repozytoriach (GitHub MT-SW, mt-sw.pl). Pytania i zgłaszanie błędów: przez nasze kanały (lokalne grupy, Discord/WhatsApp/Signal, sieć mesh).

Projekt nie jest afiliowany ani wspierany przez Meshtastic LLC. Meshtastic® jest zarejestrowanym znakiem towarowym Meshtastic LLC. Z firmware'u korzystasz na własne ryzyko, bez żadnej gwarancji.

---

## 🇬🇧 English

### About this fork

This is a modified version of the official Meshtastic firmware, tailored for the Świętokrzyskie (MT_SW) regional mesh network. Changes include extended telemetry, MQTT privacy fixes, additional chat commands, new hardware variants, radio diagnostics, and several stability fixes.

### What this fork changes vs. official Meshtastic

#### 📊 Telemetry & stats
- **`local_stats` broadcast over the mesh** — sent automatically right after standard device telemetry, on the same interval (e.g. every 2 hours as configured in `userPrefs.jsonc`, plus natural compensation depending on mesh traffic). In stock firmware, `local_stats` only goes to the phone, never over the mesh.
- **New `local_stats_extended` telemetry packet** — sent right after `local_stats`, carrying CPU usage (%), flash usage (used/total), and RAM/PSRAM (free/total). CPU usage is computed from a custom busy-time counter around the main loop (`startBusy()`/`endBusy()` in `main.cpp`), averaged over the last 60 seconds.
- **WiFi signal and disconnect reason** — `local_stats_extended` and OnDemand replies report the WiFi signal level and the reason for the last disconnect.
- **The node does not send the time when it is not valid** — applies to telemetry and position packets (prevents broadcasting bogus timestamps).
- **Telemetry intervals are no longer coerced to a role-aware minimum** — the mechanism in `NodeDB.cpp` that normally rounded configured telemetry intervals up to a role-based minimum on the default channel has been disabled. This allows e.g. a 2h device-telemetry interval to actually take effect instead of being overridden.
- Default telemetry intervals raised: device telemetry to 2h, environment telemetry to 1h, position broadcast to 12h (configurable in `userPrefs.jsonc`).
- Offline transmit-history record capacity doubled on larger flash chips (e.g. 250→500 for ≥15MB flash).

#### 🔒 Privacy & MQTT
- **Direct messages (DMs) never reach MQTT** — a guard added in `Router.cpp`/`MQTT.cpp` rejects any non-broadcast packet before it can be uplinked to the broker, regardless of channel uplink settings. Without this, a DM decrypted locally by a gateway node could be republished to the broker in plaintext.
- **Packets relayed through MQTT are no longer flagged as `via_mqtt`** — `p->via_mqtt` changed from `true` to `false` in `MQTT.cpp`, so MQTT-sourced packets are indistinguishable (by that flag) from ones received directly over radio.
- `USERPREFS_CONFIG_LORA_IGNORE_MQTT` defaults to `false` (the node does not ignore MQTT traffic).

#### 📍 Location
- **Full position precision allowed** — `MAX_POSITION_PRECISION_PUBLIC_KEY` raised from 15 to 32 bits, and the accepted precision range for MQTT map reports extended from 12–15 to 12–40. This applies to both the public channel and MQTT/map reporting.
- **Map Report position precision fix** — a too-high position resolution made the coordinates overflow their range; this has been fixed.
- Default Traffic Management position precision (`default_traffic_mgmt_position_precision_bits`) raised from 19 to 32 bits.

#### 💬 Chat commands & auto-replies
- **ReplyBot enabled** (`MESHTASTIC_EXCLUDE_REPLYBOT` flipped from 1 to 0) with distinct, localized replies for three commands:
  - `/ping` — "pong!" plus signal diagnostics (hop count, RSSI, SNR)
  - `/test` — a Polish-language reply pointing to the "scyzoryk pomoc" command, no diagnostics
  - `/hello` — a Polish welcome message linking to mt-sw.pl, no diagnostics
- **New `SignalReplyModule`** — replies to a `!!Test!!`/`!!test!!` message with either the hop count (for relayed messages) or RSSI/SNR (for direct messages).
- Remote Hardware module enabled (`MESHTASTIC_EXCLUDE_REMOTEHARDWARE` flipped from 1 to 0).

#### 📡 Mesh networking
- **LoRa has priority in MQTT/UDP routing** — routing was fixed so that transmission over LoRa takes precedence.
- **NeighborInfo reporting fix** — nodes without a valid hop count are no longer ignored when they are received on a different channel.
- **NeighborInfo can be broadcast on the public channel** — removed the restriction in `NeighborInfoModule.cpp` that normally blocked neighbor-info transmission outside the default channel/frequency slot.
- **Relay-node (`relay_node`) propagation fix** — in `RadioLibInterface.cpp` and `ReliableRouter.cpp`: `relay_node` used to be discarded whenever `hop_start == 0`, silently dropping relay info on many ACK packets (including implicit ACKs). It's now always trusted directly, and the implicit ACK forwards it on to PhoneAPI/the app — so the app can show which node relayed a message you sent.
- **OnDemand — online nodes** — the full node list is now returned split across several packets (instead of one that did not fit).
- Traceroute request rate limit shortened from 30 to 10 seconds (rate-limit notification translated to Polish).
- `HOP_RELIABLE` (default hop count for reliable messages) raised from 3 to 7.
- Added an experimental UDP multicast decode fallback (`HAS_UDP_MULTICAST`) that tries default presets/PSK when normal channel matching fails.

#### 📻 Radio & frequency
- **SPI bus diagnostics and on-the-fly radio auto-restart** — when the radio hangs, the firmware detects it and restarts the radio without rebooting the whole device.
- **Radio and SPI bus errors in the logs** — they are now logged, which makes troubleshooting easier.
- **Default preset NARROW_FAST** — a fresh device (with nothing set in `userPrefs.jsonc`) starts with the Narrow Fast preset instead of Long Fast (`Channels.cpp`), so it fits the EU_N_868 region. The region still has to be chosen in the app or set in `userPrefs.jsonc`.
- **Frequency offset from `userPrefs.jsonc`** — `USERPREFS_LORACONFIG_FREQUENCY_OFFSET` (in MHz) compensates for radio crystal error and is applied on every boot.

#### 🔋 Power & stability
- **`UptimeClock` fix** — fixed incorrect uptime calculation (jumps forward by 49.7 days).
- **Auto-reboot** — new `AutoRebootThread`, reboots the device every 30 days of uptime by default (configurable via `AUTO_REBOOT_INTERVAL_SECS` in `platformio.ini`, capped at ~49 days due to `millis()` rollover).
- **Lower RAM usage on ESP32-C3** — RAM consumption reduced on this platform.
- **Watchdog test module** (`WatchdogTestThread`, disabled by default) — deliberately hangs the firmware for a set duration to verify the hardware watchdog actually resets the device and measure how long that takes. Enabled via the `WATCHDOG_TEST_ENABLED=1` build flag.
- Battery voltage curve (`OCV_ARRAY`) tuned for the actual cells used in the project.
- Bluetooth pairing mode set to a fixed PIN for screened devices too (previously: random PIN on devices with a screen).

#### 🖥️ Platforms & build tooling
- **BaseUI for Windows native** — the BaseUI interface also works in the native Windows build.
- **JSON library disabled for OpenWrt native** — simplifies building for OpenWrt.
- **OTA tasks** — build a full image that includes the OTA partition.

#### 🔌 New hardware variants
- **`rak_killer_v2`** — SuperMini nRF52840 + Heltec HT-RA62 module on a dedicated DIY breakout board.
- **`ikoka_s3`**, **`ikoka_s3_433`** and **`ikoka_s3_e22p`** — Seeed XIAO S3-based variants, including one with the eByte E22P-868M30S module.
- **`ikoka_c6`**, **`ikoka_c6_433`** and **`ikoka_c6_e22p`** — new variants for ESP32-C6.
- **`elecrow-adv1-43-50-70-v15-tft`** — Elecrow CrowPanel Advance 4.3"/5.0"/7.0" board revision 1.4/1.5 (STC8H1K28 controller, SX1262 radio with CS on GPIO8). Older boards (1.0/1.1) still use `elecrow-adv1-43-50-70-tft`.

### Credits
- **Spontaniczne Majsterkowanie** — MT_SW network integration, regional configuration, documentation
- **cheaterenator** — custom firmware development (ReplyBot, extended telemetry, relay_node/MQTT fixes, radio diagnostics, new hardware variants)

### Building

Standard PlatformIO build:
```bash
pio run -e <environment-name>
```
e.g. `pio run -e rak_killer_v2` or `pio run -e ikoka_c6`. Full list of environments in `platformio.ini` and under `variants/`.

### Licence, name and third-party content

The code is open, under the GNU GPL v3 — you may use, modify and redistribute it under its terms. We kindly ask for good practice:

- please give a modified or third-party version a different name and state that it does not come from us;
- please do not present your product, website or group as an official MT_SW project if it is not;
- you may quote our texts, screenshots and documentation, naming the author and source;
- if you want to write about the project or the network, please contact us beforehand — we will help check the facts; please do not publish private data of the people behind the project.

We are not responsible for information, articles, videos, websites or statements made by third parties, including incorrect, outdated or unrelated facts about us. We distance ourselves from websites, structures, groups and persons who write or speak about us without any contact with us and without taking part in our community and network. Reliable information about the project is in our own channels and repositories (GitHub MT-SW, mt-sw.pl). Questions and bug reports: through our channels (local groups, Discord/WhatsApp/Signal, the mesh network).

This project is not affiliated with or endorsed by Meshtastic LLC. Meshtastic® is a registered trademark of Meshtastic LLC. You use the firmware at your own risk, with no warranty.
