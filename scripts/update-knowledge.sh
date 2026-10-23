#!/bin/sh
# Refreshes the assistant's knowledge about the other MT_SW repositories (firmware, integration, protobufs).
# Called by the build before the knowledge is packaged. Without internet it leaves the saved copies untouched.
cd "$(dirname "$0")/../knowledge" || exit 0
get() { # repo path file
  tmp="$(mktemp)" || return 0
  if curl -fsS -m 20 "https://raw.githubusercontent.com/MT-SW/$1/HEAD/$2" -o "$tmp" 2>/dev/null && [ -s "$tmp" ]; then
    cat "$tmp" > "$3"
  fi
  rm -f "$tmp"
}
get MT_SW_FIRMWARE README.md firmware.md
get MT_SW_INTEGRATION README.md integration.md
get MT_SW_INTEGRATION custom_components/meshtastic/meshtastic_web/MTSW_WEB_CLIENT.md integration-web-client.md
get MT_SW_INTEGRATION custom_components/meshtastic/ha_frontend/planner/API.md integration-planner-api.md
get MT_SW_PROTOBUFS README.md protobufs.md
exit 0
