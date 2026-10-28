# Assistant test

Model: g4-e4b-q4, questions: 45

## 1. Ile mam teraz węzłów w sieci?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 14 s
Answer:
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 50, w ostatniej godzinie: 33. Słyszanych bezpośrednio (0 skoków): 6.

## 2. Czy mam zasięg do SM_Base?
Router: AssistantIntent(intent=open_link_planner, node=SM_Base, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Otwieram planer łącza dla SM_Base.
Buttons: SM_Base -> meshtastic://meshtastic/map/planner/-969537696 [opens by itself]

## 3. Jaki teoretyczny zasięg mam przy antenach 5 dBi?
Router: AssistantIntent(intent=web_question, node=, channel=, text=teoretyczny zasięg anten 5 dBi, role=, metric=, topic=, period=, amount=0)
Time: 1 s
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
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 50, w ostatniej godzinie: 33. Słyszanych bezpośrednio (0 skoków): 6.

## 5. Który węzeł jest najdalej od SM_Base?
Router: AssistantIntent(intent=signal_list, node=SM_Base, channel=, text=, role=, metric=farthest, topic=, period=, amount=0)
Time: 1 s
Answer:
Sygnał węzła SM_Base (SM_B):
• ostatni pakiet: SNR 11.5 dB, RSSI -32 dBm, skoki 0, słyszany 1 min temu
• odbierany bezpośrednio przez ostatnie 3 dni: średni SNR 12.2 dB (od 11.5 do 12.8 dB), średni RSSI -30 dBm, pakietów: 23

## 6. Jaki jest uptime węzła SM_Base?
Router: AssistantIntent(intent=network_question, node=SM_Base, channel=, text=, role=, metric=uptime, topic=node, period=, amount=0)
Time: 24 s
Answer:
Uptime węzła SM_Base wynosi 1120896 sekund, czyli 12 dni i 23 godziny.

## 7. Jaki jest RSSI węzła SM_Base?
Router: AssistantIntent(intent=signal_list, node=SM_Base, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Sygnał węzła SM_Base (SM_B):
• ostatni pakiet: SNR 11.5 dB, RSSI -32 dBm, skoki 0, słyszany 1 min temu
• odbierany bezpośrednio przez ostatnie 3 dni: średni SNR 12.2 dB (od 11.5 do 12.8 dB), średni RSSI -30 dBm, pakietów: 23

## 8. Które węzły to routery?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 50, w ostatniej godzinie: 33. Słyszanych bezpośrednio (0 skoków): 6.

## 9. Przejdź do węzła SM_Base.
Router: AssistantIntent(intent=show_node_on_map, node=SM_Base, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Pokazuję SM_Base na mapie.
Buttons: SM_Base -> meshtastic://meshtastic/map/focus/-969537696 [opens by itself]

## 10. Policz zasięg dla anteny 8 dBi na maszcie 20 m.
Router: AssistantIntent(intent=web_question, node=, channel=, text=zasięg antena 8 dBi maszt 20 m, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Budżet łącza dla presetu Narrow Fast (ustawiony w radiu) (SF7, 62,5 kHz) przy 869,44 MHz:
• moc 27 dBm (z radia), anteny 8 dBi i 8 dBi
• czułość odbiornika ok. -127,5 dBm (granica SNR -7,5 dB)
• dopuszczalne tłumienie ok. 170,5 dB
• zasięg teoretyczny w wolnej przestrzeni: ok. 9243 km
To górna granica przy pełnej widoczności anten. Na Ziemi zasięg ogranicza horyzont radiowy: anteny na 20 m i 20 m — ok. 36,9 km. Do tego przeszkody (budynki, las, wzgórza). Konkretne łącze sprawdzisz w planerze łącza.

## 11. Policz EIRP dla mocy 20 dBm, anteny 3 dBi i strat 1 dB.
Router: AssistantIntent(intent=web_question, node=, channel=, text=EIRP 20 dBm 3 dBi 1 dB, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Moc 20 dBm = 100 mW, zysk anteny 3 dBi, straty 1 dB:
• EIRP = 22 dBm = 158 mW
• ERP = EIRP − 2,15 = 19,8 dBm = 97 mW
W zakresie naszej sieci (869,4–869,65 MHz) limit to 500 mW ERP (27 dBm): mieścisz się.

## 12. Policz bilans łącza dla 15 km, moc 20 dBm, anteny 3 dBi.
Router: AssistantIntent(intent=open_link_planner, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
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
Time: 1 s
Answer:
Wiadomości — Ostatnie 3 godz., kanał MT_SW (1): 7 (w tym moich: 0)
Najwięcej pisali:
• bot_scyzoryk — 7

## 14. Znajdź wiadomości od SM_Base.
Router: AssistantIntent(intent=search_messages, node=, channel=, text=SM_Base, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Znaleziono: 1 (najnowsze na górze)
• MT_SW, 9 dni temu, ke46: Bot Scyzoryk potwierdza test; OD: SM_Base (SM_B); ID: !c6360760; HOPS: 1; ODEBRANO: 13:47:22;

## 15. Ile jest nowych węzłów w ostatnich 24 godzinach?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 50, w ostatniej godzinie: 33. Słyszanych bezpośrednio (0 skoków): 6.

## 16. Otwórz ustawienia kanałów.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=channels, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Otwieram: Kanały.
Buttons: Kanały -> meshtastic://meshtastic/channels [opens by itself]

## 17. Otwórz Bazę wiedzy.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=Baza wiedzy, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Otwieram: Wyczyść bazę węzłów.
Buttons: Wyczyść bazę węzłów -> meshtastic://meshtastic/settings/clean-node-db [opens by itself]

## 18. Jaką moc nadawania mam ustawioną?
Router: AssistantIntent(intent=show_setting, node=, channel=, text=moc nadawania, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Otwieram stronę z ustawieniem Nadawanie włączone. Mruga, żeby łatwo je znaleźć; zmianę robisz sam.
Buttons: Nadawanie włączone -> meshtastic://meshtastic/settings/lora (blinks: Nadawanie włączone) [opens by itself]

## 19. Jak zmienić region LoRa?
Router: AssistantIntent(intent=app_help, node=, channel=, text=region LoRa, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
„Region” jest w: Ustawienia → LoRa.
Buttons: Region -> meshtastic://meshtastic/settings/lora (blinks: Region)

## 20. Jak zaktualizować firmware?
Router: AssistantIntent(intent=app_help, node=, channel=, text=aktualizacja firmware, role=, metric=, topic=, period=, amount=0)
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
Router: AssistantIntent(intent=app_help, node=, channel=, text=region LoRa, role=, metric=, topic=, period=, amount=0)
Time: 185 s
Answer:
Region LoRa to zestaw zasad, który określa, w jakim pasmie radiowym i przy jakich limitach mocy i czasu nadawania Twój węzeł ma pracować. Można to porównać do wyboru konkretnego, legalnego "korytarza" radiowego, który jest przeznaczony do użytku w danym kraju lub regionie.

Musisz go ustawić, ponieważ bez tego węzeł po prostu nie nadaje i pozostaje w ciszy.

Główne powody, dla których trzeba ustawić region, to:

Po pierwsze, region wyznacza pasmo, limity mocy i cykl pracy, czyli to, jak często możesz nadawać. Na przykład, w pasmie EU_868 obowiązuje limit 10 procent czasu nadawania. Jeśli nie ustawisz regionu, urządzenie nie wie, na jakich częstotliwościach i z jaką mocą ma działać.

Po drugie, region zapewnia kompatybilność. Aby węzły mogły ze sobą rozmawiać, muszą mieć zgodne ustawienia. Jeśli Twój węzeł ma inny region niż sąsiedzi, nie usłyszy ich.

W kontekście naszego projektu MT_SW, w województwie świętokrzyskim używamy regionu EU_N_868, który jest zgodny z oficjalnym presetem Narrow Fast i pracuje na częstotliwości 869,44165 MHz.

Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10); https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10); https://meshtastic.org/docs/getting-started/initial-config/ (sprawdzono 2026-10)

## 22. Jaki preset jest zalecany do dużej sieci?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 144 s
Answer:
Nie ma jednego, uniwersalnego presetu, który byłby zalecany dla "dużej sieci", ponieważ problem z rozrostem sieci LoRa rzadko wynika z samego wyboru preset, a częściej z zarządzania eterem i uniknięcia kolizji.

Zamiast polecać konkretny preset, nasza baza wiedzy skupia się na tym, jak zapobiegać zapchaniu sieci, niezależnie od tego, który preset używasz.

Jeśli chcesz, aby sieć była stabilna i nie cierpiała na kolizje, kluczowe jest:
•   Ograniczanie zajętości eteru: nie ustawiaj wyższego hop limitu niż jest to konieczne.
•   Zarządzanie mocą: nie nadawaj na pełnej mocy tam, gdzie wystarczy mniejsza moc.
•   Optymalizacja ról: nie ustawiaj roli routerów bez realnej potrzeby.
•   Ograniczanie telemetrii: nie włączaj zbędnych modułów telemetrii o bardzo krótkich interwałach.

Te działania pomagają zmniejszyć zajętość eteru dla wszystkich sąsiadów, co jest najważniejsze w gęstej sieci.

Źródło: https://wiki.mesh.szczecin.pl/routing-meshtastic-od-strony-firmware-0-lora-udp-role-i-strojenie/ oraz https://wehelp.com.pl/poradniki/meshtastic-lacznosc-awaryjna-jak-dziala/ (sprawdzono 2026-10)
Źródło: wnioski z wpisów bazy o routingu i airtime (sprawdzono 2026-10); zalecenia ogólne, nie przepisy

## 23. Co się stanie, gdy ustawię limit skoków na 7?
Router: AssistantIntent(intent=app_help, node=, channel=, text=limit skoków, role=, metric=, topic=, period=, amount=0)
Time: 14 s
Answer:
„Limit przeskoków” jest w: Ustawienia → LoRa.
Buttons: Limit przeskoków -> meshtastic://meshtastic/settings/lora (blinks: Limit przeskoków)

## 24. Co to jest flooding w sieci mesh?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 153 s
Answer:
Flooding w sieci mesh to w zasadzie sposób rozsyłania wiadomości, który opiera się na zasadzie: jeśli węzeł odebrał pakiet, to wysyła go dalej do wszystkich swoich sąsiadów. Sieć nie wie z góry, jaka jest najkrótsza lub najlepsza droga do odbiorcy, więc aby mieć pewność, że wiadomość dotrze, rozsyła ją szeroko.

W kontekście MT_SW i Meshtastic, ten proces jest jednak zarządzany, czyli nie jest chaotyczny. Oto jak to działa:

1.  Brak wiedzy o celu: Ponieważ sieć kratowa nie ma centralnego zarządzania, pierwszy pakiet po prostu wysyła do wszystkich w zasięgu, bo nie wie, gdzie jest adresat.
2.  Mechanizm zarządzany: Kiedy węzeł odbiera pakiet, zanim go powtórzy, najpierw chwilę nasłuchuje. Jeśli usłyszy, że inny węzeł już ten pakiet przekazał, to on milczy i nie marnuje eteru.
3.  Ograniczenia: Aby zapobiec nieskończonym pętlom i przeciążeniu, wiadomości mają limit przeskoków (HopLimit). Węzeł zmniejsza ten limit o jeden za każdym razem, gdy retransmituje pakiet.

Podsumowując, flooding to mechanizm, który zapewnia, że wiadomość dotrze do celu, rozsyłając ją szeroko, a zarządzanie tym procesem (limit przeskoków i nasłuchiwanie) zapobiega chaosowi i kolizjom w eterze.

Źródła:
https://meshtastic.org/docs/overview/mesh
