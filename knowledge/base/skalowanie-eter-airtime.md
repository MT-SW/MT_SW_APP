# Dlaczego sieć LoRa nie może być zbyt duża: eter, czas antenowy, kolizje

<!-- Uzupełnić o polski artykuł z grupy (meshtastic.pop.pl, "Dlaczego sieci radiowe oparte na protokole LoRa... to sieci lokalne, a nie globalne"), gdy będzie dostępny tekst. -->

### Dlaczego sieć mesh LoRa ma twardy limit wielkości?
Eter jest wspólny i bardzo wąski: LoRa daje pojedyncze kilobity na sekundę. Każdy węzeł w zasięgu słyszy każdy pakiet, nawet gdy nie jest jego adresatem, a każde powtórzenie zajmuje kanał dla wszystkich naraz. Dokumentacja Meshtastic przyznaje, że transmisja radiowa narzuca twardy limit pojemności sieci, który kontrola ruchu tylko łagodzi, a nawet doskonały protokół routingu nie uniknie faktu, że wszyscy w zasięgu słyszą pakiet. Dlatego sieć LoRa jest siecią lokalną, a nie globalną.
Słowa: skalowalność, skala sieci, limit, pojemność, wspólny eter, dlaczego sieć nie może być duża, sieć lokalna, globalna, zapchanie, przepustowość
Źródło: https://meshtastic.org/blog/why-meshtastic-uses-managed-flood-routing/ (sprawdzono 2026-10)

### Ile czasu antenowego zajmuje jedna wiadomość?
Wg obliczeń WeHelp (pakiet 50 B ≈ 30 znaków, 80 B ≈ 60 znaków): ShortFast (SF7, 250 kHz) 0,05 s / 0,08 s; MediumFast (SF9) 0,18 / 0,24 s; MediumSlow (SF10) 0,34 / 0,46 s; LongFast (SF11, 250 kHz) 0,64 / 0,85 s; LongModerate (SF11, 125 kHz, CR 4/8) 2,04 / 2,82 s. Wiadomość 80 B na LongFast powtórzona przez trzy węzły zajmuje kanał ok. 3,4 s. Dla porównania limit 10 % czasu nadawania to 360 s na godzinę na jeden węzeł, ale kanał jest wspólny.
Słowa: airtime, czas antenowy, czas w eterze, ile trwa pakiet, time on air, ToA, LongFast, sekundy, 360 s, 10 procent
Źródło: https://wehelp.com.pl/poradniki/meshtastic-lacznosc-awaryjna-jak-dziala/ (sprawdzono 2026-10)

### Ile wiadomości na godzinę zmieści się w limicie 10 % (WeHelp)?
Maksymalnie pakietów 80 B na godzinę na węzeł: ShortFast ok. 4700, MediumFast ok. 1490, MediumSlow ok. 780, LongFast ok. 430, LongModerate ok. 130. Automatyczne komunikaty (NodeInfo, pozycje GPS, telemetria) mogą zapchać kanał: kilkadziesiąt radiów raportujących pozycję co minutę to już problem. Po przekroczeniu limitu firmware wstrzymuje nadawanie.
Słowa: limit nadawania, 6 minut na godzinę, 430 pakietów, pozycja co minutę, zapchanie kanału, duty cycle
Źródło: https://wehelp.com.pl/poradniki/meshtastic-lacznosc-awaryjna-jak-dziala/ (sprawdzono 2026-10)

### Jak flooding przy dużej liczbie routerów powoduje kolizje?
Gdy wiele routerów ma podobny SNR, wiele węzłów nadaje prawie jednocześnie i dochodzi do kolizji w eterze; duplikaty widać dopiero po zderzeniu, a czas antenowy jest marnowany i dalsze węzły dostają tylko część ruchu. Przykład z artykułu: 10 routerów z rebroadcast_mode=ALL w jednym bloku (plus UDP na wszystkich węzłach) jako źródło zapchania. Zalecenie: mało dobrze ustawionych routerów i dużo klientów (CLIENT_BASE/KNOWN_ONLY).
Słowa: kolizje, routery, za dużo routerów, duplikaty, SNR, burza pakietów, zapchana sieć, zbyt wiele przekaźników
Źródło: https://wiki.mesh.szczecin.pl/routing-meshtastic-od-strony-firmware-0-lora-udp-role-i-strojenie/ (sprawdzono 2026-10; artykuł zaleca testy w małej podsieci)

### Jak ograniczyć zapchanie dużej sieci (zalecenia praktyczne)?
W gęstej miejskiej sieci często wystarcza hop limit 3–4 (bezpiecznie 4 w osiedlu); obniż go, gdy sieć jest zapchana. Jeśli duplikaty w historii pakietów przekraczają 50 %, dodaj ignore_incoming dla hałaśliwych węzłów. Zmień role na CLIENT_BASE/SENSOR, ustaw rebroadcast_mode=KNOWN_ONLY, ustaw ulubione dla kluczowych routerów, wyłącz udp_enabled na nie-routerach (UDP w LAN szybko generuje burzę pakietów), świadomie ustawiaj interwały pozycji i telemetrii. W sieci o wielu stałych przekaźnikach autorzy wskazują MeshCore jako alternatywę wyznaczającą trasy.
Słowa: hop limit 3, KNOWN_ONLY, ignore_incoming, udp_enabled, ulubione, favorites, interwał pozycji, MeshCore, poprawa sieci, strojenie
Źródło: https://wiki.mesh.szczecin.pl/routing-meshtastic-od-strony-firmware-0-lora-udp-role-i-strojenie/ oraz https://wehelp.com.pl/poradniki/meshtastic-lacznosc-awaryjna-jak-dziala/ (sprawdzono 2026-10)

### Dlaczego Meshtastic używa zalewania zamiast tras?
Brak odkrywania tras: węzeł może wysyłać zaraz po starcie, a utrzymywanie tras kosztowałoby czas antenowy na pakiety kontrolne. Adaptuje się do zmian topologii (ruchome węzły, pogoda, pora dnia). Większość pakietów to rozgłoszenia, więc mądrzejszy protokół niewiele by zyskał. Mało zasobów: brak tablic routingu oszczędza RAM, flash i procesor. Autorzy przyznają, że to rozwiązanie jest nieefektywne i niedoskonałe, a inne testowane protokoły nie pobiły go konsekwentnie w zastosowaniach Meshtastic. Przy kontrolowanym ruchu sieć może działać w siatkach powyżej 100 węzłów, bez gwarancji.
Słowa: managed flooding, zalewanie, routing tras, MeshCore, path discovery, dlaczego flood, ponad 100 węzłów, Meshtasticator
Źródło: https://meshtastic.org/blog/why-meshtastic-uses-managed-flood-routing/ (sprawdzono 2026-10)

### Jakie są różnice między Meshtastic a MeshCore w skali?
Wg polskich stron krytycznych wobec Meshtastic (loramesh.pl, lora.waw.pl): MeshCore wyznacza i zapamiętuje trasy (path discovery) i ma lepiej skalować się w dużych, obciążonych sieciach, a Meshtastic ma wysyłać dużo zbędnych transmisji i gubić wiadomości w obciążonych sieciach. Strony nie podają liczb ani wyników testów. Sieci Meshtastic i MeshCore nie komunikują się ze sobą (wspólny jest tylko sprzęt). Ta ocena jest opinią autorów stron: traktuj z rezerwą.
Słowa: MeshCore, Reticulum, porównanie, Meshtastic vs MeshCore, skalowanie, path discovery, opinia
Źródło: https://www.loramesh.pl/meshtastic-kontra-meshcore/ oraz https://lora.waw.pl/ (sprawdzono 2026-10; opinie bez pomiarów)
