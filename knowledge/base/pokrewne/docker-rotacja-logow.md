<!-- Baza wiedzy MT_SW: rotacja logów kontenerów Docker (json-file, max-size, max-file). Tylko informacyjnie. -->
# Docker: rotacja logów kontenerów

### Jak ustawić rotację logów Dockera, żeby nie zapchały dysku?
Domyślnie sterownik json-file ma max-size równy -1, czyli logi rosną bez limitu, bo nie są rotowane. Opcja max-file domyślnie ma wartość 1 i działa tylko wtedy, gdy ustawisz też max-size. Określa, ile plików logu zostaje. Opcja compress domyślnie jest wyłączona, a po włączeniu kompresuje obrócone pliki. Ustawienie globalne wpisuje się w /etc/docker/daemon.json jako "log-driver": "json-file" oraz "log-opts" z wartościami w cudzysłowach, np. "max-size": "10m" i "max-file": "3". Wartości liczbowe i logiczne muszą być w cudzysłowach, bo daemon.json traktuje log-opts jako tekst. Przy tych ustawieniach jeden kontener zajmie najwyżej około 30 MB logów.
Słowa: docker logi, json-file, max-size, max-file, rotacja logów, daemon.json, log-opts, compress, dysk pełny, logi kontenerów, 10m, 3 pliki, zapchany dysk
Źródło: https://docs.docker.com/engine/logging/drivers/json-file/ (sprawdzono 2026-10; oficjalna dokumentacja Docker; wyliczenie 30 MB jest z ogólnej wiedzy: 3 pliki po 10 MB)

### Czy zmiana rotacji logów w daemon.json działa od razu dla istniejących kontenerów?
Nie. Zmiany w daemon.json dotyczą tylko kontenerów utworzonych po restarcie Dockera. Istniejące kontenery zachowują stare ustawienia logów, więc trzeba je odtworzyć albo utworzyć na nowo. Dokumentacja Dockera podaje to jako zasadę. Dla pojedynczego kontenera ustawienia przekazuje się przy docker run opcjami --log-opt max-size=10m --log-opt max-file=3. Wpisanie tego w pliku compose zależy od wersji, więc sprawdź dokumentację sterownika logów w swojej wersji.
Słowa: zmiana logów, istniejące kontenery, restart docker, log-opt, docker run log, aktualizacja ustawień logów, odtworzenie kontenera, opcje sterownika
Źródło: https://docs.docker.com/engine/logging/drivers/json-file/ (sprawdzono 2026-10; oficjalna dokumentacja Docker)
