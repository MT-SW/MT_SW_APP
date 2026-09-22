import io

CRLF = "\r\n"
path = "core/network/src/commonMain/kotlin/org/meshtastic/core/network/repository/MQTTRepositoryImpl.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0

old = (
    "            session.subscriptionRefusal.value = e" + CRLF +
    "        }" + CRLF +
    "    }" + CRLF +
    "        }" + CRLF +
    "    }" + CRLF +
    "" + CRLF +
    "    private fun replaceActiveSession(replacement: ActiveMqttSession): ActiveMqttSession? {" + CRLF
)
new = (
    "            session.subscriptionRefusal.value = e" + CRLF +
    "        }" + CRLF +
    "    }" + CRLF +
    "" + CRLF +
    "    private fun replaceActiveSession(replacement: ActiveMqttSession): ActiveMqttSession? {" + CRLF
)
n = content.count(old)
assert n == 1, f"expected exactly 1 match, found {n}"
content = content.replace(old, new)

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("step4 (stray braces removed) ok, new length:", len(content))
