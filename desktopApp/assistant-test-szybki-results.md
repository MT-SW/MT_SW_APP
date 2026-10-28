# Assistant test

Model: qwen35-4b-q4, questions: 45

## 1. Ile mam teraz węzłów w sieci?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 9 s
Answer:
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 49, w ostatniej godzinie: 27. Słyszanych bezpośrednio (0 skoków): 6.

## 2. Czy mam zasięg do SP7UNR_meshmonitor?
Router: AssistantIntent(intent=open_link_planner, node=SP7UNR_meshmonitor, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 0 s
Answer:
Otwieram planer łącza dla SP7UNR_meshmonitor.
Buttons: SP7UNR_meshmonitor -> meshtastic://meshtastic/map/planner/-1476395266 [opens by itself]

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
Znanych węzłów: 58. Słyszanych w ostatnich 24 godz.: 49, w ostatniej godzinie: 27. Słyszanych bezpośrednio (0 skoków): 6.

## 5. Który węzeł jest najdalej od SP7UNR_meshmonitor?
Router: AssistantIntent(intent=signal_list, node=SP7UNR_meshmonitor, channel=, text=, role=, metric=farthest, topic=, period=, amount=0)
Time: 1 s
Answer:
Sygnał węzła SP7UNR_meshmonitor (unr8):
• ostatni pakiet: SNR brak, RSSI brak, skoki 4, słyszany przed chwilą
• przez ostatnie 3 dni brak pakietów odebranych od niego bezpośrednio (dociera przez inne węzły), więc nie ma średniej

## 6. Jaki jest uptime węzła SP7UNR_meshmonitor?
Router: AssistantIntent(intent=network_question, node=SP7UNR_meshmonitor, channel=, text=, role=, metric=uptime, topic=node, period=, amount=0)
Time: 10 s
Answer:
Uptime węzła SP7UNR_meshmonitor wynosi 604474 sekund, co odpowiada 6 dniom i 23 godzinom.

## 7. Jaki jest RSSI węzła SP7UNR_meshmonitor?
Router: AssistantIntent(intent=signal_list, node=SP7UNR_meshmonitor, channel=, text=, role=, metric=snr, topic=, period=, amount=0)
Time: 1 s
Answer:
Sygnał węzła SP7UNR_meshmonitor (unr8):
• ostatni pakiet: SNR brak, RSSI brak, skoki 4, słyszany przed chwilą
• przez ostatnie 3 dni brak pakietów odebranych od niego bezpośrednio (dociera przez inne węzły), więc nie ma średniej

## 8. Które węzły to routery?
Router: AssistantIntent(intent=network_question, node=, channel=, text=, role=, metric=, topic=ranking, period=, amount=0)
Time: 20 s
Answer:
Węzły, które są routeryami w Twojej sieci, to te, które mają wysoki wskaźnik nadawania (tx) w stosunku do odbioru (rx) oraz są wymienione w listach routingu. Na podstawie danych, które widzę, routery to węzły: SM_B (SM_Base), TKI0 (SM_R0 Łysica Master), SM_H (SM_Home_Assistant), MEHR (SP7MEH_relay), ke46 (bot_scyzoryk), TKI2 (SM_R2 Łysica Slave), unr1 (SP7UNR_base), unr3 (SP7UNR_atk10), unrZ (SP7UNR_rk) oraz DAR5 (SQ7DAR5). Węzły takie jak TOS1 nie są routeryami, ponieważ ich wskaźniki tx i rx wynoszą 0.

## 9. Przejdź do węzła SP7UNR_meshmonitor.
Router: AssistantIntent(intent=open_screen, node=, channel=, text=SP7UNR_meshmonitor, role=, metric=, topic=, period=, amount=0)
Time: 1 s
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
Wiadomości — Ostatnie 3 godz., kanał MT_SW (1): 2 (w tym moich: 0)
Najwięcej pisali:
• bot_scyzoryk — 2

## 14. Znajdź wiadomości od SP7UNR_meshmonitor.
Router: AssistantIntent(intent=search_messages, node=, channel=, text=SP7UNR_meshmonitor, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
Nie znalazłem wiadomości dla: SP7UNR_meshmonitor

## 15. Ile jest nowych węzłów w ostatnich 24 godzinach?
Router: AssistantIntent(intent=list_nodes, node=, channel=, text=, role=, metric=, topic=, period=hours, amount=24)
Time: 0 s
Answer:
Ostatnie 24 godz.: słyszanych węzłów 49 z 58 znanych

Ostatnio słyszane:
• SM_Mobile — przed chwilą
• SP7UNR_base — przed chwilą
• SP7UNR_meshmonitor — przed chwilą
• SM_Home_Assistant — 4 min temu
• SP7UNR_rpi-868 — 4 min temu
• SP7UNR_ikoka-c3 — 7 min temu
• SlawekPL — 10 min temu
• SM_R1 Raszówka — 13 min temu
… i 41 więcej

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
Otwieram: Baza wiedzy.
Buttons: Baza wiedzy -> meshtastic://meshtastic/settings/mt-sw-features [opens by itself]

## 18. Jaką moc nadawania mam ustawioną?
Router: AssistantIntent(intent=network_question, node=, channel=, text=, role=, metric=, topic=radio, period=, amount=0)
Time: 0 s
Answer:
Statystyki Twojego radia SM_Mobile:
• działa od 3 d 5 h
• zajętość kanału 0.0 %, własne nadawanie 0.0 %
• pakiety wysłane 236, odebrane 24244, błędne 2814, duplikaty 11129
• przekazane dalej 0, przekazanie anulowane 0, odrzucone przy wysyłaniu 0
• węzły online 36 z 59
• RAM zajęty 78.9 % (110 z 140 kB)
• poziom szumu -113 dBm
• procesor 3 %, flash zajęty 36.0 %

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
Time: 65 s
Answer:
Region LoRa to ustawienie, które mówi urządzeniu, w jakim paśmie częstotliwości ma nadawać i odbierać sygnały, jakie są tam obowiązujące limity mocy oraz jak często może nadawać (cykl pracy). Bez ustawienia regionu węzeł nie nadaje żadnych wiadomości, bo firmware blokuje nadawanie, dopóki nie zostanie określony region.

W sieci MT_SW w województwie świętokrzyskim używamy regionu EU_868, który pozwala pracować w pasmie 868 MHz. W tym regionie obowiązują limity: do 500 mW mocy i 10% czasu nadawania. Jeśli ustawisz inny region, np. EU_433, będziesz nadawał na innych częstotliwościach, ale nie będziesz słyszał nikogo z naszej sieci, bo oni używają innych ustawień.

Region jest też konieczny do wygenerowania tożsamości węzła – po jego ustawieniu urządzenie otrzymuje nowy numer węzła. W MT_SW nie ma własnego presetu, więc po ustawieniu regionu EU_868 firmware wybiera domyślny preset (Long Fast), ale musisz ręcznie zmienić częstotliwość na 869,44165 MHz, aby dołączyć do naszej sieci.

Źródło: [Ustawienia — Radio i użytkownik: Konfiguracja LoRa](https://meshtastic.org/docs/configuration/radio/lora/), [Baza wiedzy: LoRa: regiony, presety i parametry: Co trzeba ustawić przed rozpoczęciem pracy w sieci i jakie są ostrzeżenia dla regionów?](https://meshtastic.org/docs/getting-started/initial-config/)

## 22. Jaki preset jest zalecany do dużej sieci?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 69 s
Answer:
Do dużej sieci w kontekście MT_SW w województwie świętokrzyskim zalecany jest preset Narrow Fast. Jest to oficjalny preset używany w tej sieci, skonfigurowany w regionie EU_N_868 na częstotliwości 869,44165 MHz.

Warto jednak pamiętać, że „duża sieć" w kontekście LoRa ma swoje ograniczenia fizyczne, takie jak czas antenowy i ryzyko kolizji, o których mówi się w dokumentacji. W bardzo gęstych miejskich sieciach nawet w presetach wąskopasmowych (Narrow) może wystąpić problem z zapchanym eterem, jeśli zbyt wiele węzłów nadaje jednocześnie. W takich sytuacjach zaleca się ograniczenie liczby routerów lub zmniejszenie liczby powtórzeń (retransmissions) dla kluczowych węzłów, aby uniknąć kolizji.

Jeśli mówimy o ogólnych zasadach dla dużych sieci w innych projektach (np. MeshCore), tam zalecane są często presety wąskopasmowe (Narrow), ale z bardzo ostrożnym zarządzaniem ruchem, ponieważ szerokie pasmo (jak Long Turbo) w dużej sieci może prowadzić do błyskawicznego zapełnienia się kanału i utraty łączności dla wszystkich. W MT_SW nie ma własnego presetu, więc używa się oficjalnego Narrow Fast, ale użytkownik sam musi zadbać o to, by jego węzeł nie był „hałaśliwym" dla innych, np. nie ustawiając zbędnych modułów telemetrii czy UDP na niepotrzebnych węzłach.

## 23. Co się stanie, gdy ustawię limit skoków na 7?
Router: AssistantIntent(intent=app_help, node=, channel=, text=hop limit, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
„Limit przeskoków” jest w: Ustawienia → LoRa.
Buttons: Limit przeskoków -> meshtastic://meshtastic/settings/lora (blinks: Limit przeskoków)

## 24. Co to jest flooding w sieci mesh?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 66 s
Answer:
Flooding w sieci mesh to prosty sposób rozprzestrzeniania wiadomości, w którym każdy węzeł, który pierwszy raz otrzyma pakiet, natychmiast przekazuje go dalej swoim sąsiadom. Nie ma w tym procesie wyznaczonej trasy ani adresata – wiadomość idzie do wszystkich, kto się słyszy.

W Meshtastic i MT_SW ten proces jest kontrolowany, żeby nie psuć sieci. Węzły mają limit przeskoków (domyślnie 3, maksymalnie 7). Po każdym przekazywaniu ten limit maleje o jeden, a gdy się skończy, wiadomość przestaje się rozprzestrzeniać. Dodatkowo węzły nasłuchują krótko przed nadaniem: jeśli ktoś inny już przekazał ten sam pakiet, dany węzeł milczy, co zmniejsza liczbę powtórzeń. Istnieją też specjalne role, jak ROUTER, które mogą nadawać mimo usłyszenia cudzego sygnału, ale domyślnie większość węzłów (np. CLIENT_BASE) się powstrzymuje dla pakietów do wybranych adresatów.

Dzięki temu flooding gwarantuje, że wiadomość dotrze nawet do bardzo dalekich węzłów, które nie są bezpośrednimi sąsiadami, ale w dużych sieciach może to powodować wiele powtórzeń i blokować eter, dlatego sieć mesh LoRa jest przeznaczona do użycia w ograniczonym zasięgu.

Źródła:
• https://meshtastic.org/docs/overview/mesh-algo/
• https://meshtastic.pop.pl/viewtopic.php?t=290
• https://www.offlineprotocol.com/learning/mesh-networking/what-is-flooding-in-a-mesh-network/

## 25. Czy wiadomości w Meshtastic są szyfrowane?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 74 s
Answer:
Tak, wiadomości w Meshtastic są szyfrowane, ale sposób ochrony zależy od tego, czy są to wiadomości publiczne (kanałowe), czy prywatne (DM).

Wiadomości publiczne, które wysyłasz na głównym kanale, są szyfrowane za pomocą AES-CTR. Używany jest wspólny klucz kanału. Domyślnie ten klucz jest publiczny (AQ==), więc każdy w sieci może go odczytać. Od wersji firmware 2.8.1 można włączyć opcję AEAD, która zmienia szyfrowanie na AES-CCM. Dzięki temu dodaje się wykrywanie zmian pakietu, ale wszystkie węzły w kanale muszą mieć tę samą ustawienie, aby działało poprawnie.

Wiadomości prywatne (DM) są znacznie bardziej bezpieczne. Są one szyfrowane za pomocą AES-256-CCM. Klucz do szyfrowania jest wywodzony z pary kluczy X25519, gdzie klucz prywatny pochodzi od nadawcy, a publiczny od odbiorcy. Aby wysłać wiadomość prywatną, węzeł musi najpierw poznać klucz publiczny odbiorcy, który znajduje się w informacji o węźle (NodeInfo). Bez tego klucza wiadomość nie zostanie wysłana.

Warto jednak pamiętać o kilku ograniczeniach bezpieczeństwa: nagłówek każdego pakietu nie jest szyfrowany, bo potrzebują go przekaźniki w sieci. Nie ma też ochrony przed przodem (forward secrecy), co oznacza, że jeśli kiedyś wycieknie klucz kanału, wszystkie nagrane wiadomości z tego okresu mogą zostać odszyfrowane. Wiadomości publiczne nie są podpisane domyślnie, więc nie ma pewności, że nie zostały zmodyfikowane w trasie. W trybach bardziej rygorystycznych (BALANCED czy STRICT) niepodpisane wiadomości publiczne mogą być odrzucane, ale domyślnie są akceptowane.

W ogólnym ujęciu Meshtastic nie oferuje poziomu bezpieczeństwa tak wysokiego jak Signal czy TLS 1.3.

## 26. Jak zrobić test zasięgu?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 67 s
Answer:
Test zasięgu w sieci MT_SW możesz wykonać za pomocą wbudowanego modułu Range Test, który działa automatycznie, lub ręcznie wysyłając wiadomości.

Najprostsza metoda to włączenie modułu Range Test na obu węzłach, które mają się testować. Na nadajniku ustawiasz interwał wysyłania, np. 30 sekund dla ustawień Long Fast lub 15 sekund dla Medium/Short Fast. Odbiornik najlepiej ma włączony GPS, aby precyzyjnie znał swoją pozycję. Po włączeniu moduł na nadajniku regularnie wysyła pakiety testowe, a odbiornik je odbiera i zapisuje dane (na ESP32 jako plik CSV, w aplikacji jako log). Możesz wtedy odjechać lub odejść od nadajnika i sprawdzić, przy jakiej odległości sygnał przestaje docierać. Ważne jest ustawienie wysokiej precyzji pozycji na kanale, inaczej zapisane miejsca będą niedokładne. Po zakończeniu testu warto wyłączyć moduł, ponieważ zużywa on dużo czasu antenowego i może zapychać kanał, a sam wyłączy się po 8 godzinach.

Jeśli wolisz test ręczny, możesz wysyłać wiadomości między węzłami, zapisując miejsca, w których otrzymujesz potwierdzenie (ACK) oraz siłę sygnału. Następnie zmieniasz antenę lub pozycję i powtarzasz test, aby porównać wyniki. W aplikacji MT_SW możesz skorzystać z funkcji oceny sygnału i kalkulatora anten, aby lepiej zrozumieć warunki. Pamiętaj, że test działa tylko na zabezpieczonym kanale głównym, dopóki nie zmienisz klucza kanału.

## 27. Co to jest MT_SW_FIRMWARE?
Router: AssistantIntent(intent=app_help, node=, channel=, text=MT_SW_FIRMWARE, role=, metric=, topic=, period=, amount=0)
Time: 60 s
Answer:
MT_SW_FIRMWARE to modyfikowana wersja oficjalnego oprogramowania sprzętowego (firmware) projektu Meshtastic, stworzona przez nas dla potrzeb regionalnej sieci mesh w województwie świętokrzyskim. Nie jest to własny preset, ale zgodny z główną siecią projekt, który uruchamia się domyślnie z ustawieniami Narrow Fast dla regionu EU_N_868 na częstotliwości 869,44165 MHz.

Projekt ten, prowadzony przez Spontaniczne Majsterkowanie i cheaterenator, wprowadza kilka istotnych zmian względem oryginału. Obejmują one rozszerzoną telemetrię, która pozwala na śledzenie stanu urządzenia, poprawki dotyczące prywatności, aby wiadomości nie trafiały na zewnętrzne serwery MQTT, dodatkowe funkcje czatu, wsparcie dla nowych wariantów sprzętowych oraz szereg poprawek stabilności.

Warto wiedzieć, że firmware to oprogramowanie wbudowane w sprzęt, które mówi mu, jak ma działać. W naszym przypadku jest to logika sterująca radia i komunikacją w sieci, która musi być wgrać na każde urządzenie, aby mogło działać w naszej lokalnej sieci. Nie zmieniamy ustawień w nim automatycznie – to użytkownik dokonuje tego w aplikacji MT_SW_APP lub bezpośrednio w firmware, jeśli to konieczne.

## 28. Jak zmniejszyć zużycie baterii?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 94 s
Answer:
Aby zmniejszyć zużycie baterii w węźle MT_SW, skup się na kilku kluczowych obszarach: ustawieniach sieci, roli węzła, zasilaniu i sprzęcie.

Najpierw sprawdź ustawienia sieci w aplikacji. Jeśli sieć jest gęsta, domyślne ustawienie rozgłaszania listy sąsiadów może być zbyt agresywne. Firmware nie pozwala na krótszy interwał niż 4 godziny, ale w zajętych sieciach warto zwiększyć ten czas do 6 godzin lub więcej, aby uniknąć zbędnego nadawania. Wyłączaj moduły, z których nie korzystasz – każdy włączony moduł zwiększa zajętość eteru i pobór prądu.

Kolejnym krokiem jest wybór roli węzła. Jeśli nie chcesz uczestniczyć w przekazywaniu danych, ustaw go jako Klient pasywny. Jeśli to urządzenie do śledzenia lub czujnik, wybierz rolę Tracker lub Sensor – są one zaprojektowane tak, aby spały między transmisjami, co drastycznie oszczędza energię. Unikaj roli Repeater, jeśli nie jest to konieczne, bo przekazywanie danych zużywa znacznie więcej prądu.

Zwróć uwagę na zasilanie. Jeśli używasz akumulatora Li-ion, pamiętaj, że na zimę jego pojemność spada, a ładowanie poniżej 0 stopni Celsjusza może trwale uszkodzić ogniwo. Używaj ładowarek z ochroną i układów BMS, które ograniczają napięcie i prąd. W przypadku solarnego zasilania preferuj regulatory MPPT zamiast PWM, a w zimnych warunkach rozważ izolację akumulatora lub grzałkę, aby utrzymać temperaturę powyżej zera.

Sprawdź też sprzęt. Antena jest kluczowa – słabszy sygnał wymusza częstsze i mocniejsze nadawanie. Jeśli antena jest słaba lub źle ustawiona, zmień ją na lepszą lub popraw jej pozycję. Sprawdź też stan baterii – jeśli zużyła się do ok. 80% pojemności, warto ją wymienić. W węźle na baterii unikaj zbędnych funkcji, takich jak zdalne GPIO czy sniffer, jeśli nie są one potrzebne do Twojego zadania.

Na koniec, jeśli korzystasz z Home Assistant, upewnij się, że integracja MT_SW_INTEGRATION nie wysyła zbędnego ruchu danych. Wiele informacji o sąsiadach i statystykach może być przesłana tylko po żądaniu, a nie automatycznie, co zmniejszy zużycie anteny.

Z internetu:
Ogólne porady na oszczędzanie baterii w telefonach, takie jak ograniczanie odświeżania w tle, dopasowanie jasności ekranu czy zarządzanie aplikacjami, nie mają bezpośredniego wpływu na zużycie baterii w węźle radiowym MT_SW, ponieważ węzeł działa niezależnie od telefonu i nie korzysta z tych funkcji systemowych. Źródła: https://www.quicon.eu/artykul/jak-zmniejszyc-zuzycie-baterii-w-telefonie-skuteczne-sposoby-374332/, https://www.orange.pl/poradnik/12-sposobow-na-oszczedzanie-baterii-w-smartfonie/, https://mobidata.pl/jak-oszczedzac-baterie-w-telefonie/, https://netdiag.pl/jak-zmniejszyc-zuzycie-baterii-iphone-praktyczne-triki/, https://www.samsung.com/pl/support/mobile-devices/jak-zminimalizowac-zuzycie-baterii/.

## 29. Jak chronić węzeł przed wodą?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 71 s
Answer:
Aby chronić węzeł przed wodą, kluczowe jest odpowiednie uszczelnienie miejsc, gdzie kable wchodzą do obudowy. Na każdy kabel należy zamontować dławik, który odciąża kabel i uszczelnia przepust. Jeśli dławik ma uszczelkę elastomerową, skutecznie chroni przed kroplami i wodą pod ciśnieniem. Dla kabli z już założoną wtyczką warto używać dławików dzielonych, które mogą osiągać stopień ochrony IP66 lub IP68.

Przy wyborze dławika zwracaj uwagę na typ gwintu: mogą to być gwinty PG, metryczne lub calowe (NPT). Pamiętaj, że uszczelnienie wszystkich szczelin jest niezbędne. Dodatkowo, zgodnie z ogólnymi zasadami budowlącymi, warto zapewnić odpowiednią wentylację wewnątrz obudowy, aby uniknąć skraplania się pary wodnej, które mogłoby osłabić szczelność.

Jeśli montujesz antenę na maszcie, warto rozważyć minimalny stopień ochrony IP65, który gwarantuje odporność na pył i strumień wody z dowolnego kierunku. Pamiętaj jednak, że same stopnie ochrony nie wystarczą; kluczowe jest też właściwe zagięcie kabla w dół tuż przed wejściem do obudowy, dzięki czemu ewentualne krople wody spłyną na zewnątrz.

Z internetu:
Warto też pamiętać o zabezpieczeniu instalacji wodnej wewnątrz domu przed zimą, np. przez spuszczanie wody z muszli i syfonów oraz nalać płynu zimowego, jeśli planujesz rzadkie wizyty. Jednak w kontekście węzła Meshtastic to nie jest priorytet, bo węzeł nie jest elementem instalacji wodnej.

## 30. Co to jest filtr SAW i po co się go stosuje?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 68 s
Answer:
Filtr SAW (Surface Acoustic Wave – falą powierzchniową) to element pasywny, który działa jak bardzo precyzyjny "przepust" dla sygnału radiowego. Jego zadaniem jest przepuszczenie tylko bardzo wąskiego, dokładnie określonego pasa częstotliwości (np. 869,44165 MHz w Twojej sieci), a skuteczne tłumenie wszystkich innych sygnałów, które mogą być zakłóceniami.

Stosuje się go w węzłach Meshtastic na 868 MHz, aby radia nie "słuchało" zakłóceń z innych pasm, np. z sieci komórkowej GSM 900 MHz. Filtr ten jest pasywny, czyli nie wzmacnia sygnału, ale poprawia jakość odbioru, tłumiąc szumy wtrąceniowe. W Twojej konfiguracji w województwie świętokrzyskim (preset Narrow Fast, 869,44165 MHz) taki filtr ma być montowany między moduł LoRa a antenę.

Ważne jest, aby przy montażu nie przekraczać dopuszczalnej mocy wejściowej filtra (zazwyczaj 27 dBm) i dbać o to, aby złącza nie były narażone na naprężenia, bo filtr jest bardzo wrażliwy. Jego tłumienie wtrąceniowe trzeba uwzględnić w bilansie łącza, ale dzięki wysokiej selektywności pozwala na lepsze odbieranie słabych sygnałów z innych węzłów w sieci.

## 31. Jaki jest rekord zasięgu LoRa?
Router: AssistantIntent(intent=web_question, node=, channel=, text=rekord zasięgu LoRa, role=, metric=, topic=, period=, amount=0)
Time: 56 s
Answer:
Rekord zasięgu w sieciach LoRa to około 330 km w warunkach idealnych. W praktyce, ze względu na teren i przeszkody, typowy zasięg pojedynczego węzła wynosi od 2 do 5 km, a na wsi z dobrą widocznością może osiągać 16 km i więcej. Istnieją jednak przypadki przekraczające 10 km, a w specyficznych warunkach atmosferycznych (np. zjawisko tropo ducting) notowano odległości rzędu 832 km, choć są to wyjątki.

Źródło: https://en.wikipedia.org/wiki/LoRa i https://en.wikipedia.org/wiki/Meshtastic (sprawdzono 2026-10)

## 32. Jak włączyć zapisywanie logów?
Router: AssistantIntent(intent=app_help, node=, channel=, text=zapisywanie logów, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
„Zbieranie logów” jest w: Ustawienia → Funkcje MT_SW.
Buttons: Zbieranie logów -> meshtastic://meshtastic/settings/mt-sw-features (blinks: Zbieranie logów)

## 33. Który model AI wybrać na telefon?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 69 s
Answer:
W aplikacji MT_SW nie ma jednego zdefiniowanego modelu AI do wyboru. Zamiast tego, aplikacja pobiera model z internetu przy pierwszym uruchomieniu i używa go na własną odpowiedzialność, zgodnie z jego licencją.

Dostępne modele to Whisper (do dyktowania) oraz Gemma E2B i E4B (do dyktowania i rozmów). Wszystkie działają na jednym silniku llama.cpp wbudowanym w aplikację. Na telefonie obliczenia wykonuje procesor, na komputerze model może być dzielony między kartę graficzną a procesor.

Warto wiedzieć, że na Androidzie w wersji Google dostępny jest asystent Chirpy, który używa modelu Gemini Nano i działa w pełni offline na Twoim urządzeniu. W innych kompilacjach (np. F-Droid) lub na komputerach przycisk asystenta nie pojawia się, ponieważ nie ma tam wbudowanego silnika do uruchamiania modeli.

Nie mogę podać rekomendacji, który model jest „najlepszy", ponieważ wybór zależy od Twojego sprzętu (czy telefon ma wystarczającą pamięć RAM i moc procesora) oraz od tego, co dokładnie chcesz robić (tylko dyktowanie, czy też rozmowy). Jeśli potrzebujesz porównania telefonów z funkcjami AI, mogę podać informacje z internetu, ale pamiętaj, że są to tylko ogólnie dostępne dane.

## 34. Jak wyłączyć ikonę sąsiadów?
Router: AssistantIntent(intent=app_help, node=, channel=, text=wyłączenie ikony sąsiadów, role=, metric=, topic=, period=, amount=0)
Time: 2 s
Answer:
Z instrukcji aplikacji:
(Włączanie Informacji o sąsiadach)
1. Przejdź do Ustawienia → Ustawienia modułu → Informacje o sąsiadach.
3. Ustaw Częstotliwość aktualizacji. Aplikacja przyjmuje wszystko, co wpiszesz; firmware wymusza własne minimum i resetuje wartość poniżej niego.
4. Włącz Nadaj przez LoRa. Bez tego Twoja lista sąsiadów trafia tylko do MQTT i do tej aplikacji, nigdy drogą radiową.

## 35. Jak podłączyć moduł GPS?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 94 s
Answer:
Aby podłączyć moduł GPS do węzła MT_SW, musisz najpierw sprawdzić, czy Twój sprzęt fizycznie obsługuje GPS. Wiele popularnych urządzeń, takich jak Heltec LoRa32 czy MeshPocket, ma GPS wbudowany w płytce, więc nie trzeba nic podłączać. Jeśli jednak masz np. RAK WisBlock, T-Beam lub własną płytę, musisz dodać moduł GPS jako osobny element.

Najczęstszy sposób to podłączenie modułu przez port szeregowy (UART). Moduł GPS wysyła dane w formacie NMEA (np. $GPGGA), które MT_SW odczytuje automatycznie.

Kluczowe jest poprawne podłączenie napięcia. Większość modułów GPS (np. NEO-6M, NEO-M8N) wymaga zasilania 3,3 V lub 5 V. Jeśli Twój węzeł (np. nRF52840) pracuje na 3,3 V, a moduł GPS wymaga 5 V, musisz użyć adaptera z logicznym poziomem 3,3 V. Podłączenie 5 V bezpośrednio do nRF52840 może go uszkodzić.

Przykładowe piny do podłączenia zależą od modelu:
•   Dla T-Beam v1.1: TX modułu GPS podpinasz do piny 14 (TX), a RX do piny 13 (RX).
•   Dla RAK4631 na płycie RAK19007: TXD1 (pin 16) do RXD1 (pin 15). W tym przypadku w ustawieniach firmwareu trzeba zaznaczyć opcję "GPS Mode = Not_Present", ponieważ GPS jest podłączony zewnętrznie.
•   Dla T1000-E: TX do piny 16, RX do piny 17.

Po fizycznym podłączeniu w aplikacji MT_SW w sekcji "Ustawienia — Radio i użytkownik: Konfiguracja pozycji" musisz podać numery pinów w polach "GPS EN GPIO", "GPS Rx GPIO" i "GPS Tx GPIO". Jeśli Twój sprzęt ma GPS wbudowany, te pola zostawiasz na zero.

Warto też pamiętać, że moduł GPS może być wrażliwy na zakłócenia. Jeśli dane nie przychodzą, sprawdź czy nie masz zbyt dużej mocy nadawania w pobliżu, czy antena nie jest uszkodzona, oraz czy nie masz zasilania 5V gdzie powinno być 3,3V.

## 36. Jak zgłosić błąd w aplikacji?
Router: AssistantIntent(intent=app_help, node=, channel=, text=zgłoszenie błędu, role=, metric=, topic=, period=, amount=0)
Time: 1 s
Answer:
„Zbieranie logów” jest w: Ustawienia → Funkcje MT_SW.
Buttons: Zbieranie logów -> meshtastic://meshtastic/settings/mt-sw-features (blinks: Zbieranie logów)

## 37. What is a hop limit and what value should I use?
Router: AssistantIntent(intent=app_help, node=, channel=, text=hop limit, role=, metric=, topic=, period=, amount=0) + AssistantIntent(intent=app_help, node=, channel=, text=hop limit, role=, metric=, topic=, period=, amount=0)
Time: 79 s
Answer:
W kontekście Twojej sieci MT_SW i projektu Meshtastic, hop limit (limit przeskoków) to liczba, do której węzeł może być przekazywany przez sieć przed tym, gdy jego pakiet zostanie odrzucony.

Wyobraź sobie to tak: gdy wysyłasz wiadomość, ona musi „przeskoczyć” przez kolejne węzły, dopóki nie trafi do odbiorcy. Hop limit mówi, ile tych przeskoków jest dozwolonych. W Meshtastic (i w MT_SW) to pole jest zapisane jako 3 bity, co oznacza, że maksymalna wartość to 7. Domyślnie w oficjalnej sieci i w MT_SW ustawione jest na 3.

Dlaczego to ważne?
Gdy limit jest wysoki, wiadomości mogą podróżować bardzo daleko, ale wtedy sieć staje się bardzo „głęboka" i zajmuje dużo czasu antenowego (airtime). Każdy węzeł, który odbiera takie wiadomości, musi je przetwarzać, co może zalewać kanał i spowalniać całą sieć. W gęstej sieci lokalnej, jak ta w Świętokrzyskim regionie, lepiej zostawić limit na 3. Jeśli chcesz dotrzeć do czegoś dalej, możesz zwiększyć limit tylko na węzłach brzegowych (tych, które są na krawędzi Twojej sieci), a nie na tych w środku.

Warto też pamiętać, że w Meshtastic nie ma tak dużych limitów jak w niektórych innych protokołach (np. 64 przeskoków w starszych opisach), więc burza rozgłoszeniowa w Twojej sieci będzie miała mniejszy zasięg niż w teoretycznych przykładach z internetu.

Źródła:
• Baza wiedzy: Algorytm sieci, aplikacja Android w użyciu, Heltec V3 i V4, integracje: Hop limit, potwierdzenia i ponowienia
• Baza wiedzy: Dobre praktyki konfiguracji sieci: Ile ustawić hop limit?
• Baza wiedzy: Ustawienia LoRa w Meshtastic: pole po polu: Hop limit, moc nadawania i włączenie nadawania
• Baza wiedzy: Co z artykułu o sieciach lokalnych dotyczy naszej sieci MT_SW: Czym różni się limit przeskoków w artykule od limitu w Meshtastic?
• Baza wiedzy: Routing od strony firmware: nagłówek, historia pakietów, opóźnienia: Jak zbudowany jest nagłówek radiowy pakietu?
• Baza wiedzy: Jak działa sieć: routing, retransmisje, ACK, szyfrowanie: Jak sieć przekazuje wiadomości (managed flooding)?
• Baza wiedzy: Dlaczego sieci LoRa (Meshtastic, MeshCore) są lokalne, a nie globalne: Czym jest burza rozgłoszeniowa i dlaczego psuje dużą sieć?

“Limit przeskoków” is in: Settings → LoRa.
Buttons: Limit przeskoków -> meshtastic://meshtastic/settings/lora (blinks: Limit przeskoków)

## 38. Cześć, jak się masz?
Router: AssistantIntent(intent=chat, node=, channel=, text=, role=, metric=, topic=, period=, amount=0)
Time: 35 s
Answer:
Cześć! Ja jestem asystentem w aplikacji MT_SW, więc mam się świetnie – działam płynnie na Twoim telefonie i komputerze, gotowa do pomocy w sieci mesh.

A Ty jak się masz? Jak Ci poszło dzisiaj z radiami? Czy sprawdzasz zasięgi, planujesz trasę lub może masz jakieś pytanie o ustawienia, aktualizacje czy diagnostykę?
