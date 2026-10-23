# MT_SW – podstawowe fakty (zawsze w pamięci asystenta) / core facts (always in the assistant's memory)

<!-- Krótki plik, który asystent dostaje przy KAŻDEJ odpowiedzi. Aktualizuj go przy każdej zmianie w projekcie.
     A short file the assistant gets with EVERY answer. Update it with every change in the project. -->

## PL
- MT_SW to regionalny projekt sieci mesh LoRa (Meshtastic) dla województwa świętokrzyskiego; strona: mt-sw.pl; organizacja MT-SW na GitHubie. Autorzy: Spontaniczne Majsterkowanie i cheaterenator.
- Części projektu: aplikacja MT_SW_APP (ta aplikacja: Android i komputer, fork aplikacji Meshtastic), firmware MT_SW_FIRMWARE (fork oficjalnego firmware Meshtastic), integracja MT_SW_INTEGRATION (Home Assistant). Protobufów nie rozwijamy: używamy oryginalnych.
- Ustawienia sieci w województwie świętokrzyskim: oficjalny preset Narrow Fast, region EU_N_868, częstotliwość 869,44165 MHz. MT_SW nie ma własnego presetu ani regionu. Firmware MT_SW startuje domyślnie z presetem Narrow Fast, ale region trzeba wybrać.
- MT_SW nie omija głównej sieci Meshtastic: ma własne rozwiązania, ale jest z nią w pełni zgodny.
- Wiele osób w sieci to amatorzy bez licencji krótkofalarskiej: mówimy o „przepisach”, nie o „licencji”.
- Aplikacja MT_SW_APP: zdrowie sieci, Mesh Link Planer (planer łącza i zasięgu z terenem), korekta LNA i ocena sygnału, sniffer, diagnostyka na żądanie, zdalne GPIO, motywy czarno-białe, wybór koloru węzła, skalowanie aplikacji, offline tłumacz i asystent AI (funkcja eksperymentalna), wszystko w „Funkcjach MT_SW” w Ustawieniach.
- Firmware MT_SW_FIRMWARE: statystyki węzła wysyłane po sieci (CPU, flash, RAM, Wi-Fi), wiadomości prywatne nigdy nie trafiają na MQTT, pełna precyzja pozycji, ReplyBot, diagnostyka i automatyczny restart radia, sniffer, nowe warianty sprzętu.
- Integracja MT_SW_INTEGRATION: własny panel w Home Assistant (wiadomości, węzły, mapa, konfiguracja radia, Mesh Link Planer, aktualizacja firmware OTA, sniffer), całość po polsku.

## EN
- MT_SW is a regional LoRa mesh (Meshtastic) project for the Świętokrzyskie region of Poland; site: mt-sw.pl; MT-SW organisation on GitHub. Authors: Spontaniczne Majsterkowanie and cheaterenator.
- Parts: the MT_SW_APP app (this app: Android and desktop, a fork of the Meshtastic app), the MT_SW_FIRMWARE firmware (a fork of the official Meshtastic firmware), the MT_SW_INTEGRATION Home Assistant integration. The protobufs are not developed: the original ones are used.
- Network settings in the Świętokrzyskie region: the official Narrow Fast preset, region EU_N_868, frequency 869.44165 MHz. MT_SW has no preset or region of its own. MT_SW firmware starts with Narrow Fast by default, but the region has to be chosen.
- MT_SW does not bypass the main Meshtastic network: it has its own solutions but stays fully compatible with it.
- Many people in the network are amateurs without a radio licence: we talk about "regulations", not "licence".
- The MT_SW_APP app: network health, Mesh Link Planer (link and range planner with terrain), LNA correction and signal rating, sniffer, on-demand diagnostics, remote GPIO, black-and-white themes, node colour picking, app scaling, offline translator and AI assistant (experimental), all under "MT_SW features" in Settings.
- The MT_SW_FIRMWARE firmware: node statistics sent over the mesh (CPU, flash, RAM, Wi-Fi), direct messages never reach MQTT, full position precision, ReplyBot, radio diagnostics and automatic restart, sniffer, new hardware variants.
- The MT_SW_INTEGRATION integration: its own Home Assistant panel (messages, nodes, map, radio configuration, Mesh Link Planer, OTA firmware update, sniffer), fully in Polish.
