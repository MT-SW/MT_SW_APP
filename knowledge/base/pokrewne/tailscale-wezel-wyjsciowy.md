<!-- Baza wiedzy MT_SW: węzeł wyjściowy (exit node) w Tailscale. Tylko informacyjnie. -->
# Tailscale: węzeł wyjściowy (exit node)

### Jak ustawić urządzenie w tailnecie jako węzeł wyjściowy, przez który idzie cały ruch?
Węzeł wyjściowy (exit node) to urządzenie w tailnecie, przez które kierujesz cały ruch internetowy innego urządzenia. Wymagania: działające tailnet, Tailscale w wersji 1.20 lub nowszej na węźle i na urządzeniach, które z niego korzystają, oraz system Linux, macOS, Windows, Android, iOS lub tvOS. Konfiguracja ma pięć kroków: zainstaluj klienta Tailscale, ogłoś urządzenie jako węzeł wyjściowy (na Androidzie w aplikacji wybierz Exit Node i Run as exit node), zatwierdź je w panelu administracyjnym, chyba że działa reguła autoApprovers, nadaj dostęp użytkownikom, a potem włącz węzeł osobno na każdym urządzeniu klienckim. Zatwierdzać może tylko administrator. Domyślnie każdy użytkownik może korzystać z węzła, ale przy własnej polityce dostępu trzeba dodać grant lub ACL z celem autogroup:internet, inaczej ruch do internetu nie przejdzie.
Słowa: tailscale, exit node, węzeł wyjściowy, brama, cały ruch, ruch internetowy, vpn, autogroup:internet, grant, acl, autoApprovers, advertise-exit-node, android, run as exit node
Źródło: https://tailscale.com/docs/features/exit-nodes (sprawdzono 2026-10; oficjalna dokumentacja Tailscale)

### Jakie ograniczenia ma Android jako węzeł wyjściowy Tailscale?
Dokumentacja Tailscale zaznacza, że na Androidzie węzeł wyjściowy korzysta z routingu w przestrzeni użytkownika (userspace), który jest mniej dojrzały niż implementacja na Linuksie. Przy dłuższym użyciu trzeba trzymać telefon podłączony do zasilania, a wydajność może być za mała do większości zastosowań. Źródło nie podaje konkretnych liczb przepustowości, więc nie podaję ich tutaj. Jeśli węzeł ma działać stale, lepszym wyborem jest urządzenie z Linuksem, a telefon zostaw do dostępu awaryjnego. Ta rada to wniosek z ogólnej wiedzy, nie ze źródła.
Słowa: tailscale android, exit node android, userspace, wydajność, zasilanie telefonu, ograniczenia, węzeł na telefonie, routing, przepustowość, linux exit node
Źródło: https://tailscale.com/docs/features/exit-nodes (sprawdzono 2026-10; oficjalna dokumentacja Tailscale)
