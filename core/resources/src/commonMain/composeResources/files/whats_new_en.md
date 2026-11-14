# 5 | Version v5 (2.8.5)
## new | New features
- **SOS mode.** An "SOS" screen with a big button: an alarm with your position to the chosen channels (several can be chosen) and as a direct message to chosen nodes, also unmonitored ones. It is sent by holding the button, with repeating and confirmation. The message has the alarm bell, so for the receivers it arrives as an alarm.
- **SOS at hand.** A red "SOS" button with a yellow border next to the plate of your node in the top bar of every card (holding it for at least 2 s sends the alarm; turn it off or choose the cards in the SOS screen), a tile in the Android quick settings, a "SOS MT_SW" shortcut (Siri, Shortcuts), an item in the tray icon menu on the computer, and an SOS widget on Android and iPhone/iPad (also the lock screen).
- **Alarm when an SOS is received.** An SOS from another copy of the app (it has a hidden mark, so an ordinary text with the word SOS starts nothing) opens the conversation, covers the whole screen with a big SOS and rings again and again until you confirm it. On Android and iPhone/iPad also on a locked phone (a siren in a loop, vibration, the torch). The alarm window has ready answers to the sender, "Show on the map" and "Open the conversation", and confirming lets the sender know the SOS was seen. The "Test the alarm" button in the SOS screen tries it on this device.
- **SOS: chosen nodes, quiet hours, history.** The full alarm can be limited to chosen nodes and quiet hours can be set. Received SOS are saved in the history with "Open the conversation" and "Show on the map" buttons. The "Send SOS again" button repeats the alarm when nobody confirmed. The quiet hours have a list of exceptions of their own, for each node in the list you can choose the sound (siren, bell, silent), the history can be saved to a file, and repeating can be set to "No limit".
- **Neighbours on a map.** In the neighbours window of a node a new "Show on map" button: only the node and its neighbours with lines and the signal colour (SNR); when some have no position, a note says so.
- **Network coverage on the map.** The layer draws the approximate range of every node with a position (with the terrain), and a new "Propagation (weather forecast)" layer shows where the weather stretches the range; several areas can be added.
- **Node watchdog and availability.** A notification when a chosen node goes silent (with quiet hours and one shared notification for many nodes), and a chart of the availability history of a node.
- **Message archive.** An automatic copy of messages in a chosen folder, restoring from CSV, text and Markdown files, password encryption.
- **Range from measurements.** The route of a node coloured by signal strength, a comparison of two routes and a save of the range of the whole network to KML.
- **Terrain panorama.** Buildings as solids, forests, fog, light by the real sun, a panorama quality setting.
- **Link planner.** A comparison of results, antenna direction and tilt, antenna types, buildings and ground reflections on the map.
- **Statistics widget.** A new widget on Android, and on iPhone/iPad also small widgets for the lock screen; the plate of our node, a choice of which nodes to show.
- **iPhone and iPad like Android.** A QR code scanner, the event theme, import of KML/KMZ/GeoJSON layers, the terrain view with the camera, offline maps, background work, the full AI assistant, checks for new versions, the local TAK server, the Siri shortcut "MT_SW network status" and notification settings like on Android.
- **Computer.** The "Start with the computer" option, with a start in the tray (the window does not open), and a single copy of the program by default.
- **Look.** "Saturated" and "Very saturated" themes, categories in the theme choice, a flat app icon option.
- **Data.** "Add data from a backup" and synchronisation of the device statistics.
- **MT_SW_FIRMWARE v3.** A query for LAN network statistics in On-demand diagnostics and reading of the phone's Bluetooth signal strength.
## better | Improvements
- The plate of your node is now in the top bar of all cards, also in Settings, the Assistant, the Translator and Network health.
- Map: the filters "Max. number of hops" and "Max. distance from my node", a "choose node" list for nodes in the same place, more exact touching of the bubbles and smoother panning.
- The bottom tabs open the page where you left it, and a tap on a notification opens the conversation it came from.
- Charts skip readings with a wrong node clock, the database writes faster, and the logs on iPhone/iPad are much faster.
- Widget: temperature and humidity in one line, rarer rebuilds of the data.
- Panorama: smoother ridges, new textures of mountains and forests, faster drawing of buildings; the buildings download in small pieces and each is saved at once.
- Firmware update over Bluetooth on iPhone/iPad finds the device in update mode without a MAC address.
## fix | Fixed bugs
- "Start with the computer" works on Windows and in the AppImage on Linux.
- iPhone/iPad: shifted taps on the map bubbles, no notification sound on a locked phone, an error of the backup, the screen going off during an update.
- Remote administration asks for the MT_SW_FIRMWARE version of the remote node too.
- A GPIO write waits for the confirmation of the node and shows the result.
- Bluetooth no longer writes a separate log line for every packet.
- Ranges from a route recognise positions heard directly also when the radio gives no kind of transport.
- The neighbours window had two buttons that did the same; now there is one "Close".
- The download of buildings goes on after leaving the screen instead of being cut off silently.
## orig | Changes against the original
- Merged changes of the original (version 2.8.5 and later): among others the radio setup wizard, QR/NFC connection codes, an nRF52 bootloader update over Bluetooth and newer libraries.
- The "Local statistics" and "Node tracker" widgets were removed: one MT_SW widget is left.
- iPhone/iPad: the oldest supported system is iOS 17, and the app identifier is pl.sw.mt, like on Android.
- The build was adapted to the new Gradle.
# 0 | From the start to version v4 (2.8.4)
## new | New features
- **Network health.** Tabs for power, signal, airtime and environment, a "Network condition" card, a summary with rankings and export to PDF, HTML, Markdown, text and CSV, charts in the node details.
- **AI assistant, translator and dictation offline.** One engine (llama.cpp) and one choice of models (Gemma, Qwen, Bielik, DeepSeek), using the NPU and the graphics card; the assistant calculates by itself, knows the app and the network, remembers between conversations, shows buttons to screens and settings; optional internet; reading aloud. AI content is marked.
- **Link planner (Mesh Link Planer) and antenna calculator.** A native planner with terrain, obstacles, live weather and the range on the map, a cable and connector wizard, the dimensions of 13 antenna types.
- **Network view (terrain panorama).** The silhouette of the terrain all around with the nodes, turned by the compass, offline terrain, an AR view from the camera.
- **On-demand diagnostics and sniffer.** Queries to nodes (port 354) and a radio and MQTT sniffer on one screen with saving and grouping of duplicates.
- **MT_SW features and documentation.** A separate screen with the features of the fork, a knowledge base, all the documentation in the app in Polish and English, the welcome screen "What MT_SW adds".
- **Adaptive notifications** (Android 16+) and notifications about new versions of the app.
- **Routes and neighbours.** Traceroute on the map with signal strength, signal quality lines between nodes, a neighbours icon in the node list.
- **Nodes.** The name of the relay node, adding a contact by hand, cleaning of the node database (also automatic and over Bluetooth), the node colour when generating a key, extra telemetry of MT_SW_FIRMWARE v3 and Wi-Fi strength.
- **Chat.** Long messages split and put together, photos by link, a preview of images from links, translation before sending, reading aloud.
- **Themes and look.** Black-and-white themes, "App scaling", any node colour, status colours matching the app.
- **Computer and iPhone/iPad versions.** The app for macOS, Windows and Linux with a welcome screen and the base of the iPhone/iPad app.
- **Support.** "Support the project", reporting bugs and ideas, collecting logs.
## better | Improvements
- A smoother app: lighter node and conversation lists, the map, node details and Network health with a lot of data, a faster start on the computer.
- Modem preset names in Polish with the English name in brackets, Polish names of device roles, a full Polish translation.
- The same colours across the app: gold = good, red = medium, purple = bad.
- Noise is rated after the LNA correction, the SNR rating depends on the preset.
- LoRa settings: frequency offset, coding rate, Narrow Fast preset and 869.44165 MHz by default.
- Offline maps also for the default OpenStreetMap map, the default OpenStreetMap map and smaller plates with a tail.
- "i" descriptions for features, calmer information windows, a scroll bar on the computer.
## fix | Fixed bugs
- Reconnecting Bluetooth to an already paired device (Xiaomi/MIUI).
- Empty drop-down lists in the packaged computer version.
- Recovering a device stuck in DFU mode and updating with an own file for unknown devices.
- The app closing when using the translator and with a huge amount of data in Network health.
- Cleaning nodes over Bluetooth works to the end; fixed sending photos by link and lost diagnostic queries.
- Computer: doubled screens in the navigation and wrong columns after a saved session.
- The right count of hops in the route history and "Route time" instead of an English text.
## orig | Changes against the original
- Renamed to MT_SW_APP with its own icon and its own identifier, so it installs next to the original.
- A native link planner instead of the external Site Planner.
- The NLLB-200 translator model was removed (its licence does not match ours).
- Outdated device roles hidden (REPEATER, ROUTER_CLIENT); "Heap" is now called RAM.
- Further merges with the original (protobufs, libraries, translations, new features), with our Polish translations first.
