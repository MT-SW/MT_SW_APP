# Kanały, kopia konfiguracji i powrót do oryginalnego firmware w aplikacji

### Jak udostępnić kanał innej osobie?
Kanał udostępniasz kodem QR albo adresem URL, który zawiera nazwę kanału, ustawienia radia i klucz. Wejdź w Ustawienia → Ustawienia urządzenia → Kanały i użyj przycisku „Udostępnij kanały": aplikacja pokaże kod QR i adres URL (możesz go skopiować i wysłać wiadomością). Druga osoba w swojej aplikacji wybiera „Skanuj kod QR udostępnionych kanałów" albo wpisuje URL kanału. Dodanie zachowuje kanały, które już ma, i dokłada Twoje; można też wybrać zastąpienie jej kanałów Twoimi. Kto zna klucz kanału, odczyta jego ruch, więc udostępniaj go tylko zaufanym osobom.
Słowa: udostępnić kanał, udostępnianie kanału, podzielić się kanałem, wysłać kanał znajomemu, kod QR kanału, link do kanału, URL kanału, dodać kanał z linku, share channel, share a channel with a friend, channel QR
Źródło: Dokumentacja aplikacji: ekran Kanały (sprawdzono 2026-11)

### Jak zrobić kopię zapasową konfiguracji i przywrócić ją z pliku?
Wejdź w Ustawienia → Ustawienia urządzenia i przewiń do sekcji „Kopia zapasowa i przywracanie". „Eksport konfiguracji" zapisuje ustawienia połączonego radia do pliku, a „Import konfiguracji" wczytuje taki plik z powrotem do radia. Radio musi być połączone z aplikacją. Eksport zrób przed zmianą firmware, zmianą dużej liczby ustawień albo przywracaniem ustawień fabrycznych, bo wtedy można wrócić do poprzedniego stanu jednym plikiem. Osobna kopia bazy aplikacji (wiadomości, węzły) jest w tej samej sekcji, gdy aplikacja ją udostępnia.
Słowa: kopia zapasowa, backup, przywrócić konfigurację, przywracanie z pliku, import konfiguracji, eksport konfiguracji, zapisać ustawienia do pliku, wczytać ustawienia, restore, export config
Źródło: Dokumentacja aplikacji: ustawienia urządzenia, kopia zapasowa i przywracanie (sprawdzono 2026-11)

### Jak wrócić do oryginalnego firmware?
Wgraj oficjalną wersję firmware dla swojej płytki, tak jak każdą inną aktualizację. Najprościej przez Web Flasher na flasher.meshtastic.org: wybierz model radia i oficjalną wersję, a potem wgraj ją przez USB. Radia z układem nRF52 (np. RAK4631) wchodzą w tryb bootloadera, a plik .uf2 przeciąga się na dysk, który się pojawi; ESP32 wgrywa się przez Web Flasher lub narzędzie esptool. Zrób wcześniej „Eksport konfiguracji", bo wgranie pełnego pliku może skasować ustawienia. Z ogólnej wiedzy, nie ze źródła: po zmianie wersji firmware o większej różnicy pomaga też ustawienie fabryczne.
Słowa: oryginalny firmware, wrócić do oficjalnego, cofnąć firmware, wgrać oficjalny firmware, usunąć MT_SW_FIRMWARE, powrót do Meshtastic, downgrade, web flasher, uf2, factory reset po aktualizacji
Źródło: Dokumentacja aplikacji: aktualizacje firmware (sprawdzono 2026-11)

### Jak długo wytrzyma bateria 18650 w węźle?
Czas pracy obliczysz ze wzoru: pojemność ogniwa (mAh) razy około 0,8, podzielona przez średni pobór radia (mA); wynik jest w godzinach. Ogniwo 18650 ma zwykle od 2000 do 3500 mAh. Przykład z założonym poborem: przy średnim poborze 20 mA ogniwo 3000 mAh da około 120 godzin, czyli 5 dni. Rzeczywisty pobór zależy od płytki, roli, GPS, ekranu, trybu oszczędzania energii i ruchu w sieci, więc zmierz go miernikiem albo obserwuj spadek baterii w karcie węzła. Z ogólnej wiedzy, nie ze źródła; liczby w przykładzie są tylko ilustracją wzoru.
Słowa: bateria 18650, ogniwo 18650, czas pracy baterii, jak długo wytrzyma bateria, pojemność mAh, pobór prądu, autonomia węzła, ile godzin na baterii
Źródło: Wiedza ogólna o zasilaniu; pobór konkretnej płytki sprawdź w dokumentacji producenta (sprawdzono 2026-11)
