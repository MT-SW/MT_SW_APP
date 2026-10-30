<!-- Baza wiedzy MT_SW: polecenia pct do zarządzania kontenerami LXC w Proxmox VE (stop, shutdown, enter, exec, destroy, unprivileged). Tylko informacyjnie. -->
# Proxmox VE: polecenia pct dla kontenerów

### Czym różni się pct stop od pct shutdown w Proxmox VE?
Polecenie pct shutdown wykonuje czyste wyłączenie kontenera i czeka do domyślnie 60 sekund, aż procesy się zamkną. Polecenie pct stop przerywa wszystkie procesy w kontenerze od razu, bez czekania. Dlatego przed zatrzymaniem ważnego serwisu, np. bazy danych, wybierz shutdown. Stop jest na wypadek, gdy kontener nie reaguje. Dokumentacja pct potwierdza oba działania i ich domyślne zachowania.
Słowa: pct shutdown, pct stop, wyłączenie kontenera, zatrzymanie, 60 sekund, czyste wyłączenie, przerwanie procesów, kontener nie reaguje, lxc wyłączenie
Źródło: https://pve.proxmox.com/pve-docs/pct.1.html (sprawdzono 2026-10; oficjalna dokumentacja pct w Proxmox VE)

### Co robią polecenia pct enter, pct exec i pct destroy w Proxmox VE?
Polecenie pct enter uruchamia powłokę wewnątrz kontenera. Polecenie pct exec uruchamia pojedyncze polecenie wewnątrz kontenera i nie otwiera sesji. Polecenie pct destroy usuwa kontener razem z jego plikami, więc przed nim zrób kopię. Polecenia pct create i pct restore tworzą kontener lub przywracają go z kopii. Domyślnie nowe kontenery są nieuprzywilejowane (unprivileged): root w kontenerze mapuje się na zwykłego użytkownika poza nim. Opcja --unprivileged domyślnie wynosi 1. Ta różnica zwiększa bezpieczeństwo, bo błąd w kontenerze nie daje pełnych uprawnień na hoście.
Słowa: pct enter, pct exec, pct destroy, pct create, pct restore, pct clone, nieuprzywilejowany kontener, unprivileged, user namespace, root w kontenerze, --unprivileged, usuwanie kontenera
Źródło: https://pve.proxmox.com/pve-docs/pct.1.html (sprawdzono 2026-10; oficjalna dokumentacja pct w Proxmox VE; strona długa, opisano część z opisów poleceń)
