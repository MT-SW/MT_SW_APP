<!-- Baza wiedzy MT_SW: kopie zapasowe Home Assistant i terminy ważności certyfikatów Let's Encrypt. Tylko informacyjnie. -->
# Serwer domowy: kopie Home Assistant i certyfikaty Let's Encrypt

### Jak zrobić szybką kopię zapasową Home Assistant i czym różni się od kopii automatycznej?
Akcja backup.create w Home Assistant tworzy szybką, lokalną kopię i nie wymaga konfiguracji. Zawiera konfigurację i bazę danych, a zapisuje ją tylko w lokalnej pamięci. Nie ma w niej dodatków ani dodatkowych folderów i nie jest zabezpieczona hasłem. Jeśli chcesz kontrolować zawartość, miejsce zapisu albo hasło, użyj ustawień automatycznych kopii i akcji backup.create_automatic, która korzysta z tych ustawień. Akcja backup.create działa tylko w instalacjach core i container. Na systemie Home Assistant OS i w instalacjach supervised trzeba użyć kopii automatycznej. Źródło nie opisuje retencji, więc nie porównuję jej między tymi trybami. Zalecenie, żeby kopia była w innym miejscu niż serwer, to wniosek z ogólnej wiedzy.
Słowa: home assistant, backup, kopia zapasowa, backup.create, backup.create_automatic, kopia lokalna, konfiguracja, baza danych, hasło, core, container, supervised, home assistant os, przywracanie
Źródło: https://www.home-assistant.io/actions/backup.create/ (sprawdzono 2026-10)

### Kiedy wygasają certyfikaty Let's Encrypt i jak zmienią się terminy odnowienia?
Let's Encrypt planuje skrócić ważność certyfikatów TLS z 90 do 64 dni. Od 10 lutego 2027 roku domyślnie wydawane lub odnawiane certyfikaty będą ważne 64 dni. Certyfikaty w środowisku testowym (staging) z 64-dniową ważnością pojawią się 14 października 2026. Ostatni 90-dniowy certyfikat ma wygasnąć około 11 maja 2027. Istniejące certyfikaty zostaną ważne do daty wygaśnięcia. Zalecane jest odnawianie po około dwóch trzecich okresu ważności, a nie stałą liczbę dni przed końcem. Skrypty z zakodowanymi na sztywno wartościami, np. 83, 80 lub 60 dni, mogą przestać działać i trzeba je sprawdzić. Źródło opisuje to jako zmiany zapowiedziane, a nie już działające.
Słowa: let's encrypt, letsencrypt, certyfikat, tls, ssl, 90 dni, 64 dni, ważność certyfikatu, odnowienie, certbot, acme, staging, renewal, wygaśnięcie, cron
Źródło: https://linuxiac.com/?p=220883 (sprawdzono 2026-10; to omówienie oficjalnego ogłoszenia Let's Encrypt w serwisie Linuxiac)
