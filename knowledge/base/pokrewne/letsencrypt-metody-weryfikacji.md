<!-- Baza wiedzy MT_SW: metody weryfikacji domeny w Let's Encrypt (HTTP-01 i DNS-01). Tylko informacyjnie. -->
# Let's Encrypt: metody weryfikacji domeny

### Jak działa weryfikacja HTTP-01 w Let's Encrypt i co musi być otwarte?
Przy HTTP-01 klient ACME umieszcza na serwerze plik pod adresem http://twoja-domena/.well-known/acme-challenge/TOKEN. Plik zawiera token i odcisk klucza konta. Let's Encrypt próbuje go pobrać, czasem z kilku punktów naraz. Ta metoda działa tylko na porcie 80, bo standard ACME nie pozwala na inne porty. Let's Encrypt podąża za przekierowaniami do 10 kroków, ale tylko do celów http: lub https: na portach 80 lub 443. Dla domowego serwera oznacza to, że port 80 musi być osiągalny z internetu, a przekierowanie z HTTP na HTTPS nie może prowadzić na inny port. Ta reguła dotyczy weryfikacji, a nie samego certyfikatu.
Słowa: http-01, weryfikacja, acme, .well-known, acme-challenge, port 80, przekierowanie, certbot, walidacja domeny, token, lets encrypt, domena, otwarty port
Źródło: https://letsencrypt.org/docs/challenge-types/ (sprawdzono 2026-10; oficjalna dokumentacja Let's Encrypt, ostatnia aktualizacja 7 stycznia 2025)

### Kiedy wybrać DNS-01 zamiast HTTP-01 w Let's Encrypt?
DNS-01 dowodzi kontroli nad domeną przez rekord TXT o nazwie _acme-challenge.twoja-domena. Jest trudniejszy w konfiguracji, ale działa tam, gdzie HTTP-01 nie zadziała, i pozwala wystawić certyfikat wildcard. Klient ACME dodaje rekord po otrzymaniu tokenu, a Let's Encrypt sprawdza go w DNS. Dokumentacja zaleca usuwanie starych rekordów TXT, bo za duża odpowiedź może zostać odrzucona. Automatyzacja ma sens tylko wtedy, gdy dostawca DNS ma API, z którego klient może korzystać. Wnioski o domowym routerze są z ogólnej wiedzy, a nie ze źródła.
Słowa: dns-01, txt, _acme-challenge, wildcard, certyfikat wildcard, dostawca dns, api dns, cname, ns, rekord txt, odpowiedź za duża, nie na porcie 80
Źródło: https://letsencrypt.org/docs/challenge-types/ (sprawdzono 2026-10; oficjalna dokumentacja Let's Encrypt)
