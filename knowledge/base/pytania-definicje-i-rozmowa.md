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

### Jak powiększyć interfejs aplikacji?
Rozmiar tekstu i elementów zmieniasz w ustawieniach aplikacji przez skalowanie aplikacji: od 50% do 200%, niezależnie od ustawień telefonu. Domyślnie jest „Zgodnie z systemem”. Skrót do skalowania jest też w Ustawienia → Funkcje MT_SW. Działa także w wersji na komputer.
Słowa: powiększyć interfejs, skalowanie aplikacji, rozmiar tekstu, rozmiar czcionki, wielkość czcionki, zmienić czcionkę, większa czcionka, zoom, mniejszy interfejs, skala
Źródło: Dokumentacja aplikacji: skalowanie aplikacji (sprawdzono 2026-10)

### Jak wyłączyć (lub włączyć) automatyczne ładowanie obrazków w czacie?
Podgląd obrazków z linków w czacie jest domyślnie wyłączony. Włączasz go i wyłączasz przełącznikiem w Ustawienia → Prywatność, a ten sam przełącznik jest też w Ustawienia → Funkcje MT_SW. Aplikacja nie wysyła zdjęć przez LoRa, tylko link: odbiorca widzi podgląd, jeśli ma tę funkcję włączoną. Przed wysłaniem zdjęcia jest ostrzeżenie, że serwer ze zdjęciem jest publiczny.
Słowa: obrazki w czacie, podgląd obrazków, automatyczne ładowanie obrazków, zdjęcia przez link, prywatność, wyłączyć podgląd
Źródło: Dokumentacja aplikacji: zdjęcia przez link, prywatność (sprawdzono 2026-10)

### Jak zmienić język aplikacji?
Język aplikacji wybierasz w ustawieniach aplikacji w pozycji Język (obok pozycji Motyw i Jednostki). Domyślnie aplikacja podąża za językiem telefonu. Jednostki zależą od regionu radia, a nie od języka wyświetlania.
Słowa: język aplikacji, zmienić język, język interfejsu, polski, angielski, język wyświetlania, language
Źródło: Dokumentacja aplikacji: ustawienia aplikacji, język (sprawdzono 2026-10)

### Jak podłączyć się do radia przez Wi-Fi (sieć)?
Najpierw podłącz radio do swojej sieci: połącz się z nim przez Bluetooth lub USB, otwórz Ustawienia → Konfiguracja urządzenia → Sieć i włącz Wi-Fi. Potem w zakładce Połącz wybierz Sieć. Radio znajdziesz przez skanowanie urządzeń w sieci lokalnej albo przez Dodaj urządzenie ręcznie: wpisz adres IP radia i port (domyślnie 4403). Telefon i radio muszą być w tej samej sieci lokalnej.
Słowa: Wi-Fi, WiFi, połączenie przez sieć, TCP, adres IP, port 4403, jak podłączyć przez Wi-Fi, sieć lokalna
Źródło: Dokumentacja aplikacji: połączenia, TCP/IP (sprawdzono 2026-10)

### Jak podłączyć się do radia przez USB?
Podłącz radio do telefonu kablem USB do transmisji danych: kable tylko do ładowania nie mają linii danych i radio się nie pojawi. Aplikacja poprosi o uprawnienie USB, dotknij Zezwól, a połączenie nawiąże się samo. Telefon musi obsługiwać OTG. Gdy uprawnienie zostało odrzucone, odłącz radio i podłącz je ponownie.
Słowa: USB, kabel USB, OTG, połączenie przez USB, uprawnienie USB, jak podłączyć przez USB, port szeregowy
Źródło: Dokumentacja aplikacji: połączenia, USB (sprawdzono 2026-10)

### Co to jest zdrowie sieci (Network Health)?
Zdrowie sieci to zakładka w dolnej nawigacji, między Węzłami a Mapą. Pokazuje w jednym miejscu, jak się mają wszystkie węzły: kartę Kondycja sieci z procentem węzłów w dobrym stanie, zakładki metryk (zasilanie, sygnał, eter, środowisko, zasoby, ruch, sąsiedzi) i kolory ocen (złoty dobrze, czerwony średnio, fioletowy źle, biały bardzo źle, niebieski brak danych). Jest wyszukiwanie, sortowanie, filtr Tylko problemy oraz podsumowanie sieci z rankingami, które można zapisać jako PDF, HTML, Markdown, tekst lub CSV.
Słowa: zdrowie sieci, Network Health, kondycja sieci, kolory ocen, ranking węzłów, podsumowanie sieci, zakładka zdrowie
Źródło: Dokumentacja aplikacji: Zdrowie sieci (sprawdzono 2026-10)

### Czym różni się MT_SW_APP od oryginalnej aplikacji Meshtastic?
MT_SW_APP to aplikacja oparta na oryginalnej aplikacji Meshtastic: robi wszystko, co oryginał, i ma własne dodatki. Są to m.in. Zdrowie sieci, Mesh Link Planer, kalkulator anten, połączenia na mapie, zdalne sterowanie GPIO, diagnostyka na żądanie, Sniffer, dodatkowa telemetria (statystyki lokalne), panel debugowania z logami urządzenia, skalowanie aplikacji, motywy, wersja na komputer i opcjonalne funkcje AI: asystent, tłumacz i dyktowanie. Jest zgodna z główną siecią Meshtastic.
Słowa: czym różni się aplikacja, różnice od oryginału, oryginalna aplikacja, MT_SW_APP, nowe funkcje, dodatki, fork
Źródło: Dokumentacja aplikacji: Funkcje MT_SW (sprawdzono 2026-10)

### Czy Meshtastic działa bez internetu?
Tak. Meshtastic to sieć radiowa LoRa: węzły przekazują wiadomości między sobą drogą radiową i do tego nie potrzeba internetu ani sieci komórkowej. Internet jest potrzebny tylko do rzeczy dodatkowych, np. mostu MQTT albo pobrania aktualizacji. Telefon łączy się z węzłem przez Bluetooth, USB lub sieć lokalną.
Słowa: bez internetu, offline, brak zasięgu GSM, działa bez sieci komórkowej, internet nie jest potrzebny, Meshtastic bez internetu
Źródło: Baza wiedzy: podstawy Meshtastic (opracowanie własne, 2026-10)

### Jak podłączyć wyświetlacz OLED do własnego węzła?
Typowy wyświetlacz OLED (np. SSD1306 lub SH1106) łączy się po I2C czterema przewodami: zasilanie 3,3 V, masa, SDA i SCL do pinów I2C płytki. Firmware wykrywa wyświetlacz sam, a jego zachowanie ustawiasz w ustawieniach ekranu (czas wygaszania, karuzela, jednostki). W gotowych płytkach (np. Heltec, T-Beam) wyświetlacz jest już wlutowany.
Słowa: OLED, wyświetlacz, ekran, I2C, SDA, SCL, SSD1306, SH1106, podłączyć wyświetlacz
Źródło: Baza wiedzy: budowa własnych węzłów (wiedza ogólna, opracowanie własne, 2026-10)

### Czy MQTT jest bezpieczne?
MQTT w Meshtastic przesyła wiadomości przez internet do brokera, więc ruch przechodzi przez serwer, którego nie kontrolujesz. Dla bezpieczeństwa używaj szyfrowania TLS i własnego, zabezpieczonego hasłem brokera, a na kanałach współdzielonych przez MQTT nie wysyłaj nic poufnego. Publiczny broker społeczności jest przeznaczony do ogólnych zastosowań i testów; połączenie z nim jest szyfrowane TLS (port 8883).
Słowa: MQTT, bezpieczeństwo MQTT, broker, TLS, publiczny broker, szyfrowanie MQTT, czy MQTT jest bezpieczne
Źródło: Baza wiedzy: MQTT (sprawdzono 2026-10)

### Jak zrobić kopię zapasową konfiguracji węzła?
W aplikacji otwórz Ustawienia → Kopia zapasowa i przywracanie. Eksport konfiguracji zapisuje całą konfigurację połączonego węzła do pliku, a Import konfiguracji wczytuje zapisany plik z powrotem. Zrób kopię przed większymi zmianami albo przed aktualizacją firmware.
Słowa: kopia zapasowa, backup, eksport konfiguracji, import konfiguracji, przywrócić konfigurację, zapisać ustawienia do pliku
Źródło: Dokumentacja aplikacji: kopia zapasowa i przywracanie (sprawdzono 2026-10)

### Co to jest Meshtastic?
Meshtastic to otwartoźródłowy projekt łączności radiowej bez internetu. Małe tanie radia LoRa (węzły) przekazują sobie wiadomości tekstowe i pozycje drogą radiową, tworząc sieć kratową (mesh), a telefon łączy się z węzłem przez Bluetooth, USB lub sieć lokalną i służy za klawiaturę oraz ekran. Projekt powstał na początku 2020 roku, a jego twórcą jest Kevin Hester. Zasięg jednego łącza to zwykle od kilkuset metrów w mieście do kilkudziesięciu kilometrów w otwartym terenie.
Słowa: Meshtastic, co to jest Meshtastic, czym jest Meshtastic, projekt, sieć mesh, LoRa, łączność bez internetu, węzły
Źródło: Baza wiedzy: podstawy Meshtastic (opracowanie własne, 2026-10)

### Co to jest sieć mesh (kratowa)?
Sieć mesh (kratowa) to sieć, w której każdy węzeł może przekazać cudzą wiadomość dalej: pakiet przeskakuje z węzła na węzeł, aż dotrze do celu. Żaden pojedynczy węzeł nie jest niezbędny, a sieć sama się konfiguruje. Dzięki temu zasięg całej sieci jest większy niż zasięg jednego radia, ale każdy dodatkowy przeskok zajmuje czas antenowy, dlatego liczbę przeskoków (hop limit) się ogranicza.
Słowa: sieć mesh, sieć kratowa, mesh, przeskoki, hop limit, przekazywanie pakietów, co to jest mesh
Źródło: Baza wiedzy: podstawy Meshtastic (opracowanie własne, 2026-10)

### Co to jest LoRa?
LoRa to technika radiowa dalekiego zasięgu i bardzo małego poboru prądu: sygnał jest rozpraszany w widmie, dzięki czemu odbiornik słyszy go nawet wtedy, gdy jest słabszy od szumu. Kosztem jest mała prędkość (od setek bitów do kilku kilobitów na sekundę), więc nadaje się do krótkich wiadomości i danych z czujników, a nie do zdjęć czy rozmów. Meshtastic używa LoRa w paśmie 868 MHz w Europie.
Słowa: LoRa, co to jest LoRa, spreading factor, zasięg, małe prędkości, pasmo 868, modulacja LoRa
Źródło: Baza wiedzy: parametry LoRa (opracowanie własne, 2026-10)
