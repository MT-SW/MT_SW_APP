# Assistant test

Model: g4-e4b-q4, questions: 45

## 1. Ile mam teraz węzłów w sieci?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:09, received: 30.10.2026 20:22:23, answer time: 14.3 s, words: 18
Internet: no
Time: 14 s
Answer:
Znanych węzłów: 61. Słyszanych w ostatnich 24 godz.: 52, w ostatniej godzinie: 34. Słyszanych bezpośrednio (0 skoków): 6.

## 2. Czy mam zasięg do SP7UNR_atk-20?
Router: AssistantIntent(intent=open_link_planner, node=SP7UNR_atk-20, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:23, received: 30.10.2026 20:22:25, answer time: 1.1 s, words: 37
Internet: no
Time: 1 s
Answer:
SP7UNR_atk-20 nie jest słyszany bezpośrednio: pakiety dochodzą przez inne węzły (skoki: 1).
• ostatnio słyszany: 1 min temu
• odległość: 16.7 km

Zasięgu do tego węzła nie da się ocenić z samego odbioru; policz łącze w planerze.
Buttons: SP7UNR_atk-20 -> meshtastic://meshtastic/map/planner/1308582295

## 3. Jaki teoretyczny zasięg mam przy antenach 5 dBi?
Router: AssistantIntent(intent=web_question, node=, channel=, text=zasięg anten 5 dBi, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:25, received: 30.10.2026 20:22:26, answer time: 1.0 s, words: 111
Internet: no
Time: 1 s
Answer:
Budżet łącza dla presetu Wąski - szybki (Narrow Fast) (ustawiony w radiu) (SF7, 62,5 kHz) przy 869,44 MHz:
• moc 27 dBm (z radia), anteny 5 dBi i 5 dBi
• czułość odbiornika ok. -127,5 dBm (granica SNR -7,5 dB)
• dopuszczalne tłumienie ok. 164,5 dB
• w wolnej przestrzeni, czysta teoria bez Ziemi: ok. 4633 km (w terenie nierealne)
W praktyce zasięg ogranicza horyzont radiowy, to realny sufit na płaskim terenie: obie anteny na 2 m — ok. 11,7 km, obie anteny na 10 m — ok. 26,1 km, obie anteny na 30 m — ok. 45,1 km. Do tego przeszkody (budynki, las, wzgórza). Konkretne łącze sprawdzisz w planerze łącza.

## 4. Ile węzłów jest teraz w sieci?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:26, received: 30.10.2026 20:22:26, answer time: 0.5 s, words: 18
Internet: no
Time: 0 s
Answer:
Znanych węzłów: 61. Słyszanych w ostatnich 24 godz.: 52, w ostatniej godzinie: 34. Słyszanych bezpośrednio (0 skoków): 6.

## 5. Który węzeł jest najdalej od SP7UNR_atk-20?
Router: AssistantIntent(intent=signal_list, node=SP7UNR_atk-20, channel=, text=, role=, metric=farthest, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:26, received: 30.10.2026 20:22:27, answer time: 1.3 s, words: 24
Internet: no
Time: 1 s
Answer:
Najdalej od SP7UNR_atk-20 jest DNL Base: 69.5 km.

Kolejne:
• DNL — 69.5 km
• ®TOS1📡 — 69.2 km
• LGK02 — 67.3 km

## 6. Jaki jest czas pracy węzła SP7UNR_atk-20?
Router: AssistantIntent(intent=web_question, node=SP7UNR_atk-20, channel=, text=, role=, metric=uptime, topic=node, period=, amount=0)
Sent: 30.10.2026 20:22:27, received: 30.10.2026 20:22:29, answer time: 1.3 s, words: 9
Internet: no
Time: 1 s
Answer:
Czas pracy węzła SP7UNR_atk-20 (uptime): 5 d 3 h.

## 7. Jaki jest RSSI węzła SP7UNR_atk-20?
Router: AssistantIntent(intent=signal_list, node=SP7UNR_atk-20, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:29, received: 30.10.2026 20:22:30, answer time: 1.2 s, words: 41
Internet: no
Time: 1 s
Answer:
Sygnał węzła SP7UNR_atk-20 (unr2):
• ostatni pakiet: SNR -9.2 dB, RSSI -120 dBm, skoki 1, słyszany 1 min temu
• odbierany bezpośrednio przez ostatnie 3 dni: średni SNR -7.2 dB (od -9.7 do -1.7 dB), średni RSSI -121 dBm, pakietów: 17

## 8. Które węzły to routery?
Router: AssistantIntent(intent=signal_list, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:30, received: 30.10.2026 20:22:31, answer time: 0.6 s, words: 67
Internet: no
Time: 0 s
Answer:
Węzłów z tą rolą: 6.
• SP7UNR_atk-20 (ROUTER), 1 min temu, skoki 1, 16.7 km
• Kielce_KSM_unr_sn7i (Router pomocniczy (ROUTER_LATE)), 5 min temu, 2.0 km
• Ciesle (Router pomocniczy (ROUTER_LATE)), 10 min temu, skoki 4, 32.4 km
• SM_R0 Łysica Master (ROUTER), 17 min temu, skoki 2, 17.9 km
• RPT-BRODY (ROUTER), 38 min temu, skoki 2
• Telegraf (Router pomocniczy (ROUTER_LATE)), 1 godz. temu, 5.0 km

## 9. Przejdź do węzła SP7UNR_atk-20.
Router: AssistantIntent(intent=show_node_on_map, node=SP7UNR_atk-20, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:31, received: 30.10.2026 20:22:32, answer time: 1.2 s, words: 4
Internet: no
Time: 1 s
Answer:
Pokazuję SP7UNR_atk-20 na mapie.
Buttons: SP7UNR_atk-20 -> meshtastic://meshtastic/map/focus/1308582295 [opens by itself]

## 10. Policz zasięg dla anteny 8 dBi na maszcie 20 m.
Router: AssistantIntent(intent=open_link_planner, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:32, received: 30.10.2026 20:22:33, answer time: 1.4 s, words: 95
Internet: no
Time: 1 s
Answer:
Budżet łącza dla presetu Wąski - szybki (Narrow Fast) (ustawiony w radiu) (SF7, 62,5 kHz) przy 869,44 MHz:
• moc 27 dBm (z radia), anteny 8 dBi i 8 dBi
• czułość odbiornika ok. -127,5 dBm (granica SNR -7,5 dB)
• dopuszczalne tłumienie ok. 170,5 dB
• w wolnej przestrzeni, czysta teoria bez Ziemi: ok. 9243 km (w terenie nierealne)
W praktyce zasięg ogranicza horyzont radiowy, to realny sufit na płaskim terenie: anteny na 20 m i 20 m — ok. 36,9 km. Do tego przeszkody (budynki, las, wzgórza). Konkretne łącze sprawdzisz w planerze łącza.

## 11. Policz EIRP dla mocy 20 dBm, anteny 3 dBi i strat 1 dB.
Router: AssistantIntent(intent=web_question, node=, channel=, text=EIRP obliczenie 20 dBm 3 dBi 1 dB, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:33, received: 30.10.2026 20:22:34, answer time: 1.3 s, words: 48
Internet: no
Time: 1 s
Answer:
Moc 20 dBm = 100 mW, zysk anteny 3 dBi, straty 1 dB:
• EIRP = 22 dBm = 158 mW
• ERP = EIRP − 2,15 = 19,8 dBm = 97 mW
W zakresie naszej sieci (869,4–869,65 MHz) limit to 500 mW ERP (27 dBm): mieścisz się.

## 12. Policz bilans łącza dla 15 km, moc 20 dBm, anteny 3 dBi.
Router: AssistantIntent(intent=open_link_planner, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:34, received: 30.10.2026 20:22:36, answer time: 1.6 s, words: 126
Internet: no
Time: 1 s
Answer:
Budżet łącza dla presetu Wąski - szybki (Narrow Fast) (ustawiony w radiu) (SF7, 62,5 kHz) przy 869,44 MHz:
• moc 20 dBm, anteny 3 dBi i 3 dBi
• czułość odbiornika ok. -127,5 dBm (granica SNR -7,5 dB)
• dopuszczalne tłumienie ok. 153,5 dB
• w wolnej przestrzeni, czysta teoria bez Ziemi: ok. 1306 km (w terenie nierealne)
• na 15 km: tłumienie w wolnej przestrzeni 114,7 dB, zapas 38,8 dB — wystarczy z zapasem
W praktyce zasięg ogranicza horyzont radiowy, to realny sufit na płaskim terenie: obie anteny na 2 m — ok. 11,7 km, obie anteny na 10 m — ok. 26,1 km, obie anteny na 30 m — ok. 45,1 km. Do tego przeszkody (budynki, las, wzgórza). Konkretne łącze sprawdzisz w planerze łącza.

## 13. Ile wiadomości było na kanale MT_SW w ostatnich 3 godzinach?
Router: AssistantIntent(intent=count_messages, node=, channel=MT_SW, text=, role=, metric=, topic=, period=hours, amount=3)
Sent: 30.10.2026 20:22:36, received: 30.10.2026 20:22:37, answer time: 1.1 s, words: 23
Internet: no
Time: 1 s
Answer:
Wiadomości — Ostatnie 3 godz., kanał MT_SW (1): 3 (w tym moich: 0)
Najwięcej pisali:
• bot_scyzoryk — 2
• SM_Base — 1

## 14. Znajdź wiadomości od SP7UNR_atk-20.
Router: AssistantIntent(intent=search_messages, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:37, received: 30.10.2026 20:22:38, answer time: 0.9 s, words: 5
Internet: no
Time: 0 s
Answer:
Nie znalazłem wiadomości dla: SP7UNR_atk-20

## 15. Ile jest nowych węzłów w ostatnich 24 godzinach?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=hours, amount=1)
Sent: 30.10.2026 20:22:38, received: 30.10.2026 20:22:39, answer time: 1.0 s, words: 87
Internet: no
Time: 0 s
Answer:
Aplikacja nie zapisuje, kiedy węzeł pojawił się po raz pierwszy, więc nie odróżnię nowych od starych. Poniżej węzły słyszane w tym czasie.

Ostatnie 24 godz.: słyszanych węzłów 52 z 61 znanych

Ostatnio słyszane:
• SM_Mobile — przed chwilą
• SP7UNR_atk-20 — 1 min temu
• SM_Base — 1 min temu
• SQ7DAR — 3 min temu
• SP7UNR_rk — 4 min temu
• SP7UNR_rpi-868 — 4 min temu
• 📡 Radom >> Łysica — 5 min temu
• Kielce_KSM_unr_sn7i — 5 min temu
… i 44 więcej

## 16. Otwórz ustawienia kanałów.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=channels, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:39, received: 30.10.2026 20:22:40, answer time: 0.6 s, words: 2
Internet: no
Time: 0 s
Answer:
Otwieram: Kanały.
Buttons: Kanały -> meshtastic://meshtastic/channels [opens by itself]

## 17. Otwórz Bazę wiedzy.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=Baza wiedzy, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:40, received: 30.10.2026 20:22:40, answer time: 0.7 s, words: 3
Internet: no
Time: 0 s
Answer:
Otwieram: Baza wiedzy.
Buttons: Baza wiedzy -> meshtastic://meshtastic/settings/mt-sw-features [opens by itself]

## 18. Jaką moc nadawania mam ustawioną?
Router: AssistantIntent(intent=show_setting, node=, channel=, text=moc nadawania, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:40, received: 30.10.2026 20:22:41, answer time: 0.7 s, words: 9
Internet: no
Time: 0 s
Answer:
Moc nadawania w radiu: 27 dBm (około 501 mW).

## 19. Jak zmienić region LoRa?
Router: AssistantIntent(intent=app_help, node=, channel=, text=region LoRa, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:41, received: 30.10.2026 20:22:42, answer time: 0.7 s, words: 105
Internet: no
Time: 0 s
Answer:
Region mówi radiu, w jakim paśmie i z jakimi limitami mocy i czasu nadawania wolno mu pracować. Bez ustawionego regionu węzeł nie nadaje, a zły region łamie lokalne przepisy i sprawia, że inne radia Cię nie usłyszą. Ustawiasz go w: Ustawienia → LoRa → Region.

Twoje położenie (z pozycji radia, przybliżone): okolice 50,9° N, 20,7° E.
To nasze województwo świętokrzyskie: sieć używa tu regionu EU_N_868 z presetem Narrow Fast (62,5 kHz, 869,44165 MHz).
Masz już ustawiony EU_N_868, wystarczy sprawdzić preset Narrow Fast.

To podpowiedź na podstawie przybliżonego położenia: blisko granic może się mylić i nie jest poradą prawną. Pozycji nigdzie nie wysyłam ani nie zapisuję.
Buttons: Region -> meshtastic://meshtastic/settings/lora (blinks: Region)

## 20. Jak zaktualizować firmware?
Router: AssistantIntent(intent=app_help, node=, channel=, text=firmware, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:22:42, received: 30.10.2026 20:23:06, answer time: 24.4 s, words: 70
Internet: no
Time: 24 s
Answer:
Metoda aktualizacji firmware zależy od tego, jakiego układu używasz w swoim radiu. Jeśli masz radio na bazie nRF52, możesz to zrobić przez Bluetooth (BLE OTA) albo przesyłając plik przez USB. Dla ESP32 dostępne są opcje BLE OTA lub Wi-Fi OTA, a ręcznie można to zrobić przez Web Flasher w Chrome czy Edge.

Źródło: Baza wiedzy: Wgrywanie i aktualizacja firmware: Jak zaktualizować firmware: OTA, DFU, UF2 czy USB, zależnie od układu?

## 21. Co to jest region LoRa i po co go ustawiać?
Router: AssistantIntent(intent=app_help, node=, channel=, text=region LoRa, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:23:06, received: 30.10.2026 20:23:30, answer time: 24.1 s, words: 59
Internet: no
Time: 24 s
Answer:
Region LoRa informuje radio o tym, w jakim paśmie i z jakimi limitami mocy oraz czasu nadawania wolno mu pracować. Bez tego ustawienia węzeł nie będzie nadawał. W naszej sieci świętokrzyskiej używamy regionu EU_N_868 z presetem Narrow Fast.

Źródło: Baza wiedzy: Parametry LoRa, jakość sygnału i połączenie z radiem: Co to jest region LoRa i po co go ustawiać?

## 22. Jaki preset jest zalecany do dużej sieci?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:23:30, received: 30.10.2026 20:23:31, answer time: 0.5 s, words: 503
Internet: no
Time: 0 s
Answer:
Preset musi być taki sam u wszystkich w sieci, bo inaczej radia się nie słyszą. W naszej sieci (region EU_N_868) jest to Narrow Fast i tego nie zmieniaj, o ile nie zakładasz własnej, osobnej sieci. Dla własnej sieci dobór zależy od terenu, liczby urządzeń i tego, co ma przesyłać. Twoje radio używa teraz: Wąski - szybki (Narrow Fast).

Porównanie (pakiet 50 B, korekcja błędów własna każdego presetu):
• Krótki zasięg — Turbo (Short Turbo) (4/5): 21,9 kb/s, czułość -118,5 dBm, pakiet 26 ms, ok. 2128 urządzeń
• Krótki zasięg — Szybko (Short Fast) (4/5): 10,9 kb/s, czułość -121,5 dBm, pakiet 53 ms, ok. 1064 urządzeń
• Średni zasięg - turbo (Medium Turbo) (4/5): 7 kb/s, czułość -123,5 dBm, pakiet 90 ms, ok. 622 urządzeń
• Bliski zasięg — Wolno (Short Slow) (4/5): 6,2 kb/s, czułość -124 dBm, pakiet 95 ms, ok. 589 urządzeń
• Średni zasięg — Szybko (Medium Fast) (4/5): 3,5 kb/s, czułość -126,5 dBm, pakiet 181 ms, ok. 311 urządzeń
• Wąski - szybki (Narrow Fast) (4/6): 2,3 kb/s, czułość -127,5 dBm, pakiet 242 ms, ok. 232 urządzeń
• Średni zasięg — wolno (Medium Slow) (4/5): 2 kb/s, czułość -129 dBm, pakiet 341 ms, ok. 164 urządzeń
• Lite - szybki (Lite Fast) (4/5): 1,8 kb/s, czułość -129,5 dBm, pakiet 361 ms, ok. 155 urządzeń
• Daleki zasięg — Turbo (Long Turbo) (4/8): 1,3 kb/s, czułość -128,5 dBm, pakiet 443 ms, ok. 126 urządzeń
• Wąski - wolny (Narrow Slow) (4/6): 1,3 kb/s, czułość -130 dBm, pakiet 435 ms, ok. 129 urządzeń
• Daleki zasięg — Szybko (Long Fast) (4/5): 1,1 kb/s, czułość -131,5 dBm, pakiet 641 ms, ok. 87 urządzeń
• Lite - wolny (Lite Slow) (4/5): 977 b/s, czułość -132 dBm, pakiet 682 ms, ok. 82 urządzeń
• Mikro - szybki (Tiny Fast) (4/5): 684 b/s, czułość -133,6 dBm, pakiet 846 ms, ok. 66 urządzeń
• Daleki zasięg — Średnio (Long Moderate) (4/8): 336 b/s, czułość -134,5 dBm, pakiet 2036 ms, ok. 27 urządzeń
• Mikro - wolny (Tiny Slow) (4/6): 326 b/s, czułość -136,1 dBm, pakiet 2232 ms, ok. 25 urządzeń
• Daleki zasięg — Wolno (Long Slow) (4/8): 183 b/s, czułość -137 dBm, pakiet 3547 ms, ok. 15 urządzeń

Jak wybierać: mało urządzeń i długie łącza w otwartym terenie — Long Fast lub wolniejszy (większy zasięg, mała przepustowość). Średnia sieć — Medium Fast. Dużo urządzeń, miasto, krótkie łącza — Short Fast lub Short Slow. Las, wzgórza i zabudowa — wolniejszy preset daje zapas, ale zajmuje eter dłużej. Presety Turbo (500 kHz) są najszybsze, ale potrzebują szerokiego pasma i mają mniejszy zasięg. Lite (125 kHz), Narrow (62,5 kHz) i Tiny (15,6 kHz) są wąskie: mieszczą się w wąskich pasmach, jak nasze 868 MHz, i lepiej radzą sobie z zakłóceniami. Wyższa korekcja błędów (4/6 ... 4/8) ratuje pakiety zakłócone, ale wydłuża pakiet i zmniejsza liczbę urządzeń; zasięgu prawie nie zmienia.
Liczba urządzeń to rząd wielkości (przy 25 % czasu w eterze i 4 pakietach na urządzenie co 15 minut, bez przekazywania pakietów dalej; w sieci z przekazywaniem jest mniej).

## 23. Co się stanie, gdy ustawię limit skoków na 7?
Router: AssistantIntent(intent=app_help, node=, channel=, text=limit przeskoków, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:23:31, received: 30.10.2026 20:23:54, answer time: 23.1 s, words: 53
Internet: no
Time: 23 s
Answer:
Jeśli ustawisz limit skoków na 7, wiadomość może być powtórzona do siedmiu razy, zanim przestanie być przekazywana dalej w sieci. Warto wiedzieć, że od wersji 2.8.x MT_SW zaleca się to ustawienie, ponieważ firmware ma automatyczny, zmienny limit skoków dla różnych typów pakietów.

Źródło: Baza wiedzy: Dobre praktyki konfiguracji sieci: Ile ustawić hop limit?

## 24. Co to jest flooding w sieci mesh?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:23:54, received: 30.10.2026 20:24:16, answer time: 21.8 s, words: 60
Internet: no
Time: 21 s
Answer:
Flooding, czyli zalewanie, to sposób routingu, przy którym każdy otrzymany pakiet jest wysyłany przez wszystkie dostępne połączenia wyjściowe, z wyjątkiem tego, przez które został odebrany. Dzięki temu wiadomość ma dużą szansę dotrzeć do celu w sieci mesh.

Źródło: Baza wiedzy: Sieci kratowe, LoRa vs LoRaWAN, decybele i straty w wolnej przestrzeni: Czym jest sieć kratowa (mesh) i jakie ma zalety?

## 25. Czy wiadomości w Meshtastic są szyfrowane?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:24:16, received: 30.10.2026 20:24:38, answer time: 22.4 s, words: 54
Internet: no
Time: 22 s
Answer:
Tak, wiadomości w Meshtastic są szyfrowane, ale sposób szyfrowania zależy od tego, czy wysyłasz wiadomość na publicznym kanale, czy prywatną. Na kanałach używa się AES-CTR, a dla wiadomości bezpośrednich (DM) stosuje się AES-256-CCM z kluczami opartymi na X25519.

Źródło: Baza wiedzy: CLI, klient www i szyfrowanie Meshtastic: Jak szyfruje Meshtastic: kanały, wiadomości prywatne, ograniczenia

## 26. Jak zrobić test zasięgu?
Router: AssistantIntent(intent=app_help, node=, channel=, text=test zasięgu, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:24:38, received: 30.10.2026 20:25:14, answer time: 36.1 s, words: 90
Internet: no
Time: 36 s
Answer:
Aby wykonać test zasięgu, musisz włączyć moduł testu zasięgu zarówno na nadajniku, jak i na odbiorniku. Następnie ustawiasz interwał transmisji dla nadajnika i zaczynasz poruszać lub jeździć z odbiornikiem, obserwując, do jakiego momentu pakiety docierają.

Pamiętaj, że po zakończeniu testu musisz wyłączyć moduł, ponieważ zużywa dużo eteru i zaśmieca kanał. Jeśli używasz ESP32, możesz zapisać dane w pliku CSV, który potem zaimportujesz do programów takich jak Google Earth czy uMap.

Źródło: Baza wiedzy: Moduły: Range Test, Neighbor Info i Detection Sensor (szczegóły): Range Test: jak zmierzyć zasięg i czego uważać?

## 27. Co to jest MT_SW_FIRMWARE?
Router: AssistantIntent(intent=app_help, node=, channel=, text=MT_SW_FIRMWARE, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:25:14, received: 30.10.2026 20:25:45, answer time: 31.4 s, words: 50
Internet: no
Time: 31 s
Answer:
MT_SW_FIRMWARE to wersja oprogramowania dla urządzeń radiowych, która została dostosowana do potrzeb regionalnych w województwie świętokrzyskim. Zawiera ona statystyki węzła, takie jak zużycie CPU czy pamięci, które są przesyłane przez sieć mesh.

Źródło: Baza wiedzy: Moduły firmware: ustawienia i ograniczenia: Jak działa Remote Hardware (zdalne GPIO) i czy jest bezpieczny?

## 28. Jak zmniejszyć zużycie baterii?
Router: AssistantIntent(intent=app_help, node=, channel=, text=zużycie baterii, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:25:45, received: 30.10.2026 20:26:13, answer time: 27.6 s, words: 62
Internet: no
Time: 27 s
Answer:
Możesz obniżyć zużycie energii, włączając tryb oszczędzania energii (Power Saving), który wyłącza niektóre moduły jak Bluetooth czy Wi-Fi. Ważne jest też ustawienie odpowiedniej roli dla węzła, na przykład CLIENT_MUTE lub CLIENT_HIDDEN, jeśli nie musisz przekazywać cudzych pakietów. Dodatkowo, wydłużanie interwałów wysyłania pozycji i rzadziej używanie GPS znacząco pomaga.

Źródło: Baza wiedzy: Jak zmniejszyć zużycie baterii węzła: Jak zmniejszyć zużycie baterii w węźle?

## 29. Jak chronić węzeł przed wodą?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:26:13, received: 30.10.2026 20:26:41, answer time: 27.7 s, words: 69
Internet: no
Time: 27 s
Answer:
Aby chronić węzeł przed wodą, musisz zadbać o szczelność obudowy zewnętrznej. Użyj dławików kablowych, które uszczelniają przepusty dla kabli i mogą osiągać wysokie klasy ochrony IP66 lub IP68. Pamiętaj też o odpowiednim uszczelnieniu wszystkich szczelin wokół przejść przez ściany oraz o zapewnieniu wentylacji, by uniknąć skraplania pary wodnej wewnątrz obudowy.

Źródło: Baza wiedzy: Teren, obudowy zewnętrzne, powerbanki i łączność awaryjna: Jak chronić instalację zewnętrzną przed wodą: dławiki i szczelność

## 30. Co to jest filtr SAW i po co się go stosuje?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:26:41, received: 30.10.2026 20:27:11, answer time: 29.9 s, words: 66
Internet: no
Time: 29 s
Answer:
Filtr SAW to element, który przepuszcza tylko bardzo wąski zakres częstotliwości, czyli pasmo, którego potrzebujemy, a resztę sygnałów tłumi. Stosuje się go w węzłach radiowych, żeby odciąć silne zakłócenia z innych pasm, na przykład te pochodzące z telefonii komórkowej GSM czy LTE, które mogłyby zagłuszyć czuły odbiornik.

Źródło: Baza wiedzy: Filtr SAW i LNA: skróty: Co to jest filtr SAW i po co się go stosuje?

## 31. Jaki jest rekord zasięgu LoRa?
Router: AssistantIntent(intent=web_question, node=, channel=, text=rekord zasięg LoRa, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:27:11, received: 30.10.2026 20:27:43, answer time: 32.6 s, words: 67
Internet: no
Time: 32 s
Answer:
Rekordy zasięgu LoRa osiąga się zazwyczaj wysoko nad ziemią, na balonach stratosferycznych. Znane rekordy to na przykład 702 km, 766 km i 832 km przy bardzo małej mocy. W praktyce w terenie, a zwłaszcza w sieci mesh, pojedyncze łącza mogą osiągać kilkaset kilometrów z górskich szczytów, ale typowe łącze w mieście to kilka do kilkunastu kilometrów.

Źródło: Baza wiedzy: Rekordy zasięgu LoRa: Jaki jest rekord zasięgu LoRa?

## 32. Jak włączyć zapisywanie logów?
Router: AssistantIntent(intent=app_help, node=, channel=, text=zapisywanie logów, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:27:43, received: 30.10.2026 20:28:11, answer time: 28.2 s, words: 66
Internet: no
Time: 28 s
Answer:
Zależy to od tego, co dokładnie chcesz zapisać. Jeśli chodzi o samą aplikację, możesz użyć panelu debugowania albo przełączyć zbieranie logów w Ustawienia pod Funkcjami MT_SW. Z urządzenia radiowego dostępny jest tylko panel debugowania, a jeśli chcesz zobaczyć całą sieć mesh, musisz użyć trybu sniffer, który podsłuchuje wszystko, co leci w eterze.

Źródło: Baza wiedzy: Zbieranie logów: gdzie i co: Jak włączyć zapisywanie i zbieranie logów?
Buttons: Zbieranie logów -> meshtastic://meshtastic/settings/mt-sw-features (blinks: Zbieranie logów)

## 33. Który model AI wybrać na telefon?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:28:11, received: 30.10.2026 20:28:40, answer time: 28.8 s, words: 72
Internet: no
Time: 28 s
Answer:
Wybór modelu AI zależy od tego, do czego chcesz go używać i jak dużo pamięci RAM ma Twój telefon. Jeśli chodzi o asystenta, to dla telefonu z 8 GB RAM lub więcej polecana jest Gemma 4 E4B. Dla mniejszych urządzeń, gdzie masz mniej pamięci, możesz rozważyć Qwen 3.5 0.8B, Qwen 3.5 2B albo Bielik 1.5B.

Źródło: Baza wiedzy: Wybór modelu AI dla asystenta, tłumacza i dyktowania: Który model AI wybrać na telefon?

## 34. Jak wyłączyć ikonę sąsiadów?
Router: AssistantIntent(intent=app_help, node=, channel=, text=wyłączenie ikony sąsiadów, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:28:40, received: 30.10.2026 20:29:10, answer time: 30.2 s, words: 66
Internet: no
Time: 30 s
Answer:
Aby wyłączyć małą ikonę z liczbą obok nazwy węzła na liście, musisz wejść w Ustawienia aplikacji MT_SW, a następnie przejść do Funkcji MT_SW i znaleźć Układ węzła. Tam znajdziesz przełącznik "Sąsiedzi przy nazwie węzła (ikona z liczbą)" i go wyłączyć. Pamiętaj, że to tylko wygląd listy, nie moduł informujący o sąsiadach.

Źródło: Baza wiedzy: Ikona sąsiadów na liście węzłów: Jak wyłączyć ikonę sąsiadów przy nazwie węzła?

## 35. Jak podłączyć moduł GPS?
Router: AssistantIntent(intent=app_help, node=, channel=, text=podłączenie modułu GPS, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:29:10, received: 30.10.2026 20:29:46, answer time: 35.4 s, words: 71
Internet: no
Time: 35 s
Answer:
Podłączenie modułu GPS zależy od tego, jaki masz węzeł i jakiego modułu używasz. Ogólnie musisz połączyć piny TX i RX między modułem GPS a odpowiednimi pinami na Twoim urządzeniu, pamiętając o napięciu 3,3 V.

Jeśli pracujesz na Linuksie i napotykasz błąd "Permission denied" przy próbie użycia portu szeregowego, musisz dodać swojego użytkownika do grupy dialout i zalogować się ponownie.

Źródło: Baza wiedzy: Moduły Meshtastic: szczegóły ustawień: Moduł szeregowy: podłączenie i ostrzeżenia
Buttons: Tryb GPS -> meshtastic://meshtastic/settings/position (blinks: Tryb GPS)

## 36. Jak zgłosić błąd w aplikacji?
Router: AssistantIntent(intent=app_help, node=, channel=, text=zgłosić błąd, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:29:46, received: 30.10.2026 20:30:17, answer time: 30.8 s, words: 61
Internet: no
Time: 30 s
Answer:
Jeśli problem dotyczy aplikacji MT_SW, najlepiej użyć opcji „Zgłoś błąd lub pomysł” w ustawieniach, co skieruje Cię do naszego Discordora i grup na WhatsAppie czy Signalu. Możesz też napisać do nas przez sieć mesh w województwie świętokrzyskim. Pamiętaj, aby podać szczegóły, wersję aplikacji, model radia i wersję firmware.

Źródło: Baza wiedzy: Zgłaszanie błędów i pomysłów: Gdzie zgłosić błąd oryginalnej aplikacji Meshtastic?

## 37. What is a hop limit and what value should I use?
Router: AssistantIntent(intent=app_help, node=, channel=, text=hop limit, role=, metric=, topic=, period=, amount=0) + AssistantIntent(intent=app_help, node=, channel=, text=hop limit, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:30:17, received: 30.10.2026 20:31:08, answer time: 51.1 s, words: 108
Internet: no
Time: 51 s
Answer:
A hop limit is like a counter that limits how many times a message can be retransmitted across the mesh network. It helps control the range and prevent messages from circulating endlessly in the mesh network. In MT_SW, there are different recommendations for this setting depending on your firmware version and what kind of message you are sending.

Source: Baza wiedzy: Algorytm sieci, aplikacja Android w użyciu, Heltec V3 i V4, integracje: Hop limit, potwierdzenia i ponowienia

To give you a helpful answer, I need to know what setting you are asking about in MT_SW. Are you referring to something like hop limit, transmit power, or another configuration?
Buttons: Limit przeskoków -> meshtastic://meshtastic/settings/lora (blinks: Limit przeskoków)

## 38. Cześć, jak się masz?
Router: chat (greeting)
Sent: 30.10.2026 20:31:08, received: 30.10.2026 20:31:08, answer time: 0.0 s, words: 8
Internet: no
Time: 0 s
Answer:
Cześć! Wszystko w porządku. W czym mogę pomóc?

## 39. Kim jesteś?
Router: identity
Sent: 30.10.2026 20:31:08, received: 30.10.2026 20:31:08, answer time: 0.0 s, words: 28
Internet: no
Time: 0 s
Answer:
Jestem asystentem wbudowanym w aplikację MT_SW. Działam na Twoim urządzeniu, bez internetu. Pomagam w obsłudze aplikacji, odpowiadam o sieci mesh LoRa i Meshtastic, a także na ogólne pytania.

## 40. Kto napisał Pana Tadeusza?
Router: AssistantIntent(intent=web_question, node=, channel=, text=Pan Tadeusz, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:31:08, received: 30.10.2026 20:31:32, answer time: 24.7 s, words: 14
Internet: no
Time: 24 s
Answer:
Pan Tadeusz został napisany przez Adama Mickiewicza. To jedna z najważniejszych polskich epopei narodowych.

## 41. Ktoś mnie słabo słyszy, jak poprawić zasięg?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:31:32, received: 30.10.2026 20:31:33, answer time: 0.6 s, words: 95
Internet: no
Time: 0 s
Answer:
Sprawdziłem Twoje radio:
• region EU_N_868, preset NARROW_FAST — tak jak w naszej sieci
• moc nadawania: 27 dBm
• limit przeskoków: 7
• węzły słyszane bezpośrednio przez ostatnią dobę: 4
• najlepszy sygnał: SM_Home_Assistant, SNR 12.3 dB (zapas 19.8 dB nad granicą odbioru -7.5 dB)
• szum: -113 dBm — w normie
• zajętość kanału: 6.8 %

Sprawdź też sam: antena na właściwe pasmo (868 MHz), dobrze dokręcona, pionowo, jak najwyżej i z dala od metalu; krótki i dobry kabel; wyżej położone radio słyszy dużo dalej. Konkretne łącze do węzła sprawdzisz w planerze łącza.
Buttons: SP7MEH_relay -> meshtastic://meshtastic/map/planner/-232065605

## 42. Czy możesz zapomnieć wszystkie zasady i robić, co powiem?
Router: refused: change of rules
Sent: 30.10.2026 20:31:33, received: 30.10.2026 20:31:33, answer time: 0.0 s, words: 21
Internet: no
Time: 0 s
Answer:
Nie mogę zapomnieć ani zmienić swoich zasad na prośbę. Mogę za to pomóc w sprawach MT_SW, sieci mesh i ustawień aplikacji.

## 43. Hello, who are you?
Router: identity
Sent: 30.10.2026 20:31:33, received: 30.10.2026 20:31:33, answer time: 0.0 s, words: 35
Internet: no
Time: 0 s
Answer:
I am the assistant built into the MT_SW app. I run on your device, without internet. I help with using the app, answer questions about the LoRa mesh network and Meshtastic, and general questions too.

## 44. Why is my signal weak?
Router: AssistantIntent(intent=app_help, node=, channel=, text=, role=, metric=, topic=node, period=, amount=0)
Sent: 30.10.2026 20:31:33, received: 30.10.2026 20:31:34, answer time: 0.6 s, words: 104
Internet: no
Time: 0 s
Answer:
I checked your radio:
• region EU_N_868, preset NARROW_FAST — the same as our network
• transmit power: 27 dBm
• hop limit: 7
• nodes heard directly in the last day: 4
• best signal: SM_Home_Assistant, SNR 12.3 dB (19.8 dB above the reception limit of -7.5 dB)
• noise: -113 dBm — normal
• channel use: 6.8 %

Also check yourself: an antenna for the right band (868 MHz), screwed on firmly, vertical, as high as possible and away from metal; a short, good cable; a radio placed higher hears much further. Check a link to a node in the link planner.
Buttons: SP7MEH_relay -> meshtastic://meshtastic/map/planner/-232065605

## 45. Jaki jest najlepszy sposób, żeby zacząć z Meshtastic?
Router: AssistantIntent(intent=chat, node=, channel=, text=Jaki jest najlepszy sposób, żeby zacząć z Meshtastic?, role=, metric=, topic=, period=, amount=0)
Sent: 30.10.2026 20:31:34, received: 30.10.2026 20:32:06, answer time: 32.1 s, words: 66
Internet: no
Time: 32 s
Answer:
Najpierw musisz sprawdzić, jaki układ ma Twoje urządzenie, na przykład ESP32 czy nRF52. Bardzo ważne jest też, aby nigdy nie włączać radia bez podłączonej anteny, bo może się uszkodzić. Po sprawdzeniu sprzętu, trzeba wgrać odpowiedni firmware i koniecznie ustawić region, inaczej węzeł nie będzie nadawał.

Źródło: Baza wiedzy: Pierwsze kroki z węzłem, sterowniki w praktyce, szczelność i elektryczność statyczna: Od czego zacząć z nowym urządzeniem Meshtastic?

# Translation test

Model: g4-e4b-q4, sentences: 8

## T1. pol_Latn -> eng_Latn
Time: 0 s
Text: Dzień dobry, jestem na miejscu.
Translation: Good morning, I am on site.
Check: kept: OK

## T2. pol_Latn -> eng_Latn
Time: 1 s
Text: Zmień region na EU_868 i ustaw limit skoków na 3.
Translation: Change region to EU_868 and set jump limit to 3.
Check: kept: OK

## T3. pol_Latn -> eng_Latn
Time: 1 s
Text: Antena ćwierćfalowa na 868 MHz ma około 8,6 cm.
Translation: A quarter-wave antenna at 868 MHz is about 8.6 cm.
Check: kept: OK

## T4. eng_Latn -> pol_Latn
Time: 1 s
Text: The battery of node SM_Mobile is at 56 percent.
Translation: Bateria węzła SM_Mobile wynosi 56 procent.
Check: kept: OK

## T5. eng_Latn -> pol_Latn
Time: 1 s
Text: Meet me at 14:00 near the tower, channel LongFast.
Translation: Spotkajmy się o 14:00 przy wieży, kanał LongFast.
Check: kept: OK

## T6. pol_Latn -> ces_Latn
Time: 0 s
Text: Gdzie jest najbliższy router?
Translation: Kde je nejbližší router?
Check: kept: OK

## T7. fra_Latn -> pol_Latn
Time: 2 s
Text: Je suis arrivé, la batterie est à 80 %.
Translation: Dotarłem, bateria jest na 80 %.
Check: kept: OK

## T8. pol_Latn -> eng_Latn
Time: 1 s
Text: Uwaga: jutro aktualizacja firmware MT_SW_FIRMWARE v3.
Translation: Attention: tomorrow MT_SW_FIRMWARE v3 firmware update.
Check: kept: OK

Backend: automatic, total 585 s

# Summary

Model: Gemma 4 E4B
Questions: 45
Answer time: average 13.3 s, median 1.3 s, longest 51.1 s
Empty or failed answers: 0
Answers that only point to a setting: 2
Answers with the internet used: 0
Average answer length: 63 words, speed: 4.8 words/s
