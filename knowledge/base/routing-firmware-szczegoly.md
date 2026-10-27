# Routing od strony firmware: nagłówek, historia pakietów, opóźnienia

### Jak zbudowany jest nagłówek radiowy pakietu?
Offsety: 0x00 adresat (4 B, 0xFFFFFFFF = rozgłoszenie), 0x04 nadawca (4 B), 0x08 ID pakietu (4 B), 0x0C flagi (1 B: bity 0–2 HopLimit, bit 3 WantAck, bit 4 ViaMQTT, bity 5–7 HopStart), 0x0D hash kanału, 0x0E next-hop (młodszy bajt), 0x0F relay node (młodszy bajt), 0x10 zaszyfrowany SubPacket (protobuf).
Słowa: nagłówek, header, flagi, hop limit bity, relay_node, next_hop, ViaMQTT, pakiet
Źródło: https://wiki.mesh.szczecin.pl/routing-meshtastic-od-strony-firmware-0-lora-udp-role-i-strojenie/ (sprawdzono 2026-10)

### Jak węzeł ustala opóźnienie retransmisji?
Router lub klient z ulubionym węzłem: random(0, 2 × CWsize) × slotTime. Pozostałe węzły: (2 × CWmax × slotTime) + random(0, 2^CWsize) × slotTime, czyli dłużej. Im niższy SNR (węzeł dalej od nadawcy), tym mniejszy CWsize i krótsze opóźnienie. Węzeł może anulować własną retransmisję, jeśli usłyszy duplikat przed swoją kolejką. Artykuł nie podaje wartości slotTime, CWsize ani CWmax.
Słowa: opóźnienie, contention window, CWsize, slot time, SNR, anulowanie retransmisji, perhapsCancelDupe, PacketHistory
Źródło: https://wiki.mesh.szczecin.pl/routing-meshtastic-od-strony-firmware-0-lora-udp-role-i-strojenie/ (sprawdzono 2026-10)

### Jak działa trasowanie wiadomości bezpośrednich (next-hop) w szczegółach?
Pierwsza wiadomość bezpośrednia idzie zalewaniem; po odpowiedzi węzeł zapisuje next-hop w bazie węzłów. Fallback do zalewania następuje przy ostatnich retransmisjach (wg artykułu w wersji 2.6 po 3 próbach). Wiadomości z want_ack tworzą wpis oczekującego pakietu z licznikiem prób; ponowienia kończy ACK niejawny (usłyszenie własnego pakietu powtórzonego przez sieć) lub jawny ACK/NAK. Przy braku ACK dla DM: aktualizacja do 2.6+, reset bazy węzłów, mniejszy hop limit, ulubione.
Słowa: next hop, DM, brak ACK, wiadomość prywatna nie dochodzi, NodeDB, reset bazy węzłów, ReliableRouter
Źródło: https://wiki.mesh.szczecin.pl/routing-meshtastic-od-strony-firmware-0-lora-udp-role-i-strojenie/ (sprawdzono 2026-10)

### Jakie luki bezpieczeństwa (CVE) opisano w firmware?
Wg artykułu z wiki.mesh.szczecin.pl (daty i oceny CVSS tylko z artykułu, niezweryfikowane): CVE-2025-24798 (pakiet z want_response powodował awarię, DoS; wersje 1.2.1–2.6.1, załatane w 2.6.2); CVE-2025-24797 (przepełnienie bufora od uszkodzonych pakietów); CVE-2025-52464 (słaba entropia na niektórych platformach; zalecenie: aktualizacja do 2.6.3+ i regeneracja kluczy). Zalecenia: TLS w MQTT, rebroadcast_mode=KNOWN_ONLY, regularne aktualizacje. Routing nie chroni przed zagłuszaniem LoRa (jamming).
Słowa: CVE, bezpieczeństwo, luka, DoS, entropia, klucze, aktualizacja, jamming, zagłuszanie
Źródło: https://wiki.mesh.szczecin.pl/routing-meshtastic-od-strony-firmware-0-lora-udp-role-i-strojenie/ (sprawdzono 2026-10; wymaga potwierdzenia w oficjalnych komunikatach)
