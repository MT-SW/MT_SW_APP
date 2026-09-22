import io

CRLF = "\r\n"
path = "SnifferPanelViewModel.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0


def replace_once(content, old, new, label):
    n = content.count(old)
    assert n == 1, f"{label}: expected exactly 1 match, found {n}"
    return content.replace(old, new)


# 1. Add groupedRadioPackets to SnifferDisplayState.
old_state = (
    "    val displayedMqttPackets: List<MqttSniffedPacket>," + CRLF +
    "    val displayedRadioPackets: List<SniffedPacket>," + CRLF +
    "    val groupedMqttPackets: List<GroupedMqttSniffedPacket>," + CRLF +
    "    val itemCount: Int," + CRLF
)
new_state = (
    "    val displayedMqttPackets: List<MqttSniffedPacket>," + CRLF +
    "    val displayedRadioPackets: List<SniffedPacket>," + CRLF +
    "    val groupedMqttPackets: List<GroupedMqttSniffedPacket>," + CRLF +
    "    val groupedRadioPackets: List<GroupedSniffedPacket>," + CRLF +
    "    val itemCount: Int," + CRLF
)
content = replace_once(content, old_state, new_state, "SnifferDisplayState.groupedRadioPackets")

# 2. Add displaySource/lastRealSource declarations after activeSource.
old_active_source = (
    "    val activeSource: StateFlow<SnifferSource> = prefs.activeSource.stateInWhileSubscribed(SnifferSource.OFF)" + CRLF +
    "    val groupByGateway: StateFlow<Boolean> = prefs.groupByGateway.stateInWhileSubscribed(true)" + CRLF
)
new_active_source = (
    "    val activeSource: StateFlow<SnifferSource> = prefs.activeSource.stateInWhileSubscribed(SnifferSource.OFF)" + CRLF +
    CRLF +
    "    private val _displaySource = MutableStateFlow(SnifferSource.OFF)" + CRLF +
    "    private var lastRealSource: SnifferSource? = null" + CRLF +
    CRLF +
    "    /**" + CRLF +
    "     * Which source's packets the panel currently shows. Tracks [activeSource] while it's RADIO/MQTT, but freezes" + CRLF +
    "     * at whatever it last was while [activeSource] is OFF -- turning the sniffer off must not blank the log, only" + CRLF +
    "     * the trash icon ([clearDisplayedLogs]) does. Switching to a genuinely different source than what's frozen" + CRLF +
    "     * here clears the panel and starts fresh instead -- see the `activeSource.onEach` in [init]." + CRLF +
    "     */" + CRLF +
    "    val displaySource: StateFlow<SnifferSource> = _displaySource.asStateFlow()" + CRLF +
    CRLF +
    "    val groupByGateway: StateFlow<Boolean> = prefs.groupByGateway.stateInWhileSubscribed(true)" + CRLF
)
content = replace_once(content, old_active_source, new_active_source, "displaySource/lastRealSource declarations")

# 3. Add the displaySource-tracking onEach at the end of init{}.
old_init_tail = (
    "        mqttConfigured" + CRLF +
    "            .onEach { configured ->" + CRLF +
    "                if (!configured && activeSource.value == SnifferSource.MQTT) selectSource(SnifferSource.OFF)" + CRLF +
    "            }" + CRLF +
    "            .launchIn(viewModelScope)" + CRLF +
    "    }" + CRLF
)
new_init_tail = (
    "        mqttConfigured" + CRLF +
    "            .onEach { configured ->" + CRLF +
    "                if (!configured && activeSource.value == SnifferSource.MQTT) selectSource(SnifferSource.OFF)" + CRLF +
    "            }" + CRLF +
    "            .launchIn(viewModelScope)" + CRLF +
    CRLF +
    "        // lastRealSource starts null so the very first value seen (e.g. a source restored from prefs on a fresh" + CRLF +
    "        // launch) only seeds displaySource, without wiping out that source's existing MeshLog history. From then" + CRLF +
    "        // on, a genuine switch between two real sources clears the panel; passing through Off does not." + CRLF +
    "        activeSource" + CRLF +
    "            .onEach { source ->" + CRLF +
    "                if (source != SnifferSource.OFF) {" + CRLF +
    "                    if (lastRealSource != null && lastRealSource != source) clearDisplayedLogs()" + CRLF +
    "                    lastRealSource = source" + CRLF +
    "                    _displaySource.value = source" + CRLF +
    "                }" + CRLF +
    "            }" + CRLF +
    "            .launchIn(viewModelScope)" + CRLF +
    "    }" + CRLF
)
content = replace_once(content, old_init_tail, new_init_tail, "init{} displaySource tracking")

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("SnifferPanelViewModel.kt fixed, new length:", len(content))
