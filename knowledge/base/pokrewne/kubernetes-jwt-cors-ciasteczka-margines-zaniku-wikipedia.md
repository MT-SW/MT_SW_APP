### Czym jest margines zaniku (fade margin) według Wikipedii?
To dodatkowy zapas mocy sygnału, z jakim projektuje się system, żeby zanik nie pogarszał usługi. Strona podaje dwie pokrewne definicje: zapas projektowy, czyli dodatkowe wzmocnienie lub czułość na oczekiwany zanik, oraz tolerancję spadku sygnału, czyli o ile może spaść poziom odbioru bez spadku poniżej progu.

Słowa: margines zaniku, fade margin, zapas mocy, zanik, tolerancja spadku, próg
Źródło: https://en.wikipedia.org/wiki/Fade_margin (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Gdzie używa się marginesu zaniku według Wikipedii?
Strona podaje, że termin stosuje się głównie do systemów łączności, np. satelitarnych. Jako przykład podaje, że Globalstar działa z marginesem zaniku 25–35 dB. Strona nie wiąże go wprost z niezawodnością ani dostępnością.

Słowa: margines zaniku, systemy łączności, satelity, Globalstar, 25–35 dB, niezawodność
Źródło: https://en.wikipedia.org/wiki/Fade_margin (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Kubernetes według Wikipedii?
Kubernetes, czyli K8s, to otwartoźródłowy system orkiestracji kontenerów do automatyzacji wdrażania, skalowania i zarządzania oprogramowaniem. Łączy jedną lub więcej maszyn, wirtualnych albo fizycznych, w klaster, który uruchamia obciążenia w kontenerach.

Słowa: Kubernetes, K8s, orkiestracja kontenerów, wdrażanie, skalowanie, klaster, kontenery
Źródło: https://en.wikipedia.org/wiki/Kubernetes (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Z jakich części składa się Kubernetes według Wikipedii?
Płaszczyzna sterowania zarządza obciążeniem klastra i kieruje komunikacją w systemie. Jej elementy to etcd, który przechowuje konfigurację klastra, i serwer API, który udostępnia API Kubernetes. Węzły to maszyny, na których wdraża się kontenery. Każdy węzeł ma środowisko uruchomieniowe kontenerów i kubelet, który dba o zdrowie kontenerów na węźle.

Słowa: Kubernetes, płaszczyzna sterowania, etcd, serwer API, węzły, kubelet, środowisko kontenerów
Źródło: https://en.wikipedia.org/wiki/Kubernetes (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest JWT według Wikipedii?
JSON Web Token to proponowany standard internetowy do tworzenia danych z opcjonalnym podpisem i opcjonalnym szyfrowaniem. Ładunek zawiera JSON z deklaracjami, np. informacjami o tożsamości przekazywanymi między dostawcą tożsamości a dostawcą usługi. Tokeny są zwarte i bezpieczne dla adresów URL.

Słowa: JWT, JSON Web Token, podpis, szyfrowanie, deklaracje, tożsamość, zwarty, URL
Źródło: https://en.wikipedia.org/wiki/JSON_Web_Token (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Z jakich trzech części składa się JWT według Wikipedii?
Nagłówek wskazuje algorytm podpisu, np. HS256 (HMAC-SHA256) lub RS256 (RSA z SHA-256). Ładunek zawiera deklaracje, a specyfikacja definiuje siedem zarejestrowanych nazw. Podpis weryfikuje token i jest liczony z zakodowanego Base64url nagłówka i ładunku połączonych kropką. Wszystkie trzy części kodowane są Base64url i łączone kropkami.

Słowa: JWT, nagłówek, ładunek, podpis, HS256, RS256, Base64url, kropka, deklaracje
Źródło: https://en.wikipedia.org/wiki/JSON_Web_Token (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czy ładunek JWT jest domyślnie szyfrowany według Wikipedii?
Strona nie podaje wprost, czy ładunek jest domyślnie szyfrowany. Mówi tylko, że szyfrowanie jest opcjonalne.

Słowa: JWT, ładunek, szyfrowanie opcjonalne, nie podano wprost
Źródło: https://en.wikipedia.org/wiki/JSON_Web_Token (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest CORS według Wikipedii?
CORS to mechanizm pozwalający stronie WWW na dostęp do ograniczonych zasobów z serwera w innej domenie niż ta, która wysłała stronę. Jest bezpiecznym sposobem obejścia polityki tego samego pochodzenia (same-origin policy).

Słowa: CORS, mechanizm, zasoby, inna domena, polityka tego samego pochodzenia, same-origin
Źródło: https://en.wikipedia.org/wiki/Cross-origin_resource_sharing (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak przeglądarka obsługuje CORS według Wikipedii?
Przeglądarka dodaje nagłówek Origin do żądań między domenami. Dla niektórych żądań najpierw wysyła wstępne żądanie OPTIONS, pytając serwer o dozwolone metody. Jeśli serwer nie zatwierdzi pochodzenia, przeglądarka wstrzymuje właściwe żądanie.

Słowa: CORS, nagłówek Origin, OPTIONS, preflight, zatwierdzenie, wstrzymanie żądania
Źródło: https://en.wikipedia.org/wiki/Cross-origin_resource_sharing (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co robi nagłówek Access-Control-Allow-Origin według Wikipedii?
Ustawia go zewnętrzny serwer, nie serwer strony. Może wskazać dozwolone źródło, np. adres strony, albo symbol wieloznaczny „*”, który pozwala na żądania z każdej domeny. Strona zaznacza, że gwiazdka nie pozwala wysyłać poświadczeń, takich jak ciasteczka czy uwierzytelnienie HTTP.

Słowa: CORS, Access-Control-Allow-Origin, źródło, gwiazdka, poświadczenia, ciasteczka, uwierzytelnienie HTTP
Źródło: https://en.wikipedia.org/wiki/Cross-origin_resource_sharing (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest ciasteczko HTTP według Wikipedii?
To mały blok danych, który serwer tworzy podczas przeglądania. Przeglądarka zapisuje go na urządzeniu i odsyła przy kolejnych żądaniach. Pozwala serwerom przechowywać stan, np. zawartość koszyka albo status logowania.

Słowa: ciasteczko, cookie, HTTP, przeglądarka, stan, koszyk, logowanie
Źródło: https://en.wikipedia.org/wiki/HTTP_cookie (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co oznacza atrybut Secure w ciasteczku według Wikipedii?
Ciasteczko jest przesyłane tylko przez szyfrowane połączenie HTTPS, co zmniejsza ryzyko podsłuchu. Strona zaznacza, że ciasteczko ustawione z tym atrybutem przez niezabezpieczone połączenie nadal może zostać przechwycone, więc dla maksymalnego bezpieczeństwa ustawia się je tylko przez bezpieczne połączenie.

Słowa: ciasteczko, Secure, HTTPS, podsłuch, przechwycenie, bezpieczne połączenie
Źródło: https://en.wikipedia.org/wiki/HTTP_cookie (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co oznacza atrybut HttpOnly w ciasteczku według Wikipedii?
Ciasteczka nie można odczytać przez API po stronie klienta, np. JavaScript. Usuwa to zagrożenie kradzieży ciasteczek przez XSS, ale ciasteczko nadal jest podatne na cross-site tracing i CSRF.

Słowa: ciasteczko, HttpOnly, JavaScript, XSS, kradzież, cross-site tracing, CSRF
Źródło: https://en.wikipedia.org/wiki/HTTP_cookie (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co robi atrybut SameSite w ciasteczku według Wikipedii?
Wprowadził go Google Chrome w wersji 51 w 2016 roku. Wartości to Strict, Lax i None. Strict wysyła ciasteczka tylko do domeny zgodnej z domeną pochodzenia, co według strony skutecznie łagodzi CSRF. Lax wysyła je przy bezpiecznych żądaniach, np. GET, ale nie w ciasteczkach stron trzecich w ramkach iframe. None zezwala na ciasteczka stron trzecich, a większość przeglądarek wymaga wtedy atrybutu Secure.

Słowa: ciasteczko, SameSite, Strict, Lax, None, CSRF, iframe, Chrome 51, 2016, Secure
Źródło: https://en.wikipedia.org/wiki/HTTP_cookie (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co robi atrybut Expires w ciasteczku według Wikipedii?
Ustawia konkretną datę i godzinę, po których przeglądarka usunie ciasteczko. Ciasteczka z tym atrybutem są trwałe. Strona wspomina też, że Max-Age ustawia wygaśnięcie jako liczbę sekund od odebrania.

Słowa: ciasteczko, Expires, Max-Age, trwałe, data wygaśnięcia, sekundy
Źródło: https://en.wikipedia.org/wiki/HTTP_cookie (sprawdzono 2026-10, źródło angielskie, Wikipedia)
