<!-- Baza wiedzy MT_SW: limit pamięci ARC dla ZFS w Proxmox VE (zfs_arc_max). Tylko informacyjnie. -->
# Proxmox VE: limit pamięci ZFS (ARC)

### Ile pamięci ZFS zajmuje w Proxmox VE domyślnie i jaki limit ustawić?
W nowych instalacjach od Proxmox VE 8.1 ZFS dostaje limit ARC równy 10% pamięci hosta, ale nie więcej niż 16 GiB. Instalator zapisuje to w pliku /etc/modprobe.d/zfs.conf. Starsze instalacje nie mają tego limitu i używają wbudowanego domyślnego ustawienia ZFS, czyli 62,5% pamięci w ZFS 2.3.0 i nowszym, a 50% w starszych wersjach. Wiki Proxmox zaleca przydzielić co najmniej 2 GiB plus 1 GiB na każdy TiB pojemności. Przykład: pula 8 TiB potrzebuje około 10 GiB na ARC. ZFS narzuca minimum 64 MiB. Przy zmianie limitu trzeba uważać na wydajność operacji we/wy, bo zbyt mała pamięć ARC ją pogarsza.
Słowa: zfs arc, arc, limit pamięci zfs, zfs_arc_max, zfs.conf, modprobe, 10 procent, 16 gib, 62,5 procent, 50 procent, pamięć proxmox, zfs ram, 2 gib plus 1 gib na tib
Źródło: https://pve.proxmox.com/wiki/ZFS_on_Linux (sprawdzono 2026-10; oficjalna wiki Proxmox VE)

### Jak tymczasowo i trwale zmienić limit ARC w Proxmox VE?
Wpis tymczasowy działa do restartu: zapisuje się wartość w bajtach do /sys/module/zfs/parameters/zfs_arc_max. Trwały wpis to linia options zfs zfs_arc_max=wartość w /etc/modprobe.d/zfs.conf, na przykład 8 GiB to 8589934592. Przy ZFS na korzeniu systemu trzeba po zmianie uruchomić update-initramfs -u -k all i zrestartować system. Gdy wybrany limit jest równy lub mniejszy od zfs_arc_min (domyślnie 1/32 pamięci systemu), trzeba też ustawić zfs_arc_min na co najwyżej zfs_arc_max minus 1, inaczej limit nie zadziała. Wartości w bajtach łatwo pomylić, więc sprawdź wynik przed restartem.
Słowa: zfs_arc_max, zfs_arc_min, bajty, update-initramfs, restart, modprobe, /sys/module/zfs, 8589934592, limit trwały, limit tymczasowy, zfs na korzeniu, proxmox zfs ustawienia
Źródło: https://pve.proxmox.com/wiki/ZFS_on_Linux (sprawdzono 2026-10; oficjalna wiki Proxmox VE)
