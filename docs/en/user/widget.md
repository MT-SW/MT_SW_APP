---
title: Home Screen Widget
parent: User Guide
nav_order: 20
last_updated: 2026-11-16
description: Add the MT_SW statistics and SOS widgets to your Android home screen to see your node and mesh at a glance, or reach the SOS screen in one tap.
aliases:
  - widget
  - home-screen-widget
  - mesh-status-widget
  - sos-widget
---

# Home Screen Widget

On Android, MT_SW provides two home screen **widgets**:

- **MT_SW statistics** shows the same statistics as the permanent notification: the state of the whole mesh, or one tracked node.
- **SOS** opens the SOS screen, where the alarm is sent by holding its button.

The statistics widget shows readings the app has already saved, so it works with no radio connected, and it never sends anything over the mesh.

## MT_SW statistics

The widget shows the nodes online, unread messages, battery, uptime, channel load, traffic and the rest of what your radio reports. What it shows (the whole mesh or one chosen node) is set in the adaptive display on the **Connections** screen. When it follows the whole mesh, it switches between the nodes every minute.

## SOS

Tapping the widget opens the SOS screen. The alarm is not sent by the widget itself: you still hold the button on the SOS screen for the set time, so it cannot be sent by accident. See [SOS](mt-sw-features) for the message, channels and nodes it uses.

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
