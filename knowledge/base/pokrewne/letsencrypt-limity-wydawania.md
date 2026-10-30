<!-- Baza wiedzy MT_SW: limity wydawania certyfikatów w Let's Encrypt i wyjątek dla odnowień. Tylko informacyjnie. -->
# Let's Encrypt: limity wydawania certyfikatów

### Ile certyfikatów Let's Encrypt można wydać dla jednej domeny w tygodniu?
Limit to 50 certyfikatów dla jednej zarejestrowanej domeny (albo adresu IPv4, albo zakresu IPv6 /64) w ciągu 7 dni. Limit dotyczy wszystkich kont naraz. Odnowienie co 202 minuty zwraca jedną pozycję. Zarejestrowana domena to ta, którą kupiłeś u rejestratora, np. dla www.przyklad.pl to przyklad.pl. Podwyższenie limitu wymaga prośby do Let's Encrypt. Dla domowego serwera z jedną domeną to zwykle wystarczający zapas. Wniosek, że przy testach warto używać środowiska staging, jest zalecany przez dokumentację.
Słowa: let's encrypt limit, 50 certyfikatów, 7 dni, zarejestrowana domena, rate limit, lets encrypt limity, przekroczenie limitu, staging, zakres ipv6, 202 minuty
Źródło: https://letsencrypt.org/docs/rate-limits/ (sprawdzono 2026-10; oficjalna dokumentacja Let's Encrypt)

### Ile razy można wydać certyfikat dla tego samego zestawu nazw i kiedy wyjątek ARI nie dotyczy?
Limit dla dokładnie tego samego zestawu nazw wynosi 5 certyfikatów w ciągu 7 dni. Bez wyjątku, bez obejścia przez prośbę. Odnowienie koordynowane przez ACME Renewal Info (ARI) jest zwolnione ze wszystkich limitów, ale tylko gdy nowe zamówienie zawiera przynajmniej jeden identyfikator z wymienianego certyfikatu i ten certyfikat nie był wcześniej wymieniony przez ARI. Częste reinstalacje klienta albo kasowanie jego konfiguracji przy każdym wdrożeniu to typowe przyczyny przekroczenia. Dlatego w testach ustaw staging, a nie produkcję. Wniosek dla domu z ogólnej wiedzy: nie kasuj konfiguracji klienta przy każdej zmianie.
Słowa: duplicate certificate limit, zestaw nazw, 5 certyfikatów, ari, renewal info, odnowienie zwolnione, wyjątek od limitu, reinstalacja, staging, przekroczenie limitu certbot, konfiguracja klienta
Źródło: https://letsencrypt.org/docs/rate-limits/ (sprawdzono 2026-10; oficjalna dokumentacja Let's Encrypt; zalecenie o konfiguracji klienta z ogólnej wiedzy)
