import io
CRLF = "\r\n"
path = "gradle/libs.versions.toml"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    c = f.read()
old = (
    "<<<<<<< HEAD" + CRLF +
    'meshtastic-protobufs = "2.8.0.111-g45f6b7e-SNAPSHOT"' + CRLF +
    'wire = "6.4.7"' + CRLF +
    "=======" + CRLF +
    'meshtastic-protobufs = "2.8.0.116-g51028ca-SNAPSHOT"' + CRLF +
    ">>>>>>> upstream/main" + CRLF
)
new = (
    'meshtastic-protobufs = "2.8.0.116-g51028ca-SNAPSHOT"' + CRLF +
    'wire = "6.4.7"' + CRLF
)
n = c.count(old)
assert n == 1, f"found {n}"
c = c.replace(old, new)
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(c)
print("libs.versions.toml ok")
