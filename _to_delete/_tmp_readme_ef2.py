import io

CRLF = "\r\n"
path = "README.md"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0


def replace_once(content, old, new, label):
    n = content.count(old)
    assert n == 1, f"{label}: expected exactly 1 match, found {n}"
    return content.replace(old, new)


old_pl = (
    "- **Wymaga customowego firmware z modułem snifera radiowego** — sterowanie snifera radiowego przeniesiono "
    "na protokół OnDemand (port 354): appka po połączeniu pyta węzeł o stan snifera i czeka na odpowiedź; "
    "firmware, które nie obsługuje tego zapytania, po prostu nigdy nie odpowiada, a appka wyszarza przełącznik "
    "po przekroczeniu limitu czasu, zamiast czekać w nieskończoność. Sniffer MQTT nie wymaga customowego "
    "firmware, tylko skonfigurowanego brokera."
)
new_pl = (
    "- **Wymaga customowego firmware z modułem snifera radiowego** — sterowanie snifera radiowego przeniesiono "
    "na protokół OnDemand (port 354): appka po połączeniu pyta węzeł o stan snifera i czeka na odpowiedź; "
    "firmware, które nie obsługuje tego zapytania, po prostu nigdy nie odpowiada, a appka wyszarza przełącznik "
    "po przekroczeniu limitu czasu, zamiast czekać w nieskończoność. Jeśli mimo to spróbujesz aktywnie włączyć "
    "Radio (np. zanim appka zdąży wykryć brak wsparcia), a urządzenie nie odpowie na czas, wybór cofa się na "
    "\"Wyłączony\" i pojawia się komunikat o nieudanym włączeniu, zamiast zostawiać przełącznik w mylącym, "
    "pozornie aktywnym stanie. Sniffer MQTT nie wymaga customowego firmware, tylko skonfigurowanego brokera."
)
content = replace_once(content, old_pl, new_pl, "PL enable-failed note")

old_en = (
    "- **Requires custom firmware for the radio sniffer module** — control for the radio sniffer runs over the "
    "OnDemand protocol (port 354): on connect, the app asks the node for the sniffer's current state and waits "
    "for a reply; firmware that doesn't support the request simply never answers, and the app grays out the "
    "toggle once the timeout passes instead of waiting forever. The MQTT sniffer needs no custom firmware, "
    "just a configured broker."
)
new_en = (
    "- **Requires custom firmware for the radio sniffer module** — control for the radio sniffer runs over the "
    "OnDemand protocol (port 354): on connect, the app asks the node for the sniffer's current state and waits "
    "for a reply; firmware that doesn't support the request simply never answers, and the app grays out the "
    "toggle once the timeout passes instead of waiting forever. If you actively try to enable Radio anyway "
    "(e.g. before the app has had a chance to detect the lack of support) and the device doesn't respond in "
    "time, the selection reverts to Off with a message about the failed enable, instead of being left looking "
    "selected while nothing is actually happening. The MQTT sniffer needs no custom firmware, just a "
    "configured broker."
)
content = replace_once(content, old_en, new_en, "EN enable-failed note")

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("README.md fixed, new length:", len(content))
