import io
CRLF = "\r\n"
path = "core/model/src/commonMain/kotlin/org/meshtastic/core/model/Capabilities.kt"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    c = f.read()
old = "<<<<<<< HEAD" + CRLF + "    val supportsMeshBeacon = atLeast(V2_7_18)" + CRLF + "=======" + CRLF + "    val supportsMeshBeacon = offers(ModuleConfig.mesh_beacon)" + CRLF + ">>>>>>> upstream/main" + CRLF
new = "    val supportsMeshBeacon = offers(ModuleConfig.mesh_beacon)" + CRLF
n = c.count(old)
assert n == 1, f"found {n}"
c = c.replace(old, new)
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(c)
print("Capabilities.kt ok")
