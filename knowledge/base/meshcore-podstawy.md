# MeshCore: podstawy

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
