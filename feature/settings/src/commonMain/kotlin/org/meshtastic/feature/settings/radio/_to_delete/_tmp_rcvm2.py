import io

CRLF = "\r\n"


def replace_once(content, old, new, label):
    n = content.count(old)
    assert n == 1, f"{label}: expected exactly 1 match, found {n}"
    return content.replace(old, new)


# ============================== RadioConfigViewModel.kt ==============================
path_rc = "RadioConfigViewModel.kt"
with io.open(path_rc, "r", encoding="utf-8", newline="") as f:
    rc = f.read()
assert rc.count(CRLF) > 0

old_imports = (
    "import kotlinx.coroutines.flow.MutableStateFlow" + CRLF +
    "import kotlinx.coroutines.flow.StateFlow" + CRLF +
    "import kotlinx.coroutines.flow.asStateFlow" + CRLF
)
new_imports = (
    "import kotlinx.coroutines.flow.MutableSharedFlow" + CRLF +
    "import kotlinx.coroutines.flow.MutableStateFlow" + CRLF +
    "import kotlinx.coroutines.flow.SharedFlow" + CRLF +
    "import kotlinx.coroutines.flow.StateFlow" + CRLF +
    "import kotlinx.coroutines.flow.asSharedFlow" + CRLF +
    "import kotlinx.coroutines.flow.asStateFlow" + CRLF
)
rc = replace_once(rc, old_imports, new_imports, "RadioConfigViewModel imports")

old_state_decl = (
    "    private val _radioConfigState = MutableStateFlow(RadioConfigState())" + CRLF +
    "    val radioConfigState: StateFlow<RadioConfigState> = _radioConfigState" + CRLF
)
new_state_decl = (
    "    private val _radioConfigState = MutableStateFlow(RadioConfigState())" + CRLF +
    "    val radioConfigState: StateFlow<RadioConfigState> = _radioConfigState" + CRLF +
    CRLF +
    "    private val _snifferEnableFailed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)" + CRLF +
    CRLF +
    "    /**" + CRLF +
    "     * One-shot signal that a [setSnifferEnabled]`(true)` call timed out with no RESPONSE_SNIFFER_STATE at all --" + CRLF +
    "     * e.g. the connected firmware doesn't implement the Sniffer OnDemand protocol. [setSnifferEnabled] already" + CRLF +
    "     * clears [RadioConfigState.snifferLoading] in that case, but callers need this separate one-shot event to" + CRLF +
    "     * tell the user the enable attempt specifically failed, rather than just quietly stopping the spinner --" + CRLF +
    "     * distinct from the passive support probe every connection already runs (see `requestSnifferState`), which" + CRLF +
    "     * should stay silent." + CRLF +
    "     */" + CRLF +
    "    val snifferEnableFailed: SharedFlow<Unit> = _snifferEnableFailed.asSharedFlow()" + CRLF
)
rc = replace_once(rc, old_state_decl, new_state_decl, "snifferEnableFailed declaration")

old_set_enabled = (
    "    /** Toggles the Sniffer module on the local node via OnDemand (port 354); see [SnifferControlUseCase]. */" + CRLF +
    "    fun setSnifferEnabled(enabled: Boolean) {" + CRLF +
    "        val destNum = destNum ?: destNode.value?.num ?: return" + CRLF +
    "        safeLaunch(tag = \"setSnifferEnabled\") {" + CRLF +
    "            _radioConfigState.update { it.copy(snifferLoading = true) }" + CRLF +
    "            snifferControlUseCase.setEnabled(destNum, enabled)" + CRLF +
    "            delay(SNIFFER_STATE_TIMEOUT)" + CRLF +
    "            _radioConfigState.update { if (it.snifferEnabled == null) it.copy(snifferLoading = false) else it }" + CRLF +
    "        }" + CRLF +
    "    }" + CRLF
)
new_set_enabled = (
    "    /**" + CRLF +
    "     * Toggles the Sniffer module on the local node via OnDemand (port 354); see [SnifferControlUseCase]. Emits" + CRLF +
    "     * [snifferEnableFailed] if an `enabled = true` call times out with no response at all, so the caller can tell" + CRLF +
    "     * the user and back the selection out instead of leaving it looking enabled when it silently isn't." + CRLF +
    "     */" + CRLF +
    "    fun setSnifferEnabled(enabled: Boolean) {" + CRLF +
    "        val destNum = destNum ?: destNode.value?.num ?: return" + CRLF +
    "        safeLaunch(tag = \"setSnifferEnabled\") {" + CRLF +
    "            _radioConfigState.update { it.copy(snifferLoading = true) }" + CRLF +
    "            snifferControlUseCase.setEnabled(destNum, enabled)" + CRLF +
    "            delay(SNIFFER_STATE_TIMEOUT)" + CRLF +
    "            val timedOut = _radioConfigState.value.snifferEnabled == null" + CRLF +
    "            _radioConfigState.update { if (timedOut) it.copy(snifferLoading = false) else it }" + CRLF +
    "            if (enabled && timedOut) _snifferEnableFailed.emit(Unit)" + CRLF +
    "        }" + CRLF +
    "    }" + CRLF
)
rc = replace_once(rc, old_set_enabled, new_set_enabled, "setSnifferEnabled body")

with io.open(path_rc, "w", encoding="utf-8", newline="") as f:
    f.write(rc)
print("RadioConfigViewModel.kt fixed, new length:", len(rc))
