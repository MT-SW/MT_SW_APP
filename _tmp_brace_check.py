path = "core/network/src/commonMain/kotlin/org/meshtastic/core/network/repository/MQTTRepositoryImpl.kt"
lines = open(path, encoding="utf-8").read().split("\r\n")
print("total lines", len(lines))
bal = 0
for i, l in enumerate(lines, 1):
    bal += l.count("{") - l.count("}")
    if 185 <= i <= 280:
        print(i, bal, repr(l[:100]))
