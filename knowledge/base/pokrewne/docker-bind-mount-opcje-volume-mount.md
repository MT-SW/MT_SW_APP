<!-- Baza wiedzy MT_SW: różnica między --volume i --mount przy bind mount w Dockerze. Tylko informacyjnie. -->
# Docker: bind mount, --volume i --mount

### Kiedy w Dockerze użyć bind mount i czym różni się --volume od --mount?
Bind mount podłącza do kontenera konkretny folder z hosta, np. konfigurację albo kod w trakcie pracy. Docker zarządza tylko zawartością wolumenów nazwanych, a bind mount to ścieżka, którą sam wybierasz. Źródło wskazuje typowe przypadki: wspólny kod lub wyniki budowania między hostem a kontenerem, zapis plików generowanych przez kontener na hoście oraz udostępnienie plików konfiguracyjnych z hosta, np. /etc/resolv.conf. Różnica w składni: flaga --volume tworzy brakujący katalog na hoście automatycznie, a --mount kończy się błędem, jeśli ścieżka nie istnieje, chyba że dodasz bind-create-src. Jeśli demon Dockera nie ma uprawnień do utworzenia katalogu, musisz go założyć ręcznie przed startem kontenera.
Słowa: docker bind mount, bind mount, --volume, -v, --mount, bind-create-src, katalog na hoście, ścieżka, resolv.conf, montowanie, wolumen, uprawnienia katalogu, docker run
Źródło: https://docs.docker.com/engine/storage/bind-mounts/ (sprawdzono 2026-10; oficjalna dokumentacja Docker; strona jest długa, opisano początek i sekcję o składni)
