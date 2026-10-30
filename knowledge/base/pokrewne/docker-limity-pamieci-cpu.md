### Jaki jest minimalny limit pamięci flagi --memory w Dockerze?
Flaga `--memory` (lub `-m`) ustawia maksymalną pamięć, jaką kontener może użyć. Minimalna wartość to 6m.

Słowa: docker, --memory, -m, limit pamięci, kontener, 6m
Źródło: https://docs.docker.com/engine/containers/resource_constraints/ (sprawdzono 2026-10)

### Co dzieje się z kontenerem Docker przy przekroczeniu limitu pamięci?
Domyślnie przy błędzie braku pamięci jądro zabija procesy w kontenerze. Strona nie opisuje tego szerzej w odczytanej części.

Słowa: docker, out of memory, OOM, jądro, zabijanie procesów, limit pamięci
Źródło: https://docs.docker.com/engine/containers/resource_constraints/ (sprawdzono 2026-10)

### Jak działa --memory-swap w Dockerze?
Flaga `--memory-swap` ma sens tylko razem z `--memory`. Dodatnia wartość oznacza sumę pamięci i swapu, a `--memory` obejmuje część bez swapu. Przykład: `--memory=300m` z `--memory-swap=1g` daje 300m pamięci i 700m swapu. Wartość `0` jest ignorowana, a gdy jest równa `--memory`, kontener nie ma dostępu do swapu.

Słowa: docker, --memory-swap, swap, pamięć, limit, 1g, 300m
Źródło: https://docs.docker.com/engine/containers/resource_constraints/ (sprawdzono 2026-10)
