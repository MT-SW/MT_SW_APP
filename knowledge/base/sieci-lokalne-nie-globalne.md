# Dlaczego sieci LoRa (Meshtastic, MeshCore) są lokalne, a nie globalne

<!-- Treść opracowana własnymi słowami na podstawie artykułu użytkownika bikeman (SQ6B, Wrocław) z forum "Projekty LoRa MeshCore oraz Meshtastic dla każdego" (28.01.2026), bez komentarzy pod nim. To analiza i opinia autora, nie dokumentacja producenta. -->

### Czy da się zbudować jedną wielką sieć mesh LoRa na cały kraj?
Autor artykułu uważa, że nie. Postawić węzeł jest łatwo, ale żeby sieć naprawdę działała, trzeba rozumieć, jak działa radio i sieć, a nie wierzyć w obietnice "mesh od morza po góry", które wzięły się z naiwnego porównywania do Internetu. Jego zdaniem przy dużym obszarze kłopoty są nieuniknione, a kolejne sztuczki zwiększające przepustowość tylko odsuwają w czasie moment, gdy sieć się załamie. Przyczyny leżą w fizyce i konstrukcji technologii, a nie w tym, że oprogramowanie jest za mało dopracowane.
Słowa: sieć globalna, od morza po góry, duża sieć mesh, lokalna sieć, skalowalność, zapaść sieci, wielkoobszarowa, cała Polska
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (artykuł bikeman, sprawdzono 2026-10; opinia autora, opracowanie własnymi słowami)

### Czym sieć mesh LoRa różni się od Internetu w warstwach sieci?
Internet ma pełny zestaw warstw: adresowanie i trasowanie między ogromną liczbą podsieci (IP), mechanizmy panujące nad ruchem i retransmisjami (TCP), a nad nimi usługi. Mesh LoRa ma dobrze zrobione tylko dwie dolne warstwy, czyli radio i dostęp do łącza. Warstwa sieciowa jest tak uboga, że nie pełni roli IP: nie ma hierarchii adresów, nie ma mądrego wybierania tras. Nie ma też odpowiednika warstwy transportowej, czyli portów i kontroli przeciążenia; zostaje proste potwierdzenie albo nic. Usług w rodzaju WWW czy poczty też nie ma, tylko krótkie komunikaty i telemetria.
Słowa: model OSI, warstwy, IP, TCP, kontrola przeciążenia, routing, brak warstwy transportowej, porty, ACK, internet a mesh
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora, opracowanie własnymi słowami)

### Jak bardzo LoRa jest wolniejsza od innych łączy?
Światłowód osiąga terabity na sekundę, Ethernet setki gigabitów, Wi-Fi setki megabitów, LTE i 5G dziesiątki do setek megabitów. Mesh LoRa daje od 0,3 do ok. 50 kilobitów na sekundę. Do tego brakuje skalowalnego trasowania: bez hierarchii każdy w praktyce musi radzić sobie ze wszystkimi innymi, a najczęściej zamiast tras stosuje się zalewanie pakietami. Takie podejście znosi dziesiątki lub setki węzłów, ale nie tysiące.
Słowa: prędkość, kb/s, przepustowość LoRa, porównanie z wifi, 0.3 kb/s, 50 kb/s, setki węzłów, tysiące węzłów
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora, opracowanie własnymi słowami)

### Jakie są trzy główne bariery dużej sieci LoRa?
Pierwsza to przepisy: pasma ISM mają ograniczenie czasu nadawania, więc nie wolno nadawać bez przerwy. Druga to przepustowość: w sieci globalnej równolegle płyną miliony strumieni, a w mesh LoRa jedno nadawanie zajmuje eter w całej okolicy, więc inni muszą czekać. Trzecia to trasowanie: w Internecie dane idą wybranymi drogami, a w mesh zalewanie rozsyła je wszędzie; ruch kierowany po konkretnej trasie jest znikomy w porównaniu z zalewającym. Oprogramowanie może te ograniczenia łagodzić, ale ich nie usunie.
Słowa: bariery, duty cycle, przepustowość, flooding, prawo radiowe, granica sieci, ograniczenia fundamentalne
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora, opracowanie własnymi słowami)

### Czym jest burza rozgłoszeniowa i dlaczego psuje dużą sieć?
Sieć mesh nie wie, gdzie jest odbiorca, więc pierwszy pakiet po prostu wysyła do wszystkich. Przykład autora: wiadomość prywatna z Gdańska do Zgorzelca. Węzeł podaje pakiet najbliższemu przekaźnikowi, ten powtarza go we wszystkie strony, następne robią to samo i fala (przy limicie 64 przeskoków, jaki założył autor) obejmuje cały kraj. Każdy przekaźnik, który ją odbiera, na chwilę blokuje eter dla innych. Nawet jeśli adresat jest dwa przeskoki dalej i trasa znajdzie się szybko, burza i tak rozlewa się szeroko. Odpowiednikiem TTL z Internetu jest tu limit przeskoków. Uwaga: w Meshtastic maksimum to 7 (domyślnie 3), więc 64 dotyczy założeń autora lub innego protokołu.
Słowa: burza rozgłoszeniowa, broadcast storm, zbieżność, 64 hop, TTL, limit przeskoków, flood, zator
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora, opracowanie własnymi słowami; liczba 64 niezgodna z limitem 7 w Meshtastic)

### Dlaczego w sieci jest tłok i giną wiadomości?
Węzeł nie rozróżnia osobnych rozmów, nie ma wielu torów odbioru i prawie nie buforuje ruchu. W zasięgu bezpośrednim słyszy jednego nadawcę naraz. Gdy dwie stacje nadadzą w tej samej chwili, sygnały się nakładają i oba pakiety przepadają. Przy kilkudziesięciu węzłach chcących nadawać jednocześnie rośnie liczba kolizji, więc rośnie liczba powtórek, a te dodatkowo zapychają kanał. W końcu sieć prawie przestaje przekazywać dane (załamanie z przeciążenia). Moment krytyczny nadchodzi prędzej czy później, jeśli sieć rozbudowuje się bez umiaru.
Słowa: kolizje, tłok, zatkanie, congestion collapse, giną pakiety, nakładanie, retransmisje, gubienie wiadomości
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora, opracowanie własnymi słowami)

### Co to są "pokemony" na liście węzłów i dlaczego szkodzą?
To węzły, które pojawiają się na liście tylko dlatego, że ich ogłoszenia przypłynęły z daleka falą zalewania, zwłaszcza w czasie dobrej propagacji, gdy sygnały niosą się na setki kilometrów. Nie da się z nimi porozmawiać. Podobnie bywa z wiadomościami przychodzącymi z dalekich miejsc bez żadnego kontekstu, na które trudno odpowiedzieć. Według autora nie dają niczego poza ciekawostką, a płacimy za nie lokalnym ruchem: zapchany eter sprawia, że przestają dochodzić nawet zwykłe wiadomości z odległości jednego czy dwóch przeskoków.
Słowa: pokemony, pokemon, głuchy telefon, nody z daleka, propagacja, lista węzłów, duch, widmo, daleki węzeł
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora, opracowanie własnymi słowami)

### Jak według autora radzić sobie z rozmiarem sieci lokalnej?
Wybór odpowiedniego presetu zwiększa pojemność i szybkość sieci na danym terenie, ale nie wydłuża zasięgu przy tej samej gęstości węzłów, i zawsze istnieje granica, której nie da się przekroczyć. Zmiana presetu pomaga tylko na jakiś czas: rośnie pojemność lokalna i prędkość, a maleje odległość łączności bezpośredniej. Sensownym narzędziem jest ograniczenie liczby przeskoków: odcina ruch z odległych węzłów, a nie przeszkadza w łączności na własnym obszarze. Autor podsuwa pomysł jednego "polskiego presetu" w całym kraju z hop limitem dopasowanym tak, żeby osoba w podróży zawsze trafiała na sieć lokalną bez zmiany ustawień.
Słowa: hop limit, preset, polski preset, obszar lokalny, ograniczenie przeskoków, pojemność sieci, rozwiązanie, lokalna sieć
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora, opracowanie własnymi słowami)

### Co pokazują rysunki w tym artykule?
Mapa Polski z czterema przekaźnikami w rogach kraju i przekreślonymi strzałkami między nimi: bezpośrednia łączność między dalekimi końcami nie działa. Tabela stosu Internetu: aplikacja, prezentacja, sesja, transport (TCP/UDP: błędy, retransmisje, porty, przepływ), sieć (adresowanie i trasowanie), łącze danych (Ethernet, Wi-Fi), warstwa fizyczna. Tabela mesh LoRa: warstwy aplikacyjne praktycznie nie istnieją, brak TCP, warstwa sieciowa uproszczona (flooding lub proste tablice sąsiadów, bez IP i hierarchii), łącze z identyfikatorami węzłów, radio LoRa o bardzo małej, sztywno ograniczonej przepustowości. Zrzuty mapy z aplikacji pokazują setki węzłów rozsianych po Polsce i sąsiednich krajach, z którymi w praktyce nie da się rozmawiać.
Słowa: rysunek, tabela OSI, stos, mapa Polski, krzyżyki, mapa węzłów, zrzut ekranu, ilustracje, pokemony na mapie
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opis rysunków, opracowanie własnymi słowami)
