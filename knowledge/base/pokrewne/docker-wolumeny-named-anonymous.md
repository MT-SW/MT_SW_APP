### Czym są wolumeny Docker i gdzie są przechowywane?
Wolumen to trwały magazyn danych dla kontenerów, tworzony i zarządzany przez Dockera. Jest przechowywany w katalogu na hoście Dockera, a ten katalog jest montowany w kontenerze. Na Linuksie strona nie podaje domyślnej ścieżki.

Słowa: docker, wolumen, volume, trwałe dane, katalog hosta, montowanie
Źródło: https://docs.docker.com/engine/storage/volumes/ (sprawdzono 2026-10)

### Jakie zalety mają wolumeny Docker wobec bind mount?
Strona wymienia, że wolumeny łatwiej wykonać kopię zapasową lub przenieść, da się nimi zarządzać przez CLI i API Dockera, działają w Linuksie i Windowsie, można je bezpiecznie współdzielić między kontenerami i przeznaczone są do wydajnego I/O. Bind mount zależy od struktury katalogów i systemu hosta.

Słowa: docker, wolumen, bind mount, zalety, backup, współdzielenie, I/O
Źródło: https://docs.docker.com/engine/storage/volumes/ (sprawdzono 2026-10)

### Czym różni się nazwany wolumen od anonimowego w Dockerze?
Nazwany wolumen dostaje nazwę wybraną przez użytkownika. Anonimowy dostaje losową nazwę unikalną na hoście i żeby go współdzielić, trzeba go zamontować po losowym ID. Anonimowe wolumeny nie są automatycznie ponownie używane.

Słowa: docker, nazwany wolumen, anonimowy wolumen, losowa nazwa, ID, współdzielenie
Źródło: https://docs.docker.com/engine/storage/volumes/ (sprawdzono 2026-10)

### Czy wolumen Docker zostaje po usunięciu kontenera?
Oba typy wolumenów zostają po usunięciu kontenera, który ich używał. Wyjątkiem jest anonimowy wolumen utworzony z flagą `--rm`, który jest usuwany razem z kontenerem.

Słowa: docker, wolumen, --rm, usuwanie kontenera, trwałość danych
Źródło: https://docs.docker.com/engine/storage/volumes/ (sprawdzono 2026-10)
