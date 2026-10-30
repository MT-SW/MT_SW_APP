### Czym jest VPN według Wikipedii?
VPN to sieć nakładkowa, która rozciąga prywatną sieć na sieć publiczną, np. internet, dzięki szyfrowaniu i protokołom tunelowania.

Słowa: VPN, sieć nakładkowa, sieć prywatna, sieć publiczna, szyfrowanie, tunelowanie
Źródło: https://en.wikipedia.org/wiki/Virtual_private_network (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie korzyści daje VPN według Wikipedii?
Prywatność, bo VPN może sprawić, że dostawca internetu nie będzie miał dostępu do danych przesyłanych przez VPN. Bezpieczny zdalny dostęp, bo VPN typu host–sieć pozwala użytkownikom spoza biura bezpiecznie łączyć się z siecią firmową. Poufność, bo szyfrowanie zmniejsza ryzyko podsłuchu danych z zewnątrz.

Słowa: VPN, prywatność, dostawca internetu, zdalny dostęp, poufność, podsłuch, szyfrowanie
Źródło: https://en.wikipedia.org/wiki/Virtual_private_network (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie ograniczenie VPN wskazuje Wikipedia?
VPN nie czyni połączonych użytkowników anonimowymi wobec niezaufanego nośnika, np. dostawcy internetu. Dostawca VPN i usługi online, z których się korzysta przez VPN, mogą nadal widzieć aktywność użytkownika.

Słowa: VPN, anonimowość, dostawca internetu, dostawca VPN, aktywność, ograniczenie
Źródło: https://en.wikipedia.org/wiki/Virtual_private_network (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest split tunneling według Wikipedii?
Split tunneling w VPN pozwala jednocześnie łączyć się z różnymi domenami bezpieczeństwa, np. z pobliską siecią Wi-Fi, zdalną siecią firmową i publicznym internetem. Tunelem przechodzi tylko ruch kierowany do VPN, a reszta idzie bezpośrednio do celu.

Słowa: split tunneling, tunel VPN, domeny bezpieczeństwa, ruch, bezpośrednio, sieć firmowa
Źródło: https://en.wikipedia.org/wiki/Split_tunneling (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie są typy split tunneling według Wikipedii?
Split-include: tunelem idzie tylko ruch do określonego zbioru celów. Split-exclude: tunelem idzie cały ruch oprócz ruchu do określonego zbioru celów.

Słowa: split tunneling, split-include, split-exclude, zbiór celów, tunel
Źródło: https://en.wikipedia.org/wiki/Split_tunneling (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie ryzyko niesie split tunneling według Wikipedii?
Główne ryzyko to obejście zabezpieczeń na poziomie bramy w infrastrukturze firmy, bo ruch poza tunelem nie przechodzi przez tę bramę.

Słowa: split tunneling, ryzyko, obejście zabezpieczeń, brama, infrastruktura firmy
Źródło: https://en.wikipedia.org/wiki/Split_tunneling (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co to jest NAT według Wikipedii?
NAT odwzorowuje jedną przestrzeń adresów IP na inną, zmieniając informacje adresowe w nagłówkach IP pakietów przechodzących przez urządzenie routujące.

Słowa: NAT, odwzorowanie, adresy IP, nagłówki IP, routing
Źródło: https://en.wikipedia.org/wiki/Network_address_translation (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie typy NAT wymienia Wikipedia?
Static NAT to odwzorowanie jeden do jednego między adresem wewnętrznym a publicznym, zwane też basic NAT. Dynamic NAT używa grupy publicznych adresów zamiast jednego. PAT, czyli NAPT, odwzorowuje wiele prywatnych hostów na jeden publiczny adres, rozróżniając połączenia numerami portów. Strona nazywa PAT najczęstszym typem, znanym też jako NAT overload.

Słowa: NAT, static NAT, dynamic NAT, PAT, NAPT, NAT overload, numery portów, jeden do jednego
Źródło: https://en.wikipedia.org/wiki/Network_address_translation (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Dlaczego NAT pomaga przy braku adresów IPv4 według Wikipedii?
Adresy IPv4 są ograniczone. Jeden publiczny adres IP może obsłużyć całą sieć prywatną, co oszczędza publiczną przestrzeń adresową w miarę wyczerpywania się IPv4.

Słowa: NAT, IPv4, ograniczone adresy, jeden publiczny adres, oszczędność, wyczerpywanie
Źródło: https://en.wikipedia.org/wiki/Network_address_translation (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest hairpinning według Wikipedii?
Hairpinning, zwany też NAT loopback albo NAT reflection, to funkcja wielu domowych routerów. Pozwala maszynie w sieci LAN dotrzeć do innej maszyny w tej samej sieci przez zewnętrzny adres IP routera, o ile skonfigurowano przekierowanie portów.

Słowa: hairpinning, NAT loopback, NAT reflection, LAN, zewnętrzny adres IP, przekierowanie portów, router
Źródło: https://en.wikipedia.org/wiki/Hairpinning (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak działa hairpinning i kiedy wymaga SNAT według Wikipedii?
Host w LAN wysyła ruch na publiczny adres WAN routera. Router rozpoznaje ten adres jako własny interfejs i używa reguł przekierowania portów (DNAT), żeby wybrać cel. Gdy brak pasującej reguły, pakiet jest odrzucany, a router może odesłać ICMP Destination Unreachable. Odpowiedź serwera może pójść bezpośrednio do komputera w LAN, który ją odrzuci. Reguła SNAT przepisująca adres źródłowy na adres routera kieruje odpowiedzi z powrotem przez router.

Słowa: hairpinning, DNAT, SNAT, reguła przekierowania, ICMP, Destination Unreachable, odpowiedź, adres źródłowy
Źródło: https://en.wikipedia.org/wiki/Hairpinning (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czy wszystkie routery obsługują hairpinning według Wikipedii?
Strona nie podaje, że wszystkie routery to obsługują. Zaznacza jedynie, że wiele domowych routerów ma tę funkcję.

Słowa: hairpinning, routery domowe, nie wszystkie, obsługa, funkcja
Źródło: https://en.wikipedia.org/wiki/Hairpinning (sprawdzono 2026-10, źródło angielskie, Wikipedia)
