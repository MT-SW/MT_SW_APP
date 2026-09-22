import io
CRLF = "\r\n"
path = "feature/settings/src/commonMain/kotlin/org/meshtastic/feature/settings/navigation/ModuleRoute.kt"
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
theirs_block = lines[sepi + 1 : e]
assert len(theirs_block) == 15, len(theirs_block)
lines[s : e + 1] = theirs_block
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(CRLF.join(lines))
print("ModuleRoute.kt ok, kept upstream block of", len(theirs_block), "lines")
