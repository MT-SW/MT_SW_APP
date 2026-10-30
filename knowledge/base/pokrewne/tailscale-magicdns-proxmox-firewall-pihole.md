<!-- Baza wiedzy MT_SW: nazwy w Tailscale, zapora Proxmox i Pi-hole na domowym serwerze. Tylko informacyjnie. -->
# Homelab: MagicDNS w Tailscale, zapora Proxmox, Pi-hole

### Jak działają nazwy MagicDNS w Tailscale i jak wyłączyć je na jednym urządzeniu?
MagicDNS nadaje każdemu urządzeniu w sieci tailnet pełną nazwę domenową. Składa się ona z nazwy maszyny i nazwy tailnetu, np. monitoring.yak-bebop.ts.net, gdzie monitoring to nazwa maszyny, a yak-bebop.ts.net to tailnet. Dzięki automatycznym domenom wyszukiwania wystarczy wpisać samą nazwę maszyny. Zmiana nazwy maszyny zmienia też wpis MagicDNS. Dla tailnetów założonych od 20 października 2022 roku MagicDNS jest domyślnie włączony, a starsze można włączyć na stronie DNS w panelu administracyjnym. Gdy na jednym Linuksie coś nie działa z nazwami, można wyłączyć przyjmowanie ustawień DNS poleceniem tailscale set --accept-dns=false. Źródło nie podaje, czy to wyłączenie wpływa na inne urządzenia, więc sprawdź to na swoim serwerze.
Słowa: tailscale, magicdns, nazwy, tailnet, ts.net, domena, nazwa maszyny, dns, search domains, accept-dns, wyszukiwanie nazw, monitoring
Źródło: https://tailscale.com/docs/features/magicdns.md (sprawdzono 2026-10)

### Jak działa zapora Proxmox VE i czego nie zapomnieć przed jej włączeniem?
Zapora Proxmox VE dzieli ruch na strefy: Host (ruch do węzła lub przez niego przekazywany), VM (ruch do konkretnej maszyny lub kontenera) oraz VNet (ruch przez sieć SDN, tylko przekazywany). Konfiguracja leży w plikach: klastrowy /etc/pve/firewall/cluster.fw, węzłowy /etc/pve/nodes/nazwa_wezla/host.fw oraz dla maszyny /etc/pve/firewall/VMID.fw. Przed włączeniem zapory otwórz połączenie SSH do hosta, żeby mieć dostęp, gdyby coś poszło źle. Po włączeniu zapora blokuje ruch do hostów, z wyjątkami dla usług w sieci lokalnej. Trzeba więc dodać reguły dla zdalnych adresów IP do panelu WWW na porcie 8006 i ewentualnie SSH na porcie 22. Zestaw IPSet o nazwie management z tymi adresami tworzy potrzebne reguły. Źródło nie opisuje, jak działa zapora w starszych wersjach, więc zapisuję to dla obecnej dokumentacji.
Słowa: proxmox firewall, zapora, pve-firewall, strefy, host, vm, vnet, cluster.fw, host.fw, port 8006, ssh, ipset, management, blokada, reguły, lockout, dostęp zdalny
Źródło: https://pve.proxmox.com/wiki/Firewall (sprawdzono 2026-10)

### Czym jest Pi-hole i jak działa jako DNS sinkhole?
Pi-hole to program typu DNS sinkhole, który blokuje niechciane treści, np. reklamy, w całej sieci. Według dokumentacji nie wymaga instalowania oprogramowania na każdym urządzeniu. Blokuje też treści w aplikacjach mobilnych i telewizorach, które nie są przeglądarką, buforuje zapytania DNS i blokuje reklamy zarówno w IPv4, jak i IPv6. Ma interfejs wiersza poleceń i panel WWW. Dokumentacja nie opisuje, jak dokładnie odpowiada na zablokowane zapytania, ani jak wskazać urządzenia na Pi-hole. Wskazówka, że wystarczy wskazać adres Pi-hole jako DNS w routerze, jest z ogólnej wiedzy, nie ze źródła. Opcja serwera DHCP w Pi-hole może też objąć wszystkie urządzenia automatycznie.
Słowa: pi-hole, pihole, dns sinkhole, blokada reklam, dns, reklamy, trackery, serwer dns, dhcp, ipv6, raspberry pi, panel, filtr dns
Źródło: https://docs.pi-hole.net/ (sprawdzono 2026-10; oficjalna dokumentacja Pi-hole)
