# Podstawowe pytania: definicje i krótkie instrukcje

### Co to jest NodeInfo?
NodeInfo to pakiet, w którym węzeł przedstawia się innym: podaje swoją nazwę długą i krótką, model sprzętu, rolę i klucz publiczny. Dzięki niemu inni widzą Twój węzeł na liście z nazwą zamiast samego numeru. Węzeł rozgłasza NodeInfo co jakiś czas sam, a inni mogą o niego poprosić. Jeśli ktoś w sieci widzi Twój węzeł tylko jako numer, to znaczy, że jego NodeInfo jeszcze do niego nie dotarł.
Słowa: NodeInfo, node info, informacje o węźle, nazwa węzła, przedstawienie węzła, co to NodeInfo, węzeł bez nazwy
Źródło: Baza wiedzy: podstawy Meshtastic (opracowanie własne, 2026-10)

### Co to jest traceroute (wyznaczanie trasy)?
Traceroute, po polsku wyznaczanie trasy, pokazuje drogę, jaką pakiet przechodzi przez sieć mesh od Twojego węzła do wybranego: przez które węzły-przekaźniki przeszedł i jaki był sygnał (SNR) na kolejnych odcinkach. Używasz go, gdy chcesz zobaczyć, którędy docierają wiadomości albo który odcinek trasy jest najsłabszy. Trasa wyznaczona raz nie musi być taka sama następnym razem, bo sieć wybiera drogę na bieżąco.
Słowa: traceroute, wyznaczanie trasy, trasa pakietu, przekaźniki, skoki, którędy idą pakiety, co to traceroute
Źródło: Baza wiedzy: podstawy Meshtastic (opracowanie własne, 2026-10)

### Jak zmienić nazwę mojego węzła?
Nazwę swojego węzła ustawiasz w ustawieniach radia, w sekcji użytkownika: Długa nazwa (wyświetlana, do 24 bajtów w firmware 2.8 lub nowszym) i Krótka nazwa (kilka znaków, widoczna na awatarze). Po zapisaniu radio rozgłasza nową nazwę innym. Jeśli chcesz zmienić nazwę cudzego węzła tylko u siebie, na liście węzłów nadaj mu nazwę własną: to zmienia nazwę tylko w Twojej aplikacji.
Słowa: zmienić nazwę węzła, długa nazwa, krótka nazwa, nazwa własna, user, nazwa mojego węzła, awatar
Źródło: Dokumentacja aplikacji: ustawienia radia, użytkownik (sprawdzono 2026-10)

### Jak wysłać logi twórcom?
Logi zbierzesz w Panelu debugowania (Ustawienia → Zaawansowane → Panel debugowania). Ma zakładki: Pakiety, Dzienniki aplikacji i Logi urządzenia, a każda ma własny przycisk eksportu, który tworzy plik .txt. Wyeksportowany plik dołącz do zgłoszenia przez Ustawienia → Zgłoś błąd lub pomysł (nasz Discord, grupy i sieć mesh). Na GitHub zgłaszasz tylko błędy, które występują także w oryginalnej aplikacji Meshtastic.
Słowa: logi, wysłać logi, eksport logów, panel debugowania, zgłoszenie błędu, dzienniki aplikacji, logi twórcom
Źródło: Dokumentacja aplikacji: dzienniki debugowania (sprawdzono 2026-10)

### Jak pobrać model AI?
Modele nie pobierają się same. Włącz wybraną funkcję AI (asystent, tłumacz albo dyktowanie) w Ustawienia → Funkcje MT_SW. Pod przełącznikiem pojawi się mały czerwony napis „Wybierz model do zainstalowania”. Stuknij go, wybierz model na liście na dole ekranu (osobne zakładki: Asystent, Tłumacz, Dyktowanie) i pobierz. Pobranie wymaga internetu, a potem funkcja działa na urządzeniu bez internetu. Przy modelach są podane rozmiary i zalecana pamięć, a dla asystenta polecamy rodzinę Gemma.
Słowa: pobrać model, model AI, instalacja modelu, wybierz model do zainstalowania, asystent, tłumacz, dyktowanie, Gemma
Źródło: Dokumentacja aplikacji: Funkcje MT_SW, inteligentne funkcje (sprawdzono 2026-10)

### Jak ustawić pozycję ręcznie?
Jeśli węzeł nie ma GPS albo stoi w jednym miejscu, możesz dać mu stałą pozycję (fixed position). W ustawieniach pozycji włącz stałą pozycję i wpisz szerokość oraz długość geograficzną albo użyj aktualnej pozycji telefonu. Węzeł rozgłasza wtedy zapisane współrzędne, a GPS (jeśli jest) może działać według swojego interwału.
Słowa: pozycja ręcznie, stała pozycja, fixed position, współrzędne, bez GPS, ustawić pozycję, szerokość, długość geograficzna
Źródło: Baza wiedzy: ustawienia zasilania i GPS (sprawdzono 2026-10)

### Co zrobić, gdy węzeł nie łączy się z telefonem?
Sprawdź po kolei: czy Bluetooth jest włączony w telefonie i w ustawieniach radia, czy węzeł nie jest już połączony z innym telefonem lub komputerem, i czy aplikacja ma uprawnienia do Bluetooth i urządzeń w pobliżu. Potem zapomnij sparowane urządzenie w ustawieniach Bluetooth telefonu, zrestartuj radio i sparuj ponownie, wpisując PIN z ekranu radia. Gdy nadal nie działa, spróbuj połączenia przez USB albo Wi-Fi.
Słowa: nie łączy się, połączenie z telefonem, Bluetooth, parowanie, PIN, zapomnij urządzenie, restart radia, uprawnienia
Źródło: Baza wiedzy: Bluetooth i połączenie z radiem (opracowanie własne, 2026-10)

### Czy to normalne, że węzeł grzeje się podczas ładowania?
Lekkie ciepło podczas ładowania akumulatora jest normalne. Jeśli obudowa robi się gorąca, że nie da się jej trzymać, akumulator puchnie albo czuć zapach, od razu odłącz ładowanie i nie używaj tego akumulatora. Ładuj w miejscu, w którym nic się nie zapali, i nie zostawiaj ładującego się węzła bez nadzoru na całą noc.
Słowa: grzeje się, gorący, ładowanie, akumulator, Li-ion, puchnięcie, nagrzewanie, bezpieczeństwo baterii
Źródło: Baza wiedzy: akumulatory Li-ion (wiedza ogólna, opracowanie własne, 2026-10)
