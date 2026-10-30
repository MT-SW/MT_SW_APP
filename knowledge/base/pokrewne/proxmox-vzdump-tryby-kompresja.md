<!-- Baza wiedzy MT_SW: tryby kopii zapasowych vzdump w Proxmox VE (snapshot, suspend, stop) i kompresja. Tylko informacyjnie. -->
# Proxmox VE: tryby kopii vzdump

### Który tryb kopii vzdump wybrać w Proxmox VE: snapshot, suspend czy stop?
Tryb snapshot jest domyślny i daje najmniejszy przestój, ale z małym ryzykiem niespójności, bo dane kopiowane są w trakcie pracy maszyny. Jeśli włączony jest agent gościa, Proxmox wywołuje zamrożenie i odmrożenie systemu plików, co poprawia spójność. Tryb suspend zawiesza maszynę przed trybem snapshot. Przestój jest dłuższy, a spójność nie zawsze się poprawia, więc dokumentacja zaleca snapshot. Tryb stop daje największą spójność kosztem krótkiego przestoju: maszyna jest wyłączana w uporządkowany sposób, kopiowana przez proces QEMU w tle i uruchamiana ponownie, jeśli wcześniej działała.
Słowa: vzdump mode, snapshot, suspend, stop, kopia maszyny, przestój, spójność kopii, agent gościa, fsfreeze, zamrożenie systemu plików, kopia na żywo, tryb kopii
Źródło: https://pve.proxmox.com/pve-docs/vzdump.1.html (sprawdzono 2026-10; oficjalna dokumentacja vzdump w Proxmox VE)

### Jak działa kompresja kopii vzdump i czym są opcje zstd, gzip i lzo?
Opcja --compress przyjmuje wartości 0, 1, gzip, lzo albo zstd. Domyślnie jest 0, czyli bez kompresji. Dokumentacja podaje, że zstd jest najszybszy z tych trzech i obsługuje wielowątkowość, a lzo i gzip są szerzej znane i domyślnie zainstalowane. Opcja --zstd określa liczbę wątków, domyślnie 1. Wartość 0 oznacza połowę dostępnych rdzeni, a wartość powyżej zera to liczba wątków. Nazwa pliku kopii zdradza użytą kompresję po rozszerzeniu. Źródło nie porównuje stopnia kompresji, więc nie podaję tu zysku miejsca.
Słowa: vzdump compress, zstd, gzip, lzo, kompresja kopii, --zstd, wątki zstd, połowa rdzeni, rozszerzenie pliku kopii, miejsce na kopie, zstd proxmox
Źródło: https://pve.proxmox.com/pve-docs/vzdump.1.html (sprawdzono 2026-10; oficjalna dokumentacja vzdump w Proxmox VE)
