# Dobre praktyki konfiguracji sieci

### Jaką rolę ustawić w moim węźle?
Zasada: trzymaj rolę CLIENT, CLIENT_MUTE lub CLIENT_BASE; inne role tylko z konkretnego, zrozumiałego powodu. CLIENT jest najlepszy w większości sytuacji (ma automatyczne inteligentne opóźnienia retransmisji), także dla węzłów na dachu, w wysokich punktach i w odludnych miejscach. CLIENT_MUTE nigdy nie powtarza: dla urządzenia ręcznego przy mocniejszym węźle w zatłoczonej sieci i dla węzłów w budynku, które sięgają tylko jednego węzła na zewnątrz. Gdy kilka węzłów stoi blisko siebie, najlepiej położony ustaw na CLIENT, resztę na CLIENT_MUTE. CLIENT_BASE: jeden dobrze umieszczony węzeł na poddaszu lub dachu, a pozostałe swoje węzły dodaj w nim jako ulubione.
Słowa: rola, jaką rolę, client, client_mute, client_base, router czy client, ustawienie roli, poradnik
Źródło: https://meshtastic.org/docs/configuration/tips/ (sprawdzono 2026-10)

### Kiedy używać ROUTER lub REPEATER, a kiedy nie?
Nie używaj ROUTER ani REPEATER bez konkretnej potrzeby: niepotrzebne użycie zwiększa kolizje pakietów, obniża skuteczność dostarczania i skraca efektywny zasięg przez zużywanie przeskoków. Przed zmianą z CLIENT skonsultuj się z lokalną społecznością. Najstabilniejszy układ to wiele węzłów CLIENT i niewielka liczba dobrze umieszczonych routerów.
Słowa: router, repeater, nie używać routera, kolizje, zasięg, hop, społeczność, ile routerów
Źródło: https://meshtastic.org/docs/configuration/tips/ (sprawdzono 2026-10)

### Ile ustawić hop limit?
Od wersji 2.8.x zalecamy w MT_SW ustawić 7. Firmware 2.8 ma automatyczny, zmienny hop limit: ważne pakiety, jak wiadomości, dostają pełny limit, a mniej ważne (np. telemetria) mają go niższy, zależnie od ruchu w całej sieci. Dlatego 7 nie zalewa już sieci tak jak dawniej. Oficjalna dokumentacja nadal podaje 3 jako domyślne i wystarczające dla większości sieci; na starszym firmware (przed 2.8) zostaw 3 i podnoś tylko na węzłach brzegowych.
Słowa: hop limit, limit skoków, ile skoków, max hops, liczba przeskoków, 7, 3, zwiększyć zasięg, węzły brzegowe, automatyczny hop limit, 2.8
Źródło: https://github.com/meshtastic/firmware/releases/tag/v2.8.0.7239fe8 (automatyczny, zmienny hop limit) oraz https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Ile jest kanałów i jak działa udostępnianie pozycji (2.7.1+)?
Jest 8 kanałów: 0 to główny, 1–7 prywatne grupy lub zastosowania specjalne (np. admin). Wszystkie dodatkowe muszą mieć tę samą konfigurację modemu LoRa co główny. Domyślny kanał główny to LongFast z kluczem AQ==, więc Twoja pozycja jest widoczna dla każdego węzła w pobliżu. Od firmware 2.7.1: wyłącz udostępnianie pozycji na kanale 0, włącz je na jednym kanale dodatkowym z wybraną precyzją; automatyczne aktualizacje dostaje tylko kanał dodatkowy o najniższym indeksie z włączoną lokalizacją, pozostałe obsługują ręczne zapytania o pozycję.
Słowa: 8 kanałów, kanały, pozycja, prywatność, lokalizacja kanał 0, udostępnianie pozycji, 2.7.1, precyzja
Źródło: https://meshtastic.org/docs/configuration/tips/ (sprawdzono 2026-10)

### Dlaczego nie widzę innych węzłów po zmianie ustawień?
Węzły muszą mieć ten sam preset modemu i slot częstotliwości, by się słyszeć. Baza węzłów przetrwa zmianę konfiguracji, więc dawno słyszane węzły mogą nadal być na liście; firmware 2.8.1+ oznacza węzły niesłyszane na bieżących ustawieniach, ale nie wie, jakich używają teraz. Przed zmianą ustawień całej sieci sprawdź, czego używają sąsiedzi. Zasięg testuj wiadomością bezpośrednią (wiadomość kanałowa może się udać, gdy usłyszy ją tylko pobliski węzeł). Zmieniając nazwę kanału głównego, ustaw ręcznie kanał LoRa na domyślny dla regionu.
Słowa: nie widzę węzłów, zmiana presetu, slot, niewidoczne węzły, test zasięgu, DM, po zmianie ustawień
Źródło: https://meshtastic.org/docs/configuration/tips/ (sprawdzono 2026-10)

### Jak uciszyć hałaśliwą sieć lub węzeł?
Wycisz kanał ustawieniem channel.mute. Pojedyncze węzły wyciszaj lub ignoruj przez Python CLI: --set-muted-node / --remove-muted-node oraz --set-ignored-node / --remove-ignored-node z ID w formacie !xxxxxxxx. Ostrzeżenia: nie zmieniaj w dużej sieci ustawień, których nie rozumiesz; testuj sprzęt i ustawienia przed instalacją w trudno dostępnym miejscu; połączenie z publicznym serwerem MQTT może opublikować w internecie pozycje wszystkich węzłów Twojej sieci.
Słowa: wyciszenie, mute, ignore, hałaśliwy węzeł, set-ignored-node, channel.mute, publiczny MQTT pozycje
Źródło: https://meshtastic.org/docs/configuration/tips/ (sprawdzono 2026-10)
