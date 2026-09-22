import io

CRLF = "\r\n"
path = "README.md"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0


def replace_once(content, old, new, label):
    n = content.count(old)
    assert n == 1, f"{label}: expected exactly 1 match, found {n}"
    return content.replace(old, new)


old_pl = (
    "- **Ręczny przycisk auto-przewijania** — pływający przycisk w rogu ekranu pokazuje, czy lista śledzi "
    "najnowsze pakiety na żywo, czy jest wstrzymana (np. bo przeglądasz starsze wpisy); gdy wstrzymana i "
    "przyjdą nowe pakiety, przycisk zamienia się w pigułkę z licznikiem nieprzeczytanych, z krótkim "
    "niebieskim błyskiem i poświatą przy każdym nowym pakiecie. Dotknięcie przycisku przełącza tryb i "
    "wraca na górę listy." + CRLF
)
content = replace_once(content, old_pl, "", "remove PL stale auto-scroll bullet")

old_en = (
    "- **Manual auto-scroll button** — a floating button in the corner shows whether the list is following "
    "new packets live or paused (e.g. because you're browsing older entries); when paused and new packets "
    "arrive, the button turns into a pill showing the unseen count, with a brief blue flash and glow on "
    "each new arrival. Tapping it toggles the mode and jumps back to the top." + CRLF
)
content = replace_once(content, old_en, "", "remove EN stale auto-scroll bullet")

with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(content)
print("README.md fixed, new length:", len(content))
