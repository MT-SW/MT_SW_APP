### Czym jest vzdump w Proxmox VE?
Vzdump to narzędzie Proxmox VE do tworzenia kopii zapasowych gości, czyli maszyn wirtualnych i kontenerów. Można je uruchomić z interfejsu graficznego albo z linii poleceń `vzdump`. Strona podaje, że kopie w Proxmox VE są zawsze pełne i zawierają konfigurację gościa oraz wszystkie jego dane.

Słowa: proxmox, vzdump, kopia zapasowa, pełna kopia, gość, VM, kontener
Źródło: https://pve.proxmox.com/wiki/Backup_and_Restore (sprawdzono 2026-10)

### Jak działa tryb snapshot w vzdump dla działającej VM?
W trybie snapshot maszyna działa dalej, a bloki danych są kopiowane na żywo. Przestoje są najmniejsze, ale istnieje małe ryzyko niespójności. Jeśli agent gościa jest włączony i działa, zamraża on system plików na czas kopii, co poprawia spójność.

Słowa: proxmox, vzdump, snapshot, na żywo, agent gościa, zamrażanie, spójność
Źródło: https://pve.proxmox.com/wiki/Backup_and_Restore (sprawdzono 2026-10)

### Czym jest tryb suspend w vzdump i kiedy jest odradzany?
Suspend jest opisany jako tryb zapewniający zgodność. Dla VM wstrzymuje maszynę przed uruchomieniem trybu snapshot, co wydłuża przestój i nie zawsze poprawia spójność, więc strona zaleca snapshot. Dla kontenerów kopiowanie idzie rsyncem przy działającym kontenerze, potem następuje krótkie wstrzymanie na drugi, przyrostowy rsync. Potrzebna jest dodatkowa przestrzeń na tymczasową kopię.

Słowa: proxmox, vzdump, suspend, rsync, kontener, przestój, przyrostowy, przestrzeń
Źródło: https://pve.proxmox.com/wiki/Backup_and_Restore (sprawdzono 2026-10)

### Czym jest tryb stop w vzdump i jaki daje skutek?
Tryb stop wyłącza maszynę w uporządkowany sposób, wykonuje kopię i potem przywraca działanie, jeśli maszyna była włączona. Daje najwyższą spójność kosztem krótkiego przestoju. Dla kontenerów zatrzymuje kontener na cały czas kopii, co może oznaczać długi przestój.

Słowa: proxmox, vzdump, stop, spójność, przestój, kontener, uporządkowane wyłączenie
Źródło: https://pve.proxmox.com/wiki/Backup_and_Restore (sprawdzono 2026-10)

### Jaki tryb kopii vzdump jest domyślny w Proxmox VE?
Opcja `mode` ma domyślnie wartość `snapshot`.

Słowa: proxmox, vzdump, mode, domyślny, snapshot
Źródło: https://pve.proxmox.com/wiki/Backup_and_Restore (sprawdzono 2026-10)
