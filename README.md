# MT_SW_APP — osobisty fork Meshtastic-Android

Fork oficjalnej aplikacji [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android) rozwijany na potrzeby sieci mesh radiowej **Świętokrzyskie** (mt-sw.pl). Bazuje na architekturze KMP/Compose Multiplatform oryginału (warianty `fdroid`/`google`, moduł desktopowy) i dokłada zestaw lokalnych funkcji, poprawek i personalizacji, których nie ma w wersji upstream.

Stan roboczy — repo służy głównie do własnego użytku i testów z niewielką grupą osób, niekoniecznie buduje się na bieżąco.

## Zarządzanie węzłami i siecią mesh

- **Zdalne sterowanie GPIO** — na ekranie szczegółów węzła (moduł Remote Hardware) można wpisać numer pinu, appka sama liczy maskę bitową i wysyła `WRITE_GPIOS`/`READ_GPIOS` do zdalnego węzła. Przyciski aktywne tylko gdy klucze PKC z węzłem zostały wymienione.
- **Zdalne ulubione/ignorowanie węzłów** przez sieć LoRa (nie tylko lokalnie) — z prawdziwym potwierdzeniem doręczenia opartym o routing ACK z mesh, zamiast tylko zmiany po stronie telefonu.
- **Ręczne dodawanie kontaktu przez ID węzła** — zarówno lokalnie, jak i zdalnie, z ujednoliconym formatem `!a1b2c3d4` (hex) wszędzie w appce.
- **Pasywne zbieranie NeighborInfo** — log sąsiadów pokazuje teraz też podsłuchane rozgłoszenia innych węzłów, nie tylko odpowiedzi na własne zapytania; naprawiony też przypadek żądania Neighbor Info dla własnego, lokalnie podłączonego urządzenia (wcześniej nic nie zwracał).
- **Przyciski szybkich komend** (`/ping`, `/hello`, `/test`) na ekranie węzła — wysyłają wiadomość prywatną nawet do węzłów, których rola normalnie blokuje ręczne wiadomości; przydatne do szybkiego testowania nowych buildów firmware na urządzeniach w terenie.
- **Lista węzłów pośredniczących (relay) w dostarczeniu wiadomości** — dialog "Status doręczenia" pokazuje pełną listę nazw wszystkich węzłów biorących udział w retransmisji (nie tylko licznik ani jedną nazwę jak w oryginale), łącznie z poprawkami po stronie firmware, żeby te dane w ogóle docierały do appki.
- **Nazwa węzła pośredniczącego (relay) widoczna wprost na liście węzłów i w szczegółach węzła** — nie trzeba już otwierać dialogu "Status doręczenia", żeby zobaczyć przez kogo dany węzeł się łączy.
- Przywrócony ekran konfiguracji **Traffic Management** (w pewnym momencie usunięty w upstreamie).
- **Naprawiony nieaktualny czas ostatniej pozycji dla węzłów ze stałą (ręcznie wpisaną) lokalizacją** — firmware nie odświeża czasu przy retransmisji tej samej pozycji, appka pokazuje teraz realny czas ostatniego kontaktu z węzłem zamiast zamrożonej daty sprzed dni.
- **Ukryte nieaktualne role urządzenia** (REPEATER, ROUTER_CLIENT) na liście wyboru roli w konfiguracji urządzenia — firmware ich już nie wspiera, więc nie da się ich przez pomyłkę wybrać.
- **Spolszczone nazwy ról urządzeń** — appka wcześniej pokazywała surową, angielską nazwę techniczną roli (np. `ROUTER_LATE`) wszędzie: w ustawieniach, na liście węzłów, w szczegółach węzła i w telemetrii; teraz każda rola ma właściwą polską nazwę (np. "Router pomocniczy"), a opisy ról też odwołują się do nich po polsku zamiast po angielsku.
- **Nowa sekcja "Role" w oknie pomocy listy węzłów** — pełny opis wszystkich ról urządzenia (ikona, nazwa, znaczenie) dostępny bezpośrednio z listy węzłów.
- **Nowe sekcje "Bezpieczeństwo" i "Połączenie" w oknie pomocy listy węzłów** — legenda wszystkich stanów ikony bezpieczeństwa węzła (kolory, ikony, znaczenie) oraz stanów i kolorów sygnalizacji połączenia z urządzeniem (status, miganie przy nadawaniu/odbieraniu), dostępna bezpośrednio z listy węzłów.
- Poprawiony wygląd czasu działania (uptime) na liście węzłów — dodana ikonka odróżniająca go wizualnie od czasu ostatniego kontaktu.
- **Automatyczne czyszczenie bazy węzłów** — na ekranie "Wyczyść bazę węzłów" można włączyć automatyczne usuwanie nieaktywnych węzłów: suwak progu nieaktywności (1–90 dni) i suwak częstotliwości sprawdzania (1–30 dni). Działa zarówno na Androidzie (WorkManager), jak i na desktopie (własna pętla sprawdzająca co godzinę, z zapamiętanym czasem ostatniego uruchomienia, żeby przetrwać restart appki); domyślnie wyłączone, węzły ulubione i ignorowane nigdy nie są usuwane automatycznie.
- **Naprawione gubienie/przestawianie znaków w polu wyszukiwania listy węzłów** — gdy lista była przewinięta niżej, wpisywanie tekstu w trakcie przeliczania przewijania mogło przestawiać kolejność wpisywanych znaków; pole ma teraz własny, natychmiastowy stan wpisywania niezależny od odświeżania listy.

## Mapa

Silnik mapy (Android i desktop) korzysta teraz ze współdzielonej, natywnie renderowanej biblioteki **MapLibre** — przyjętej z upstreamu zamiast wcześniejszego, własnoręcznie napisanego renderera (osmdroid na Androidzie, autorski renderer kafelków OSM na Compose Canvas na desktopie). Powody tej zmiany: wydajniejsze natywne renderowanie (GPU) i brak konieczności utrzymywania osobnego silnika mapy przy każdej synchronizacji z upstreamem.

Co to oznacza w praktyce:

- **Desktop ma teraz mapę "za darmo"** — wcześniej zakładka "Mapa" na desktopie była pustym placeholderem, potem dorobiona jako własny renderer od zera; teraz korzysta z tego samego współdzielonego komponentu co Android, więc funkcje typu import warstw GeoJSON/KML, pobieranie kafelków offline czy filtry mapy pochodzą już ze wspólnego modułu, a nie z osobnej implementacji tylko dla desktopu.
- **Site Planner** (symulacja zasięgu radiowego) na desktopie działa teraz przez przeglądarkę zamiast wcześniejszej integracji z wbudowaną przeglądarką Chromium (JCEF) pisaną specjalnie pod ten fork.
- Mini-mapa w szczegółach węzła, mapa trasy pozycji i główny ekran mapy korzystają z tego samego, wspólnego komponentu na obu platformach.
- Kliknięcie węzła na mapie desktopowej nadal otwiera najpierw listę węzłów, potem szczegóły — to zachowanie przetrwało przejście na nowy silnik mapy.
- **Domyślnie włączone nakładki terenu i pogody** — nakładka cieniowania rzeźby terenu (hillshade, przydatna do oceny zasięgu LoRa ograniczonego ukształtowaniem terenu) oraz radar pogodowy NOAA są teraz zaznaczone od razu po otwarciu mapy, zamiast wymagać ręcznego włączenia w warstwach.
- **Mniejsze plakietki węzłów z ogonkiem wskazującym dokładną pozycję** — plakietka jest teraz kompaktowa, z małym zaokrąglonym ogonkiem pod spodem wskazującym dokładny punkt GPS węzła, zamiast być wyśrodkowana na nim.
- **Węzły na tej samej pozycji GPS są od siebie subtelnie odsunięte** — kilka urządzeń zgłaszających identyczną (np. ustawioną na sztywno) lokalizację jest teraz rozstawionych o kilka metrów w rzeczywistości: niewidoczne przy oddaleniu, ale pozwalające zobaczyć i tapnąć każde z osobna po zbliżeniu, oraz rozbić taki klaster zoomem zamiast utykać na liście.
- **Mapa zawsze otwiera się dopasowana do wszystkich węzłów** — zamiast wracać do ostatnio zapamiętanej pozycji i przybliżenia.
- **Ostrzejsze kafelki map rastrowych (np. OSM)** — naprawiony błąd powodujący rozmycie przez błędny domyślny rozmiar kafelka.
- **Dostrojone klastrowanie węzłów na mapie** — małe grupki węzłów nie zlewają się już w jeden bąbel z liczbą; grupowanie zaczyna się dopiero przy realnie gęstym skupisku.
- **Naprawiona migająca liczba w bąblu klastra** — liczba zgrupowanych węzłów potrafiła pojawić się na chwilę i zniknąć, wracając dopiero przy kolejnym przeliczeniu etykiet (sama bąbelkowa otoczka zawsze zostawała widoczna) — to efekt domyślnej kolizyjnej obsługi etykiet w MapLibre; liczba jest teraz zawsze widoczna niezależnie od kolizji z innymi plakietkami.
- **Domyślna mapa bazowa zmieniona na OpenStreetMap** — zamiast wektorowego stylu MapLibre Liberty, appka startuje teraz z rastrowymi kafelkami OSM.

## Ustawienia desktopowe

- **Naprawiony import/eksport konfiguracji urządzenia** — wcześniej przycisk działał, ale nie tworzył żadnego pliku (błąd w parsowaniu ścieżki na Windowsie, cichy błąd bez informacji dla użytkownika).
- Przywrócony brakujący przełącznik **automatyczne ładowanie obrazków w czacie** (zgubiony przy jednym z merge'y z upstreamem).
- **Trzecia kolumna widoku węzła (metryki/traceroute) na desktopie teraz podąża za wybranym węzłem** — wcześniej po kliknięciu innego węzła na liście trzecia kolumna (np. otwarte metryki urządzenia) zostawała przy poprzednio wybranym węźle; teraz przełącza się na ten sam typ ekranu dla nowo wybranego węzła.
- **Pasek szybkiego przewijania na desktopie** — każda dłuższa lista w appce (węzły, wiadomości, kontakty, ekrany metryk/logów węzła, logi appki, panel debugowania, konfiguracja radia/modułów, główne menu Ustawień, ekran "Zdrowie sieci") ma teraz przeciągany pasek przy prawej krawędzi do szybkiego skoku w dowolne miejsce listy — przydatny na desktopie, gdzie nawigacja jest tylko myszką/kółkiem, bez dotykowego przewijania jak na telefonie. Widoczny wyłącznie na desktopie — na Androidzie/iOS lista działa bez zmian (dotyk/swipe).

## Ekran "Zdrowie sieci"

Nowa, szósta zakładka w dolnej nawigacji (między Węzłami a Mapą), której nie ma w oryginalnej appce:

- **7 kategorii metryk** per węzeł: zasilanie (bateria/napięcie/prąd), sygnał (SNR/RSSI/poziom szumu, z fallbackiem na liczbę przeskoków gdy brak bezpośrednich odczytów), sieć (kanał/eter), środowisko (temperatura/wilgotność/ciśnienie), **zasoby** (CPU/pamięć/flash/PSRAM hosta węzła — wymaga customowego firmware z rozszerzoną telemetrią), ruch (TX/RX/duplikaty/przekazane/uszkodzone) i sąsiedzi.
- **Widżet na pulpit Androida "Local Stats"** — pokazuje na żywo CPU/Flash/PSRAM hosta, z zachowaniem wartości między restartami appki.
- Lista węzłów z sortowaniem, przypinaniem ulubionych na górze, ukrywaniem pustych wpisów i wyszukiwarką; szczegóły każdej metryki jako wykres w oknie 24h/7d/30d.
- **Ekran "Podsumowanie"** — karty z rankingami top-3: najcichsze węzły, najlepszy sygnał, najwięcej wysłanych pozycji, fizycznie najbliższe węzły, najwięcej danych telemetrii, najwięcej wiadomości (tydzień/dziś) i inne — wszystkie poprawnie wykluczają lokalnie podłączone urządzenie z rankingów, żeby nie zaburzało wyników.
- Architektura danych: wszystko dekodowane na żywo z istniejącego logu zdarzeń mesh przy każdym odczycie ekranu — żadne dane nie są duplikowane w osobnej tabeli, więc statystyki są zawsze aktualne i nie zajmują dodatkowego miejsca w bazie.

## Diagnostyka na żądanie (OnDemand)

Osobny ekran dostępny z ekranu szczegółów węzła (Administracja → "Diagnostyka na żądanie"), niezależny od zdalnego sterowania GPIO. Pozwala odpytać dowolny węzeł w zasięgu o bieżące statystyki na żądanie, zamiast czekać na okresowe rozgłoszenia telemetrii.

- **10 typów zapytań**: statystyki węzła (bateria, czas pracy, CPU/heap/flash/PSRAM, liczniki floodu i nexthop, blokady limitem hopów), ping (RSSI/SNR), lista węzłów online, historia błędów routingu, liczniki użycia portów, aktywność eteru, log ostatnich wymian pakietów, historia średniego czasu odbioru, historia liczby odebranych pakietów oraz wersja firmware MT_SW.
- Odpowiedzi przychodzą na dedykowanym porcie protokołu (354) i są dekodowane na żywo z istniejącego logu zdarzeń mesh — ten sam wzorzec danych co ekran "Zdrowie sieci", nic nie jest dodatkowo zapisywane.
- **Wymaga customowego firmware z modułem OnDemand** — protokół zdefiniowany we własnym module `ondemand.proto` dołączonym bezpośrednio do appki (wcześniej żył w osobnym forku protobufów, [MT_SW_PROTOBUFS](https://github.com/MT-SW/MT_SW_PROTOBUFS)); na starszym lub oficjalnym firmware przyciski wysyłają zapytanie, ale węzeł na nie nie odpowiada.
- **Pochodzenie funkcji** — zaadaptowana z historycznego forka firmware Meshtastic (`musznik/firmware`, gałąź `trunk-io/update-trunk`).

## Sniffer

Tryb pokazujący ruch w eterze, który normalnie by zniknął — wszystkie pakiety usłyszane przez węzeł, łącznie z ruchem broadcastowym (wiadomości na kanałach, telemetria), są przekazywane surowo do telefonu zamiast po prostu odrzucane. Appka ma dwa źródła snifera — **radiowy** (LoRa) i **MQTT** (ruch na skonfigurowanym brokerze) — pokazywane na jednym, wspólnym ekranie logu.

- **Jeden wspólny ekran "Sniffer" w Ustawienia → Advanced** zamiast dwóch osobnych — z menu ustawień (ikona koła zębatego) wybiera się aktywne źródło: Wyłączony / Radio / MQTT, tylko jedno naraz. Każde włączenie pyta osobno o potwierdzenie: Radio ostrzega, że może opóźniać lub gubić część wiadomości czatu bądź telemetrii (dzielą tę samą kolejkę transmisji do telefonu); MQTT ostrzega, że otwiera własne połączenie z brokerem i przerwie na czas swojego działania "Proxy MQTT na tym telefonie", bo appka obsługuje tylko jedno aktywne połączenie MQTT naraz.
- **Log zostaje na ekranie po wyłączeniu snifera** — znika dopiero po ręcznym kliknięciu ikony kosza albo przy realnej zmianie aktywnego źródła (Radio↔MQTT), co czyści widok i zaczyna zbierać od nowa.
- **Naprawiony log snifera pokazujący stare wpisy mimo wyłączenia** — log filtrował się tylko wizualnie (znacznik czasu ostatniego wyczyszczenia), ale samo zapytanie do bazy nie miało żadnego ograniczenia czasowego i czytało całą, współdzieloną tabelę logów appki (do 5000 wpisów, nie tylko ze snifera) — stąd wrażenie, że po włączeniu radia pokazują się logi sprzed dnia mimo wyłączonego snifera. Teraz zapytanie jest związane z tym samym znacznikiem co widok (ustawianym automatycznie przy pierwszym włączeniu źródła, jeśli nikt wcześniej nie kliknął kosza), więc log pokazuje wyłącznie to, co sniffer faktycznie zebrał od ostatniego uruchomienia/wyczyszczenia — nic więcej, i mniej pracy przy dekodowaniu przy każdym odświeżeniu.
- **Grupowanie duplikatów** — ten sam pakiet usłyszany więcej niż raz (przez kilka bramek MQTT albo przekazany przez różne węzły pośredniczące w sieci radiowej) pokazuje się jako jeden wiersz z pełną listą źródeł ("Widziane przez bramki: ..." / "Przekazane przez: ..."), zamiast osobnego wpisu dla każdej kopii. Włącznik w ustawieniach snifera.
- **Automatyczne przewijanie i próba deszyfrowania jako osobne przełączniki** w ustawieniach snifera — pierwszy decyduje, czy lista ma skakać do najnowszego pakietu; drugi, czy zaszyfrowana zawartość ma być automatycznie odkodowywana znanymi kluczami kanałów appki, czy pokazywana jako surowy hex.
- **Zapis i wczytywanie logu** — ikona zapisu w pasku górnym eksportuje aktualnie wyświetlany log do pliku (txt/JSON/CSV do wyboru w ustawieniach), a z menu ustawień można wczytać wcześniej zapisany log JSON z powrotem do podglądu (np. do analizy offline albo przesłania komuś innemu).
- **Ekran logu pokazuje na żywo** nadawcę/odbiorcę (z krótką nazwą węzła obok numeru ID, gdy jest znana), kanał, liczbę przeskoków, RSSI/SNR i port pakietu (dla MQTT dodatkowo temat na brokerze). Zawartość pakietu dekoduje się dopiero po kliknięciu w niego — tym samym mechanizmem co Panel Debugowania (trasa traceroute z nazwami węzłów, pozycja, telemetria, NodeInfo itd., a dla nieznanych/zaszyfrowanych danych surowy hex jako fallback). Kliknięty pakiet można skopiować do schowka (z tą samą redakcją danych wrażliwych co Panel Debugowania); znacznik czasu pokazuje sekundy.
- **Pakiety, których węzeł nie potrafił rozszyfrować (bo telefon nie zna klucza danego kanału), są widoczne w logu** — wcześniej takie pakiety były po cichu pomijane jeszcze przed zapisem do bazy, zamiast trafiać na listę z surowym hexem jak reszta nieznanych danych; poprawka dotyczy też ogólnego Panelu Debugowania, bo korzysta z tego samego logu.
- **Log radiowy pokazuje kompletny, nieprzefiltrowany ruch** — łącznie z pakietami, które sam wysyłasz, i odpowiedziami adresowanymi do Twojego węzła, nie tylko podsłuchany ruch obcy i broadcastowy. Dzięki temu pełna para żądanie/odpowiedź — np. traceroute z rozwiązaną trasą — jest widoczna w jednym miejscu.
- **Podsłuchane odpowiedzi traceroute adresowane do innych węzłów nie są już mylnie pokazywane jako wynik własnego zapytania o trasę.**
- **Naprawione podwójne wpisy przy własnym traceroute** — appka filtruje duplikaty po ID pakietu.
- **Naprawiony brak sygnału (SNR) trasy powrotnej w podsłuchanym traceroute** — appka decydowała, czy doliczyć węzły początkowy/końcowy do trasy powrotnej, patrząc tylko na wersję firmware nadawcy (pola `hop_start`/bitfield), których sniffowane pakiety — zwłaszcza z innych węzłów widzianych przez MQTT — często nie mają ustawionych; gdy appka źle to rozpoznała, realne dane SNR trasy powrotnej były po cichu odrzucane i zastępowane brakiem sygnału, mimo że pakiet je zawierał. Appka rozpoznaje to teraz też po samej długości listy SNR, więc prawdziwe dane trasy powrotnej nie znikają.
- **Sniffer radiowy działa wyłącznie lokalnie** — pokazuje tylko to, co fizycznie usłyszy radiem węzeł aktualnie podłączony do telefonu; sniffer MQTT słyszy tyle, ile publikuje broker, niezależnie od tego, który węzeł go zasila.
- **Naprawione migotanie odszyfrowanej treści z powrotem na "Zaszyfrowany"** — przy grupowaniu duplikatów tego samego pakietu appka zawsze wybierała kopię najnowszą wg czasu odbioru, nawet jeśli akurat ta konkretna kopia nie dała się rozszyfrować; wcześniej pokazana treść potrafiła więc na chwilę zniknąć. Teraz preferowana jest najnowsza kopia, która faktycznie się rozszyfrowała.
- **Telemetria zasilania (Power Metrics) i statystyki węzła (Local Stats) widoczne od razu na widoku ogólnym** — te dwa warianty ramki telemetrii wcześniej nie miały żadnego podsumowania na liście i trzeba było kliknąć w pakiet, żeby cokolwiek zobaczyć; teraz widok ogólny pokazuje od razu napięcie/prąd na każdym kanale zasilania oraz uptime, liczbę węzłów online, pakiety TX/RX i zajętość pamięci.
- **Sniffer MQTT działa teraz też na desktopie** — wersja desktopowa miała podstawioną atrapę zamiast prawdziwego połączenia MQTT, więc mimo włączonego MQTT na węźle sniffer nigdy nawet nie próbował się połączyć i wisiał na statusie "Nieaktywny"; desktop korzysta teraz z tej samej implementacji co Android i łączy się bezpośrednio z komputera z brokerem skonfigurowanym w Ustawienia radia → MQTT, niezależnie od telefonu.
- **Wymaga customowego firmware z modułem snifera radiowego** — sterowanie snifera radiowego przeniesiono na protokół OnDemand (port 354): appka po połączeniu pyta węzeł o stan snifera i czeka na odpowiedź; firmware, które nie obsługuje tego zapytania, po prostu nigdy nie odpowiada, a appka wyszarza przełącznik po przekroczeniu limitu czasu, zamiast czekać w nieskończoność. Jeśli mimo to spróbujesz aktywnie włączyć Radio (np. zanim appka zdąży wykryć brak wsparcia), a urządzenie nie odpowie na czas, wybór cofa się na "Wyłączony" i pojawia się komunikat o nieudanym włączeniu, zamiast zostawiać przełącznik w mylącym, pozornie aktywnym stanie. Sniffer MQTT nie wymaga customowego firmware, tylko skonfigurowanego brokera.
- **Pochodzenie funkcji** — sniffer radiowy zaadaptowany z historycznego forka firmware Meshtastic (`musznik/firmware`, gałąź `trunk-io/update-trunk`).

## Komunikator

- **Zdjęcia w czacie przez link** — appka nie wysyła surowych bajtów zdjęcia przez LoRa (za mała przepustowość), tylko uploaduje je anonimowo na zewnętrzny serwer i wysyła sam link jako wiadomość tekstową; odbiorca widzi automatyczny podgląd. Przed wysyłką pojawia się dialog ostrzegający, że serwer hostingu jest publiczny.
- **Zapis zdjęcia z podglądu na cały ekran** — po otwarciu zdjęcia z czatu na cały ekran pojawia się ikona zapisu, która pobiera oryginalny plik i zapisuje go na urządzeniu przez systemowe okno zapisu (tak jak przy eksporcie logów).
- **Podgląd obrazków wklejonych jako link** — sterowany osobnym przełącznikiem w Ustawienia → Prywatność (domyślnie wyłączone), dostępny zarówno na Androidzie, jak i w wersji desktopowej.
- **Desktop: Enter = nowa linijka, Ctrl+Enter = wyślij** — zamiast wymuszonego wysyłania samym Enterem, zachowanie typowe dla komunikatorów na komputerze; na telefonie wysyłanie zostaje osobnym przyciskiem obok pola tekstowego.
- **Domyślne szablony w Szybkim Czacie (Quick Chat)** — appka wcześniej startowała z pustą listą szablonów wiadomości; teraz przy pierwszym uruchomieniu automatycznie wypełnia ją zestawem własnych komend sieciowych (np. `scyzoryk pomoc`, `scyzoryk test`, `scyzoryk range`, `scyzoryk pogoda`, `scyzoryk info`, `scyzoryk aktualnosci`).

## Bezpieczeństwo

- **Wybór koloru węzła przy generowaniu klucza** — na ekranie Zabezpieczenia, obok pola klucza prywatnego, dostępny jest wybór koloru z palety; appka miele losowe klucze X25519 lokalnie na telefonie (kilka równoległych wątków, z suwakiem tolerancji dopasowania) aż trafi kolor węzła zbliżony do wybranego. Ponieważ tolerancja dopuszcza pewien rozrzut, appka pokazuje obok siebie wybrany kolor i faktyczny wynik przed wpisaniem klucza — można go zaakceptować albo szukać dalej; klucz trafia do pola dopiero po potwierdzeniu, bez automatycznego zapisu. Wymaga customowego firmware wyprowadzającego numer węzła z klucza publicznego (od wersji 2.8) — na starszym firmware przycisk potwierdzenia jest wyszarzony i pokazuje komunikat o wymaganej aktualizacji zamiast mielić klucz, który i tak nie dałby oczekiwanego koloru po połączeniu z urządzeniem.

## Branding i personalizacja

- Własny `applicationId`, dzięki czemu appka instaluje się obok oryginalnej appki Meshtastic bez konfliktu (osobne dane, można mieć obie naraz).
- **Pełny rebranding wizualny** — appka i wersja desktopowa mają teraz własną tożsamość: nazwa MT_SW_APP wszędzie w interfejsie i powiadomieniach, autorska ikona (kontur województwa świętokrzyskiego + wordmark "MT_SW", w miejsce wcześniejszego tymczasowego oznaczenia "MS+"), oraz własna paleta kolorów (złoto/granat zamiast domyślnego zielonego brandingu Meshtastic) zastosowana w ikonie połączenia, tarczy podpisanego węzła i czasie ostatniego kontaktu w appce.
- **Ujednolicona kolorystyka wskaźników bezpieczeństwa węzła** — złoty (kolor brandingu appki) dla każdego stanu oznaczającego zaufanie: własne urządzenie, ręcznie zweryfikowany kontakt, podpisany węzeł (firmware 2.8+) i klucz publiczny na starszym firmware; czerwony (ten sam co ikona rozłączonego urządzenia) dla niezgodności klucza; niebieski dla węzła, od którego nie dotarły jeszcze dane (NodeInfo); fioletowy dla świadomego braku klucza publicznego na starszym firmware.
- Osobna ikona w zasobniku systemowym (tray) wersji desktopowej, spójna z resztą brandingu.
- **Ekran "O aplikacji"** wskazuje na repozytorium tego forka (nie oryginalnego projektu) i pokazuje jawny disclaimer "nieoficjalny fork, niezwiązany z Meshtastic LLC" — zgodnie z licencją GPL-3.0 i polityką znaku towarowego Meshtastic LLC.
- **Naprawiony zbyt ostry dźwięk powiadomień Alert (SOS)** — kanał alertów miał własny, dedykowany plik dźwiękowy odtwarzany w trybie alarmowym; appka używa teraz domyślnego dźwięku systemowego, tak jak pozostałe kanały powiadomień.
- **Kolorowa ikona statusu połączenia z urządzeniem** — złoty gdy połączony, biały podczas łączenia/ponownego łączenia, czerwony gdy rozłączony, fioletowy gdy urządzenie śpi; dodatkowo krótki błysk samej ikony i poświaty na zielono przy nadawaniu danych i na niebiesko przy odbiorze.
- Wersja desktopowa przemianowana z "Meshtastic Desktop" na tę samą nazwę co appka mobilna, z uzupełnioną sekcją Prywatności w ustawieniach (wcześniej niedostępną na desktopie mimo że logika już istniała).
- Rozpoznawanie niestandardowej edycji firmware używanej w sieci Świętokrzyskiej — appka pokazuje czytelną nazwę zamiast surowej wartości technicznej.
- Wygenerowany plik tłumaczeń PL uzupełniający ok. 1000 wcześniej brakujących stringów (appka była przetłumaczona na polski w ok. 43%).

## Łączność Bluetooth

- **Naprawiona zawodność ponownego łączenia z już sparowanym urządzeniem** — na części telefonów (potwierdzone na Xiaomi/MIUI) appka potrafiła nie połączyć się ponownie z węzłem po tym, jak ten na chwilę zniknął z zasięgu lub się wyłączył, mimo że urządzenie pozostawało sparowane; jedynym działającym obejściem było ręczne odparowanie i sparowanie od nowa. Naprawione poprzez: odświeżanie cache usług GATT przy każdym połączeniu (a nie tylko reaktywnie, po wykryciu problemu), samodzielną negocjację MTU z automatycznym ponowieniem próby zamiast pojedynczej próby wystrzelonej natychmiast po odkryciu usług, oraz dodatkowe mechanizmy wykrywania i odzyskiwania połączenia działające również wtedy, gdy ręczne przerwanie i ponowienie łączenia zerowałoby licznik nieudanych prób.
- **Naprawione niewykrywanie węzła po Bluetooth na desktopie (Windows)** — appka skanowała bez końca, nie znajdując żadnego urządzenia, mimo że telefon widział ten sam węzeł bez problemu. Przyczyna: aktualizacja biblioteki Kable (0.44.3 → 0.45.0) przyniosła nowszą wersję Rustowego `btleplug`, która na Windowsie/WinRT bezwarunkowo włącza `SetAllowExtendedAdvertisements`, co u części adapterów BT czyni je całkowicie niewidocznymi dla skanowania (potwierdzony błąd w upstreamie btleplug). Naprawione przez przypięcie Kable z powrotem do 0.44.3.

## Podziękowania

- Za korektę tłumaczeń oraz część pomysłów na nowe funkcje odpowiada [cheaterenator](https://github.com/cheaterenator).

## Status i zastrzeżenia

- To osobisty, roboczy fork — część zmian jest zweryfikowana buildem i przetestowana na urządzeniu, część czeka na potwierdzenie w terenie.
- Brak oficjalnych release'ów/tagów — zmiany trzymane na bieżąco na gałęzi `main`.
- Fork korzysta z tej samej licencji GPL-3.0 co projekt macierzysty.

---
*Bazuje na [meshtastic/Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android). Nieoficjalny, niezwiązany z Meshtastic LLC.*



____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________



# MT_SW_APP — personal Meshtastic-Android fork

A fork of the official [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android) app, developed for the **Świętokrzyskie** mesh radio network (mt-sw.pl). Builds on the upstream KMP/Compose Multiplatform architecture (`fdroid`/`google` flavors, desktop module) and adds a set of local features, fixes, and customizations not found in the upstream version.

Work in progress — this repo is mainly for personal use and testing with a small group of people; it doesn't necessarily build cleanly at all times.

## Node and mesh network management

- **Remote GPIO control** — on the node detail screen (Remote Hardware module) you can enter a pin number; the app computes the bitmask itself and sends `WRITE_GPIOS`/`READ_GPIOS` to the remote node. Buttons are only enabled once PKC keys have been exchanged with that node.
- **Remote favorite/ignore over the LoRa mesh** (not just locally) — with real delivery confirmation based on mesh routing ACKs, instead of only a local, phone-side change.
- **Manual contact add by node ID** — both locally and remotely, with a unified `!a1b2c3d4` (hex) format used everywhere in the app.
- **Passive NeighborInfo collection** — the neighbor log now also shows overheard broadcasts from other nodes, not just responses to your own requests; also fixes requesting Neighbor Info for your own, locally connected device (previously returned nothing).
- **Quick command buttons** (`/ping`, `/hello`, `/test`) on the node screen — send a private message even to nodes whose role normally hides the manual message option; useful for quickly testing new firmware builds on devices in the field.
- **Full list of relay nodes for message delivery** — the "Delivery status" dialog shows the full list of node names involved in relaying a message (not just a count or a single name like upstream), including firmware-side fixes so that data actually reaches the app.
- **Relay node name shown directly in the node list and node detail** — no need to open the "Delivery status" dialog anymore to see which node a given node relays through.
- Restored the **Traffic Management** configuration screen (removed from upstream at one point).
- **Fixed a stale last-position timestamp for nodes with a manually configured fixed position** — firmware doesn't refresh the timestamp when retransmitting the same position, so the app now shows the node's actual last-heard time instead of a frozen date from days ago.
- **Hid outdated device roles** (REPEATER, ROUTER_CLIENT) from the role picker in device configuration — firmware no longer supports them, so they can't be selected by mistake anymore.
- **Localized device role names** — the app used to show the raw, English technical role name (e.g. `ROUTER_LATE`) everywhere: settings, node list, node detail, and telemetry; every role now has a proper localized name (e.g. "Router pomocniczy" in Polish), and the role descriptions reference them the same way instead of the raw English name.
- **New "Roles" section in the node list help sheet** — a full reference of every device role (icon, name, meaning) available directly from the node list.
- **New "Security" and "Connection" sections in the node list help sheet** — a legend of every node security indicator state (colors, icons, meaning) and every device connection status and blink color (transmit/receive), available directly from the node list.
- Cleaned up the uptime display in the node list — added an icon to visually separate it from the last-heard time.
- **Automatic node-database cleanup** — the "Clean Node Database" screen now has a toggle for automatically removing inactive nodes, with sliders for the inactivity threshold (1–90 days) and how often the check runs (1–30 days). Works on both Android (WorkManager) and desktop (a lightweight hourly check loop with the last run persisted so timing survives app restarts); off by default, and favorited/ignored nodes are never auto-deleted.
- **Fixed the node list search field losing/reordering characters while typing** — when the list was scrolled down, typing in the search field while the list recalculated its scroll position could scramble the order of the characters you typed; the field now keeps its own immediate local typing state, independent of the list's scroll recalculation.

## Map

The map engine (Android and desktop) now uses the shared, natively rendered **MapLibre** library — adopted from upstream in place of the earlier, hand-written renderer (osmdroid on Android, a custom OSM tile renderer on Compose Canvas on desktop). Reasons for the switch: faster native (GPU) rendering, and no longer having to maintain a separate map engine on every sync with upstream.

What this means in practice:

- **Desktop gets a map "for free" now** — the desktop "Map" tab used to be an empty placeholder, then a from-scratch custom renderer; it now uses the same shared component as Android, so features like GeoJSON/KML layer import, offline tile downloads, and map filters come from the shared module rather than a desktop-only implementation.
- **Site Planner** (coverage simulation) on desktop now runs through the browser instead of the earlier integration with an embedded Chromium browser (JCEF) built specifically for this fork.
- The node-detail mini-map, the position-track map, and the main map screen all use the same shared component on both platforms.
- Clicking a node on the desktop map still opens the node list first, then details — that behavior survived the switch to the new map engine.
- **Terrain and weather overlays enabled by default** — the hillshade overlay (useful for judging LoRa range limited by terrain) and the NOAA weather radar overlay are now checked as soon as the map opens, instead of requiring a manual toggle in the layers menu.
- **Smaller node chips with a tail pointing at the exact GPS position** — the chip badge is now more compact, with a small rounded tail underneath pointing at the node's precise coordinate instead of being centered over it.
- **Nodes reporting an identical GPS position are nudged slightly apart** — several devices reporting an identical (e.g. fixed) position are now spread a few metres apart in real space: invisible when zoomed out, but letting each one be seen and tapped individually once zoomed in, and letting such a cluster break apart by zooming instead of getting stuck.
- **Map always opens framed on all nodes** — instead of reopening at the last remembered position and zoom.
- **Sharper raster basemap tiles (e.g. OSM)** — fixed a bug causing blur from an incorrect default tile size.
- **Tuned node clustering on the map** — small groups of nodes no longer collapse into a single numbered bubble; clustering now only kicks in for a genuinely dense cluster.
- **Fixed a flickering count inside the cluster bubble** — the number of grouped nodes could show for a moment then vanish, only coming back on the next label-placement pass (the bubble itself always stayed visible throughout) — caused by MapLibre's default label collision handling; the count is now always shown regardless of collisions with other chips.
- **Default basemap changed to OpenStreetMap** — instead of MapLibre's vector Liberty style, the app now starts with raster OSM tiles by default.

## Desktop settings

- **Fixed device configuration import/export** — the button worked but silently failed to create any file (a URI-parsing bug on Windows, with no error shown to the user).
- Restored the missing **auto-load chat images** toggle (lost during an upstream merge).
- **The node detail third column (metrics/traceroute) on desktop now follows the selected node** — previously, clicking a different node in the list left the third column (e.g. an open Device Metrics view) showing the previously selected node; it now switches to the same screen type for the newly selected node.
- **Fast-scroll sidebar on desktop** — every long list in the app (nodes, messages, contacts, node metrics/log screens, app logs, the debug panel, radio/module config screens, the main Settings menu, the "Network Health" screen) now has a draggable thumb along the right edge to jump straight to any point in the list — useful on desktop, where navigation is mouse/wheel-only, unlike a phone's touch scrolling. Desktop-only — Android/iOS lists behave exactly as before (touch/swipe).

## "Network Health" screen

A new, sixth tab in the bottom navigation (between Nodes and Map) that doesn't exist in the original app:

- **7 metric categories** per node: power (battery/voltage/current), signal (SNR/RSSI/noise floor, with a hop-count fallback when there are no direct readings), network (channel/air utilization), environment (temperature/humidity/pressure), **resources** (node host CPU/memory/flash/PSRAM — requires custom firmware with extended telemetry), traffic (TX/RX/duplicates/relayed/corrupted), and neighbors.
- **Android home-screen "Local Stats" widget** — shows live host CPU/Flash/PSRAM, persisted across app restarts.
- Node list with sorting, pinning favorites to the top, hiding empty entries, and search; each metric's detail view is a chart over a 24h/7d/30d window.
- **"Summary" screen** — cards with top-3 rankings: quietest nodes, best signal, most positions sent, physically closest nodes, most telemetry data, most messages (week/today), and more — all correctly exclude the locally connected device from the rankings so it doesn't skew results.
- Data architecture: everything is decoded live from the existing mesh event log every time the screen is read — nothing is duplicated into a separate table, so the stats are always current and take no extra database space.

## On-demand diagnostics (OnDemand)

A dedicated screen reachable from the node detail screen (Administration → "On-Demand Diagnostics"), separate from remote GPIO control. Lets you query any node in range for current stats on demand, instead of waiting for periodic telemetry broadcasts.

- **10 query types**: node stats (battery, uptime, CPU/heap/flash/PSRAM, flood and nexthop counters, hop-limit blocks), ping (RSSI/SNR), nodes online, routing error history, port usage counters, air activity, recent packet exchange log, average RX time history, RX packet count history, and the MT_SW firmware version.
- Responses arrive on a dedicated protocol port (354) and are decoded live from the existing mesh event log — the same data pattern as the "Network Health" screen, nothing is persisted separately.
- **Requires custom firmware with the OnDemand module** — the protocol is defined in a small `ondemand.proto` Wire module bundled directly with the app (it used to live in a separate protobufs fork, [MT_SW_PROTOBUFS](https://github.com/MT-SW/MT_SW_PROTOBUFS)); on older or stock firmware the buttons send the request, but the node never responds.
- **Feature origin** — adapted from a historical Meshtastic firmware fork (`musznik/firmware`, `trunk-io/update-trunk` branch).

## Sniffer

A mode that surfaces air traffic that would normally just vanish — every packet the node overhears, including broadcast traffic (channel messages, telemetry), is forwarded raw to the phone instead of being dropped. The app has two sniffer sources — **Radio** (LoRa) and **MQTT** (traffic on the configured broker) — shown on one shared log screen.

- **One shared "Sniffer" screen in Settings → Advanced** instead of two separate ones — the settings menu (gear icon) picks the active source: Off / Radio / MQTT, only one at a time. Enabling either one prompts for confirmation separately: Radio warns it can delay or drop some chat messages or telemetry (sniffed traffic shares the same queue to the phone); MQTT warns that it opens its own connection to the broker and will interrupt "MQTT proxy on this phone" for as long as it runs, since the app only supports one active MQTT connection at a time.
- **The log stays on screen after the sniffer is turned off** — it only clears when you tap the trash icon, or when the active source actually changes (Radio↔MQTT), which resets the view and starts collecting fresh.
- **Fixed the sniffer log showing stale entries even when off** — the log was only filtered visually (a last-cleared watermark); the underlying database query itself had no time bound and read the entire, app-wide log table (up to 5000 entries, not sniffer-specific) — hence logs from the previous day showing up right after turning the radio on, despite the sniffer being off. The query is now tied to the same watermark as the view (auto-set the first time a source is ever turned on, if the trash icon was never clicked before), so the log shows only what the sniffer has actually collected since the last activation/clear — nothing else, and less decode work on every refresh.
- **Duplicate grouping** — the same packet heard more than once (via several MQTT gateways, or relayed through different nodes on the radio mesh) shows as a single row with the full list of sources ("Seen via gateways: ..." / "Relayed via: ...") instead of a separate entry per copy. Toggle in the sniffer settings.
- **Auto-scroll and attempt-decryption as separate toggles** in the sniffer settings — the first decides whether the list jumps to the newest packet; the second whether encrypted content is automatically decoded with this app's known channel keys, or shown as raw hex.
- **Save and load a log** — the save icon in the top bar exports the currently displayed log to a file (txt/JSON/CSV, chosen in settings), and the settings menu can load a previously saved JSON log back in for viewing (e.g. for offline analysis or sharing with someone else).
- **The log screen shows live** sender/receiver (with a short node name next to the ID when known), channel, hop count, RSSI/SNR, and the packet's port (for MQTT, also the broker topic). The packet's content only decodes once you tap on it — using the same decoder as the Debug Panel (traceroute path with node names, position, telemetry, NodeInfo, etc., falling back to raw hex for unknown or still-encrypted data). A tapped packet can be copied to the clipboard (with the same sensitive-data redaction as the Debug Panel); the timestamp shows seconds.
- **Packets the node couldn't decrypt (because the phone doesn't know that channel's key) are visible in the log** — previously such packets were silently dropped before being persisted, instead of showing up in the list with a raw-hex fallback like other unknown data; the fix also applies to the general Debug Panel, since it reads from the same log.
- **The radio log shows the complete, unfiltered traffic** — including packets you send yourself and responses addressed to your node, not just overheard foreign and broadcast traffic. This means a full request/response pair — e.g. a traceroute with its resolved route — is visible together in one place.
- **Sniffed traceroute responses addressed to other nodes no longer show up as if they were the result of your own traceroute request.**
- **Fixed duplicate entries for your own traceroute** — the app filters duplicates by packet ID.
- **Fixed missing return-path signal (SNR) on sniffed traceroutes** — the app decided whether to add the start/end nodes to the return path by looking only at the sender's firmware version (the `hop_start`/bitfield fields), which sniffed packets — especially from other nodes seen via MQTT — often don't have set; when the app got this wrong, real return-path SNR data was silently discarded and shown as no signal, even though the packet actually contained it. The app now also recognizes this from the SNR list's own length, so genuine return-path data no longer disappears.
- **The Radio sniffer is local only** — it surfaces only what the node currently connected to the phone physically hears over the radio; the MQTT sniffer hears whatever the broker publishes, regardless of which node feeds it.
- **Fixed decoded content flickering back to "Encrypted"** — when grouping duplicate copies of the same packet, the app always picked the newest copy by receive time, even if that particular copy happened to fail decryption; previously shown content could briefly disappear as a result. It now prefers the newest copy that actually decrypted successfully.
- **Power Metrics and Local Stats telemetry now show up right on the general view** — these two telemetry frame variants previously had no summary at all in the list, requiring a tap to see anything; the general view now shows voltage/current per power channel, plus uptime, online node count, TX/RX packet counts, and heap usage.
- **The MQTT sniffer now also works on desktop** — the desktop build had a stand-in stub instead of a real MQTT connection, so even with MQTT enabled on the node, the sniffer never even attempted to connect and stayed stuck on "Inactive"; desktop now uses the same implementation as Android and connects directly from the computer to the broker configured under Radio Configuration → MQTT, independent of the phone.
- **Requires custom firmware for the radio sniffer module** — control for the radio sniffer runs over the OnDemand protocol (port 354): on connect, the app asks the node for the sniffer's current state and waits for a reply; firmware that doesn't support the request simply never answers, and the app grays out the toggle once the timeout passes instead of waiting forever. If you actively try to enable Radio anyway (e.g. before the app has had a chance to detect the lack of support) and the device doesn't respond in time, the selection reverts to Off with a message about the failed enable, instead of being left looking selected while nothing is actually happening. The MQTT sniffer needs no custom firmware, just a configured broker.
- **Feature origin** — the radio sniffer is adapted from a historical Meshtastic firmware fork (`musznik/firmware`, `trunk-io/update-trunk` branch).

## Messaging

- **Photos in chat via link** — the app doesn't send raw photo bytes over LoRa (not enough bandwidth); instead it anonymously uploads the photo to an external server and sends just the link as a text message, with the recipient seeing an automatic preview. A confirmation dialog appears before sending, warning that the hosting server is public.
- **Save a photo from the full-screen viewer** — opening a chat photo full-screen now shows a save icon that downloads the original file and saves it to the device through the system's own save-file dialog (the same as log export).
- **Preview for images pasted as links** — controlled by a separate toggle in Settings → Privacy (off by default), available on both Android and the desktop version.
- **Desktop: Enter = new line, Ctrl+Enter = send** — instead of forcing a send on plain Enter, matching the behavior people expect from desktop chat apps; on the phone, sending stays a separate button next to the text field.
- **Default Quick Chat templates** — the app's Quick Chat template list used to start out empty; now on first launch it's automatically seeded with a set of custom network commands (e.g. `scyzoryk pomoc`, `scyzoryk test`, `scyzoryk range`, `scyzoryk pogoda`, `scyzoryk info`, `scyzoryk aktualnosci`).

## Security

- **Node color picker when generating a key** — on the Security screen, next to the private key field, there's a color picker; the app grinds random X25519 keys locally on the phone (a few parallel threads, with a tolerance slider) until it finds one whose resulting node color is close enough to the chosen one. Since the tolerance allows some spread, the app shows the chosen color next to the actual result before the key is applied — you can accept it or search again; the key only goes into the field once confirmed, with no auto-save. Requires custom firmware that derives the node number from the public key (firmware 2.8+) — on older firmware the confirm button is grayed out and shows a message about the required update instead of grinding a key that wouldn't produce the expected color once connected to the device anyway.

## Branding and customization
- **Color-coded device connection status icon** — gold when connected, white while connecting/reconnecting, red when disconnected, purple when the device is asleep; plus a brief flash of the icon itself and a matching glow, green when transmitting and blue when receiving data.
- A custom `applicationId`, so the app installs side by side with the original Meshtastic app without conflicting (separate data, both can be installed at once).
- **Full visual rebrand** — the app and desktop build now have their own identity: the name MT_SW_APP everywhere in the UI and notifications, a custom icon (Świętokrzyskie voivodeship outline + "MT_SW" wordmark, replacing the earlier placeholder "MS+" mark), and a custom color palette (gold/navy instead of Meshtastic's default green branding) applied to the connection icon, the signed-node shield, and the last-heard time indicator throughout the app.
- **Unified node security indicator colors** — gold (the app's brand color) for every state that signals trust: the local device itself, a manually verified contact, a signed node (firmware 2.8+), and a public key on file on older firmware; red (matching the disconnected-device color) for a key mismatch; blue for a node whose info (NodeInfo) hasn't arrived yet; purple for a node on older firmware with no public key on file at all.
- A dedicated system-tray icon for the desktop build, matching the rest of the branding.
- The **"About" screen** now points to this fork's own repository (not the upstream project) and shows an explicit "unofficial fork, not affiliated with Meshtastic LLC" disclaimer — in line with the GPL-3.0 license and Meshtastic LLC's trademark policy.
- **Fixed an overly harsh Alert (SOS) notification sound** — the alert channel had its own dedicated sound file played in alarm mode; the app now uses the default system notification sound, matching every other notification channel.
- **Color-coded device connection status icon** — gold when connected, white while connecting/reconnecting, red when disconnected, yellow when the device is asleep; plus a brief flash of the icon itself and a matching glow, green when transmitting and blue when receiving data.
- The desktop build renamed from "Meshtastic Desktop" to match the mobile app's name, with a Privacy section added to its settings (previously missing on desktop even though the underlying logic already existed).
- Detection of the custom firmware edition used on the Świętokrzyskie network — the app shows a readable name instead of the raw technical value.
- A generated PL translation file filling in roughly 1,000 previously untranslated strings (the app was only about 43% translated into Polish).

## Bluetooth connectivity

- **Fixed unreliable reconnection to an already-paired device** — on some phones (confirmed on Xiaomi/MIUI) the app could fail to reconnect to a node after it briefly went out of range or powered off, even though the device remained paired; the only working workaround was manually unpairing and re-pairing. Fixed by: refreshing the GATT service cache on every connect (not just reactively after a detected problem), negotiating MTU explicitly with an automatic retry instead of a single attempt fired immediately after service discovery, and additional detection/recovery mechanisms that keep working even when a manual stop-and-retry would otherwise reset the failure counter.
- **Fixed BLE device discovery not working on desktop (Windows)** — the app would scan indefinitely without ever finding a device, even though the phone saw the same node fine. Root cause: a Kable library bump (0.44.3 → 0.45.0) pulled in a newer version of the Rust `btleplug` backend that unconditionally enables `SetAllowExtendedAdvertisements` on Windows/WinRT, which makes some BT adapters completely invisible to scanning (a confirmed upstream btleplug bug). Fixed by pinning Kable back to 0.44.3.

## Credits

- Translation corrections and some new feature ideas courtesy of [cheaterenator](https://github.com/cheaterenator).

## Status and caveats

- This is a personal, work-in-progress fork — some changes are build-verified and tested on-device, others are still awaiting confirmation in the field.
- No official releases/tags — changes are kept up to date directly on the `main` branch.
- The fork uses the same GPL-3.0 license as the upstream project.

---
*Based on [meshtastic/Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android). Unofficial, not affiliated with Meshtastic LLC.*