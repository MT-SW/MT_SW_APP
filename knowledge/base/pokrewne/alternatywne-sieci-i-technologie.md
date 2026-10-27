# Alternatywne sieci i technologie radiowe (informacyjnie)

### Czym jest MeshCore i czym różni się od Meshtastic?
MeshCore to otwartoźródłowy (licencja MIT) protokół i oprogramowanie do sieci kratowych na LoRa, stworzone w 2024 przez Scotta Powella; klienty www i mobilne rozwija Liam Cottle. Urządzenia mają role: Companion (urządzenie użytkownika, łączone z aplikacją), Repeater (jedyny typ węzła, który przekazuje dalej pakiety) i Room Server (serwer pokoju). Kanały publiczne używają rozsyłania lawinowego, a poza tym MeshCore opiera się na wyznaczonych trasach i przechowywaniu wiadomości. Według opisu ogranicza to zużycie pasma i kolizje w gęstym ruchu, ale też zasięg wielu skoków. Meshtastic częściej rozsyła pakiety lawinowo przez wiele węzłów. Chodzi o inną filozofię, nie o „lepsze” i „gorsze”. Minusy według źródła: mała przepustowość, wrażliwość na teren i antenę, rozproszona dokumentacja. Sprzęt: płytki ESP32 z LoRa (np. Heltec, LilyGo), pasmo 868 MHz w Europie. MeshCore i Meshtastic nie rozmawiają ze sobą, to osobne sieci.
Słowa: MeshCore, Meshtastic różnice, repeater, companion, room server, alternatywa, flooding, routing, Liam Cottle
Źródło: https://en.wikipedia.org/wiki/MeshCore (sprawdzono 2026-10)

### Czym jest Reticulum i czy to to samo co Meshtastic?
Reticulum to stos sieciowy działający w Pythonie 3, niezależny od IP (IP może być tylko jednym z nośników). Ma adresy i tożsamości bez centralnej koordynacji, samokonfigurujący się routing wielu skoków przez różne media, anonimowość inicjatora (pakiety nie niosą adresu nadawcy), szyfrowane łącza z utajnieniem w przód i potwierdzenia dostarczenia. Nośniki: LoRa przez radio RNode (do zbudowania samemu lub kupienia), Wi-Fi/Ethernet po TCP i UDP, porty szeregowe i modemy KISS/TNC. Szyfrowanie: Ed25519 i X25519, AES-256-CBC, HMAC-SHA256. Instalacja: pip install rns. Wiadomości niesie protokół LXMF, a aplikacja Sideband (Android, Linux, macOS, Windows) obsługuje pliki, obrazy, głos i mapy. To zupełnie inny system niż Meshtastic i nie jest z nim zgodny; porównania ze źródła nie ma.
Słowa: Reticulum, RNode, Sideband, LXMF, rnsd, rns, mesh bez IP, alternatywa dla Meshtastic
Źródło: https://github.com/markqvist/Reticulum (sprawdzono 2026-10)

### Czym jest MeshCore i czym różni się od Meshtastic?
MeshCore to lekka biblioteka w C++ do wieloskokowego trasowania pakietów w projektach wbudowanych na LoRa i innych radiach pakietowych, do sieci działających bez internetu. Strona projektu opisuje Meshtastic jako dostosowany do luźnej łączności LoRa, a MeshCore jako nastawiony na lekkie trasowanie wieloskokowe w projektach wbudowanych. Licencja MIT. Liczbę przeskoków można konfigurować, strona nie podaje wartości domyślnej ani maksymalnej.
Słowa: MeshCore, biblioteka, porównanie z Meshtastic, licencja MIT, trasowanie wieloskokowe, Reticulum, bez internetu
Źródło: https://github.com/meshcore-dev/MeshCore (sprawdzono 2026-10; porównania pochodzą ze strony projektu)

### Jakie role ma firmware MeshCore?
Companion łączy się z aplikacją przez BLE, USB lub Wi-Fi i nie przekazuje cudzych wiadomości, co według projektu zapobiega złym ścieżkom trasowania. Repeater przekazuje wiadomości dalej i konfiguruje się przez USB (narzędzie WWW) lub zdalnie przez LoRa z aplikacji mobilnej. Room Server to prosty serwer w stylu BBS do wspólnych wpisów. Są też przykłady: modem KISS, Simple Secure Chat i Simple Sensor (telemetria i alarmy).
Słowa: Companion, Repeater, Room Server, BBS, KISS, role MeshCore, zdalne zarządzanie, przekaźnik
Źródło: https://github.com/meshcore-dev/MeshCore (sprawdzono 2026-10)

### Na jakim sprzęcie i z jakimi aplikacjami działa MeshCore?
Wspiera urządzenia LoRa, m.in. Heltec i RAK Wireless; aktualna lista i gotowe wersje Companion, Repeater i Room Server są w flasherze meshcore.io/flasher. Klienty: WWW, Android, iOS oraz biblioteki meshcore.js (Node.js) i meshcore-cli (Python). Strona nie wyjaśnia dokładnie, jak wybierane są ścieżki (flood czy direct); w planach wymienia ręczne ścieżki w obie strony i specyfikację V2 ze skrótami ścieżek.
Słowa: flasher MeshCore, Heltec, RAK, meshcore.js, meshcore-cli, klient, aplikacja MeshCore, ścieżki
Źródło: https://github.com/meshcore-dev/MeshCore (sprawdzono 2026-10)

### Czym LoRaWAN różni się od LoRa i od Meshtastic?
LoRa to warstwa fizyczna (radio), a LoRaWAN to protokół i architektura sieci, która na niej działa. Urządzenia końcowe (czujniki na baterii) wysyłają krótkie pakiety do bramek, które tylko je przekazują do serwera sieciowego; ten usuwa duplikaty, kieruje dane do aplikacji i zarządza kanałami, spreading factor i mocą (może zasugerować wyższą prędkość lub niższą moc, by oszczędzić baterię). Jedna ramka może dotrzeć do wielu bramek. Standardem zarządza LoRa Alliance; LoRę opracowała firma Cycleo, przejęta przez Semtech w 2012. To architektura gwiazdy z bramkami i serwerem, a Meshtastic to sieć kratowa (mesh), w której węzły same przekazują pakiety, bez bramek i serwera. Źródło nie opisuje klas urządzeń, planu kanałów EU868 ani kluczy.
Słowa: LoRaWAN, brama, gateway, serwer sieciowy, LoRa Alliance, Semtech, czujniki IoT, gwiazda, mesh, różnice, TTN
Źródło: https://en.wikipedia.org/wiki/LoRaWAN (sprawdzono 2026-10; porównanie z Meshtastic to wniosek z opisu architektury)

### APRS: co to jest i czym różni się od Meshtastic?
APRS to amatorski system wymiany informacji lokalnych w czasie rzeczywistym (pozycje GPS, pogoda, krótkie wiadomości), stworzony pod koniec lat 80. przez Boba Bruningę (WB4APR). Działa zwykle 1200 bit/s (Bell 202 AFSK) na paśmie 2 m, w Europie na 144,800 MHz. Digipeatery przekazują pakiety dalej (ścieżka WIDE1-1,WIDE2-1 dla stacji ruchomych), a bramki iGate łączą radio z internetową siecią APRS-IS. Pakiety to nieszyfrowane, niepotwierdzane ramki AX.25: niezawodność bierze się z powtórzeń, a nie z ponowień. Źródło mówi, że jest to system oparty na łączności amatorskiej i działa na pasmach amatorskich.
Słowa: APRS, digipeater, iGate, 144,800 MHz, AX.25, krótkofalowcy, pozycja GPS, APRS-IS, WIDE1-1, LoRa APRS
Źródło: https://en.wikipedia.org/wiki/Automatic_Packet_Reporting_System (sprawdzono 2026-10; źródło nie wspomina LoRa APRS)
