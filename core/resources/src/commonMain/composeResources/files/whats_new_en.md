# 5 | Version v5 (2.8.5)
## new | New features
- **SOS mode.** An "SOS" screen with a big button: an alarm with your position to the chosen channels (several can be chosen) and as a direct message to chosen nodes, also unmonitored ones. It is sent by holding the button, with repeating and confirmation. The message has the alarm bell, so for the receivers it arrives as an alarm.
- **SOS at hand.** A red "SOS" button with a yellow border next to the plate of your node in the top bar of every card (holding it for at least 2 s sends the alarm; turn it off or choose the cards in the SOS screen), a tile in the Android quick settings, a "SOS MT_SW" shortcut (Siri, Shortcuts), an item in the tray icon menu on the computer, and an SOS widget on Android and iPhone/iPad (also the lock screen).
- **Alarm when an SOS is received.** An SOS from another copy of the app (it has a hidden mark, so an ordinary text with the word SOS starts nothing) opens the conversation, covers the whole screen with a big SOS and rings again and again until you confirm it. On Android and iPhone/iPad also on a locked phone (a siren in a loop, vibration, the torch). The alarm window has ready answers to the sender, "Show on the map" and "Open the conversation", and confirming lets the sender know the SOS was seen. The "Test the alarm" button in the SOS screen tries it on this device.
- **SOS: chosen nodes, quiet hours, history.** The full alarm can be limited to chosen nodes and quiet hours can be set. Received SOS are saved in the history with "Open the conversation" and "Show on the map" buttons. The "Send SOS again" button repeats the alarm when nobody confirmed. The quiet hours have a list of exceptions of their own, for each node in the list you can choose the sound (siren, bell, silent), the history can be saved to a file, and repeating can be set to "No limit". Optionally the alarm goes off only after a second SOS from the same node. On the sender's side the "Who confirmed the SOS" button shows the list of confirmations. A channel or node can have a separate SOS text, and the history cleans itself after the chosen number of days. When someone answers your SOS, the repeating stops, you get a notification, and the list of confirmations can be saved to a file. "Calling for help" has the bell sound, the SOS notification has answer buttons, and on the map the nodes that answered have a ring.
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
- **Look.** "Saturated" and "Very saturated" themes, categories in the theme choice, a flat app icon option; the dark background is grey instead of bluish, and pure black in the "Saturated" and "Very saturated" themes (good for AMOLED screens).
- **Floating navigation bar.** The bottom tab bar (and on a computer or tablet the side bar too) is a rounded "island" with a name under each icon and the open tab highlighted; it looks the same on Android, the computer and iPhone/iPad, and a page's content ends above it, so the last item can always be scrolled into view. In "MT_SW features" the switch "Floating navigation bar at the bottom" brings the old bar back. Optionally (off by default) the bar hides while scrolling down (after a short, medium or long scroll) and returns on scrolling up or only at the very top of the page. The bar also has new icons (an outline, filled when open; the old ones can be brought back) and a vibration when the tab is changed.
- **Look of the elements.** In "MT_SW features" the section "Look of the elements" (all versions): for the bottom bar, the cards (devices, chats), the message box and the search you choose the look — plain (as before), with depth (shadow and light edges), see-through or glass (blurs what is under the element; real blur on the bar, a see-through pane on Android older than 12) — plus the strength of the effect and the blur. The effects are off by default. Also an option for a slightly grey white in light themes, a strong vibration while the send button is held (until you let go or the message is sent) and a vibration when the tab is changed. The chat and device lists scroll under the bar, so with glass they show through it. It uses the Haze library (Chris Banes, Apache-2.0).
- **Channel description.** Your own note of up to 200 characters on a channel (in the channel editor), kept on the phone only. An optional switch shows it in the conversation list in place of the last message.
- **Data.** "Add data from a backup" and synchronisation of the device statistics.
- **MT_SW_FIRMWARE v3.** A query for LAN network statistics in On-demand diagnostics and reading of the phone's Bluetooth signal strength.
## better | Improvements
- The plate of your node (colour and short name) is in the top bar of the main cards, and the red SOS button is in the top bar of every screen, also in a conversation and in Network health; while it is held its border is traced around in white, and the SOS screen has the same button.
- SOS: you can leave every channel unchosen (the alarm then goes only to the chosen people), and "Test the alarm" is a plain button that tries the alarm at once.
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
# 0 | From the beginning of the app
## new | New features
- **Network health.** Tabs for power, signal, airtime, environment, traffic, neighbours and resources (CPU, memory, flash), a "Network condition" card, a summary with rankings, charts in node details and export to PDF, HTML, Markdown, text and CSV; the data is decoded live from the mesh log.
- **AI assistant, translator and dictation offline.** One engine (llama.cpp) and one choice of models (Gemma, Qwen, Bielik, DeepSeek), using the NPU and the graphics card. The assistant calculates by itself, knows the app and the network, remembers the conversation, can read aloud and, if you want, use the internet; the translator works without a network and dictation turns speech into text in the chat.
- **Link planner (Mesh Link Planer) and antenna calculator.** A native planner with terrain, obstacles, live weather and the coverage on the map, a cable and connector wizard and the dimensions of 13 kinds of antennas.
- **Network view (terrain panorama).** The silhouette of the terrain all around with the nodes, turned by the compass, offline terrain and a camera view (AR).
- **On-demand diagnostics and sniffer.** Ten kinds of queries to nodes (port 354) and a radio and MQTT sniffer on one screen: grouping of duplicates with a list of receivers, saving and loading the log (txt, JSON, CSV), coloured packet categories, decoding on tap and a buffer of 5000 packets.
- **Device logs and signal quality.** Live device logs with search and export, LNA gain correction for noise and RSSI, an SNR rating that depends on the preset and status colours that agree across the whole app.
- **Map.** A shared MapLibre engine on Android and the computer, the OpenStreetMap map by default, terrain shading, a weather radar, offline maps, spreading nodes with the same position, better clustering and a view fitted to all the nodes.
- **Nodes.** The name of the relaying node and the full list of relays in the delivery status, adding a contact by ID, remote favourite and ignore over the LoRa network, remote GPIO control, clearing the node database (also automatic and over Bluetooth), a node colour when generating a key, extra MT_SW_FIRMWARE telemetry.
- **Routes and neighbours.** Traceroute on the map with signal strength, signal quality lines between nodes, a neighbours icon in the node list and passive NeighborInfo.
- **Chat.** Long messages split and put together on the fly, photos by link and a preview of images from links, translation before sending, reading aloud, quick command buttons (/ping, /hello, /test) and Quick Chat templates.
- **MT_SW features and documentation.** A separate screen with the features of the fork, a knowledge base, all the documentation in the app in Polish and English, a "What MT_SW adds" welcome screen and the restored Traffic Management screen.
- **Themes and look.** Black-and-white themes, "App scaling", any node colour, an own icon and colour palette and an "About" screen.
- **Notifications.** Adaptive notifications (Android 16+) and notifications about new versions of the app.
- **App for the computer and iPhone/iPad.** Versions for macOS, Windows and Linux with a welcome screen, a quick scroll bar and a third column that follows the selected node, and the base of the iPhone/iPad app.
- **Support.** "Support the project", reporting bugs and ideas, collecting logs.
## better | Improvements
- A smoother app: lighter node and chat lists, the map, node details and Network health with a lot of data, a faster start on the computer.
- A full Polish translation (over a thousand texts), modem preset names in Polish with the English name in brackets, Polish device role names.
- The same colours across the whole app: gold = good, red = fair, purple = bad; trust in a node in gold, a key mismatch in red.
- Noise is rated after the LNA correction, the SNR rating depends on the preset.
- LoRa settings: frequency offset, coding rate, the Narrow Fast preset and 869.44165 MHz by default; warnings about the narrow band and about leaving the region.
- Offline maps also for the default OpenStreetMap map and smaller badges with a tail.
- "i" descriptions next to features, calmer information windows, a scroll bar on the computer; on the computer Enter is a new line and Ctrl+Enter sends the message.
## fix | Fixed bugs
- Reconnecting Bluetooth to an already paired device (Xiaomi/MIUI).
- Empty drop-down lists and a map that did not work in the packaged computer version.
- Recovering a device locked in DFU mode and updating with an own file for unknown devices.
- The app closing when using the translator and with a huge amount of data in Network health.
- Clearing nodes over Bluetooth now works to the end; sending photos by link and losing diagnostics queries fixed.
- Computer: double screens in the navigation and wrong columns after a saved session.
- Correct hop counts in the route history and "Route time" instead of English text; the position trail map no longer starts in the ocean.
## orig | Changes against the original
- Renamed to MT_SW_APP with an own icon and an own identifier, so the app installs next to the original.
- A native link planner instead of the external Site Planner.
- The NLLB-200 translator model removed (its licence does not agree with ours).
- Obsolete device roles hidden (REPEATER, ROUTER_CLIENT); "Heap" is now called RAM.
- Further merges with the original (protobufs, libraries, translations, new features), with our Polish translations taking priority.
