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

* Asystent zna aplikację: wbudowana wiedza z dokumentacji i listy zmian (dobierana do każdego pytania), mała liczba narzędzi dobierana do tematu pytania, większe okno kontekstu dla większych modeli oraz w logach informacja, na jakim układzie (NPU/GPU/CPU) działa model.
* Asystent pokazuje pod odpowiedzią **przyciski** (otwórz ekran, pokaż ustawienie z żółtym mruganiem, węzeł na mapie, planer łącza, szkic wiadomości); nic nie dzieje się samo, wiadomość nigdy nie jest wysyłana automatycznie.
* **Czytanie na głos** odpowiedzi asystenta i wiadomości z czatu (menu po przytrzymaniu) oraz dyktowanie pytania (Android).
* **Internet dla asystenta**: jeden przełącznik, domyślnie wyłączony, z ostrzeżeniem i widocznym wskaźnikiem. Włączony pozwala na wyszukiwanie w sieci (z pokazaniem zapytania i źródeł, tekst z sieci tylko jako informacja) i na automatyczne, weryfikowane pobieranie nowości o aplikacji z naszego repozytorium; wyłączony = pełny tryb offline.
* Czat: przycisk tłumaczenia przed wysłaniem to teraz mały szary przycisk „Przetłumacz” nad polem tekstu.
* Funkcje MT_SW: skróty do motywu i panelu debugowania.
* Wybór koloru węzła: dowolny kolor (kod, odcień/nasycenie/jasność), ostatnio użyte kolory, tryby „Szybko” i „Dokładnie” zamiast tolerancji, licznik czasu i przerwanie szukania.
* Motywy czarno-białe: na mapie linie, znaczniki węzłów i ślady są też szare, a górna belka i dolna nawigacja (ikona połączenia, tytuły, trójkąt ostrzeżenia) pozostają czarno-białe.
* Menu rozmowy (trzy kropki): czytanie na głos i tłumaczenie widocznych wiadomości lub całej rozmowy.
* Tłumacz: przetłumaczony tekst można przeczytać na głos.
* Opcja przeniesienia przycisku dodawania i udostępniania znad listy węzłów i rozmów do górnej belki (domyślnie wyłączona).
* Asystent: pewniejsze odpowiedzi o sieci: pytania uzupełniające korzystają z poprzedniego tematu, rankingi odebranych i wysłanych ramek (największe lub najmniejsze), kto był słyszany ostatnio lub milczy najdłużej, role i ulubione.
* **Dyktowanie** w asystencie działa tak samo na telefonie i komputerze, bez usług systemu: mikrofon + model Gemma E2B/E4B, w całości na urządzeniu. Mikrofon także w rozmowach (kanały, wiadomości prywatne) i w tłumaczu; widoczny tylko przy włączonym asystencie i wybranym modelu, który słyszy.
* Okres pamiętania odebranych ramek (logów): wybór w dniach od 1 do 60 (1, 2, 3, 7, 14, 30, 60) zamiast od godziny do roku, także w zwykłych Ustawieniach; wcześniejszy wybór jest zachowany, dopóki nie wybierzesz nowego.
* Domyślnie aplikacja pamięta odebrane ramki z **60 dni**, a lista ostatnich adresów Wi-Fi mieści do **30** pozycji.
* Asystent: gdy mały model zamiast użyć narzędzia wpisze je tekstem, zapis zmienia się w prawdziwy przycisk „Przejdź do”; informacja nad rozmową mówi prawdę o tym, czy internet dla asystenta jest włączony.
* Asystent: kanał rozpoznawany też po numerze („kanał 1”, „kanał jeden”), więc streszczenie dotyczy właściwego kanału; liczba wiadomości na kanałach jest dostępna dla modelu jako narzędzie.
* Pamięć heap i PSRAM są wszędzie pokazywane jako **zajęte** (a nie wolna): lista węzłów, wykresy zasobów, zdrowie sieci, widżet, powiadomienie, podgląd pakietów, zapis zdalny i asystent. Np. 10% wolnego to teraz 90% zajętego.
* Naprawa zamykania się aplikacji na telefonie przy użyciu tłumacza (po optymalizacji wersji release brakowało klas biblioteki ONNX).
* Asystent: dane z czujników i zasięgu trafiają do modelu w czytelnej, ułożonej postaci (z jednostkami, pominięte niemożliwe wartości); zawsze odpowiada model, nic nie jest odpowiadane na sztywno.
* Asystent: pytania o „nasz projekt”, założenia i osiągnięcia dotyczą MT_SW (z dokumentacji i listy zmian), a główny Meshtastic wspomina tylko krótko, chyba że ktoś o niego zapyta; dane o sieci są dołączane tylko wtedy, gdy pytanie ich dotyczy.
* Asystent zna cały projekt MT_SW (firmware, integrację z Home Assistant, zgodność z główną siecią), jego autorów i osiągnięcia; ta wiedza jest zapisana w aplikacji, odświeżana przy każdym budowaniu (oraz przez internet, gdy asystent ma go włączony) i widoczna tylko dla asystenta.
* Asystent: opis sieci i własnego węzła po polsku, gdy użytkownik pisze po polsku (odległości w słowach zamiast par liczb, których model nie mieszał).
* Asystent: zapis narzędzia wpisany tekstem bez nawiasów (np. „show_setting …”) też zmienia się w przycisk, a miejsce jest szukane najpierw po słowach użytkownika.
* Asystent: **pamięć między rozmowami**: sam zapisuje krótkie notatki w kilku plikach (o Tobie, sprzęcie i sieci, stylu rozmowy, projektach), a Ty możesz je zobaczyć, poprawić, usunąć i wpisać własne instrukcje; przełącznik w ustawieniach asystenta, ostrzeżenie nad rozmową, wszystko tylko na urządzeniu.
* Asystent odpowiada bardziej naturalnie, jak w zwykłej rozmowie, bez sztywnych zwrotów.
* Czat: przyciski formatowania są w jednej linii z przyciskiem „Przetłumacz”, przy prawej krawędzi.
* Czat: wiadomość podzielona na kilka części nie „podczepia się” już pod poprzedniego nadawcę (brak nazwy, połączone rogi, zły kolor); sąsiedzi wiadomości są liczeni tylko z widocznych dymków.
* Asystent: węzeł rozpoznawany też po nazwie miejsca w innej odmianie („router na Łysicy”), pytanie o węzeł zawsze dostaje jego dane, a zapis „show_node_on_map” bez nazwy zmienia się w przycisk dla węzła z pytania.
* Asystent: pytania „gdzie jest najbliższy router / czujnik / przekaźnik” dostają listę najbliższych węzłów danej roli razem z pozycją i odległością.
* Asystent: pewniejszy język odpowiedzi (niższa „kreatywność” modelu), zamiast dopytywać sam szuka danych, a notatki pamięci powstają tylko z tego, co napisał użytkownik.
* Asystent: odczyty czujników, które nie mogą być prawdziwe (np. temperatura 25252 °C), są oznaczane jako „prawdopodobnie uszkodzony czujnik”, a odpowiedź, która zaczyna się powtarzać w kółko, jest przerywana po pierwszym razie.
**Scalenie z oryginałem — wersja 2.8.4 (początek)**
* Scalenie z oryginałem po wydaniu 2.8.3: numer wersji aplikacji 2.8.4, nowsze protobufy (2.8.1.18), odświeżone dane o sprzęcie i firmware, tłumaczenia i zrzuty ekranu sklepów. Biblioteka Bluetooth Kable zostaje przypięta do 0.44.3, a nasze tłumaczenia PL bez zmian.
* Drugie scalenie z oryginałem: nowy **widok AR sieci** (węzły i punkty trasy na obrazie z aparatu telefonu), **podgląd 3D urządzenia**, szybkie akcje „Otwórz kompas” i „Otwórz widok sieci” w menu węzła, protobufy 2.8.1.20 i nowsze narzędzia budowania. Nowe napisy przetłumaczone na polski, a nasze funkcje (np. nazwy planera, Zdrowie sieci) zostały zachowane.
* Trzecie scalenie z oryginałem: kafelek połączenia z radiem w szybkich ustawieniach Androida, zdjęcia widoku sieci robione przez aparat, kierunek i wygaszanie śladu oraz wysokość z barometru w widoku sieci, nowy model urządzenia (Chirpy) w podglądzie 3D, poprawki ustawień (hasło MQTT do 31 bajtów, działanie przełącznika diody LED, listy ustawień tylko z tym, co obsługuje firmware), przetłumaczone poziomy jakości powietrza i nowsze biblioteki. Nasz planer zostaje bez zmian.
* Pasek szybkich reakcji ma teraz od lewej: trzy kropki (więcej działań), dodawanie reakcji, a potem najczęściej używane emoji (najczęstsze obok dodawania). Wcześniej opcje były schowane za emoji i łatwo je było przeoczyć.
* Dwa nowe motywy **czarno-białe** (jasny i ciemny) w wyborze motywu: jasny motyw czarno-biały działa teraz także, gdy telefon ma ustawiony tryb ciemny; całe wnętrze aplikacji jest w odcieniach szarości, także sygnał, wykresy, kolory węzłów, zdjęcia i mapa. Dla osób, którym nie podobają się nasze kolory.
* Wersja na komputer pokazuje się w systemie (np. w oknie „wznowić aplikację?” na macOS) pod nazwą **MT_SW_APP**, a nie „Meshtastic Desktop”.
* Nowa grupa **„Funkcje MT_SW”** w Ustawieniach (jeden wpis, osobny ekran): przełącznik obrazków z linków w czacie, powiadomienia o nowych wersjach i układ listy węzłów przeniesione z głównego ekranu; skróty do skalowania aplikacji, LoRa, czyszczenia bazy węzłów i Sniffera; wpis „Pozostałe funkcje” opisuje funkcje przy węźle. Działa na telefonie i komputerze, jest w wyszukiwarce ustawień.
* Opcjonalny **tłumacz i asystent offline** (domyślnie wyłączone, włączane osobno): działają w całości na urządzeniu, bez internetu. Tłumaczenie odebranych wiadomości, tłumaczenie wiadomości przed wysłaniem (z podglądem oryginału i tłumaczenia) i karta „Tłumacz” na dolnym pasku (po włączeniu asystenta „AI”). Lista modeli w trzech poziomach (lekki, standardowy, mocny) z osobnym pobieraniem i usuwaniem każdego, wyborem używanego modelu osobno dla tłumacza i asystenta oraz ostrzeżeniami o rozmiarze i pamięci RAM. Informacja o lokalnym przetwarzaniu i licencjach modeli. Lista: NLLB oraz Gemma 4 E2B, E4B i 12B (licencja Apache 2.0) na silniku Google LiteRT-LM, z użyciem układu graficznego, gdy urządzenie go obsługuje; wybrany model decyduje, którym silnikiem tłumaczy aplikacja. Gemma nie tłumaczy nazw własnych. Asystent to rozmowa w dymkach bez limitu długości rozmowy (starsza część zapamiętywana w streszczeniu, odpowiedzi nie są ucinane) z menu po przytrzymaniu (kopiuj, zapytaj ponownie; po nieudanej odpowiedzi pytanie wraca do pola) (Twoje po prawej, asystenta po lewej, z pulsującymi kropkami podczas myślenia i podpowiedziami pytań na start) z odpowiedziami pojawiającymi się na bieżąco, z przyciskiem zatrzymania, historią rozmów zapisywaną tylko na urządzeniu (ikona zegara: kontynuowanie i usuwanie) i przyciskiem „Nowa rozmowa”, Asystent sam sięga po dane aplikacji przez narzędzia tylko do odczytu (przegląd sieci, nazwy i historia dowolnego węzła, rankingi węzłów, czujniki i pogoda, ustawienia radia, wyszukiwanie wiadomości), więc nie jest ograniczony do z góry wybranych tematów. Ma dostęp tylko do odczytu do danych aplikacji: własny węzeł, każdy węzeł sieci (po nazwie z pytania) wraz z całą zapisaną historią i tendencją (np. pamięć RAM/heap, procesor, bateria, zajętość kanału, sygnał), podsumowanie całej sieci, ustawienia radia (bez kluczy i haseł) i ostatnie wiadomości; niczego sam nie zmienia ani nie wysyła. Do tego oraz uwagą, że odpowiedzi mogą być błędne; działa na tym samym modelu co tłumacz (kolejka zapytań). Pierwsze tłumaczenie lub odpowiedź trwa dłużej, bo model jest wczytywany do pamięci, więc pokazuje się pasek postępu. Wpis „Funkcje MT_SW” przeniesiony do Ustawienia → Zaawansowane, nad aktualizację oprogramowania.

**Dodatkowe dane przy węźle i odporność Zdrowia sieci**
* W aplikacji na komputer lista węzłów i szczegóły węzła zaczynają po pół szerokości (dzielnik można przesunąć). Ustawienie „Skalowanie aplikacji” nazywa się teraz „Zgodnie z systemem”, a nie „z telefonem”.
* W aplikacji na komputer ekran Ustawień ma teraz strzałkę cofania podczas zdalnego zarządzania innym węzłem (tak jak w telefonie); wcześniej nie było jak z niego wrócić.
* Nowa dodatkowa linia pod każdym węzłem na liście. Przełącznik „Metryki środowiskowe” pokazuje w niej na górze napięcie i prąd, a pod nimi wszystkie czujniki pogody. Pod spodem, w ustawieniach „Dodatkowe statystyki”, osobne przełączniki (domyślnie wszystkie włączone): eter (zajętość kanału, czas nadawania, szum tła — z krótkimi podpisami; w linii ze „Skoków” same ikony od prawej strony), pakiety (TX, RX, błędne, duplikaty) oraz sprzęt (procesor, pamięć, siła Wi-Fi węzła); pakiety i sprzęt mają osobne linie przy prawej krawędzi. Napięcie i prąd w dodatkowej linii pochodzą z czujnika (telemetria środowiskowa lub zasilania), nie z podstawowej telemetrii przy baterii. Wszystko to ostatnie dane z pakietów węzła, nic nie jest mierzone na bieżąco. Strony albo wszystko naraz, opcja „same ikony” z legendą (i), a podgląd w ustawieniach od razu pokazuje zmiany.
* Zdrowie sieci: obce lub zepsute pakiety (np. z innego firmware) nie mogą już zawiesić ani wywrócić ekranu; ekran sam próbuje się podnieść po błędzie i nie czyta już pakietów trybu na żądanie.

**Wybór sieci Wi-Fi z listy**
* W ustawieniach sieci radia nowy przycisk „Wybierz sieć Wi-Fi z listy”: pokazuje sieci 2,4 GHz w pobliżu telefonu lub komputera (obecna sieć na górze, siła sygnału, kłódka przy sieciach z hasłem). Po wybraniu sieci z hasłem pojawia się osobne okienko na hasło, a po „Połącz” radio od razu dostaje sieć i hasło (jak „Wyślij”) i łączy się z nią; sieć bez hasła łączy się od razu. Aplikacja nigdy nie odczytuje zapisanych haseł telefonu.

**Gierka przy aktualizacji firmware**
* Ludzik w grze (też ten ogłuszony oraz ten na ekranie aktualizacji i w pomocy) jest teraz cały żółty i ma na brzuchu logo MT_SW takie jak w ikonie aplikacji, zamiast zielonego koloru i oryginalnego znaczka.

**Okresowe komunikaty**
* Drobne zmiany w czasie pojawiania się okresowych komunikatów w aplikacji.

**Wyciszanie urządzenia na kanale i jednolite czasy wyciszenia**
* Przytrzymaj wiadomość → „Więcej działań” → „Wycisz wiadomości od tego urządzenia na tym kanale”. Wiadomości nadal się zapisują i są widoczne; wyłączone są tylko powiadomienia i dźwięk, tylko na tym kanale. Obok nazwy jest mała ikona głośnika, wyciszenie przetrwa restart i można je cofnąć w tym samym menu.
* Czasy wyciszenia są teraz wszędzie takie same (rozmowy prywatne, kanały, urządzenie na kanale): 1 godzina, 24 godziny, 2 dni, 1 tydzień, 2 tygodnie, 1 miesiąc, na zawsze. Dawne wyciszenia (np. na 8 godzin) działają, aż wygasną.

**Ikona (i) na liście rozmów i nowe opisy**
* Na liście rozmów jest ikona (i) z opisem: dotknięcie, przytrzymanie, przesunięcie, wyciszanie i klikalne etykiety dostarczenia (ile urządzeń przekazało wiadomość i które — tylko w tej aplikacji).
* Panel debugowania ma ikonę (i) opisującą zakładki Pakiety, Logi aplikacji i Logi urządzenia; przełącznik połączeń na mapie ma krótki opis.

**Nowa opcja zakresu „2 dni”**
* Wszędzie, gdzie wybiera się zakres danych (wykresy statystyk węzła, szczegóły metryk), jest teraz „2 dni” między 24 h a 7 dni.

**Wczytywanie danych — „Wczytuję dane…”**
* Ekrany statystyk i zdrowia sieci pokazują kółko z napisem „Wczytuję dane…” zamiast sprawiać wrażenie zawieszenia; zmiana zakresu w szczegółach metryki też pokazuje kółko w miejscu wykresów, a przyciski zakresu działają od razu.
* Szczegóły węzła: ikony statystyk pojawiają się szybciej (lżejsze wczytywanie na starcie).

**Wgrywanie ESP32 przez aplikację (OTA) — ostrzeżenie o pliku -full.bin**
* Na ekranie aktualizacji, przy wyborze własnego pliku dla urządzeń ESP32, jest ostrzeżenie: wcześniej trzeba wgrać plik `-full.bin` (kasuje wszystkie ustawienia) przez stronę internetową (oryginalną lub inną) — wgranie go przez aplikację nie zadziała. Kto już wgrywał firmware oryginalnym flasherem z wymazaniem, ma to załatwione.

**Wygląd na komputerze i okna dialogowe**
* Ekran powitalny na komputerze ma pasek na dole na całą szerokość, bez jaśniejszych pól po bokach.
* Lżejszy start aplikacji na komputerze: większa pamięć na starcie, a sprzątanie węzłów rusza dopiero po minucie, żeby nie spowalniało otwierania.
* Lista węzłów nie pyta już w kółko bazy o ikony urządzeń przy każdym nowym pakiecie (dotyczy telefonu i komputera), przez co mniej się przycina po połączeniu z radiem.
* Mała mapka w szczegółach węzła nie buduje się już od nowa za każdym razem, gdy w szczegółach pojawiają się kolejne wiersze statystyk (telefon i komputer).
* Szczegóły węzła na komputerze otwierają się płynniej: mała mapka startuje chwilę po reszcie ekranu.
* Okna dialogowe (np. trasy) uwzględniają „Skalowanie aplikacji”.

**Szczegóły węzła — ikony statystyk pojawiają się szybciej**
* Przy pierwszym wejściu w szczegóły węzła ikony wejścia w statystyki nie czekają już na wczytanie całej historii (do 5000 wpisów): najpierw pojawiają się z najnowszych ~300 wpisów prawie od razu, a pełna historia dociąga się w tle.

**Dokumentacja „Funkcje MT_SW” uzupełniona**
* Strona zawiera teraz wszystkie funkcje forka: zdalne zarządzanie, Zdrowie sieci, jakość sygnału i LNA, mapę, Mesh Link Planer, diagnostykę, Sniffer, komunikator, listę węzłów i bazę, ustawienia oraz nową sekcję o panelu debugowania z zakładką „Logi urządzenia”. Strona „Logi debugowania” też opisuje trzecią zakładkę.

**Ekran powitalny i dokumentacja o funkcjach MT_SW**
* Po ekranie powitalnym przy pierwszym uruchomieniu jest teraz druga karta z funkcjami tego forka (zdalne sterowanie GPIO, zdalne dodawanie urządzeń do bazy, ulubione i ignorowanie, Zdrowie sieci, mapa z połączeniami i planerem, diagnostyka i Sniffer, skalowanie aplikacji).
* Wersja na komputer też ma ekran powitalny przy pierwszym uruchomieniu (bez ekranów uprawnień); pokazuje się raz, a w Ustawienia → Informacja jest pozycja „Wprowadzenie”, która pokazuje go ponownie.
* Nowa strona „Funkcje MT_SW” w Pomocy i dokumentacji, po polsku i po angielsku.

**Zdrowie sieci — chowająca się karta podsumowania**
* Przesunięcie listy palcem w górę najpierw zwija kartę „Kondycja sieci”, zostawiając wyszukiwanie, zakładki, sortowanie i listę węzłów; przesunięcie w dół do samej góry rozwija ją z powrotem. Można też chwycić samą kartę lub wiersz sortowania. Domyślnie ukrywanie jest zablokowane; przełącznik „Blokuj ukrywanie kondycji sieci przy przesuwaniu listy” jest w oknie informacji („i” u góry). Więcej miejsca na listę, szczególnie przy powiększonym skalowaniu.

**Wygląd — skalowanie aplikacji**
* Nowe ustawienie „Skalowanie aplikacji” (Ustawienia → Wygląd): rozmiar tekstu i elementów tylko w tej aplikacji, niezależnie od czcionki i rozmiaru ekranu w telefonie (50–200%). Działa też w wersji na komputer. Domyślnie „Zgodnie z systemem” — wygląd się nie zmienia. Pomaga, gdy telefon ma powiększone elementy i ekrany (np. Zdrowie sieci) wyglądają za duże.

**Mapa — połączenia między węzłami**
* Nowe linie na mapie między węzłami, które wymieniają pakiety, w kolorze jakości sygnału (SNR). Źródła: raporty sąsiadów (NeighborInfo), traceroute z ostatniej doby i bezpośrednie odbiory podłączonego radia (cieńsze, bledsze). Ta sama para z kilku źródeł jest pokazana raz, według najpewniejszego. Przełącznik „Pokaż połączenia między węzłami” w filtrach mapy, domyślnie włączony. Dane są czytane w tle i odświeżane po 2 s spokoju. Na razie na mapie MapLibre (Android F-Droid i desktop), bez mapy Google.

**Mapa — „Pokaż na mapie” w szczegółach węzła**
* Nowy przycisk „Pokaż na mapie” pod pozycją węzła: przenosi na główną mapę, przybliża do węzła i rysuje go na wierzchu, ponad innymi węzłami w tym samym miejscu (z obrączką wokół dokładnej pozycji). Węzeł jest widoczny także wtedy, gdy filtry mapy by go ukryły. Działa na mapie MapLibre (Android F-Droid i desktop) oraz na mapie Google.

**Mapa — płynniejsze ładowanie**
* Węzły na mapie odświeżają się dopiero po chwili spokoju (0,5 s), zamiast przebudowywać warstwy przy każdym odebranym pakiecie; pierwszy widok i zmiana filtrów działają od razu. Przetwarzanie punktów (waypointów) idzie w tle.

**Wiadomości — szybsza lista rozmów**
* Lista kanałów i wiadomości prywatnych czyta z bazy tylko najnowszą wiadomość każdej rozmowy zamiast wszystkich wiadomości, a liczniki nieprzeczytanych i wiadomości pobiera jednym zapytaniem dla wszystkich rozmów (wcześniej dwa zapytania na każdą rozmowę) i szuka węzłów jednym przebiegiem. Karta wiadomości otwiera się szybciej i nie zwalnia wraz z historią.

**Szczegóły węzła — płynniejsze wejście w historię**
* Wejście w sekcje i historię w szczegółach węzła nie zamula już tak bardzo: dane są budowane raz zamiast dwa razy, filtrowanie i dekodowanie wpisów idzie w tle zamiast na głównym wątku, a nowe pakiety nie przeliczają wszystkiego od razu, tylko po chwili spokoju (pierwsze dane pojawiają się bez opóźnienia).
* Lista węzłów i lista rozmów: filtrowanie i budowanie list idzie w tle zamiast na głównym wątku, więc przewijanie i wpisywanie są płynniejsze przy dużej liczbie węzłów i rozmów.

**Zdrowie sieci — stabilność**
* Zabezpieczenia przed zamknięciem aplikacji w Zdrowiu sieci: błąd przy wczytywaniu danych (zakładki, podsumowanie, szczegóły metryki) jest teraz zapisywany w logu, a ekran zostaje otwarty zamiast zamykać aplikację. Wykresy pomijają błędne wartości (NaN / nieskończoność), a pusty wykres słupkowy, wskaźnik i mapa cieplna nie wywołują już błędu.
* Zdrowie sieci przy dużej ilości danych (np. zakres „Wszystkie”): szczegóły metryki czytają pełną historię węzła z wybranego zakresu (nic nie jest obcinane), sąsiedzi czytają tylko najnowszy raport każdego węzła (to jedyne, czego używają), ciężkie obliczenia (zakładki, podsumowanie, sąsiedzi) idą w tle, a nie na głównym wątku, i czekają aż seria nowych pakietów ucichnie, zamiast przeliczać wszystko przy każdym pakiecie.
* Zdrowie sieci — listy lżejsze: ekrany list (zakładki, podsumowanie, sąsiedzi) czytają dane z ostatnich 24 godzin jednym zapytaniem zamiast czterech i bez tekstowej kopii każdego pakietu, której nigdy nie używają; sąsiedzi pobierają z bazy od razu tylko najnowszy raport każdego węzła. Pełny wybrany zakres (także „Wszystkie”) jest czytany dopiero po wejściu w konkretny węzeł i statystykę.
* Baza danych (migracja 64→65): trzy nowe indeksy w tabeli logów (port + czas, węzeł + czas, port + węzeł + czas), tworzone w miejscu bez kopiowania tabeli. Przyspieszają zapytania ekranów zdrowia sieci i szczegółów węzła. Pierwsze uruchomienie po aktualizacji może chwilę potrwać przy bardzo dużej bazie logów.

**Pasek przewijania — stały rozmiar suwaka**
* Suwak paska przewijania (desktop) ma teraz stały rozmiar zależny od ilości treści i tylko się przesuwa. Wcześniej na listach o różnej wysokości wierszy (np. szczegóły urządzenia) zmieniał rozmiar w zależności od położenia.

**Trasa — ostatni przekaźnik zamiast „Nieznany skok”**
* Naprawa: w historii tras zamiast liczby skoków pokazywało się „%d skoki” — teraz „2 skoki” itd.
* Gdy ostatni węzeł przed nami nie wpisał się do trasy (np. jest na innym kanale), a znamy go z bazy po końcówce adresu, okno wyniku (także przy otwarciu z historii tras) pokazuje go jako „Prawdopodobnie: Nazwa” zamiast „Nieznany skok”. Dotyczy tylko ostatniego skoku trasy powrotnej i tylko gdy pasuje dokładnie jeden węzeł.

**Wyszukiwanie węzłów — bez nakładającej się listy**
* Pole wyszukiwania na liście węzłów jest teraz zwykłym polem tekstowym nad listą: wpisany tekst od razu filtruje tę samą listę, bez otwierania drugiego okna z drugą kopią listy. Naprawia to nakładanie się list i bardzo wolne przewijanie na desktopie po przewinięciu listy i kliknięciu w wyszukiwarkę.

**Wgrywanie firmware — własne pliki i nieznane urządzenia**
* Urządzenie, którego nie ma na liście sprzętu, nie kończy się już błędem ani nie wisi na „Sprawdzanie”: ekran wgrywania przełącza się na „Plik lokalny” i pozwala wgrać własny plik (.zip dla nRF52, .bin dla ESP32) — przydatne dla własnych buildów.
* Przy własnym pliku i nieznanym urządzeniu ekran oraz okno potwierdzenia pokazują wyraźne ostrzeżenie o ryzyku (możliwe uszkodzenie urządzenia, odpowiedzialność użytkownika, co sprawdzić przed wgraniem).
* Nowy przełącznik „Własny plik (bez sprawdzania nazwy)”: plik jest sprawdzany tylko pod kątem typu, a jego nazwa nie musi zawierać nazwy modelu ani kończyć się „-ota.zip”.
* Odzyskiwanie urządzenia zablokowanego w trybie DFU działa teraz także po wgraniu własnego pliku i dla urządzeń spoza listy: aplikacja zapamiętuje próbę, a po przerwaniu pozwala wgrać własny plik bez połączenia z urządzeniem (wcześniej odzyskiwanie było tylko dla wersji z listy i kończyło się błędem przy nieznanym modelu).
* Naprawa desktopu: sprawdzanie dostępności aktualizacji nie czeka już na serwer (pobieranie listy plików konserwacyjnych idzie w tle), więc przy słabym połączeniu ekran nie wisi.

**Pasek przewijania na desktopie jak w Windowsie**
* Pasek przewijania (desktop) ma teraz widoczny, delikatny tor. Kliknięcie w tor przewija od razu w to miejsce — kliknięcie na samym dole przewija na koniec, na samej górze na początek — a przeciąganie suwaka nadal przewija płynnie.

**Scalenie z oryginałem (18.10)**
* Scalenie z najnowszym oryginałem: w pełnym czasie wiadomości widać teraz sekundy, ustawienia pinów GPS są ukrywane tylko na płytkach, na których producent na stałe podłączył GPS (wcześniej na wszystkich płytkach oznaczonych jako DIY), wersja protobufów podniesiona do 2.8.1.14, odświeżone dane o sprzęcie i firmware oraz profile przyspieszające start. Tłumaczenia PL bez zmian (zmieniły się tylko komentarze w plikach schematu). Nasze pozostałe zmiany bez zmian. Biblioteka Bluetooth Kable zostaje przy wersji 0.44.3.

**Scalenie z oryginałem (17.10)**
* Scalenie z najnowszym oryginałem: przycisk „Zachowaj” przy ostrzeżeniu o niesłyszanych węzłach jest teraz zapamiętywany trwale dla każdego węzła osobno (wersja oryginału zastąpiła naszą, która pamiętała tylko do restartu), z aplikacji usunięto deklarację obsługi Android Auto oraz zaktualizowano opisy wydań i profile przyspieszające start. Bez zmian zostały nasze zmiany w forku.

**Trasa — kanał wysyłania i napis czasu**
* Zapytanie o trasę idzie teraz kanałem, na którym ostatnio faktycznie usłyszeliśmy dany węzeł (z ostatnich 3 dni), a nie kanałem zapisanym z jego ostatniego NodeInfo — naprawia brak wysyłania do węzłów pracujących na drugim kanale. Gdy nic nie wiadomo, zostaje dotychczasowy kanał.
* Okno wyniku trasy zamiast angielskiego „Duration” pokazuje „Czas trasy”.

**Szczegóły trasy — czas trasy**
* W szczegółach trasy zamiast „Czas trwania” jest teraz „Czas trasy” — to czas od wysłania zapytania do odebrania odpowiedzi.

**Sniffer — zapamiętane ramki i filtr własnego węzła**
* Sniffer w trybie „Radio”: po wyłączeniu radia (lub rozłączeniu) zapisane ramki już nie znikają — zostają na liście do czasu kliknięcia kosza, tak jak w trybie WiFi/MQTT.
* Nowa opcja w ustawieniach Snifera: „Ukryj ramki własnego węzła” — ukrywa ramki wysyłane przez podłączony węzeł, który zgłasza je na bieżąco w dużej liczbie.

**Scalenie z oryginałem (16.10)**
* Scalenie z najnowszym oryginałem: historia węzła zapisuje traceroute (także obserwowane i odebrane od innych węzłów — z opisem „Zaobserwowano” i „Trasę sprawdził”), nieznane przeskoki w traceroute są podpisane jako nieznane, a długie naciśnięcie traceroute ma przerwę między kolejnymi wysłaniami, wykres traceroute używa tych samych kolorów co mapa, wyszukiwarka ustawień po przejściu do opcji ją podświetla i przewija na środek, długa nazwa węzła jest ucinana do 24 bajtów (firmware 2.8) oraz aktualizacje bibliotek (Firebase 35.0.2, fastlane) i tłumaczeń. Dopisane polskie tłumaczenia trzech nowych tekstów. W forku zostają bez zmian: kolory i podpisy SNR w traceroute, ostrzeżenie przy „Nadpisz cykl pracy”, ukryte opcje oraz biblioteka Kable przypięta do 0.44.3.

**Scalenie z oryginałem (15.10)**
* Scalenie z najnowszym oryginałem: kopia zapasowa i przywracanie danych aplikacji (wiadomości i węzły) w ustawieniach radia, ustawienia radia można otwierać i czytać bez połączenia z radiem (zapis wymaga połączenia), wybór koloru z gotowymi kolorami przy oświetleniu, ponawianie nieudanej wiadomości w tym samym miejscu, karta połączonego radia pokazuje preset LoRa i region, wymóg aktualizacji firmware (stare radio zostaje połączone, ale aplikacja prosi o aktualizację), przełączniki powiadomień o reakcjach i nowych waypointach na pulpicie, wyszukiwarka ustawień przechodzi do konkretnej opcji, nazwy nieznanych przeskoków w traceroute oraz aktualizacje bibliotek (Compose Multiplatform 1.12.2, markdown 0.7.18, kotest, Firebase) i tłumaczeń. Dopisane polskie tłumaczenia nowych tekstów. W forku zostają bez zmian: wyłączone automatyczne udostępnianie kontaktu i ulubione przy wiadomościach prywatnych (także przy ponawianiu), niższy próg firmware dla trybu blokady oraz kolory i podpisy SNR w traceroute. Biblioteka Bluetooth Kable zostaje przypięta do 0.44.3.

**Ekran połączenia — preset i region w dwóch liniach**
* Pod nazwą połączonego radia preset LoRa jest teraz w pierwszej linii, a region w drugiej (mniejszą, szarą czcionką). Długie nazwy zawijają się do dwóch linii zamiast się ucinać.

**Zdrowie sieci — niebieski jako brak danych**
* Węzeł bez danych w zakładce, która go ocenia (Zasilanie, Sygnał, Eter, Sąsiedzi), ma teraz niebieski kolor i napis „brak danych”, żeby nie mylić go z oceną. Biały zostaje wyłącznie dla „bardzo źle”. Zakładki bez ocen (Środowisko, Zasoby, Ruch) też liczą brak danych: węzeł, który coś tam zgłasza, jest „ma dane” (złoty), a bez danych — niebieski; liczniki w karcie u góry można klikać. Gdy większość węzłów nie ma nic na danej zakładce, jej ikona na pasku zakładek robi się niebieska.
* Liczba węzłów bez danych jest też w karcie „Kondycja sieci” u góry (niebieski kółko i licznik, który można kliknąć, żeby je pokazać), więc wszystko sumuje się do liczby wszystkich węzłów. Procent „dobrze” nadal liczy się tylko z węzłów, które mają dane.
* W „Podsumowaniu sieci” paski rozkładu baterii i sygnału pokazują też, ile węzłów nie ma danych (niebieski), więc sumują się do wszystkich węzłów.

**Ustawienia LoRa — opis „Nadpisz cykl pracy”**
* Opcja ma teraz opis i wyraźne ostrzeżenie wielkimi literami pod nim: włączenie jej łamie przepisy w większości regionów i wolno to robić tylko tam, gdzie pozwalają na to przepisy.

**Niesłyszane węzły i częstotliwość**
* Węzeł, od którego właśnie odebrano pakiet przez radio (LoRa, nie MQTT), przestaje być oznaczany jako niesłyszany. Wcześniej radio podawało tę ocenę tylko przy łączeniu, więc ikona przekreślonego zasięgu zostawała mimo odświeżającego się czasu.
* Pole „Częstotliwość nadpisana” znów pokazuje pełną rozdzielczość (do 5 miejsc po przecinku, np. 869,44165), zamiast zaokrąglać do 3.
* Szczegóły węzła w „Zdrowiu sieci”: przy 30 dniach i „Wszystko” aplikacja wczytuje już tylko dane wybranego węzła, a nie telemetrię całej sieci, co mogło ją wywalać (brak pamięci).

**Diagnostyka na żądanie — nazwa paska heap**
* Pasek pamięci heap pokazuje zajętą pamięć, więc nazywa się teraz „Zajęty heap” (wcześniej „Wolny heap”). Lista wartości poniżej nadal podaje wolny i łączny heap.

**Zdrowie sieci — szczegóły węzła: więcej zakresów i płynność**
* Zakresy czasu jak w metrykach urządzenia: 1 h, 24 h, 7 dni, 2 tygodnie, 30 dni i „Wszystko”.
* Wykresy Zegar, Słupki i Mapa cieplna nie zacinają się już przy długich zakresach: dane są liczone poza głównym wątkiem i zmniejszane do rozsądnej liczby punktów, a mapa cieplna jest rysowana jako jeden obraz (pokazuje do 31 dni).

**Zdrowie sieci — filtr po ocenie**
* Kliknięcie w liczbę w karcie „Kondycja sieci” (np. „3 Źle”) pokazuje na liście tylko węzły z tą oceną; ponowne kliknięcie pokazuje wszystkie. Wybrana ocena jest podświetlona.
* Każda zakładka (Zasilanie, Sygnał, Eter…) pamięta swój filtr osobno, więc po przełączeniu zakładek każda zachowuje własny wybór.

**Zdrowie sieci — kolor szumu tła**
* Zakładka Sygnał: liczba (szum tła) ma teraz własny kolor — im niższa wartość (większy minus), tym lepiej. Progi jak w „Jakości sygnału”: poniżej −100 dBm złoty, od −100 do −90 dBm czerwony, od −90 do −80 dBm fioletowy, wyżej biały. Pasek, tło wiersza, wykres i ocena „Kondycji sieci” wynikają z tej samej wartości; węzeł, który nie wysyła szumu, nie dostaje koloru ani paska (SNR już go nie ocenia).
* Szum jest oceniany po korekcie LNA węzła (wcześniej kolor liczył się z wartości przed korektą, więc nie zgadzał się z tym, co widać). Dotyczy listy, szczegółów węzła i wykresu z progami.
* Zakładka Sygnał: kolor, pasek i ocena wynikają teraz wyłącznie z szumu tła. Węzeł bez szumu (np. słyszany tylko bezpośrednio) nie ma koloru, więc lista zgadza się z tym, co na niej widać.

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

* The assistant knows the app: built-in knowledge from the documentation and changelog (picked per question), a small set of tools chosen by the topic of the question, a larger context window for larger models, and a log line showing which backend (NPU/GPU/CPU) the model runs on.
* The assistant shows **buttons** under an answer (open a screen, show a setting with a yellow blink, node on the map, link planner, message draft); nothing happens by itself and a message is never sent automatically.
* **Read aloud** for assistant answers and chat messages (long-press menu) and dictation of the question (Android).
* **Internet for the assistant**: a single switch, off by default, with a warning and a visible indicator. On, it allows web search (the query and sources are shown, web text is information only) and automatic, verified download of news about the app from our repository; off = fully offline.
* Chat: the translate-before-sending button is now a small grey "Translate" button above the text field.
* MT_SW features: shortcuts to the theme and the debug panel.
* Node colour picker: any colour (code, hue/saturation/brightness), recently used colours, "Fast" and "Precise" modes instead of tolerance, a timer and cancelling of the search.
* Black-and-white themes: on the map the lines, node markers and tracks are grey too, and the top bar and the bottom navigation (connection icon, titles, warning triangle) stay black and white.
* Chat menu (three dots): read aloud and translate the visible messages or the whole conversation.
* Translator: the translated text can be read aloud.
* Option to move the add / share button from over the node and conversation lists to the top bar (off by default).
* Assistant: more reliable answers about the network: follow-up questions reuse the previous topic, rankings of received and sent packets (highest or lowest), who was heard last or is silent for longest, roles and favourites.
* **Dictation** in the assistant works the same on phone and computer, without system services: microphone + Gemma E2B/E4B model, entirely on the device. Microphone also in chats (channels, direct messages) and in the translator; shown only with the assistant on and a hearing model chosen.
* Assistant: questions about "our project", goals and achievements are answered about MT_SW (from the manual and change list); the main Meshtastic is mentioned only briefly unless asked; network data is attached only when the question is about it.
* The assistant knows the whole MT_SW project (firmware, Home Assistant integration, compatibility with the main network), its authors and achievements; this knowledge is packaged in the app, refreshed on every build (and from the internet, when the assistant's internet access is on) and visible only to the assistant.
* Assistant: the network and own-node description is written in Polish when the user writes Polish (distances in words instead of number pairs).
* Assistant: a tool call written as plain text without brackets (e.g. "show_setting ...") also becomes a button, and the place is looked up from the user's own words first.
* Assistant: **memory between conversations**: it saves short notes in several files (about you, devices and network, conversation style, projects) on its own, and you can view, edit and delete them and write your own instructions; a switch in the assistant settings, a notice above the conversation, everything stays on the device.
* The assistant answers more naturally, like in a normal conversation, without stiff phrases.
* Chat: the formatting buttons sit in the same line as the "Translate" button, at the right edge.
* Chat: a message split into several parts no longer attaches itself to the previous sender (missing name, joined corners, wrong colour); neighbours are counted from the visible bubbles only.
* Assistant: a node is also recognised by a place name in another Polish case ("router na Łysicy"), a question naming a node always gets its data, and a written "show_node_on_map" without a name becomes a button for the node in the question.
* Assistant: questions like "where is the nearest router / sensor / repeater" get a list of the nearest nodes of that role with position and distance.
* Assistant: more reliable wording (lower model randomness), it looks things up instead of asking back, and the memory notes are written only from what the user wrote.
* How long received packets (logs) are remembered: a choice in days from 1 to 60 (1, 2, 3, 7, 14, 30, 60) instead of one hour to one year, also in the regular Settings; an earlier choice is kept until you pick a new one.
* By default the app now remembers received packets for **60 days**, and the list of recent Wi-Fi addresses holds up to **30** entries.
* Assistant: when a small model writes a tool call as plain text instead of using the tool, it is turned into a real "Go to" button; the note above the chat tells the truth about whether internet access for the assistant is on.
* Assistant: channels are also recognised by number ("channel 1", "channel one"), so a summary covers the right channel; message counts per channel are available to the model as a tool.
* Heap and PSRAM memory are now shown as **used** (not free) everywhere: node list, resource charts, network health, widget, notification, packet view, on-demand log and the assistant. E.g. 10% free now shows as 90% used.
* Fixed the app closing on the phone when using the translator (after release optimisation, ONNX library classes were missing).
* Assistant: sensor and range data reach the model in a clean, laid-out form (with units, impossible values left out); the model always answers, nothing is answered by fixed code.
* Assistant: sensor readings that cannot be real (e.g. 25252 °C) are marked as "probably a faulty sensor", and an answer that starts repeating itself is cut after the first occurrence.

**Merge with the original — version 2.8.4 (start)**
* Merged the original after the 2.8.3 release: app version number 2.8.4, newer protobufs (2.8.1.18), refreshed hardware and firmware data, translations and store screenshots. The Kable Bluetooth library stays pinned to 0.44.3, and our Polish translations are unchanged.
* Second merge with the original: the new **AR mesh view** (nodes and waypoints over the phone camera), the **3D device model viewer**, the quick actions "Open compass" and "Open mesh view" in the node menu, protobufs 2.8.1.20 and newer build tooling. New strings are translated to Polish, and our features (e.g. the planner naming, Network Health) are kept.
* Third merge with the original: a Quick Settings tile for the radio connection on Android, mesh view pictures taken through the camera, track direction and time fade plus barometric height in the mesh view, a new device model (Chirpy) in the 3D viewer, settings fixes (MQTT password capped at 31 bytes, the LED heartbeat switch now does what it says, option lists show only what the firmware supports), translated air-quality levels and newer libraries. Our planner stays as it is.
* The quick reaction bar now reads, from the left: the three dots (more actions), add reaction, then the most used emoji (the most used next to the add button). Before, the options were hidden behind the emoji and easy to miss.
* Two new **black and white** themes (light and dark) in the theme picker: the light black and white theme now also works when the phone is set to dark mode; the whole inside of the app is in shades of grey, including signal, charts, node colours, pictures and the map. For people who do not like our colours.
* The desktop version shows up in the system (for example in the macOS "reopen the app?" window) under the name **MT_SW_APP** instead of "Meshtastic Desktop".
* New **"MT_SW features"** group in Settings (one entry, separate screen): the chat link-image switch, update notifications and node list layout moved from the main screen; shortcuts to app scaling, LoRa, node database cleaning and Sniffer; an "Other features" entry describes the features that live next to a node. Works on phone and desktop and is found by the settings search.
* Optional **offline translator and assistant** (off by default, switched on separately): they run entirely on the device, without internet. Translates received messages, translates messages before sending (with the original and translation shown side by side) and adds a "Translator" tab to the bottom bar ("AI" once the assistant is on). A model list with three levels (light, standard, strong), each downloaded and deleted on its own, a choice of the model used separately for the translator and the assistant, and warnings about size and RAM. Information about local processing and model licences. Models: NLLB plus Gemma 4 E2B, E4B and 12B (Apache 2.0) on Google's LiteRT-LM engine, using the graphics chip when the device supports it; the model chosen in settings decides which engine translates. Gemma leaves proper names untouched. The assistant is a chat in bubbles with no limit on the length of the conversation (the older part is kept in a summary, answers are not cut off) with a press-and-hold menu (copy, ask again; after a failed answer the question returns to the input box) (yours on the right, the assistant's on the left, with pulsing dots while it thinks and suggested questions to start) with answers appearing as they are written, with a stop button, a conversation history saved only on the device (clock icon: continue and delete) and a "New conversation" button, The assistant fetches the app's data itself through read-only tools (network overview, names and history of any node, node rankings, sensors and weather, radio settings, message search), so it is not limited to topics chosen in advance. It has read-only access to the app's data: the own node, any node of the network (by the name in the question) with its whole recorded history and trend (for example RAM/heap, CPU, battery, channel utilization, signal), a summary of the whole network, the radio settings (without keys and passwords) and the latest messages; it never changes anything or sends anything. Also and a note that answers may be wrong; it runs on the same model as the translator (requests are queued). The first translation or answer takes longer because the model is loaded into memory, so a progress bar is shown. The "MT_SW features" entry moved to Settings → Advanced, above the firmware update.

**Extra data under a node, and a sturdier Network Health**
* In the desktop app the node list and the node detail start at half the width each (the divider can be dragged). The "App scale" setting is now called "Follow the system", not "the phone".
* In the desktop app the Settings screen now has a back arrow while remotely administering another node (as on the phone); before, there was no way back from it.
* A new extra line under every node in the list. The "Environment metrics" switch shows voltage and current at the top and all weather sensors under them. Below, in the "Extra statistics" settings, separate switches (all on by default): ether (channel utilization, air time, noise floor -- with short labels; on the "Hops" line, icons only from the right, packets (TX, RX, bad, duplicates) and hardware (CPU, memory, the node's Wi-Fi strength); packets and hardware have their own lines at the right edge. Voltage and current in the extra line come from the sensor (environment or power telemetry), not from the basic telemetry next to the battery. All of it is the latest data from the node's packets; nothing is measured live. Pages or all at once, an "icons only" option with an (i) legend, and the preview in the settings shows changes at once.
* Network Health: foreign or broken packets (e.g. from other firmware) can no longer freeze or crash the screen; it now tries to recover after an error and no longer reads on-demand mode packets.

**Picking a Wi-Fi network from a list**
* In the radio's network settings there is a new "Pick a Wi-Fi network from the list" button: it shows the 2.4 GHz networks around the phone or computer (current network on top, signal strength, a padlock on networks with a password). After you pick a network with a password, a separate window asks for the password, and after "Connect" the radio gets the network and the password at once (like "Send") and joins it; a network without a password connects at once. The app never reads the phone's saved passwords.

**Firmware-update mini-game**
* The character in the game (also the dazed one, and the one on the update and help screens) is now fully yellow and wears the MT_SW logo from the app icon instead of the green colour and the original mark.

**Periodic notices**
* Minor changes to when periodic notices appear in the app.

**Muting a device on a channel and one set of mute lengths**
* Long-press a message → "More actions" → "Mute messages from this device on this channel". Messages are still saved and visible; only notifications and sound are off, only on that channel. A small speaker icon appears by the name, the mute survives a restart and can be undone from the same menu.
* Mute lengths are now the same everywhere (private chats, channels, a device on a channel): 1 hour, 24 hours, 2 days, 1 week, 2 weeks, 1 month, always. Older mutes (e.g. 8 hours) keep working until they expire.

**(i) icon on the conversation list and new descriptions**
* The conversation list has an (i) icon explaining touch, touch & hold, swipe, muting and the tappable delivery labels (how many devices relayed the message and which — only in this app).
* The debug panel has an (i) icon describing the Packets, App logs and Device logs tabs; the map's node-links toggle has a short description.

**New "2 days" range option**
* Everywhere a data range is chosen (node statistics charts, metric details) there is now "2 days" between 24 h and 7 days.

**Loading data — "Loading data…"**
* Statistics and network-health screens show a spinner with "Loading data…" instead of looking frozen; changing the range in a metric detail also shows the spinner in place of the charts, and the range buttons respond at once.
* Node details: the statistics icons appear faster (lighter loading at start).

**ESP32 flashing from the app (OTA) — -full.bin notice**
* On the update screen, when choosing your own file for ESP32 devices, a notice explains that the `-full.bin` file (which erases all settings) must first be flashed through a web page (the original or another one) — flashing it through the app will not work. Anyone who already flashed with the original flasher including an erase is set.

**Desktop look and dialogs**
* The desktop welcome screen's bottom bar now spans the full width, without lighter margins at the sides.
* Lighter desktop start: a bigger starting memory, and the node cleanup begins only after a minute so it does not slow down opening.
* The node list no longer asks the database for device icons again on every new packet (phone and desktop), so it stutters less after connecting to the radio.
* The small map in node details is no longer rebuilt each time further statistics rows appear in the details (phone and desktop).
* Node details on the desktop open more smoothly: the small map starts a moment after the rest of the screen.
* Dialog windows (e.g. routes) now follow the "App scale" setting.

**Node details — statistics icons appear faster**
* On first opening a node's details, the icons that open its statistics no longer wait for the whole history (up to 5000 entries) to load: they first appear from the newest ~300 entries almost at once, and the full history loads in the background.

**“MT_SW features” documentation completed**
* The page now covers all fork features: remote management, Network health, signal quality and LNA, map, Mesh Link Planner, diagnostics, Sniffer, messaging, node list and database, settings, and a new section about the debug panel with the “Device logs” tab. The “Debug logs” page also describes the third tab.

**Welcome screen and documentation about MT_SW features**
* After the welcome screen on first launch there is now a second card with this fork's features (remote GPIO control, adding devices to a node's database remotely, favourites and ignore, Network health, the map with links and the planner, diagnostics and Sniffer, app scale).
* The desktop version also has a welcome screen on first launch (without the permission screens); it is shown once, and Settings → Info has a "Show Introduction" item that shows it again.
* New "MT_SW features" page in Help and documentation, in Polish and English.

**Network health — collapsing summary card**
* Scrolling the list up first folds away the "Network health" card, leaving search, tabs, sorting and the node list; scrolling back down to the top unfolds it again. You can also grab the card itself or the sorting row. Folding is locked by default; the lock switch is in the info window ("i" at the top). More room for the list, especially at larger scales.

**Appearance — app scale**
* New "App scale" setting (Settings → Appearance): size of text and elements in this app only, independent of the phone's font and display size (50–200%). Also on desktop. Default is "Follow the system", so nothing changes. Helps when the phone has enlarged items and screens (e.g. Network health) look too big.

**Map — links between nodes**
* New lines on the map between nodes that exchange packets, coloured by signal quality (SNR). Sources: neighbour reports (NeighborInfo), traceroutes of the last day and the connected radio's direct receptions (thinner, fainter). The same pair known from several sources is drawn once, by the most certain one. A “Show links between nodes” switch in the map filters, on by default. The data is read in the background and refreshed after 2 s of quiet. For now on the MapLibre map (Android F-Droid and desktop), not on the Google map.

**Map — “Show on map” in the node details**
* A new “Show on map” button under the node's position: opens the main map, zooms to the node and draws it on top of any other nodes on the same spot (with a ring around its exact position). The node is visible even when the map filters would hide it. Works on the MapLibre map (Android F-Droid and desktop) and on the Google map.

**Map — smoother loading**
* Nodes on the map refresh only after a short quiet moment (0.5 s) instead of rebuilding the layers on every received packet; the first view and filter changes still apply at once. Waypoint processing runs in the background.

**Messages — faster conversation list**
* The channels and direct messages list reads only the newest message of each conversation from the database instead of all messages, fetches the unread and message counters for all conversations in one query (previously two queries per conversation) and looks nodes up in a single pass. The messages tab opens faster and no longer slows down as history grows.

**Node details — smoother history screens**
* Opening the sections and history in node details no longer lags as much: the data is built once instead of twice, filtering and decoding of log entries runs in the background instead of on the main thread, and new packets no longer trigger an immediate full recalculation, only after a short quiet moment (the first data still shows without delay).
* Node list and conversation list: filtering and building the lists runs in the background instead of on the main thread, so scrolling and typing stay smoother with many nodes and conversations.

**Network health — stability**
* Crash guards in Network health: an error while loading the data (tabs, summary, metric detail) is now logged and the screen stays open instead of closing the app. Charts skip invalid values (NaN / infinity), and an empty bar chart, gauge or heatmap no longer throws.
* Network health with a lot of data (e.g. the "All" range): metric detail reads the node's full history for the chosen range (nothing is cut off), neighbours read only each node's newest report (the only thing they use), the heavy calculations (tabs, summary, neighbours) run in the background instead of on the main thread, and wait for a burst of new packets to settle instead of recalculating on every packet.
* Network health — lighter lists: the list screens (tabs, summary, neighbours) read the last 24 hours with one query instead of four and without the text copy of every packet that they never use; neighbours fetch only each node's newest report straight from the database. The full chosen range ("All" included) is read only once you open a specific node and statistic.
* Database (migration 64→65): three new indices on the log table (port + time, node + time, port + node + time), created in place without copying the table. They speed up the queries of the network health and node detail screens. The first start after the update can take a moment with a very large log database.

**Scrollbar — constant thumb size**
* The desktop scrollbar thumb now keeps one size based on the amount of content and only moves. Previously, on lists with rows of different heights (e.g. device details) it changed size depending on the scroll position.

**Traceroute — last relay instead of "Unknown hop"**
* Fix: the route history showed a raw "%d skoki" placeholder instead of the hop count (Polish plural).
* When the last node before us did not add itself to the route (e.g. it is on another channel) but we know it from the node database by its address ending, the result window (also when opened from the route history) shows it as "Probably: Name" instead of "Unknown hop". Only the last hop of the return route is resolved, and only when exactly one node matches.

**Node search — no more overlapping list**
* The node list search is now a plain text field above the list: typing filters that same list in place instead of opening a second window with a duplicate list. This fixes the overlapping lists and very slow scrolling on desktop after scrolling the list and clicking the search field.

**Firmware update — own files and unknown devices**
* A device that is not in the hardware list no longer ends in an error or hangs on "Checking": the update screen switches to "Local file" and lets you flash your own file (.zip for nRF52, .bin for ESP32) — useful for self-built firmware.
* For an own file and an unknown device, both the screen and the confirmation dialog show a clear risk warning (possible damage to the device, user responsibility, what to check before flashing).
* New switch "Own file (skip name check)": the file is checked for its type only; its name does not have to contain the model name or end with "-ota.zip".
* Recovery of a device stuck in DFU mode now also works after flashing an own file and for devices outside the list: the app remembers the attempt and, after an interruption, lets you flash an own file without a connection to the device (before, recovery covered listed releases only and failed with an unknown model).
* Desktop fix: the update check no longer waits for the server (the maintenance-file list is fetched in the background), so a poor connection no longer leaves the screen hanging.

**Desktop scrollbar like in Windows**
* The desktop scrollbar now shows a faint track. Clicking the track scrolls straight to that spot — clicking the very bottom scrolls to the end, the very top to the start — and dragging the thumb still scrolls smoothly.

**Merge with the original (18.10)**
* Merge with the latest original: the full message time now shows seconds, the GPS pin settings are hidden only on boards where the maker wired the GPS (before: on every board tagged as DIY), the protobufs version raised to 2.8.1.14, refreshed hardware and firmware data and startup profiles. Polish translations unchanged (only comments in the schema files changed). Our other changes are unchanged. The Bluetooth library Kable stays pinned to 0.44.3.

**Merge with the original (17.10)**
* Merge with the latest original: "Keep" on the unheard-nodes warning is now remembered permanently per node (the original's version replaced ours, which only remembered until a restart), the Android Auto declaration was removed from the app, and release notes and start-up profiles were updated. The fork's own changes are untouched.

**Traceroute — sending channel and time label**
* A traceroute request now goes out on the channel on which the node was actually last heard (within the last 3 days) instead of the channel stored from its last NodeInfo — fixes requests not leaving for nodes that work on a secondary channel. With nothing known, the previous channel is kept.
* The traceroute result window shows a localised "Route time" instead of the English "Duration".

**Traceroute details — route time**
* In the traceroute details the line formerly labelled "Duration" now reads "Route time" (Polish: „Czas trasy”) — the time from sending the request to receiving the reply.

**Sniffer — kept frames and own-node filter**
* Sniffer in "Radio" mode: switching the radio off (or disconnecting) no longer makes the saved frames disappear — they stay in the list until the trash icon is pressed, as in WiFi/MQTT mode.
* New Sniffer setting: "Hide frames from my own node" — hides frames sent by the connected node, which reports them live in large numbers.

**Connections screen — preset and region on two lines**
* Under the connected radio's name the LoRa preset is now on the first line and the region on the second (smaller, grey text). Long names wrap to two lines instead of being cut off.

**Network Health — blue for no data**
* A node without data in a tab that rates it (Power, Signal, Ether, Neighbours) now shows in blue with the "no data" label, so it is not mistaken for a rating. White stays only for "very bad". Tabs without ratings (Environment, Resources, Traffic) count missing data too: a node that reports anything there is "has data" (gold), one without data is blue; the counts in the top card are tappable. When most nodes have nothing on a tab, its icon in the tab bar turns blue.
* The number of nodes without data is also in the "Network health" card at the top (a blue ring segment and a tappable count that lists them), so everything adds up to all nodes. The "good" percentage is still counted only from nodes that have data.
* In the "Network summary" the battery and signal distribution bars also show how many nodes have no data (blue), so they add up to all nodes.

**LoRa settings — "Override Duty Cycle" description**
* The option now has a description and a clear warning in capital letters below it: enabling it breaks the regulations in most regions and may be done only where the regulations allow it.

**Unheard nodes and frequency**
* A node from which a packet was just received over the radio (LoRa, not MQTT) is no longer marked as unheard. Before, the radio reported that rating only at connection time, so the crossed-out signal icon stayed while the time kept refreshing.
* The "Override frequency" field shows full resolution again (up to 5 decimals, e.g. 869.44165) instead of rounding to 3.
* Node details in "Network Health": for 30 days and "All" the app now loads only the chosen node's data, not the whole network's telemetry, which could crash it (out of memory).

**On-demand diagnostics — heap bar name**
* The heap memory bar shows used memory, so it is now called "Heap Used" (before: "Heap Free"). The value list below still shows free and total heap.

**Network Health — node details: more ranges and smoothness**
* Time ranges as in device metrics: 1 h, 24 h, 7 days, 2 weeks, 30 days and "All".
* The Gauge, Bars and Heatmap views no longer freeze on long ranges: data is computed off the main thread and reduced to a sensible number of points, and the heatmap is drawn as one image (showing up to 31 days).

**Merge with the original (16.10)**
* Merge with the latest original: node history now records traceroutes (including ones received from other nodes — labelled "Observed" and "Traced by"), unknown traceroute hops are labelled as unknown and a long-press traceroute has a cool-down between sends, the traceroute chart uses the same colours as the map, the settings search highlights the option it lands on and scrolls it to the centre, a node's long name is capped at 24 bytes (firmware 2.8), plus library (Firebase 35.0.2, fastlane) and translation updates. Polish translations added for the three new texts. The fork keeps unchanged: the SNR colours and labels in traceroute, the warning at "Override Duty Cycle", the hidden options and the Kable library pinned to 0.44.3.

**Merge with the original (15.10)**
* Merge with the latest original: backup and restore of the app data (messages and nodes) in the radio settings, radio settings can be opened and read without a connected radio (saving needs a connection), a colour picker with preset colours for the lighting, resending a failed message in place, the connected-radio card shows the LoRa preset and region, a firmware update requirement (an old radio stays connected, but the app asks for an update), notification switches for reactions and new waypoints on the desktop, the settings search jumps to the exact option, names for unknown traceroute hops, and updates of libraries (Compose Multiplatform 1.12.2, markdown 0.7.18, kotest, Firebase) and translations. Polish translations added for the new texts. Unchanged in the fork: automatic contact sharing and favouriting on direct messages stay off (also when resending), the lowered firmware limit for lockdown mode, and the SNR colours and labels in traceroute. The Bluetooth library Kable stays pinned to 0.44.3.

**Network Health — filter by rating**
* Tapping a count in the "Network condition" card (e.g. "3 Bad") lists only the nodes with that rating; tapping again shows all. The chosen rating is highlighted.
* Every tab (Power, Signal, Ether…) keeps its own filter, so each one retains its own choice when you switch tabs.

**Network Health — noise floor colour**
* Signal tab: the number (noise floor) now has its own colour — the lower the value (the bigger the minus), the better. Limits as in "Signal quality": below −100 dBm gold, −100 to −90 dBm red, −90 to −80 dBm purple, higher white. The bar, the row tint, the chart and the "Network health" rating follow the same value; a node that sends no noise floor gets no colour and no bar (it is no longer rated by its SNR).
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
