<!-- Baza wiedzy MT_SW: typy magazynów w Proxmox VE i domyślne wpisy po instalacji. Tylko informacyjnie. -->
# Proxmox VE: typy magazynów

### Które typy magazynów w Proxmox VE są współdzielone i które obsługują migawki?
Tabela w oficjalnej wiki Proxmox podaje, że ZFS lokalny (zfspool) nie jest współdzielony, ale ma migawki. Katalog (dir) też nie jest współdzielony, a migawki ma tylko w formacie qcow2. LVM-thin nie jest współdzielony, ale migawki ma. NFS i CIFS są współdzielone, a migawki w qcow2. Proxmox Backup (pbs) jest współdzielony, a dla niego migawki nie dotyczą. Ceph/RBD i CephFS są współdzielone z migawkami. Zwykły LVM bez thin jest nieudostępniany, chyba że leży na LUN-ach iSCSI, FC lub SAS. Dla domowego serwera z jednym hostem istotne jest, że magazyny lokalne nie przenoszą się same między węzłami. Dane lokalne a współdzielone to rozróżnienie z oficjalnej tabeli, a wnioski dla domu są z ogólnej wiedzy.
Słowa: proxmox magazyn, storage, typy magazynów, zfspool, dir, lvm, lvmthin, nfs, cifs, ceph, rbd, iscsi, pbs, współdzielony, shared, migawki, snapshot, qcow2, lokalny
Źródło: https://pve.proxmox.com/wiki/Storage (sprawdzono 2026-10; oficjalna wiki Proxmox VE; wnioski dla domu z ogólnej wiedzy)

### Jakie magazyny tworzy Proxmox VE przy instalacji i do czego służą?
Przy instalacji Proxmox tworzy domyślne wpisy w pliku /etc/pve/storage.cfg. Katalog o nazwie local w /var/lib/vz służy do ISO, szablonów kontenerów i kopii. Jeśli instalacja była na LVM, powstaje local-lvm, czyli pula thin o nazwie data w grupie woluminów pve, na dyski maszyn i katalogi główne kontenerów. Jeśli instalacja była na ZFS, powstaje local-zfs, czyli pula rpool/data na te same dane. Wniosek, że nie mieszaj typów w tej samej roli, to wskazówka z ogólnej wiedzy. Wiki zaznacza, że aliasowane wpisy na tym samym magazynie mogą dać dwa identyfikatory tego samego obrazu, co Proxmox uważa za błąd.
Słowa: local, local-lvm, local-zfs, storage.cfg, /var/lib/vz, rpool/data, pool data, pve, instalacja proxmox, dysk maszyny, szablony kontenerów, iso, aliasy magazynu
Źródło: https://pve.proxmox.com/wiki/Storage (sprawdzono 2026-10; oficjalna wiki Proxmox VE; wniosek o niemieszaniu typów z ogólnej wiedzy)
