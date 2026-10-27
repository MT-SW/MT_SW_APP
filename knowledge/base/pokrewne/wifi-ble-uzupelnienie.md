# Wi-Fi i Bluetooth LE: uzupełnienie

### Wi-Fi: pasma, kanały i bezpieczeństwo
2,4 GHz ma największy zasięg, ale jest zatłoczone (mikrofalówki, Bluetooth); 5 GHz jest szybsze i ma więcej kanałów, ale gorzej przenika ściany; 6 GHz dotyczy Wi-Fi 6E i 7. Na 2,4 GHz kanały mają po ok. 20 MHz, więc nie nakładają się tylko 1, 6 i 11 (w Europie dostępne też 12 i 13). Zasięg w domu to ok. 20 m. Bezpieczeństwo: WEP jest złamany, WPA2 (AES) z mocnym hasłem uchodzi za bezpieczny, WPA3 jest następcą; wyłącz WPS, a ukrywanie SSID i filtr MAC dają niewiele. W miejscach publicznych używaj VPN i HTTPS.
Słowa: Wi-Fi, 2,4 GHz, 5 GHz, kanały 1 6 11, WPA2, WPA3, WPS, router, zasięg, interferencje, Wi-Fi 6
Źródło: https://en.wikipedia.org/wiki/Wi-Fi (sprawdzono 2026-10)

### Bluetooth Low Energy: zasięg, pobór prądu i GATT
BLE działa na 2,4 GHz, 40 kanałów po 2 MHz, 1 Mbit/s (2 Mbit/s opcjonalnie w BT 5). Nominalny zasięg do ok. 100 m; Bluetooth 5 może go czterokrotnie zwiększyć (tryb coded PHY). Pobiera ok. 0,01–0,5 W (klasyczny ok. 1 W). Beacon może pracować 1–2 lata na ogniwku 1000 mAh, ale ciągłe skanowanie wyczerpuje je w kilka godzin. Dane są ułożone w usługi, charakterystyki i deskryptory (GATT), klient czyta, zapisuje i dostaje powiadomienia od serwera; dla większych danych używa się odczytu długiego. Aplikacja Meshtastic łączy się z węzłem po BLE właśnie tak, ale szczegółów protokołu Meshtastic ten artykuł nie podaje.
Słowa: BLE, Bluetooth Low Energy, GATT, charakterystyka, zasięg Bluetooth, pobór energii, MTU, Bluetooth 5, parowanie, beacon
Źródło: https://en.wikipedia.org/wiki/Bluetooth_Low_Energy (sprawdzono 2026-10; ostatnie zdanie to wiedza ogólna)
