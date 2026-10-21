---
title: Ustawienia — Moduły i administracja
parent: Przewodnik użytkownika
nav_order: 8
last_updated: 2026-10-21
description: Konfiguracja opcjonalnych modułów funkcji (MQTT, telemetria, wiadomości predefiniowane, TAK i inne) oraz administrowanie urządzeniem.
aliases:
  - modules
  - module-config
  - administration
---

# Ustawienia — Moduły i administracja

Konfiguracja opcjonalnych modułów funkcji oraz administrowanie urządzeniem. Moduły rozszerzają MT_SW o wyspecjalizowane możliwości — każdy z nich można włączać i wyłączać niezależnie.

Włączaj tylko te moduły, z których korzystasz. Wyłączenie nieużywanych modułów zmniejsza zajętość eteru, oszczędza baterię i upraszcza konfigurację. Oczekiwany moduł może nie być widoczny z trzech powodów: rola Twojego węzła go nie włącza, Twój firmware jest starszy niż wydanie, które go dodało, albo kompilacja firmware pomija go dla tego sprzętu.

Ustawienia modułów mają układ kart z przełącznikami, listami rozwijanymi, polami tekstowymi i suwakami:

![Przełącznik w pozycji włączonej](../../assets/screenshots/settings_switch.png)

![Lista rozwijana, rozwinięta i pokazująca listę opcji](../../assets/screenshots/settings_dropdown.png)

![Pole tekstowe z wpisaną wartością](../../assets/screenshots/settings_text_field.png)

![Karta ustawień modułu z tytułem i zgrupowanymi kontrolkami](../../assets/screenshots/settings_titled_card.png)

## Konfiguracja modułów

Każdy moduł znajdziesz w **Ustawienia → Ustawienia modułu** (Module configuration).

Zapisanie ekranu modułu uruchamia węzeł ponownie: przycisk nosi nazwę **Zapisz i uruchom ponownie**, a węzeł jest przez kilka sekund niedostępny. Wyjątkiem są Zewnętrzne Powiadomienie i Meshbeacon: ich przycisk nosi nazwę **Zapisz**. Zewnętrzne Powiadomienie może nadal restartować węzeł przy niektórych zmianach, natomiast zmiana w Meshbeacon zostaje zastosowana bez restartu.

### Moduł MQTT

Łączy wiadomości z sieci mesh z brokerem MQTT i odwrotnie, zapewniając dostęp do internetu. Dzięki temu możesz rozszerzyć swoją sieć poza zasięg LoRa lub zintegrować ją z systemami automatyki domowej.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Adres | Adres brokera MQTT | `mqtt.meshtastic.org` |
| Szyfrowanie włączone | Publikuje pakiety nadal zaszyfrowane kluczem kanału | Włączone |
| Włącz wyjście JSON | Publikuje i odbiera wiadomości MQTT w formacie JSON. Widoczne tylko w węźle z firmware starszym niż 2.8, który usunął obsługę JSON, albo dopóki to ustawienie jest nadal włączone | Wyłączone |
| Raportowanie na mapie | Publikuje pozycję na publicznej mapie — zobacz następującą dalej kartę raportowania na mapie | Wyłączone |
| Proxy klienta MQTT | Pozwala połączonej aplikacji przenosić ruch MQTT węzła, zamiast łączyć się z brokerem bezpośrednio z węzła | Wyłączone |
| Włącz MQTT | Włącza most MQTT | Wyłączone |
| Proxy MQTT w tej aplikacji | Część tej samej funkcji po stronie aplikacji dla **Proxy klienta MQTT**: określa, czy ta aplikacja pełni rolę przekaźnika. Pokazuje, czy przekaźnik działa, i można go włączyć tylko wtedy, gdy włączone są jednocześnie **Włącz MQTT** i **Proxy klienta MQTT**. Zobacz [MQTT](mqtt) | — |
| Hasło | Hasło do uwierzytelniania | Hasło publicznego brokera |
| Główny temat | Bazowa ścieżka tematu MQTT | `msh` |
| TLS włączony | Używa bezpiecznego połączenia | Wyłączone |
| Nazwa użytkownika | Nazwa użytkownika do uwierzytelniania | Nazwa użytkownika publicznego brokera |

Włączenie **Raportowania na mapie** odsłania kartę zgody z nagłówkiem *Zgoda na udostępnianie niezaszyfrowanych danych węzła za pośrednictwem protokołu MQTT*, a pod nią przełącznik **Wyrażam zgodę.** (I agree.). Reszta karty nie istnieje na ekranie, dopóki nie wyrazisz zgody:

| Ustawienie | Opis |
|---------|-------------|
| Wyrażam zgodę. | Wyraźna zgoda na przesyłanie danych Twojego węzła, w tym przybliżonej pozycji, bez szyfrowania. Bez niej raportowanie na mapie się nie rozpocznie |
| Interwał publikowania na mapie | Jak często raportować. Lista rozwijana ze stałymi interwałami od 1 godziny do 72 godzin — nic krótszego nie jest oferowane |
| Suwak dokładności | Suwak nie ma własnej etykiety. Ustawia, jak zgrubnie publikowana jest Twoja pozycja, w zakresie od 12 do 15; linia pod nim pokazuje *±* wynikową odległość w Twoich jednostkach |

Szczegółowy przewodnik, obejmujący szyfrowanie, prywatność i konfigurację brokera, znajdziesz w [MQTT](mqtt).

### Moduł portu szeregowego

Włącza komunikację przez port szeregowy dla integracji z zewnętrznymi urządzeniami (modułami GPS, czujnikami lub własnym sprzętem). Po włączeniu port szeregowy węzła może wysyłać i odbierać dane protobuf lub tekstowe, co pozwala zewnętrznym mikrokontrolerom lub komputerom współpracować z siecią mesh.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Prędkość transmisji | Szybkość portu, od **110 Baud** do **921600 Baud** | Domyślna |
| Włącz echo | Odsyła z powrotem przez port szeregowy pakiety, które wysyłasz | Wyłączone |
| Tryb serial | Jakim protokołem porozumiewa się port: **Default**, **Simple**, **Protobufs**, **Text Message**, **NMEA Positions** lub **CalTopo**, a do tego `WS85`, `VE_DIRECT`, `MS_CONFIG`, `LOG` i `LOGTEXT` pod nazwami z firmware | Domyślny |
| Nadpisz port szeregowy konsoli | Przejmuje port, z którego zwykle korzysta konsola debugowania | Wyłączone |
| Pin GPIO odbioru (rxd) / Pin GPIO nadawania (txd) | Piny GPIO połączenia szeregowego | 0 |
| Włącz tryb serial | Włącza komunikację szeregową | Wyłączone |
| Limit czasu | Jak długo czekać, zanim odebrane dane zostaną uznane za kompletny pakiet. 0 oznacza 250 ms | 0 |

### Moduł Zewnętrznego Powiadomienia

Steruje alertami brzęczyka, diody LED lub wibracji na sprzęcie Twojego węzła. Przydatny w urządzeniach, które mają fizycznie sygnalizować nadejście wiadomości — szczególnie pomocny w instalacjach bez nadzoru lub na zewnątrz.

Są dwa niezależne wyzwalacze — przychodząca **wiadomość** i odebrany znak **dzwonka** — a każdy z nich może osobno sterować diodą LED, brzęczykiem i silnikiem wibracyjnym, co daje sześć przełączników. Firmware włącza moduł i wybiera wyjścia na sprzęcie, który ma brzęczyk, silnik wibracyjny lub diodę powiadomień; na innym sprzęcie wszystko jest na początku wyłączone.

| Ustawienie | Opis |
|---------|-------------|
| Aktywny | Czy pin diody LED jest aktywny stanem wysokim, czy niskim. Widoczne, gdy ustawiony jest **Pin wyjściowy GPIO** |
| Alert po odebraniu dzwonka / Alert brzęczykiem GPIO po odebraniu dzwonka / Alert silnikiem wibracyjnym GPIO po odebraniu dzwonka | Które wyjścia reagują na odebrany znak dzwonka |
| Alert po odebraniu wiadomości / Alert brzęczykiem GPIO po odebraniu wiadomości / Alert wibracją | Które wyjścia reagują na przychodzącą wiadomość |
| Zewnętrzne powiadomienie włączone | Główny przełącznik modułu |
| Czas trwania (w milisekundach) | Jak długo trwa pojedynczy alert |
| Czas przypomnienia (sekundy) | Powtarza alert przez ten czas, aż zostanie potwierdzony. **Nieustawiony** wyłącza przypominanie |
| Pin wyjściowy GPIO | Pin, do którego podłączona jest dioda LED |
| Pin wyjściowy brzęczyka GPIO | Pin, do którego podłączony jest brzęczyk |
| Pin wyjściowy silnika wibracyjnego GPIO | Pin, do którego podłączony jest silnik wibracyjny |
| Dzwonek | Melodia odtwarzana na brzęczyku PWM, w formacie RTTTL. Można ją zaimportować z pliku |
| Użyj I2S jako buzzera | Kieruje alert na wyjście audio I2S |
| Użyj buzzera PWM | Steruje brzęczykiem sygnałem PWM, co pozwala na różne tony zamiast jednej wysokości dźwięku. Widoczne, gdy ustawiony jest **Pin wyjściowy brzęczyka GPIO** |

### Moduł Store & Forward

Buforuje wiadomości dla węzłów, które były chwilowo offline, a następnie odtwarza je, gdy te węzły ponownie się połączą. Niezbędny w sieciach, w których węzły regularnie wchodzą w zasięg i z niego wychodzą — dba o to, aby wiadomości nie ginęły podczas krótkich rozłączeń. Serwer wymaga ESP32 z PSRAM albo węzła Linux `meshtasticd`. Węzeł w roli Router lub Router Late działa jako serwer, nawet gdy ustawienie serwera jest wyłączone.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Maks. zwracana historia | Największa liczba zapisanych wiadomości zwracanych na jedno żądanie. 0 oznacza 25 | 0 |
| Okno zwracanej historii | Jak daleko wstecz, w minutach, zwracane są zapisane wiadomości. 0 oznacza 240 | 0 |
| Liczba rekordów | Największa liczba przechowywanych wiadomości. 0 oznacza trzy czwarte wolnej pamięci PSRAM | 0 |
| Wysyłaj bicie serca | Okresowo ogłasza, że ten węzeł jest serwerem store-and-forward | Wyłączone |
| Serwer | Pełni rolę serwera store-and-forward dla sieci | Wyłączone |
| Store & Forward włączony | Włącza funkcję store and forward | Wyłączone |

### Moduł Testu zasięgu

Zautomatyzowane narzędzie do testowania zasięgu, służące do oceny jakości łącza między węzłami. Po włączeniu węzeł okresowo wysyła wiadomości testowe z rosnącymi licznikami. Węzeł odbierający zapisuje te wiadomości, dzięki czemu możesz odejść lub odjechać, a potem przeanalizować, przy jakiej odległości wiadomości przestały docierać.

Test zasięgu działa tylko na zabezpieczonym kanale głównym. Dopóki Twój kanał główny nadal używa domyślnego klucza kanału, kontrolki interwału i CSV pozostają wyłączone — nadal możesz wyłączyć już włączony moduł — a zapisanie wymusza wyłączenie modułu, jeśli kanał wrócił do stanu publicznego.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Test zasięgu włączony | Włącza test zasięgu | Wyłączone |
| Zapisz | Zapisuje odebrane dane testowe jako plik CSV w systemie plików samego węzła. Tylko węzły ESP32 | Wyłączone |
| Interwał wiadomości nadawcy (sekundy) | Czas między transmisjami testowymi, wybierany z listy rozwijanej stałych interwałów. **Nieustawiony** wstrzymuje wysyłanie | Nieustawiony |

### Moduł Telemetrii

Określa, jakie dane telemetryczne Twój węzeł udostępnia w sieci mesh. Telemetria obejmuje kondycję urządzenia (bateria, czas pracy) oraz dane z czujników środowiskowych (temperatura, wilgotność, ciśnienie).

Każda z czterech grup metryk ma własny przełącznik włączenia i własny interwał, więc możesz raportować stan baterii często, a odczyty czujników rzadko. Interwały są od 30 minut do 72 godzin.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Włącz moduł metryk jakości powietrza | Raportuje dane z czujników pyłów i CO₂ | Wyłączone |
| Czas aktualizacji metryk jakości powietrza | Jak często je raportować | — |
| Rozgłaszaj metryki urządzenia | Wysyła metryki urządzenia do sieci mesh. Gdy jest wyłączone, trafiają tylko do połączonych klientów. Widoczne tylko w firmware 2.7.13 i nowszym | Wyłączone |
| Interwał aktualizacji metryk urządzenia | Jak często raportować baterię, czas pracy i wykorzystanie kanału | — |
| Metryki środowiskowe w Fahrenheitach | Używa °F na wyświetlaczu urządzenia. Dotyczy wyłącznie ekranu węzła — aplikacja stosuje ustawienia regionalne telefonu, zobacz [Jednostki i lokalizacja](units-and-locale) | Wyłączone |
| Moduł metryk środowiskowych włączony | Raportuje podłączone czujniki środowiskowe | Wyłączone |
| Interwał aktualizacji metryk środowiskowych | Jak często je raportować | — |
| Włącz moduł metryk zasilania | Raportuje odczyty napięcia i prądu dla poszczególnych kanałów | Wyłączone |
| Czas aktualizacji metryk zasilania | Jak często je raportować | — |
| Wyświetlaj metryki zasilania na ekranie | Pokazuje odczyty zasilania także na wyświetlaczu urządzenia | Wyłączone |
| Pokaż na ekranie urządzenia | Pokazuje odczyty środowiskowe także na wyświetlaczu urządzenia | Wyłączone |

Obsługiwane czujniki i zalecenia konfiguracyjne znajdziesz w [Telemetria i czujniki](telemetry-and-sensors).

### Moduł Wiadomości predefiniowanych

Wcześniej przygotowane wiadomości dostępne z fizycznych przycisków węzła (w węzłach z enkoderami obrotowymi, klawiaturami lub podobnym sprzętem wejściowym). Zdefiniuj listę wiadomości do szybkiego wysłania, które można nadać bez podłączonego telefonu — idealne do pracy w terenie.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Zdarzenie obrotu w prawo / Zdarzenie obrotu w lewo / Zdarzenie naciśnięcia enkodera | Jakie zdarzenie klawisza wywołuje każda czynność enkodera: **Brak**, **Góra**, **Dół**, **Lewo**, **Prawo**, **Wybierz**, **Wstecz** lub **Anuluj** | Brak |
| Wiadomości | Lista wiadomości rozdzielonych znakami nowej linii | Pusta |
| Pin A / Pin B / Pin przycisku | Trzy piny, do których podłączony jest enkoder obrotowy | 0 |
| Enkoder obrotowy | Używa enkodera obrotowego jako urządzenia wejściowego | Wyłączone |
| Wyślij dzwonek | Wysyła znak dzwonka razem z wiadomością, aby mógł zabrzmieć moduł Zewnętrznego Powiadomienia w węźle odbierającym | Wyłączone |
| Wejście Góra/Dół/Wybierz | Osobny, prostszy schemat wejścia z użyciem przycisków góra/dół/wybierz zamiast enkodera | Wyłączone |

Niektóre płytki z wbudowanymi enkoderami lub klawiszami mają ustawienia wejścia już wypełnione. Dwa starsze pola, **Wiadomości predefiniowane włączone** i **Zezwól na źródło wejścia**, pojawiają się tylko w węźle z firmware starszym niż 2.7 albo dopóki nadal mają jakąś wartość.

### Moduł Dźwięku

Obsługa dźwięku Codec2 do komunikacji głosowej o niskiej przepustowości przez sieć mesh. To funkcja **eksperymentalna**, która koduje głos w bardzo małe pakiety danych za pomocą kodeka Codec2. Wymaga specjalnego sprzętu, mikrofonu i głośnika I2S, a jakość głosu jest niska.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Przepływność | Przepływność Codec2, od **450 bps** do **3200 bps**. Niższa zużywa mniej pasma i daje gorszy dźwięk | Domyślna |
| CODEC 2 włączony | Włącza moduł dźwięku | Wyłączone |
| Pin zegara I2S | Pin GPIO zegara bitowego I2S | 0 |
| Pin danych wejściowych I2S | Pin GPIO wejścia danych I2S | 0 |
| Pin danych wyjściowych I2S | Pin GPIO wyjścia danych I2S | 0 |
| Pin wyboru słowa I2S | Pin GPIO wyboru słowa I2S | 0 |
| Pin PTT (Push-To-Talk) | Pin GPIO przycisku push-to-talk. 0 oznacza GPIO 39 | 0 |

### Moduł Zdalnego sprzętu

Sterowanie GPIO przez sieć mesh. Pozwala zdalnemu węzłowi odczytywać lub zapisywać piny GPIO w innym węźle — przydatne do włączania przekaźników, odczytu przełączników lub sterowania zewnętrznym sprzętem na odległość.

> ⚠️ **Ostrzeżenie:** Włączenie opcji **Zezwól na dostęp do niezdefiniowanych pinów** daje zdalnym węzłom dostęp do wszystkich pinów GPIO, co może zakłócić pracę własnego sprzętu węzła. Włączaj ją tylko w węzłach przeznaczonych wyłącznie do obsługi GPIO.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Zezwól na dostęp do niezdefiniowanych pinów | Zezwala na dostęp do dowolnego pinu GPIO (ryzyko dla bezpieczeństwa) | Wyłączone |
| Dostępne piny | Do 4 pinów GPIO, które ten węzeł udostępnia do zdalnego odczytu/zapisu | Brak |
| Zdalny sprzęt włączony | Włącza zdalny dostęp do GPIO | Wyłączone |

### Moduł Informacji o sąsiadach

Rozgłasza informacje o bezpośrednio słyszanych sąsiadach, umożliwiając tworzenie mapy topologii sieci mesh. Każdy włączony węzeł okresowo udostępnia listę innych węzłów, które słyszy, wraz z jakością ich sygnału.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Włącz informacje o sąsiadach | Włącza rozgłaszanie informacji o sąsiadach | Wyłączone |
| Nadaj przez LoRa | Rozgłasza informacje o sąsiadach także przez LoRa, a nie tylko przez MQTT i połączoną aplikację. Niedostępne na kanale używającym domyślnego klucza i nazwy | Wyłączone |
| Interwał aktualizacji | Jak często rozgłaszać listę sąsiadów, w sekundach. Minimum 4 godziny | 0 (domyślna wartość firmware) |

Jak wykorzystać dane o sąsiadach do badania topologii sieci mesh, opisano w [Odkrywanie lokalnej sieci mesh](discovery).

### Moduł Oświetlenia otoczenia

Steruje wbudowanymi diodami NeoPixel lub innymi adresowalnymi diodami RGB na obsługiwanym sprzęcie. Może służyć do wizualnych wskaźników stanu, świateł powiadomień lub efektów dekoracyjnych.

Kolor wybierasz w rzędzie **Gotowe kolory** przy próbce albo suwakami **Czerwony**, **Zielony** i **Niebieski**. **Zaawansowane** otwiera pola liczbowe dla tych samych trzech wartości.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Prąd | Prąd zasilania diody LED, od 0 do 31 | 10 |
| Stan diody LED | Włącza lub wyłącza diodę LED | Wyłączone |
| Czerwony / Zielony / Niebieski | Wartości poszczególnych kanałów koloru, od 0 do 255 | Wyliczone z numeru węzła |

### Moduł Czujnika detekcji

Zamienia Twój węzeł w system alarmowy czujnika ruchu lub otwarcia drzwi. Gdy pin GPIO wykryje zmianę stanu (wykryty ruch, otwarte drzwi), węzeł rozgłasza w sieci mesh wiadomość alarmową.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Czujnik detekcji włączony | Włącza czujnik detekcji. Musi być też włączony w węzłach, które odbierają jego alarmy lub rysują jego dziennik | Wyłączone |
| Pin GPIO do monitorowania | Pin GPIO podłączony do czujnika. 0 wyłącza moduł | 0 |
| Minimalny czas transmisji (sekundy) | Minimalny czas między rozgłaszaniem alarmów | 0, co firmware traktuje jako 1 godzinę (12 godzin w rolach Router) |
| Nazwa | Własna nazwa tego czujnika, używana w jego wiadomościach | Pusta |
| Wyślij dzwonek | Dołącza znak dzwonka do alarmów | Wyłączone |
| Nadawanie stanu (sekundy) | Jak często wysyłać stan czujnika, niezależnie od tego, czy cokolwiek wykryto. **Nieustawione** wysyła tylko przy wykryciu | Nieustawione |
| Typ wyzwalacza | Jak stan pinu przekłada się na zdarzenie wykrycia: **Wysoki**, **Niski**, **Zbocze narastające**, **Zbocze opadające**, **Dowolne zbocze, stan wysoki** lub **Dowolne zbocze, stan niski** | Wysoki |
| Użyj wewnętrznego podciągania | Włącza wewnętrzny rezystor podciągający pinu | Wyłączone |

### Moduł Paxcounter

Licznik osób wykorzystujący zapytania sondujące Wi-Fi i BLE. Zlicza pobliskie urządzenia, nasłuchując pasywnie zapytań sondujących, które telefony i laptopy wysyłają podczas wyszukiwania sieci. Dostępny tylko na urządzeniach ESP32. Liczby są przybliżone, bo jedna osoba może mieć przy sobie kilka urządzeń, więc moduł nadaje się do szacowania natężenia ruchu pieszego na początku szlaków lub w miejscach wydarzeń.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Próg BLE (dBm) | Taka sama granica dla ogłoszeń BLE | 0, co firmware traktuje jako −80 dBm |
| Paxcounter włączony | Włącza zliczanie osób. Wi-Fi i Bluetooth muszą być w tym węźle wyłączone, aby działało | Wyłączone |
| Interwał aktualizacji | Jak często raportować liczniki | — |
| Próg WiFi (dBm) | Ignoruje zapytania Wi-Fi słabsze niż ta wartość, aby nie liczyć odległych urządzeń | 0, co firmware traktuje jako −80 dBm |

### Moduł Wiadomości statusu

Wiadomość statusu nie ma własnego ekranu modułu. Edytuje się ją razem z pozostałymi danymi tożsamości węzła, w [Ustawienia — Radio i użytkownik](settings-radio-user#user-profile).

### Moduł Meshbeacon

Rozgłasza zaproszenie do Twojej sieci mesh i odbiera zaproszenia od innych. Pozycja pojawia się tylko w węzłach z firmware 2.8.0 lub nowszym. Pełny opis krok po kroku znajdziesz w [Odkrywanie lokalnej sieci mesh](discovery).

### Moduł TAK

Integracja z Team Awareness Kit zapewniająca współpracę z ATAK i WinTAK. Pozycja nosi nazwę **TAK (ATAK)**. Aby pojawiła się na liście modułów, muszą być spełnione dwa warunki: węzeł pracuje na firmware 2.8.0 lub nowszym, a jego **Rola urządzenia** (Device Role) w **Ustawienia → Konfiguracja urządzenia → Urządzenie** jest ustawiona na **TAK** lub **TAK Tracker**. Szczegółową konfigurację i sposób użycia opisano w [Integracja TAK](tak).

## Zarządzanie

### Zdalne zarządzanie

Zdalne zarządzanie zmienia ustawienia innego węzła przez sieć mesh. Węzeł docelowy musi mieć klucz publiczny Twojego węzła na liście wpisów **Klucz administratora** (Admin Key), ustawianych na jego ekranie **Ustawienia → Bezpieczeństwo**.

1. Dotknij docelowego węzła na liście węzłów, aby otworzyć jego ekran szczegółów.
2. W karcie **Zarządzanie** dotknij **Zdalne zarządzanie**. Aplikacja najpierw nawiązuje sesję z węzłem, pokazując *Establishing remote session…*, a potem otwiera jego **Ustawienia**.
3. Zmień konfigurację.
4. Dotknij **Zapisz** lub **Zapisz i uruchom ponownie**. Zmiany zostaną wysłane przez sieć mesh.

### Akcje urządzenia

**Ustawienia → Zarządzanie** zawiera jednorazowe akcje, z których każda wymaga potwierdzenia w oknie dialogowym:

| Akcja | Co robi |
|---|---|
| Ustaw czas | Wysyła zegar Twojego telefonu do węzła |
| Restart | Uruchamia węzeł ponownie |
| Restart w trybie DFU | Uruchamia węzeł nRF52 ponownie w bootloaderze, gdzie pozostaje poza siecią mesh, dopóki nie zostanie wgrane nowe oprogramowanie lub nie zostanie zresetowany. Widoczne tylko dla węzłów nRF52 |
| Wyłącz | Wyłącza zasilanie węzła |
| Ustawienia fabryczne | Przywraca wszystkim ustawieniom wartości fabryczne |
| Zresetuj NodeDB | Czyści bazę węzłów. To okno dialogowe zawiera przełącznik **Zachować ulubione?** (Preserve Favorites?) |

> ⚠️ **Ostrzeżenie:** Ustawienia fabryczne usuwają wszystkie ustawienia, kanały i klucze i nie można tego cofnąć. Przed resetem użyj **Eksport konfiguracji**, aby zapisać ustawienia węzła, oraz **Kopia kluczy** na ekranie Bezpieczeństwo, aby zapisać jego klucze, dzięki czemu będziesz mógł przywrócić jedno i drugie.

### Kopia zapasowa i przywracanie

**Ustawienia → Kopia zapasowa i przywracanie** zapisuje całą konfigurację połączonego węzła do pliku za pomocą **Eksport konfiguracji** i wczytuje zapisany plik z powrotem za pomocą **Import konfiguracji**. Ustawienia Traffic Management są eksportowane, ale nie są stosowane przy imporcie. Wykonaj eksport przed przywróceniem ustawień fabrycznych lub aby skopiować konfigurację jednego węzła na drugi. Ta sekcja jest widoczna tylko dla Twojego własnego węzła, nie przez zdalne zarządzanie.

Ta sama sekcja tworzy też kopię danych samej aplikacji dla węzła: jego wiadomości i węzłów zapisanych w aplikacji, a nie ustawień węzła.

- **Kopia danych aplikacji** zapisuje te dane do pliku nazwanego od węzła i daty.
- **Przywróć dane aplikacji** po potwierdzeniu zastępuje wiadomości i węzły aplikacji dla tego węzła kopią zapasową. Przyjmuje tylko nieuszkodzoną kopię wykonaną przez aplikację z taką samą wersją bazy danych. Przywrócenie zadziała po ponownym uruchomieniu aplikacji i ponownym połączeniu z tym węzłem.

Obie akcje najpierw (po zapytaniu) rozłączają węzeł i działają na danych węzła, który był połączony. Gdy żaden węzeł nie jest połączony, działają na ostatnio połączonym węźle. Pozostają wyłączone, z napisem *Connect to a radio first. There is no app data to back up or restore yet.*, dopóki aplikacja nie zapisze danych jakiegoś węzła.

### Zaawansowane

**Ustawienia → Zaawansowane** zbiera narzędzia, które odczytują lub przepisują stan lokalny, i również jest widoczne tylko dla Twojego własnego węzła: **Aktualizacja oprogramowania** (Firmware Update) na sprzęcie obsługującym OTA, **Wyczyść bazę węzłów**, **Serwer TAK**, **Odkrywanie lokalnej sieci mesh** oraz **Panel debugowania**.

#### Wyczyść bazę węzłów

Usuwa węzły z Twojej bazy węzłów — z kopii w aplikacji *oraz* z bazy samego węzła, więc nie jest to czyszczenie wyłącznie wyświetlanej listy. Oba filtry łączą się, a nie działają osobno; ekran ujmuje to słowami *Wybory się sumują*.

- **Wyczyść węzły, które są starsze niż N dni** — stosowane zawsze. Suwak działa w zakresie od 7 do 365 dni i startuje od 30; po włączeniu **Wyczyść tylko nieznane węzły** jego dolna granica spada do 0.
- **Wyczyść tylko nieznane węzły** — zawęża to samo czyszczenie do węzłów, które nigdy nie wysłały swoich informacji o użytkowniku. Limit wieku nadal obowiązuje dodatkowo.

Ekran wyświetla listę węzłów oczekujących na usunięcie w miarę zmiany filtrów. **Wyczyść teraz** wykonuje czyszczenie po jeszcze jednym potwierdzeniu i nie można tego cofnąć. Ulubione węzły, ignorowane węzły oraz węzły z kluczem publicznym słyszane w ciągu ostatnich siedmiu dni nigdy nie są usuwane, niezależnie od filtrów — dlatego lista oczekujących może być krótsza, niż się spodziewasz.

#### Panel debugowania

Otwiera karty **Pakiety** i **Dzienniki aplikacji** do przeglądania, filtrowania i eksportowania danych diagnostycznych. Pełny opis znajdziesz w [Dzienniki debugowania](debug-logs).

### Ustawienia aplikacji

Sekcja **Aplikacja** na ekranie **Ustawienia** konfiguruje aplikację, a nie węzeł, i pojawia się tylko wtedy, gdy wybrany jest Twój własny węzeł. Zawiera, po kolei:

- **Zezwalaj na analizę i raportowanie awarii.** — zgoda lub rezygnacja z diagnostyki. Tylko w wersji z Google Play.
- **Podaj lokalizację telefonu do sieci** — udostępnia pozycję tego telefonu, gdy węzeł nie ma ustalonej pozycji GPS. Wymaga dokładnej lokalizacji: jeśli dozwolona jest tylko przybliżona, jej włączenie poprosi o dokładną.
- **Kompaktowe kodowanie cyrylicy** — jak traktowane są podobnie wyglądające znaki w nazwach.
- **Język**, **Motyw** i **Jednostki**. **Jednostki** to metryczne, imperialne lub zgodne z systemem; to pozycja, do której odsyła [Jednostki i lokalizacja](units-and-locale).
- **Wyświetlaj pełne znaczniki czasu**.
- **Limit pamięci podręcznej bazy urządzenia**, który określa, ile historii jest przechowywane.
- **Eksportuj pakiety zasięgu**, **Eksportuj wszystkie pakiety** i **Eksportuj bazę danych węzłów**.
- **Układ węzła** — ile szczegółów pokazuje każdy wiersz listy węzłów.
- **Konfiguracja Wi-Fi dla mPWRD-OS** — zobacz [Połączenia](connections).
- **Filtry wiadomości** — ukrywa przychodzące wiadomości zawierające wymienione przez Ciebie słowa. Bez skonfigurowanych słów nic nie robi.
- **AI systemowe** — pojawia się tylko tam, gdzie dostępne są funkcje aplikacji; zobacz [Funkcje aplikacji](app-functions).

Sekcja **Uprawnienia** znajduje się nad sekcją **Aplikacja**, aby użytkownik, który pominął jakieś uprawnienie podczas pierwszego uruchomienia, mógł do niego wrócić. Pod nią jest sekcja **Informacje** z pozycjami **Pokaż wprowadzenie**, **Powiadomienia aplikacji**, **Ustawienia systemowe**, **O aplikacji** oraz wersją aplikacji. Ekran zamyka **Pomoc i dokumentacja**.

### O aplikacji

**Ustawienia → O aplikacji** zawiera informacje o samej aplikacji, a nie o węźle, w trzech sekcjach:

- **Czym jest Meshtastic?** — krótki opis projektu.
- **Aplikacje** — zaczyna się od **Potrzebujesz sprzętu?**, obracanej karuzeli popularnych urządzeń z odnośnikami do miejsc zakupu. Zawiera także repozytorium GitHub, uruchomioną wersję aplikacji oraz **Potwierdzenia** (zobacz następną sekcję).
- **Informacje o projekcie** — odnośniki do strony internetowej, tej dokumentacji oraz licencji GNU GPL v3, na której wydana jest aplikacja.

Stopka podaje informację o prawach autorskich oraz że aplikacja jest wolnym oprogramowaniem bez żadnej gwarancji, które możesz rozpowszechniać na tej samej licencji.

### Potwierdzenia

Dostępne z ekranu **O aplikacji**; wyświetla każdą bibliotekę open source dołączoną do aplikacji wraz z jej licencją, generowaną w czasie budowania przez AboutLibraries.

### Rozwiązywanie problemów ze zdalnym zarządzaniem

- **„Could not reach node — try again or move closer.”** Węzeł docelowy może być poza zasięgiem lub wyłączony albo nie ma Twojego klucza publicznego na liście kluczy administratora. Sprawdź wpisy **Klucz administratora** w węźle docelowym.
- **„Connect to a radio to administer remote nodes.”** Twój własny węzeł nie jest połączony. Połącz go ponownie na ekranie [Połączenia](connections).
- **Zmiany nie są stosowane** — niektóre ustawienia wymagają restartu, aby zaczęły działać. Po zapisaniu spróbuj akcji Restart.

## Powiązane tematy

- [Ustawienia — Radio i użytkownik](settings-radio-user) — podstawowe ustawienia węzła i profilu użytkownika
- [Opis konfiguracji modułów](https://meshtastic.org/docs/configuration/module) — szczegółowa dokumentacja modułów na meshtastic.org
- [FAQ](https://meshtastic.org/docs/faq/) — najczęstsze pytania na meshtastic.org
