<!-- Baza wiedzy MT_SW: migawki (snapshots) maszyn w Proxmox VE, wymagania magazynu i relacja do kopii zapasowej. Tylko informacyjnie. -->
# Proxmox VE: migawki maszyn

### Jakie wymagania muszą być spełnione, żeby zrobić migawkę na żywo w Proxmox VE?
Migawka na żywo zachowuje stan maszyny wirtualnej KVM. Wszystkie dyski maszyny muszą być w formacie qcow2 albo leżeć na magazynie obsługującym migawki na żywo i zarządzanym przez Proxmox VE. Listę takich magazynów podaje dokumentacja magazynów Proxmox. Migawka obejmuje pamięć maszyny, jej ustawienia i stan wszystkich dysków wirtualnych. Przywrócenie cofa wszystkie trzy elementy do chwili zrobienia migawki. Warto pamiętać, że to wnioski z tej samej strony wiki, a szczegóły zależą od wersji Proxmox.
Słowa: migawka proxmox, snapshot, live snapshot, qcow2, stan maszyny, pamięć, przywrócenie migawki, rollback, magazyn migawek, kvm, cofnięcie zmian
Źródło: https://pve.proxmox.com/wiki/Live_Snapshots (sprawdzono 2026-10; oficjalna wiki Proxmox VE)

### Czy migawka Proxmox zastępuje kopię zapasową maszyny?
Nie. Oficjalna wiki Proxmox mówi, że migawki nie są uwzględniane w kopii zapasowej. Do kopii na żywo maszyny z migawkami potrzebny jest co najmniej Proxmox VE 2.3. Migawka jest punktem przywracania na krótki czas, np. przed aktualizacją, a kopia zapasowa służy do odtworzenia po awarii. Dlatego przy ważnych danych potrzebujesz obu, a kopię trzymaj poza samą maszyną. Że migawki nie powinny trzymać się latami, to wniosek z ogólnej wiedzy.
Słowa: migawka a kopia, snapshot vs backup, kopia zapasowa maszyny, odtworzenie po awarii, punkt przywracania, przed aktualizacją, nie zastępuje backupu, długo trzymane migawki
Źródło: https://pve.proxmox.com/wiki/Live_Snapshots (sprawdzono 2026-10; oficjalna wiki Proxmox VE; wniosek o długim trzymaniu migawek z ogólnej wiedzy)
