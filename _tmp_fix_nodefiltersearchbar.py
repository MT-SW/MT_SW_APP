import io
CRLF = "\r\n"
path = "feature/node/src/commonMain/kotlin/org/meshtastic/feature/node/component/NodeFilterSearchBar.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    c = f.read()
lines = c.split(CRLF)
start = [i for i, l in enumerate(lines) if l.startswith("<<<<<<< HEAD")]
end = [i for i, l in enumerate(lines) if l.startswith(">>>>>>> upstream/main")]
assert len(start) == 1 and len(end) == 1, (start, end)
s, e = start[0], end[0]
# upstream side (between ======= and >>>>>>>) must be empty -- verify by finding the ======= line
sep = [i for i in range(s, e) if lines[i].strip() == "======="]
assert len(sep) == 1
sepi = sep[0]
assert sepi == e - 1, f"expected upstream side empty, sep at {sepi}, end at {e}"
del lines[s : e + 1]
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(CRLF.join(lines))
print("NodeFilterSearchBar.kt ok, removed lines", s, "to", e)
