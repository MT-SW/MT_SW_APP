<!-- Baza wiedzy MT_SW: kontenery LXC w Proxmox VE i uruchamianie Dockera w nich. Tylko informacyjnie. -->
# Proxmox: kontenery LXC i Docker

### Czym jest kontener LXC w Proxmox VE i czego w nim nie uruchomisz?
Kontener LXC w Proxmox VE to lekka alternatywa dla pełnej maszyny wirtualnej. Współdzieli jądro Linuksa hosta zamiast emulować cały system, więc narzut jest mały. Ograniczenie jest takie, że w kontenerze działają tylko dystrybucje Linuksa. FreeBSD ani Windows nie są obsługiwane. Proxmox VE zarządza kontenerami narzędziem pct. Źródło wskazuje też, że gdy potrzebujesz maksymalnej izolacji lub migracji na żywo, zalecane jest zagnieżdżanie kontenerów wewnątrz maszyny QEMU. To zalecenie dotyczy też Dockera, bo kontenery w kontenerze mają swoje ograniczenia.
Słowa: lxc, proxmox lxc, kontener, pct, jądro hosta, współdzielone jądro, linux, windows w kontenerze, freebsd, maszyna wirtualna, kvm, qemu, migracja na żywo
Źródło: https://pve.proxmox.com/wiki/Linux_Container (sprawdzono 2026-10; oficjalna wiki Proxmox VE)

### Jak uruchomić Dockera w kontenerze LXC w Proxmox VE?
Wiki Proxmox nie ma osobnego poradnika o Dockerze, ale wymienia dwie opcje, które są do tego potrzebne. Opcja keyctl jest wymagana, żeby uruchomić Dockera w kontenerze, i dotyczy tylko kontenerów nieuprzywilejowanych. Opcja nesting udostępnia procfs i sysfs, co pozwala na zagnieżdżone kontenery. Wiki zaleca jednak, dla maksymalnej izolacji i migracji na żywo, zagnieżdżanie kontenerów w maszynie QEMU. Wybór między kontenerem LXC a maszyną wirtualną dla Dockera to decyzja zależna od wymagań, a ryzyka bezpieczeństwa z nim związane są z ogólnej wiedzy.
Słowa: docker w lxc, keyctl, nesting, zagnieżdżanie, proxmox docker, kontener nieuprzywilejowany, unprivileged, procfs, sysfs, docker w kontenerze, docker na proxmox
Źródło: https://pve.proxmox.com/wiki/Linux_Container (sprawdzono 2026-10; oficjalna wiki Proxmox VE; wnioski o bezpieczeństwie z ogólnej wiedzy)
