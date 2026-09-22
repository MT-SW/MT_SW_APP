import io

CRLF = "\r\n"
LF = "\n"


def insert_after_prefix(lines, prefix, new_line, label):
    idx = [i for i, l in enumerate(lines) if l.strip().startswith(prefix)]
    assert len(idx) == 1, f"{label}: expected exactly 1 match for prefix {prefix!r}, found {len(idx)}"
    i = idx[0]
    return lines[: i + 1] + [new_line] + lines[i + 1 :]


# --- EN (LF + BOM) ---
path_en = "core/resources/src/commonMain/composeResources/values/strings.xml"
with io.open(path_en, "r", encoding="utf-8", newline="") as f:
    en = f.read()
assert en.count(LF) > 0
assert en.count(CRLF) == 0

en_lines = en.split(LF)
en_new_line = (
    '    <string name="sniffer_enable_failed">Couldn\'t enable the sniffer — the device didn\'t respond (it may '
    "not support this feature)</string>"
)
en_lines = insert_after_prefix(en_lines, '<string name="sniffer_dismiss_loaded_file">', en_new_line, "EN")
en = LF.join(en_lines)
with io.open(path_en, "w", encoding="utf-8", newline="") as f:
    f.write(en)
print("EN strings.xml fixed, new length:", len(en))

# --- PL (CRLF) ---
path_pl = "core/resources/src/commonMain/composeResources/values-pl/strings.xml"
with io.open(path_pl, "r", encoding="utf-8", newline="") as f:
    pl = f.read()
assert pl.count(CRLF) > 0

pl_lines = pl.split(CRLF)
pl_new_line = (
    '    <string name="sniffer_enable_failed">Nie udało się włączyć snifera — urządzenie nie odpowiedziało (może '
    "nie obsługiwać tej funkcji)</string>"
)
pl_lines = insert_after_prefix(pl_lines, '<string name="sniffer_dismiss_loaded_file">', pl_new_line, "PL")
pl = CRLF.join(pl_lines)
with io.open(path_pl, "w", encoding="utf-8", newline="") as f:
    f.write(pl)
print("PL strings.xml fixed, new length:", len(pl))
