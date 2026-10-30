### Czym jest zapora sieciowa według Wikipedii?
Zapora to system bezpieczeństwa sieci, który monitoruje i kontroluje ruch przychodzący i wychodzący według skonfigurowanych reguł. Zwykle stoi między zaufaną siecią a niezaufaną, np. internetem.

Słowa: zapora sieciowa, firewall, reguły, ruch przychodzący, wychodzący, zaufana sieć, internet
Źródło: https://en.wikipedia.org/wiki/Firewall_(computing) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie rodzaje zapór wymienia Wikipedia?
Filtr pakietów (pierwsza generacja) sprawdza pakiety według listy kontroli dostępu, po adresie IP, protokole lub porcie. Brama na poziomie obwodu (druga generacja) dodaje śledzenie rozmów między punktami końcowymi, w tym używanych portów. Zapora warstwy aplikacji rozumie konkretne protokoły, np. FTP, DNS i HTTP, i wychwytuje usługi na nietypowych portach. Zapora nowej generacji rozszerza to o filtrowanie stron, zapobieganie włamaniom, zarządzanie tożsamością użytkownika i inspekcję TLS.

Słowa: zapora, filtr pakietów, brama na poziomie obwodu, warstwa aplikacji, nowa generacja, FTP, DNS, HTTP, TLS
Źródło: https://en.wikipedia.org/wiki/Firewall_(computing) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak domyślnie działa filtr pakietów według Wikipedii?
Domyślną akcją filtra pakietów jest ciche odrzucanie ruchu. Typowa konfiguracja przepuszcza ruch z LAN do WAN, a odrzuca ruch z WAN do LAN.

Słowa: filtr pakietów, domyślna akcja, ciche odrzucanie, LAN, WAN, odrzucanie
Źródło: https://en.wikipedia.org/wiki/Firewall_(computing) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest zapora stanowa według Wikipedii?
Zapora stanowa śledzi każdą sesję połączeń przechodzących przez nią. Obejmuje strumienie TCP, datagramy UDP i komunikaty ICMP, z etykietami takimi jak LISTEN, ESTABLISHED lub CLOSING. Wpisy dla dozwolonego ruchu TCP i UDP trafiają do tabeli stanów i są usuwane po okresie bez ruchu, a dla TCP mogą być usunięte po zakończeniu połączenia przez FIN.

Słowa: zapora stanowa, sesja, tabela stanów, LISTEN, ESTABLISHED, CLOSING, FIN, TCP, UDP, ICMP
Źródło: https://en.wikipedia.org/wiki/Stateful_firewall (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak zapora stanowa decyduje o pakiecie według Wikipedii?
Pakiet należący do zapisanej sesji jest traktowany jako powiązany i przepuszczany. Strona podaje, że powiązane pakiety mogą wrócić przez zaporę nawet wtedy, gdy żaden wyraźny wpis nie zezwala na ten host. Odpowiedzi ICMP związane z dozwoloną sesją też są przepuszczane.

Słowa: zapora stanowa, powiązane pakiety, sesja, dozwolona sesja, ICMP, odpowiedź, tabela stanów
Źródło: https://en.wikipedia.org/wiki/Stateful_firewall (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest przekierowanie portu według Wikipedii?
To forma NAT, w której brama, np. router lub zapora, przekierowuje ruch przychodzący na jedną parę adres–port do innego urządzenia. Zwykle ruch z portu routera trafia do usługi w wewnętrznej sieci prywatnej, dzięki czemu hosty z zewnątrz mogą do niej dotrzeć.

Słowa: przekierowanie portu, port forwarding, NAT, brama, router, sieć prywatna, usługa wewnętrzna
Źródło: https://en.wikipedia.org/wiki/Port_forwarding (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Do czego używa się przekierowania portu według Wikipedii?
Do udostępniania usługi z sieci LAN w internecie. Strona wymienia publiczny serwer HTTP, dostęp SSH lub FTP z internetu i publiczny serwer gier.

Słowa: przekierowanie portu, publiczny serwer HTTP, SSH, FTP, serwer gier, LAN, internet
Źródło: https://en.wikipedia.org/wiki/Port_forwarding (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Dlaczego przekierowanie portu może być bezpieczniejsze według Wikipedii?
Systemy uniksopodobne ograniczają porty poniżej 1024 do roota. Uruchamianie oprogramowania z uprawnieniami superużytkownika, by zająć te porty, może być ryzykowne. Przekierowanie portu o niskim numerze na wysoki pozwala uruchomić aplikację jako zwykły użytkownik z ograniczonymi uprawnieniami.

Słowa: przekierowanie portu, porty poniżej 1024, root, superużytkownik, ryzyko, zwykły użytkownik, uprawnienia
Źródło: https://en.wikipedia.org/wiki/Port_forwarding (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest strefa DMZ według Wikipedii?
DMZ, zwana też strefą zdemilitaryzowaną, siecią brzegową lub ekranowaną podsiecią, to podsieć fizyczna lub logiczna, która zawiera usługi zewnętrzne organizacji i wystawia je na niezaufaną sieć, zwykle internet.

Słowa: DMZ, strefa zdemilitaryzowana, siec brzegowa, ekranowana podsieć, usługi zewnętrzne, internet
Źródło: https://en.wikipedia.org/wiki/Demilitarized_zone_(computing) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jaki jest cel strefy DMZ według Wikipedii?
Dodaje warstwę bezpieczeństwa do sieci LAN organizacji. Użytkownicy z zewnątrz mogą dotrzeć tylko do tego, co wystawiono w DMZ, a reszta sieci wewnętrznej pozostaje za zaporą.

Słowa: DMZ, warstwa bezpieczeństwa, LAN, zapora, sieć wewnętrzna, użytkownicy zewnętrzni
Źródło: https://en.wikipedia.org/wiki/Demilitarized_zone_(computing) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak ustawić zapory przy DMZ według Wikipedii?
Pojedyncza zapora (model trzech nóg) ma co najmniej trzy interfejsy: do internetu, sieci wewnętrznej i DMZ. Jest to pojedynczy punkt awarii. Podwójne zapory (back-to-back) mają przednią zaporę przepuszczającą tylko ruch do DMZ i tylną, która przepuszcza ruch do DMZ tylko z sieci wewnętrznej. Strona nazywa to najbezpieczniejszym rozwiązaniem.

Słowa: DMZ, model trzech nóg, pojedynczy punkt awarii, back-to-back, podwójna zapora, przednia, tylna, najbezpieczniejsze
Źródło: https://en.wikipedia.org/wiki/Demilitarized_zone_(computing) (sprawdzono 2026-10, źródło angielskie, Wikipedia)
