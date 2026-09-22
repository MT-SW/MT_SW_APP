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


old_import = (
    "import org.meshtastic.core.resources.sniffer_dismiss_loaded_file" + CRLF +
    "import org.meshtastic.core.resources.sniffer_load_failed" + CRLF
)
new_import = (
    "import org.meshtastic.core.resources.sniffer_dismiss_loaded_file" + CRLF +
    "import org.meshtastic.core.resources.sniffer_enable_failed" + CRLF +
    "import org.meshtastic.core.resources.sniffer_load_failed" + CRLF
)
content = replace_once(content, old_import, new_import, "sniffer_enable_failed import")

old_effect = (
    "    val snackbarHostState = remember { SnackbarHostState() }" + CRLF +
    "    val loadFailedMessage = stringResource(Res.string.sniffer_load_failed)" + CRLF +
    "    LaunchedEffect(panelViewModel) {" + CRLF +
    "        panelViewModel.loadFailed.collect { snackbarHostState.showSnackbar(loadFailedMessage) }" + CRLF +
    "    }" + CRLF
)
new_effect = (
    "    val snackbarHostState = remember { SnackbarHostState() }" + CRLF +
    "    val loadFailedMessage = stringResource(Res.string.sniffer_load_failed)" + CRLF +
    "    LaunchedEffect(panelViewModel) {" + CRLF +
    "        panelViewModel.loadFailed.collect { snackbarHostState.showSnackbar(loadFailedMessage) }" + CRLF +
    "    }" + CRLF +
    "    val snifferEnableFailedMessage = stringResource(Res.string.sniffer_enable_failed)" + CRLF +
    "    LaunchedEffect(radioConfigViewModel, panelViewModel) {" + CRLF +
    "        radioConfigViewModel.snifferEnableFailed.collect {" + CRLF +
    "            panelViewModel.selectSource(SnifferSource.OFF)" + CRLF +
    "            snackbarHostState.showSnackbar(snifferEnableFailedMessage)" + CRLF +
    "        }" + CRLF +
    "    }" + CRLF
)
content = replace_once(content, old_effect, new_effect, "snifferEnableFailed LaunchedEffect")

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("SnifferSettingsScreen.kt fixed, new length:", len(content))
