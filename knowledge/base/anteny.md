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
