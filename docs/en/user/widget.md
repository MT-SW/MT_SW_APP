---
title: Home Screen Widget
parent: User Guide
nav_order: 20
last_updated: 2026-11-16
description: Add the MT_SW statistics and SOS widgets to your Android home screen to see your node and mesh at a glance, or send the alarm with a few quick taps.
aliases:
  - widget
  - home-screen-widget
  - mesh-status-widget
  - sos-widget
---

# Home Screen Widget

On Android, MT_SW provides two home screen **widgets**:

- **MT_SW statistics** shows the same statistics as the permanent notification: the state of the whole mesh, or one tracked node.
- **SOS** sends the alarm after a few quick taps in a row.

The statistics widget shows readings the app has already saved, so it works with no radio connected, and it never sends anything over the mesh.

## MT_SW statistics

The widget shows the nodes online, unread messages, battery, uptime, channel load, traffic and the rest of what your radio reports. What it shows (the whole mesh or one chosen node) is set in the adaptive display on the **Connections** screen. When it follows the whole mesh, it switches between the nodes every minute.

## SOS

The alarm is sent by several quick taps on the widget in a row (at most 2.5 s between taps). A hold does not work, because the launcher then starts editing the widget. Set the number of taps in the **SOS** screen, section "Home-screen widget": from 3 (default 3), so a pocket cannot send the alarm by accident. While counting, the widget shows "Tap N more", and "Sent" after the alarm went out; a tap then opens the SOS screen with the answers. See [SOS](mt-sw-features) for the message, channels and nodes. On iPhone/iPad the last tap opens the app, which sends the alarm.

## Adding a Widget

1. Touch & hold an empty area of your Android home screen.
2. Tap **Widgets**, then find **MT_SW** in the list.
3. Drag **MT_SW statistics** (4x2) or **SOS** to your home screen.
4. Resize it as needed — the layout adapts to the available space.

> ℹ️ **Note:** The widgets are Android-only. On iPhone and iPad the statistics are a widget added from the home screen's **+** button, and on the desktop they are in the tray icon and the main window.

## Related Topics

- [Nodes](nodes) — favorites, last heard, and node details
- [Connections](connections) — connect to a node so the widget has data to show
- [Local Mesh Discovery](discovery) — channel and airtime utilization across the mesh
