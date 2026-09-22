import io
CRLF = "\r\n"
path = "README.md"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    c = f.read()
old = (
    "<<<<<<< HEAD" + CRLF +
    'Osobny ekran dostępny z ekranu szczegółów węzła (Administracja → "Diagnostyka na żądanie"), niezależny od zdalnego sterowania GPIO. Pozwala odpytać dowolny węzeł w zasięgu o bieżące statystyki na żądanie, zamiast czekać na okresowe rozgłoszenia telemetrii.' + CRLF +
    "=======" + CRLF +
    "| Channel | Currently | Released |" + CRLF +
    "|---|---|---|" + CRLF +
    "| **Latest release** | `v2.8.1` | 2026-08-20 |" + CRLF +
    "| **Open beta** | `v2.8.2-open.3` | 2026-09-20 |" + CRLF +
    ">>>>>>> upstream/main" + CRLF
)
new = (
    'Osobny ekran dostępny z ekranu szczegółów węzła (Administracja → "Diagnostyka na żądanie"), niezależny od zdalnego sterowania GPIO. Pozwala odpytać dowolny węzeł w zasięgu o bieżące statystyki na żądanie, zamiast czekać na okresowe rozgłoszenia telemetrii.' + CRLF
)
n = c.count(old)
assert n == 1, f"found {n}"
c = c.replace(old, new)
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(c)
print("README.md ok")
