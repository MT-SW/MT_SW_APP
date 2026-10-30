<!-- Baza wiedzy MT_SW: generowanie kluczy SSH poleceniem ssh-keygen (typ ed25519, komentarz, hasło). Tylko informacyjnie. -->
# SSH: generowanie kluczy ssh-keygen

### Jak wygenerować klucz SSH typu ed25519 poleceniem ssh-keygen?
Polecenie ssh-keygen bez argumentów tworzy klucz Ed25519, więc -t ed25519 jest tu tylko wyraźnym wyborem typu. Przydatne opcje to -C, które ustawia komentarz (domyślnie user@host), oraz -f, które wskazuje plik wynikowy. Dla przykładu ssh-keygen -t ed25519 -C "komentarz" -f ~/.ssh/nazwa tworzy parę kluczy: prywatny i publiczny z końcówką .pub. Źródło nie podaje, czy domyślna nazwa to id_ed25519, więc sprawdź ją na swoim systemie. Klucz publiczny wklejasz na serwer, a prywatnego nigdzie nie wysyłasz.
Słowa: ssh-keygen, ed25519, klucz ssh, id_ed25519, komentarz, -C, -f, klucz publiczny, klucz prywatny, .pub, para kluczy, logowanie bez hasła, serwer ssh
Źródło: https://man.openbsd.org/ssh-keygen.1 (sprawdzono 2026-10; oficjalna strona podręcznika OpenBSD dla ssh-keygen; domyślna nazwa pliku z ogólnej wiedzy)

### Jak zmienić hasło klucza SSH i jak sprawdzić jego odcisk?
Opcja -p zmienia hasło istniejącego klucza prywatnego. Program pyta o plik, stare hasło i nowe hasło dwa razy. Opcja -N podaje nowe hasło w poleceniu, a pusta wartość oznacza klucz bez hasła. Bez hasła każdy, kto skopiuje plik klucza, może się nim zalogować, więc hasło warto ustawić dla kluczy osobistych. Opcja -l pokazuje odcisk klucza publicznego, a z -v także obrazek ASCII. Odcisku używa się do sprawdzenia, czy klucz na serwerze jest ten sam, co na komputerze.
Słowa: ssh-keygen -p, zmiana hasła klucza, -N, pusty passphrase, odcisk klucza, fingerprint, -l, -v, sprawdzenie klucza, hasło ssh, klucz bez hasła, passphrase
Źródło: https://man.openbsd.org/ssh-keygen.1 (sprawdzono 2026-10; oficjalna strona podręcznika OpenBSD dla ssh-keygen)
