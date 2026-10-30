<!-- Baza wiedzy MT_SW: tailscale serve, czyli udostępnianie lokalnej usługi w tailnecie. Tylko informacyjnie. -->
# Tailscale Serve: udostępnianie usługi w tailnecie

### Czym różni się tailscale serve od tailscale funnel?
Polecenie tailscale serve udostępnia lokalną usługę bezpiecznie w obrębie tailnetu. Działa tylko dla urządzeń w Twojej sieci Tailscale. Jeśli chcesz wystawić usługę publicznie dla całego internetu, dokumentacja wskazuje tailscale funnel. Dla usług domowych, z których korzystasz tylko z własnych urządzeń, serve jest zwykle właściwym wyborem, bo nikt z zewnątrz nie ma do nich dostępu.
Słowa: tailscale serve, funnel, tailnet, publiczny dostęp, udostępnianie lokalne, prywatna usługa, internet, dla całego internetu, serve vs funnel
Źródło: https://tailscale.com/docs/reference/tailscale-cli/serve.md (sprawdzono 2026-10; oficjalna dokumentacja Tailscale)

### Jakie flagi ma tailscale serve: --https, --http i --bg?
Domyślnie tailscale serve działa po HTTPS na podanym porcie. Tailscale sam wystawia certyfikat TLS, a demon na urządzeniu kończy połączenie. Flaga --http wystawia zwykłe HTTP, a takie serwery są dostępne pod krótkimi nazwami MagicDNS. Flaga --bg uruchamia Serve jako proces w tle. Działa po restartach i cyklach tailscale down i up, aż wyłączysz usługę. Bez --bg po restarcie trzeba uruchomić Serve ponownie. Przykład z dokumentacji to tailscale serve --https=443 localhost:3000, który przekierowuje lokalną aplikację na port 3000 do tailnetu przez HTTPS.
Słowa: tailscale serve https, --https, --http, --bg, tło, certyfikat tailscale, magicdns, localhost 3000, restart serve, port 443, zwykłe http, tailscale http
Źródło: https://tailscale.com/docs/reference/tailscale-cli/serve.md (sprawdzono 2026-10; oficjalna dokumentacja Tailscale)

### Jak wyłączyć udostępnianie Tailscale Serve i co zrobić, gdy działa w tle?
Serve uruchomiony w tle nie zatrzymuje się sam. Aby go wyłączyć, dopisz off do pierwotnego polecenia, zachowując jego flagi. Polecenie tailscale serve reset czyści całą konfigurację Serve. Serwowanie plików i katalogów, np. tailscale serve /ścieżka, działa tylko z wariantem open source Tailscale na macOS, bo wersja z App Store i Standalone pozwala udostępniać tylko porty. Wtedy pliki udostępnisz tylko na Linuksie lub Windows albo wariantem open source na macOS.
Słowa: tailscale serve off, serve reset, wyłączenie serve, serve w tle, macos app store, standalone, udostępnianie katalogu, pliki w tailnecie, serve pliki
Źródło: https://tailscale.com/docs/reference/examples/serve.md (sprawdzono 2026-10; oficjalna dokumentacja Tailscale; polecenie reset z dokumentacji CLI serve)

### Jak przez tailscale serve udostępnić SSH albo inną usługę TCP i czym jest ścieżka --set-path?
Flaga --tcp=port tworzy surowy przekaźnik TCP, przydatny dla protokołów takich jak SSH albo RDP. Wariant --tls-terminated-tcp działa podobnie dla pakietów TLS zakończonych na serwerze. Flaga --set-path=ścieżka dopisuje ścieżkę URL do adresu bazowego, więc usługa jest dostępna pod podścieżką, np. /foo. Te funkcje wymagają świadomego wyboru portu, bo każdy udostępniony port jest widoczny dla całego tailnetu, a zasady dostępu określa się w grants.
Słowa: tailscale tcp, --tcp, ssh przez tailscale, rdp, set-path, podścieżka, tls-terminated-tcp, udostępnianie portu, grants, dostęp do portu, forwarder tcp
Źródło: https://tailscale.com/docs/reference/tailscale-cli/serve.md (sprawdzono 2026-10; oficjalna dokumentacja Tailscale; zasada grants jako wniosek z ogólnej wiedzy)
