# Meshtastic: szyfrowanie, algorytm sieci i obsługa aplikacji (dokumentacja 2.8)

### Jak szyfrowane są kanały i wiadomości prywatne?
Każdy kanał ma własny wspólny klucz; kto go zna, czyta wszystko na kanale. Kanały używają AES-CTR (klucz 16 bajtów to AES-128, 32 bajty to AES-256). Domyślny klucz AQ== jest publiczny, więc na nim każdy może czytać. Od 2.8.1 kanał może używać AES-CCM (AEAD) z 12-bajtowym znacznikiem wykrywającym zmiany; muszą go włączyć wszystkie węzły kanału. Wiadomości prywatne: każdy węzeł ma parę kluczy X25519, wspólny klucz powstaje z prywatnego klucza nadawcy i publicznego odbiorcy, a treść szyfruje AES-256-CCM. Węzeł musi najpierw poznać klucz publiczny odbiorcy (z NodeInfo), zanim wyśle DM. Od 2.8 wiadomości rozgłoszeniowe są podpisywane XEdDSA (66 bajtów), chyba że są za duże.
Słowa: szyfrowanie, AES-CTR, AES-128, AES-256, AQ==, AEAD, AES-CCM, X25519, klucz publiczny, DM, podpis, XEdDSA, 2.8.1
Źródło: https://meshtastic.org/docs/overview/encryption/ (sprawdzono 2026-10)

### Czego szyfrowanie Meshtastic nie chroni?
Nagłówek pakietu nigdy nie jest szyfrowany, bo potrzebują go przekaźniki. Brak tzw. forward secrecy: nagrany ruch da się odszyfrować później, jeśli klucz wycieknie. X25519 nie jest odporny na komputery kwantowe. AES-CTR nie wykrywa modyfikacji (wykrywają ją kanały AES-CCM i podpisane broadcasty). Przy domyślnej polityce podpisów niepodpisane pakiety podające się za dowolny węzeł są przyjmowane; tryby BALANCED i STRICT odrzucają niepodpisane broadcasty od węzłów, o których wiadomo, że podpisują. Dokumentacja wprost mówi, że Meshtastic nie jest tak bezpieczny jak WPA3, TLS 1.3 czy Signal. Węzeł przekazuje odszyfrowane dane podłączonym klientom.
Słowa: bezpieczeństwo, nagłówek, forward secrecy, podszywanie, podpis pakietów, STRICT, BALANCED, kwantowe, Signal, ograniczenia szyfrowania
Źródło: https://meshtastic.org/docs/overview/encryption/ (sprawdzono 2026-10)

### Numer węzła, klucze administracyjne i dobre praktyki
Numer węzła to CRC-32 jego klucza publicznego: nowy klucz oznacza nowy numer. Węzły ufają pierwszemu kluczowi, jaki zobaczą dla danego numeru. Zdalna administracja używa szyfrowania jak DM; węzeł przyjmuje polecenia od maksymalnie 3 kluczy administratora, a każda zmiana wymaga hasła sesji wydanego w ostatnich 5 minutach. Stary kanał admin działa tylko, gdy jest jawnie włączony. Zalecenia: nie trzymaj prywatnych kanałów na węzłach bez nadzoru (ktoś może je zabrać i odczytać), okresowo zmieniaj klucze kanałów, weryfikuj klucz publiczny odbiorcy innym kanałem przed wrażliwą rozmową i rób kopię kluczy przed wymazaniem węzła.
Słowa: numer węzła, CRC-32, klucz admina, zdalna administracja, admin key, kopia kluczy, rotacja kluczy, węzeł bez nadzoru
Źródło: https://meshtastic.org/docs/overview/encryption/ (sprawdzono 2026-10)

### Jak węzły decydują, kto powtarza pakiet (opóźnienie wg SNR)?
Przy zalewaniu węzeł przed powtórzeniem chwilę słucha i rezygnuje, jeśli usłyszy, że ktoś już powtórzył. Okno oczekiwania zależy od SNR odebranego pakietu: niski SNR (daleki węzeł) daje krótsze okno, więc dalsze węzły powtarzają pierwsze, a bliższe mogą odpuścić. ROUTER i ROUTER_LATE powtarzają mimo usłyszenia innych. Przed nadaniem węzeł sprawdza zajętość kanału (CAD) i czeka losową liczbę szczelin, a okno rośnie z zajętością kanału. Hop limit i hop start to po 3 bity w nagłówku. Nagłówek ma 16 bajtów, ładunek do 239 bajtów (z 12 bajtami dodatkowymi przy szyfrowaniu PKC lub AEAD); większe pakiety nie są wysyłane.
Słowa: zalewanie zarządzane, managed flooding, SNR, opóźnienie, kto powtarza, CAD, CSMA, rozmiar pakietu, 239 bajtów, nagłówek, hop start
Źródło: https://meshtastic.org/docs/overview/mesh-algo/ (sprawdzono 2026-10)

### Potwierdzenia, powtórzenia i automatyczne wydłużanie interwałów
Wiadomości z prośbą o potwierdzenie (WantAck): broadcast wysyłany jest najwyżej 3 razy, DM najwyżej 5 razy. Dla broadcastu potwierdzeniem jest usłyszenie, że ktoś go powtórzył; dla DM odbiorca odsyła prawdziwe ACK. Bez potwierdzenia po ostatniej próbie powstaje wewnętrzne NAK. NodeInfo domyślnie co 3 godziny; pozycja i telemetria po włączeniu domyślnie co godzinę (12 godzin dla ROUTER i ROUTER_LATE). W sieciach z ponad 40 węzłami widzianymi w ostatnich 2 godzinach interwały pozycji i telemetrii same się wydłużają: Interwał × (1 + (liczba węzłów − 40) × 2^SF / (BW_kHz × 100)). Przykład: 62 węzły na LongFast zmieniają godzinę w ok. 168 minut. Nie dotyczy ról ROUTER, ROUTER_LATE, SENSOR, TRACKER i TAK_TRACKER.
Słowa: ACK, NAK, potwierdzenie, ile razy powtarza, 5 prób, skalowanie interwałów, 40 węzłów, wzór, 168 minut, telemetria co godzinę
Źródło: https://meshtastic.org/docs/overview/mesh-algo/ (sprawdzono 2026-10)

### Co znaczą ikonki przy wysłanej wiadomości (oryginalna aplikacja Android)?
Według dokumentacji oryginalnej aplikacji: chmurka ze strzałką w górę to wiadomość w kolejce w aplikacji; chmurka to wysłana przez LoRa; chmurka z ptaszkiem to potwierdzenie od co najmniej jednego węzła; ludzik z ptaszkiem to potwierdzenie od adresata wiadomości prywatnej; przekreślona chmurka to brak potwierdzenia przed upływem czasu. Wiadomości nie są domyślnie przechowywane i przesyłane później, więc to, czego odbiorca nie usłyszał, przepada (chyba że działa Store & Forward). W naszej aplikacji MT_SW wygląd może się różnić; opis aplikacji jest w jej dokumentacji.
Słowa: ikony wiadomości, chmurka, ptaszek, potwierdzenie, doręczona, nie doszła, przekreślona chmurka, status wiadomości, kolejka
Źródło: https://meshtastic.org/docs/software/android/usage/ (sprawdzono 2026-10; dotyczy oryginalnej aplikacji)

### Kanały, QR i typowe problemy w aplikacji
Kanał domyślny LongFast widzi każdy w pobliżu. Prywatny kanał tworzy się w zakładce kanałów (najpierw odblokuj kłódkę). Zmiana kanału zrywa łączność z każdym, kto nie ma nowych ustawień, więc najpierw udostępnij kod QR lub link. Przyjęcie kanału z QR zastępuje twoje obecne kanały i ustawienia radia. Nie da się sparować? Sprawdź PIN: urządzenie z ekranem może mieć losowy; reset fabryczny przywraca 123456. Węzeł "śpi"? Jest poza zasięgiem albo w trybie uśpienia. Mapa pozwala pobrać obszar do pracy bez internetu.
Słowa: kanał, QR, link kanału, zmiana kanału, prywatny kanał, LongFast, parowanie, PIN, śpi, mapa offline, problem z aplikacją
Źródło: https://meshtastic.org/docs/software/android/usage/ (sprawdzono 2026-10; dotyczy oryginalnej aplikacji)
