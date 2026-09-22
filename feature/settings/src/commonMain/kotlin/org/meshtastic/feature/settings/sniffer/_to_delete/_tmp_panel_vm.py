import io

path = "/sessions/rcw-01daoeywosxkkw3mqeiacsub/mnt/Meshtastic_Android/feature/settings/src/commonMain/kotlin/org/meshtastic/feature/settings/sniffer/SnifferPanelViewModel.kt"

with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()

CRLF = "\r\n"
assert content.count(CRLF) > 0

# --- 1. Add the two new imports (mqtt package types used by SnifferDisplayState) ---
old_imports = (
    "import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed" + CRLF +
    "import org.meshtastic.feature.settings.sniffer.mqtt.MqttSnifferManager" + CRLF +
    "import org.meshtastic.proto.Config.LoRaConfig" + CRLF
)
new_imports = (
    "import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed" + CRLF +
    "import org.meshtastic.feature.settings.sniffer.mqtt.GroupedMqttSniffedPacket" + CRLF +
    "import org.meshtastic.feature.settings.sniffer.mqtt.MqttSniffedPacket" + CRLF +
    "import org.meshtastic.feature.settings.sniffer.mqtt.MqttSnifferManager" + CRLF +
    "import org.meshtastic.proto.Config.LoRaConfig" + CRLF
)
assert content.count(old_imports) == 1, "imports anchor not found"
content = content.replace(old_imports, new_imports)

# --- 2. Insert SnifferDisplayState right after LoadedSnifferLog ---
old_anchor = (
    "/** A previously saved log the user asked to view -- see [SnifferPanelViewModel.loadLog]. Not persisted. */" + CRLF +
    "data class LoadedSnifferLog(val fileName: String, val rows: List<SnifferExportRow>)" + CRLF
)
new_block = (
    "/** A previously saved log the user asked to view -- see [SnifferPanelViewModel.loadLog]. Not persisted. */" + CRLF +
    "data class LoadedSnifferLog(val fileName: String, val rows: List<SnifferExportRow>)" + CRLF +
    CRLF +
    "/**" + CRLF +
    " * Everything [org.meshtastic.feature.settings.sniffer.SnifferSettingsScreen] needs to know about what to show --" + CRLF +
    " * computed by its own `rememberSnifferDisplayState` helper and shared with the top-bar actions, empty-state text" + CRLF +
    " * and packet list composables that live in SnifferPanelComponents.kt." + CRLF +
    " */" + CRLF +
    "data class SnifferDisplayState(" + CRLF +
    "    val showingMqtt: Boolean," + CRLF +
    "    val showingRadio: Boolean," + CRLF +
    "    val displayedMqttPackets: List<MqttSniffedPacket>," + CRLF +
    "    val displayedRadioPackets: List<SniffedPacket>," + CRLF +
    "    val groupedMqttPackets: List<GroupedMqttSniffedPacket>," + CRLF +
    "    val itemCount: Int," + CRLF +
    "    val newestKey: String?," + CRLF +
    ")" + CRLF
)
assert content.count(old_anchor) == 1, "LoadedSnifferLog anchor not found"
content = content.replace(old_anchor, new_block)

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)

print("OK, new length:", len(content))
