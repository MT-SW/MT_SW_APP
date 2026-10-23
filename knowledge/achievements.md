# MT_SW – osiągnięcia projektu / project achievements

PL: Najważniejsze osiągnięcia projektu MT_SW (szczegóły: lista zmian aplikacji oraz dokumenty firmware i integration).

Aplikacja MT_SW_APP (Android i komputer), start forka w lipcu 2026:
- Zdrowie sieci: zakładka z metrykami zasilania, sygnału, eteru, środowiska, ruchu i sąsiadów, z rankingami i danymi dekodowanymi na żywo z logu.
- Mesh Link Planer: planer łącza i zasięgu (punkty A i B, model terenu, propagacja ITM, bilans łącza, profil terenu), także w integracji z Home Assistant.
- Offline tłumacz i asystent AI, w całości na urządzeniu, bez internetu: tłumaczenie wiadomości, dyktowanie, czytanie na głos, asystent sam otwiera mapę z węzłem, ustawienia, ekrany i planer łącza, gdy o to prosisz (nigdy nie zmienia ustawień i nie wysyła wiadomości); funkcja eksperymentalna; internet dla asystenta tylko za zgodą użytkownika.
- Funkcje MT_SW: osobna grupa ustawień z rzeczami, których nie ma w oryginale, m.in. dodatkowe dane przy węzłach (czujniki, sygnał), dwa motywy czarno-białe, ręczne dodawanie kontaktu po ID, zdalne ulubione i ignorowanie, zdalne sterowanie GPIO, ostrzeżenia o paśmie wąskim.
- Własna marka: MT_SW_APP, autorska ikona z konturem województwa, instaluje się obok oryginału, wersja na komputer.
- Regularne scalanie z oryginalną aplikacją Meshtastic (np. widok AR sieci, podgląd 3D, kafelek szybkich ustawień), przy zachowaniu zgodności z główną siecią.

Firmware MT_SW_FIRMWARE (z cheaterenatorem): statystyki węzła wysyłane po sieci mesh (local_stats, local_stats_extended z CPU, flash, RAM i WiFi), wiadomości prywatne nigdy nie trafiają na MQTT, pełna precyzja pozycji, ReplyBot i SignalReplyModule, diagnostyka SPI i automatyczny restart zawieszonego radia, nowe urządzenie startuje domyślnie z istniejącym presetem Narrow Fast (MT_SW nie ma własnego presetu ani własnego regionu), poprawki routingu i relay_node, diagnostyka na żądanie (OnDemand) i sniffer radiowy, auto-restart co 30 dni, nowe warianty sprzętowe.

Integracja MT_SW_INTEGRATION (Home Assistant): własny panel w pasku bocznym (radio, wiadomości, węzły na żywo, szczegóły węzła, administracja zdalna, pełna konfiguracja radia, mapa z traceroute, Mesh Link Planer, debugowanie), aktualizacja firmware OTA z panelu, sniffer radiowy i MQTT z odszyfrowywaniem i eksportem, tryby pracy „Panel i statystyki / Tylko panel / Tylko statystyki”, całość po polsku.

Protobufów nie rozwijamy: używamy oryginalnych. Wszystko to przy zachowaniu kompatybilności z główną siecią Meshtastic.

EN: Main achievements of the MT_SW project (details: the app change list and the firmware and integration documents).

The MT_SW_APP app (Android and desktop), fork started in July 2026: Network Health tab with power, signal, airtime, environment, traffic and neighbour metrics and rankings; Mesh Link Planer (link and range planner with terrain and ITM propagation), also in the Home Assistant integration; offline translator and AI assistant running fully on the device (message translation, dictation, read-aloud, buttons under assistant answers; internet for the assistant only with the user's consent); an "MT_SW features" settings group with things the original lacks (extra node data, two black-and-white themes, manual contact by ID, remote favourite/ignore, remote GPIO, narrow-band warnings); own branding and an app that installs next to the original; regular merges with the original Meshtastic app while staying compatible with the main network.

The MT_SW_FIRMWARE firmware (with cheaterenator): node statistics sent over the mesh, direct messages never reach MQTT, full position precision, ReplyBot and SignalReplyModule, SPI diagnostics with automatic radio restart, a new device starts by default with the existing Narrow Fast preset (MT_SW has no preset or region of its own), routing and relay_node fixes, OnDemand diagnostics and a radio sniffer, auto-restart every 30 days, new hardware variants.

The MT_SW_INTEGRATION Home Assistant integration: its own sidebar panel (radio, messages, live nodes, node details, remote administration, full radio configuration, map with traceroute, Mesh Link Planer, debugging), OTA firmware update from the panel, radio and MQTT sniffer with decryption and export, three working modes, fully in Polish.

The protobufs are not developed: the original ones are used. All of this keeps compatibility with the main Meshtastic network.
