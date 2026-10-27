# Kanały, klucze (PSK) i precyzja pozycji

### Jakie są role kanałów i ile może być kanałów głównych?
PRIMARY (główny): tworzony przy konfiguracji, może być tylko jeden i nie da się go wyłączyć; domyślnie to nim idą okresowe pozycje i telemetria. SECONDARY (dodatkowy): może mieć własny klucz PSK. DISABLED: wyłączony, ustawienia wracają do domyślnych. Aktywne kanały muszą iść po kolei: wyłączony nie może leżeć między aktywnymi.
Słowa: kanał, primary, secondary, główny, dodatkowy, disabled, wyłączony
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)

### Jak nazwać kanał i co znaczy kanał "admin"?
Nazwa to krótki identyfikator poniżej 12 bajtów. Pusta nazwa na kanale głównym oznacza kanał "default". Na kanale dodatkowym nazwa "admin" (wielkość liter bez znaczenia) oznacza kanał administracyjny i działa tylko po włączeniu Admin Channel Enabled; to funkcja starsza (legacy). Aby urządzenia się porozumiewały, nazwy kanału muszą się zgadzać.
Słowa: nazwa kanału, admin, default, 12 bajtów, administracja
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)

### Jakie klucze PSK są dozwolone i który wybrać?
Klucz ma 0 bajtów (brak szyfrowania), 16 (AES128) albo 32 bajty (AES256); wszystkie urządzenia na kanale muszą mieć ten sam. Opcje: none, default, random, simple0..simple254, własny klucz hex lub base64. Do prywatnej łączności używaj random (bezpieczny klucz 256-bitowy). Opcje default i simple używają publicznie znanych kluczy z kodu źródłowego: każdy może słuchać takich wiadomości, więc nadają się tylko do testów i kanałów publicznych.
Słowa: PSK, klucz, szyfrowanie, AES, AES128, AES256, random, default, simple, base64, hex, prywatny kanał, bezpieczeństwo
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)

### Jaki jest klucz domyślny kanału?
Kanał główny "default" używa bajtu hex 0x01, czyli klucza base64 AQ==. To słaby, publicznie znany klucz.
Słowa: AQ==, 0x01, klucz domyślny, publiczny
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)

### Co to uplink i downlink na kanale?
Uplink Enabled (domyślnie wyłączone): wysyła wiadomości z sieci mesh do internetu przez bramkę skonfigurowaną na węźle. Downlink Enabled (domyślnie wyłączone): przekazuje do lokalnej sieci mesh wiadomości zebrane z publicznej bramki internetowej. Konfiguracja bramki jest w ustawieniach MQTT.
Słowa: uplink, downlink, MQTT, bramka, gateway, internet
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)

### Co oznacza precyzja pozycji na kanale?
Liczba całkowita 0–32. 0: pozycja nigdy nie jest wysyłana na tym kanale, 32: pełna precyzja. Na kanałach z kluczem publicznym (brak klucza, klucz domyślny, klucze jednobajtowe) firmware ogranicza precyzję do 15 bitów. Przybliżone rozmiary: 10 bitów to 23,3 km, 13 bitów 2,9 km, 15 bitów 729 m, 19 bitów 45 m. Publiczny serwer MQTT odfiltrowuje dokładne pozycje.
Słowa: precyzja, pozycja, lokalizacja, prywatność, bity, 15 bitów, dokładność GPS
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)

### Czy zmiana nazwy kanału głównego zmienia częstotliwość?
Tak, może. Nazwa kanału głównego wyznacza slot częstotliwości w paśmie; gdy slot ustawiono na 0, większość regionów liczy go z hasha nazwy kanału głównego. Zmiana nazwy może więc przenieść węzeł z częstotliwości używanej w okolicy, chyba że slot ustawiono jawnie. Prywatne grupy lepiej trzymać na kanałach dodatkowych. Ustawienia modemu (preset) są wspólne dla wszystkich kanałów.
Słowa: slot, częstotliwość, hash, nazwa kanału głównego, channel num, nie widzę innych węzłów, inna częstotliwość
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)

### Co to jest "Is Muted" i AEAD na kanale?
Is Muted (domyślnie wyłączone): wycisza moduł powiadomień zewnętrznych i blokuje wyskakujące powiadomienia o wiadomościach z kanału. AEAD (firmware 2.8.1+): zamiast AES-CTR używa uwierzytelnionego AES-CCM z 12-bajtowym znacznikiem, więc węzły odrzucają zmienione lub sfałszowane pakiety; musi być włączony na wszystkich węzłach kanału (inaczej się nie odczytają), wymaga klucza, jest eksperymentalny i domyślnie wyłączony.
Słowa: muted, wyciszenie, AEAD, AES-CCM, uwierzytelnianie, eksperymentalne
Źródło: https://meshtastic.org/docs/configuration/radio/channels/ (sprawdzono 2026-10)
