### Czym jest nieuprzywilejowany kontener w Proxmox VE?
Przy tworzeniu kontenera wybiera się, czy będzie uprzywilejowany, czy nieuprzywilejowany. Nieuprzywilejowany używa przestrzeni nazw użytkowników, więc root (UID 0) w kontenerze odpowiada nieuprzywilejowanemu użytkownikowi na hoście. Strona podaje, że nieuprzywilejowany jest domyślny dla nowych kontenerów.

Słowa: proxmox, kontener, nieuprzywilejowany, uprzywilejowany, UID 0, przestrzeń nazw, root
Źródło: https://pve.proxmox.com/wiki/Linux_Container (sprawdzono 2026-10)

### Dlaczego kontenery Proxmox VE współdzielą jądro z hostem?
Kontenery nie mają własnego jądra systemu, tylko korzystają z jądra hosta. Strona zwraca uwagę, że to tworzy powierzchnię ataku, więc dla niezaufanych użytkowników pełne maszyny wirtualne dają lepszą izolację.

Słowa: proxmox, kontener, jądro hosta, powierzchnia ataku, izolacja, maszyna wirtualna
Źródło: https://pve.proxmox.com/wiki/Linux_Container (sprawdzono 2026-10)

### Jakie szablony kontenerów poleca Proxmox VE?
Oficjalne szablony systemowe Proxmox VE pobiera się z interfejsu albo narzędziem `pveam`. Dostępne są też szablony TurnKey Linux, a obrazy OCI są w fazie technology preview.

Słowa: proxmox, szablony, pveam, TurnKey Linux, OCI, technology preview
Źródło: https://pve.proxmox.com/wiki/Linux_Container (sprawdzono 2026-10)

### Jakie magazyny można użyć dla kontenerów w Proxmox VE?
Można użyć dowolnego typu obsługiwanego przez bibliotekę storage Proxmox VE, np. lokalnego LVM, ZFS, katalogu, współdzielonych iSCSI lub NFS czy Ceph. W klastrach strona preferuje współdzielone storage dla szablonów.

Słowa: proxmox, storage, LVM, ZFS, iSCSI, NFS, Ceph, klaster, szablony
Źródło: https://pve.proxmox.com/wiki/Linux_Container (sprawdzono 2026-10)
