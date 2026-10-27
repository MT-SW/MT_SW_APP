# SSH, Docker, MQTT, Home Assistant, Proxmox, Git (uzupełnienie)

### SSH: klucze, port 22, scp i tunele
Uwierzytelnianie kluczem: klucz prywatny nigdy nie idzie przez sieć, serwer sprawdza tylko, że klient go ma. Klucze publiczne trafiają do ~/.ssh/authorized_keys (SSH czyta ten plik, tylko jeśli nikt poza właścicielem i rootem nie może do niego pisać). Parę generuje ssh-keygen, klucz wskazuje opcja -i, a klucz prywatny warto zabezpieczyć hasłem. Port 22 (TCP) jest przypisany do SSH. SCP wywodzi się z RCP i działa przez SSH, SFTP to bezpieczna alternatywa dla FTP, a rsync zwykle też jedzie przez SSH i jest wydajniejszy od SCP. SSH przekazuje porty TCP i X11, a klienci SOCKS mogą go używać do szyfrowanego przeglądania; tunel SSH to nie to samo co VPN. Dobre praktyki: tylko SSH-2, weryfikuj klucze hosta przed zaakceptowaniem (rekord DNS SSHFP publikuje odciski), blokuj zapis do authorized_keys i aktualizuj OpenSSH do 9.6 lub nowszego (łatka na Terrapin, po obu stronach).
Słowa: SSH, ssh-keygen, authorized_keys, scp, sftp, rsync, tunel, port 22, klucz hosta, Terrapin
Źródło: https://en.wikipedia.org/wiki/Secure_Shell (sprawdzono 2026-10)

### Docker: obraz, kontener, wolumen, Compose, różnica od VM
Obraz to niezmienny szablon, z którego powstają kontenery. Kontener to izolowane środowisko z aplikacją, bibliotekami i konfiguracją. Wolumen daje trwałe dane niezależne od kontenera (przetrwają jego usunięcie). Docker Compose opisuje wiele usług w pliku docker-compose.yml i uruchamia je jednym poleceniem. W Dockerfile: FROM (obraz bazowy), COPY (pliki), ENV (zmienne), RUN (polecenia, np. apt-get update), VOLUME (punkt montowania), EXPOSE (dokumentuje port). W odróżnieniu od maszyn wirtualnych kontenery dzielą jądro systemu gospodarza, więc zużywają mniej zasobów; na Linuksie korzystają z przestrzeni nazw i cgroups, a na macOS Docker działa wewnątrz maszyny wirtualnej z Linuksem.
Słowa: Docker, kontener, obraz, Dockerfile, docker compose, wolumen, namespaces, cgroups, VM kontra kontener
Źródło: https://en.wikipedia.org/wiki/Docker_(software) (sprawdzono 2026-10)

### MQTT: broker, tematy, QoS, porty
Broker odbiera wiadomości od publikujących i kieruje je do subskrybentów; nadawcy i odbiorcy nie muszą się znać. Informacje są w hierarchii tematów, a każda wiadomość trafia do jednego tematu. QoS 0: najwyżej raz, bez potwierdzenia; QoS 1: co najmniej raz, z ponawianiem do potwierdzenia; QoS 2: dokładnie raz, dwustopniowe uzgodnienie. Wiadomość retained broker zapamiętuje (tylko ostatnią na temat) i wysyła nowym subskrybentom od razu. Kto się łączy, może ustawić wiadomość „ostatniej woli”, wysyłaną, gdy rozłączy się nieoczekiwanie. Porty: 1883 bez szyfrowania, 8883 szyfrowany. MQTT przesyła dane logowania jawnym tekstem i sam nie ma zabezpieczeń; chroń go TLS, certyfikatami, loginem i hasłem. Znaków wieloznacznych + i # źródło nie opisuje.
Słowa: MQTT, broker, temat, QoS, retained, last will, port 1883, port 8883, TLS, Mosquitto
Źródło: https://en.wikipedia.org/wiki/MQTT (sprawdzono 2026-10)

### Home Assistant: instalacja, integracje, automatyzacje
Home Assistant to wolne oprogramowanie do centralnej automatyki domowej, uruchomione przez Paulusa Schoutsena w 2013, od 2024 pod Open Home Foundation. Instalacje: Home Assistant OS (najpopularniejsza, własny system z supervisorem do kopii i aktualizacji, na SBC, mini PC, w VM lub NAS), Container (Linux, macOS, Windows przez WSL; bez dodatków), gotowe urządzenia (Blue, Yellow, Green). Integracje: Bluetooth, Zigbee, Z-Wave, Matter/Thread, Wi-Fi, Ethernet oraz inne ekosystemy przez API lub MQTT (Alexa, Google Nest, HomeKit, SmartThings, Hue). Urządzenia są encjami. Automatyzacje składają się z wyzwalaczy, warunków i akcji, od grudnia 2020 można importować blueprinty, a YAML pozostaje dostępny. Dodatki w HAOS działają jak usługi obok. Dane są lokalne, zdalny dostęp domyślnie wyłączony (opcjonalnie płatny Nabu Casa); zabezpiecz konta dwuskładnikowo.
Słowa: Home Assistant, HAOS, automatyzacja, blueprint, encje, dodatki, Nabu Casa, Zigbee, Matter, MQTT
Źródło: https://en.wikipedia.org/wiki/Home_Assistant (sprawdzono 2026-10)

### Proxmox VE: maszyny KVM kontra kontenery LXC
Maszyny wirtualne KVM dają pełną wirtualizację dowolnego systemu x86 i wymagają AMD-V lub Intel VT. Kontenery LXC (zastąpiły OpenVZ od wersji 4.0) zużywają mniej pamięci, ale gości muszą być Linuksami, bo dzielą jądro. Jeden interfejs www zarządza obiema rzeczami. Dodatkowo: firewall oparty na iptables, konsola SPICE lub noVNC, przekazywanie PCIe i GPU (od 5.3, wymaga VT-d lub AMD-Vi), logowanie przez PAM, OIDC, LDAP, AD z MFA (TOTP, WebAuthn, YubiKey), sieci programowe (od 8.1), magazyny LVM, ZFS, Ceph, NFS, iSCSI. Kopie robi vzdump, a Proxmox Backup Server dodaje przyrostowe kopie z szyfrowaniem i weryfikacją. Klastry z migracją na żywo i HA oparte na Corosync. Portu interfejsu www źródło nie podaje.
Słowa: Proxmox, KVM, LXC, kontener, maszyna wirtualna, vzdump, ZFS, klaster, passthrough, homelab
Źródło: https://en.wikipedia.org/wiki/Proxmox_Virtual_Environment (sprawdzono 2026-10)

### Git: pojęcia i dobre praktyki
Commit to migawka drzewa projektu z czasem, opisem i wskazaniem na rodziców, identyfikowana skrótem SHA-1. Gałąź to tylko wskaźnik na jeden commit, więc jest lekka. HEAD to odniesienie, względem którego tworzy się commit. Tag to stały wskaźnik na commit. Merge łączy gałęzie (domyślnie strategia recursive). Remote to repozytorium współdzielące historię, domyślnie origin. Polecenia: git init, clone, add, commit -m, show-ref, gc, fsck. Rady: .gitignore dla kluczy, haseł i dużych plików; wypchnięte commity cofaj przez revert, a nie nadpisywanie; rename commituj osobno od zmian treści. Pull requesty należą do serwisów (GitHub, GitLab), nie do Gita, a Git nie zapewnia kontroli dostępu. GitHub i GitLab tworzą domyślnie gałąź main. O rebase źródło nie mówi.
Słowa: git, commit, branch, merge, tag, HEAD, origin, .gitignore, revert, pull request
Źródło: https://en.wikipedia.org/wiki/Git (sprawdzono 2026-10)
