<!-- Baza wiedzy MT_SW: konfiguracja routera podsieci (subnet router) w Tailscale krok po kroku. Tylko informacyjnie. -->
# Tailscale: router podsieci krok po kroku

### Jakie kroki wykonać, żeby Linux działał jako router podsieci w Tailscale?
Najpierw instaluje się klienta Tailscale na urządzeniu bramowym. Potem włącza się przekazywanie IP: w pliku sysctl ustawia się net.ipv4.ip_forward = 1 i analogicznie IPv6, a zapora zostaje w trybie blokowania przekazywania domyślnie. Na systemach z firewalld może być potrzebne polecenie firewall-cmd --permanent --add-masquerade. Następnie ogłaszasz trasy poleceniem sudo tailscale set --advertise-routes= z listą podsieci oddzieloną przecinkami. Trasy można zatwierdzić automatycznie przez autoApprovers albo ręcznie w panelu administracyjnym na stronie Machines. Osobno trzeba dodać reguły dostępu w pliku polityk, bo zatwierdzenie trasy i prawo dostępu to dwie różne kontrole.
Słowa: subnet router, router podsieci, advertise-routes, ip_forward, sysctl, firewalld, masquerade, zatwierdzanie tras, machines, subnets, podsieć domowa, tailscale brama
Źródło: https://tailscale.com/docs/features/subnet-routers (sprawdzono 2026-10; oficjalna dokumentacja Tailscale)

### Dlaczego na Linuksie trzeba dodatkowo uruchomić accept-routes, żeby korzystać z tras innych urządzeń?
Dokumentacja Tailscale podaje, że Android, iOS, macOS, tvOS i Windows wykrywają nowe trasy automatycznie. Urządzenia z Linuksem tego nie robią i trzeba uruchomić sudo tailscale set --accept-routes. Bez tego klient Linux nie będzie kierował ruchu do podsieci ogłoszonych przez router. Ta różnica jest częstą przyczyną problemów przy pierwszej konfiguracji, więc sprawdź ją przed diagnozowaniem reszty sieci.
Słowa: accept-routes, trasy na linuksie, klient linux nie widzi podsieci, tailscale set accept-routes, ruch do podsieci, brak trasy, problem z trasą, podsieć niedostępna
Źródło: https://tailscale.com/docs/features/subnet-routers (sprawdzono 2026-10; oficjalna dokumentacja Tailscale)
