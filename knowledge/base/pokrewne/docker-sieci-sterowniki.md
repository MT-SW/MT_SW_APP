### Jakie sterowniki sieci ma Docker na Linuksie?
Dokumentacja wymienia sterowniki bridge (domyślny), host, none, overlay i macvlan. Jest też ipvlan, który podłącza kontenery do zewnętrznych VLAN-ów. Każdy sterownik ma inne zasady izolacji i dostępu do sieci hosta.

Słowa: docker, sieć, bridge, host, none, overlay, macvlan, ipvlan
Źródło: https://docs.docker.com/engine/network/ (sprawdzono 2026-10)

### Czym różni się sterownik host od none w Dockerze?
Sterownik host usuwa izolację sieciową między kontenerem a hostem Dockera. Sterownik none całkowicie odcina kontener od hosta i od innych kontenerów.

Słowa: docker, host, none, izolacja sieci, kontener, hosta
Źródło: https://docs.docker.com/engine/network/ (sprawdzono 2026-10)

### Dlaczego kontenery na domyślnym bridge nie widzą się po nazwie?
Na domyślnej sieci bridge kontenery mogą łączyć się tylko po adresie IP i nie mogą odwoływać się do siebie po nazwie. W sieciach zdefiniowanych przez użytkownika działa komunikacja po adresie IP i po nazwie kontenera.

Słowa: docker, bridge, nazwa kontenera, DNS, sieć użytkownika, adres IP
Źródło: https://docs.docker.com/engine/network/ (sprawdzono 2026-10)

### Jak udostępnić port kontenera Docker poza hosta?
Porty na sieci bridge są dostępne z hosta i z kontenerów w tej samej sieci, ale nie z zewnątrz hosta. Flaga `--publish` albo `-p` udostępnia port na zewnątrz hosta i kontenerom w innych sieciach bridge.

Słowa: docker, -p, publish, port, bridge, udostępnianie portu
Źródło: https://docs.docker.com/engine/network/ (sprawdzono 2026-10)
