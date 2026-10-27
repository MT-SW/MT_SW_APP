# Store & Forward: ustawienia i wymagania

### Store & Forward: jakie ma ustawienia i wartości domyślne?
Moduł Store & Forward pozwala węzłowi, który był poza zasięgiem, poprosić specjalny serwer o ponowne wysłanie wiadomości tekstowych. Od firmware 2.4, po połączeniu aplikacji z serwerem, historia pobiera się sama. Ustawienia: Enabled (włącza moduł); Heartbeat (serwer wysyła co 15 minut sygnał, że jest w zasięgu; strona zaleca wyłączyć, jeśli historię pobiera się tylko przez aplikację); History Return Max (ile wiadomości maksymalnie zwrócić, domyślnie 25); History Return Window (z ilu minut wstecz, domyślnie 240); Records (limit zapisów, domyślnie 0, czyli 2/3 dostępnej pamięci PSRAM, około 11 000 rekordów); Is server (ustawia węzeł z PSRAM jako serwer, alternatywa dla roli ROUTER, od wersji 2.4).
Słowa: store forward s&f historia wiadomości serwer psram records heartbeat history return max window
Źródło: https://meshtastic.org/docs/configuration/module/store-and-forward-module/ (sprawdzono 2026-10)

### Store & Forward: jaki sprzęt może być serwerem i jakie są ograniczenia?
Serwerem może być tylko urządzenie ESP32 z pamięcią PSRAM (strona wymienia m.in. T-Beam nowszy niż wersja 1.0 oraz T3S3). Serwer powinien działać stale; jeśli coś pomija, niezawodność zapisanej historii spada. Ograniczenia: serwer zwraca tylko wiadomości z żądanego okna czasu i do ustawionego limitu; możliwe są duplikaty, bo serwer nie wie, które wiadomości klient naprawdę przegapił; duża liczba zwracanych wiadomości może chwilowo obciążyć sieć; żądanie historii przez LoRa nie działa na domyślnym publicznym kanale. W Androidzie historię można zażądać wiadomością bezpośrednią z tekstem „SF” (aplikacje od wersji 2.2.23). W aplikacjach Apple: długie przytrzymanie węzła i „Client History”.
Słowa: store forward serwer esp32 psram t-beam t3s3 duplikaty sf android klient historia ograniczenia
Źródło: https://meshtastic.org/docs/configuration/module/store-and-forward-module/ (sprawdzono 2026-10)
