<!-- Baza wiedzy MT_SW: czyszczenie nieużywanych zasobów Dockera (prune) i ostrożność z woluminami. Tylko informacyjnie. -->
# Docker: czyszczenie zasobów (prune)

### Co usuwa docker image prune i czym różni się od docker image prune -a?
Domyślnie docker image prune usuwa tylko obrazy „wiszące” (dangling), czyli nieotagowane i niewykorzystywane przez żaden kontener. Opcja -a rozszerza to na wszystkie obrazy, których nie używa żaden istniejący kontener. Przed czyszczeniem obrazów dobrze zrobić docker ps -a, bo zatrzymane kontenery nadal zajmują miejsce na swoich warstwach zapisu. Ta zasada jest w oficjalnej dokumentacji i warto ją znać przed uruchomieniem polecenia.
Słowa: docker image prune, dangling, obrazy wiszące, -a, nieużywane obrazy, zwolnienie miejsca, docker ps -a, zatrzymane kontenery, warstwy, czyszczenie dysku
Źródło: https://docs.docker.com/engine/manage-resources/pruning/ (sprawdzono 2026-10; oficjalna dokumentacja Docker)

### Dlaczego Docker nie usuwa woluminów automatycznie i co z tym robić przy docker system prune?
Dokumentacja Dockera podkreśla, że woluminy nigdy nie są usuwane automatycznie, bo mogłoby to skasować dane. Polecenie docker system prune czyści kontenery, sieci i obrazy, ale nie woluminy, a usunięcie woluminów wymaga osobnej flagi --volumes. Osobne polecenie docker volume prune domyślnie usuwa tylko anonimowe, nieużywane woluminy. Przed czyszczeniem woluminów sprawdź, które są potrzebne, bo ich utrata oznacza utratę danych bazy czy kopii.
Słowa: docker volume prune, woluminy, --volumes, system prune, utrata danych, anonimowe woluminy, dane bazy, bezpieczne czyszczenie, nie kasuj woluminów
Źródło: https://docs.docker.com/engine/manage-resources/pruning/ (sprawdzono 2026-10; oficjalna dokumentacja Docker; dokładny zakres flagi volume prune z opisu strony)
