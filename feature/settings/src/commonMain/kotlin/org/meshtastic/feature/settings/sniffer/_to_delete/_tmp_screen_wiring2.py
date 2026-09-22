import io

CRLF = "\r\n"
path = "SnifferSettingsScreen.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0


def replace_once(content, old, new, label):
    n = content.count(old)
    assert n == 1, f"{label}: expected exactly 1 match, found {n}"
    return content.replace(old, new)


# 1. Collect displaySource alongside activeSource.
old_collect = (
    "    val activeSource by panelViewModel.activeSource.collectAsStateWithLifecycle()" + CRLF +
    "    val groupByGateway by panelViewModel.groupByGateway.collectAsStateWithLifecycle()" + CRLF
)
new_collect = (
    "    val activeSource by panelViewModel.activeSource.collectAsStateWithLifecycle()" + CRLF +
    "    val displaySource by panelViewModel.displaySource.collectAsStateWithLifecycle()" + CRLF +
    "    val groupByGateway by panelViewModel.groupByGateway.collectAsStateWithLifecycle()" + CRLF
)
content = replace_once(content, old_collect, new_collect, "collect displaySource")

# 2. Pass displaySource into rememberSnifferDisplayState instead of activeSource.
old_call = (
    "    val display =" + CRLF +
    "        rememberSnifferDisplayState(" + CRLF +
    "            loadedLog = loadedLog," + CRLF +
    "            mqttPackets = mqttPackets," + CRLF +
    "            radioPackets = radioPackets," + CRLF +
    "            clearedAtMillis = clearedAtMillis," + CRLF +
    "            groupByGateway = groupByGateway," + CRLF +
    "            activeSource = activeSource," + CRLF +
    "        )" + CRLF
)
new_call = (
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
content = replace_once(content, old_call, new_call, "rememberSnifferDisplayState call site")

# 3. LazyColumn radio branch: grouped packets + renamed card.
old_lazy = (
    "                        } else if (display.showingRadio) {" + CRLF +
    "                            items(" + CRLF +
    "                                display.displayedRadioPackets," + CRLF +
    "                                key = { \"${it.receivedAtMillis}-${it.fromId}-${it.toId}\" }," + CRLF +
    "                            ) { packet ->" + CRLF +
    "                                var isExpanded by remember(packet) { mutableStateOf(false) }" + CRLF +
    "                                RadioPacketCard(" + CRLF +
    "                                    packet = packet," + CRLF +
    "                                    decryptPayloads = decryptPayloads," + CRLF +
    "                                    isExpanded = isExpanded," + CRLF +
    "                                    onClick = { isExpanded = !isExpanded }," + CRLF +
    "                                )" + CRLF +
    "                            }" + CRLF +
    "                        }" + CRLF
)
new_lazy = (
    "                        } else if (display.showingRadio) {" + CRLF +
    "                            items(" + CRLF +
    "                                display.groupedRadioPackets," + CRLF +
    "                                key = {" + CRLF +
    "                                    \"${it.packet.packetId}-${it.packet.receivedAtMillis}-\" +" + CRLF +
    "                                        \"${it.packet.fromId}-${it.packet.toId}\"" + CRLF +
    "                                }," + CRLF +
    "                            ) { grouped ->" + CRLF +
    "                                var isExpanded by remember(grouped) { mutableStateOf(false) }" + CRLF +
    "                                GroupedRadioPacketCard(" + CRLF +
    "                                    grouped = grouped," + CRLF +
    "                                    decryptPayloads = decryptPayloads," + CRLF +
    "                                    isExpanded = isExpanded," + CRLF +
    "                                    onClick = { isExpanded = !isExpanded }," + CRLF +
    "                                )" + CRLF +
    "                            }" + CRLF +
    "                        }" + CRLF
)
content = replace_once(content, old_lazy, new_lazy, "LazyColumn radio branch")

# 4. Replace rememberSnifferDisplayState entirely + add the two extracted helper functions.
old_fn = (
    "/**" + CRLF +
    " * Derives everything the panel needs to know about what to show -- pulled out of [SnifferSettingsScreen] itself so that" + CRLF +
    " * function's own branching stays under detekt's CyclomaticComplexMethod limit. A loaded file overrides the live stream;" + CRLF +
    " * otherwise the currently active source decides." + CRLF +
    " */" + CRLF +
    "@Composable" + CRLF +
    "private fun rememberSnifferDisplayState(" + CRLF +
    "    loadedLog: LoadedSnifferLog?," + CRLF +
    "    mqttPackets: List<MqttSniffedPacket>," + CRLF +
    "    radioPackets: List<SniffedPacket>," + CRLF +
    "    clearedAtMillis: Long," + CRLF +
    "    groupByGateway: Boolean," + CRLF +
    "    activeSource: SnifferSource," + CRLF +
    "): SnifferDisplayState {" + CRLF +
    "    val loadedIsMqtt = loadedLog?.rows?.firstOrNull()?.source == \"mqtt\"" + CRLF +
    "    val loadedIsRadio = loadedLog != null && !loadedIsMqtt" + CRLF +
    "    val showingMqtt = if (loadedLog != null) loadedIsMqtt else activeSource == SnifferSource.MQTT" + CRLF +
    "    val showingRadio = if (loadedLog != null) loadedIsRadio else activeSource == SnifferSource.RADIO" + CRLF +
    CRLF +
    "    val displayedMqttPackets =" + CRLF +
    "        remember(loadedLog, mqttPackets, clearedAtMillis) {" + CRLF +
    "            val source = loadedLog" + CRLF +
    "            if (source != null) {" + CRLF +
    "                source.rows.flatMap { it.toMqttSniffedPackets() }.sortedByDescending { it.receivedAtMillis }" + CRLF +
    "            } else {" + CRLF +
    "                mqttPackets.filter { it.receivedAtMillis > clearedAtMillis }" + CRLF +
    "            }" + CRLF +
    "        }" + CRLF +
    "    val displayedRadioPackets =" + CRLF +
    "        remember(loadedLog, radioPackets, clearedAtMillis) {" + CRLF +
    "            val source = loadedLog" + CRLF +
    "            if (source != null) {" + CRLF +
    "                source.rows.mapNotNull { it.toSniffedPacket() }.sortedByDescending { it.receivedAtMillis }" + CRLF +
    "            } else {" + CRLF +
    "                radioPackets.filter { it.receivedAtMillis > clearedAtMillis }" + CRLF +
    "            }" + CRLF +
    "        }" + CRLF +
    "    val groupedMqttPackets =" + CRLF +
    "        remember(displayedMqttPackets, groupByGateway) {" + CRLF +
    "            if (groupByGateway) {" + CRLF +
    "                displayedMqttPackets.groupedByGateway()" + CRLF +
    "            } else {" + CRLF +
    "                displayedMqttPackets.map { GroupedMqttSniffedPacket(it, listOfNotNull(it.gatewayId)) }" + CRLF +
    "            }" + CRLF +
    "        }" + CRLF +
    CRLF +
    "    val itemCount =" + CRLF +
    "        if (showingMqtt) {" + CRLF +
    "            groupedMqttPackets.size" + CRLF +
    "        } else if (showingRadio) {" + CRLF +
    "            displayedRadioPackets.size" + CRLF +
    "        } else {" + CRLF +
    "            0" + CRLF +
    "        }" + CRLF +
    CRLF +
    "    val newestKey =" + CRLF +
    "        when {" + CRLF +
    "            showingMqtt -> groupedMqttPackets.firstOrNull()?.let { \"${it.packet.receivedAtMillis}-${it.packet.topic}\" }" + CRLF +
    CRLF +
    "            showingRadio ->" + CRLF +
    "                displayedRadioPackets.firstOrNull()?.let { \"${it.receivedAtMillis}-${it.fromId}-${it.toId}\" }" + CRLF +
    CRLF +
    "            else -> null" + CRLF +
    "        }" + CRLF +
    CRLF +
    "    return SnifferDisplayState(" + CRLF +
    "        showingMqtt = showingMqtt," + CRLF +
    "        showingRadio = showingRadio," + CRLF +
    "        displayedMqttPackets = displayedMqttPackets," + CRLF +
    "        displayedRadioPackets = displayedRadioPackets," + CRLF +
    "        groupedMqttPackets = groupedMqttPackets," + CRLF +
    "        itemCount = itemCount," + CRLF +
    "        newestKey = newestKey," + CRLF +
    "    )" + CRLF +
    "}" + CRLF
)

new_fn = (
    "/**" + CRLF +
    " * Derives everything the panel needs to know about what to show -- pulled out of [SnifferSettingsScreen] itself so that" + CRLF +
    " * function's own branching stays under detekt's CyclomaticComplexMethod limit. A loaded file overrides the live stream;" + CRLF +
    " * otherwise [displaySource] decides -- see [SnifferPanelViewModel.displaySource] for why that isn't just activeSource." + CRLF +
    " */" + CRLF +
    "@Composable" + CRLF +
    "private fun rememberSnifferDisplayState(" + CRLF +
    "    loadedLog: LoadedSnifferLog?," + CRLF +
    "    mqttPackets: List<MqttSniffedPacket>," + CRLF +
    "    radioPackets: List<SniffedPacket>," + CRLF +
    "    clearedAtMillis: Long," + CRLF +
    "    groupByGateway: Boolean," + CRLF +
    "    displaySource: SnifferSource," + CRLF +
    "): SnifferDisplayState {" + CRLF +
    "    val loadedIsMqtt = loadedLog?.rows?.firstOrNull()?.source == \"mqtt\"" + CRLF +
    "    val loadedIsRadio = loadedLog != null && !loadedIsMqtt" + CRLF +
    "    val showingMqtt = if (loadedLog != null) loadedIsMqtt else displaySource == SnifferSource.MQTT" + CRLF +
    "    val showingRadio = if (loadedLog != null) loadedIsRadio else displaySource == SnifferSource.RADIO" + CRLF +
    CRLF +
    "    val displayedMqttPackets =" + CRLF +
    "        remember(loadedLog, mqttPackets, clearedAtMillis) {" + CRLF +
    "            computeDisplayedMqttPackets(loadedLog, mqttPackets, clearedAtMillis)" + CRLF +
    "        }" + CRLF +
    "    val displayedRadioPackets =" + CRLF +
    "        remember(loadedLog, radioPackets, clearedAtMillis) {" + CRLF +
    "            computeDisplayedRadioPackets(loadedLog, radioPackets, clearedAtMillis)" + CRLF +
    "        }" + CRLF +
    "    val groupedMqttPackets =" + CRLF +
    "        remember(displayedMqttPackets, groupByGateway) {" + CRLF +
    "            if (groupByGateway) {" + CRLF +
    "                displayedMqttPackets.groupedByGateway()" + CRLF +
    "            } else {" + CRLF +
    "                displayedMqttPackets.map { GroupedMqttSniffedPacket(it, listOfNotNull(it.gatewayId)) }" + CRLF +
    "            }" + CRLF +
    "        }" + CRLF +
    "    val groupedRadioPackets =" + CRLF +
    "        remember(displayedRadioPackets, groupByGateway) {" + CRLF +
    "            if (groupByGateway) {" + CRLF +
    "                displayedRadioPackets.groupedByRelay()" + CRLF +
    "            } else {" + CRLF +
    "                displayedRadioPackets.map { GroupedSniffedPacket(it, listOfNotNull(it.relayId)) }" + CRLF +
    "            }" + CRLF +
    "        }" + CRLF +
    CRLF +
    "    val itemCount =" + CRLF +
    "        if (showingMqtt) {" + CRLF +
    "            groupedMqttPackets.size" + CRLF +
    "        } else if (showingRadio) {" + CRLF +
    "            groupedRadioPackets.size" + CRLF +
    "        } else {" + CRLF +
    "            0" + CRLF +
    "        }" + CRLF +
    CRLF +
    "    val newestKey =" + CRLF +
    "        when {" + CRLF +
    "            showingMqtt -> groupedMqttPackets.firstOrNull()?.let { \"${it.packet.receivedAtMillis}-${it.packet.topic}\" }" + CRLF +
    CRLF +
    "            showingRadio ->" + CRLF +
    "                groupedRadioPackets.firstOrNull()?.let {" + CRLF +
    "                    \"${it.packet.receivedAtMillis}-${it.packet.fromId}-${it.packet.toId}\"" + CRLF +
    "                }" + CRLF +
    CRLF +
    "            else -> null" + CRLF +
    "        }" + CRLF +
    CRLF +
    "    return SnifferDisplayState(" + CRLF +
    "        showingMqtt = showingMqtt," + CRLF +
    "        showingRadio = showingRadio," + CRLF +
    "        displayedMqttPackets = displayedMqttPackets," + CRLF +
    "        displayedRadioPackets = displayedRadioPackets," + CRLF +
    "        groupedMqttPackets = groupedMqttPackets," + CRLF +
    "        groupedRadioPackets = groupedRadioPackets," + CRLF +
    "        itemCount = itemCount," + CRLF +
    "        newestKey = newestKey," + CRLF +
    "    )" + CRLF +
    "}" + CRLF +
    CRLF +
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
content = replace_once(content, old_fn, new_fn, "rememberSnifferDisplayState rewrite")

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("SnifferSettingsScreen.kt fixed, new length:", len(content))
