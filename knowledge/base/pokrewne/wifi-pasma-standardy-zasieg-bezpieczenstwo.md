# Wi-Fi: pasma, standardy, zasięg, zabezpieczenia

### Pasma i kanały Wi-Fi: 2,4, 5 i 6 GHz
2,4 GHz jest najpopularniejsze, ale zatłoczone i podatne na zakłócenia. 5 GHz ma więcej niezachodzących kanałów i większe prędkości, lecz mniejszy zasięg. 6 GHz zaczęło się od Wi-Fi 6E. W 2,4 GHz numery kanałów dzielą 5 MHz, a sygnał zajmuje około 20 MHz, więc w Ameryce Płn. rozłączne są tylko 1, 6 i 11 (Europa i Japonia dopuszczają więcej kanałów). W 5 GHz na świecie jest co najmniej 23 rozłączne kanały po 20 MHz, które można łączyć do 40, 80 i 160 MHz. 2,4 GHz dzielą z Wi-Fi: mikrofalówki, telefony bezprzewodowe, Bluetooth, huby USB 3.0, niania elektroniczna, kamery i Zigbee. W 5 GHz część kanałów zajmuje radar, dlatego access pointy stosują DFS.
Słowa: Wi-Fi, 2.4 GHz, 5 GHz, 6 GHz, kanały, 1 6 11, DFS, zakłócenia, mikrofalówka, router
Źródło: https://en.wikipedia.org/wiki/Wi-Fi (sprawdzono 2026-10)

### Nazwy standardów Wi-Fi i zasięg
Wi-Fi 4 to 802.11n (2009, 2,4 i 5 GHz), Wi-Fi 5 to 802.11ac (2013, tylko 5 GHz), Wi-Fi 6 to 802.11ax (2021, 2,4 i 5 GHz), Wi-Fi 6E to Wi-Fi 6 w paśmie 6 GHz, Wi-Fi 7 to 802.11be (2024, 2,4/5/6 GHz); Wi-Fi 8 (802.11bn) nie jest jeszcze ukończone. Punkt dostępu sięga typowo około 20 m w pomieszczeniu, niektóre modele zewnętrzne deklarują do 150 m. Niższa częstotliwość daje większy zasięg, ale mniejszą przepustowość; wyższe są silniej tłumione przez ściany. Zasięg zmniejszają metal (zbrojenie, szkło low-e), skała i woda; najlepiej działa linia widoczności.
Słowa: Wi-Fi 6, Wi-Fi 7, 802.11ax, 802.11ac, standard, zasięg wifi, ściany, zbrojenie
Źródło: https://en.wikipedia.org/wiki/Wi-Fi (sprawdzono 2026-10)

### Zabezpieczenia Wi-Fi: WEP, WPA2, WPA3
WEP jest uznany za złamany. WPA to wczesny, dziś przestarzały następca. WPA2 używa AES i jest uważany za bezpieczny przy silnym haśle, choć w 2017 wykryto lukę KRACK, a WPS może omijać zabezpieczenie, więc go wyłącz. WPA3 ogłoszono w 2018 jako następcę z mocniejszą ochroną. W sieci otwartej pomaga VPN lub HTTPS. Węzły Meshtastic łączą się z Wi-Fi w 2,4 GHz (ESP32), więc przy konfiguracji sieci domowej pamiętaj o paśmie 2,4 GHz (to uwaga z ogólnej wiedzy o ESP32, nie ze źródła).
Słowa: WPA2, WPA3, WEP, KRACK, WPS, hasło wifi, VPN, otwarta sieć, bezpieczeństwo wifi
Źródło: https://en.wikipedia.org/wiki/Wi-Fi (sprawdzono 2026-10)
