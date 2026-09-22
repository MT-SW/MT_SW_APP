import io
CRLF = "\r\n"
path = "feature/settings/src/jvmMain/kotlin/org/meshtastic/feature/settings/DesktopSettingsScreen.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    c = f.read()
old = (
    "<<<<<<< HEAD" + CRLF +
    "import org.meshtastic.feature.settings.radio.component.EditDeviceProfileDialog" + CRLF +
    "import org.meshtastic.proto.DeviceProfile" + CRLF +
    "=======" + CRLF +
    "import org.meshtastic.feature.settings.search.SettingsSearchBar" + CRLF +
    "import org.meshtastic.feature.settings.search.SettingsSearchViewModel" + CRLF +
    ">>>>>>> upstream/main" + CRLF
)
new = (
    "import org.meshtastic.feature.settings.radio.component.EditDeviceProfileDialog" + CRLF +
    "import org.meshtastic.feature.settings.search.SettingsSearchBar" + CRLF +
    "import org.meshtastic.feature.settings.search.SettingsSearchViewModel" + CRLF +
    "import org.meshtastic.proto.DeviceProfile" + CRLF
)
n = c.count(old)
assert n == 1, f"found {n}"
c = c.replace(old, new)
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(c)
print("DesktopSettingsScreen.kt ok")
