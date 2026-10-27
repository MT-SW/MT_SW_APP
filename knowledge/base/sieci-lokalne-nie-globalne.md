# Dlaczego sieci LoRa (Meshtastic, MeshCore) są lokalne, a nie globalne

<!-- Artykuł: bikeman (SQ6B, Wrocław), forum "Projekty LoRa MeshCore oraz Meshtastic dla każdego", 28.01.2026. Tylko artykuł główny, bez komentarzy. Dodany na prośbę użytkownika projektu MT_SW (warto potwierdzić zgodę autora, bikeman/SQ6B). To opinia i analiza autora, nie dokumentacja producenta. -->

### Dlaczego mesh LoRa nie nadaje się na wielkie obszary (od morza po góry)?
Autor artykułu (bikeman, SQ6B) twierdzi, że zbudować sprzęt jest łatwo, ale zbudować sprawnie działającą sieć wymaga minimum wiedzy o jej działaniu; ostrzega przed wiarą w modele łączności "mesh od morza po góry", opartych na złudnych podobieństwach do Internetu. Jego zdaniem w kontekście wielkoobszarowym mesh nie ma szansy działać poprawnie, a próby zwiększania przepustowości bez zrozumienia podstaw przesuwają tylko w czasie moment degradacji i zapaści sieci. Powody są techniczne, nie programowe.
Słowa: sieć globalna, od morza po góry, duża sieć mesh, lokalna sieć, skalowalność, zapaść sieci, wielkoobszarowa, bikeman
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (artykuł z forum, sprawdzono 2026-10; opinia autora)

### Czego brakuje sieci mesh w porównaniu z Internetem (model OSI)?
Internet ma pełny stos: warstwa 3 (IP) to globalne adresowanie i hierarchiczny routing między milionami podsieci, warstwa 4 kontroluje przeciążenie i gubienie danych, warstwy 5–7 to usługi. W sieci mesh pełne są tylko warstwy 1 i 2, a warstwa 3 jest tak prymitywna, że nie spełnia funkcji IP. Brak warstwy 4 oznacza brak portów i brak kontroli przeciążenia; brak inteligentnego wyboru trasy; brak usług znanych z Internetu. Mesh ma proste ACK albo nic, a retransmisje przy dużym ruchu pogłębiają zator.
Słowa: model OSI, warstwy, IP, TCP, kontrola przeciążenia, routing, brak warstwy transportowej, porty, ACK, internet a mesh
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora)

### Jak duże różnice w szybkości są między LoRa a innymi łączami?
Wg artykułu: światłowód terabity na sekundę, Ethernet setki Gb/s, Wi-Fi setki Mb/s, LTE/5G dziesiątki–setki Mb/s, LoRa mesh tylko 0,3–50 kb/s. Brak skalowalnego routingu (brak hierarchii adresów, często flooding zamiast routingu) działa dla dziesiątek lub setek węzłów, nie dla tysięcy.
Słowa: prędkość, kb/s, przepustowość LoRa, porównanie z wifi, 0.3 kb/s, 50 kb/s, setki węzłów, tysiące węzłów
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora)

### Jakie są trzy twarde bariery wielkiej sieci LoRa?
Bariera 1, prawo radiowe: pasma ISM mają limit czasu nadawania (duty cycle), nie wolno nadawać bez przerwy. Bariera 2, przepustowość: globalna sieć wymaga równoległych milionów strumieni, a w mesh LoRa jedna transmisja blokuje eter w okolicy dla wszystkich. Bariera 3, routing: w Internecie ruch idzie wybranymi trasami, w mesh flooding wysyła go wszędzie, a ruch trasowany jest pomijalny wobec ruchu zalewającego. Oprogramowanie może tylko zmniejszyć dokuczliwość ograniczeń, nigdy ich nie wyeliminuje.
Słowa: bariery, duty cycle, przepustowość, flooding, prawo radiowe, granica sieci, ograniczenia fundamentalne
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora)

### Co to jest burza rozgłoszeniowa (broadcast storm) i dlaczego jest największym problemem?
Mesh LoRa działa zasadą "nie wiem, gdzie jest odbiorca, więc wyślę do wszystkich". Przykład autora: wiadomość prywatna z Gdańska do Zgorzelca; węzeł wysyła pakiet do najbliższego przekaźnika, ten rozsyła dalej we wszystkich kierunkach, kolejne robią to samo; przy limicie 64 przeskoków (tak podaje autor) fala obejmuje całą Polskę, a każde przyjęcie danych blokuje przekaźnik dla innych transmisji. Nawet jeśli odbiorca stoi 1–2 przeskoki dalej i trasa szybko się znajdzie, burza i tak rozchodzi się szeroko. Ratunkiem jest odpowiednik TTL: limit przeskoków. Uwaga: w Meshtastic maksymalny hop limit to 7 (domyślnie 3); 64 dotyczy innego protokołu lub założenia autora.
Słowa: burza rozgłoszeniowa, broadcast storm, zbieżność, 64 hop, TTL, limit przeskoków, flood, zator
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora; liczba 64 niezgodna z limitem 7 w Meshtastic)

### Dlaczego w sieci mesh jest tłok i giną pakiety (zatkanie)?
Węzeł nie ma portów ani wielu niezależnych kanałów odbioru, nie buforuje dużo ruchu, w obszarze bezpośredniego zasięgu słyszy tylko jednego nadawcę naraz. Gdy dwie osoby nadają jednocześnie w zasięgu bezpośrednim, pakiety się nakładają, następuje kolizja i dane przepadają ("jak nie weszło, przepadło"). Kilkadziesiąt węzłów nadających naraz: kolizje, utrata pakietów, retransmisje, jeszcze większy tłok, sieć praktycznie przestaje działać (congestion collapse). Moment krytyczny pojawia się prędzej czy później przy powiększaniu sieci bez umiaru.
Słowa: kolizje, tłok, zatkanie, congestion collapse, giną pakiety, nakładanie, retransmisje, gubienie wiadomości
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora)

### Co to są "pokemony" i "głuchy telefon" na liście węzłów?
Pokemony to węzły widoczne na liście tylko dlatego, że ich ogłoszenia dotarły burzą rozgłoszeniową z dalekich miejsc (szczególnie przy wzmożonej propagacji, gdy pakiety z ogromnych obszarów docierają setki kilometrów); nie da się z nimi nawiązać łączności. Głuchy telefon to wiadomości bez kontekstu z odległych miejsc, na które trudno odpowiedzieć. Wg autora nie mają wartości poza emocjonalną "butelką z wiadomością z końca świata", a ich ceną jest zablokowanie lokalnego ruchu (przestają latać wiadomości lokalne przez 1–2 przeskoki).
Słowa: pokemony, pokemon, głuchy telefon, nody z daleka, propagacja, lista węzłów, duch, widmo, daleki węzeł
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora)

### Jak według autora radzić sobie z wielkością sieci lokalnej?
Ustawienie właściwego presetu zwiększa pojemność i szybkość sieci na danym obszarze, ale nie zasięg przy danej gęstości węzłów, i zawsze jest granica nie do przekroczenia. Zmiana presetu pomaga tylko na jakiś czas (większa pojemność lokalna i prędkość, mniejszy dystans bezpośredni). Rozsądnym mechanizmem jest ustawienie maksymalnej liczby przeskoków: zabija ruch śmieciowy z odległych węzłów, a pozwala na sprawną łączność na założonym lokalnym obszarze. Autor proponuje "polski preset" w całym kraju z ograniczonym obszarowo hop limit, tak by osoby podróżujące zawsze były w zasięgu lokalnej sieci bez zmiany presetów.
Słowa: hop limit, preset, polski preset, obszar lokalny, ograniczenie przeskoków, pojemność sieci, rozwiązanie, lokalna sieć
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opinia autora)

### Co pokazują rysunki w artykule o sieciach lokalnych?
Rysunek z mapą Polski: cztery przekaźniki w rogach kraju, a między nimi strzałki z czerwonymi krzyżykami, czyli bezpośrednia łączność między odległymi końcami kraju nie działa. Tabela stosu Internetu: 7 aplikacja (HTTP, SMTP, DNS), 6 prezentacja (TLS, kompresja), 5 sesja, 4 transport (TCP/UDP: kontrola błędów, retransmisje, porty, przepływ), 3 sieciowa (globalne adresowanie i routing), 2 łącza danych (Ethernet, Wi-Fi, MAC, LAN), 1 fizyczna (praktycznie nieograniczona przepustowość). Tabela mesh LoRa: 7–5 praktycznie brak (proste komunikaty: tekst, telemetria), 4 brak TCP (brak portów i kontroli przepływu, zwykle proste ACK), 3 uproszczona (brak IP, globalnego routingu i hierarchii; flooding lub proste tablice sąsiedztwa), 2 LoRa MAC i identyfikatory węzłów, 1 radio LoRa (bardzo mała, sztywno ograniczona przepustowość). Zrzuty map z aplikacji pokazują setki węzłów (np. 307 w polu "Search 307 Nodes") rozsianych po Polsce i sąsiednich krajach, mimo że realnie z większością nie ma łączności ("pokemony").
Słowa: rysunek, tabela OSI, stos, mapa Polski, krzyżyki, mapa węzłów, zrzut ekranu, ilustracje, pokemony na mapie
Źródło: https://meshtastic.pop.pl/viewtopic.php?t=290 (sprawdzono 2026-10; opis rysunków artykułu)
