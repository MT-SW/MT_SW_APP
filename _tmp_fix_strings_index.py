import io

CRLF = "\r\n"
path = ".skills/compose-ui/strings-index.txt"

with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()

# Despite the generator script writing newline='\n', the checked-out file is CRLF
# (project-wide .gitattributes normalization) -- confirmed via byte count before running.
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

assert len(blocks) == 5, f"expected 5 blocks, found {len(blocks)}"

# All 5 are upstream-empty (pure fork-only keys) except block 2, which also needs
# upstream's new "label_with_unit" key appended after our label_* group.
for num in range(len(blocks), 0, -1):
    s, sep, e = blocks[num - 1]
    head_lines = lines[s + 1 : sep]
    upstream_lines = lines[sep + 1 : e]
    if num == 2:
        assert upstream_lines == ["label_with_unit"], upstream_lines
        assert head_lines[-1] == "label_very_long_slow", head_lines[-1]
        head_lines = head_lines + ["label_with_unit"]
    else:
        assert upstream_lines == [], (num, upstream_lines)
    lines[s : e + 1] = head_lines

new_content = CRLF.join(lines)
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(new_content)

print("strings-index.txt resolved:", len(blocks), "blocks")
