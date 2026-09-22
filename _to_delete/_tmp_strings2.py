import io

CRLF = "\r\n"


def replace_once(content, old, new, label):
    n = content.count(old)
    assert n == 1, f"{label}: expected exactly 1 match, found {n}"
    return content.replace(old, new)


# --- EN ---
path_en = "core/resources/src/commonMain/composeResources/values/strings.xml"
with io.open(path_en, "r", encoding="utf-8", newline="") as f:
    en = f.read()
assert en.count(CRLF) > 0

en = replace_once(
    en,
    '    <string name="sniffer_group_by_gateway_summary">Combine the same MQTT packet seen from multiple gateways '
    'into one row</string>' + CRLF,
    '    <string name="sniffer_group_by_gateway_summary">Combine the same packet seen more than once (multiple '
    'MQTT gateways, or relayed through different nodes) into one row</string>' + CRLF,
    "EN group_by_gateway_summary",
)
en = replace_once(
    en,
    '    <string name="sniffer_group_by_gateway_title">Group by gateway</string>' + CRLF,
    '    <string name="sniffer_group_by_gateway_title">Group duplicates</string>' + CRLF,
    "EN group_by_gateway_title",
)
en = replace_once(
    en,
    '    <string name="sniffer_via_gateways">via %1$s</string>' + CRLF,
    '    <string name="sniffer_via_gateways">Seen via gateways: %1$s</string>' + CRLF +
    '    <string name="sniffer_via_relays">Relayed via: %1$s</string>' + CRLF,
    "EN via_gateways + new via_relays",
)
with io.open(path_en, "w", encoding="utf-8", newline="") as f:
    f.write(en)
print("EN strings.xml fixed, new length:", len(en))

# --- PL ---
path_pl = "core/resources/src/commonMain/composeResources/values-pl/strings.xml"
with io.open(path_pl, "r", encoding="utf-8", newline="") as f:
    pl = f.read()
assert pl.count(CRLF) > 0

pl = replace_once(
    pl,
    '    <string name="sniffer_group_by_gateway_title">Grupuj według bramki</string>' + CRLF,
    '    <string name="sniffer_group_by_gateway_title">Grupuj duplikaty</string>' + CRLF,
    "PL group_by_gateway_title",
)
pl = replace_once(
    pl,
    '    <string name="sniffer_group_by_gateway_summary">Łącz ten sam pakiet MQTT widziany z wielu bramek w jeden '
    'wiersz</string>' + CRLF,
    '    <string name="sniffer_group_by_gateway_summary">Łącz ten sam pakiet widziany więcej niż raz (wiele bramek '
    'MQTT lub różne węzły przekazujące) w jeden wiersz</string>' + CRLF,
    "PL group_by_gateway_summary",
)
pl = replace_once(
    pl,
    '    <string name="sniffer_via_gateways">przez %1$s</string>' + CRLF,
    '    <string name="sniffer_via_gateways">Widziane przez bramki: %1$s</string>' + CRLF +
    '    <string name="sniffer_via_relays">Przekazane przez: %1$s</string>' + CRLF,
    "PL via_gateways + new via_relays",
)
with io.open(path_pl, "w", encoding="utf-8", newline="") as f:
    f.write(pl)
print("PL strings.xml fixed, new length:", len(pl))
