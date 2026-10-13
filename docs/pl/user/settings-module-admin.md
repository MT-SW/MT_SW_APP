---
title: Ustawienia — Moduły i administracja
parent: Przewodnik użytkownika
nav_order: 8
last_updated: 2026-09-28
description: Konfiguracja opcjonalnych modułów funkcji (MQTT, telemetria, wiadomości predefiniowane, TAK i inne) oraz administrowanie urządzeniem.
aliases:
  - modules
  - module-config
  - administration
---

# Ustawienia — Moduły i administracja

Konfiguracja opcjonalnych modułów funkcji oraz administrowanie urządzeniem. Moduły rozszerzają MT_SW o wyspecjalizowane możliwości — każdy z nich można włączać i wyłączać niezależnie.

> 💡 **Wskazówka:** Włączaj tylko te moduły, z których naprawdę korzystasz. Wyłączenie nieużywanych modułów zmniejsza zajętość eteru, oszczędza baterię i upraszcza konfigurację. Oczekiwany moduł może nie być widoczny z trzech powodów: rola Twojego węzła go nie włącza, Twój firmware jest starszy niż wydanie, które go dodało, albo kompilacja firmware pomija go dla tego sprzętu.

Ustawienia modułów mają układ kart z przełącznikami, listami rozwijanymi, polami tekstowymi i suwakami:

![Przełącznik w pozycji włączonej](../../assets/screenshots/settings_switch.png)

![Lista rozwijana, rozwinięta i pokazująca listę opcji](../../assets/screenshots/settings_dropdown.png)

![Pole tekstowe z wpisaną wartością](../../assets/screenshots/settings_text_field.png)

![Karta ustawień modułu z tytułem i zgrupowanymi kontrolkami](../../assets/screenshots/settings_titled_card.png)

## Konfiguracja modułów

Każdy moduł znajdziesz w **Ustawienia → Ustawienia modułu** (Module configuration).

> ⚠️ **Ważne:** Zapisanie ekranu modułu uruchamia węzeł ponownie: przycisk nosi nazwę **Zapisz i uruchom ponownie**, a węzeł jest przez kilka sekund niedostępny. Wyjątkiem są Zewnętrzne Powiadomienie i Meshbeacon: ich przycisk nosi nazwę **Zapisz**. Zewnętrzne Powiadomienie może nadal restartować węzeł przy niektórych zmianach, natomiast zmiana w Meshbeacon zostaje zastosowana bez restartu.

### Moduł MQTT

Łączy wiadomości z sieci mesh z brokerem MQTT i odwrotnie, zapewniając dostęp do internetu. Dzięki temu możesz rozszerzyć swoją sieć poza zasięg LoRa lub zintegrować ją z systemami automatyki domowej.

| Ustawienie | Opis |
|---------|-------------|
| Włącz MQTT | Włącza lub wyłącza most MQTT |
| Adres | Adres brokera MQTT |
| Nazwa użytkownika | Nazwa użytkownika do uwierzytelniania |
| Hasło | Hasło do uwierzytelniania |
| Szyfrowanie włączone | Szyfruje dane przesyłane przez MQTT |
| Włącz wyjście JSON | Publikuje i odbiera wiadomości MQTT w formacie JSON. Oznaczone jako przestarzałe w schemacie protobuf, ale nadal jest to jedyny przełącznik tego zachowania i firmware wciąż go respektuje |
| TLS włączony | Używa bezpiecznego połączenia |
| Główny temat | Bazowa ścieżka tematu MQTT |
| Proxy do klienta włączone | Pozwala połączonej aplikacji przenosić ruch MQTT węzła, zamiast łączyć się z brokerem bezpośrednio z węzła |
| Proxy MQTT w tej aplikacji | Część tej samej funkcji po stronie aplikacji dla **Proxy do klienta włączone**: określa, czy ta aplikacja pełni rolę przekaźnika. Zobacz [MQTT](mqtt) |
| Raportowanie na mapie | Publikuje pozycję na publicznej mapie — zobacz następującą dalej grupę raportowania na mapie |

Włączenie **Raportowania na mapie** (Map reporting) odsłania kartę zgody z nagłówkiem *Zgoda na udostępnianie niezaszyfrowanych danych węzła za pośrednictwem protokołu MQTT*, a pod nią przełącznik **Wyrażam zgodę.** (I agree.). Reszta karty nie istnieje na ekranie, dopóki nie wyrazisz zgody:

| Ustawienie | Opis |
|---------|-------------|
| Wyrażam zgodę. | Wyraźna zgoda na przesyłanie danych Twojego węzła, w tym przybliżonej pozycji, bez szyfrowania. Bez niej raportowanie na mapie się nie rozpocznie |
| Suwak dokładności | Suwak nie ma własnej etykiety. Ustawia, jak zgrubnie publikowana jest Twoja pozycja, w zakresie od 12 do 15; linia pod nim pokazuje *±* wynikową odległość w Twoich jednostkach |
| Interwał publikowania na mapie | Jak często raportować. Lista rozwijana ze stałymi interwałami od 1 godziny do 72 godzin — nic krótszego nie jest oferowane |

Szczegółowy przewodnik, obejmujący szyfrowanie, prywatność i konfigurację brokera, znajdziesz w [MQTT](mqtt).

### Moduł portu szeregowego

Włącza komunikację przez port szeregowy dla integracji z zewnętrznymi urządzeniami (modułami GPS, czujnikami lub własnym sprzętem). Po włączeniu port szeregowy węzła może wysyłać i odbierać dane protobuf lub tekstowe, co pozwala zewnętrznym mikrokontrolerom lub komputerom współpracować z siecią mesh.

| Ustawienie | Opis |
|---------|-------------|
| Włącz tryb serial | Aktywuje komunikację szeregową |
| Włącz echo | Odsyła z powrotem odebrane dane szeregowe |
| Tryb serial | Jakim protokołem porozumiewa się port — Default, Simple, Proto, Text message, NMEA, CalTopo, WS85 weather station, VE.Direct, MeshSolar config, Log lub Log (text only) |
| RX / TX | Piny GPIO połączenia szeregowego |
| Prędkość transmisji | Szybkość portu |
| Limit czasu | Jak długo czekać, zanim przychodząca wiadomość zostanie uznana za kompletną |
| Nadpisz port szeregowy konsoli | Przejmuje port, z którego zwykle korzysta konsola debugowania |

### Moduł Zewnętrznego Powiadomienia

Steruje alertami brzęczyka, diody LED lub wibracji na sprzęcie Twojego węzła. Przydatny w urządzeniach, które mają fizycznie sygnalizować nadejście wiadomości — szczególnie pomocny w instalacjach bez nadzoru lub na zewnątrz.

Są dwa niezależne wyzwalacze — przychodząca **wiadomość** i odebrany znak **dzwonka** — a każdy z nich może osobno sterować diodą LED, brzęczykiem i silnikiem wibracyjnym, co daje sześć przełączników.

| Ustawienie | Opis |
|---------|-------------|
| Zewnętrzne powiadomienie włączone | Główny przełącznik modułu |
| Dioda / brzęczyk / wibracja wiadomości alarmowej | Które wyjścia reagują na przychodzącą wiadomość |
| Dioda / brzęczyk / wibracja dzwonka alarmowego | Które wyjścia reagują na odebrany znak dzwonka |
| Wyjście LED (GPIO) | Pin, do którego podłączona jest dioda LED |
| Wyjście LED aktywne wysokim stanem | Czy pin diody LED jest aktywny stanem wysokim, czy niskim |
| Wyjście buzzera (GPIO) | Pin, do którego podłączony jest brzęczyk |
| Wyjście silnika wibracyjnego (GPIO) | Pin, do którego podłączony jest silnik wibracyjny |
| Użyj buzzera PWM | Steruje brzęczykiem sygnałem PWM, co pozwala na różne tony zamiast jednej wysokości dźwięku |
| Użyj I2S jako buzzera | Kieruje alert na wyjście audio I2S |
| Czas trwania (w milisekundach) | Jak długo trwa pojedynczy alert |
| Czas przypomnienia (sekundy) | Powtarza alert przez ten czas, aż zostanie potwierdzony. 0 wyłącza przypominanie |
| Dzwonek | Melodia odtwarzana na brzęczyku PWM, w formacie RTTTL. Można ją zaimportować z pliku |

### Moduł Store & Forward

Buforuje wiadomości dla węzłów, które były chwilowo offline, a następnie odtwarza je, gdy te węzły ponownie się połączą. Niezbędny w sieciach, w których węzły regularnie wchodzą w zasięg i z niego wychodzą — dba o to, aby wiadomości nie ginęły podczas krótkich rozłączeń.

| Ustawienie | Opis |
|---------|-------------|
| Store & Forward włączony | Aktywuje funkcję store and forward |
| Bicie serca | Okresowo ogłasza możliwości store-and-forward tego węzła |
| Liczba rekordów | Maksymalna liczba przechowywanych wiadomości |
| Maks. zwracana historia | Maksymalna liczba wiadomości do odtworzenia |
| Okno zwracanej historii | Okno czasowe odtwarzania |
| Serwer | Pełni rolę serwera store-and-forward dla sieci (wymaga dużej ilości pamięci, np. ESP32 z PSRAM) |

> 💡 **Wskazówka:** Store and Forward działa najlepiej na węzłach z dużą ilością pamięci (ESP32 z PSRAM). Węzły Router są idealnymi kandydatami, ponieważ zwykle pracują cały czas.

### Moduł Testu zasięgu

> ⚠️ **Ostrzeżenie:** Test zasięgu działa tylko na zabezpieczonym kanale głównym. Dopóki Twój kanał główny nadal używa domyślnego klucza kanału, kontrolki interwału i CSV pozostają wyłączone — nadal możesz wyłączyć już włączony moduł — a zapisanie wymusza wyłączenie modułu, jeśli kanał wrócił do stanu publicznego.

Zautomatyzowane narzędzie do testowania zasięgu, służące do oceny jakości łącza między węzłami. Po włączeniu węzeł okresowo wysyła wiadomości testowe z rosnącymi licznikami. Węzeł odbierający zapisuje te wiadomości, dzięki czemu możesz odejść lub odjechać, a potem przeanalizować, przy jakiej odległości wiadomości przestały docierać.

| Ustawienie | Opis |
|---------|-------------|
| Test zasięgu włączony | Aktywuje test zasięgu |
| Interwał wiadomości nadawcy (sekundy) | Czas między transmisjami testowymi, wybierany z listy rozwijanej stałych interwałów |
| Zapisz .CSV w pamięci (tylko ESP32) | Zapisuje odebrane dane testowe w systemie plików samego węzła. Tylko sprzęt ESP32 |

### Moduł Telemetrii

Określa, jakie dane telemetryczne Twój węzeł udostępnia w sieci mesh. Telemetria obejmuje kondycję urządzenia (bateria, czas pracy) oraz dane z czujników środowiskowych (temperatura, wilgotność, ciśnienie).

Każda z czterech grup metryk ma własny przełącznik włączenia i własny interwał, więc możesz raportować stan baterii często, a odczyty czujników rzadko.

| Ustawienie | Opis |
|---------|-------------|
| Wysyłaj telemetrię urządzenia (Send Device Telemetry) | Główny przełącznik metryk urządzenia. Widoczny tylko w firmware 2.7.12 i nowszym |
| Interwał aktualizacji metryk urządzenia | Jak często raportować baterię, czas pracy i wykorzystanie kanału |
| Moduł metryk środowiskowych włączony | Raportuje podłączone czujniki środowiskowe |
| Interwał aktualizacji metryk środowiskowych | Jak często je raportować |
| Metryki środowiskowe na ekranie włączone | Pokazuje te odczyty także na własnym wyświetlaczu urządzenia |
| Metryki środowiskowe w Fahrenheitach | Używa °F na wyświetlaczu urządzenia. Dotyczy wyłącznie ekranu węzła — aplikacja stosuje ustawienia regionalne telefonu, zobacz [Jednostki i lokalizacja](units-and-locale) |
| Włącz moduł metryk jakości powietrza | Raportuje dane z czujników pyłów i CO₂ |
| Czas aktualizacji metryk jakości powietrza | Jak często je raportować |
| Włącz moduł metryk zasilania | Raportuje odczyty napięcia i prądu dla poszczególnych kanałów |
| Czas aktualizacji metryk zasilania | Jak często je raportować |
| Wyświetlaj metryki zasilania na ekranie | Pokazuje odczyty zasilania także na wyświetlaczu urządzenia |

Obsługiwane czujniki i zalecenia konfiguracyjne znajdziesz w [Telemetria i czujniki](telemetry-and-sensors).

### Moduł Wiadomości predefiniowanych

Wcześniej przygotowane wiadomości dostępne z fizycznych przycisków węzła (w węzłach z enkoderami obrotowymi, klawiaturami lub podobnym sprzętem wejściowym). Zdefiniuj listę wiadomości do szybkiego wysłania, które można nadać bez podłączonego telefonu — idealne do pracy w terenie.

| Ustawienie | Opis |
|---------|-------------|
| ~~Wiadomości predefiniowane włączone~~ | ⚠️ **Przestarzałe** w schemacie protobuf |
| Wiadomości | Lista wiadomości rozdzielonych znakami nowej linii |
| Wyślij dzwonek | Wysyła znak dzwonka razem z wiadomością, aby mógł zabrzmieć moduł Zewnętrznego Powiadomienia w węźle odbierającym |
| Enkoder obrotowy #1 włączony | Używa enkodera obrotowego jako urządzenia wejściowego |
| Pin GPIO dla portu A / B / Press enkodera obrotowego | Trzy piny, do których podłączony jest enkoder |
| Generuj zdarzenie wejściowe przy Press / CW / CCW | Jakie zdarzenie klawisza wywołuje każda czynność enkodera |
| Wejście Góra/Dół/Wybierz włączone | Osobny, prostszy schemat wejścia z użyciem przycisków góra/dół/wybierz zamiast enkodera |
| ~~Zezwól na źródło wejścia~~ | ⚠️ **Przestarzałe** w schemacie protobuf |

### Moduł Dźwięku

Obsługa dźwięku Codec2 do komunikacji głosowej o niskiej przepustowości przez sieć mesh. To funkcja **eksperymentalna**, która koduje głos w bardzo małe pakiety danych za pomocą kodeka Codec2.

| Ustawienie | Opis |
|---------|-------------|
| CODEC 2 włączony | Aktywuje moduł dźwięku |
| Częstotliwość próbkowania CODEC2 | Kompromis między jakością dźwięku a przepustowością |
| Pin PTT (Push-To-Talk) | Pin GPIO przycisku push-to-talk |
| I2S wybór słowa | Pin GPIO dla I2S WS |
| I2S dane wejściowe | Pin GPIO dla I2S DIN |
| I2S dane wyjściowe | Pin GPIO dla I2S DOUT |
| Zegar I2S | Pin GPIO dla zegara bitowego I2S |

> ℹ️ **Uwaga:** Dźwięk wymaga specjalnego sprzętu (mikrofon i głośnik I2S). Jakość głosu jest bardzo niska ze względu na małą przepustowość — pomyśl o „zrozumiałym głosie radiowym”, a nie o jakości rozmowy telefonicznej.

### Moduł Zdalnego sprzętu

Sterowanie GPIO przez sieć mesh. Pozwala zdalnemu węzłowi odczytywać lub zapisywać piny GPIO w innym węźle — przydatne do włączania przekaźników, odczytu przełączników lub sterowania zewnętrznym sprzętem na odległość.

> ⚠️ **Ostrzeżenie:** Włączenie opcji **Zezwól na dostęp do niezdefiniowanych pinów** daje zdalnym węzłom dostęp do wszystkich pinów GPIO, co może zakłócić pracę własnego sprzętu węzła. Włączaj ją tylko w węzłach przeznaczonych wyłącznie do obsługi GPIO.

| Ustawienie | Opis |
|---------|-------------|
| Zdalny sprzęt włączony | Aktywuje zdalny dostęp do GPIO |
| Zezwól na dostęp do niezdefiniowanych pinów | Zezwala na dostęp do dowolnego pinu GPIO (ryzyko dla bezpieczeństwa) |
| Dostępne piny | Do 4 pinów GPIO, które ten węzeł udostępnia do zdalnego odczytu/zapisu |

### Moduł Informacji o sąsiadach

Rozgłasza informacje o bezpośrednio słyszanych sąsiadach, umożliwiając tworzenie mapy topologii sieci mesh. Każdy włączony węzeł okresowo udostępnia listę innych węzłów, które słyszy, wraz z jakością ich sygnału.

| Ustawienie | Opis |
|---------|-------------|
| Włącz informacje o sąsiadach | Aktywuje rozgłaszanie informacji o sąsiadach |
| Interwał aktualizacji | Jak często rozgłaszać listę sąsiadów |
| Nadaj przez LoRa | Rozgłasza informacje o sąsiadach także przez LoRa, a nie tylko przez MQTT/telefon. Niedostępne na kanale używającym domyślnego klucza i nazwy |

Jak wykorzystać dane o sąsiadach do badania topologii sieci mesh, opisano w [Odkrywanie lokalnej sieci mesh](discovery).

### Moduł Oświetlenia otoczenia

Steruje wbudowanymi diodami NeoPixel lub innymi adresowalnymi diodami RGB na obsługiwanym sprzęcie. Może służyć do wizualnych wskaźników stanu, świateł powiadomień lub efektów dekoracyjnych.

| Ustawienie | Opis |
|---------|-------------|
| Stan diody LED | Włącza lub wyłącza diodę LED |
| Prąd | Limit prądu diody LED (0–31) |
| Czerwony / Zielony / Niebieski | Wartości poszczególnych kanałów koloru (0–255) |

### Moduł Czujnika detekcji

Zamienia Twój węzeł w system alarmowy czujnika ruchu lub otwarcia drzwi. Gdy pin GPIO wykryje zmianę stanu (wykryty ruch, otwarte drzwi), węzeł rozgłasza w sieci mesh wiadomość alarmową.

| Ustawienie | Opis |
|---------|-------------|
| Czujnik detekcji włączony | Aktywuje czujnik detekcji |
| Pin GPIO do monitorowania | Pin GPIO podłączony do czujnika |
| Typ wyzwalacza wykrywania | Jak stan pinu przekłada się na zdarzenie wykrycia (np. aktywny stan wysoki/niski, wyzwalanie zboczem) |
| Użyj trybu INPUT_PULLUP | Włącza wewnętrzny rezystor podciągający pinu |
| Minimalny czas transmisji (sekundy) | Minimalny czas między rozgłaszaniem alarmów |
| Nadawanie stanu (sekundy) | Interwał okresowego rozgłaszania stanu |
| Wyślij dzwonek z wiadomością alarmową | Dołącza znak dzwonka do alarmów |
| Przyjazna nazwa | Własna nazwa tego czujnika |

### Moduł Paxcounter

Licznik osób wykorzystujący zapytania sondujące Wi-Fi i BLE. Zlicza pobliskie urządzenia, nasłuchując pasywnie zapytań sondujących, które telefony i laptopy wysyłają podczas wyszukiwania sieci. Dostępny tylko na urządzeniach ESP32.

| Ustawienie | Opis |
|---------|-------------|
| Paxcounter włączony | Aktywuje zliczanie osób |
| Interwał aktualizacji | Jak często raportować liczniki |
| Próg WiFi (dBm) | Ignoruje zapytania Wi-Fi słabsze niż ta wartość, aby nie liczyć odległych urządzeń (domyślnie −80 dBm) |
| Próg BLE (dBm) | Taka sama granica dla ogłoszeń BLE (domyślnie −80 dBm) |

> 💡 **Wskazówka:** Paxcounter jest przydatny do szacowania natężenia ruchu pieszego na początku szlaków, w miejscach wydarzeń i innych lokalizacjach. Liczby są przybliżone — jedna osoba może mieć przy sobie kilka urządzeń.

### Moduł Wiadomości statusu

Wiadomość statusu nie ma własnego ekranu modułu. Edytuje się ją razem z pozostałymi danymi tożsamości węzła, w [Ustawienia — Radio i użytkownik](settings-radio-user#user-profile).

### Moduł Meshbeacon

Rozgłasza zaproszenie do Twojej sieci mesh i odbiera zaproszenia od innych. Pozycja pojawia się tylko w węzłach z firmware 2.8.0 lub nowszym. Pełny opis krok po kroku znajdziesz w [Odkrywanie lokalnej sieci mesh](discovery).

### Moduł TAK

Integracja z Team Awareness Kit zapewniająca współpracę z ATAK i WinTAK. Aby pozycja pojawiła się na liście modułów, muszą być spełnione dwa warunki: węzeł pracuje na firmware 2.8.0 lub nowszym, a jego **Rola urządzenia** (Device Role) w **Ustawienia → Konfiguracja urządzenia → Urządzenie** jest ustawiona na `TAK` lub `TAK_TRACKER`. Szczegółową konfigurację i sposób użycia opisano w [Integracja TAK](tak).

## Zarządzanie

### Zdalne zarządzanie

Zdalna konfiguracja węzłów, które mają ten sam klucz administratora co Twój:

1. Wybierz docelowy węzeł na liście węzłów.
2. Przejdź do **Ustawienia** tego węzła.
3. Zmień konfigurację.
4. Dotknij **Zapisz** — zmiany zostaną wysłane przez sieć mesh.

> ⚠️ **Ważne:** Wymaga skonfigurowanego klucza administratora zarówno w Twoim węźle, jak i w węźle docelowym.

### Akcje urządzenia

**Ustawienia → Zarządzanie** zawiera pięć jednorazowych akcji, z których każda wymaga potwierdzenia w oknie dialogowym:

| Akcja | Co robi |
|---|---|
| Ustaw czas | Wysyła zegar Twojego telefonu do węzła |
| Restart | Uruchamia węzeł ponownie |
| Wyłącz | Wyłącza zasilanie węzła |
| Ustawienia fabryczne | Przywraca wszystkim ustawieniom wartości fabryczne |
| Zresetuj NodeDB | Czyści bazę węzłów. To okno dialogowe zawiera przełącznik **Zachować ulubione?** (Preserve Favorites?) |

> ⚠️ **Ostrzeżenie:** Ustawienia fabryczne usuwają wszystkie ustawienia, kanały i klucze i nie można tego cofnąć. Przed resetem użyj **Eksport konfiguracji**, aby zapisać ustawienia węzła, oraz **Kopia kluczy** na ekranie Bezpieczeństwo, aby zapisać jego klucze, dzięki czemu będziesz mógł przywrócić jedno i drugie.

### Kopia zapasowa i przywracanie

**Ustawienia → Kopia zapasowa i przywracanie** zapisuje całą konfigurację połączonego węzła do pliku za pomocą **Eksport konfiguracji** i wczytuje zapisany plik z powrotem za pomocą **Import konfiguracji**. Ustawienia Traffic Management są eksportowane, ale nie są stosowane przy imporcie. Wykonaj eksport przed przywróceniem ustawień fabrycznych lub aby skopiować konfigurację jednego węzła na drugi. Ta sekcja jest widoczna tylko dla Twojego własnego węzła, nie przez zdalne zarządzanie.

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

Blok **Aplikacja** (App Settings) na ekranie **Ustawienia** konfiguruje aplikację, a nie węzeł, i pojawia się tylko wtedy, gdy wybrany jest Twój własny węzeł. Jest pogrupowany, a nie płaski:

**Prywatność**

- **Zezwalaj na analizę i raportowanie awarii.** — zgoda lub rezygnacja z diagnostyki.
- **Podaj lokalizację telefonu do sieci** — udostępnia pozycję tego telefonu, gdy węzeł nie ma ustalonej pozycji GPS. Wymaga dokładnej lokalizacji: jeśli dozwolona jest tylko przybliżona, jej włączenie poprosi o dokładną.
- **Homoglyph encoding** — jak traktowane są podobnie wyglądające znaki w nazwach.

**Wygląd**

- **Jednostki** — metryczne, imperialne lub zgodne z systemem. To pozycja, do której odsyła [Jednostki i lokalizacja](units-and-locale).
- **Motyw** i **Język**.
- **Wyświetlaj pełne znaczniki czasu**.

**Trwałość danych**

- Limit pamięci podręcznej określający, ile historii jest przechowywane.
- **Eksportuj pakiety zasięgu**, **Eksportuj wszystkie pakiety** i **Eksportuj bazę danych węzłów**.

**Pozycje samodzielne**

- **Układ węzła** — ile szczegółów pokazuje każdy wiersz listy węzłów.
- **Konfiguracja Wi-Fi dla mPWRD-OS** — zobacz [Połączenia](connections).
- **Filtry wiadomości** — ukrywa przychodzące wiadomości zawierające wymienione przez Ciebie słowa. Bez skonfigurowanych słów nic nie robi.
- **AI systemowe** — pojawia się tylko tam, gdzie dostępne są funkcje aplikacji; zobacz [Funkcje aplikacji](app-functions).

Blok **Uprawnienia** znajduje się nad Ustawieniami aplikacji, aby użytkownik, który pominął jakieś uprawnienie podczas pierwszego uruchomienia, mógł do niego wrócić, a **Informacje o aplikacji** (App Info) znajdują się pod nim.

### O aplikacji

**Ustawienia → O aplikacji** zawiera informacje o samej aplikacji, a nie o węźle:

Trzy sekcje:

- **Czym jest Meshtastic?** — krótki opis projektu.
- **Aplikacje** — zaczyna się od **Potrzebujesz sprzętu?**, obracanej karuzeli popularnych urządzeń z odnośnikami do miejsc zakupu. Zawiera także repozytorium GitHub, uruchomioną wersję aplikacji oraz **Potwierdzenia** (zobacz następną sekcję).
- **Informacje o projekcie** — odnośniki do strony internetowej, tej dokumentacji oraz licencji GNU GPL v3, na której wydana jest aplikacja.

Stopka podaje informację o prawach autorskich oraz że aplikacja jest wolnym oprogramowaniem bez żadnej gwarancji, które możesz rozpowszechniać na tej samej licencji.

### Potwierdzenia

Dostępne z ekranu **O aplikacji**; wyświetla każdą bibliotekę open source dołączoną do aplikacji wraz z jej licencją, generowaną w czasie budowania przez AboutLibraries. Wcześniej nazywało się to ekranem licencji.

### Rozwiązywanie problemów ze zdalnym zarządzaniem

- **„No response from target node”** — węzeł docelowy może być poza zasięgiem, offline lub mieć niezgodny klucz administratora. Sprawdź, czy klucz administratora jest taki sam w obu węzłach.
- **Zmiany nie są stosowane** — niektóre ustawienia wymagają restartu, aby zaczęły działać. Po zapisaniu spróbuj akcji Restart.
- **Nie widać ustawień zdalnych** — upewnij się, że Twój węzeł ma klucz administratora węzła docelowego. Kanał administracyjny jest konfigurowany automatycznie po ustawieniu klucza administratora.

## Powiązane tematy

- [Ustawienia — Radio i użytkownik](settings-radio-user) — podstawowe ustawienia węzła i profilu użytkownika
- [Opis konfiguracji modułów](https://meshtastic.org/docs/configuration/module) — szczegółowa dokumentacja modułów na meshtastic.org
- [FAQ](https://meshtastic.org/docs/faq/) — najczęstsze pytania na meshtastic.org
