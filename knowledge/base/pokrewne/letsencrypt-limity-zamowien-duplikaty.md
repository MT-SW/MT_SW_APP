### Ile certyfikatów Let's Encrypt można wystawić dla domeny w 7 dni?
Limit to 50 certyfikatów na zarejestrowaną domenę (lub adres IPv4, albo zakres IPv6 /64) na 7 dni. Limit jest globalny, więc liczą się zamówienia ze wszystkich kont. Pojemność odnawia się po 1 certyfikacie co 202 minuty, a strona podaje możliwość prośby o podwyższenie.

Słowa: letsencrypt, limit, 50 certyfikatów, 7 dni, domena, IPv4, IPv6, 202 minuty
Źródło: https://letsencrypt.org/docs/rate-limits/ (sprawdzono 2026-10)

### Ile razy można wystawić ten sam zestaw nazw w Let's Encrypt?
Dla dokładnie tego samego zestawu identyfikatorów dozwolone jest 5 certyfikatów na 7 dni, liczone globalnie. Pojemność odnawia się po 1 certyfikacie co 34 godziny. Strona nie oferuje podwyższenia tego limitu.

Słowa: letsencrypt, duplikat, 5 certyfikatów, 7 dni, zestaw identyfikatorów, 34 godziny
Źródło: https://letsencrypt.org/docs/rate-limits/ (sprawdzono 2026-10)

### Jaki jest limit nieudanych walidacji w Let's Encrypt?
Konto może mieć do 5 nieudanych autoryzacji na identyfikator w ciągu godziny. Pojemność odnawia się po 1 błędzie na identyfikator co 12 minut. Po przekroczeniu konto nie może składać nowych zamówień dla tego identyfikatora do resetu limitu. Nie ma tu podwyższeń.

Słowa: letsencrypt, nieudana walidacja, autoryzacja, 5 na godzinę, 12 minut, identyfikator
Źródło: https://letsencrypt.org/docs/rate-limits/ (sprawdzono 2026-10)
