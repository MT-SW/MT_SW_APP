import io
CRLF = "\r\n"
path = "core/ui/src/commonMain/kotlin/org/meshtastic/core/ui/component/DropDownPreference.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    c = f.read()
old = (
    "<<<<<<< HEAD" + CRLF +
    "        remember(selectedItem, excludedItems) {" + CRLF +
    "            enumEntriesOf(selectedItem).filter {" + CRLF +
    '                it.name != "UNRECOGNIZED" && !it.isDeprecatedEnumEntry() && it !in excludedItems' + CRLF +
    "=======" + CRLF +
    "        remember(selectedItem) {" + CRLF +
    "            enumEntriesOf(selectedItem).filter {" + CRLF +
    '                it.name != "UNRECOGNIZED" && (it == selectedItem || !it.isDeprecatedEnumEntry())' + CRLF +
    ">>>>>>> upstream/main" + CRLF
)
new = (
    "        remember(selectedItem, excludedItems) {" + CRLF +
    "            enumEntriesOf(selectedItem).filter {" + CRLF +
    '                it.name != "UNRECOGNIZED" &&' + CRLF +
    "                    (it == selectedItem || (!it.isDeprecatedEnumEntry() && it !in excludedItems))" + CRLF
)
n = c.count(old)
assert n == 1, f"found {n}"
c = c.replace(old, new)
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(c)
print("DropDownPreference.kt ok")
