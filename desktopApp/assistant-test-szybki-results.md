# Assistant test

Model: qwen35-9b-q4, questions: 45

## 1. Ile mam teraz węzłów w sieci?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 16 s
Answer:
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 49, w ostatniej godzinie: 30. Słyszanych bezpośrednio (0 skoków): 5.

## 2. Czy mam zasięg do SP7UNR_bm-node_NRF?
Router: AssistantIntent(intent=open_link_planner, node=SP7UNR_bm-node_NRF, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Otwieram planer łącza dla SP7UNR_bm-node_NRF.
Buttons: SP7UNR_bm-node_NRF -> meshtastic://meshtastic/map/planner/-413163166 [opens by itself]

## 3. Jaki teoretyczny zasięg mam przy antenach 5 dBi?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Budżet łącza dla presetu Narrow Fast (ustawiony w radiu) (SF7, 62,5 kHz) przy 869,44 MHz:
• moc 27 dBm (z radia), anteny 5 dBi i 5 dBi
• czułość odbiornika ok. -127,5 dBm (granica SNR -7,5 dB)
• dopuszczalne tłumienie ok. 164,5 dB
• zasięg teoretyczny w wolnej przestrzeni: ok. 4633 km
To górna granica przy pełnej widoczności anten. Na Ziemi zasięg ogranicza horyzont radiowy: obie anteny na 2 m — ok. 11,7 km, obie anteny na 10 m — ok. 26,1 km, obie anteny na 30 m — ok. 45,1 km. Do tego przeszkody (budynki, las, wzgórza). Konkretne łącze sprawdzisz w planerze łącza.

## 4. Ile węzłów jest teraz w sieci?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 49, w ostatniej godzinie: 30. Słyszanych bezpośrednio (0 skoków): 5.

## 5. Który węzeł jest najdalej od SP7UNR_bm-node_NRF?
Router: AssistantIntent(intent=signal_list, node=SP7UNR_bm-node_NRF, channel=, text=, role=, metric=farthest, topic=, period=, amount=0)
Time: 1 s
Answer:
Sygnał węzła SP7UNR_bm-node_NRF (unrA):
• ostatni pakiet: SNR brak, RSSI brak, skoki 2, słyszany przed chwilą
• przez ostatnie 3 dni brak pakietów odebranych od niego bezpośrednio (dociera przez inne węzły), więc nie ma średniej

## 6. Jaki jest uptime węzła SP7UNR_bm-node_NRF?
Router: AssistantIntent(intent=network_question, node=SP7UNR_bm-node_NRF, channel=, text=, role=, metric=uptime, topic=node, period=, amount=0)
Time: 19 s
Answer:
Uptime węzła SP7UNR_bm-node_NRF wynosi 7765 sekund, co odpowiada 2 godzinom i 9 minutom.

## 7. Jaki jest RSSI węzła SP7UNR_bm-node_NRF?
Router: AssistantIntent(intent=signal_list, node=SP7UNR_bm-node_NRF, channel=, text=, role=, metric=snr, topic=, period=, amount=0)
Time: 2 s
Answer:
Sygnał węzła SP7UNR_bm-node_NRF (unrA):
• ostatni pakiet: SNR brak, RSSI brak, skoki 2, słyszany 1 min temu
• przez ostatnie 3 dni brak pakietów odebranych od niego bezpośrednio (dociera przez inne węzły), więc nie ma średniej

## 8. Które węzły to routery?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=router, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 49, w ostatniej godzinie: 30. Słyszanych bezpośrednio (0 skoków): 5.

## 9. Przejdź do węzła SP7UNR_bm-node_NRF.
Router: AssistantIntent(intent=show_node_on_map, node=SP7UNR_bm-node_NRF, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Pokazuję SP7UNR_bm-node_NRF na mapie.
Buttons: SP7UNR_bm-node_NRF -> meshtastic://meshtastic/map/focus/-413163166 [opens by itself]

## 10. Policz zasięg dla anteny 8 dBi na maszcie 20 m.
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Budżet łącza dla presetu Narrow Fast (ustawiony w radiu) (SF7, 62,5 kHz) przy 869,44 MHz:
• moc 27 dBm (z radia), anteny 8 dBi i 8 dBi
• czułość odbiornika ok. -127,5 dBm (granica SNR -7,5 dB)
• dopuszczalne tłumienie ok. 170,5 dB
• zasięg teoretyczny w wolnej przestrzeni: ok. 9243 km
To górna granica przy pełnej widoczności anten. Na Ziemi zasięg ogranicza horyzont radiowy: anteny na 20 m i 20 m — ok. 36,9 km. Do tego przeszkody (budynki, las, wzgórza). Konkretne łącze sprawdzisz w planerze łącza.

## 11. Policz EIRP dla mocy 20 dBm, anteny 3 dBi i strat 1 dB.
Router: AssistantIntent(intent=web_question, node=, channel=, text=Policz EIRP 20 dBm 3 dBi 1 dB, role=, metric=, topic=, period=, amount=0)
Time: 2 s
Answer:
Moc 20 dBm = 100 mW, zysk anteny 3 dBi, straty 1 dB:
• EIRP = 22 dBm = 158 mW
• ERP = EIRP − 2,15 = 19,8 dBm = 97 mW
W zakresie naszej sieci (869,4–869,65 MHz) limit to 500 mW ERP (27 dBm): mieścisz się.

## 12. Policz bilans łącza dla 15 km, moc 20 dBm, anteny 3 dBi.
Router: AssistantIntent(intent=web_question, node=, channel=, text=bilans łącza 15 km 20 dBm 3 dBi, role=, metric=, topic=, period=, amount=0)
Time: 2 s
Answer:
Budżet łącza dla presetu Narrow Fast (ustawiony w radiu) (SF7, 62,5 kHz) przy 869,44 MHz:
• moc 20 dBm, anteny 3 dBi i 3 dBi
• czułość odbiornika ok. -127,5 dBm (granica SNR -7,5 dB)
• dopuszczalne tłumienie ok. 153,5 dB
• zasięg teoretyczny w wolnej przestrzeni: ok. 1306 km
• na 15 km: tłumienie w wolnej przestrzeni 114,7 dB, zapas 38,8 dB — wystarczy z zapasem
To górna granica przy pełnej widoczności anten. Na Ziemi zasięg ogranicza horyzont radiowy: obie anteny na 2 m — ok. 11,7 km, obie anteny na 10 m — ok. 26,1 km, obie anteny na 30 m — ok. 45,1 km. Do tego przeszkody (budynki, las, wzgórza). Konkretne łącze sprawdzisz w planerze łącza.

## 13. Ile wiadomości było na kanale MT_SW w ostatnich 3 godzinach?
Router: AssistantIntent(intent=count_messages, node=, channel=MT_SW, text=, role=, metric=, topic=, period=hours, amount=3)
Time: 1 s
Answer:
Wiadomości — Ostatnie 3 godz., kanał MT_SW (1): 0 (w tym moich: 0)

## 14. Znajdź wiadomości od SP7UNR_bm-node_NRF.
Router: AssistantIntent(intent=search_messages, node=, channel=, text=SP7UNR_bm-node_NRF, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Nie znalazłem wiadomości dla: SP7UNR_bm-node_NRF

## 15. Ile jest nowych węzłów w ostatnich 24 godzinach?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=hours, amount=24)
Time: 1 s
Answer:
Ostatnie 24 godz.: słyszanych węzłów 49 z 58 znanych

Ostatnio słyszane:
• SM_Mobile — przed chwilą
• SP7UNR_bm-node_NRF — 1 min temu
• SP7UNR_rpi-868 — 1 min temu
• SM_Base — 1 min temu
• SP7NL  — 2 min temu
• SN7I Dom — 5 min temu
• SM_R0 Łysica Master — 13 min temu
• SP7UNR_ — 14 min temu
… i 41 więcej

## 16. Otwórz ustawienia kanałów.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=channels, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Otwieram: Kanały.
Buttons: Kanały -> meshtastic://meshtastic/channels [opens by itself]

## 17. Otwórz Bazę wiedzy.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=Baza wiedzy, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Otwieram: Baza wiedzy.
Buttons: Baza wiedzy -> meshtastic://meshtastic/settings/mt-sw-features [opens by itself]

## 18. Jaką moc nadawania mam ustawioną?
Router: AssistantIntent(intent=show_setting, node=, channel=, text=moc nadawania, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Otwieram stronę z ustawieniem Nadawanie włączone. Mruga, żeby łatwo je znaleźć; zmianę robisz sam.
Buttons: Nadawanie włączone -> meshtastic://meshtastic/settings/lora (blinks: Nadawanie włączone) [opens by itself]

## 19. Jak zmienić region LoRa?
Router: AssistantIntent(intent=app_help, node=, channel=, text=region, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
„Region” jest w: Ustawienia → LoRa.
Buttons: Region -> meshtastic://meshtastic/settings/lora (blinks: Region)

## 20. Jak zaktualizować firmware?
Router: AssistantIntent(intent=app_help, node=, channel=, text=firmware, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
To jest na ekranie „Oprogramowanie”: przycisk poniżej go otwiera.

Z instrukcji aplikacji:
(Sprawdzanie aktualizacji)
1. Otwórz Ustawienia połączonego radia i w sekcji Zaawansowane dotknij Aktualizacja oprogramowania. Pozycja pojawia się tylko dla radiów obsługujących OTA.
2. Aplikacja sprawdza dostępne wersje firmware.
3. Dostępne aktualizacje pokazują numer wersji i informacje o wydaniu.
Buttons: Oprogramowanie -> meshtastic://meshtastic/firmware

## 21. Co to jest region LoRa i po co go ustawiać?
Router: AssistantIntent(intent=app_help, node=, channel=, text=region, role=, metric=, topic=, period=, amount=0)
Time: 102 s
Answer:
Region LoRa to ustawienie, które mówi radiu, w jakim paśmie częstotliwości ma pracować i jakie limity mocy oraz czasu nadawania obowiązują w danym kraju. Bez ustawienia regionu węzeł nie nadaje żadnych sygnałów, bo domyślnie jest on w stanie UNSET.

Główne zadania regionu to:
1. Wyznaczenie pasma częstotliwości, np. 869,4–869,65 MHz dla naszego regionu EU_N_868.
2. Określenie dozwolonej mocy nadawania i cyklu pracy, czyli jak często można nadawać, aby nie zakłócać innych użytkowników pasma. W naszym regionie limit to 10% czasu nadawania.
3. Dostarczenie listy dozwolonych presetów, czyli zestawów ustawień modemu, które są legalne w danym kraju.

Musisz go ustawić przed rozpoczęciem pracy w sieci, bo inaczej nie nawiążesz łączności z innymi węzłami. Jeśli region nie pasuje do lokalnych przepisów, firmware może go zmienić lub odrzucić, a węzeł może zacząć nadawać nielegalnie lub nie słyszeć nikogo, bo będzie używał presetu niedozwolonego w tym regionie.

W naszym projekcie MT_SW w województwie świętokrzyskim ustawiamy region EU_N_868, aby pracować na oficjalnej częstotliwości 869,44165 MHz z presetem Narrow Fast.

Źródło: https://meshtastic.org/docs/getting-started/initial-config/ (sprawdzono 2026-10)
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

## 22. Jaki preset jest zalecany do dużej sieci?
Router: AssistantIntent(intent=app_help, node=, channel=, text=preset duża sieć, role=, metric=, topic=, period=, amount=0)
Time: 2 s
Answer:
„Presety” jest w: Ustawienia → LoRa.
Buttons: Presety -> meshtastic://meshtastic/settings/lora (blinks: Presety)

## 23. Co się stanie, gdy ustawię limit skoków na 7?
Router: AssistantIntent(intent=app_help, node=, channel=, text=limit skoków, role=, metric=, topic=, period=, amount=0)
Time: 2 s
Answer:
„Limit przeskoków” jest w: Ustawienia → LoRa.
Buttons: Limit przeskoków -> meshtastic://meshtastic/settings/lora (blinks: Limit przeskoków)

## 24. Co to jest flooding w sieci mesh?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 183 s
Answer:
Flooding w sieci mesh to sposób przekazywania wiadomości, w którym każdy węzeł, który otrzyma pakiet pierwszy raz, wysyła go dalej do swoich sąsiadów. Dzięki temu wiadomość rozchodzi się jak fala po całej sieci, nawet jeśli nie znamy dokładnej trasy do odbiorcy.

W praktyce w Meshtastic i MT_SW wygląda to tak: gdy Twój węzeł odbiera wiadomość, sprawdza, czy nie jest to powtórzenie (czy nie usłyszał już tego samego pakietu od innego sąsiada). Jeśli to nowy pakiet, a nie przekroczyłeś limitu przeskoków, wysyłasz go dalej. To pozwala sieci samemu znaleźć najlepszą ścieżkę do celu, omijając uszkodzone węzły.

Jednak nieskrępowane flooding powoduje ogromną liczbę powtórzeń, co zapycha eter i blokuje innych użytkowników. Dlatego w Meshtastic stosuje się zarządzane zalewanie (managed flooding). Węzły najpierw chwilę nasłuchują, czy ktoś inny już nie powtórzył wiadomości, i jeśli tak, to milczą. Dodatkowo istnieje limit przeskoków (domyślnie 3, maksymalnie 7), który zapobiega rozchodzeniu się wiadomości w nieskończoność.

Z internetu:
Flooding to wielokrotne wysyłanie tej samej wiadomości przez węzły, które ją odebrały, aż dotrze ona do wszystkich połączonych urządzeń. Jest to podstawowy mechanizm w sieciach mesh, który nie wymaga mapowania tras, ale wymaga kontroli, aby uniknąć kolizji i nadmiernego obciążenia sieci. Źródło: https://www.offlineprotocol.com/learning/mesh-networking/what-is-flooding-in-a-mesh-network

Z internetu:
W sieciach komputerowych flooding to algorytm, w którym węzeł przekazuje przychodzący pakiet przez wszystkie swoje połączenia oprócz tego, z którego dotarł. Gwarantuje dostarczenie danych, ale może prowadzić do duplikatów i pętli, dlatego nowoczesne sieci stosują limity (TTL/hop) i mechanizmy zapobiegające nadmiarowemu wysyłaniu. Źródło: https://www.youstable.com/pl/blog/zalewanie-sieci-komputerowych/

Z internetu:
W kontekście sieci mesh flooding oznacza rozsyłanie wiadomości do wszystkich sąsiadów przez każdy węzeł, który ją odbierze. W Meshtastic maksymalna liczba przeskoków to 7, co jest znacznie mniejsze niż przykładowe 64 wspomniane w niektórych dyskusjach, co ogranicza zasięg rozsyłania. Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290

## 25. Czy wiadomości w Meshtastic są szyfrowane?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 140 s
Answer:
Tak, wiadomości w Meshtastic są szyfrowane, ale sposób tego szyfrowania zależy od tego, czy wysyłasz wiadomość grupową, czy prywatną.

Wiadomości grupowe (na kanałach) są szyfrowane za pomocą klucza wspólnego dla wszystkich użytkowników danego kanału. Oznacza to, że jeśli ktoś zna ten klucz, może odczytać treść wiadomości. Domyślny klucz jest publiczny, więc ruch na głównym kanale jest czytelny dla każdego. Od wersji firmware 2.8.1 można włączyć tryb AES-CCM, który dodatkowo wykrywa, czy wiadomość została zmieniona w trakcie przesyłania.

Wiadomości prywatne (DM) między dwoma konkretnymi węzłami są szyfrowane indywidualnie za pomocą algorytmu AES-256-CCM. Klucz do nich powstaje na podstawie kluczy publicznych i prywatnych obu stron. Węzeł nie wyśle wiadomości prywatnej, jeśli nie zna klucza publicznego odbiorcy.

Warto pamiętać, że nagłówek każdego pakietu radiowego nigdy nie jest szyfrowany, bo przekaźniki muszą go odczytać, aby wiedzieć, gdzie przekazać dane. Dodatkowo nie ma tu tak zwanego utajnienia w przód, co oznacza, że jeśli kiedyś wycieknie klucz szyfrujący, można będzie odszyfrować też stare wiadomości, które zostały nagrane.

MT_SW jako aplikacja dla województwa świętokrzyskiego stosuje te same standardy szyfrowania co główny projekt Meshtastic i nie wprowadza własnych algorytmów.

Źródło: https://meshtastic.org/docs/overview

## 26. Jak zrobić test zasięgu?
Router: AssistantIntent(intent=app_help, node=, channel=, text=test zasięgu, role=, metric=, topic=, period=, amount=0)
Time: 2 s
Answer:
„Presety” jest w: Ustawienia → LoRa.
Buttons: Presety -> meshtastic://meshtastic/settings/lora (blinks: Presety)
