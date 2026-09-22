import io
CRLF = "\r\n"
path = "feature/settings/src/commonMain/kotlin/org/meshtastic/feature/settings/radio/component/DeviceConfigScreen.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    c = f.read()
old = (
    "<<<<<<< HEAD" + CRLF +
    "                    itemLabel = { stringResource(it.label) }," + CRLF +
    "                    excludedItems = setOf(Config.DeviceConfig.Role.REPEATER, Config.DeviceConfig.Role.ROUTER_CLIENT)," + CRLF +
    "=======" + CRLF +
    ">>>>>>> upstream/main" + CRLF
)
new = (
    "                    itemLabel = { stringResource(it.label) }," + CRLF +
    "                    excludedItems = setOf(Config.DeviceConfig.Role.REPEATER, Config.DeviceConfig.Role.ROUTER_CLIENT)," + CRLF
)
n = c.count(old)
assert n == 1, f"found {n}"
c = c.replace(old, new)
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(c)
print("DeviceConfigScreen.kt ok")
