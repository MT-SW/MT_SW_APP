# Jak działa sieć: routing, retransmisje, ACK, szyfrowanie

### Jak sieć przekazuje wiadomości (managed flooding)?
Węzeł retransmituje odebrany pakiet, dopóki HopLimit jest większy od zera, i zmniejsza go o 1. Przed retransmisją chwilę nasłuchuje: jeśli ktoś inny już go powtórzył, nie powtarza. ROUTER i ROUTER_LATE retransmitują mimo to, CLIENT_BASE tak samo dla pakietów od i do ulubionych węzłów. HopLimit i HopStart to pola 3-bitowe (stąd maksimum 7). Czas oczekiwania zależy od SNR: węzły dalsze (niskie SNR) mają krótsze okno i zwykle powtarzają pierwsze.
Słowa: routing, flooding, retransmisja, hop limit, HopStart, SNR, zalewanie, jak działa sieć
Źródło: https://meshtastic.org/docs/overview/mesh-algo/ (sprawdzono 2026-10)

### Jak działają potwierdzenia (ACK) i ponowienia?
Rozgłoszenie z WantAck nie dostaje zwykłego ACK: usłyszenie dowolnej retransmisji (z dowolnym kluczem) jest ACK niejawnym. Pewne rozgłoszenia: maksymalnie 3 nadania łącznie z pierwszym; pewne wiadomości bezpośrednie: do 5. Bez ACK/NAK nadawca sam generuje NAK. Od wersji 2.6 wiadomości bezpośrednie najpierw idą zalewaniem; węzeł, przez którego wróciła odpowiedź, staje się następnym przeskokiem (next-hop) i tylko on dalej przekazuje, a przy ostatnim ponowieniu wraca zalewanie.
Słowa: ACK, NAK, potwierdzenie, ponowienie, retry, next hop, wiadomość bezpośrednia, DM, 2.6
Źródło: https://meshtastic.org/docs/overview/mesh-algo/ (sprawdzono 2026-10)

### Jak węzeł unika kolizji (CSMA/CA)?
Przed nadaniem wykrywa aktywność na kanale (CAD) i czeka, gdy jest zajęty; po zwolnieniu czeka losową liczbę slotów z okna rywalizacji, a okno rośnie wraz z obciążeniem kanału (channel utilization).
Słowa: CSMA, CAD, kolizja, zajęty kanał, channel utilization, obciążenie, airtime
Źródło: https://meshtastic.org/docs/overview/mesh-algo/ (sprawdzono 2026-10)

### Jak często węzły nadają okresowo i jak to się skaluje z liczbą węzłów?
NodeInfo co 3 godziny. Telemetria (domyślnie wyłączona) co 1 godzinę, dla ROUTER i ROUTER_LATE co 12 godzin. Pozycja (tylko na kanałach z precyzją powyżej 0) co 1 godzinę, dla routerów co 12 godzin. Gdy w ciągu 2 godzin widziano ponad 40 węzłów, interwał rośnie: ScaledInterval = Interval × (1 + (N − 40) × 2^SF / (BW_kHz × 100)). Dla Long Fast każdy węzeł ponad 40 dodaje 0,08192; przy 62 węzłach interwał 1 h wydłuża się do ok. 168 minut. Role ROUTER, ROUTER_LATE, SENSOR, TRACKER i TAK_TRACKER nie są skalowane.
Słowa: interwał, skalowanie, 40 węzłów, nodeinfo, telemetria, pozycja, duża sieć, obciążenie kanału
Źródło: https://meshtastic.org/docs/overview/mesh-algo/ (sprawdzono 2026-10)

### Jak szyfrowane są wiadomości kanałowe i prywatne (DM)?
Kanał: AES-CTR kluczem kanału (16 B = AES-128, 32 B = AES-256); każdy z kluczem czyta wszystko, a klucz domyślny AQ== jest publiczny. Od 2.8.1 można włączyć AEAD (AES-CCM). Wiadomości bezpośrednie: AES-256-CCM z kluczem wyprowadzonym przez X25519 z klucza prywatnego nadawcy i publicznego odbiorcy; klucze publiczne węzły poznają z NodeInfo i nie wyślą DM do węzła bez klucza. Rozgłoszenia od 2.8 są podpisywane XEdDSA (66 bajtów; zbyt duże wysyłane bez podpisu), węzły STM32WL nie podpisują.
Słowa: szyfrowanie, AES, X25519, Curve25519, klucz publiczny, DM, wiadomość prywatna, XEdDSA, podpis, AEAD
Źródło: https://meshtastic.org/docs/overview/encryption/ (sprawdzono 2026-10)

### Co nie jest szyfrowane i jak zarządzać kluczami?
Nagłówek pakietu nie jest szyfrowany (przekaźniki muszą go czytać). Numer węzła to CRC-32 klucza publicznego, więc nowy klucz oznacza nową tożsamość: zrób kopię kluczy przed wymazaniem lub ponownym flashowaniem. Węzły ufają pierwszemu usłyszanemu kluczowi publicznemu danego numeru. Brak forward secrecy; kanały AES-CTR nie mają ochrony integralności. Wiadomości administracyjne używają szyfrowania jak DM, przyjmowane są tylko od maksymalnie trzech kluczy admina, z kluczem sesji ważnym 5 minut. Nie konfiguruj kanałów prywatnych na węzłach bez nadzoru; okresowo zmieniaj klucze kanałów.
Słowa: klucz admina, admin key, tożsamość węzła, CRC-32, kopia kluczy, bezpieczeństwo, passkey, forward secrecy
Źródło: https://meshtastic.org/docs/overview/encryption/ (sprawdzono 2026-10)
