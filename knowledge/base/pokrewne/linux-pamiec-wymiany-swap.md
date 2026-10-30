<!-- Baza wiedzy MT_SW: pamięć wymiany (swap) w Linuksie na serwerze domowym. Tylko informacyjnie. -->
# Linux: pamięć wymiany (swap)

### Czym jest swap w Linuksie i czy pomaga przy braku RAM?
Swap to miejsce na dysku, z którego Linux korzysta, gdy pamięć operacyjna nie mieści stron tekstu i danych działających procesów. System zapisuje wtedy nieaktywne strony na dysk i zwalnia RAM. Dysk jest dużo wolniejszy od pamięci, więc częste wymiany pogarszają działanie systemu. Oracle Linux podkreśla, że dodawanie swapu nie jest prawdziwym rozwiązaniem braku pamięci. Jeśli system często wymienia strony, lepiej dołożyć RAM niż zwiększać swap. Ta reguła pochodzi z dokumentacji Oracle Linux, która dotyczy tej dystrybucji, ale ogólna zasada jest taka sama w innych Linuksach.
Słowa: swap, pamięć wymiany, ram, brak pamięci, wymiana stron, swapping, dysk, wydajność, oom, pamięć operacyjna, zram
Źródło: https://docs.oracle.com/en/operating-systems/oracle-linux/6/admin/about-swap-space.html (sprawdzono 2026-10; dokumentacja Oracle Linux 6)

### Partycja swap czy plik swap: co wybrać na serwerze domowym?
Dokumentacja Oracle Linux porównuje dwie możliwości. Partycja swap jest szybsza, więc warto ją wybrać, gdy już wiesz, ile swapu potrzebujesz. Plik swap łatwiej zmienić, więc na początku, gdy nie wiesz, zacznij od pliku, a partycję zrób później, gdy poznasz wymagania. Źródło nie porównuje wpływu na zużycie SSD ani nie opisuje zram, więc tych kwestii nie podaję jako faktów. Uwaga o zram to wniosek z ogólnej wiedzy.
Słowa: partycja swap, plik swap, swapfile, mkswap, swapon, wybór swapu, szybkość swapu, zmiana rozmiaru, zram, serwer domowy
Źródło: https://docs.oracle.com/en/operating-systems/oracle-linux/6/admin/about-swap-space.html (sprawdzono 2026-10; dokumentacja Oracle Linux 6)
