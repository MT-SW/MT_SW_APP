### Czym różni się OnBootSec od OnUnitActiveSec w timerze systemd?
`OnBootSec=` liczy czas od startu maszyny. Jeśli moment już minął, gdy jednostka timera się aktywuje, uruchamia się od razu. `OnUnitActiveSec=` liczy czas od ostatniej aktywacji uruchamianej jednostki. Połączenie obu daje powtarzający się interwał.

Słowa: systemd, timer, OnBootSec, OnUnitActiveSec, interwał, harmonogram
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemd.timer.html (sprawdzono 2026-10)

### Jak działa OnCalendar i wiele wpisów w timerze systemd?
`OnCalendar=` używa wyrażeń kalendarzowych zgodnych ze składnią z systemd.time(7) i może wystąpić wielokrotnie. Jeśli system był uśpiony i przegapił czas wyzwolenia, po wybudzeniu uruchamia usługę raz, a nie kilka razy.

Słowa: systemd, OnCalendar, timer, uśpienie, wyrażenie kalendarzowe
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemd.timer.html (sprawdzono 2026-10)

### Do czego służy Persistent= w timerze systemd?
`Persistent=` działa tylko z `OnCalendar=` i domyślnie jest wyłączone. Gdy włączone, timer zapisuje czas ostatniego wyzwolenia na dysku. Jeśli w czasie nieaktywności timera przegapiono uruchomienie, usługa wystartuje przy aktywacji, z uwzględnieniem `RandomizedDelaySec=`.

Słowa: systemd, Persistent, OnCalendar, RandomizedDelaySec, przegapione uruchomienie
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemd.timer.html (sprawdzono 2026-10)

### Ile wynosi domyślna dokładność AccuracySec w systemd?
Domyślnie `AccuracySec=` to 1 minuta. Systemd może wtedy odpalić timer w dowolnym miejscu tego okna, żeby grupować wybudzenia. Wartość `1us` daje największą precyzję kosztem większej liczby wybudzeń.

Słowa: systemd, AccuracySec, dokładność, timer, wybudzenia
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemd.timer.html (sprawdzono 2026-10)

### Dlaczego timer OnCalendar czeka na synchronizację czasu?
Timery z `OnCalendar=` są automatycznie ustawiane po `time-sync.target`. Na systemach bez baterii podtrzymującej RTC pomaga `systemd-time-wait-sync.service`, który dba o poprawny zegar przed uruchomieniem timerów.

Słowa: systemd, time-sync.target, OnCalendar, RTC, synchronizacja czasu
Źródło: https://www.freedesktop.org/software/systemd/man/latest/systemd.timer.html (sprawdzono 2026-10)
