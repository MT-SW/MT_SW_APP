# Assistant test

Model: qwen35-2b-q4, questions: 45

## 1. Ile mam teraz węzłów w sieci?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 5 s
Answer:
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 49, w ostatniej godzinie: 29. Słyszanych bezpośrednio (0 skoków): 5.

## 2. Czy mam zasięg do SQ7DAR5?
Router: AssistantIntent(intent=open_link_planner, node=SQ7DAR5, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Otwieram planer łącza dla SQ7DAR5.
Buttons: SQ7DAR5 -> meshtastic://meshtastic/map/planner/-470810681 [opens by itself]

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
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 49, w ostatniej godzinie: 29. Słyszanych bezpośrednio (0 skoków): 5.

## 5. Który węzeł jest najdalej od SQ7DAR5?
Router: AssistantIntent(intent=signal_list, node=SQ7DAR5, channel=, text=, role=, metric=farthest, topic=, period=, amount=0)
Time: 0 s
Answer:
Sygnał węzła SQ7DAR5 (DAR5):
• ostatni pakiet: SNR -7.0 dB, RSSI -117 dBm, skoki 0, słyszany 1 min temu
• odbierany bezpośrednio przez ostatnie 3 dni: średni SNR -4.2 dB (od -7.0 do -0.5 dB), średni RSSI -118 dBm, pakietów: 31

## 6. Jaki jest uptime węzła SQ7DAR5?
Router: AssistantIntent(intent=network_question, node=SQ7DAR5, channel=, text=, role=, metric=uptime, topic=node, period=, amount=0)
Time: 155 s
Answer:


## 7. Jaki jest RSSI węzła SQ7DAR5?
Router: AssistantIntent(intent=signal_list, node=SQ7DAR5, channel=, text=, role=, metric=snr, topic=, period=, amount=0)
Time: 6 s
Answer:
Sygnał węzła SQ7DAR5 (DAR5):
• ostatni pakiet: SNR -7.0 dB, RSSI -117 dBm, skoki 0, słyszany 4 min temu
• odbierany bezpośrednio przez ostatnie 3 dni: średni SNR -4.2 dB (od -7.0 do -0.5 dB), średni RSSI -118 dBm, pakietów: 31

## 8. Które węzły to routery?
Router: AssistantIntent(intent=network_question, node=, channel=, text=, role=, metric=rx, topic=ranking, period=, amount=0)
Time: 235 s
Answer:


## 9. Przejdź do węzła SQ7DAR5.
Router: AssistantIntent(intent=show_node_on_map, node=SQ7DAR5, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 5 s
Answer:
Pokazuję SQ7DAR5 na mapie.
Buttons: SQ7DAR5 -> meshtastic://meshtastic/map/focus/-470810681 [opens by itself]

## 10. Policz zasięg dla anteny 8 dBi na maszcie 20 m.
Router: AssistantIntent(intent=open_link_planner, node=20 m, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Budżet łącza dla presetu Narrow Fast (ustawiony w radiu) (SF7, 62,5 kHz) przy 869,44 MHz:
• moc 27 dBm (z radia), anteny 8 dBi i 8 dBi
• czułość odbiornika ok. -127,5 dBm (granica SNR -7,5 dB)
• dopuszczalne tłumienie ok. 170,5 dB
• zasięg teoretyczny w wolnej przestrzeni: ok. 9243 km
To górna granica przy pełnej widoczności anten. Na Ziemi zasięg ogranicza horyzont radiowy: anteny na 20 m i 20 m — ok. 36,9 km. Do tego przeszkody (budynki, las, wzgórza). Konkretne łącze sprawdzisz w planerze łącza.

## 11. Policz EIRP dla mocy 20 dBm, anteny 3 dBi i strat 1 dB.
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Moc 20 dBm = 100 mW, zysk anteny 3 dBi, straty 1 dB:
• EIRP = 22 dBm = 158 mW
• ERP = EIRP − 2,15 = 19,8 dBm = 97 mW
W zakresie naszej sieci (869,4–869,65 MHz) limit to 500 mW ERP (27 dBm): mieścisz się.

## 12. Policz bilans łącza dla 15 km, moc 20 dBm, anteny 3 dBi.
Router: AssistantIntent(intent=network_question, node=, channel=, text=15 km 20 dBm 3 dBi, role=, metric=, topic=radio, period=, amount=0)
Time: 1 s
Answer:
Obliczenia dla 869,44 MHz (częstotliwość naszej sieci): długość fali λ = c / f = 34,5 cm. Elementy z drutu 1,5 mm, współczynnik skrócenia 0,952 (z grubości przewodu).
• Ćwierćfalowa (ground plane):
   A — promiennik: 8,2 cm
   B — każda przeciwwaga: 8,2 cm
   zysk ok. 2,2 dBi, impedancja ok. 50 Ω
• Dipol półfalowy:
   A — długość całkowita: 16,4 cm
   B — każde ramię: 8 cm
   C — przerwa: 3,4 mm
   zysk ok. 2,2 dBi, impedancja ok. 73 Ω
• 5/8 fali:
   A — promiennik: 20,5 cm
   B — każda przeciwwaga: 8,2 cm
   zysk ok. 4 dBi, impedancja ok. 50 Ω
Na koniec dostrój miernikiem SWR albo NanoVNA: gdy rezonans wypada za nisko, skracaj po 1–2 mm. Inne typy anten, inną grubość przewodu i straty kabla policzysz w kalkulatorze anten.
Buttons: Kalkulator anten -> meshtastic://meshtastic/settings/antenna-calculator

## 13. Ile wiadomości było na kanale MT_SW w ostatnich 3 godzinach?
Router: AssistantIntent(intent=count_messages, node=, channel=MT_SW, text=, role=, metric=, topic=, period=hours, amount=3)
Time: 0 s
Answer:
Wiadomości — Ostatnie 3 godz., kanał MT_SW (1): 2 (w tym moich: 0)
Najwięcej pisali:
• bot_scyzoryk — 2

## 14. Znajdź wiadomości od SQ7DAR5.
Router: AssistantIntent(intent=search_messages, node=, channel=, text=SQ7DAR5, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Nie znalazłem wiadomości dla: SQ7DAR5

## 15. Ile jest nowych węzłów w ostatnich 24 godzinach?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=hours, amount=24)
Time: 0 s
Answer:
Ostatnie 24 godz.: słyszanych węzłów 49 z 58 znanych

Ostatnio słyszane:
• SM_Mobile — przed chwilą
• BRODY TEST — przed chwilą
• SP7UNR_ — przed chwilą
• SP7UNR_openwrt_usb — 3 min temu
• SM_Base — 4 min temu
• LGK02 — 5 min temu
• SP7UNR_rk — 6 min temu
• SQ7DAR5 — 8 min temu
… i 41 więcej

## 16. Otwórz ustawienia kanałów.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=kanały, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Otwieram: Kanały.
Buttons: Kanały -> meshtastic://meshtastic/channels [opens by itself]

## 17. Otwórz Bazę wiedzy.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=Baza_wiedzy, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Otwieram: Wyczyść bazę węzłów.
Buttons: Wyczyść bazę węzłów -> meshtastic://meshtastic/settings/clean-node-db [opens by itself]

## 18. Jaką moc nadawania mam ustawioną?
Router: AssistantIntent(intent=network_question, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 212 s
Answer:


## 19. Jak zmienić region LoRa?
Router: AssistantIntent(intent=app_help, node=, channel=, text=region, role=, metric=, topic=, period=, amount=0)
Time: 5 s
Answer:
„Region” jest w: Ustawienia → LoRa.
Buttons: Region -> meshtastic://meshtastic/settings/lora (blinks: Region)

## 20. Jak zaktualizować firmware?
Router: AssistantIntent(intent=app_help, node=, channel=, text=firmware, role=, metric=, topic=, period=, amount=0)
Time: 0 s
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
Time: 166 s
Answer:


## 22. Jaki preset jest zalecany do dużej sieci?
Router: AssistantIntent(intent=web_question, node=, channel=, text=preset duza siec, role=, metric=, topic=, period=, amount=0)
Time: 5 s
Answer:
Ustawienia sieci w województwie świętokrzyskim: oficjalny preset Narrow Fast, region EU_N_868, częstotliwość 869,44165 MHz. MT_SW nie ma własnego presetu ani regionu. Firmware MT_SW startuje domyślnie z presetem Narrow Fast, ale region trzeba wybrać.

## 23. Co się stanie, gdy ustawię limit skoków na 7?
Router: AssistantIntent(intent=app_help, node=, channel=, text=limit skoków, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
„Limit przeskoków” jest w: Ustawienia → LoRa.
Buttons: Limit przeskoków -> meshtastic://meshtastic/settings/lora (blinks: Limit przeskoków)

## 24. Co to jest flooding w sieci mesh?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 147 s
Answer:

