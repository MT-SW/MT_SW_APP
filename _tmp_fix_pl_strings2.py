import io

CRLF = "\r\n"
path = "core/resources/src/commonMain/composeResources/values-pl/strings.xml"

with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()

lines = content.split(CRLF)

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

assert len(blocks) == 14, f"expected 14 blocks, found {len(blocks)}"


def edit_block_2(head_lines, upstream_lines):
    # "distance": ours "Dystans" vs upstream "Odległość" -- the rest of the distance_* group
    # (distance_filters_description, distance_measurements_description) already say "odległość",
    # so take upstream's wording here for internal consistency.
    assert upstream_lines == ['    <string name="distance">Odległość</string>'], upstream_lines
    old = '    <string name="distance">Dystans</string>'
    assert old in head_lines
    idx = head_lines.index(old)
    head_lines[idx] = '    <string name="distance">Odległość</string>'
    return head_lines


def edit_block_13(head_lines, upstream_lines):
    # upstream added two new keys (unit_dbm, unit_meters) our fork doesn't have yet -- append them.
    assert upstream_lines == [
        '    <string name="unit_dbm">dBm</string>',
        '    <string name="unit_meters">m</string>',
    ], upstream_lines
    return head_lines + upstream_lines


overrides = {2: edit_block_2, 13: edit_block_13}

for num in range(len(blocks), 0, -1):
    s, sep, e = blocks[num - 1]
    head_lines = lines[s + 1 : sep]
    upstream_lines = lines[sep + 1 : e]
    if num in overrides:
        head_lines = overrides[num](head_lines, upstream_lines)
    else:
        assert upstream_lines == [] or all(
            u in head_lines for u in upstream_lines
        ), (num, upstream_lines)
    lines[s : e + 1] = head_lines

new_content = CRLF.join(lines)
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(new_content)

print("values-pl/strings.xml resolved:", len(blocks), "blocks")
