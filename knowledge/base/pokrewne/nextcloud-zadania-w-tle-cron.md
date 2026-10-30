<!-- Baza wiedzy MT_SW: zadania w tle w Nextcloud (AJAX, webcron, cron, timer systemd). Tylko informacyjnie. -->
# Nextcloud: zadania w tle (cron)

### Jak ustawić zadania w tle w Nextcloud, żeby działały regularnie?
Nextcloud ma trzy sposoby uruchamiania zadań w tle. Domyślny AJAX uruchamia zadanie przy każdym załadowaniu strony, więc przy długiej bezczynności nic się nie wykona. Oficjalna dokumentacja uznaje go za najmniej niezawodny i nie zaleca go przy wielu użytkownikach, aplikacji Activity albo zewnętrznych magazynach. Webcron wywołuje cron.php z zewnętrznej usługi i nadaje się tylko dla bardzo małych instancji, rzędu 1–5 użytkowników. Cron to zalecana metoda dla większych instancji: systemowy cron uruchamia cron.php bezpośrednio, więc nie zależy od limitów serwera WWW. Przykładowy wpis to co 5 minut: */5 * * * * php -f /var/www/nextcloud/cron.php, uruchamiany jako użytkownik serwera WWW. Wybierz Cron w ustawieniach zadań w tle albo ustaw to poleceniem occ background:cron.
Słowa: nextcloud cron, zadania w tle, background jobs, ajax, webcron, cron.php, */5, www-data, occ background:cron, aktualizacja cron, zadania nextcloud, activity, mało użytkowników
Źródło: https://docs.nextcloud.com/server/stable/admin_manual/configuration_server/background_jobs_configuration.html (sprawdzono 2026-10; oficjalna dokumentacja Nextcloud)

### Dlaczego cron.php w Nextcloud wykonuje tylko jedno zadanie na wywołanie i jak to ująć w timer systemd?
Oficjalna dokumentacja podaje, że przy webcronie wykonywane jest tylko jedno zadanie na wywołanie. Przy wywołaniu co 5 minut daje to 288 zadań dziennie, więc webcron nie pasuje do większych instancji. Na systemach z systemd można zastąpić wpis cron timerem. Potrzebne są dwa pliki w /etc/systemd/system/: usługa uruchamiająca cron.php i timer wywołujący ją co 5 minut, z ustawieniami OnBootSec=5min i OnUnitActiveSec=5min. Timer włącza się poleceniem systemctl enable --now nextcloudcron.timer. Wpis w cronie czy timer to wybór sposobu uruchamiania, a nie różnica w wyniku.
Słowa: nextcloud timer, systemd timer, nextcloudcron.timer, onbootsec, onunitactivesec, 288 zadań, webcron limit, usługa systemd, cron vs timer, zadania co 5 minut, systemctl enable
Źródło: https://docs.nextcloud.com/server/stable/admin_manual/configuration_server/background_jobs_configuration.html (sprawdzono 2026-10; oficjalna dokumentacja Nextcloud)
