# WireGuard, DNS, hasła i Git

### Czym jest WireGuard i jak działają jego klucze?
WireGuard to otwarty, lekki protokół VPN przez UDP, domyślnie port 51820; w jądrze Linuksa jest od wersji 5.6. Każdy uczestnik (peer) ma parę kluczy Curve25519 i wymienia się kluczami publicznymi; każdemu przypisuje się adres IP w tunelu, zwykle z puli prywatnej. Handshake opiera się na Noise IK, a szyfrowanie na ChaCha20-Poly1305. Opcjonalny klucz wstępny dokłada ochronę na przyszłość. Narzut to 60 B na pakiet w IPv4 i 80 B w IPv6; sugerowane MTU to 1420 (podwójny stos), 1440 (tylko IPv4) i 1280 dla klientów mobilnych. Jest prostszy i podobno szybszy od OpenVPN i IPsec, ale liczb zestawienia artykuł nie podaje. Składni pliku konfiguracyjnego nie opisuje.
Słowa: WireGuard, VPN, UDP 51820, Curve25519, klucz publiczny, peer, tunel, MTU, OpenVPN, ChaCha20, Tailscale
Źródło: https://en.wikipedia.org/wiki/WireGuard (sprawdzono 2026-10)

### Jak działa DNS i dlaczego zmiana nie działa od razu?
DNS tłumaczy nazwy domen na adresy IP. Rekordy: A (IPv4), AAAA (IPv6), CNAME (alias), MX (serwer poczty), TXT (tekst, np. weryfikacje). Resolver wypytuje serwery od głównych do autorytatywnych; w domu zwykle robi to rekurencyjny serwer z pamięcią podręczną, często dostawcy internetu. Każdy rekord ma TTL (od sekund do tygodni), a zapamiętane odpowiedzi wygasają dopiero po nim, więc zmiany rozchodzą się z opóźnieniem. Standardowo port 53 (UDP, a TCP dla dużych odpowiedzi). Zwykły DNS nie jest szyfrowany; DoT szyfruje go w TLS na porcie 853, a DoH w HTTPS na 443.
Słowa: DNS, rekord A, AAAA, CNAME, MX, TXT, TTL, resolver, port 53, DoH, DoT, cache DNS, domena, Pi-hole
Źródło: https://en.wikipedia.org/wiki/Domain_Name_System (sprawdzono 2026-10)

### Hasła: menedżer, 2FA i phishing
Menedżer haseł trzyma dane w zaszyfrowanym sejfie odblokowywanym hasłem głównym, generuje długie losowe hasła i je wstawia; dzięki temu każde konto ma inne hasło, a reużywanie haseł to główny problem, który rozwiązuje. Sam sejf i hasło główne są cennym celem, więc hasło główne musi być silne i nie oparte na starych hasłach ani danych osobowych. Dodatkowa ochrona to uwierzytelnianie wieloskładnikowe odporne na phishing oraz klucze dostępu (passkeys) związane z konkretną stroną. Uważaj na fałszywe ekrany odblokowania (w jednej symulacji z 2025 r. ok. 31% osób wpisało hasło główne) oraz na autouzupełnianie na przejętych stronach.
Słowa: hasła, menedżer haseł, 2FA, passkey, phishing, hasło główne, reużywanie haseł, generator haseł, bezpieczeństwo konta
Źródło: https://en.wikipedia.org/wiki/Password_manager (sprawdzono 2026-10)

### Git: commit, gałąź, zdalne repozytorium i ostrożność z force push
Commit to zapis stanu projektu z komunikatem i odnośnikiem do poprzedników: git add dodaje pliki do poczekalni, git commit -m zapisuje. Gałąź to lekki wskaźnik na commit; nazwa master nie jest szczególna, wiele serwisów używa main. Każde repozytorium ma pełną historię, domyślny zdalny nazywa się origin. Gdy scalanie samo nie wyjdzie, Git każe poprawić ręcznie. Wypchnięte commity zwykle się nie nadpisuje, tylko cofa nowym commitem; przepisywanie wspólnej historii (np. force push) jest ryzykowne, bo cudze commity mogą wskazywać na coś, czego serwer już nie ma, a bezpieczniej jest --force-with-lease. Plik .gitignore wyklucza z śledzenia klucze, hasła i duże pliki. Rebase źródło nie opisuje.
Słowa: Git, commit, branch, merge, origin, push, pull, force push, force-with-lease, gitignore, repozytorium, GitHub
Źródło: https://en.wikipedia.org/wiki/Git (sprawdzono 2026-10; --force-with-lease: wiedza ogólna)

### WireGuard: prosty VPN do domowego serwera
WireGuard to darmowy, otwarty protokół VPN, lżejszy i szybszy z założenia niż IPsec i OpenVPN. Każda strona ma parę kluczy (publiczny i prywatny, Curve25519); wymienia się klucze publiczne i nadaje adresy, zwykle prywatne. Działa tylko po UDP, domyślnie port 51820. Jest na Androidzie, iOS, Linuksie (w jądrze od wersji 5.6), macOS i Windows. Narzut na pakiet: 60 bajtów przez IPv4, 80 przez IPv6, co trzeba uwzględnić w MTU. Tailscale to osobna usługa; w artykule występuje tylko jako darczyńca projektu.
Słowa: WireGuard, VPN, port 51820, UDP, klucz publiczny, dostęp zdalny, serwer domowy, Tailscale, MTU, tunel
Źródło: https://en.wikipedia.org/wiki/WireGuard (sprawdzono 2026-10)
