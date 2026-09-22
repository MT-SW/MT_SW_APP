import io

CRLF = "\r\n"
path = "core/resources/src/commonMain/composeResources/values-pl/strings.xml"

with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()

lines = content.split(CRLF)

# Locate all conflict marker triples in order.
blocks = []
i = 0
n = len(lines)
while i < n:
    if lines[i].startswith("<<<<<<<"):
        s = i
        j = i + 1
        while not lines[j].startswith("======="):
            j += 1
        sep = j
        k = j + 1
        while not lines[k].startswith(">>>>>>>"):
            k += 1
        e = k
        blocks.append((s, sep, e))
        i = e + 1
    else:
        i += 1

assert len(blocks) == 37, f"expected 37 blocks, found {len(blocks)}"

# Per-block (1-indexed) overrides. Default resolution = keep the HEAD (ours) side verbatim.
# Only three blocks need a text edit within the kept HEAD side; everything else is pure HEAD.
def edit_block_10(head_lines):
    old = '    <string name="config_security_private_key">Klucz prywatny, na podstawie którego generujemy ID (fw 2.8) oraz klucz publiczny.</string>'
    new = '    <string name="config_security_private_key">Używane do tworzenia klucza współdzielonego ze zdalnym urządzeniem.</string>'
    assert old in head_lines, "config_security_private_key line not found"
    idx = head_lines.index(old)
    head_lines[idx] = new
    return head_lines

def edit_block_16(head_lines):
    old = '    <string name="gpio_pin">Funkcja wymaga kanału "gpio" na 1 pozycji. Podaj GPIO (liczba całkowita)</string>'
    new = '    <string name="gpio_pin">Pin GPIO</string>'
    assert old in head_lines, "gpio_pin line not found"
    idx = head_lines.index(old)
    head_lines[idx] = new
    return head_lines

def edit_block_19(head_lines):
    anchor = '    <string name="licensed_amateur_radio">Radioamator licencjonowany (Ham)</string>'
    assert anchor in head_lines, "licensed_amateur_radio line not found"
    idx = head_lines.index(anchor)
    head_lines.insert(idx, "    <!-- LICENSED -->")
    return head_lines

overrides = {10: edit_block_10, 16: edit_block_16, 19: edit_block_19}

# Process blocks from the bottom up so earlier indices stay valid as we splice.
for num in range(len(blocks), 0, -1):
    s, sep, e = blocks[num - 1]
    head_lines = lines[s + 1 : sep]
    if num in overrides:
        head_lines = overrides[num](head_lines)
    lines[s : e + 1] = head_lines

new_content = CRLF.join(lines)
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(new_content)

print("values-pl/strings.xml resolved:", len(blocks), "blocks")
