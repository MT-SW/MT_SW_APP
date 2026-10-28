# Assistant test

Model: qwen35-4b-q4, questions: 45

## 1. Ile mam teraz węzłów w sieci?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 13 s
Answer:
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 50, w ostatniej godzinie: 29. Słyszanych bezpośrednio (0 skoków): 7.

## 2. Czy mam zasięg do Telegraf?
Router: AssistantIntent(intent=open_link_planner, node=Telegraf, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Otwieram planer łącza dla Telegraf.
Buttons: Telegraf -> meshtastic://meshtastic/map/planner/-1343069967 [opens by itself]

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
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 50, w ostatniej godzinie: 29. Słyszanych bezpośrednio (0 skoków): 7.

## 5. Który węzeł jest najdalej od Telegraf?
Router: AssistantIntent(intent=signal_list, node=Telegraf, channel=, text=, role=, metric=farthest, topic=, period=, amount=0)
Time: 0 s
Answer:
Sygnał węzła Telegraf (unrT):
• ostatni pakiet: SNR 7.3 dB, RSSI -105 dBm, skoki 0, słyszany 14 min temu
• odbierany bezpośrednio przez ostatnie 3 dni: średni SNR 6.2 dB (od 5.5 do 7.3 dB), średni RSSI -106 dBm, pakietów: 21

## 6. Jaki jest uptime węzła Telegraf?
Router: AssistantIntent(intent=network_question, node=Telegraf, channel=, text=, role=, metric=uptime, topic=node, period=, amount=0)
Time: 1 s
Answer:
Nie znam węzła: Telegraf

## 7. Jaki jest RSSI węzła Telegraf?
Router: AssistantIntent(intent=signal_list, node=Telegraf, channel=, text=, role=, metric=snr, topic=, period=, amount=0)
Time: 0 s
Answer:
Najlepszy zasięg — węzły słyszane bezpośrednio najpierw, od najwyższego SNR (0 z odczytem, z 0):

## 8. Które węzły to routery?
Router: AssistantIntent(intent=network_question, node=, channel=, text=, role=, metric=, topic=ranking, period=, amount=0)
Time: 109 s
Answer:
Na podstawie dostarczonych danych nie mam możliwości wskazania, które węzły są routery. Widać, że nie ma telemetrii z węzłów w ciągu ostatnich trzech dni. Z tego powodu nie mam informacji o tym, które urządzenia w sieci pełnią funkcję routera.

## 9. Przejdź do węzła Telegraf.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=Telegraf, role=, metric=, topic=, period=, amount=0)
Time: 9 s
Answer:
Otwieram: Węzły.
Buttons: Węzły -> meshtastic://meshtastic/nodes [opens by itself]

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
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Moc 20 dBm = 100 mW, zysk anteny 3 dBi, straty 1 dB:
• EIRP = 22 dBm = 158 mW
• ERP = EIRP − 2,15 = 19,8 dBm = 97 mW
W zakresie naszej sieci (869,4–869,65 MHz) limit to 500 mW ERP (27 dBm): mieścisz się.

## 12. Policz bilans łącza dla 15 km, moc 20 dBm, anteny 3 dBi.
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
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
Nie ma takiego kanału: MT_SW. Kanały:

## 14. Znajdź wiadomości od Telegraf.
Router: AssistantIntent(intent=search_messages, node=, channel=, text=Telegraf, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Nie znalazłem wiadomości dla: Telegraf

## 15. Ile jest nowych węzłów w ostatnich 24 godzinach?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=hours, amount=24)
Time: 0 s
Answer:
Ostatnie 24 godz.: słyszanych węzłów 0 z 0 znanych

## 16. Otwórz ustawienia kanałów.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=kanały, role=, metric=, topic=, period=, amount=0)
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
Router: AssistantIntent(intent=network_question, node=, channel=, text=, role=, metric=, topic=radio, period=, amount=0)
Time: 0 s
Answer:
Brak jeszcze statystyk z Twojego radia.

## 19. Jak zmienić region LoRa?
Router: AssistantIntent(intent=app_help, node=, channel=, text=region, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
„Region” jest w: Ustawienia → LoRa.
Buttons: Region -> meshtastic://meshtastic/settings/lora (blinks: Region)

## 20. Jak zaktualizować firmware?
Router: AssistantIntent(intent=app_help, node=, channel=, text=update firmware, role=, metric=, topic=, period=, amount=0)
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
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 90 s
Answer:
Region LoRa to ustawienie regulacyjne, które mówi urządzeniu, w jakim paśmie częstotliwości ma pracować, jakie limity mocy i czasu nadawania obowiązują oraz jakie są zasady bezpieczeństwa. Bez ustawienia regionu węzeł nie nadaje żadnych wiadomości – po prostu milczy.

W Meshtastic region wyznacza, gdzie w eterze możesz nadawać. Dla regionu EU_868 (w tym w województwie świętokrzyskim) obowiązują limity mocy i czasu nadawania. W tym regionie limit duty cycle wynosi 10%, co oznacza, że możesz nadawać tylko 10% czasu w godzinie. Jeśli region nie jest ustawiony poprawnie, urządzenie może łamać przepisy radiowe, co jest nielegalne.

W MT_SW dla świętokrzyskiego regionu używamy oficjalnego presetu Narrow Fast w regionie EU_N_868 na częstotliwości 869,44165 MHz. To ustawienie jest zgodne z oficjalną siecią Meshtastic i nie ma własnego presetu.

Przy pierwszym uruchomieniu firmware ustawia domyślny preset, ale region trzeba wybrać ręcznie. Jeśli region jest ustawiony niepoprawnie, węzeł nie będzie słyszał nikogo ani nie będzie słyszany przez nikogo.

Warto też pamiętać, że moc podana w oprogramowaniu to sufit firmware, a nie krajowy limit prawny. Sprawdź lokalne przepisy radiowe, bo mogą być inne niż te w firmware.

## 22. Jaki preset jest zalecany do dużej sieci?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 99 s
Answer:
Do dużej sieci w regionie EU_N_868 (województwo świętokrzyskie) zalecany jest oficjalny preset Narrow Fast. To ustawienie jest zgodne z oficjalną siecią i używa częstotliwości 869,44165 MHz.

W przypadku większych sieci warto pamiętać o kilku rzeczach: nie ustawiaj zbyt wysokiego hop limitu (bezpiecznie 3–4 w osiedlu), unikaj nadawania na pełnej mocy tam, gdzie wystarczy mniej, i nie włączaj zbędnych modułów telemetrii o krótkim interwale. Każda taka zmiana zmniejsza zajętość eteru dla wszystkich sąsiadów.

W sieci z dużą liczbą routerów zaleca się ustawienie roli CLIENT_BASE/SENSOR, rebroadcast_mode=KNOWN_ONLY, oraz wyłączanie udp_enabled na nie-routerach, aby uniknąć burzy pakietów. Jeśli sieć jest zapchana, można obniżyć hop limit i dodać ignore_incoming dla hałaśliwych węzłów.

Dla ogólnego zastosowania w innych regionach, Long Fast jest zazwyczaj uważany za zrównoważony, ale w świętokrzyskim regionie Narrow Fast jest oficjalnym presetem.

## 23. Co się stanie, gdy ustawię limit skoków na 7?
Router: AssistantIntent(intent=app_help, node=, channel=, text=hop limit, role=, metric=, topic=, period=, amount=0)
Time: 14 s
Answer:
„Limit przeskoków” jest w: Ustawienia → LoRa.
Buttons: Limit przeskoków -> meshtastic://meshtastic/settings/lora (blinks: Limit przeskoków)

## 24. Co to jest flooding w sieci mesh?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 352 s
Answer:
Flooding to sposób rozsyłania wiadomości w sieci mesh, w którym węzeł, który otrzymał pakiet, przekazuje go dalej do swoich sąsiadów. W Meshtastic i MT_SW stosuje się zarządzane zalewanie (managed flooding), aby uniknąć nadmiaru transmisji. Węzły retransmitują odebrany pakiet, dopóki nie skończy się limit przeskoków. Domyślnie jest to 3 przeskoków, a maksymalne 7. Przed nadaniem węzeł chwilę nasłuchuje eteru: jeśli inny węzeł już go przekazał, ten nie powtarza. Czas oczekiwania zależy od jakości sygnału (SNR). Dalsze węzły (słabszy sygnał) mają krótsze okno i zwykle powtarzają pierwsze, a bliższe mogą odpuścić.

Wady tego podejścia leżą w fizyce: eter jest wspólny i wąski. Każdy węzeł w zasięgu słyszy każdy pakiet. Przy dużej liczbie routerów mogą dochodzić do kolizji, gdy wielu nadaje prawie jednocześnie. Dlatego sieć mesh LoRa jest lokalna, a nie globalna.

Z internetu:
Wiele źródeł opisuje flooding jako wysyłanie wiadomości do wszystkich sąsiadów, którzy ją odbiorą, bez konieczności trasy. Może to prowadzić do wielu kopii tej samej wiadomości. Wikipedia definiuje to jako wielokrotne wysłanie tej samej wiadomości. IEEE Xplore dodaje, że flooding jest proste i odporne, ale powoduje wiele kopii. WDS Media omawia to w kontekście sieci mesh ogólnie.

Źródła:
[Baza wiedzy: Algorytm sieci, aplikacja Android w użyciu, Heltec V3 i V4, integracje: Zarządzane zalewanie (managed flooding): jak węzły decydują o retransmisji]
[Baza wiedzy: Jak działa sieć: routing, retransmisje, ACK, szyfrowanie: Jak sieć przekazuje wiadomości (managed flooding)?]
[Baza wiedzy: Dlaczego sieci LoRa (Mes

## 25. Czy wiadomości w Meshtastic są szyfrowane?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 120 s
Answer:
Tak, wiadomości w Meshtastic są szyfrowane. Istnieją dwa główne rodzaje szyfrowania:

Na kanałach komunikacji (broadcast) wiadomości są szyfrowane za pomocą AES-CTR. Klucz szyfrujący jest wspólny dla wszystkich węzłów na danym kanale. Domyślnie używa się AES-128, ale można włączyć AES-256 dla większej bezpieczeństwa. Od wersji 2.8.1 dostępne jest też szyfrowanie z wykrywaniem modyfikacji (AEAD), które wymaga, by wszystkie węzły na kanale miały to samo ustawienie.

Wiadomości prywatne (Direct Messages) używają szyfrowania na poziomie par kluczy. Każdy węzeł ma własną parę kluczy X25519, z których wywodzi się wspólny klucz do szyfrowania AES-256-CCM. Aby wysłać wiadomość prywatną, nadawca musi najpierw poznać klucz publiczny odbiorcy, który jest dostępny w NodeInfo.

Warto pamiętać, że szyfrowanie nie jest tak zaawansowane jak w Signal czy TLS 1.3. Brakuje tam tzw. forward secrecy, co oznacza, że nagrany ruch można odszyfrować później, jeśli klucz kiedyś wycieknie. Wiadomości nie są domyślnie przechowywane i przesyłane później, więc jeśli odbiorca nie usłyszał wiadomości, ona przepada.

W MT_SW nie ma własnego presetu szyfrowania - używamy oficjalnego presetu Narrow Fast z regionem EU_N_868 dla województwa świętokrzyskiego.

Źródła:
• https://meshtastic.org/docs/overview/encryption/ (sprawdzono 2026-10)
• https://meshtastic.org/docs/overview/encryption/ (sprawdzono 2026-10)

## 26. Jak zrobić test zasięgu?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 119 s
Answer:
Test zasięgu w MT_SW to moduł Range Test, który działa na obu węzłach - nadajniku i odbiorniku. Moduł ten automatycznie wysyła wiadomości testowe z rosnącymi licznikami, które odbierający węzeł zapisuje do pliku CSV. Dzięki temu możesz odejść lub odjechać, a potem przeanalizować, przy jakiej odległości wiadomości przestały docierać.

Aby to zrobić, musisz włączyć moduł na nadajniku i odbiorniku. Na stałym nadajniku ustaw interwał wysyłania - zalecane 30 sekund dla Long Fast, 15 dla Medium i Short Fast. Odbiornik najlepiej ma GPS dla dokładnej pozycji. W CLI polecenie to: meshtastic --set range_test.enabled true --set range_test.sender 60.

Pamiętaj, że zapis do pliku CSV działa tylko na ESP32. Plik pobierzesz przez Wi-Fi z adresu meshtastic.local/rangetest.csv. Ważne: ustaw wysoką precyzję pozycji na kanale, inaczej zapisane położenie będzie przybliżone.

Po teście koniecznie wyłącz moduł - zajmuje dużo eteru, zapycha kanał i sam wyłącza się po 8 godzinach. W aplikacji MT_SW możesz też korzystać z funkcji oceny sygnału i kalkulatora anten do porównania zasięgu z różnymi antenami.

Źródła:
• https://meshtastic.org/docs/configuration/module/range-test/ (sprawdzono 2026-10)
• https://meshtastic.org/docs/configuration/module/range-test/ (sprawdzono 2026-10)
• https://meshtastic.org/docs/hardware/antennas/antenna-testing/ (sprawdzono 2026-10)

## 27. Co to jest MT_SW_FIRMWARE?
Router: AssistantIntent(intent=app_help, node=, channel=, text=MT_SW_FIRMWARE, role=, metric=, topic=, period=, amount=0)
Time: 110 s
Answer:
MT_SW_FIRMWARE to modyfikowana wersja oficjalnego firmware Meshtastic, przygotowana dla regionalnej sieci mesh MT_SW w województwie świętokrzyskim. Jest to fork projektu, który zawiera zmiany dostosowane do potrzeb lokalnej sieci.

Główne cechy tego firmware to:
• Rozszerzona telemetria
• Poprawki prywatności w MQTT
• Dodatkowe komendy czatu
• Nowe warianty sprzętowe
• Poprawki stabilności

Warto wiedzieć, że MT_SW nie ma własnego presetu – używa oficjalnego presetu Narrow Fast z regionem EU_N_868 (częstotliwość 869,44165 MHz). Firmware startuje z tym presetem, ale region trzeba wybrać w aplikacji.

Projekt jest prowadzony przez Spontaniczne Majsterkowanie i cheaterenator. Firmware jest częścią trójkomponentowego projektu MT_SW razem z aplikacją MT_SW_APP i integracją z Home Assistant.

Jeśli potrzebujesz więcej szczegółów o konkretnym urządzeniu, ustawieniu lub funkcji, daj znać.
