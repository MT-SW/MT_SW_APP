# IP, NAT, TLS, zapora, systemd, cron i Proxmox

### Adresy IP: IPv4, IPv6, prywatne zakresy, maska
IPv4 to 32 bity zapisywane dziesiętnie (192.0.2.1), pula się wyczerpała w latach 2010; IPv6 ma 128 bitów w zapisie szesnastkowym i już przekroczył 50% ruchu na świecie. Zakresy prywatne IPv4: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16 (nieroutowane w internecie), w IPv6 adresy lokalne unikalne fc00::/7; adresy link-local to 169.254.0.0/16 i fe80::/10. Maska 255.255.255.0 to /24: 24 bity sieci i 8 bitów hostów. DHCP przydziela adresy dzierżawą i potrafi zawsze dawać ten sam adres urządzeniu o danym MAC; routery i serwery zwykle mają adresy stałe. NAT pozwala wielu prywatnym urządzeniom dzielić jeden publiczny adres przez mapowanie portów. NAT operatora (CGNAT) źródło pomija.
Słowa: adres IP, IPv4, IPv6, 192.168, maska podsieci, /24, DHCP, NAT, adres prywatny, CIDR, sieć lokalna
Źródło: https://en.wikipedia.org/wiki/IP_address (sprawdzono 2026-10)

### Przekierowanie portów i alternatywy
Port forwarding to technika NAT, w której router przekierowuje ruch z jednego adresu i portu na inny, zwykle by udostępnić z internetu usługę z sieci prywatnej. W Linuksie służą do tego reguły DNAT (iptables/netfilter), a UPnP (IGD) potrafi robić to automatycznie. Ryzyko: wystawiasz usługę (WWW, SSH, FTP, gry) do internetu, a zwykle tylko jeden host wewnętrzny może używać danego portu. Alternatywy: tunel SSH (lokalny, zdalny/odwrotny, dynamiczny SOCKS), proxy aplikacyjne, a w praktyce VPN (np. WireGuard) bez otwierania portów. Dla brokera MQTT lepiej udostępniać dostęp przez VPN niż otwierać port 1883.
Słowa: port forwarding, przekierowanie portów, NAT, router, UPnP, tunel SSH, DNAT, DMZ, wystawianie usługi, dostęp z zewnątrz
Źródło: https://en.wikipedia.org/wiki/Port_forwarding (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### TLS i certyfikaty: co chroni HTTPS?
TLS zapewnia poufność, integralność i uwierzytelnienie połączenia; najbardziej znany w HTTPS (port 443; HTTP bez szyfrowania na 80). Certyfikat wiąże klucz publiczny z właścicielem i jest poświadczony przez urząd certyfikacji (CA), któremu ufa system lub przeglądarka. Wersje: SSL 2.0 i 3.0 wycofane (2011 i 2015), TLS 1.0 i 1.1 wycofane w 2021, TLS 1.2 nadal dozwolony, a TLS 1.3 (2018) jest aktualny, wymusza utajnienie z wyprzedzeniem; we wrześniu 2025 wspierało go 75,3% badanych stron. Źródło nie omawia Let's Encrypt ani certyfikatów samopodpisanych.
Słowa: TLS, SSL, HTTPS, certyfikat, CA, port 443, TLS 1.3, szyfrowanie połączenia, certyfikat samopodpisany, Let's Encrypt
Źródło: https://en.wikipedia.org/wiki/Transport_Layer_Security (sprawdzono 2026-10)

### Zapora sieciowa (firewall): rodzaje i zasady
Zapora sieciowa stoi między sieciami (np. LAN i WAN) albo działa na samym komputerze (hostowa). Filtr pakietów analizuje pojedyncze pakiety po adresie, porcie i protokole, zapory stanowe śledzą rozmowy, warstwy aplikacji rozumieją konkretne protokoły, a nowej generacji dodają filtrowanie WWW i inspekcję TLS. Typowa polityka: ruch z LAN do WAN wolno, z WAN do LAN odrzucaj; ruch bez pasującej reguły jest po cichu odrzucany. Porty: HTTP/HTTPS 80 i 443, FTP 21, SMTP 25. Zapory domowych użytkowników źródło nie opisuje. W praktyce domowy router pełni funkcję zapory dla NAT, a przekierowanie portów ją omija dla wskazanej usługi.
Słowa: firewall, zapora, filtr pakietów, porty, iptables, ufw, WAN, LAN, reguły, polityka domyślna
Źródło: https://en.wikipedia.org/wiki/Firewall_(computing) (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### systemd: jednostki, usługi i dzienniki
systemd zarządza demonami: jest pierwszym procesem przy starcie i ostatnim przy zamykaniu. Instrukcje startowe trzymają pliki jednostek (.service, .socket, .mount, .target, .timer i inne), zapisane w deklaratywnym formacie podobnym do .ini. Jednostki .timer mogą zastąpić cron. Procesy śledzi przez cgroups, więc demon nie ucieknie spod nadzoru. systemctl steruje stanem usług, a dzienniki zbiera systemd-journald (binarne pliki; można użyć też rsyslog). Z dokumentacji meshtasticd: sudo systemctl daemon-reload, enable, start, status, restart, a logi poleceniem journalctl -u nazwa_usługi -b.
Słowa: systemd, systemctl, journalctl, unit, usługa, timer, daemon-reload, enable, Linux, autostart
Źródło: https://en.wikipedia.org/wiki/Systemd (sprawdzono 2026-10; polecenia: dokumentacja meshtasticd)

### Cron: harmonogram zadań
Cron uruchamia zadania według czasu; harmonogram trzymają pliki crontab. Linia ma pięć pól czasu i polecenie: minuta (0–59), godzina (0–23), dzień miesiąca (1–31), miesiąc (1–12), dzień tygodnia (0–6, niedziela=0). Znaki: * wszystkie, przecinek lista, myślnik zakres, ukośnik krok; makra @hourly, @daily, @weekly, @monthly, @yearly, @reboot. Przykład: 45 23 * * 6 /home/user/skrypt.sh to sobota 23:45. Pułapki: gdy ograniczone są oba dni (miesiąca i tygodnia), zadanie uruchomi się przy dopasowaniu któregokolwiek; znak % w poleceniu trzeba poprzedzić backslashem; zadania idą wg strefy systemowej, a kroki działają równo tylko, gdy dzielą zakres bez reszty.
Słowa: cron, crontab, harmonogram, zadanie cykliczne, @reboot, składnia cron, minuta godzina, backup nocny, Linux
Źródło: https://en.wikipedia.org/wiki/Cron (sprawdzono 2026-10)

### Proxmox VE: maszyny wirtualne i kontenery
Proxmox VE to otwarta platforma do zarządzania maszynami wirtualnymi i kontenerami, oparta na Debianie. KVM daje pełną wirtualizację (procesor z AMD-V lub Intel VT), LXC to kontenery Linuksa, lżejsze, ale gość musi być Linuksem. Jeden interfejs WWW obsługuje oba (jest też aplikacja mobilna). Klastry z migracją na żywo i menedżerem HA, zapora na bazie iptables, SDN od wersji 8.1, logowanie wewnętrzne, PAM, OIDC, LDAP, AD z MFA. Możliwe przekazywanie urządzeń PCIe i GPU. Pamięć: dyski lokalne, NFS, iSCSI, LVM/qcow2, ZFS, Ceph. Kopie: vzdump oraz Proxmox Backup Server z przyrostami, szyfrowaniem i weryfikacją.
Słowa: Proxmox, KVM, LXC, wirtualizacja, homelab, maszyna wirtualna, kontener, ZFS, Proxmox Backup Server, passthrough
Źródło: https://en.wikipedia.org/wiki/Proxmox_Virtual_Environment (sprawdzono 2026-10)
