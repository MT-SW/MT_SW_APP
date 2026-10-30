<!-- Baza wiedzy MT_SW: jak Proxmox Backup Server dzieli dane na fragmenty i deduplikuje kopie. Tylko informacyjnie. -->
# Proxmox Backup Server: fragmenty i deduplikacja

### Jak Proxmox Backup Server dzieli kopie na fragmenty i co to daje?
Proxmox Backup Server zapisuje dane w fragmentach (chunks), a każdy fragment ma identyfikator wyliczony z skrótu SHA-256 jego zawartości. Dla obrazów dysków maszyn wirtualnych używa stałych fragmentów, zwykle po 4 MiB. Ponieważ obraz jest dzielony w tych samych miejscach, niezmieniony blok daje ten sam fragment. Dla kopii plikowych stosuje dynamiczne fragmenty wyznaczane przez dane, dzięki czemu edycja zmienia tylko pobliskie fragmenty, a nie wszystko za nią. Fragmenty przechowywane są raz w magazynie (datastore) w katalogu .chunks, a migawki (snapshoty) tylko się do nich odwołują. Dzięki temu wiele migawek może współdzielić te same fragmenty. Przy kolejnej kopii klient wysyła tylko skróty fragmentów, których serwer jeszcze nie ma. Każda migawka pozostaje kompletna i może działać samodzielnie.
Słowa: proxmox backup server, pbs, fragmenty, chunks, deduplikacja, sha-256, migawka, snapshot, datastore, .chunks, przyrostowa kopia, obraz dysku, 4 mib, dynamiczne fragmenty, pxar
Źródło: https://pbs.proxmox.com/docs/technical-overview.html (sprawdzono 2026-10; oficjalna dokumentacja PBS)

### Czy szyfrowanie kopii w PBS psuje deduplikację?
Dokumentacja PBS mówi, że przy szyfrowanych fragmentach skrót liczy się z zawartości połączonej z kluczem szyfrującym, a nie z samego zaszyfrowanego tekstu. Te same dane zaszyfrowane różnymi kluczami dostają więc różne identyfikatory i się nie łączą. Klient szyfruje też tylko te fragmenty, które faktycznie wysyła. W praktyce deduplikacja działa w obrębie jednego klucza. Serwer bez klucza nie sprawdzi zawartości, więc weryfikuje tylko sumę kontrolną CRC-32. Jeśli masz kilka węzłów, użycie jednego klucza daje lepszą deduplikację, ale to wniosek z ogólnej wiedzy o konfiguracji, nie wprost ze źródła.
Słowa: pbs szyfrowanie, klucz szyfrujący, deduplikacja a szyfrowanie, crc-32, weryfikacja kopii, wiele klucz, oszczędność miejsca, kopia zaszyfrowana, datastore, klient
Źródło: https://pbs.proxmox.com/docs/technical-overview.html (sprawdzono 2026-10; oficjalna dokumentacja PBS)
