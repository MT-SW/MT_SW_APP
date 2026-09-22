import io

CRLF = "\r\n"
path = "SnifferSettingsScreen.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0

old = (
    "        SnifferSource.RADIO ->" + CRLF +
    "            RadioSnifferWarningDialog(" + CRLF +
    "                onConfirm = {" + CRLF +
    "                    pendingSource = null" + CRLF +
    "                    radioConfigViewModel.setSnifferEnabled(true)" + CRLF +
    "                    panelViewModel.selectSource(SnifferSource.RADIO)" + CRLF +
    "                }," + CRLF +
    "                onDismiss = { pendingSource = null }," + CRLF +
    "            )" + CRLF
)
new = (
    "        SnifferSource.RADIO ->" + CRLF +
    "            RadioSnifferWarningDialog(" + CRLF +
    "                // Only request the enable here -- selecting RADIO (and so showing/unfreezing the panel) is" + CRLF +
    "                // left entirely to the state.snifferEnabled LaunchedEffect below, once firmware actually" + CRLF +
    "                // confirms it. Selecting optimistically here used to let ordinary, non-sniffed MeshLog traffic" + CRLF +
    "                // show up in the panel for the few seconds before an unsupported-firmware timeout reverted it," + CRLF +
    "                // making it look like sniffing had briefly worked when it never did." + CRLF +
    "                onConfirm = {" + CRLF +
    "                    pendingSource = null" + CRLF +
    "                    radioConfigViewModel.setSnifferEnabled(true)" + CRLF +
    "                }," + CRLF +
    "                onDismiss = { pendingSource = null }," + CRLF +
    "            )" + CRLF
)
n = content.count(old)
assert n == 1, f"expected exactly 1 match, found {n}"
content = content.replace(old, new)

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("SnifferSettingsScreen.kt fixed, new length:", len(content))
