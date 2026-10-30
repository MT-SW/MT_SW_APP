<!-- Baza wiedzy MT_SW: depends_on w Docker Compose, warunki gotowości usług i kolejność startu. Tylko informacyjnie. -->
# Docker Compose: depends_on i warunki gotowości

### Jak Docker Compose ustala kolejność startu i zatrzymania usług?
Opcja depends_on w pliku Compose steruje kolejnością uruchamiania i zatrzymywania usług. Compose tworzy zależności przed zależnymi, a zatrzymuje w odwrotnej kolejności. Oprócz depends_on kolejność wyznaczają też links, volumes_from i network_mode: "service:...". Sama kolejność nie gwarantuje jednak, że usługa jest gotowa do pracy. Do tego służą warunki w polu condition.
Słowa: docker compose depends_on, kolejność startu, kolejność zatrzymania, links, volumes_from, network_mode, zależności usług, compose up, startup order, usługi
Źródło: https://docs.docker.com/compose/how-tos/startup-order/ (sprawdzono 2026-10; oficjalna dokumentacja Docker)

### Jaka jest różnica między service_started a service_healthy w depends_on?
Warunek service_started oznacza, że zależność jest uruchomiona. Warunek service_healthy oznacza, że zależność przeszła test healthcheck, czyli jest gotowa. Warunek service_completed_successfully czeka, aż zależność zakończy się sukcesem, np. dla kontenera wykonującego migrację. Dokumentacja zaznacza, że service_healthy działa tylko wtedy, gdy zależność ma zdefiniowany healthcheck, bo bez niego Compose nie oceni zdrowia. Przykład z dokumentacji to test pg_isready dla usługi Postgres z ponowieniem co 10 sekund. Opcja restart: true sprawia, że gdy zależność zostanie zrestartowana poleceniem docker compose restart, zależna usługa też się zrestartuje, żeby ponownie połączyć się z zależnością.
Słowa: service_healthy, service_started, service_completed_successfully, healthcheck, pg_isready, restart true, zależna usługa, baza danych gotowa, czekanie na bazę, kontener nie działa, migracja
Źródło: https://docs.docker.com/compose/how-tos/startup-order/ (sprawdzono 2026-10; oficjalna dokumentacja Docker)
