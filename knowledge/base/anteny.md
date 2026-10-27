# Anteny i dopasowanie

<!-- Plik edytowalny ręcznie. Format: "### pytanie", treść do ok. 800 znaków, wiersz "Słowa:" (ukryte słowa kluczowe, tylko do wyszukiwania),
     na końcu wiersz "Źródło: adres (sprawdzono RRRR-MM)". Wpisy oznaczone "do przeglądu" czekają na sprawdzenie przez Rafała. -->

### Czy można włączyć radio bez anteny?
Nie. Sygnał nadawany bez anteny może odbić się z powrotem i uszkodzić urządzenie. Podobnie ostrożnie trzeba używać anteny słabo dopasowanej. Zawsze podłącz antenę przed włączeniem radia.
Słowa: bez anteny, uszkodzenie, spalenie, PA, wzmacniacz mocy, nadawanie, odbicie
Źródło: https://meshtastic.org/docs/hardware/antennas/antenna-testing/ (sprawdzono 2026-10)

### Jak sprawdzić, czy antena jest dopasowana (SWR)?
Antena musi być dostrojona do używanej częstotliwości; tanie anteny bywają błędnie opisane i wtedy sygnał jest słabszy. Dopasowanie mierzy analizator wektorowy (VNA): pokazuje, czy mało mocy wraca do nadajnika na częstotliwości pracy. Kryterium z dokumentacji: niskie VSWR (poniżej 2) na częstotliwości strojenia (patrz karta katalogowa anteny). VNA mówi tylko, czy antena jest dopasowana, a nie jak dobrze promieniuje; to sprawdza się testem zasięgu.
Słowa: SWR, VSWR, KFS, WFS, NanoVNA, VNA, analizator, miernik, dopasowanie, strojenie, odbita moc
Źródło: https://meshtastic.org/docs/hardware/antennas/antenna-testing/ (sprawdzono 2026-10)

### Jak porównać dwie anteny w praktyce?
Test zasięgu: idź z urządzeniem i wysyłaj wiadomości, zapisuj miejsca, w których przychodzi potwierdzenie (ACK), i podawaną siłę sygnału. Potem wymień antenę i powtórz tę samą trasę. Test można zautomatyzować modułem Range Test. W aplikacji MT_SW pomaga też ocena sygnału i kalkulator anten.
Słowa: porównanie, test zasięgu, range test, RSSI, SNR, ACK, pomiar
Źródło: https://meshtastic.org/docs/hardware/antennas/antenna-testing/ (sprawdzono 2026-10)

### Jaką antenę wybrać do mojego pasma?
Antena musi pasować do pasma urządzenia (np. 868 MHz w Polsce, 915 MHz w USA, 433 MHz). Lista społeczności dzieli popularne anteny na kompaktowe/ręczne, stacjonarne/przekaźnikowe i samochodowe. Nie jest to rekomendacja projektu Meshtastic: sprawdź własne potrzeby. W sieci świętokrzyskiej (869,44165 MHz) szukaj anten na 868 MHz.
Słowa: wybór anteny, 868, 915, 433, pasmo, kompaktowa, stacjonarna, samochodowa, omni, zysk, dBi
Źródło: https://meshtastic.org/docs/hardware/antennas/ (sprawdzono 2026-10)

### Co znaczy RP-SMA i SMA, czy złącza pasują?
RP-SMA to wersja z odwróconą polaryzacją i nie jest wymienna ze zwykłym SMA. Przed zakupem anteny lub kabla sprawdź, jakiego złącza używa Twoje urządzenie.
Słowa: złącze, SMA, RP-SMA, gniazdo, wtyk, pigtail, kabel, adapter
Źródło: https://meshtastic.org/docs/hardware/antennas/ (sprawdzono 2026-10)

### Jaki rodzaj anteny: pół fali, ćwierć fali czy kierunkowa?
Półfalowy bat może mieć większy zasięg dookólny niż ćwierćfalowa krótka antena, ale rzuca się w oczy w kieszeni. Ćwierćfalowa krótka często potrzebuje płaszczyzny masy, by pracować najlepiej. Duża antena kierunkowa sięga znacznie dalej niż dookólna, ale trzeba ją skierować na cel, więc nie nadaje się do ruchu. Nie nadają się antena zbliżeniowa (PAN) ani wielka antena na długim kablu zaprojektowana pod nadajnik 100 W.
Słowa: półfalowa, ćwierćfalowa, whip, stubby, kierunkowa, yagi, dookólna, płaszczyzna masy, ground plane, rodzaje anten
Źródło: https://meshtastic.org/docs/hardware/antennas/lora-antenna/ (sprawdzono 2026-10)

### Gdzie umieścić antenę i czy kabel szkodzi?
Ściany i budynki tłumią sygnał; antena przy ścianie traci sygnał wysyłany w jej stronę. Często lepiej postawić węzeł na zewnątrz, niż trzymać go w środku z anteną na zewnątrz. Większość kabli znacząco osłabia sygnał na dłuższym odcinku; wyjątkiem są ekstremalne warunki (upał, mróz, wilgoć), gdzie używa się najkrótszego kabla o małych stratach. Węzeł trzymaj w porządnej obudowie chroniącej przed pogodą. Antena powinna mieć impedancję 50 Ω i złącze SMA.
Słowa: umiejscowienie, montaż anteny, kabel, straty, tłumienie, ściana, obudowa, 50 omów, impedancja, na zewnątrz, maszt
Źródło: https://meshtastic.org/docs/hardware/antennas/lora-antenna/ (sprawdzono 2026-10)

### Czy antena musi pasować do częstotliwości?
Tak. Urządzenia na innych częstotliwościach nie porozumieją się z Twoim, a antena strojona na inne pasmo słabo promieniuje. Dokumentacja odsyła do listy częstotliwości The Things Network według krajów. Autor radzi, że warto zapłacić więcej lokalnemu dostawcy z porządnymi kartami katalogowymi.
Słowa: częstotliwość anteny, pasmo, 868, 915, dostawca, karta katalogowa, datasheet
Źródło: https://meshtastic.org/docs/hardware/antennas/lora-antenna/ (sprawdzono 2026-10)
