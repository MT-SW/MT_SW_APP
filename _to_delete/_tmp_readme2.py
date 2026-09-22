import io

CRLF = "\r\n"
path = "README.md"
with io.open(path, "r", encoding="utf-8", newline="") as f:
    content = f.read()
assert content.count(CRLF) > 0
assert content.count("\n") == content.count(CRLF), "unexpected stray LF in README.md"

lines = content.split(CRLF)


def replace_section(lines, heading, next_heading, new_body_lines, occurrence):
    idx = [i for i, l in enumerate(lines) if l == heading]
    assert len(idx) > occurrence, f"heading {heading!r} occurrence {occurrence} not found (found {len(idx)})"
    start = idx[occurrence]
    end = None
    for i in range(start + 1, len(lines)):
        if lines[i] == next_heading:
            end = i
            break
    assert end is not None, f"next heading {next_heading!r} not found after {heading!r}"
    return lines[:start] + new_body_lines + lines[end:]


def insert_after(lines, anchor_line, new_line, label):
    idx = [i for i, l in enumerate(lines) if l == anchor_line]
    assert len(idx) == 1, f"{label}: expected exactly 1 match for anchor, found {len(idx)}"
    i = idx[0]
    return lines[: i + 1] + [new_line] + lines[i + 1 :]


PL_SNIFFER = [
    "## Sniffer",
    "",
    "Tryb pokazujący ruch w eterze, który normalnie by zniknął — wszystkie pakiety usłyszane przez węzeł, "
    "łącznie z ruchem broadcastowym (wiadomości na kanałach, telemetria), są przekazywane surowo do telefonu "
    "zamiast po prostu odrzucane. Appka ma dwa źródła snifera — **radiowy** (LoRa) i **MQTT** (ruch na "
    "skonfigurowanym brokerze) — pokazywane na jednym, wspólnym ekranie logu.",
    "",
    "- **Jeden wspólny ekran \"Sniffer\" w Ustawienia → Advanced** zamiast dwóch osobnych — z menu ustawień "
    "(ikona koła zębatego) wybiera się aktywne źródło: Wyłączony / Radio / MQTT, tylko jedno naraz. Każde "
    "włączenie pyta osobno o potwierdzenie: Radio ostrzega, że może opóźniać lub gubić część wiadomości czatu "
    "bądź telemetrii (dzielą tę samą kolejkę transmisji do telefonu); MQTT ostrzega, że otwiera własne "
    "połączenie z brokerem i przerwie na czas swojego działania \"Proxy MQTT na tym telefonie\", bo appka "
    "obsługuje tylko jedno aktywne połączenie MQTT naraz.",
    "- **Log zostaje na ekranie po wyłączeniu snifera** — znika dopiero po ręcznym kliknięciu ikony kosza "
    "albo przy realnej zmianie aktywnego źródła (Radio↔MQTT), co czyści widok i zaczyna zbierać od nowa.",
    "- **Grupowanie duplikatów** — ten sam pakiet usłyszany więcej niż raz (przez kilka bramek MQTT albo "
    "przekazany przez różne węzły pośredniczące w sieci radiowej) pokazuje się jako jeden wiersz z pełną "
    "listą źródeł (\"Widziane przez bramki: ...\" / \"Przekazane przez: ...\"), zamiast osobnego wpisu dla "
    "każdej kopii. Włącznik w ustawieniach snifera.",
    "- **Automatyczne przewijanie i próba deszyfrowania jako osobne przełączniki** w ustawieniach snifera — "
    "pierwszy decyduje, czy lista ma skakać do najnowszego pakietu; drugi, czy zaszyfrowana zawartość ma być "
    "automatycznie odkodowywana znanymi kluczami kanałów appki, czy pokazywana jako surowy hex.",
    "- **Zapis i wczytywanie logu** — ikona zapisu w pasku górnym eksportuje aktualnie wyświetlany log do "
    "pliku (txt/JSON/CSV do wyboru w ustawieniach), a z menu ustawień można wczytać wcześniej zapisany log "
    "JSON z powrotem do podglądu (np. do analizy offline albo przesłania komuś innemu).",
    "- **Ekran logu pokazuje na żywo** nadawcę/odbiorcę (z krótką nazwą węzła obok numeru ID, gdy jest "
    "znana), kanał, liczbę przeskoków, RSSI/SNR i port pakietu (dla MQTT dodatkowo temat na brokerze). "
    "Zawartość pakietu dekoduje się dopiero po kliknięciu w niego — tym samym mechanizmem co Panel "
    "Debugowania (trasa traceroute z nazwami węzłów, pozycja, telemetria, NodeInfo itd., a dla "
    "nieznanych/zaszyfrowanych danych surowy hex jako fallback). Kliknięty pakiet można skopiować do "
    "schowka (z tą samą redakcją danych wrażliwych co Panel Debugowania); znacznik czasu pokazuje sekundy.",
    "- **Ręczny przycisk auto-przewijania** — pływający przycisk w rogu ekranu pokazuje, czy lista śledzi "
    "najnowsze pakiety na żywo, czy jest wstrzymana (np. bo przeglądasz starsze wpisy); gdy wstrzymana i "
    "przyjdą nowe pakiety, przycisk zamienia się w pigułkę z licznikiem nieprzeczytanych, z krótkim "
    "niebieskim błyskiem i poświatą przy każdym nowym pakiecie. Dotknięcie przycisku przełącza tryb i "
    "wraca na górę listy.",
    "- **Pakiety, których węzeł nie potrafił rozszyfrować (bo telefon nie zna klucza danego kanału), są "
    "widoczne w logu** — wcześniej takie pakiety były po cichu pomijane jeszcze przed zapisem do bazy, "
    "zamiast trafiać na listę z surowym hexem jak reszta nieznanych danych; poprawka dotyczy też ogólnego "
    "Panelu Debugowania, bo korzysta z tego samego logu.",
    "- **Log radiowy pokazuje kompletny, nieprzefiltrowany ruch** — łącznie z pakietami, które sam "
    "wysyłasz, i odpowiedziami adresowanymi do Twojego węzła, nie tylko podsłuchany ruch obcy i "
    "broadcastowy. Dzięki temu pełna para żądanie/odpowiedź — np. traceroute z rozwiązaną trasą — jest "
    "widoczna w jednym miejscu.",
    "- **Podsłuchane odpowiedzi traceroute adresowane do innych węzłów nie są już mylnie pokazywane jako "
    "wynik własnego zapytania o trasę.**",
    "- **Naprawione podwójne wpisy przy własnym traceroute** — appka filtruje duplikaty po ID pakietu.",
    "- **Sniffer radiowy działa wyłącznie lokalnie** — pokazuje tylko to, co fizycznie usłyszy radiem "
    "węzeł aktualnie podłączony do telefonu; sniffer MQTT słyszy tyle, ile publikuje broker, niezależnie "
    "od tego, który węzeł go zasila.",
    "- **Wymaga customowego firmware z modułem snifera radiowego** — sterowanie snifera radiowego "
    "przeniesiono na protokół OnDemand (port 354): appka po połączeniu pyta węzeł o stan snifera i czeka "
    "na odpowiedź; firmware, które nie obsługuje tego zapytania, po prostu nigdy nie odpowiada, a appka "
    "wyszarza przełącznik po przekroczeniu limitu czasu, zamiast czekać w nieskończoność. Sniffer MQTT "
    "nie wymaga customowego firmware, tylko skonfigurowanego brokera.",
    "- **Pochodzenie funkcji** — sniffer radiowy zaadaptowany z historycznego forka firmware Meshtastic "
    "(`musznik/firmware`, gałąź `trunk-io/update-trunk`).",
    "",
]

EN_SNIFFER = [
    "## Sniffer",
    "",
    "A mode that surfaces air traffic that would normally just vanish — every packet the node overhears, "
    "including broadcast traffic (channel messages, telemetry), is forwarded raw to the phone instead of "
    "being dropped. The app has two sniffer sources — **Radio** (LoRa) and **MQTT** (traffic on the "
    "configured broker) — shown on one shared log screen.",
    "",
    "- **One shared \"Sniffer\" screen in Settings → Advanced** instead of two separate ones — the settings "
    "menu (gear icon) picks the active source: Off / Radio / MQTT, only one at a time. Enabling either one "
    "prompts for confirmation separately: Radio warns it can delay or drop some chat messages or telemetry "
    "(sniffed traffic shares the same queue to the phone); MQTT warns that it opens its own connection to "
    "the broker and will interrupt \"MQTT proxy on this phone\" for as long as it runs, since the app only "
    "supports one active MQTT connection at a time.",
    "- **The log stays on screen after the sniffer is turned off** — it only clears when you tap the trash "
    "icon, or when the active source actually changes (Radio↔MQTT), which resets the view and starts "
    "collecting fresh.",
    "- **Duplicate grouping** — the same packet heard more than once (via several MQTT gateways, or "
    "relayed through different nodes on the radio mesh) shows as a single row with the full list of "
    "sources (\"Seen via gateways: ...\" / \"Relayed via: ...\") instead of a separate entry per copy. "
    "Toggle in the sniffer settings.",
    "- **Auto-scroll and attempt-decryption as separate toggles** in the sniffer settings — the first "
    "decides whether the list jumps to the newest packet; the second whether encrypted content is "
    "automatically decoded with this app's known channel keys, or shown as raw hex.",
    "- **Save and load a log** — the save icon in the top bar exports the currently displayed log to a "
    "file (txt/JSON/CSV, chosen in settings), and the settings menu can load a previously saved JSON log "
    "back in for viewing (e.g. for offline analysis or sharing with someone else).",
    "- **The log screen shows live** sender/receiver (with a short node name next to the ID when known), "
    "channel, hop count, RSSI/SNR, and the packet's port (for MQTT, also the broker topic). The packet's "
    "content only decodes once you tap on it — using the same decoder as the Debug Panel (traceroute path "
    "with node names, position, telemetry, NodeInfo, etc., falling back to raw hex for unknown or still-"
    "encrypted data). A tapped packet can be copied to the clipboard (with the same sensitive-data "
    "redaction as the Debug Panel); the timestamp shows seconds.",
    "- **Manual auto-scroll button** — a floating button in the corner shows whether the list is following "
    "new packets live or paused (e.g. because you're browsing older entries); when paused and new packets "
    "arrive, the button turns into a pill showing the unseen count, with a brief blue flash and glow on "
    "each new arrival. Tapping it toggles the mode and jumps back to the top.",
    "- **Packets the node couldn't decrypt (because the phone doesn't know that channel's key) are "
    "visible in the log** — previously such packets were silently dropped before being persisted, instead "
    "of showing up in the list with a raw-hex fallback like other unknown data; the fix also applies to "
    "the general Debug Panel, since it reads from the same log.",
    "- **The radio log shows the complete, unfiltered traffic** — including packets you send yourself and "
    "responses addressed to your node, not just overheard foreign and broadcast traffic. This means a "
    "full request/response pair — e.g. a traceroute with its resolved route — is visible together in one "
    "place.",
    "- **Sniffed traceroute responses addressed to other nodes no longer show up as if they were the "
    "result of your own traceroute request.**",
    "- **Fixed duplicate entries for your own traceroute** — the app filters duplicates by packet ID.",
    "- **The Radio sniffer is local only** — it surfaces only what the node currently connected to the "
    "phone physically hears over the radio; the MQTT sniffer hears whatever the broker publishes, "
    "regardless of which node feeds it.",
    "- **Requires custom firmware for the radio sniffer module** — control for the radio sniffer runs "
    "over the OnDemand protocol (port 354): on connect, the app asks the node for the sniffer's current "
    "state and waits for a reply; firmware that doesn't support the request simply never answers, and the "
    "app grays out the toggle once the timeout passes instead of waiting forever. The MQTT sniffer needs "
    "no custom firmware, just a configured broker.",
    "- **Feature origin** — the radio sniffer is adapted from a historical Meshtastic firmware fork "
    "(`musznik/firmware`, `trunk-io/update-trunk` branch).",
    "",
]

lines = replace_section(lines, "## Sniffer", "## Komunikator", PL_SNIFFER, occurrence=0)
lines = replace_section(lines, "## Sniffer", "## Messaging", EN_SNIFFER, occurrence=1)

pl_anchor = (
    "- **Zdjęcia w czacie przez link** — appka nie wysyła surowych bajtów zdjęcia przez LoRa (za mała "
    "przepustowość), tylko uploaduje je anonimowo na zewnętrzny serwer i wysyła sam link jako wiadomość "
    "tekstową; odbiorca widzi automatyczny podgląd. Przed wysyłką pojawia się dialog ostrzegający, że "
    "serwer hostingu jest publiczny."
)
pl_new_bullet = (
    "- **Zapis zdjęcia z podglądu na cały ekran** — po otwarciu zdjęcia z czatu na cały ekran pojawia się "
    "ikona zapisu, która pobiera oryginalny plik i zapisuje go na urządzeniu przez systemowe okno zapisu "
    "(tak jak przy eksporcie logów)."
)
lines = insert_after(lines, pl_anchor, pl_new_bullet, "PL Komunikator insert")

en_anchor = (
    "- **Photos in chat via link** — the app doesn't send raw photo bytes over LoRa (not enough "
    "bandwidth); instead it anonymously uploads the photo to an external server and sends just the link "
    "as a text message, with the recipient seeing an automatic preview. A confirmation dialog appears "
    "before sending, warning that the hosting server is public."
)
en_new_bullet = (
    "- **Save a photo from the full-screen viewer** — opening a chat photo full-screen now shows a save "
    "icon that downloads the original file and saves it to the device through the system's own save-file "
    "dialog (the same as log export)."
)
lines = insert_after(lines, en_anchor, en_new_bullet, "EN Messaging insert")

new_content = CRLF.join(lines)
with io.open(path, "w", encoding="utf-8", newline="") as f:
    f.write(new_content)
print("README.md updated, new length:", len(new_content))
