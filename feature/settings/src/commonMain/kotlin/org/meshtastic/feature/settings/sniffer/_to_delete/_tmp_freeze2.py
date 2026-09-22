import io

CRLF = "\r\n"


def replace_once(content, old, new, label):
    n = content.count(old)
    assert n == 1, f"{label}: expected exactly 1 match, found {n}"
    return content.replace(old, new)


# ============================== SnifferPanelViewModel.kt ==============================
path_vm = "SnifferPanelViewModel.kt"
with io.open(path_vm, "r", encoding="utf-8", newline="") as f:
    vm = f.read()
assert vm.count(CRLF) > 0

old_doc = (
    "    /**" + CRLF +
    "     * Which source's packets the panel currently shows. Tracks [activeSource] while it's RADIO/MQTT, but freezes at" + CRLF +
    "     * whatever it last was while [activeSource] is OFF -- turning the sniffer off must not blank the log, only the" + CRLF +
    "     * trash icon ([clearDisplayedLogs]) does. Switching to a genuinely different source than what's frozen here clears" + CRLF +
    "     * the panel and starts fresh instead -- see the `activeSource.onEach` in [init]." + CRLF +
    "     */" + CRLF +
    "    val displaySource: StateFlow<SnifferSource> = _displaySource.asStateFlow()" + CRLF +
    CRLF +
    "    val groupByGateway: StateFlow<Boolean> = prefs.groupByGateway.stateInWhileSubscribed(true)" + CRLF
)
new_doc = (
    "    /**" + CRLF +
    "     * Which source's packets the panel currently shows. Tracks [activeSource] while it's RADIO/MQTT, but freezes at" + CRLF +
    "     * whatever it last was while [activeSource] is OFF -- turning the sniffer off must not blank the log, only the" + CRLF +
    "     * trash icon ([clearDisplayedLogs]) does. Switching to a genuinely different source than what's frozen here clears" + CRLF +
    "     * the panel and starts fresh instead -- see the `activeSource.onEach` in [init]." + CRLF +
    "     */" + CRLF +
    "    val displaySource: StateFlow<SnifferSource> = _displaySource.asStateFlow()" + CRLF +
    CRLF +
    "    private val _freezeAtMillis = MutableStateFlow(Long.MAX_VALUE)" + CRLF +
    CRLF +
    "    /**" + CRLF +
    "     * Upper bound for what's shown from the live packet stream -- [Long.MAX_VALUE] (no bound) while a real source is" + CRLF +
    "     * selected, or the exact moment [activeSource] became OFF. Without this, turning the sniffer off wouldn't stop" + CRLF +
    "     * the displayed log from silently growing: the underlying MeshLog stream keeps logging ordinary (non-sniffed)" + CRLF +
    "     * traffic regardless of the toggle, and a not-yet-fully-torn-down MQTT session can still deliver a packet or two" + CRLF +
    "     * in flight. Reset back to unbounded once a real source is reselected. See [clearedAtMillis] for the lower bound." + CRLF +
    "     */" + CRLF +
    "    val freezeAtMillis: StateFlow<Long> = _freezeAtMillis.asStateFlow()" + CRLF +
    CRLF +
    "    val groupByGateway: StateFlow<Boolean> = prefs.groupByGateway.stateInWhileSubscribed(true)" + CRLF
)
vm = replace_once(vm, old_doc, new_doc, "freezeAtMillis declaration")

old_oneach = (
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
new_oneach = (
    "        activeSource" + CRLF +
    "            .onEach { source ->" + CRLF +
    "                if (source != SnifferSource.OFF) {" + CRLF +
    "                    if (lastRealSource != null && lastRealSource != source) clearDisplayedLogs()" + CRLF +
    "                    lastRealSource = source" + CRLF +
    "                    _displaySource.value = source" + CRLF +
    "                    _freezeAtMillis.value = Long.MAX_VALUE" + CRLF +
    "                } else {" + CRLF +
    "                    _freezeAtMillis.value = Clock.System.now().toEpochMilliseconds()" + CRLF +
    "                }" + CRLF +
    "            }" + CRLF +
    "            .launchIn(viewModelScope)" + CRLF +
    "    }" + CRLF
)
vm = replace_once(vm, old_oneach, new_oneach, "activeSource onEach freeze logic")

with io.open(path_vm, "w", encoding="utf-8", newline="") as f:
    f.write(vm)
print("SnifferPanelViewModel.kt fixed, new length:", len(vm))


# ============================== SnifferSettingsScreen.kt ==============================
path_screen = "SnifferSettingsScreen.kt"
with io.open(path_screen, "r", encoding="utf-8", newline="") as f:
    screen = f.read()
assert screen.count(CRLF) > 0

old_collect = (
    "    val activeSource by panelViewModel.activeSource.collectAsStateWithLifecycle()" + CRLF +
    "    val displaySource by panelViewModel.displaySource.collectAsStateWithLifecycle()" + CRLF +
    "    val groupByGateway by panelViewModel.groupByGateway.collectAsStateWithLifecycle()" + CRLF
)
new_collect = (
    "    val activeSource by panelViewModel.activeSource.collectAsStateWithLifecycle()" + CRLF +
    "    val displaySource by panelViewModel.displaySource.collectAsStateWithLifecycle()" + CRLF +
    "    val freezeAtMillis by panelViewModel.freezeAtMillis.collectAsStateWithLifecycle()" + CRLF +
    "    val groupByGateway by panelViewModel.groupByGateway.collectAsStateWithLifecycle()" + CRLF
)
screen = replace_once(screen, old_collect, new_collect, "collect freezeAtMillis")

old_call = (
    "    val display =" + CRLF +
    "        rememberSnifferDisplayState(" + CRLF +
    "            loadedLog = loadedLog," + CRLF +
    "            mqttPackets = mqttPackets," + CRLF +
    "            radioPackets = radioPackets," + CRLF +
    "            clearedAtMillis = clearedAtMillis," + CRLF +
    "            groupByGateway = groupByGateway," + CRLF +
    "            displaySource = displaySource," + CRLF +
    "        )" + CRLF
)
new_call = (
    "    val display =" + CRLF +
    "        rememberSnifferDisplayState(" + CRLF +
    "            loadedLog = loadedLog," + CRLF +
    "            mqttPackets = mqttPackets," + CRLF +
    "            radioPackets = radioPackets," + CRLF +
    "            clearedAtMillis = clearedAtMillis," + CRLF +
    "            freezeAtMillis = freezeAtMillis," + CRLF +
    "            groupByGateway = groupByGateway," + CRLF +
    "            displaySource = displaySource," + CRLF +
    "        )" + CRLF
)
screen = replace_once(screen, old_call, new_call, "rememberSnifferDisplayState call site")

old_fn_sig = (
    "private fun rememberSnifferDisplayState(" + CRLF +
    "    loadedLog: LoadedSnifferLog?," + CRLF +
    "    mqttPackets: List<MqttSniffedPacket>," + CRLF +
    "    radioPackets: List<SniffedPacket>," + CRLF +
    "    clearedAtMillis: Long," + CRLF +
    "    groupByGateway: Boolean," + CRLF +
    "    displaySource: SnifferSource," + CRLF +
    "): SnifferDisplayState {" + CRLF
)
new_fn_sig = (
    "private fun rememberSnifferDisplayState(" + CRLF +
    "    loadedLog: LoadedSnifferLog?," + CRLF +
    "    mqttPackets: List<MqttSniffedPacket>," + CRLF +
    "    radioPackets: List<SniffedPacket>," + CRLF +
    "    clearedAtMillis: Long," + CRLF +
    "    freezeAtMillis: Long," + CRLF +
    "    groupByGateway: Boolean," + CRLF +
    "    displaySource: SnifferSource," + CRLF +
    "): SnifferDisplayState {" + CRLF
)
screen = replace_once(screen, old_fn_sig, new_fn_sig, "rememberSnifferDisplayState signature")

old_remembers = (
    "    val displayedMqttPackets =" + CRLF +
    "        remember(loadedLog, mqttPackets, clearedAtMillis) {" + CRLF +
    "            computeDisplayedMqttPackets(loadedLog, mqttPackets, clearedAtMillis)" + CRLF +
    "        }" + CRLF +
    "    val displayedRadioPackets =" + CRLF +
    "        remember(loadedLog, radioPackets, clearedAtMillis) {" + CRLF +
    "            computeDisplayedRadioPackets(loadedLog, radioPackets, clearedAtMillis)" + CRLF +
    "        }" + CRLF
)
new_remembers = (
    "    val displayedMqttPackets =" + CRLF +
    "        remember(loadedLog, mqttPackets, clearedAtMillis, freezeAtMillis) {" + CRLF +
    "            computeDisplayedMqttPackets(loadedLog, mqttPackets, clearedAtMillis, freezeAtMillis)" + CRLF +
    "        }" + CRLF +
    "    val displayedRadioPackets =" + CRLF +
    "        remember(loadedLog, radioPackets, clearedAtMillis, freezeAtMillis) {" + CRLF +
    "            computeDisplayedRadioPackets(loadedLog, radioPackets, clearedAtMillis, freezeAtMillis)" + CRLF +
    "        }" + CRLF
)
screen = replace_once(screen, old_remembers, new_remembers, "remember() calls with freezeAtMillis")

old_helpers = (
    "/**" + CRLF +
    " * The packets currently backing the MQTT side of the display -- a loaded file's rows, or the live stream since" + CRLF +
    " * [clearedAtMillis]." + CRLF +
    " */" + CRLF +
    "private fun computeDisplayedMqttPackets(" + CRLF +
    "    loadedLog: LoadedSnifferLog?," + CRLF +
    "    mqttPackets: List<MqttSniffedPacket>," + CRLF +
    "    clearedAtMillis: Long," + CRLF +
    "): List<MqttSniffedPacket> = if (loadedLog != null) {" + CRLF +
    "    loadedLog.rows.flatMap { it.toMqttSniffedPackets() }.sortedByDescending { it.receivedAtMillis }" + CRLF +
    "} else {" + CRLF +
    "    mqttPackets.filter { it.receivedAtMillis > clearedAtMillis }" + CRLF +
    "}" + CRLF +
    CRLF +
    "/** The Radio equivalent of [computeDisplayedMqttPackets]. */" + CRLF +
    "private fun computeDisplayedRadioPackets(" + CRLF +
    "    loadedLog: LoadedSnifferLog?," + CRLF +
    "    radioPackets: List<SniffedPacket>," + CRLF +
    "    clearedAtMillis: Long," + CRLF +
    "): List<SniffedPacket> = if (loadedLog != null) {" + CRLF +
    "    loadedLog.rows.mapNotNull { it.toSniffedPacket() }.sortedByDescending { it.receivedAtMillis }" + CRLF +
    "} else {" + CRLF +
    "    radioPackets.filter { it.receivedAtMillis > clearedAtMillis }" + CRLF +
    "}" + CRLF
)
new_helpers = (
    "/**" + CRLF +
    " * The packets currently backing the MQTT side of the display -- a loaded file's rows, or the live stream bounded" + CRLF +
    " * to ([clearedAtMillis], [freezeAtMillis]] (see [SnifferPanelViewModel.freezeAtMillis] for why an upper bound is" + CRLF +
    " * needed too, not just the trash icon's lower one)." + CRLF +
    " */" + CRLF +
    "private fun computeDisplayedMqttPackets(" + CRLF +
    "    loadedLog: LoadedSnifferLog?," + CRLF +
    "    mqttPackets: List<MqttSniffedPacket>," + CRLF +
    "    clearedAtMillis: Long," + CRLF +
    "    freezeAtMillis: Long," + CRLF +
    "): List<MqttSniffedPacket> = if (loadedLog != null) {" + CRLF +
    "    loadedLog.rows.flatMap { it.toMqttSniffedPackets() }.sortedByDescending { it.receivedAtMillis }" + CRLF +
    "} else {" + CRLF +
    "    mqttPackets.filter { it.receivedAtMillis > clearedAtMillis && it.receivedAtMillis <= freezeAtMillis }" + CRLF +
    "}" + CRLF +
    CRLF +
    "/** The Radio equivalent of [computeDisplayedMqttPackets]. */" + CRLF +
    "private fun computeDisplayedRadioPackets(" + CRLF +
    "    loadedLog: LoadedSnifferLog?," + CRLF +
    "    radioPackets: List<SniffedPacket>," + CRLF +
    "    clearedAtMillis: Long," + CRLF +
    "    freezeAtMillis: Long," + CRLF +
    "): List<SniffedPacket> = if (loadedLog != null) {" + CRLF +
    "    loadedLog.rows.mapNotNull { it.toSniffedPacket() }.sortedByDescending { it.receivedAtMillis }" + CRLF +
    "} else {" + CRLF +
    "    radioPackets.filter { it.receivedAtMillis > clearedAtMillis && it.receivedAtMillis <= freezeAtMillis }" + CRLF +
    "}" + CRLF
)
screen = replace_once(screen, old_helpers, new_helpers, "compute*Packets helpers with freezeAtMillis")

with io.open(path_screen, "w", encoding="utf-8", newline="") as f:
    f.write(screen)
print("SnifferSettingsScreen.kt fixed, new length:", len(screen))
