import io
CRLF = "\r\n"
path = "feature/node/src/commonMain/kotlin/org/meshtastic/feature/node/list/NodeListScreen.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    c = f.read()
lines = c.split(CRLF)
start = [i for i, l in enumerate(lines) if l.startswith("<<<<<<< HEAD")]
end = [i for i, l in enumerate(lines) if l.startswith(">>>>>>> upstream/main")]
assert len(start) == 1 and len(end) == 1, (start, end)
s, e = start[0], end[0]
sep = [i for i in range(s, e) if lines[i].strip() == "======="]
assert len(sep) == 1
sepi = sep[0]
ours_block = lines[s + 1 : sepi]
theirs_block = lines[sepi + 1 : e]
assert theirs_block == ["                items(nodes, key = { it.num }, itemContent = nodeRow)"], theirs_block
lines[s : e + 1] = ours_block
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(CRLF.join(lines))
print("NodeListScreen.kt ok, kept ours block of", len(ours_block), "lines")
