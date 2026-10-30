### Do czego służy replikacja storage w Proxmox VE?
Replikacja kopiuje wolumeny gościa na inny węzeł, więc dane są dostępne bez współdzielonego storage. Działa na migawkach, więc po pierwszej pełnej synchronizacji wysyłane są tylko zmiany. Uruchamia się automatycznie w ustawianych interwałach.

Słowa: proxmox, replikacja, storage, migawki, węzeł, synchronizacja
Źródło: https://pve.proxmox.com/wiki/Storage_Replication (sprawdzono 2026-10)

### Jaki typ storage obsługuje replikacja w Proxmox VE?
Według strony replikacja obsługuje lokalne ZFS, czyli typ pluginu `zfspool`. Ma wsparcie migawek i jest oznaczona jako stabilna.

Słowa: proxmox, replikacja, zfspool, ZFS, migawki, stabilna
Źródło: https://pve.proxmox.com/wiki/Storage_Replication (sprawdzono 2026-10)

### Jaki jest minimalny interwał replikacji w Proxmox VE?
Minimalny interwał replikacji to jedna minuta. Gość można replikować na kilka węzłów docelowych, ale nie dwa razy na ten sam węzeł.

Słowa: proxmox, replikacja, interwał, jedna minuta, węzeł docelowy
Źródło: https://pve.proxmox.com/wiki/Storage_Replication (sprawdzono 2026-10)

### Jaki błąd pojawia się przy replikacji, gdy storage nie istnieje na węźle docelowym?
Strona podaje jako częstą przyczynę komunikat, że storage o tym samym ID nie jest dostępny na węźle docelowym. Oznacza to, że ID storage musi istnieć po obu stronach.

Słowa: proxmox, replikacja, storage ID, węzeł docelowy, błąd, konfiguracja
Źródło: https://pve.proxmox.com/wiki/Storage_Replication (sprawdzono 2026-10)
