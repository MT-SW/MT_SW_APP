import io

CRLF = "\r\n"
path = "SnifferSettingsScreen.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0

old = (
    "/**" + CRLF +
    " * The packets currently backing the MQTT side of the display -- a loaded file's rows, or the live stream bounded" + CRLF +
    " * to ([clearedAtMillis], [freezeAtMillis]] (see [SnifferPanelViewModel.freezeAtMillis] for why an upper bound is" + CRLF +
    " * needed too, not just the trash icon's lower one)." + CRLF +
    " */" + CRLF
)
new = (
    "/**" + CRLF +
    " * The packets currently backing the MQTT side of the display -- a loaded file's rows, or the live stream" + CRLF +
    " * restricted to timestamps after [clearedAtMillis] and at or before [freezeAtMillis] (see" + CRLF +
    " * [SnifferPanelViewModel.freezeAtMillis] for why an upper bound is needed too, not just the trash icon's" + CRLF +
    " * lower one)." + CRLF +
    " */" + CRLF
)
n = content.count(old)
assert n == 1, f"expected exactly 1 match, found {n}"
content = content.replace(old, new)

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("Doc comment fixed, new length:", len(content))
