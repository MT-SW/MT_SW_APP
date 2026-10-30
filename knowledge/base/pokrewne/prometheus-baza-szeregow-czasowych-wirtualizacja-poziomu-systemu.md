### Czym jest Prometheus według Wikipedii?
Prometheus to darmowe, otwartoźródłowe oprogramowanie do monitorowania zdarzeń i alertowania. Jest napisane w Go i ma licencję Apache 2.0.

Słowa: Prometheus, monitoring, alerty, Go, Apache 2.0, otwarte źródło
Źródło: https://en.wikipedia.org/wiki/Prometheus_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak Prometheus przechowuje metryki według Wikipedii?
Metryki są zapisywane jako szeregi czasowe. Każda nazwana metryka może mieć dowolną liczbę par klucz-wartość, czyli etykiet, np. nazwę serwera, kod statusu HTTP lub endpoint. Dzięki temu dane są wielowymiarowe i można odpytywać dowolną kombinację etykiet.

Słowa: Prometheus, metryki, szeregi czasowe, etykiety, klucz-wartość, wielowymiarowe
Źródło: https://en.wikipedia.org/wiki/Prometheus_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak Prometheus zbiera dane według Wikipedii?
Serwer Prometheus okresowo odpytuje skonfigurowaną listę celów, zwanych eksporterami, i agreguje zwrócone wartości szeregów czasowych. Ma też wykrywanie usług, które znajduje cele w dynamicznych środowiskach. Ten model to pobieranie danych (pull), a nie wysyłanie przez cel.

Słowa: Prometheus, pull, eksportery, cele, wykrywanie usług, zbieranie danych
Źródło: https://en.wikipedia.org/wiki/Prometheus_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak wygląda baza danych Prometheus według Wikipedii?
Prometheus ma własną bazę. Świeże dane są w pamięci i plikach mapowanych w pamięci, starsze trafiają do trwałych bloków, a dziennik write-ahead zapewnia trwałość po awarii.

Słowa: Prometheus, baza, pamięć, mapowanie, bloki, write-ahead log, awaria
Źródło: https://en.wikipedia.org/wiki/Prometheus_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak działają alerty w Prometheus według Wikipedii?
Reguła alertu określa warunek i czas trwania. Gdy warunek utrzymuje się tak długo, Prometheus wysyła alert do Alertmanagera, który obsługuje routing i powiadomienia.

Słowa: Prometheus, alert, reguła, warunek, czas trwania, Alertmanager, powiadomienia
Źródło: https://en.wikipedia.org/wiki/Prometheus_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest baza danych szeregów czasowych według Wikipedii?
To system programowy zoptymalizowany do przechowywania i udostępniania szeregów czasowych, czyli par czas-wartość. Wczesne przykłady służyły przemysłowi jako tzw. historyki danych z czujników, a dziś zastosowania są szersze.

Słowa: baza szeregów czasowych, czas, wartość, historyk danych, przemysł, czujniki
Źródło: https://en.wikipedia.org/wiki/Time_series_database (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Do czego używa się baz szeregów czasowych według Wikipedii?
Strona wymienia monitoring, czyli przetwarzanie danych z czujników i metryk w czasie rzeczywistym, ciągłe zapytania i wykrywanie anomalii. Używa się ich też w IoT do zbierania danych z czujników i na rynkach finansowych do ciągłego gromadzenia danych.

Słowa: baza szeregów czasowych, monitoring, IoT, rynki finansowe, anomalie, czujniki, czas rzeczywisty
Źródło: https://en.wikipedia.org/wiki/Time_series_database (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jak kompresja i retencja działają w bazach szeregów czasowych według Wikipedii?
Bazy często używają algorytmów kompresji. Ponieważ dane szeregów czasowych są jednorodne, specjalistyczna kompresja może przewyższać ogólną. Można je skonfigurować tak, by regularnie usuwały stare dane lub je próbkowały w mniejszej rozdzielczości, w przeciwieństwie do zwykłych baz, które przechowują dane bezterminowo.

Słowa: baza szeregów czasowych, kompresja, retencja, downsampling, usuwanie starych danych
Źródło: https://en.wikipedia.org/wiki/Time_series_database (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Docker według Wikipedii?
Docker to zestaw produktów, który używa wirtualizacji na poziomie systemu operacyjnego do pakowania oprogramowania w kontenery. Automatyzuje wdrażanie aplikacji w lekkich kontenerach, które działają spójnie w różnych środowiskach. Rdzeniem jest Docker Engine.

Słowa: Docker, kontenery, wirtualizacja poziomu systemu, Docker Engine, wdrażanie, spójność
Źródło: https://en.wikipedia.org/wiki/Docker_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym kontenery Dockera różnią się od maszyn wirtualnych według Wikipedii?
W odróżnieniu od maszyn wirtualnych wszystkie kontenery współdzielą jedno jądro systemu operacyjnego, przez co zużywają mniej zasobów.

Słowa: Docker, kontener, maszyna wirtualna, jądro, współdzielenie, zasoby
Źródło: https://en.wikipedia.org/wiki/Docker_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest Docker Compose i Docker Hub według Wikipedii?
Docker Compose to narzędzie do definiowania i uruchamiania aplikacji wielokontenerowych, konfigurowane plikami YAML i uruchamiane jednym poleceniem. Docker Hub to główny publiczny rejestr obrazów i domyślne miejsce, gdzie Docker ich szuka.

Słowa: Docker Compose, Docker Hub, YAML, rejestr obrazów, aplikacje wielokontenerowe
Źródło: https://en.wikipedia.org/wiki/Docker_(software) (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Czym jest wirtualizacja na poziomie systemu operacyjnego według Wikipedii?
To paradygmat, w którym jądro pozwala na wiele odizolowanych instancji przestrzeni użytkownika, np. kontenerów. Programy w kontenerze widzą tylko jego zawartość i przydzielone urządzenia. W systemach uniksowych to zaawansowana forma chroot. Przykłady to LXC, Docker i chroot.

Słowa: wirtualizacja poziomu systemu, kontener, jądro, izolacja, chroot, LXC, Docker
Źródło: https://en.wikipedia.org/wiki/Operating-system-level_virtualization (sprawdzono 2026-10, źródło angielskie, Wikipedia)

### Jakie są zalety i ograniczenia wirtualizacji poziomu systemu według Wikipedii?
Narzut jest zwykle mniejszy niż przy pełnej wirtualizacji sprzętowej. Programy używają zwykłego interfejsu wywołań systemowych, bez emulacji ani maszyny pośredniej, i nie wymagają wsparcia sprzętowego. Wadą jest elastyczność: kontenery współdzielą jądro hosta, więc nie uruchomią innego systemu ani jądra. Różne dystrybucje Linuksa działają, Windows już nie. Przykłady pełnej wirtualizacji to VMware ESXi, QEMU i Hyper-V.

Słowa: wirtualizacja poziomu systemu, narzut, wywołania systemowe, jądro hosta, Windows, VMware ESXi, QEMU, Hyper-V
Źródło: https://en.wikipedia.org/wiki/Operating-system-level_virtualization (sprawdzono 2026-10, źródło angielskie, Wikipedia)
