# Changelog / Lista zmian

Dwujęzyczny plik zmian: najpierw **polska** część, niżej **angielska** (oryginał).
A bilingual changelog: the **Polish** part comes first, the **English** original below it.

* [Polski](#polski) — zmiany forka MT_SW względem oryginalnej aplikacji
* [English](#english) — MT_SW fork changes compared to the original app

---

# Polski

Ten plik opisuje wyłącznie zmiany wprowadzone w aplikacji MT_SW_APP **względem oryginalnej aplikacji** [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android). Zmiany z oryginalnych wydań upstreamu nie są tu powtarzane — ich pełną listę znajdziesz w [wydaniach oryginału](https://github.com/meshtastic/Meshtastic-Android/releases). Plik jest prowadzony ręcznie.

## Zmiany forka MT_SW

Wpisy niżej opisują wyłącznie to, co ten fork dokłada do oryginału, od początku (27 lipca 2026) do dziś, w kolejności chronologicznej. Opis funkcji: [README.md](README.md).

### Październik 2026

**Scalenie z oryginałem (15.10)**
* Scalenie z najnowszym oryginałem: kopia zapasowa i przywracanie danych aplikacji (wiadomości i węzły) w ustawieniach radia, ustawienia radia można otwierać i czytać bez połączenia z radiem (zapis wymaga połączenia), wybór koloru z gotowymi kolorami przy oświetleniu, ponawianie nieudanej wiadomości w tym samym miejscu, karta połączonego radia pokazuje preset LoRa i region, wymóg aktualizacji firmware (stare radio zostaje połączone, ale aplikacja prosi o aktualizację), przełączniki powiadomień o reakcjach i nowych waypointach na pulpicie, wyszukiwarka ustawień przechodzi do konkretnej opcji, nazwy nieznanych przeskoków w traceroute oraz aktualizacje bibliotek (Compose Multiplatform 1.12.2, markdown 0.7.18, kotest, Firebase) i tłumaczeń. Dopisane polskie tłumaczenia nowych tekstów. W forku zostają bez zmian: wyłączone automatyczne udostępnianie kontaktu i ulubione przy wiadomościach prywatnych (także przy ponawianiu), niższy próg firmware dla trybu blokady oraz kolory i podpisy SNR w traceroute. Biblioteka Bluetooth Kable zostaje przypięta do 0.44.3.

**Zdrowie sieci — filtr po ocenie**
* Kliknięcie w liczbę w karcie „Kondycja sieci” (np. „3 Źle”) pokazuje na liście tylko węzły z tą oceną; ponowne kliknięcie pokazuje wszystkie. Wybrana ocena jest podświetlona.
* Każda zakładka (Zasilanie, Sygnał, Eter…) pamięta swój filtr osobno, więc po przełączeniu zakładek każda zachowuje własny wybór.

**Zdrowie sieci — kolor szumu tła**
* Zakładka Sygnał: liczba (szum tła) ma teraz własny kolor — im niższa wartość (większy minus), tym lepiej. Progi jak w „Jakości sygnału”: poniżej −100 dBm złoty, od −100 do −90 dBm czerwony, od −90 do −80 dBm fioletowy, wyżej biały. Pasek, tło wiersza, wykres i ocena „Kondycji sieci” wynikają z tej samej wartości; dopiero gdy węzeł nie wysyła szumu, ocenia go SNR.
* Szum jest oceniany po korekcie LNA węzła (wcześniej kolor liczył się z wartości przed korektą, więc nie zgadzał się z tym, co widać). Dotyczy listy, szczegółów węzła i wykresu z progami.

**Sygnał WiFi urządzenia w „Jakości sygnału” i „Zdrowiu sieci”**
* Aplikacja odczytuje nowe pola firmware: siła WiFi (RSSI), liczba zerwań łącza WiFi od startu i kod powodu ostatniego zerwania (tylko ESP32).
* „Jakość sygnału”: osobna linia WiFi RSSI na wykresie obok szumu, RSSI i SNR (z legendą i opisem pod (i)) oraz karty w liście z siłą sygnału, liczbą zerwań i powodem zapisanym po ludzku (np. „utracono punkt dostępowy (200)”); brak siły sygnału przy rosnącej liczbie zerwań jest pokazany jako „WiFi rozłączone”.
* „Zdrowie sieci”, zakładka Sygnał: w szczegółach węzła dochodzą wykresy „WiFi RSSI” i „Zerwania WiFi”.
* „Diagnostyka na żądanie”: odpowiedź ze statystykami węzła pokazuje też WiFi RSSI, zerwania i kod powodu.
* Przycisk „Zapisz WiFi” w „Jakości sygnału” eksportuje WiFi do CSV (data, godzina, siła, zerwania, kod powodu).
* Statystyki hosta (procesor, pamięć) zostają bez zmian — WiFi jest tylko w sygnale.

**Pomoc i dokumentacja po polsku**
* Cała dokumentacja w aplikacji (Przewodnik użytkownika i Przewodnik dewelopera, 36 stron) przetłumaczona na polski, wraz z nazwami stron; strona dokumentacji (docs/pl) też jest teraz w całości po polsku.
* Opis wyboru presetu mówi, że domyślnym presetem dla MT_SW_FIRMWARE jest Wąski szybki (Narrow Fast).
* Strony pomocy mają mniejsze nagłówki i przyciski „A−” / „A+” w górnym pasku do zmiany wielkości tekstu.

**Zdrowie sieci — nowy wygląd**
* Na górze karta „Kondycja sieci”: kolorowy pierścień, liczba węzłów w każdym stanie i ile jest online.
* Kolory jak w całej aplikacji: złoty = dobrze, czerwony = średnio, fioletowy = źle, biały = bardzo źle.
* Wiersze z kolorowym paskiem, wykresem z wypełnieniem, wartością i słowem oceny; ikony zakładek w kolorze najgorszego węzła.
* Filtr „Tylko problemy”, prawdziwe ikony zamiast emotikon oraz przycisk (i) z opisem kolorów i progów.
* Poziom baterii na liście węzłów: pełna = złoty, średnia = czerwony, słaba = fioletowy.

**Podsumowanie sieci i szczegóły węzła — nowy wygląd**
* Podsumowanie ma kartę z kołowym wykresem online/offline, kolorowe kafelki, wykresy kołowe i słupkowe, rozkład baterii i sygnału oraz wykres wiadomości z 7 dni.
* Rankingi (najlepszy sygnał, najdłużej działające, najbliżej itd.) pokazane jako paski.
* Podsumowanie można udostępnić albo zapisać jako PDF, stronę HTML, Markdown, tekst lub CSV (arkusz).
* Szczegóły węzła: karta z ikoną, wartością w kolorze oceny i wykresem trendu oraz kolorowe karty z min, max i ostatnią wartością.
* Szczegóły węzła: nowe wykresy (gładka linia, gradient, siatka, dotknięcie pokazuje wartość i godzinę), a ikona ma świecący pierścień wskaźnika.
* Zdrowie sieci: Sąsiedzi, Ruch i Zasoby pokazują dane zapisane wcześniej (także po ponownym uruchomieniu), a własne urządzenie jest liczone w Sąsiadach.
* Szczegóły węzła: cztery sposoby pokazania tych samych danych do wyboru — Linia (z kolorowymi strefami), Zegar, Słupki i Mapa cieplna (godziny × dni) — oraz pasek „Czas w każdym stanie” dla baterii, SNR i zajęcia kanału.
* Zakładka Sygnał pokazuje jako główną wartość poziom szumu (noise floor) po korekcie LNA; kolor nadal wynika z SNR.

**Szczegóły węzła i diagnostyka — nowy wygląd**
* Wykresy w szczegółach węzła można oglądać na kilka sposobów (linia, wskaźnik, słupki, mapa cieplna); każdy wybiera ten, który jest dla niego czytelniejszy.
* Sąsiedzi, ruch i zasoby czytają dane z zapisanych logów (także po restarcie aplikacji); w Sygnale główna wartość to szum tła po korekcie LNA.
* Kolory sygnału liczone z SNR (a gdy go brak — z szumu tła), także dla węzłów, których nie słychać bezpośrednio; „Kondycja sieci” też to uwzględnia.
* Pole GPIO i szybkie komendy (/ping, /hello, /test) mają kafelki, sprawdzanie numeru pinu, przyciski „Włącz (1)” / „Wyłącz (0)”, czytelny opis z odstępami i opis każdej komendy z informacją, że działają od firmware 2.8.0.
* Zdalne zarządzanie (ulubione, ignorowane, dodawanie kontaktu) ma kafelki, sprawdzanie ID węzła i krótkie wyjaśnienia.
* Diagnostyka na żądanie: karta z przeglądem wszystkich możliwości i licznikiem sprawdzonych, a każde zapytanie ma opis, przycisk „Zapytaj” oraz wynik w kolorach, jako kafelki, wskaźniki, wykresy kołowe i słupkowe.
* Przełącznik „Odwróć znak prądu” dla każdego węzła z osobna (także własnego), w oknie informacji (ikona „i”) statystyk zasilania i pogody, tak jak LNA: gdy czujnik jest podłączony odwrotnie, prąd ładowania pokazuje się na plusie na liście węzłów, w szczegółach i w Zdrowiu sieci. Zmienia się tylko znak wyświetlanego prądu i wykresów — nie wpływa to na wykrywanie ładowania ani na nic innego.
* Węzeł, który w danych pogodowych wysyła tylko napięcie i prąd, ma teraz ikonę wejścia w zapisane ramki (wcześniej pojawiała się dopiero z temperaturą, wilgotnością lub burzami).
* Pole wzmocnienia LNA jest teraz w szczegółach sygnału także dla własnego węzła (można je ustawić tam i w ustawieniach LoRa).
* Na liście rozmów tekst nazwy głównego kanału ma kolor motywu aplikacji, pozostałych kanałów czerwony (zamiast białego), a wiadomości prywatnych w kolorze węzła.
* Kłódki przy kanałach w kolorach aplikacji: złota = bezpieczny kanał, czerwona otwarta = publiczny bez precyzyjnej lokalizacji, fioletowa otwarta = publiczny z precyzyjną lokalizacją (z białym ostrzeżeniem, gdy idzie też przez MQTT); opisy w pomocy dopasowane.
* Osobny przełącznik „Odwróć wskazanie zasilania / baterii” (w oknie informacji metryk urządzenia): dla modułu pomiarowego podłączonego odwrotnie zamienia „zasilanie” z „procent baterii” na liście węzłów, w szczegółach, w Zdrowiu sieci i w podsumowaniu. Procent jest szacowany z napięcia. Zmienia tylko to wskazanie; znak prądu ma swój osobny przełącznik.
* Statystyki pogodowe: przełącznik „Pogoda / Zasilanie” nad wykresem. „Zasilanie” pokazuje napięcie i prąd czujnika takim samym wykresem jak w metrykach zasilania (dla czujnika, który podaje tylko napięcie i prąd, jest od razu włączone).
* Diagnostyka na żądanie: udostępnianie i zapis wszystkich odebranych odpowiedzi do PDF, strony HTML, Markdown, tekstu i CSV (arkusz).
* Pomoc listy węzłów (w aplikacji i w dokumentacji) ma sekcję „Poziom baterii”: wskaźniki w kolorach złotym, czerwonym i fioletowym, zasilanie zewnętrzne i brak danych, a dokumentacja także kolory w „Zdrowiu sieci”.

**Okna informacyjne**
* Okna informacyjne w aplikacji pojawiają się rzadziej, w losowych odstępach; termin jest zapamiętywany trwale.
* Przy zdalnej konfiguracji urządzenia pokazuje się osobne okno, zależne od lokalizacji.
* Przepisane, spokojniejsze i bardziej informacyjne teksty okien.
* Powtarzające się okno powitalne pojawia się około raz na dwa dni, o losowej porze.

**Scalenie z oryginałem (14.10)**
* Scalenie z najnowszym oryginałem: aktualizacja biblioteki z listą licencji (aboutlibraries 15.3.0), aktualizacja biblioteki do wyświetlania stron pomocy (markdown 0.46.0) i szyfrowania kluczy (bouncycastle 1.86), odświeżone profile startowe, odświeżona lista sprzętu i linków do urządzeń (m.in. Seeed Wio Tracker L2 jest teraz aktywnie wspierany) oraz tłumaczenia. Biblioteka Bluetooth Kable zostaje przypięta do 0.44.3 (0.45.0 nadal jest najnowsza i psuje skanowanie BLE w Windows). Bez zmian w działaniu pozostałych funkcji.

**Scalenie z oryginałem (13.10)**
* Scalenie z najnowszym oryginałem: poprawka skanowania Discovery (presety publiczne skanowane na domyślnym kanale publicznym i slocie, potem przywracany własny kanał i ustawienia LoRa), poprawka działania mapy na pulpicie (LWJGL w wersji z ProGuard), logi mapy w jednym miejscu, aktualizacje bibliotek (paging, markdown, mapy Google) i tłumaczeń. Biblioteka Bluetooth Kable zostaje przypięta do 0.44.3 (nowsza psuje skanowanie BLE w Windows). Bez zmian w działaniu pozostałych funkcji.

**Scalenie z oryginałem (12.10)**
* Scalenie z najnowszym oryginałem Meshtastic-Android: nowsza biblioteka map (maplibre-compose 0.20) z dopasowaniem naszych warstw mapy, planera i map offline oraz drobne poprawki z upstreamu; bez zmian w działaniu aplikacji.
* Nazwy presetów modemu po polsku, a w nawiasie angielska nazwa, np. „Szeroki daleki szybki (Long Fast)”; dopisek „Szeroki” oznacza kanał 250 kHz, „Pośredni” kanał 125 kHz (Lite to teraz „Pośredni”), „Wąski” 62,5 kHz, „XXL” 500 kHz (turbo).
* Opis pod przełącznikiem „Ok dla MQTT” w ustawieniach LoRa, tak jak pod pozostałymi (z zaleceniem, żeby było włączone, by dało się monitorować sieć); opis domyślnego presetu w wyborze presetu po polsku z angielską nazwą w nawiasie.

**Mesh Link Planer (natywny planer zasięgu, 1–2.10)**
* Natywny planer zamiast zewnętrznego Site Plannera, w całości w aplikacji (Android i desktop): dwa niezależne punkty A/B (lista węzłów, mapa, współrzędne, stacja), teren Mapterhorn, ITM/Longley-Rice, strefa Fresnela, wykres profilu, bilans łącza, namiary i kąty anten, porównanie z ostatnim pomiarem, zasięg dookólny, eksport PDF/CSV/KML/GeoJSON/PNG, okna informacji „i”, podziękowania i licencje.
* Kreator kabli i złączy (baza z kart katalogowych, złącza bez kabla, np. przejściówki SMA→N), wszystkie pasma od 169 MHz do 2,4 GHz i dowolna częstotliwość; wybór pasma ustawia częstotliwość i szerokość kanału (2,4 GHz jako szeroki LoRa, pierwszy slot dla 433 i 470 MHz).
* Domyślnie preset Narrow Fast i częstotliwość 869,44165 MHz; presety bez ręcznego BW i SF; zmiana presetu ustawia jego domyślną częstotliwość.
* Zasięg na mapie jako gęsty raster z płynną skalą kolorów (styl MeshMap Planner), suwak krycia, lista warstw u góry z przewijaniem, licznik warstw na ikonie, limit 9 warstw, warstwy zasięgu nie są zapamiętywane po restarcie. Nazwa „Mesh Link Planer (by MT_SW)”.
* Opcjonalna pogoda na żywo (Open-Meteo): współczynnik k, refrakcja, ducting.
* Przeszkody: presety zabudowy/roślinności (domyślnie) albo opcjonalne **dokładne odwzorowanie terenu z OpenStreetMap** (budynki i lasy przez Overpass API, na trasie i w zasięgu do wybranego promienia od środka), z ostrzeżeniem o większym zużyciu zasobów i dłuższym liczeniu; przy błędzie pobierania powrót do presetu.
* Menu zasięgu danych o terenie (5 / 10 / 15 / 30 / 50 / 100 km, domyślnie 30 km); pobieranie z OpenStreetMap jest czytane kawałkami z limitem rozmiaru, a brak pamięci jest łapany, więc gęsty obszar nie wywala już aplikacji (także na emulatorze z małą pamięcią), tylko pokazuje komunikat „za dużo danych, zmniejsz zasięg” i wraca do ustawienia wstępnego przeszkód, także podczas liczenia zasięgu.
* Naprawa „zabrakło pamięci” przy dokładnym odwzorowaniu terenu: limit pamięci serwera Overpass w zapytaniu (wcześniej błędnie obniżony do 16 MB) wrócił do bezpiecznej wartości, więc serwer nie odmawia już nawet dla małego obszaru; odmowa serwera („out of memory”, przeciążenie, HTTP 429/504) ma teraz osobny komunikat zamiast „urządzeniu zabrakło pamięci”, z krótkim powodem podanym przez serwer; przy odmowie planer próbuje zapasowych serwerów Overpass.
* Zasięg danych o terenie powyżej 10 km (50 i 100 km): obszar jest pobierany kawałkami (kafelki po ok. 30 km, jedno zapytanie naraz) zamiast jednej ogromnej odpowiedzi, której nie udźwignąłby serwer ani urządzenie; kafelek zbyt ciężki dla serwera jest dzielony na cztery, zajęty serwer jest cierpliwie czekany, obiekty na granicach kafelków liczą się raz, punkty obrysów są przerzedzane (od 25 m, mocniej im większy zasięg, a drobne lasy poniżej tej skali pomijane), żeby oszczędzić pamięć, a ostatni działający serwer jest zapamiętywany. Zapytanie o tereny używa dokładnych dopasowań (`landuse=forest`, `residential` itd.) zamiast wyrażenia regularnego, więc serwer nie czyta już wszystkich pól i łąk w kafelku i odpowiada znacznie szybciej. Całe pobieranie ma limit 12 minut i kończy się całością albo błędem (bez „połowicznych” map). Postęp (fragment x z y) widać pod paskiem zasięgu, a długie pobieranie nie blokuje już zwykłego liczenia łącza A–B. Zapytania do Overpass mają własne limity (90 s oczekiwania na dane zamiast 30 s i bez automatycznego ponawiania, które powtarzało ciężkie zapytania); przekroczenie czasu oznacza „kafelek za ciężki” i dzieli go na mniejsze, a chwilowy zanik sieci jest ponawiany. Zapytanie o przeszkody na trasie łącza jest dzielone na lżejsze części (lasy, potem budynki przy każdym końcu) z limitem serwera 60 s i jedną ponowną próbą, bo jedno ciężkie zapytanie o długie łącze przekraczało limit czasu serwera; najmniejszy kafelek zasięgu, na którym serwer przekroczył czas, jest ponawiany po krótkim oczekiwaniu.
* Komunikat pod ustawieniami dokładnego terenu podaje teraz wybrany zasięg danych zamiast stałych „30 km”.
* Okno traceroute: gdy trasy nie da się pokazać na mapie, jest jeden przycisk OK (wcześniej obok pojawiał się drugi); opisy trasy na mapie mają cieńszą obwódkę i większą czcionkę.

**Sniffer, klucze, coding rate (8.10)**
* Ustawienia: nowa pozycja „Zgłoś błąd lub pomysł” otwiera okno „Masz pomysł albo znalazłeś błąd?” — klikalne odnośniki do serwera Discord oraz grup WhatsApp i Signal, informacja, że można też napisać przez sieć mesh w województwie świętokrzyskim, i podpowiedź, co podać przy zgłaszaniu błędu.
* Powiadomienia o nowych wersjach aplikacji: dwa razy dziennie sprawdzanie wydań MT-SW/MT_SW_APP na GitHubie (telefon i komputer), powiadomienie z linkiem do listy wydań od razu po nowej wersji i przypomnienie co dwa dni do czasu aktualizacji. Wersje testowe tylko po włączeniu „Także wersje testowe”; przełącznik „Powiadamiaj o nowych wersjach” wyłącza całość. Zastępuje stare sprawdzanie aktualizacji desktopa, które pytało o wydania oryginału. Powiadamianie działa od wersji V3, więc o wersji V4 i nowszych. Pliki komputerowe budowane na GitHubie (Windows, Linux, macOS) są teraz wersjami wydaniowymi, dzięki czemu sprawdzanie aktualizacji działa też na komputerze. Na komputerze pierwsze sprawdzenie jest minutę po uruchomieniu, a kliknięcie w powiadomienie otwiera aplikację z oknem i przyciskiem „Pokaż listę wydań”. Zmiana przełącznika sprawdza od razu.
* Windows: instalator (.msi/.exe) ma w środku wyższy numer przy każdej zmianie kodu, więc można go zainstalować na starą wersję bez odinstalowywania; aplikacja i nazwa pliku dalej pokazują zwykłą wersję (np. 2.8.3).
* Info o sąsiadach: interwał aktualizacji wybierasz z listy godzin (6, 8, 10, 12, 18, 24) zamiast wpisywać sekundy.
* Scalenie z oryginałem (10.10): tylko techniczne aktualizacje (biblioteka sprawdzająca styl kodu, profil startowy) — bez zmian w działaniu aplikacji.
* Scalenie z oryginałem (11.10): tylko drobne aktualizacje bibliotek (wyszukiwanie urządzeń w sieci), poprawione komentarze w kodzie Bluetooth i profil startowy — bez zmian w działaniu aplikacji.
* Sniffer: po ponownym połączeniu stan jest odczytywany od nowa — aplikacja nie pokazuje już starej odpowiedzi z poprzedniego połączenia i dopytuje radio kilka razy, aż odpowie.
* Sniffer: zapamiętany wybór „Radio” nie jest już wierzony, dopóki podłączone radio w tym połączeniu nie potwierdzi, że sniffer jest włączony — po resecie radia, zmianie urządzenia albo przy firmware bez snifera panel od razu przestaje pokazywać i zbierać pakiety (wcześniej robił to dopiero po kilkudziesięciu sekundach).
* Desktop: w ustawieniach Zabezpieczenia są teraz przyciski „Kopia kluczy”, „Przywróć klucze” i „Usuń kopię kluczy” — tak samo jak w aplikacji na telefon.
* Coding rate: dla presetów Narrow (Fast/Slow) i Tiny Slow, które domyślnie mają 4/6, na liście dostępne jest także 4/5.
* Ustawienia LoRa: nowe pole „Przesunięcie częstotliwości (kHz)” — podajesz przesunięcie w kHz (np. 12,5), a aplikacja sama przelicza je na MHz dla radia.
* Coding rate: naprawiony powrót do „domyślne dla presetu” — po zapisie i ponownym połączeniu pokazywało się 4/5, bo radio samo wpisuje 5; teraz domyślna wartość jest zapisywana jako własny coding rate presetu (4/6) i nie myli się z wyborem 4/5.
* Mapy offline: pobieranie działa teraz także dla domyślnej mapy OpenStreetMap i pozostałych map rastrowych (wcześniej tylko dla stylów wektorowych).
* Budowanie na GitHubie (Actions → „Build Binaries (Manual)”): każdy system i format wybierasz osobno — Windows (.msi, .exe, .jar), Linux (.deb, .rpm, .AppImage, .jar), macOS (.dmg, .jar) i Android (APK).

**Mapa**
* Płynne przewijanie i powiększanie przy dużej liczbie węzłów: usunięte z danych mapy pola „ostatnio słyszany”/„online”, węzły odświeżane tylko przy zmianie pozycji, nazwy, ulubionego i ignorowanego, błysk na własnym małym źródle w 15 krokach, stabilny obszar widoku podczas przesuwania, pamięć podręczna obrazków plakietek, brak niewidocznych obrysów w warstwie zasięgu.
* **Trasa traceroute na mapie z siłą sygnału:** przycisk „Pokaż na mapie” tylko gdy wszystkie węzły trasy mają lokalizację (inaczej okno tłumaczy dlaczego); osobna cienka linia na skok w kolorze jakości sygnału, ze strzałką na końcu i SNR w dB wzdłuż linii, kończąca się tuż przed węzłem (węzły jako punkty pod plakietkami), oba kierunki symetrycznie obok siebie; legenda jakości; świeżo otrzymany wynik traceroute od razu ma wartości SNR (wcześniej linie były szare do otwarcia z zapisanych tras).

**Sieć**
* Lista ostatnio używanych urządzeń sieciowych (WiFi/TCP): 20 wpisów zamiast 3.

**Opisy i nazewnictwo (4.10)**
* **Opis przy sterowaniu GPIO pod ikoną informacji:** nagłówek karty GPIO w szczegółach węzła ma ikonę „i”, która otwiera krótką instrukcję krok po kroku — moduł Zdalny sprzęt i dostępne piny na węźle docelowym, kanał „gpio” na pozycji 1 z tym samym kluczem na obu urządzeniach, znaczenie przycisków 1 / 0 / Odczyt.
* **Jednolita nazwa „Diagnostyka na żądanie”** w całej aplikacji (sniffer, nazwa portu w pakietach, opisy) zamiast „OnDemand”.

**Węzły, czyszczenie bazy i scalenie z upstreamem (3.10)**
* **Czyszczenie węzłów przez Bluetooth działa do końca:** polecenia „usuń węzeł” do radia są wysyłane po kolei, z krótką przerwą i ponawiane, gdy łącze chwilowo ich nie przyjmuje (Bluetooth przyjmuje naraz tylko kilka zapisów, więc wcześniej przy setkach węzłów większość poleceń była po cichu odrzucana, a radio oddawało te węzły po ponownym połączeniu). Wysyłanie kończy się nawet po wyjściu z ekranu; gdy radio nie jest połączone, czyszczone jest tylko to, co w aplikacji. Przez WiFi działało dobrze już wcześniej.
* **Czyszczenie wszystkich nieznanych węzłów bez względu na datę:** po włączeniu „Wyczyść tylko nieznane węzły” pojawia się przełącznik „Ignoruj datę: wszystkie nieznane węzły bez klucza” (węzły, które się nie przedstawiły i dla których nie ma klucza; ulubione i ignorowane zawsze zostają).
* **Czyszczenie węzłów z niezgodnym kluczem:** nowy przełącznik „Tylko węzły z niezgodnym kluczem (usuwane z aplikacji i z radia)” na ekranie czyszczenia — lista pokazuje wszystkie węzły ze stanem „niezgodny klucz” bez względu na wiek (ulubione, ignorowane i własny węzeł zostają), a czyszczenie usuwa je z bazy aplikacji i z radia.
* **Czyszczenie samej bazy węzłów w aplikacji** (bez kontaktu z radiem, działa też bez połączenia): przycisk na dole ekranu czyszczenia z potwierdzeniem i przełącznikiem „Zachowaj ulubione węzły”; po jego wyłączeniu lista idzie do zera. Podłączone radio wyśle swoje węzły ponownie przy następnej synchronizacji.
* **Płynniejszy pasek szybkiego przewijania (desktop):** nie przelicza już całego ekranu przy każdej klatce przewijania, suwak sunie razem z listą (zamiast skakać co element), a przeciąganie przesuwa listę ciągle, w pikselach.
* **Scalenie z upstreamem (meshtastic/Meshtastic-Android):** protobufy 2.8.1 i nowe zależności, nowe funkcje upstreamu (m.in. zdalny terminal, powiadomienia o reakcjach, stronicowane logi, `SendMessageOutcome`), nowe teksty przetłumaczone na polski; nasze tłumaczenia, branding, planer i pasek przewijania zostały zachowane. Polecenie formatowania czasu i rozmiarów przeszło na nowe funkcje upstreamu.

**Dokumentacja**
* README i CHANGELOG uzupełnione o całą historię forka.

### Wrzesień 2026

**Jakość sygnału, LNA, logi (29–30.09, 1.10)**
* Korekta wzmocnienia LNA dla szumu i RSSI (pole w LoRa pod mocą radia i w oknie „Jakość sygnału”, osobne pole dla obcych węzłów w szczegółach węzła, wykresy, Zdrowie sieci, średni szum w podsumowaniu).
* Ocena SNR względem presetu (Narrow −3/−7/−12 dB, Lite −5/−10/−15 dB), RSSI może tylko obniżyć ocenę; kolory jak w statusach połączenia (dobry złoty, wystarczający czerwony, słaby fioletowy, brak biały/czarny zależnie od motywu); opis z progami zależnymi od presetu; kolorowanie SNR w szczegółach pakietów sąsiadów i w traceroute.
* Kolory statusów wiadomości (dostarczono, potwierdzono, błąd, tarcza podpisu na złoto).
* Logi urządzenia w panelu debug (na żywo, wyszukiwanie, eksport; Wi-Fi i USB tylko z firmware MT_SW), czas z ramki w snifferze, przycisk „Wymuś zatrzymanie aplikacji”.
* Desktop: naprawione puste listy rozwijane w wersji spakowanej (ProGuard i wartości enum).

**Sniffer i OnDemand (5–26.09)**
* Diagnostyka na żądanie (OnDemand, port 354): 10 typów zapytań; naprawione gubienie zapytań do lokalnie podłączonego węzła.
* Sniffer radiowy i MQTT na jednym ekranie z wyborem źródła, potwierdzenie przed włączeniem, pokazywanie ruchu broadcastowego i własnych pakietów, dekodowanie po kliknięciu, kopiowanie, automatyczne przewijanie z przyciskiem live/pauza i licznikiem nowych pakietów, krótkie nazwy węzłów, znaczniki czasu co do sekundy.
* Przejście snifera na oficjalne protobufy przez OnDemand (port 354); wykrywanie wsparcia po wersji firmware (próg 2) oraz po zgłoszeniu modułu.
* Grupowanie duplikatów z listą odbiorów, zapis i wczytywanie logu (txt/JSON/CSV), bufor 5000 pakietów z licznikiem i konfigurowalną polityką przepełnienia, przeprojektowane karty pakietów (kategorie i kolory), filtr kanału 0, podsumowania telemetrii (uptime, prąd, zajętość kanału i eteru, CPU i pamięć hosta, power metrics, local stats).
* Poprawki: pakiety nieodszyfrowalne trafiają do logu, wybór kanału przez hash, kategoria „Nieznany”, znikanie treści po grupowaniu, trasa powrotna traceroute (SNR), podwójne wpisy przy własnym traceroute, stan snifera po ponownym połączeniu i przy wejściu na ekran, MQTT na desktopie, limit 15 s połączenia MQTT.

**Komunikator**
* Długie wiadomości dzielone na części i składane na bieżąco po odebraniu (podgląd na liście kontaktów, status wysyłania i odbierania zależny od kierunku) (24–25.09).
* Zdjęcia przez link, zapis zdjęcia z podglądu, desktop: Enter = nowa linia, Ctrl+Enter = wysłanie; domyślne szablony Szybkiego Czatu zapisywane per baza.

**Bezpieczeństwo i węzły**
* Wybór koloru węzła przy generowaniu klucza (lokalne szukanie klucza X25519) (2.09).
* Automatyczne czyszczenie bazy węzłów (Android: WorkManager, desktop: pętla godzinowa) (2.09).
* Spolszczone role urządzeń i sekcja „Role” w pomocy; nowe sekcje „Bezpieczeństwo” i „Połączenie” w pomocy listy węzłów (13.09).
* Ujednolicone kolory wskaźników bezpieczeństwa węzła (złoty = zaufanie, czerwony = niezgodność klucza).

**Mapa i desktop**
* Domyślna mapa OpenStreetMap, mniejsze plakietki z ogonkiem, rozsunięcie węzłów o identycznej pozycji, dostrojone klastrowanie, ostrzejsze kafelki rastrowe, mapa zawsze dopasowana do wszystkich węzłów, naprawiona migająca liczba w klastrze, mniejsze przybliżenie mini-mapy w szczegółach węzła, naprawiony padający selektor zakresu czasu.
* Desktop: trzecia kolumna podąża za wybranym węzłem, pasek szybkiego przewijania, naprawiona mapa w spakowanym MSI (ProGuard/LWJGL), biblioteki macOS ARM64, wykrywanie Bluetooth na Windows.
* Naprawione ponowne łączenie Bluetooth z już sparowanym urządzeniem (Xiaomi/MIUI) (8.09). Rozmrożony panel Advanced podczas przełączania Snifera (7.09).
* Odczyt `local_stats_extended` jako natywnego pola telemetrii po zmianie protobufów (7.09).

**Utrzymanie**
* Synchronizacje z upstreamem (1.09, 13.09, 20.09) wraz z aktualizacją protobufów (2.8.0.x); uzupełnione brakujące tłumaczenia PL (w tym 320 wpisów schematu); poprawki formatowania i statycznej analizy.

### Sierpień 2026

* **Zdrowie sieci** (1–2.08): nowa zakładka z metrykami zasilania, sygnału, eteru, środowiska, ruchu i sąsiadów; podsumowanie z rankingami; dane dekodowane na żywo z logu mesh. W sierpniu doszła zakładka **Zasoby** (CPU/pamięć/flash/PSRAM) i widżet „Local Stats”.
* **Nazwa węzła pośredniczącego (relay)** na liście węzłów i w szczegółach oraz pełna lista relayów w statusie doręczenia (do 10.08, 15.08).
* Rozpoznawanie edycji firmware MT-SW (5.08), własny `applicationId` – aplikacja instaluje się obok oryginału (2.08).
* Przyciski szybkich komend `/ping` `/hello` `/test` (3.08), domyślne szablony Szybkiego Czatu (24.08), uptime z ikoną (2 i 10.08).
* Ukryte przestarzałe role (REPEATER, ROUTER_CLIENT), naprawiona mapa śladu pozycji startująca w oceanie, import/eksport konfiguracji na desktopie, własna mapa desktopowa (później zastąpiona MapLibre).
* **Rebranding (27–29.08):** MT_SW_APP, autorska ikona (kontur województwa i „MT_SW”), paleta złoto/granat, ekran „O aplikacji” z zastrzeżeniem o nieoficjalnym forku, nazwa wersji desktopowej, ikona w zasobniku.
* Synchronizacje z upstreamem (5.08, 10.08, 15.08, 16.08, 29.08); przejście na wspólny silnik mapy MapLibre (Android i desktop), domyślnie włączone nakładki cieniowania terenu i radaru pogody.

### Lipiec 2026 — start forka

* Przywrócony ekran Traffic Management (27.07).
* Zawsze widoczna lista sparowanych urządzeń Bluetooth; widoczność Remote Hardware; zdalne ulubione/ignorowanie przez sieć LoRa z potwierdzeniem routing ACK (27.07).
* Ręczne dodawanie kontaktu po ID (lokalnie i zdalnie, format `!a1b2c3d4`) i zdalne sterowanie GPIO (29.07).
* Ostrzeżenia o paśmie wąskim i o wyjściu poza region świętokrzyski; pasywny NeighborInfo z naprawami (30.07).
* Podgląd obrazków z linków w czacie (domyślnie wyłączony); pełny plik tłumaczeń PL (ok. 1000 tekstów) (30.07).

---

# English

This file lists only the changes made in MT_SW_APP **compared to the original app** [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android). Changes from the original upstream releases are not repeated here — see the [original releases](https://github.com/meshtastic/Meshtastic-Android/releases) for the full list. The file is maintained by hand.

## MT_SW fork changes

The entries below describe only what this fork adds on top of the original, from the very start (27 July 2026) to today, in chronological order. Feature details: [README.md](README.md).

### October 2026

**Merge with the original (15.10)**
* Merge with the latest original: backup and restore of the app data (messages and nodes) in the radio settings, radio settings can be opened and read without a connected radio (saving needs a connection), a colour picker with preset colours for the lighting, resending a failed message in place, the connected-radio card shows the LoRa preset and region, a firmware update requirement (an old radio stays connected, but the app asks for an update), notification switches for reactions and new waypoints on the desktop, the settings search jumps to the exact option, names for unknown traceroute hops, and updates of libraries (Compose Multiplatform 1.12.2, markdown 0.7.18, kotest, Firebase) and translations. Polish translations added for the new texts. Unchanged in the fork: automatic contact sharing and favouriting on direct messages stay off (also when resending), the lowered firmware limit for lockdown mode, and the SNR colours and labels in traceroute. The Bluetooth library Kable stays pinned to 0.44.3.

**Network Health — filter by rating**
* Tapping a count in the "Network condition" card (e.g. "3 Bad") lists only the nodes with that rating; tapping again shows all. The chosen rating is highlighted.
* Every tab (Power, Signal, Ether…) keeps its own filter, so each one retains its own choice when you switch tabs.

**Network Health — noise floor colour**
* Signal tab: the number (noise floor) now has its own colour — the lower the value (the bigger the minus), the better. Limits as in "Signal quality": below −100 dBm gold, −100 to −90 dBm red, −90 to −80 dBm purple, higher white. The bar, the row tint, the chart and the "Network health" rating follow the same value; only a node that sends no noise floor is rated by its SNR.
* The noise is judged after the node's LNA correction (before, the colour was computed from the uncorrected value, so it did not match what is shown). This covers the list, the node detail and the chart with limits.

**Device WiFi signal in "Signal quality" and "Network Health"**
* The app reads the new firmware fields: WiFi strength (RSSI), the number of WiFi link drops since start and the code of the last drop reason (ESP32 only).
* "Signal quality": a separate WiFi RSSI line on the chart next to noise, RSSI and SNR (with a legend entry and a description under (i)) and list cards with the strength, the number of drops and the reason in plain words (e.g. "access point lost (200)"); a missing strength with a growing drop count is shown as "WiFi disconnected".
* "Network Health", Signal tab: the node detail gets "WiFi RSSI" and "WiFi drops" charts.
* "On-demand diagnostics": the node statistics response also shows WiFi RSSI, drops and the reason code.
* A "Save WiFi" button in "Signal quality" exports WiFi to CSV (date, time, strength, drops, reason code).
* Host statistics (CPU, memory) are unchanged — WiFi lives only in signal quality.

**Network Health — new look**
* A “Network condition” card on top: a colored ring, the number of nodes in each state and how many are online.
* The app's usual colors: gold = good, red = fair, purple = poor, white = very poor.
* Rows with a colored bar, a filled trend line, the value and a verdict word; tab icons take the color of the worst node.
* A “Problems only” filter, real icons instead of emoji and an (i) button explaining the colors and limits.
* Battery level on the node list: full = gold, medium = red, low = purple.

**Network summary and node details — new look**
* The summary has a card with an online/offline donut chart, colored tiles, donut and bar charts, battery and signal distribution and a 7-day message chart.
* Rankings (best signal, longest running, closest and so on) are shown as bars.
* The summary can be shared or saved as PDF, an HTML page, Markdown, text or CSV (spreadsheet).
* Node details: a card with an icon, the value in the verdict color and a trend chart, plus colored cards with min, max and latest value.
* Node details: new charts (smooth line, gradient, grid, touching shows the value and time) and the icon has a glowing gauge ring.
* Network Health: Neighbors, Traffic and Resources show previously saved data (also after a restart), and the own device is counted in Neighbors.
* Node details: four ways to show the same data to choose from — Line (with colored zones), Gauge, Bars and Heatmap (hours × days) — plus a “Time in each state” strip for battery, SNR and channel use.
* The Signal tab shows the noise floor after LNA correction as the main value; the color still comes from the SNR.

**Node details and diagnostics — new look**
* Charts in node details can be shown in several ways (line, gauge, bars, heatmap); everyone picks what reads best for them.
* Neighbors, traffic and resources read their data from the saved logs (also after the app restarts); on Signal the main value is the noise floor after LNA correction.
* Signal colors are based on SNR (or on the noise floor when SNR is missing), also for nodes that are not heard directly; “Network condition” follows the same rule.
* The GPIO field and quick commands (/ping, /hello, /test) have tiles, pin number checking, “Switch on (1)” / “Switch off (0)” buttons, a clearly spaced help text and a description of every command noting they need firmware 2.8.0 or newer.
* Remote management (favorites, ignored, adding a contact) has tiles, node ID checking and short explanations.
* On-demand diagnostics: an overview card of all capabilities with a counter of checked ones, and every query has a description, an “Ask” button and a colored result shown as tiles, gauges, donut and bar charts.
* A “Reverse current sign” switch for every node separately (ours too), in the info window (“i” icon) of the power and environment statistics, like LNA: when a sensor is wired backwards, the charging current shows as positive on the node list, in details and in Network Health. Only the sign of the displayed current and its charts changes — charging detection and everything else are unaffected.
* A node that sends only voltage and current in its environment data now has the icon to open its saved frames (before, it appeared only with temperature, humidity or lightning).
* The LNA gain field is now in the signal details for our own node too (it can be set there and in the LoRa settings).
* On the conversation list the name text of the main channel is in the app theme color and of other channels red (instead of white), and of direct messages the node's color.
* Channel locks use the app colors: gold = secure channel, red open = public without precise location, purple open = public with precise location (with a white warning when it also goes through MQTT); help texts updated.
* A separate “Swap powered / battery indication” switch (in the info window of the device metrics): for a monitor wired backwards it swaps “powered” and “battery percent” on the node list, in details, in Network Health and in the summary. The percentage is estimated from the voltage. It changes only this indication; the current sign has its own switch.
* Environment statistics: a “Weather / Power” switch above the chart. “Power” shows the sensor's voltage and current with the same chart as the power metrics (switched on by itself for a sensor that reports only voltage and current).
* On-demand diagnostics: sharing and saving all received answers as PDF, an HTML page, Markdown, text and CSV (spreadsheet).
* The node list help (in the app and in the docs) has a “Battery level” section: gold, red and purple indicators, external power and no data; the docs also explain the colors in Network Health.

**Help and documentation in Polish**
* All in-app documentation (User Guide and Developer Guide, 36 pages) translated into Polish, including page names; the documentation site (docs/pl) is now fully in Polish too.
* The preset picker hint says that the default preset for MT_SW_FIRMWARE is Narrow Fast.
* Help pages have smaller headings and “A−” / “A+” buttons in the top bar to change the text size.

**Information windows**
* Information windows in the app now appear less often, at random intervals; the schedule is stored permanently.
* When configuring a device remotely, a separate, location-dependent window is shown.
* Rewritten, calmer and more informative window texts.
* The repeating welcome window appears about once every two days, at a random time.

**Merge with the original (14.10)**
* Merged the latest original: license-list library update (aboutlibraries 15.3.0), updates to the help-page rendering library (markdown 0.46.0) and the key-encryption library (bouncycastle 1.86), refreshed startup profiles, refreshed device hardware and link lists (e.g. Seeed Wio Tracker L2 is now actively supported) and translations. The Kable Bluetooth library stays pinned at 0.44.3 (0.45.0 is still the newest and breaks BLE scanning on Windows). No change to other features.

**Merge with the original (13.10)**
* Merged the latest original: Discovery scan fix (public presets are scanned on the default public channel and slot, then your own channel and LoRa settings are restored), desktop map fix (LWJGL kept through ProGuard), map logging in one place, library updates (paging, markdown, Google Maps) and translations. The Kable Bluetooth library stays pinned at 0.44.3 (newer breaks BLE scanning on Windows). No change to other features.

**Merge with the original (12.10)**
* Merged the latest original Meshtastic-Android: newer map library (maplibre-compose 0.20) with our map layers, planner and offline maps adapted to it, plus small upstream fixes; no change in app behaviour.
* Modem preset names in Polish with the English name in brackets, e.g. “Szeroki daleki szybki (Long Fast)”; “Szeroki” means a 250 kHz channel, “Pośredni” (intermediate) 125 kHz (Lite is now “Pośredni”), “Wąski” (narrow) 62.5 kHz, “XXL” 500 kHz (turbo).
* Description under the “Ok to MQTT” switch in LoRa settings, like the other switches (recommending it stays on so the network can be monitored); the default-preset hint in the preset picker now shows the Polish name with the English one in brackets.

**Mesh Link Planner (native coverage planner, 1–2 Oct)**
* A native planner replaces the external Site Planner, entirely inside the app (Android and desktop): two independent points A/B (node list, map, coordinates, own station), Mapterhorn terrain, ITM/Longley-Rice, Fresnel zone, profile chart, link budget, antenna bearings and angles, comparison with the last measurement, omnidirectional coverage, export to PDF/CSV/KML/GeoJSON/PNG, "i" info dialogs, credits and licences.
* Cable and connector wizard (datasheet-based database, connectors without a cable, e.g. SMA→N adapters), every band from 169 MHz to 2.4 GHz plus any frequency; picking a band sets the frequency and channel width (2.4 GHz as wide LoRa, first slot for 433 and 470 MHz).
* Default Narrow Fast preset and 869.44165 MHz; presets without manual BW and SF; changing the preset sets its default frequency.
* Coverage on the map as a dense raster with a smooth colour scale (MeshMap Planner style), an opacity slider, a layer list on top with scrolling, a layer counter on the icon, a limit of 9 layers; coverage layers are not remembered after a restart. Named "Mesh Link Planer (by MT_SW)".
* Optional live weather (Open-Meteo): k-factor, refractivity, ducting.
* Obstacles: clutter presets (default) or the optional **detailed terrain from OpenStreetMap** (buildings and forests via the Overpass API, along the path and in coverage within the selected radius of the centre), with a warning about higher resource use and longer calculation; falls back to the preset if the download fails.
* Terrain data range menu (5 / 10 / 15 / 30 / 50 / 100 km, 30 km by default); the OpenStreetMap download is read in chunks with a size limit and out-of-memory is caught, so a dense area no longer crashes the app (an emulator with little memory included) but shows "too much data, reduce the range" and falls back to the obstacles preset, also during the coverage calculation.
* Fix for "out of memory" with detailed terrain: the Overpass server memory cap written into the query (wrongly lowered to 16 MB earlier) is back to a safe value, so the server no longer refuses even a small area; a server refusal ("out of memory", overload, HTTP 429/504) now has its own message instead of "the device ran out of memory", with the short reason given by the server; on a refusal the planner tries backup Overpass servers.
* Terrain data range above 10 km (50 and 100 km): the area is downloaded in pieces (tiles of about 30 km, one request at a time) instead of one huge answer that neither the server nor the device could handle; a tile the server finds too heavy is split into four, a busy server is waited for, elements on tile borders are counted once, outline points are thinned (from 25 m, more the larger the range, and tiny woods below that scale skipped) to save memory, and the last working server is remembered. The terrain query uses exact matches (`landuse=forest`, `residential` etc.) instead of a regular expression, so the server no longer reads every field and meadow in a tile and answers much faster. The whole download has a 12-minute limit and ends either complete or with an error (no half-maps). The progress (piece x of y) is shown under the coverage bar, and a long download no longer blocks the ordinary A–B link calculation. Overpass requests have their own limits (90 s wait for data instead of 30 s and no automatic repeats, which re-sent heavy queries); a timeout now means "tile too heavy" and splits it, and a brief network drop is retried. The obstacle query for a link path is split into lighter parts (woods, then the buildings at each end) with a 60 s server limit and one retry, because a single heavy query for a long link ran into the server time limit; the smallest coverage tile that timed out is retried after a short wait.
* The note under the detailed terrain settings now shows the selected data range instead of a fixed “30 km”.
* Traceroute dialog: when the route cannot be shown on the map there is a single OK button (before, a second one appeared next to it); traceroute labels on the map have a thinner outline and a larger font.

**Sniffer, keys, coding rate (Oct 8)**
* Settings: the new "Report a bug or idea" entry opens a "Got an idea or found a bug?" window — clickable links to the Discord server and the WhatsApp and Signal groups, a note that you can also write over the mesh network in the Świętokrzyskie region, and a hint on what to include in a bug report.
* New app version notifications: twice a day the app checks the MT-SW/MT_SW_APP releases on GitHub (phone and desktop), shows a notification with a link to the list of releases as soon as a new version is out and reminds every two days until you update. Test versions only when "Include test versions" is on; "Notify about new versions" switches it all off. Replaces the old desktop update check, which looked at the original app's releases. Notifications work from V3 on, so they announce V4 and newer. Desktop files built on GitHub (Windows, Linux, macOS) are now release builds, so the update check also works on the desktop. On the desktop the first check is a minute after start, and clicking the notification opens the app with a window and a "Show releases" button. Changing a switch checks at once.
* Windows: the installer (.msi/.exe) gets a higher number inside with every code change, so it installs over the old version without uninstalling; the app and file name still show the normal version (e.g. 2.8.3).
* Neighbor info: the update interval is picked from a list of hours (6, 8, 10, 12, 18, 24) instead of typing seconds.
* Merge with upstream (Oct 10): technical updates only (code-style checking library, startup profile) — no change in app behaviour.
* Merge with upstream (Oct 11): minor library updates (network device discovery), corrected comments in the Bluetooth code and the startup profile — no change in app behaviour.
* Sniffer: the state is read again after every reconnect — the app no longer shows an old answer from the previous connection and asks the radio several times until it answers.
* Sniffer: a remembered "Radio" selection is no longer trusted until the connected radio confirms during this connection that its sniffer is on — after a radio reset, a device change, or with firmware that has no sniffer, the panel stops showing and collecting packets at once (before, only after tens of seconds).
* Desktop: the Security settings now have "Back up keys", "Restore keys" and "Delete key backup" buttons, the same as the phone app.
* Coding rate: the Narrow (Fast/Slow) and Tiny Slow presets, which default to 4/6, now also offer 4/5.
* LoRa settings: new "Frequency offset (kHz)" field — you enter the offset in kHz (e.g. 12.5) and the app converts it to MHz for the radio.
* Coding rate: fixed returning to "preset default" — after saving and reconnecting it showed 4/5 because the radio fills in 5 by itself; the default is now stored as the preset's own coding rate (4/6) and no longer looks like a 4/5 choice.
* Offline maps: downloading now also works for the default OpenStreetMap and the other raster basemaps (before, only vector styles could be downloaded).
* GitHub builds (Actions → "Build Binaries (Manual)"): every system and format is picked separately — Windows (.msi, .exe, .jar), Linux (.deb, .rpm, .AppImage, .jar), macOS (.dmg, .jar) and Android (APK).

**Map**
* Smooth panning and zooming with many nodes: the "last heard"/"online" fields removed from map data, nodes refreshed only when position, name, favorite or ignored state change, pulse on its own small source in 15 steps, a stable view area while panning, cached chip images, no invisible outlines in the coverage layer.
* **Traceroute on the map with signal strength:** the "View on map" button only when every node on the route has a position (otherwise the dialog explains why); one thin line per hop in the colour of its signal quality, with an arrowhead at its end and the SNR in dB along the line, stopping just short of the node (nodes as dots under their chips), both directions symmetrically side by side; quality legend; a freshly received traceroute now has its SNR values straight away (previously the lines were grey until opened from the saved traces).

**Network**
* Recently used network (WiFi/TCP) devices: 20 entries instead of 3.

**Descriptions and naming (4 Oct)**
* **GPIO control guide behind an info icon:** the GPIO card header on the node detail screen now has an info icon that opens a short step-by-step guide — the Remote Hardware module and available pins on the target node, a "gpio" channel at position 1 with the same key on both devices, and what the 1 / 0 / Read buttons do.
* **One name, "On-Demand Diagnostics",** across the app (sniffer, packet port name, descriptions) instead of "OnDemand".

**Nodes, database cleanup and the upstream merge (3 Oct)**
* **Node cleanup over Bluetooth now completes:** the "remove node" commands to the radio are sent one by one with a short gap and retried when the link briefly refuses them (Bluetooth accepts only a few writes at a time, so with hundreds of nodes most commands used to be silently dropped and the radio handed those nodes back after reconnecting). Sending runs to the end even if you leave the screen; when the radio is not connected only the app is cleaned. It already worked over WiFi.
* **Clean all unknown nodes regardless of date:** with "Clean up only unknown nodes" on, a new switch "Ignore the date: all unknown nodes without a key" appears (nodes that never introduced themselves and that we hold no key for; favorites and ignored nodes are always kept).
* **Clean nodes with a mismatched key:** a new switch "Only nodes with a mismatched key (removed from the app and the radio)" on the cleanup screen — the list shows every node in the "key mismatch" state regardless of age (favorites, ignored nodes and our own node are kept), and cleaning removes them from the app database and from the radio.
* **Clear only the app's own node database** (no contact with the radio, works while disconnected): a button at the bottom of the cleanup screen with a confirmation and a "Keep favorite nodes" switch; turn it off to empty the list completely. A connected radio sends its nodes again on the next sync.
* **Smoother fast-scroll strip (desktop):** it no longer recomposes the whole screen on every scroll frame, the thumb glides with the list (instead of stepping item by item), and dragging moves the list continuously in pixels.
* **Upstream merge (meshtastic/Meshtastic-Android):** protobufs 2.8.1 and new dependencies, new upstream features (including the remote shell, reaction notifications, paged logs, `SendMessageOutcome`), new strings translated to Polish; our translations, branding, planner and scroll strip were kept. Time and size formatting moved to the new upstream helpers.

**Documentation**
* README and CHANGELOG completed with the whole history of the fork.

### September 2026

**Signal quality, LNA, logs (29–30 Sep, 1 Oct)**
* LNA gain correction for noise floor and RSSI (field in LoRa under the radio power and in the "Signal quality" dialog, a separate field for foreign nodes in node details, charts, Network Health, average noise in the summary).
* SNR rating relative to the preset (Narrow −3/−7/−12 dB, Lite −5/−10/−15 dB), RSSI can only lower the rating; colours as in the connection statuses (good gold, sufficient red, weak purple, none white/black depending on the theme); description with preset-dependent thresholds; SNR colouring in neighbor packet details and in traceroute.
* Message status colours (delivered, acknowledged, error, signature shield in gold).
* Device logs in the debug panel (live, search, export; Wi-Fi and USB only with MT_SW firmware), the sender's frame time in the sniffer, a "Force stop app" button.
* Desktop: fixed empty dropdown lists in the packaged build (ProGuard and enum values).

**Sniffer and OnDemand (5–26 Sep)**
* On-demand diagnostics (OnDemand, port 354): 10 query types; fixed requests to the locally connected node being dropped.
* Radio and MQTT Sniffer on one screen with a source selector, confirmation before enabling, showing broadcast traffic and own packets, decoding on click, copying, auto-scroll with a live/pause button and a new-packet counter, short node names, second-precision timestamps.
* Sniffer moved to the official protobufs over OnDemand (port 354); support detected by firmware version (threshold 2) and by the advertised module.
* Duplicate grouping with a list of receipts, saving and loading the log (txt/JSON/CSV), a 5000-packet buffer with a counter and a configurable overflow policy, redesigned packet cards (categories and colours), a channel-0 filter, telemetry summaries (uptime, current, channel and air utilisation, host CPU and memory, power metrics, local stats).
* Fixes: undecryptable packets reach the log, channel choice by hash, the "Unknown" category, content vanishing after grouping, traceroute return path (SNR), double entries for own traceroute, sniffer state after reconnecting and on re-entering the screen, MQTT on desktop, a 15 s MQTT connect timeout.

**Messaging**
* Long messages are split into parts and reassembled live on receipt (contact list preview, direction-aware sending and receiving status) (24–25 Sep).
* Photos by link, saving a photo from the preview, desktop: Enter = new line, Ctrl+Enter = send; default Quick Chat templates seeded per database.

**Security and nodes**
* Node colour picker when generating a key (local X25519 key search) (2 Sep).
* Automatic node-database cleanup (Android: WorkManager, desktop: hourly loop) (2 Sep).
* Localized device roles and a "Roles" help section; new "Security" and "Connection" sections in the node list help (13 Sep).
* Unified node security indicator colours (gold = trust, red = key mismatch).

**Map and desktop**
* OpenStreetMap as the default map, smaller chips with a tail, spreading of nodes at an identical position, tuned clustering, sharper raster tiles, the map always framed on all nodes, fixed flickering count in the cluster, a smaller mini-map zoom in node details, fixed crashing time-range selector.
* Desktop: the third column follows the selected node, a quick-scroll bar, fixed map in the packaged MSI (ProGuard/LWJGL), macOS ARM64 libraries, Bluetooth discovery on Windows.
* Fixed Bluetooth reconnection to an already-paired device (Xiaomi/MIUI) (8 Sep). Unfrozen Advanced settings panel while toggling the Sniffer (7 Sep).
* `local_stats_extended` read as a native telemetry field after the protobufs change (7 Sep).

**Maintenance**
* Upstream syncs (1 Sep, 13 Sep, 20 Sep) together with protobufs updates (2.8.0.x); missing PL translations completed (including 320 schema entries); formatting and static-analysis fixes.

### August 2026

* **Network Health** (1–2 Aug): a new tab with power, signal, airtime, environment, traffic and neighbor metrics; a summary with rankings; data decoded live from the mesh log. A **Resources** tab (CPU/memory/flash/PSRAM) and a "Local Stats" widget followed in August.
* **Relay node name** in the node list and node details, and the full list of relays in the delivery status (until 10 Aug, 15 Aug).
* MT-SW firmware edition detection (5 Aug), own `applicationId` so the app installs next to the original (2 Aug).
* `/ping` `/hello` `/test` quick command buttons (3 Aug), default Quick Chat templates (24 Aug), uptime with an icon (2 and 10 Aug).
* Hidden obsolete roles (REPEATER, ROUTER_CLIENT), fixed position-track map starting in the ocean, config import/export on desktop, a custom desktop map (later replaced by MapLibre).
* **Rebranding (27–29 Aug):** MT_SW_APP, custom icon (province outline and "MT_SW"), gold/navy palette, an "About" screen with the unofficial-fork disclaimer, desktop app name, tray icon.
* Upstream syncs (5 Aug, 10 Aug, 15 Aug, 16 Aug, 29 Aug); switch to the shared MapLibre map engine (Android and desktop), hillshade and weather radar overlays on by default.

### July 2026 — start of the fork

* Restored the Traffic Management screen (27 Jul).
* An always visible list of paired Bluetooth devices; Remote Hardware visibility; remote favorite/ignore over the LoRa mesh with routing-ACK confirmation (27 Jul).
* Manual contact add by ID (local and remote, `!a1b2c3d4` format) and remote GPIO control (29 Jul).
* Narrow-band and out-of-region (Świętokrzyskie) warnings; passive NeighborInfo with fixes (30 Jul).
* Image preview from links in chat (off by default); a full PL translation file (about 1000 strings) (30 Jul).
