---
title: MT_SW features
parent: User Guide
nav_order: 23
last_updated: 2026-10-18
description: An overview of what MT_SW_APP adds to the original Meshtastic app — remote GPIO control, network health, links on the map, Mesh Link Planner, on-demand diagnostics, Sniffer, app scale and more.
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

MT_SW_APP is built on the original Meshtastic app. On top of everything the original does, it has a few features of its own that make life in a mesh easier, especially in the Świętokrzyskie network. This page describes them briefly and tells you where to find them.

> ℹ️ **Note:** Some features (On-demand diagnostics, node resources) need firmware with the OnDemand module. On regular firmware the app works normally, but these features get no reply.

## Remote control

The app lets you control other nodes over the mesh without touching the device in the field. Everything is in node details.

- **Remote GPIO control** — in node details (the Remote Hardware module) you enter a pin number, and the app calculates the bit mask itself and sends the remote node a command to switch the pin on, off or read it. This way you can remotely switch relays or lighting, or restart equipment connected to the node. The buttons work only after keys have been exchanged with the node, because the commands are encrypted. The GPIO card header has an information icon with a description.
- **Remote favourites and ignore** — you can add a node to favourites or ignore it not only on your own phone but also in the remote node, over LoRa. The app waits for a real confirmation from the network (routing ACK), so you know the change arrived.
- **Quick commands** — the `/ping`, `/hello` and `/test` buttons send a direct message even to nodes whose role normally blocks manual messages. They are handy for a quick check of a node in the field.
- **Adding new devices to the database** — you can add a new node by hand by entering its ID (for example `!a1b2c3d4`), to the database of your own node or remotely, to the database of another node over the mesh. The ID format is the same everywhere in the app.

> ⚠️ **Important:** Remote commands change real hardware. Make sure you are controlling the right node before you send a GPIO command.

## Network health

The **Network health** tab is in the bottom navigation, between Nodes and Map. It shows how all your nodes are doing in one place.

- **"Network health" card** — a ring with the percentage of nodes in good shape and the number of nodes in each rating. Tapping a rating (for example "2 Bad") shows only the nodes with that rating.
- **Metric tabs** — power, signal, airtime, environment, traffic, resources and neighbors. Each node has a coloured bar, a value and a small trend chart.
- **Colours** — gold is good, red is fair, purple is bad, white is very bad, blue is no data. The **(i)** button at the top describes every colour and threshold.
- **Search and sorting** — a "Search node…" field, sorting by value or by activity, a "Problems only" filter, favourites on top and hiding nodes without data.
- **Folding the summary card** — dragging up folds the whole "Network health" card away to leave more room for the list; dragging down unfolds it. Folding is locked by default. The "Lock hiding of network health when scrolling" switch is in the **(i)** window.
- **Summary** — the list icon at the top opens a ranking of nodes (best signal, quietest, most messages and more) with charts. You can save the result as PDF, HTML, Markdown, text or CSV.
- **Widget** — on Android you can add the "Local Stats" widget to the home screen. See [Home Screen Widget](widget).

## Map

- **Links between nodes** — thin lines connect nodes that exchange packets, coloured by signal quality (SNR). The lines come from neighbor reports, from traceroutes of the last day and from nodes heard directly. You turn them on or off in the map filters ("Show links between nodes"); they are on by default.
- **Show on map** — in node details, under the position, there is a **Show on map** button. It takes you to the main map, zooms to the node and draws it on top of other nodes standing in the same place.
- **Traceroute on the map** — every hop is a separate line coloured by signal quality, with an arrow and the SNR value.
- **Offline maps** — they also work for OpenStreetMap. Download small areas, because the OSM server does not like bulk downloads.
- **Terrain and weather overlays** — hillshade and the weather radar are on by default.

More about the map: [Map and waypoints](map-and-waypoints).

## Mesh Link Planner

The planner simulates the range of a radio link between two points, **A** and **B**, entirely inside the app, on the phone and on the desktop.

- You can set each point from the node list, by tapping the map, with coordinates or from the connected station.
- Terrain height comes from the Mapterhorn DEM model.
- For each side you set power, antenna gain, antenna height and feeder loss (by hand or with a wizard that has a database of cables and connectors).
- It supports amateur bands from 169 MHz to 2.4 GHz and any frequency from 20 MHz to 20 GHz.
- It calculates the ITM (Longley-Rice) model, Earth curvature and the first Fresnel zone, draws the terrain profile and gives the link budget.

Open the planner with the **Planner** button in node details, next to the compass.

## On-demand diagnostics

In node details (Administration → "On-demand diagnostics") you can ask any node in range for its current statistics instead of waiting for periodic broadcasts. There are 10 kinds of queries, including node statistics (battery, uptime, CPU and memory), ping with RSSI and SNR, the list of online nodes, the routing error history and airtime activity. You can save and share the results. This feature needs firmware with the OnDemand module.

## Sniffer

The **Sniffer** screen (Settings → Advanced) shows network traffic live, including packets the phone cannot decrypt. It remembers frames after the radio is turned off, groups duplicates, has auto-scroll and decryption attempt, and saves and loads the log.

## Messaging

- **Pictures by link** — the app does not send a picture over LoRa, it sends a link instead; the receiver sees a preview. Previews of images from links are off by default and are turned on in Settings → Privacy.
- **Long messages** — text that does not fit in one packet is split into parts and put together on the other side.
- **Quick chat** — it comes with a ready set of templates on first launch.
- **Desktop** — Enter is a new line, Ctrl+Enter sends the message.

## Settings and appearance

- **App scale** — in the app settings you choose the size of text and elements from 50% to 200%, independent of the phone's settings. The default is "Follow the phone". It helps when the phone has an enlarged font or display size and screens look too big. It works on the desktop too.
- **LNA correction** — in Settings → LoRa you can enter the LNA gain, and the app corrects the displayed noise and RSSI. The raw data stays unchanged.
- **Inverted current and battery** — for a power sensor connected the other way round.
- **Device roles** — role names and descriptions are translated.
- **Reporting bugs and ideas** — the "Report a bug or idea" item in settings leads to our Discord and groups.

## Help and documentation

All help and documentation in the app is available in Polish and in English. You change the text size on the help pages with the "A−" and "A+" buttons at the top. See [Help and documentation](help-and-docs).

## Related Topics

- [Nodes](nodes) — the node list and node details
- [Map and waypoints](map-and-waypoints) — the map, layers and waypoints
- [Node metrics](node-metrics) — the telemetry charts that Network health is built on
- [Help and documentation](help-and-docs) — how to use the in-app help
