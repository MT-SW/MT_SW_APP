import io

CRLF = "\r\n"
path = "SnifferPanelComponents.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0


def replace_once(content, old, new, label):
    n = content.count(old)
    assert n == 1, f"{label}: expected exactly 1 match, found {n}"
    return content.replace(old, new)


# 1. Import the new string resource, right after sniffer_via_gateways.
old_import = "import org.meshtastic.core.resources.sniffer_via_gateways" + CRLF
new_import = (
    "import org.meshtastic.core.resources.sniffer_via_gateways" + CRLF +
    "import org.meshtastic.core.resources.sniffer_via_relays" + CRLF
)
content = replace_once(content, old_import, new_import, "sniffer_via_relays import")

# 2. Rename RadioPacketCard -> GroupedRadioPacketCard(grouped: GroupedSniffedPacket, ...) and add the relay-list row.
old_card = (
    "@Composable" + CRLF +
    "internal fun RadioPacketCard(" + CRLF +
    "    packet: SniffedPacket," + CRLF +
    "    decryptPayloads: Boolean," + CRLF +
    "    isExpanded: Boolean," + CRLF +
    "    onClick: () -> Unit," + CRLF +
    ") {" + CRLF +
    "    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {" + CRLF +
    "        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {" + CRLF +
    "            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {" + CRLF +
    "                val fromLabel = packet.fromShortName?.let { \"${packet.fromId} ($it)\" } ?: packet.fromId" + CRLF +
    "                val toLabel = packet.toShortName?.let { \"${packet.toId} ($it)\" } ?: packet.toId" + CRLF +
    "                Text(text = \"$fromLabel → $toLabel\", style = MaterialTheme.typography.titleSmall)" + CRLF +
    "                Text(" + CRLF +
    "                    text =" + CRLF +
    "                    \"${DateFormatter.formatDate(packet.receivedAtMillis)} \" +" + CRLF +
    "                        DateFormatter.formatTimeWithSeconds(packet.receivedAtMillis)," + CRLF +
    "                    style = MaterialTheme.typography.labelSmall," + CRLF +
    "                    color = MaterialTheme.colorScheme.onSurfaceVariant," + CRLF +
    "                )" + CRLF +
    "            }" + CRLF +
    "            val hopsText = \"hops ${packet.hopStart - packet.hopLimit}/${packet.hopStart}\"" + CRLF +
    "            val signalText = listOfNotNull(packet.rssi?.let { \"RSSI $it\" }, \"SNR ${packet.snr}\").joinToString(\" • \")" + CRLF +
    "            Text(" + CRLF +
    "                text = \"ch ${packet.channel} • $hopsText • $signalText\"," + CRLF +
    "                style = MaterialTheme.typography.bodySmall," + CRLF +
    "                color = MaterialTheme.colorScheme.onSurfaceVariant," + CRLF +
    "            )" + CRLF +
    "            val portText = if (packet.isEncrypted) \"encrypted\" else packet.portNum?.let { \"port $it\" } ?: \"unknown port\"" + CRLF +
    "            Text(" + CRLF +
    "                text = portText," + CRLF +
    "                style = MaterialTheme.typography.bodySmall," + CRLF +
    "                color = MaterialTheme.colorScheme.onSurfaceVariant," + CRLF +
    "            )" + CRLF +
    "            if (isExpanded) {" + CRLF +
    "                val payload = if (decryptPayloads) packet.decodedPayload ?: packet.payloadHex else packet.payloadHex" + CRLF +
    "                Text(text = payload, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))" + CRLF +
    "                CopyIconButton(valueToCopy = packet.copyText, modifier = Modifier.padding(top = 4.dp))" + CRLF +
    "            }" + CRLF +
    "        }" + CRLF +
    "    }" + CRLF +
    "}" + CRLF
)

new_card = (
    "@Composable" + CRLF +
    "internal fun GroupedRadioPacketCard(" + CRLF +
    "    grouped: GroupedSniffedPacket," + CRLF +
    "    decryptPayloads: Boolean," + CRLF +
    "    isExpanded: Boolean," + CRLF +
    "    onClick: () -> Unit," + CRLF +
    ") {" + CRLF +
    "    val packet = grouped.packet" + CRLF +
    "    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {" + CRLF +
    "        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {" + CRLF +
    "            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {" + CRLF +
    "                val fromLabel = packet.fromShortName?.let { \"${packet.fromId} ($it)\" } ?: packet.fromId" + CRLF +
    "                val toLabel = packet.toShortName?.let { \"${packet.toId} ($it)\" } ?: packet.toId" + CRLF +
    "                Text(text = \"$fromLabel → $toLabel\", style = MaterialTheme.typography.titleSmall)" + CRLF +
    "                Text(" + CRLF +
    "                    text =" + CRLF +
    "                    \"${DateFormatter.formatDate(packet.receivedAtMillis)} \" +" + CRLF +
    "                        DateFormatter.formatTimeWithSeconds(packet.receivedAtMillis)," + CRLF +
    "                    style = MaterialTheme.typography.labelSmall," + CRLF +
    "                    color = MaterialTheme.colorScheme.onSurfaceVariant," + CRLF +
    "                )" + CRLF +
    "            }" + CRLF +
    "            val hopsText = \"hops ${packet.hopStart - packet.hopLimit}/${packet.hopStart}\"" + CRLF +
    "            val signalText = listOfNotNull(packet.rssi?.let { \"RSSI $it\" }, \"SNR ${packet.snr}\").joinToString(\" • \")" + CRLF +
    "            Text(" + CRLF +
    "                text = \"ch ${packet.channel} • $hopsText • $signalText\"," + CRLF +
    "                style = MaterialTheme.typography.bodySmall," + CRLF +
    "                color = MaterialTheme.colorScheme.onSurfaceVariant," + CRLF +
    "            )" + CRLF +
    "            val portText = if (packet.isEncrypted) \"encrypted\" else packet.portNum?.let { \"port $it\" } ?: \"unknown port\"" + CRLF +
    "            Text(" + CRLF +
    "                text = portText," + CRLF +
    "                style = MaterialTheme.typography.bodySmall," + CRLF +
    "                color = MaterialTheme.colorScheme.onSurfaceVariant," + CRLF +
    "            )" + CRLF +
    "            if (grouped.relayIds.isNotEmpty()) {" + CRLF +
    "                Text(" + CRLF +
    "                    text = stringResource(Res.string.sniffer_via_relays, grouped.relayIds.joinToString(\", \"))," + CRLF +
    "                    style = MaterialTheme.typography.bodySmall," + CRLF +
    "                    color = MaterialTheme.colorScheme.onSurfaceVariant," + CRLF +
    "                )" + CRLF +
    "            }" + CRLF +
    "            if (isExpanded) {" + CRLF +
    "                val payload = if (decryptPayloads) packet.decodedPayload ?: packet.payloadHex else packet.payloadHex" + CRLF +
    "                Text(text = payload, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))" + CRLF +
    "                CopyIconButton(valueToCopy = packet.copyText, modifier = Modifier.padding(top = 4.dp))" + CRLF +
    "            }" + CRLF +
    "        }" + CRLF +
    "    }" + CRLF +
    "}" + CRLF
)
content = replace_once(content, old_card, new_card, "RadioPacketCard -> GroupedRadioPacketCard")

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("SnifferPanelComponents.kt fixed, new length:", len(content))
