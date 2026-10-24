---
title: MT_SW features
parent: User Guide
nav_order: 23
last_updated: 2026-10-24
description: An overview of what MT_SW_APP adds to the original Meshtastic app — remote GPIO control, network health, links on the map, Mesh Link Planner, antenna calculator, on-demand diagnostics, Sniffer, debug panel with device logs, app scale and more.
aliases:
  - mt-sw
  - gpio
  - remote-control
  - mt-sw-features
  - fork
  - features
  - network-health
  - planner
---

# MT_SW features

MT_SW_APP is built on the original Meshtastic app. On top of everything the original does, it has many features of its own that make life in a mesh easier, especially in the Świętokrzyskie network. This page describes them one by one and tells you where to find them.

> ℹ️ **Note:** Some features (On-demand diagnostics, radio Sniffer, node resources, device logs over Wi-Fi and USB) need MT_SW firmware. On regular firmware the app works normally, but these features get no answer.

## Remote management

The app lets you control other nodes over the mesh, without touching the device in the field. Everything is in the node details.

- **Remote GPIO control** — in the node details (Remote Hardware module) you type a pin number, and the app computes the bit mask and sends the remote node a command to switch the pin on ("On (1)"), off ("Off (0)") or read it. You can use it to switch relays or lights, or to restart equipment attached to the node. The buttons work only after keys have been exchanged with the node, because commands are encrypted. The GPIO card header has an info icon with a step-by-step description of the setup.
- **Remote favorite and ignore** — you can favorite or ignore a node not only on your phone but also in the remote node, over LoRa. The app waits for a real confirmation from the network (routing ACK), so you know the change arrived.
- **Adding new devices to the database** — you can add a new node manually by typing its ID (e.g. `!a1b2c3d4`), to your own node's database or remotely to another node's database over the mesh. The ID format is the same across the app, and the app validates what you type.
- **Quick commands** — the `/ping`, `/hello` and `/test` buttons send a direct message even to nodes whose role normally blocks manual messages. Handy for a quick check of a node in the field. They work from firmware 2.8.0.
- **Neighbors** — the neighbor log also shows overheard broadcasts from other nodes, not only replies to your own requests. In the Neighbor Info module settings you pick the interval from a list of hours (6, 8, 10, 12, 18 and 24).
- **Relay names** — the "Delivery status" window shows the full list of names of nodes that relayed the message. The relay name is also visible directly in the node list and in the node details.
- **Traceroute** — when the last node before us did not write itself into the route and exactly one known node matches it, the route window shows it as "Probably: Name" instead of "Unknown hop".

> ⚠️ **Important:** Remote commands change real hardware. Make sure you are controlling the right node before sending a GPIO command.

## Network health

The **Network health** tab is in the bottom navigation, between Nodes and Map. It shows in one place how all nodes are doing.

- **"Network condition" card** — a ring with the percentage of nodes in good condition, the number of nodes in each rating and how many are online. Tapping a rating (e.g. "2 Bad") shows only nodes with that rating; every tab remembers its own filter.
- **Metric tabs** — power, signal, airtime, environment, resources, traffic and neighbors. Each node has a coloured bar, a value and a small trend chart. In Signal the main value is the noise floor after LNA correction.
- **Colours** — gold is good, red is fair, purple is bad, white is very bad, blue is no data. The **(i)** button at the top describes all colours and thresholds. The battery level in the node list uses the same colours.
- **Search and sorting** — the "Search node…" field, sorting "By value" or "By activity", the "Problems only" filter, favorites on top and hiding nodes without data.
- **Collapsing the summary card** — dragging up folds the whole "Network condition" card so more room is left for the list; dragging down expands it. You can also drag by grabbing the card itself. It works on desktop too. By default folding is locked; the switch "Lock hiding network health when scrolling the list" is in the **(i)** window.
- **Node details** — charts in several views to choose from: line (with coloured zones), clock, bars and heat map; ranges of 1 h, 24 h, 2 days, 7 days, 2 weeks, 30 days and "All"; a "Time in each state" bar.
- **Network summary** — the list icon at the top opens node rankings (best signal, quietest, nearest, most messages and more), pie and bar charts, and battery and signal distribution. You can share the result or save it as PDF, HTML, Markdown, text or CSV.
- **Device WiFi signal** — when the firmware sends it, you see the WiFi strength, the number of link drops and the reason of the last drop (in Signal quality and in Network health).
- **Widget** — on Android you can add the "Local Stats" widget to the home screen. See [Home screen widget](widget).

## Signal quality and LNA correction

- **LNA correction** — in Settings → LoRa you enter your device's LNA gain, and the app reduces the displayed noise and RSSI by that value. In a foreign node's details there is a separate field that corrects only that node's noise. The correction is applied at display time, so raw data stays unchanged.
- **Signal rating** — colours as in connection statuses (gold, red, purple, white). The help screen shows the actual thresholds for the selected preset.
- **Reversing power and current** — two switches for a sensor connected the other way round: swapping "power" with battery percent, and reversing the sign of the current. You set them per node in the info window ("i" icon). They change only the display.
- **Device WiFi** — a separate line on the chart, cards with signal strength and drops, and CSV export with the "Save WiFi" button.

## Map

- **Links between nodes** — thin lines join nodes that exchange packets, coloured by signal quality (SNR). Lines come from neighbor reports, traceroutes from the last day and directly heard nodes. They are switched in the map filters ("Show links between nodes"); on by default.
- **Show on map** — in the node details, under its position, there is a **Show on map** button. It switches to the main map, zooms to the node and draws it on top of other nodes standing at the same place.
- **Traceroute on the map** — every hop is a separate line coloured by signal quality, with an arrow and the SNR value.
- **Nodes at the same place** — they are subtly offset, so after zooming in you can tap each one. Badges are small, with a tail pointing at the exact position.
- **Offline maps** — work for OpenStreetMap too, which is the default map. Download small areas, because the OSM server does not like bulk downloads.
- **Terrain and weather overlays** — hillshade and the weather radar are on by default.
- **Smoothness** — with many nodes the map pans and zooms more smoothly, and it always opens fitted to all nodes.
- **Desktop** — the map also works in the desktop version, with the same layers and filters as on the phone.

More about the map: [Map and waypoints](map-and-waypoints).

## Mesh Link Planner

The planner simulates the range of a radio link between two points, **A** and **B**, entirely inside the app, on phone and desktop.

- You set each point from the node list, by tapping the map, by coordinates or from the connected station.
- Terrain height comes from the Mapterhorn DEM model.
- For each side separately you set power, antenna gain, mounting height and feeder loss (manually or with a wizard that has a database of cables and connectors).
- It supports amateur bands from 169 MHz to 2.4 GHz and any frequency from 20 MHz to 20 GHz.
- It computes the ITM (Longley-Rice) model, Earth curvature and the first Fresnel zone, draws a terrain profile and gives the link budget plus antenna bearings and angles.
- **Exact terrain** — optionally downloads real buildings and forests from OpenStreetMap and takes their height into account.
- **Live weather** — optionally includes propagation conditions from the current weather (Open-Meteo).
- **Range all around** — draws the coverage as a coloured raster on the map, with an opacity slider.
- **Comparison with a measurement** — the result can be compared with the last measured SNR and RSSI.
- **Export** to PDF, CSV, KML, GeoJSON, JSON (all the data of the report: sections, terrain profile, points and coverage cells) and PNG.

The planner is opened with the **Planner** button in the node details, next to the compass.

## Antenna calculator

**Settings → Advanced → MT_SW features → Tools → Antenna calculator.** It works out antenna sizes for any frequency and draws the antenna with its parts A, B, C… and their values.

- **Antenna types:** quarter-wave ground plane, half-wave dipole, folded dipole (with a λ/2 coax balun), 5/8-wave vertical, J-pole, Slim Jim, coaxial collinear (COCO), Yagi-Uda (3–20 elements, corrected for a metal boom), Moxon, Biquad, axial-mode helix, patch on a PCB and coax sections λ/4 and λ/2.
- **Material:** the wire or tube diameter and insulation change the element lengths; antennas made of coax use the chosen cable's velocity factor, the patch the board's permittivity and thickness.
- **Properties:** wavelength, the shortening factor used, impedance, gain, polarisation and building tips.
- **Feed line:** transmit power (taken from your radio by default; you can change it or pick the band limit), a cable from the list (loss worked out for the chosen frequency) or your own, cable length, connectors and other parts, SWR and antenna gain. Result: the loss of each part and in total, power at the antenna, EIRP and ERP, comparison with the band limit (e.g. 500 mW ERP in 869.4–869.65 MHz) and how the losses affect range.

The formulas come from established sources (Stutzman & Thiele, Rothammel/DL6WU, L. B. Cebik, J. D. Kraus, Balanis, Hammerstad, cable makers' tables). The result is a very good starting point, but every antenna is still tuned with an SWR meter or NanoVNA. Under its antenna calculations the assistant gives a button to the calculator with the same frequency and antenna type.

The **Save result…** button at the bottom saves the input, dimensions, properties and feed-line results as PDF (a text report, without the drawing), Markdown, TXT, CSV (just the dimension table) or JSON.

The feed line is worked out just like in the Mesh Link Planner: any number of connectors joined by cable sections, with a cable database from datasheets (loss at your frequency), ready-made sets and a breakdown of the loss of each part. In **My antennas** you save the antenna together with its feed line, power, SWR and gain under your own name, and later load or delete it. In the Mesh Link Planner, in the transmitter and antenna section, you can pick a saved antenna (it sets the power, gain and feed line in one tap) or use the **Work it out in the calculator** button, which opens the calculator on the planner's frequency and power. Under the results the calculator tells which part of the line loses the most, how much a shorter cable would save, and can compare the current line with a saved antenna (losses, EIRP, range).

## On-demand diagnostics

In the node details (Administration → "On-demand diagnostics") you can ask any node in range for its current statistics instead of waiting for periodic broadcasts. There are 10 kinds of queries, including node stats (battery, uptime, CPU, memory, WiFi), ping with RSSI and SNR, the list of online nodes, routing error history, port counters, airtime activity and the MT_SW firmware version. The screen has an overview of capabilities with a counter of checked queries, and results are coloured (tiles, gauges, charts). All answers can be shared or saved as PDF, HTML, Markdown, text or CSV. This feature needs firmware with the OnDemand module.

## Sniffer

The **Sniffer** screen (Settings → Advanced) shows network traffic live, including packets the phone cannot decrypt.

- **Two sources in one screen** — Off, Radio or MQTT (one at a time). Each switch-on asks for confirmation and warns what may change. The MQTT sniffer works on desktop too.
- **Radio** — shows everything the connected node heard, including your own traffic. Needs MT_SW firmware with the sniffer module; without it the switch is greyed out.
- **The log stays** after the sniffer is switched off, until you clear it with the bin. A counter in the bar shows buffer fill (e.g. "1530/5000").
- **Duplicate grouping** — the same packet heard many times is one row with a list of gateways or relays. Copying a packet copies all copies.
- **Auto-scroll** and **decryption attempt** with known channel keys — separate switches.
- **Saving and loading the log** (txt, JSON, CSV), and tapping a packet shows its full content.
- Position and telemetry cards show the time written by the sender; an impossible time is marked as wrong.

## Debug panel

The debug panel (Settings → Advanced → Debug Panel) has an extra tab in this app. See also [Debug logs](debug-logs).

- **Log file on the computer** — the desktop version writes the app log (information level and above) and any crash to the file `mt_sw_desktop.log` in the `.meshtastic/logs` folder of your home directory, so it can be read and sent after the app has closed or crashed. A file is at most 5 MB, three older ones are kept.

- **Device logs** — the third tab shows live the logs sent by the radio, the same ones you see on the serial port. It has search, log-level colours and export. For logs to appear, enable "Debug logs" in the device security settings. Over Bluetooth this works with any firmware; over Wi-Fi and USB only with MT_SW firmware.
- The panel also shows packets the phone could not decrypt, and the raw content is available as hex.

## Messaging

- **Photos via link** — the app does not send a photo over LoRa; it sends a link, and the recipient sees a preview. Before sending there is a warning that the server is public. Previews of images from links are off by default and are enabled in Settings → Privacy. A photo opened full screen can be saved to the device.
- **Long messages** — text that does not fit in one packet is split into parts with a marker such as `[k9 2/4]` and assembled on the other side.
- **Quick chat** — comes with a ready set of templates on first launch.
- **Status colours** — delivered to the network and receipt confirmation are gold, just "delivered to the network" on a direct message is red, and a send error is purple.
- **Conversation list** — opens faster and more smoothly even with many messages.
- **Desktop** — Enter is a new line, Ctrl+Enter sends the message.

## Offline translator and assistant

The app has two optional AI features that run **entirely on your device, without internet**: a translator and an assistant. Both are **off by default** and are switched on separately in Settings → Advanced → MT_SW features (at the very top, above the firmware update) → "Offline translator and assistant". You can have only the translator, only the assistant, or both.

**Privacy.** Everything is processed locally. Once a model is downloaded, no texts or conversations are sent to the internet. The internet is only needed for the one-time model download.

**Models.** There are four models: "Translator only" and three assistant levels (light, standard, strong). Gemma 4 runs on Google's LiteRT-LM engine and uses the graphics chip (GPU) when the device supports it; otherwise the processor does the work. You can keep several downloaded at once, download and delete each one separately, and in one place, "AI model (assistant, translator and dictation)", choose one model for everything. Only that one model is in memory. The NLLB model ("Translator only") can only translate: choosing it while the assistant is on shows a warning that this turns off the assistant and dictation, and the assistant is turned off only after you confirm.

- **Translator only — NLLB-200 600M.** Translates, but has no assistant or dictation. Download about 870 MB, RAM about 2.3 GB, a phone with at least 4 GB of RAM is recommended.
- **Light — Gemma 4 E2B.** Translator and assistant in one file. Download about 2.6 GB, RAM about 3.5 GB, a phone with at least 6 GB of RAM.
- **Standard — Gemma 4 E4B.** Translator and assistant, better quality. Download about 3.7 GB, RAM about 5 GB, a device with at least 8 GB of RAM. It leaves proper names, numbers and addresses as they are.
- **Strong — Gemma 4 12B.** Translator and assistant, the best assistant answers. Download about 6.9 GB, RAM about 9.5 GB, a device with at least 16 GB of RAM.

On weaker devices large models can slow the app down or close it, which is why every model lists its sizes and recommended memory.

**Tab in the bottom bar.** When either mode is on, a "Translator" tab appears in the bottom bar. When the assistant is on too, the tab is called "AI" and has a sparkles icon, with two separate tabs inside: translator and assistant. The assistant works only on this tab; channels and private messages only have the translator. The assistant needs a Gemma 4 model; NLLB only translates.

**How the assistant works.** The chat looks like a messenger: your messages are on the right, the answers on the left, and at the start you can tap a suggested question. Press and hold a bubble to copy its text or to ask the same question again; when an answer fails, the question returns to the input box so you can send it again. The conversation can go on without end: the assistant keeps the older part in a short summary, and answers are not cut off. You type a question and the answer appears as it is written; the "Stop" button interrupts it at any time and "New conversation" starts over. The first answer (and the first translation) takes longer because the model is loaded into memory. **App data:** the assistant can only *read* data: your own node, any known node of the network (just name it in the question) with its whole recorded history and trend (for example the radio's RAM/heap, CPU, battery, channel utilization, signal), a summary of the whole network, the radio settings (without keys and passwords) and the latest messages. It never changes a setting and never sends a message. Ask for example "How is the heap memory changing?". Answers may be wrong — check important things. Conversations are saved **only on this device** and are never sent anywhere. The clock icon in the corner opens the history: tap a conversation to continue it, or delete it. "New conversation" starts from scratch and the previous one stays in the history.

**Experimental feature.** The translator and the assistant are experimental features. Their correct operation and answers are not guaranteed: a small model running on the device can be wrong, so always check important information.

**How the assistant understands questions.** First the model briefly recognises what you are asking (for example a message count, a node on the map, a setting, help with the app, or plain conversation), and it can only pick one of the known kinds. Then: exact things (counts, searches, node lists, packet routes, connection state, sensor readings) are written by the app's code in the app's language, so the numbers are right; for questions about the app the assistant shows where the setting is and quotes the fitting part of this manual (and for "what's new" the newest entries of the changelog), and questions about your network from data chosen for the question. On any other topic you can talk with the assistant freely. Before it writes an answer, the model thinks quietly for a moment: the answers are better, at the cost of a few seconds.

**The assistant goes where you ask.** When you write for example "show node Łysica on the map", "show the nearest router", "open the LoRa settings", "show the hop limit" or "do I have range to …", the app opens the map with the node marked, the right screen (the setting blinks yellow) or the link planner right away. "Write on channel … that …" opens the conversation with the text ready in the message box, and **you send it yourself**. The assistant never changes a setting and never sends a message. A button stays under the answer to go back there later.

**Assistant test.** Type `/test` in the assistant and send it: the assistant asks itself about 50 test questions (with a real node and channel name from your network) and shows the questions and answers in the chat, and at the end a full report with answer times and the model used, which can be copied (press and hold the bubble). On a computer the report is also saved to `assistant-test-results.md`. This way answer quality can be compared across devices and models. Nothing opens by itself during the test; the "Stop" button ends it.

**Copying the conversation.** Above the chat there is a "Copy whole conversation" button: it copies all questions and answers (including the names of the redirect buttons) to the clipboard, e.g. to paste into a message when reporting a problem. Next to it is "Save conversation…": it saves the conversation to a file in the format you choose — PDF (looks like the chat, best for showing someone the whole thing), Markdown, TXT, HTML or JSON (the data in a structured form, the easiest for a program to read). A single bubble is copied by pressing and holding it.

**Calculations.** The assistant does not guess numbers: the app works them out. Ask in any words, e.g. "how long is a 433 MHz dipole", "27 dBm in mW", "EIRP at 22 dBm with a 6 dBi antenna", "Fresnel zone for 15 km", "how far does a radio on a 40 m mast reach", "theoretical range on Narrow Fast", "how long does sending a 100-character message take" or "calculate 12*7+3". When you give no frequency, preset or power, it takes them from your radio or our network and says so. For weak range it checks your radio's settings and reception and suggests what to improve.

**Translator:**

- **Received messages** — the message menu has "Translate message". The original stays, and you can switch between it and the translation.
- **Outgoing messages** — when the field has text, a "Translate before sending" button appears next to it. It shows the original and the translation side by side; you can send the translation, put it in the field to edit, or cancel.
- **Separate screen** — the "Translator" tab: type a text, choose the languages (or "Detect automatically"), translate and copy the result.
- One model covers many languages and any pair between them, including Polish, English, German, Czech, Slovak, Ukrainian, Russian, French and Spanish.
- Translations can be inaccurate, especially names and jargon, so treat them as a hint. Text is translated sentence by sentence. If a translation is over 200 characters, the app warns that it will be sent in several parts over the radio.

**Buttons, app knowledge and voice (assistant).** The assistant knows the app: it has built-in knowledge from this documentation and the changelog and picks only the fitting part for each question. Under an answer it can show **buttons**: open a screen, show a setting (it blinks yellow for a few seconds), show a node on the map, open the link planner, or a message draft placed in the input field. When you directly ask to show or open something, the app opens it right away; otherwise the button waits for your tap. A message draft is **never sent automatically**. Long-press an answer to have it **read aloud**; on a phone there is also a "Dictate" button next to the question field. Above the chat field, when there is text to translate, a small grey "Translate" button appears.

**Internet for the assistant (off by default).** The assistant settings have a single internet switch. Off = the assistant works fully offline. On (after reading a warning, with a red note that it is on): the assistant may search the web for general information, and once a day the app checks our repository for news about the app (downloaded files are verified with a hash; if it fails, the built-in knowledge is used). The search engine sees your IP address and the words of the query. Web text is treated only as information, never as an instruction, and under an answer that used the internet a small note in the app's colour appears: "Answered with help from the internet", what was searched and the sources. Whether a question needs a web search is decided when the question is recognised, and the app runs the search itself.

**Counts and filters (assistant).** The assistant can count messages on a channel (today since midnight or the last hours, with who writes most), filter nodes (active in the last hours, favourites, by hardware, low battery, with position) read direct messages with a chosen node, show packet routes (traceroute) and neighbours (who hears whom), the connection state, unread messages, whether your messages were delivered, and waypoints. You can give any time span (minutes, hours, days, week, month, year, today, yesterday). Such answers are written by the program in the language of the app. The program computes the numbers, not the model, so they stay correct.

**Searching messages (assistant).** The assistant finds a word or phrase (for example in quotes) in the messages of all channels, of one channel or of the private chat with a chosen node; case and Polish accents do not matter. It gives the number of hits and the channel they were on.

**Assistant memory (off by default).** The assistant saves short notes about you (a few small files: who you are, your devices and network, how you like to talk, what you work on) and remembers them between conversations, so the conversation feels more natural. The notes stay on this device only. In the assistant settings you can view, edit and delete them, switch the memory off, and write **your own instructions** (for example how it should talk to you) that the assistant always follows. Above the conversation the assistant reminds you that it saves notes.

**Reading and translating a whole conversation.** The three-dots menu of a channel or direct message has "Read visible messages aloud", "Read all messages aloud", "Translate visible messages" and "Translate all messages" (and "Stop reading" while reading). "Visible" means what is on the screen; "all" means up to the 500 newest. Your own messages are not translated, and translated texts can be switched back to the original as usual.

**Add / share button in the top bar.** By default the round button (import, share, add contact) floats over the node and conversation lists. In "MT_SW features" you can move it to the top bar, next to the info icon, so it does not cover the list; turning the switch off restores the standard place.

**Translator – reading aloud.** Next to the translated text there is a "Read aloud" (and "Stop reading") button beside copy.

**Dictation.** The "Dictate" button in the assistant works the same on phone and computer, without any system service: it records the microphone and a downloaded Gemma model (E2B or E4B) turns the speech into text entirely on the device. Tap "Dictate", speak (up to about 30 seconds) and tap "Stop and insert". The phone asks for microphone permission on first use. The button appears only once an E2B or E4B model is downloaded. The microphone (small yellow circle) is also next to the send button in chats (channels and direct messages) and in the translator. It appears only when the assistant is on and its chosen model can hear (Gemma E2B or E4B). In chats and the assistant, the model chosen for the assistant is used for dictation (no second model is started). In the translator, dictation is available only when the translator's own model can hear (E2B or E4B); with the smallest translator model (NLLB) there is no button. A note is also shown in the AI settings. The "Headset microphone for dictation" switch makes dictation everywhere in the app record from a connected headset's microphone (Bluetooth, for example a car headset, wired or USB); without a headset the normal microphone is used.

**How long to remember received packets.** The Settings (also in "MT_SW features") have a field with a number of days (1, 2, 3, 7, 14, 30 or 60) for which the app keeps the received packets that statistics and charts are built from. The default is 60 days. Older packets are deleted. The history of each device is kept separately.

**Node colour picker.** The key colour dialog lets you pick any colour (code, hue/saturation/brightness sliders, or ready colours and recently used ones), choose "Fast" (up to 20 s, similar colour) or "Precise" (up to 3 min, very close colour), see the search time, cancel the search and compare the chosen colour with the result. The key is not saved automatically.

**Model licences.** The models are not part of the app: you download them from the internet and use them at your own responsibility, under their licences.

- NLLB-200 (Meta) is released under **CC BY-NC 4.0**, which means **non-commercial use only**.
- Gemma 4 (Google) is released under the **Apache 2.0** licence.
- Model answers can be wrong or invented. Do not base safety decisions on them.

## Node list and database

- **Search** — the field above the list filters the same list at once, with no second window.
- **Node list help** — sections about roles, security, connection and battery level with a legend of icons and colours.
- **Node roles in Polish** — device role names and descriptions are translated, and obsolete roles are hidden from the choice list.
- **Current contact time** — nodes with a fixed position show the real time of the last contact.
- **Clearing the node database** — on the "Clear node database" screen you can switch on automatic removal of inactive nodes (threshold 1–90 days, check every 1–30 days); it also works on desktop. There are the "Ignore date" and "Only nodes with a mismatched key" switches.
- **Unheard nodes** — a node from which a packet was just received over the radio is no longer marked as unheard.

## Settings and appearance

- **MT_SW features group** — Settings has one entry, "MT_SW features", that opens a separate screen with our settings: the switch for previewing images from links in chat, update notifications (two switches) and the node list layout. Below are shortcuts to settings that have a natural home elsewhere: app scaling, theme, node colour (opens the security settings with the "Pick colour" button blinking), LoRa (LNA correction and frequency offset), node database cleaning, Sniffer and the debug panel. The last entry, "Other features", describes the features that live next to a node (node LNA correction, inverted current and power, remote GPIO, on-demand diagnostics) and where to find them. Every item is also found by the settings search.
- **App scale** — in the app settings you choose the size of text and elements from 50% to 200%, independently of the phone settings. The default is "Same as phone". It works on desktop too.
- **LoRa settings** — a "Frequency offset (kHz)" field, coding rate 4/5 for Narrow presets, full resolution of the overridden frequency, a warning description for "Override duty cycle" and the restored Traffic Management screen.
- **Security** — key backup on desktop too, and a node colour choice when generating a key.
- **Firmware update** — a custom file can be flashed to a device not on the list too, with risk warnings; recovery of a device in DFU mode also works after a custom file.
- **Bluetooth** — improved reconnecting to an already paired device, node detection on Windows, and the recent network devices list remembers 20 entries.
- **New version notifications** — the app checks GitHub for a newer release and shows a notification with a link.
- **Reporting bugs and ideas** — the "Report a bug or idea" item in settings leads to our Discord and groups.
- **Look and brand** — own name, icon and colours (gold and navy), a coloured connection status icon, installation next to the original Meshtastic app. On Windows a new version installs over the old one without uninstalling.
- **Desktop** — a draggable scroll bar with a visible track on long lists, the third column follows the selected node, and an "Introduction" item in settings.

## Help and documentation

All help and documentation in the app are available in Polish. You can change the text size on help pages with the "A−" and "A+" buttons at the top. On first launch, after the welcome screen, there is a second card with a short description of MT_SW features; you can see it again in settings under "Introduction". See [Help and documentation](help-and-docs).

## Related topics

- [Nodes](nodes) — the node list and node details
- [Map and waypoints](map-and-waypoints) — the map, layers and waypoints
- [Node metrics](node-metrics) — telemetry charts that Network health is based on
- [Help and documentation](help-and-docs) — how to use in-app help
