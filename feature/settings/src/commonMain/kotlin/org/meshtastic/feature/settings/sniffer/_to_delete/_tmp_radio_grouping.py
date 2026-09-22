import io

CRLF = "\r\n"
path = "SnifferLogViewModel.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0


def replace_once(content, old, new, label):
    n = content.count(old)
    assert n == 1, f"{label}: expected exactly 1 match, found {n}"
    return content.replace(old, new)


# 1. import Node (alphabetically between MeshLog and NodeAddress)
content = replace_once(
    content,
    "import org.meshtastic.core.model.MeshLog" + CRLF +
    "import org.meshtastic.core.model.NodeAddress" + CRLF,
    "import org.meshtastic.core.model.MeshLog" + CRLF +
    "import org.meshtastic.core.model.Node" + CRLF +
    "import org.meshtastic.core.model.NodeAddress" + CRLF,
    "Node import",
)

# 2. Add relayId field to SniffedPacket + doc comment for packetId (add relayId right after packetId).
old_packet_id_field = (
    "    /**" + CRLF +
    "     * The originating MeshPacket's own id -- lets a saved-and-reloaded log retry decryption the same way the MQTT" + CRLF +
    "     * Sniffer's [org.meshtastic.feature.settings.sniffer.mqtt.MqttSniffedPacket.packetId] does." + CRLF +
    "     */" + CRLF +
    "    val packetId: Int? = null," + CRLF +
    ") {" + CRLF
)
new_packet_id_field = (
    "    /**" + CRLF +
    "     * The originating MeshPacket's own id -- lets a saved-and-reloaded log retry decryption the same way the MQTT" + CRLF +
    "     * Sniffer's [org.meshtastic.feature.settings.sniffer.mqtt.MqttSniffedPacket.packetId] does. Also lets several" + CRLF +
    "     * relayed copies of the same over-the-air transmission be spotted and grouped -- see [groupedByRelay]." + CRLF +
    "     */" + CRLF +
    "    val packetId: Int? = null," + CRLF +
    "    /**" + CRLF +
    "     * The single relay node (formatted \"!hex\" or \"!hex (SHORTNAME)\") this particular copy of the packet was last" + CRLF +
    "     * heard from, resolved from the wire's truncated [org.meshtastic.proto.MeshPacket.relay_node] byte via" + CRLF +
    "     * [org.meshtastic.core.model.Node.getRelayNode]. Null when unset (a direct/0-hop packet) or unresolvable against" + CRLF +
    "     * the current node database. Grouping several same-[packetId] entries into one row is done downstream (see" + CRLF +
    "     * [groupedByRelay]), not here -- mirrors MQTT's own gatewayId field." + CRLF +
    "     */" + CRLF +
    "    val relayId: String? = null," + CRLF +
    ") {" + CRLF
)
content = replace_once(content, old_packet_id_field, new_packet_id_field, "SniffedPacket packetId/relayId fields")

# 3. copyText: add relayId line after the hops/signal line.
old_copy_text = (
    "                    appendLine(\"$fromId → $toId\")" + CRLF +
    "                    appendLine(\"ch $channel • hops ${hopStart - hopLimit}/$hopStart\")" + CRLF +
    "                    appendLine(listOfNotNull(rssi?.let { \"RSSI $it\" }, \"SNR $snr\").joinToString(\" • \"))" + CRLF +
    "                    appendLine(if (isEncrypted) \"encrypted\" else portNum?.let { \"port $it\" } ?: \"unknown port\")" + CRLF
)
new_copy_text = (
    "                    appendLine(\"$fromId → $toId\")" + CRLF +
    "                    appendLine(\"ch $channel • hops ${hopStart - hopLimit}/$hopStart\")" + CRLF +
    "                    appendLine(listOfNotNull(rssi?.let { \"RSSI $it\" }, \"SNR $snr\").joinToString(\" • \"))" + CRLF +
    "                    relayId?.let { appendLine(\"via $it\") }" + CRLF +
    "                    appendLine(if (isEncrypted) \"encrypted\" else portNum?.let { \"port $it\" } ?: \"unknown port\")" + CRLF
)
content = replace_once(content, old_copy_text, new_copy_text, "copyText relayId line")

# 4. Add GroupedSniffedPacket + groupedByRelay() right after the SniffedPacket class, before the ViewModel doc comment.
old_anchor = (
    "/**" + CRLF +
    " * Sniffer Log screen ViewModel, reached from Settings → Advanced. Sniffer mode has no distinguishing marker on the wire" + CRLF
)
new_anchor = (
    "/**" + CRLF +
    " * One row in the combined Sniffer panel's grouped Radio view (see [groupedByRelay]): one over-the-air packet" + CRLF +
    " * ([SniffedPacket.packetId]), possibly re-heard through several relaying nodes. [relayIds] lists every relay this" + CRLF +
    " * packet was seen from, most-recently-seen first, deduplicated. [packet] is the most recently received copy -- its" + CRLF +
    " * own [SniffedPacket.relayId] is superseded by [relayIds] for display. Mirrors" + CRLF +
    " * [org.meshtastic.feature.settings.sniffer.mqtt.GroupedMqttSniffedPacket]." + CRLF +
    " */" + CRLF +
    "data class GroupedSniffedPacket(val packet: SniffedPacket, val relayIds: List<String>)" + CRLF +
    CRLF +
    "/**" + CRLF +
    " * Groups packets sharing the same [SniffedPacket.packetId] -- i.e. the same over-the-air transmission re-heard" + CRLF +
    " * through several relaying nodes -- into a single row per packet, newest first. Entries with no packetId are never" + CRLF +
    " * merged and each get their own single-relay row. Pure and stateless, mirroring" + CRLF +
    " * [org.meshtastic.feature.settings.sniffer.mqtt.groupedByGateway]: called from the display layer only when the user" + CRLF +
    " * has grouping enabled -- the underlying live packet list itself always stays ungrouped." + CRLF +
    " */" + CRLF +
    "fun List<SniffedPacket>.groupedByRelay(): List<GroupedSniffedPacket> {" + CRLF +
    "    val (groupable, ungroupable) = partition { it.packetId != null }" + CRLF +
    "    val grouped =" + CRLF +
    "        groupable" + CRLF +
    "            .groupBy { it.packetId }" + CRLF +
    "            .values" + CRLF +
    "            .map { packets ->" + CRLF +
    "                val newest = packets.maxBy { it.receivedAtMillis }" + CRLF +
    "                val relayIds = packets.mapNotNull { it.relayId }.distinct()" + CRLF +
    "                GroupedSniffedPacket(newest, relayIds)" + CRLF +
    "            }" + CRLF +
    "    val singles = ungroupable.map { GroupedSniffedPacket(it, listOfNotNull(it.relayId)) }" + CRLF +
    "    return (grouped + singles).sortedByDescending { it.packet.receivedAtMillis }" + CRLF +
    "}" + CRLF +
    CRLF +
    "/**" + CRLF +
    " * Sniffer Log screen ViewModel, reached from Settings → Advanced. Sniffer mode has no distinguishing marker on the wire" + CRLF
)
content = replace_once(content, old_anchor, new_anchor, "GroupedSniffedPacket + groupedByRelay insertion")

# 5. toSniffedPacket: capture myNodeNum, pass relayId into the constructed SniffedPacket, add resolveRelayId helper.
old_to_sniffed = (
    "    private fun toSniffedPacket(log: MeshLog): SniffedPacket? {" + CRLF +
    "        val packet = log.meshPacket ?: return null" + CRLF +
    "        nodeRepository.myNodeInfo.value?.myNodeNum ?: return null" + CRLF +
    CRLF +
    "        val nodeMap = nodeRepository.nodeDBbyNum.value" + CRLF +
    "        val decodedText = decodePayloadFromPacket(packet, nodeRepository, knownChannelPsks())" + CRLF +
    "        val rawBytes = packet.decoded?.payload ?: packet.encrypted" + CRLF +
    "        return SniffedPacket(" + CRLF +
    "            fromId = NodeAddress.numToDefaultId(packet.from)," + CRLF +
    "            toId = NodeAddress.numToDefaultId(packet.to)," + CRLF +
    "            fromShortName = nodeMap[packet.from]?.user?.short_name?.takeIf { it.isNotBlank() }," + CRLF +
    "            toShortName = nodeMap[packet.to]?.user?.short_name?.takeIf { it.isNotBlank() }," + CRLF +
    "            channel = packet.channel," + CRLF +
    "            hopStart = packet.hop_start," + CRLF +
    "            hopLimit = packet.hop_limit," + CRLF +
    "            rssi = packet.rx_rssi," + CRLF +
    "            snr = packet.rx_snr," + CRLF +
    "            portNum = packet.decoded?.portnum?.value," + CRLF +
    "            isEncrypted = decodedText == null," + CRLF +
    "            payloadHex = rawBytes?.hex().orEmpty()," + CRLF +
    "            decodedPayload = decodedText," + CRLF +
    "            receivedAtMillis = log.received_date," + CRLF +
    "            packetId = packet.id," + CRLF +
    "        )" + CRLF +
    "    }" + CRLF +
    "}" + CRLF
)
new_to_sniffed = (
    "    private fun toSniffedPacket(log: MeshLog): SniffedPacket? {" + CRLF +
    "        val packet = log.meshPacket ?: return null" + CRLF +
    "        val myNodeNum = nodeRepository.myNodeInfo.value?.myNodeNum ?: return null" + CRLF +
    CRLF +
    "        val nodeMap = nodeRepository.nodeDBbyNum.value" + CRLF +
    "        val decodedText = decodePayloadFromPacket(packet, nodeRepository, knownChannelPsks())" + CRLF +
    "        val rawBytes = packet.decoded?.payload ?: packet.encrypted" + CRLF +
    "        return SniffedPacket(" + CRLF +
    "            fromId = NodeAddress.numToDefaultId(packet.from)," + CRLF +
    "            toId = NodeAddress.numToDefaultId(packet.to)," + CRLF +
    "            fromShortName = nodeMap[packet.from]?.user?.short_name?.takeIf { it.isNotBlank() }," + CRLF +
    "            toShortName = nodeMap[packet.to]?.user?.short_name?.takeIf { it.isNotBlank() }," + CRLF +
    "            channel = packet.channel," + CRLF +
    "            hopStart = packet.hop_start," + CRLF +
    "            hopLimit = packet.hop_limit," + CRLF +
    "            rssi = packet.rx_rssi," + CRLF +
    "            snr = packet.rx_snr," + CRLF +
    "            portNum = packet.decoded?.portnum?.value," + CRLF +
    "            isEncrypted = decodedText == null," + CRLF +
    "            payloadHex = rawBytes?.hex().orEmpty()," + CRLF +
    "            decodedPayload = decodedText," + CRLF +
    "            receivedAtMillis = log.received_date," + CRLF +
    "            packetId = packet.id," + CRLF +
    "            relayId = resolveRelayId(packet.relay_node, nodeMap, myNodeNum)," + CRLF +
    "        )" + CRLF +
    "    }" + CRLF +
    CRLF +
    "    /**" + CRLF +
    "     * Resolves [relayNode] (the wire's truncated last-byte relay id) against [nodeMap] via [Node.getRelayNode]." + CRLF +
    "     * Returns null for an unset (0) or unresolvable relay rather than showing a misleading partial id." + CRLF +
    "     */" + CRLF +
    "    private fun resolveRelayId(relayNode: Int, nodeMap: Map<Int, Node>, ourNodeNum: Int?): String? {" + CRLF +
    "        if (relayNode == 0) return null" + CRLF +
    "        return Node.getRelayNode(relayNode, nodeMap.values.toList(), ourNodeNum)?.let { relay ->" + CRLF +
    "            val shortName = relay.user?.short_name?.takeIf { it.isNotBlank() }" + CRLF +
    "            val id = NodeAddress.numToDefaultId(relay.num)" + CRLF +
    "            shortName?.let { \"$id ($it)\" } ?: id" + CRLF +
    "        }" + CRLF +
    "    }" + CRLF +
    "}" + CRLF
)
content = replace_once(content, old_to_sniffed, new_to_sniffed, "toSniffedPacket + resolveRelayId")

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("SnifferLogViewModel.kt fully fixed, new length:", len(content))
