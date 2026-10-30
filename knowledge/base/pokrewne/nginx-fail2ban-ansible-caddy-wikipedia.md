### Czym jest Nginx według Wikipedii?
Nginx to serwer WWW, który może też działać jako reverse proxy, równoważnik obciążenia, proxy poczty i cache HTTP. Obsługuje żądania asynchronicznie i sterowany zdarzeniami, a nie wątkami.

Słowa: Nginx, serwer WWW, reverse proxy, równoważnik obciążenia, cache HTTP, asynchroniczny, zdarzenia
Źródło: https://en.wikipedia.org/wiki/Nginx (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Do czego Nginx służy według Wikipedii?
Do serwowania plików statycznych wraz z plikami indeksowymi i auto-indeksem, do programowego równoważenia obciążenia z kontrolą zdrowia w paśmie, oraz jako reverse proxy z buforowaniem.

Słowa: Nginx, pliki statyczne, auto-indeks, równoważenie obciążenia, health check, reverse proxy, buforowanie
Źródło: https://en.wikipedia.org/wiki/Nginx (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest fail2ban według Wikipedii?
Fail2ban to framework do zapobiegania włamaniom, napisany w Pythonie, zaprojektowany do przeciwdziałania atakom brute-force na systemach POSIX. Monitoruje wpisy w plikach logów i uruchamia skrypty na ich podstawie.

Słowa: fail2ban, włamania, brute-force, POSIX, Python, logi, skrypty
Źródło: https://en.wikipedia.org/wiki/Fail2ban (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak fail2ban blokuje adresy według Wikipedii?
Fail2ban może zbanować adres IP, który za dużo razy próbuje się zalogować lub wykonuje inne niechciane działania w ustalonym czasie. Bany są zwykle tymczasowe, ale dla powtarzających się sprawców można ustawić dłuższe. Blokowanie odbywa się przez reguły Netfilter/iptables lub PF albo wpisy w pliku hosts.deny TCP Wrapper. Może też wysłać e-mail lub uruchomić skrypt Pythona.

Słowa: fail2ban, ban, czas, iptables, PF, hosts.deny, e-mail, skrypt
Źródło: https://en.wikipedia.org/wiki/Fail2ban (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie pojęcia są w konfiguracji fail2ban według Wikipedii?
Filtry są definiowane wyrażeniami regularnymi Pythona, które wskazują złośliwe wpisy w logach. Akcje to reakcje na wykrytego sprawcę, np. aktualizacja zapory lub powiadomienie. Jail to połączenie filtra i akcji, które blokuje złośliwego hosta dostęp do wskazanych usług.

Słowa: fail2ban, filtr, akcja, jail, wyrażenia regularne, konfiguracja
Źródło: https://en.wikipedia.org/wiki/Fail2ban (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Ansible według Wikipedii?
Ansible to zestaw otwartych narzędzi do infrastruktury jako kodu, obejmujących provisioning, zarządzanie konfiguracją i wdrażanie aplikacji. Red Hat przejął go w 2015 roku, a może konfigurować systemy uniksowe i Windows.

Słowa: Ansible, infrastruktura jako kod, provisioning, konfiguracja, Red Hat, 2015, Windows
Źródło: https://en.wikipedia.org/wiki/Ansible_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czy Ansible wymaga agenta na zarządzanych maszynach według Wikipedii?
Nie. Ansible działa bez agentów, łączy się tymczasowo przez SSH, a dla Windows przez Windows Remote Management. Instaluje i uruchamia moduły tylko na czas zadania. Gdy nie zarządza maszyną, nie działają na niej żadne demony i nie zostają zainstalowane programy.

Słowa: Ansible, bez agenta, SSH, WinRM, moduły, demony, tymczasowo
Źródło: https://en.wikipedia.org/wiki/Ansible_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym są playbooki Ansible według Wikipedii?
Playbooki to pliki YAML z listami zadań do wielokrotnego wykonywania na zarządzanych maszynach. Każdy playbook przypisuje grupę hostów do zestawu ról.

Słowa: Ansible, playbook, YAML, zadania, hosty, role
Źródło: https://en.wikipedia.org/wiki/Ansible_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Co to jest idempotentność w Ansible według Wikipedii?
Strona podaje, że starannie napisany playbook może być idempotentny, co zapobiega nieoczekiwanym skutkom ubocznym na zarządzanych systemach. Playbooki mogą być też pisane nieidempotentnie. Moduły projektuje się tak, by powtarzane operacje zostawiały system w tym samym stanie.

Słowa: Ansible, idempotentność, playbook, skutki uboczne, moduły, ten sam stan
Źródło: https://en.wikipedia.org/wiki/Ansible_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Caddy według Wikipedii?
Caddy to rozszerzalny, wieloplatformowy, otwartoźródłowy serwer WWW napisany w Go. Jest znany z automatycznego HTTPS.

Słowa: Caddy, serwer WWW, Go, otwarte źródło, automatyczny HTTPS
Źródło: https://en.wikipedia.org/wiki/Caddy_(web_server) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak Caddy zarządza certyfikatami według Wikipedii?
Domyślnie, gdy trasa ma niepuste dopasowanie hosta, Caddy automatycznie uzyskuje i odnawia certyfikaty TLS dla skonfigurowanych nazw lub adresów IP i przekierowuje HTTP na HTTPS. Robi to protokołem ACME, dzięki czemu współpracuje z usługami takimi jak Let's Encrypt.

Słowa: Caddy, certyfikat TLS, ACME, Let's Encrypt, przekierowanie HTTP na HTTPS, automatyczne odnawianie
Źródło: https://en.wikipedia.org/wiki/Caddy_(web_server) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### W jakim formacie konfiguruje się Caddy według Wikipedii?
Natywny format to JSON, przyjmowany przez restowe API HTTP. Adaptery konfiguracji mogą zamieniać inne formaty, np. Caddyfile, YAML, TOML i Nginx, na JSON.

Słowa: Caddy, JSON, REST API, Caddyfile, adapter, YAML, TOML, Nginx
Źródło: https://en.wikipedia.org/wiki/Caddy_(web_server) (sprawdzono 2026-10, źródło angielskie, Wikipedia)
