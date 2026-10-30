### Czym jest problem ukrytego węzła według Wikipedii?
Występuje w sieci bezprzewodowej, gdy węzeł może łączyć się z punktem dostępowym, ale nie słyszy innych węzłów, które też z nim rozmawiają. Te węzły mogą nadawać jednocześnie, a wynikająca z tego interferencja w punkcie dostępowym uniemożliwia dotarcie pakietów.

Słowa: problem ukrytego węzła, hidden node, punkt dostępowy, interferencja, kolizja, bezprzewodowa
Źródło: https://en.wikipedia.org/wiki/Hidden_node_problem (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Kiedy powstaje problem ukrytego węzła według Wikipedii?
Gdy węzły są na przeciwnych krańcach zasięgu punktu dostępowego albo między nimi jest przeszkoda, która odbija lub pochłania fale radiowe. Oba węzły dochodzą do punktu dostępowego, ale nie słyszą siebie nawzajem. Nie wykrywają więc kolizji, a CSMA/CD nie działa, i dane ulegają uszkodzeniu w punkcie dostępowym.

Słowa: ukryty węzeł, krańce zasięgu, przeszkoda, odbicie, pochłanianie, CSMA/CD, uszkodzenie danych
Źródło: https://en.wikipedia.org/wiki/Hidden_node_problem (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak łagodzi się problem ukrytego węzła według Wikipedii?
Strona wymienia RTS/CTS (Request To Send / Clear To Send). Węzły wysyłają krótkie żądanie pozwolenia do punktu dostępowego przed dłuższym pakietem danych. Odpowiedź punktu widzą wszystkie węzły, co pozwala im koordynować nadawanie. Strona zaznacza, że RTS/CTS nie jest pełnym rozwiązaniem, dodaje opóźnienie, a narzut może przewyższać korzyść, zwłaszcza przy krótkich pakietach.

Słowa: RTS/CTS, Request To Send, Clear To Send, pozwolenie, koordynacja, opóźnienie, narzut, krótkie pakiety
Źródło: https://en.wikipedia.org/wiki/Hidden_node_problem (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest zalewanie (flooding) w sieciach według Wikipedii?
To sposób routingu, w którym każdy przychodzący pakiet jest wysyłany przez wszystkie łącza wyjściowe oprócz tego, którym przyszedł.

Słowa: flooding, zalewanie, routing, pakiet, łącze wyjściowe, wszystkie łącza
Źródło: https://en.wikipedia.org/wiki/Flooding_(computer_networking) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie problemy niesie zalewanie w sieci według Wikipedii?
Niekontrolowane zalewanie może wywołać burze rozgłoszeniowe, gdy nic nie zapobiega nieskończonemu krążeniu tego samego pakietu. Duplikaty wiadomości mogą też krążyć bez końca, zwiększając obciążenie i wymagając dodatkowej pracy do ich odrzucania.

Słowa: flooding, burza rozgłoszeniowa, duplikaty, krążenie, obciążenie, odrzucanie
Źródło: https://en.wikipedia.org/wiki/Flooding_(computer_networking) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak ograniczyć zalewanie w sieci według Wikipedii?
Strona wymienia licznik przeskoków albo TTL dołączany do każdego pakietu, który powinien odzwierciedlać, przez ile węzłów pakiet może przejść. Inne środki to zapamiętywanie przez każdy węzeł pakietów, które już widział, żeby przesłać każdy tylko raz, oraz wymuszanie topologii bez pętli.

Słowa: flooding, TTL, licznik przeskoków, zapamiętywanie, jednokrotne przesłanie, topologia bez pętli
Źródło: https://en.wikipedia.org/wiki/Flooding_(computer_networking) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym różni się sieć w pełni połączona od częściowej według Wikipedii?
Sieć w pełni połączona to taka, w której każdy węzeł jest połączony z każdym. Strona podaje, że przewodowe sieci w pełni połączone są bezpieczniejsze i niezawodniejsze, bo problem z kablem dotyczy tylko dwóch węzłów, które łączy. Liczba kabli i koszt szybko rosną wraz z liczbą węzłów. Strona nie definiuje wprost częściowej siatki, tylko przeciwstawia siatkę gwieździe i drzewu, gdzie węzły łączą się tylko z niewielkim podzbiorem.

Słowa: sieć w pełni połączona, siatka, gwiazda, drzewo, niezawodność, koszt, kable, podzbiór
Źródło: https://en.wikipedia.org/wiki/Mesh_networking (sprawdzono 2026-10, źródło angielskie, Wikipedia)
