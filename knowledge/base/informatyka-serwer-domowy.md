# Serwer domowy: SSH, Home Assistant, Proxmox, Git

### SSH: jak bezpiecznie łączyć się z serwerem?
SSH domyślnie działa na porcie TCP 22. Logowanie kluczem: polecenie ssh-keygen tworzy parę kluczy; publiczny kładziesz na serwerze w ~/.ssh/authorized_keys, prywatny zostaje u ciebie i nigdy nie jest wysyłany. Klucz prywatny zabezpiecz hasłem. Inny klucz wskażesz opcją -i. SSH ignoruje authorized_keys, jeśli plik może zapisywać ktoś poza właścicielem i rootem. Przy pierwszym połączeniu sprawdź odcisk klucza serwera, bo akceptacja klucza napastnika wpuszcza go jako zaufanego. Pliki kopiujesz przez scp, sftp lub rsync. SSH umie tunelować porty (dostęp do usługi za zaporą bez wystawiania jej do internetu). Aktualizuj oprogramowanie i nie używaj przestarzałych szyfrów.
Słowa: SSH, port 22, klucz, ssh-keygen, authorized_keys, hasło do klucza, scp, sftp, rsync, tunel, przekierowanie portu, logowanie bez hasła
Źródło: https://en.wikipedia.org/wiki/Secure_Shell (sprawdzono 2026-10)

### Home Assistant: co to jest i jak go zainstalować?
Darmowe, otwarte oprogramowanie (licencja Apache 2.0) do automatyki domowej, działające lokalnie, bez chmury. Od 2024 należy do fundacji Open Home Foundation. Instalacje: Home Assistant OS (najpopularniejsza, gotowy system na mini-komputer, maszynę wirtualną lub NAS, z menedżerem kopii zapasowych, aktualizacji i dodatków) oraz Container (w Dockerze, bez dodatków). Obsługuje Zigbee, Z-Wave, Thread, Matter, Bluetooth, Wi-Fi i MQTT. Dodatki (add-ons) to aplikacje działające obok, np. broker MQTT. Zdalny dostęp domyślnie jest wyłączony; płatna chmura Nabu Casa go zapewnia. Sprzęt: Home Assistant Green (Ethernet, 2× USB, bez radia) i Yellow (moduł Raspberry Pi CM4, radio Zigbee/Thread).
Słowa: Home Assistant, HAOS, Container, add-on, dodatek, MQTT, Zigbee, automatyka domowa, Nabu Casa, Green, Yellow, lokalnie
Źródło: https://en.wikipedia.org/wiki/Home_Assistant (sprawdzono 2026-10)

### Proxmox: maszyny wirtualne i kontenery LXC
Proxmox VE to platforma wirtualizacji z panelem WWW (licencja AGPL v3). Maszyny KVM uruchamiają praktycznie każdy system x86 bez zmian i wymagają wsparcia sprzętowego (Intel VT lub AMD-V). Kontenery LXC zużywają mniej pamięci, ale tylko z Linuksem, bo dzielą jądro z gospodarzem. System plików ext4 lub ZFS (ZFS bardziej wymagający). Kopie zapasowe: wbudowane vzdump albo osobny Proxmox Backup Server (kopie przyrostowe, szyfrowanie, weryfikacja). Węzły można łączyć w klaster z migracją na żywo. Przekazanie sprzętu PCIe do maszyny wymaga włączonego VT-d/AMD-Vi w BIOS-ie.
Słowa: Proxmox, PVE, maszyna wirtualna, VM, KVM, LXC, kontener, ZFS, kopia zapasowa, backup, klaster, passthrough, VT-d
Źródło: https://en.wikipedia.org/wiki/Proxmox_Virtual_Environment (sprawdzono 2026-10)

### Git: podstawowe pojęcia i polecenia
Repozytorium to pełna lokalna kopia historii projektu; Git działa bez sieci. Commit to niezmienna migawka z opisem, rozpoznawana po skrócie (hash). Gałąź (branch) to wskaźnik na commit, przesuwający się z nowymi commitami; domyślnie master lub main. Merge łączy zmiany z innej gałęzi, a konflikty trzeba poprawić ręcznie. Remote to odnośnik do innego repozytorium, domyślnie "origin". Polecenia: git clone (kopiuje repozytorium), git add (dodaje zmiany do commita), git commit -m "opis" (zapisuje), git push (wysyła), git pull (pobiera i łączy). Opublikowane commity zwykle cofa się nowym commitem odwracającym, a nie nadpisywaniem historii.
Słowa: git, repozytorium, commit, gałąź, branch, merge, konflikt, push, pull, clone, origin, main, GitHub
Źródło: https://en.wikipedia.org/wiki/Git (sprawdzono 2026-10)
