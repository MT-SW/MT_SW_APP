---
title: Ustawienia — Radio i użytkownik
parent: Przewodnik użytkownika
nav_order: 7
last_updated: 2026-09-28
description: Konfiguracja sprzętu węzła, presetów LoRa, profilu użytkownika, udostępniania pozycji, zarządzania energią i bezpieczeństwa.
aliases:
  - settings
  - radio-config
  - user-config
  - lora
---

# Ustawienia — Radio i użytkownik

Konfiguracja tożsamości użytkownika Twojego węzła, regionu i parametrów LoRa, zachowania pozycji i zasilania, łączności sieciowej i Bluetooth oraz ustawień bezpieczeństwa.

## Jak działają te ekrany

Wszystko znajduje się na ekranie **Ustawienia**. **Użytkownik**, **LoRa**, **Kanały** i **Bezpieczeństwo** są wymienione bezpośrednio na nim. **Urządzenie**, **Pozycjonowanie**, **Zasilanie**, **Sieć**, **Wyświetlacz** i **Bluetooth** znajdują się poziom niżej, w **Ustawienia → Konfiguracja urządzenia**. **Sieć** pojawia się tylko w węzłach z Wi-Fi lub Ethernetem, a **Bluetooth** tylko w węzłach z Bluetooth.

Ustawienia korzystają ze standardowych kontrolek — list rozwijanych, przełączników i suwaków:

| Kontrolka | Zrzut ekranu |
|---------|------------|
| Lista rozwijana | ![Lista rozwijana, rozwinięta i pokazująca listę opcji](../../assets/screenshots/settings_dropdown.png) |
| Przełącznik | ![Przełącznik w pozycji włączonej](../../assets/screenshots/settings_switch.png) |
| Suwak | ![Suwak z pokazaną bieżącą wartością liczbową](../../assets/screenshots/settings_slider.png) |

## Ustawienia użytkownika

### Profil użytkownika

W **Ustawienia → Użytkownik**.

| Ustawienie | Opis |
|---------|-------------|
| Długa nazwa | Twoja wyświetlana nazwa (do 39 znaków) |
| Krótka nazwa | Skrócona nazwa, 4 znaki |
| Wiadomość statusu | Krótki, publiczny, dowolny status wyświetlany przez inne węzły obok Twojego węzła — do 80 bajtów, usuwany za pomocą **✕** w polu. Węzeł rozgłasza go w sieci mesh po zmianie i ponownie co 12 godzin. Wymaga firmware 2.8 lub nowszego, w innym przypadku jest niedostępny |
| Nie przyjmuje wiadomości | Oznacza węzeł jako taki, do którego nikt nie powinien próbować pisać — dla węzła bez nadzoru lub węzła infrastrukturalnego. Inni klienci ukrywają go na liście kontaktów. Wymaga obsługującego to firmware |
| Radioamator licencjonowany (Ham) | Włącz, jeśli posiadasz licencję na amatorski węzeł (pozwala na większą moc). Włączenie wymaga potwierdzenia w oknie dialogowym. We własnym węźle zmienia wtedy etykietę **Długa nazwa** na **Znak wywoławczy** i dodaje osobne pole Długiej nazwy; przez zdalne zarządzanie pole pozostaje **Długa nazwa** |

### Stosowanie zmian

Stopka pojawia się, gdy tylko coś zmienisz. **Odrzuć** porzuca zmianę, a drugi przycisk zapisuje ją w węźle: nosi nazwę **Zapisz i uruchom ponownie** na ekranach, które firmware stosuje z restartem — Pozycjonowanie, Sieć, Bluetooth, Bezpieczeństwo i większość ekranów modułów — oraz **Zapisz** wszędzie indziej.

Wiadomość statusu jest zapisywana tym samym przyciskiem **Zapisz**, ale nigdy nie restartuje węzła — i, tak jak reszta tego ekranu, może być edytowana na zdalnym węźle, którym zarządzasz. Dla własnego węzła, gdy jest połączony, istnieje skrót: dotknij i przytrzymaj swój węzeł na [liście węzłów](nodes.md) i wybierz **Status aktualizacji**. Starsze firmware i rozłączony węzeł nie mają skrótu — nadal można skorzystać z samego pola.

## Konfiguracja

### Konfiguracja urządzenia

W **Ustawienia → Konfiguracja urządzenia → Urządzenie**.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Rola urządzenia | Zachowanie węzła. Lista wyboru zawiera nazwy z firmware (`CLIENT`, `ROUTER`, `ROUTER_LATE`, `TAK` i tak dalej), a opis wybranej roli pojawia się pod polem. Wybór `ROUTER` lub `ROUTER_LATE` prosi o potwierdzenie, że przeczytano wskazówki dotyczące ról urządzeń | `CLIENT` |
| Tryb retransmisji (Rebroadcast Mode) | Jak węzeł retransmituje wiadomości. Podobnie jak w przypadku roli, lista wyboru zawiera nazwy z firmware i opisuje tylko wybraną | `ALL` |
| Interwał rozgłaszania informacji o węźle (Node Info Broadcast Interval) | Jak często węzeł ponownie się ogłasza. Lista rozwijana ze stałymi interwałami — Nieustawiony, potem od 3 do 72 godzin — a nie wartość wpisywana w sekundach | 3 godziny |
| Podwójne dotknięcie jako naciśnięcie przycisku | Traktuje podwójne dotknięcie jak naciśnięcie przycisku | Wyłączone |
| Potrójne kliknięcie: Ping adhoc | Wysyła doraźny ping z pozycją po potrójnym kliknięciu | Włączone |
| LED bicia serca | Okresowo miga diodą stanu | Włączone |
| Strefa czasowa (Time Zone) | Ciąg strefy czasowej POSIX dla zegara urządzenia, z przyciskami do skopiowania strefy telefonu lub jej wyczyszczenia | — |
| Przycisk GPIO / Buzzer GPIO | Zaawansowane: do jakich pinów podłączone są przycisk i brzęczyk | — |

### Konfiguracja LoRa

W **Ustawienia → LoRa**.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Region | Region regulacyjny określający pasma częstotliwości. Musisz go ustawić przed nadawaniem. W firmware 2.8 lub nowszym pierwsze ustawienie regionu tworzy także klucz tożsamości węzła i nadaje mu nowy numer węzła | Nieustawiony (trzeba skonfigurować) |
| Presety | Kompromis między szybkością a zasięgiem | LongFast |
| Liczba skoków | Maksymalna liczba skoków retransmisji | 3 |
| Moc nadawania (dBm) | Moc nadawania; 0 = maksimum dozwolone w regionie | 0 (maks. regionu) |
| Wymuszenie częstotliwości | Całkowicie zastępuje obliczoną częstotliwość pracy (MHz). Nie jest to przesunięcie względem wartości obliczonej — zostaw 0, chyba że wiesz, że potrzebujesz konkretnej częstotliwości | 0 (użyj obliczonej) |
| Użyj presetu | Domyślnie włączone. Wyłącz, aby ustawić Współczynnik rozproszenia, Szybkość kodowania i Pasmo ręcznie, zamiast brać je z presetu modemu | Włączone |
| Nadpisanie współczynnika kodowania (Coding Rate Override) | Tylko w trybie presetu, firmware 2.7.18 lub nowszy: podnosi szybkość kodowania presetu, aby uzyskać większą korekcję błędów, zachowując jego pasmo i współczynnik rozproszenia. Oferowane są tylko wartości wyższe od własnej wartości presetu, a preset, który już ma 4/8, nie ma żadnej. Węzły na tym samym presecie nadal się słyszą, ale każdy pakiet trwa dłużej w eterze i zużywa więcej duty cycle. Przełączenie na preset, który już spełnia nadpisanie, resetuje je do wartości domyślnej presetu | Domyślna presetu |
| Współczynnik rozproszenia | Tylko tryb ręczny: 5–12. Wyższy daje większy zasięg, ale wolniej. W radiach SX127x (RF95) firmware nie przyjmuje 5 ani 6 i używa 11 | Z presetu |
| Szybkość kodowania | Tylko tryb ręczny: 5–8. Większa redundancja kosztuje czas w eterze | Z presetu |
| Pasmo | Tylko tryb ręczny: szerokość pasma kanału w kHz, wpisywana bezpośrednio. W regionie 2.4 GHz aplikacja oferuje zamiast tego listę pasm obsługiwanych przez Twój węzeł, a zapisana wartość spoza tej listy jest pokazywana jako *Nieobsługiwane* i blokuje zapis, dopóki nie wybierzesz obsługiwanej | Z presetu |
| Slot częstotliwości | Którego slotu w paśmie regionu użyć. 0 wyznacza go na podstawie nazwy kanału głównego | 0 (automatycznie) |
| Nadawanie włączone (Transmit Enabled) | Wyłączenie sprawia, że węzeł tylko odbiera | Włączone |
| Pomiń limit czasu nadawania | Ignoruje limit duty cycle regionu. W większości regionów nielegalne; włączaj tylko tam, gdzie pozwala na to Twoja licencja | Wyłączone |
| Ignoruj MQTT | Odrzuca pakiety, które dotarły z MQTT, a nie drogą radiową. Firmware włącza to za Ciebie, ilekroć ustawisz region z limitem duty cycle — pasma UE, Tajlandia i Ukraina 433 | Wyłączone, dopóki nie ustawisz regionu z limitem duty cycle |
| Ok dla MQTT | Pozwala bramom przekazywać Twoje pakiety do MQTT | Wyłączone |
| Wzmocniony zysk RX | Dodatkowe wzmocnienie odbioru w radiach SX126x; zużywa nieco więcej prądu | Wyłączone |
| Wentylator PA wyłączony | Wyłącza wentylator wzmacniacza mocy w sprzęcie, który go ma | Wyłączone |

Lista presetów jest również filtrowana do tego, na co pozwala prawo w Twoim regionie, więc preset dozwolony w jednym regionie nie pojawia się w innym. Zmiana regionu może więc sprawić, że Twój obecny preset stanie się niedozwolony — aplikacja naprawia to za Ciebie, przełączając na domyślny preset danego regionu, zamiast zostawiać nieużyteczne ustawienie. Sama lista regionów jest filtrowana według tego, co obsługuje Twój firmware, więc nowsze regiony pojawiają się dopiero w wersji 2.8 lub nowszej, obok regionu, który węzeł już ma ustawiony.

Niektóre regiony to przydziały dla węzłów amatorskich, których presetów mogą używać tylko licencjonowani operatorzy. W firmware 2.8 lub nowszym aplikacja wie, które to regiony, i wyszarza całą listę **Presetów**, dopóki nie włączysz **Radioamator licencjonowany (Ham)** dla konfigurowanego węzła; tekst pod polem informuje o tym, gdy jest wyszarzona.

> ⚠️ **Ważne:** Praca bez prawidłowego regionu może naruszać lokalne przepisy radiowe. Szczegóły znajdziesz w [przewodniku konfiguracji regionu](https://meshtastic.org/docs/getting-started/initial-config) na meshtastic.org.

### Presety modemu

Presety Lite, Narrow, Medium Turbo i Tiny wymagają firmware 2.8 lub nowszego — aplikacja ukrywa je w starszych węzłach.

> 💡 **Wskazówka:** Wartości **Limitu SNR** są ujemne celowo. LoRa potrafi dekodować sygnały *poniżej* poziomu szumu, więc bardziej ujemny limit oznacza, że preset toleruje słabszy, bardziej zaszumiony sygnał (większy zasięg). Pełne wyjaśnienie znajdziesz w [Jak działa miernik sygnału](signal-meter).

| Preset | Zasięg | Szybkość | Limit SNR | Najlepszy do |
|--------|-------|-------|-----------|----------|
| Short Turbo | ~1 km | 21,9 kbps | −7,5 dB | Gęsta zabudowa miejska z widocznością bezpośrednią; aplikacje wymagające dużej ilości danych |
| Short Fast | ~3 km | 10,9 kbps | −7,5 dB | Osiedla miejskie; budynki w odległości kilku przecznic |
| Short Slow | ~5 km | 6,25 kbps | −10 dB | Przedmieścia, krótki zasięg; umiarkowana gęstość zabudowy |
| Medium Fast | ~5 km | 3,52 kbps | −12,5 dB | Przedmieścia; umiarkowana gęstość zabudowy |
| Medium Slow | ~8 km | 1,95 kbps | −15 dB | Przedmieścia/wieś; umiarkowany zasięg przy mniejszej szybkości |
| Long Turbo | ~10 km | 1,34 kbps | −17,5 dB | Zasięg podobny do Long Fast, ale z pasmem 500 kHz; większa przepustowość |
| Long Fast | ~10 km | 1,1 kbps | −17,5 dB | **Zastosowanie ogólne (domyślny)** — zrównoważony zasięg i szybkość |
| Long Moderate | ~20 km | 0,34 kbps | −17,5 dB | Tereny wiejskie z pewnymi przeszkodami terenowymi; sporadyczne użycie |
| Lite Fast | ~5 km | 1,76 kbps | −12,5 dB | Pasmo SRD 866 MHz w UE (pasmo 125 kHz); porównywalny z Medium Fast |
| Lite Slow | ~10 km | 0,98 kbps | −15 dB | Pasmo SRD 866 MHz w UE (pasmo 125 kHz); porównywalny z Long Fast |
| Narrow Fast | ~5 km | 2,28 kbps | −7,5 dB | Pasmo 868 MHz w UE (pasmo 62,5 kHz); unika zakłóceń z innymi urządzeniami |
| Narrow Slow | ~10 km | 1,30 kbps | −10 dB | Pasmo 868 MHz w UE (pasmo 62,5 kHz); porównywalny z Long Fast |
| Medium Turbo | ~5 km | 7,0 kbps | −12,5 dB | Jak Medium Fast, ale z pasmem 500 kHz; nie jest legalny w każdym regionie. Wymaga firmware 2.8 lub nowszego |
| Tiny Fast | ~10 km | 0,68 kbps | −7,5 dB | Pasma amatorskie z limitem zajmowanej szerokości pasma; te presety używają 15,6 kHz. Wymaga firmware 2.8 lub nowszego, radia SX126x lub SX127x oraz TCXO o dokładności ±5 ppm lub lepszej |
| Tiny Slow | ~20 km | 0,33 kbps | −10 dB | Te same ograniczenia pasma co Tiny Fast, większy zasięg. Te same wymagania co do firmware, węzła i TCXO |
| ~~Long Slow~~ | ~30 km | 0,18 kbps | −20 dB | ⚠️ **Przestarzały** — nadal można go wybrać, ale może zostać usunięty w przyszłym wydaniu firmware |
| ~~Very Long Slow~~ | ~40+ km | 0,09 kbps | −20 dB | ⚠️ **Przestarzały** — nadal można go wybrać, ale może zostać usunięty w przyszłym wydaniu firmware |

> ℹ️ **Uwaga:** Ta tabela używa powszechnych krótkich nazw angielskich. Lista rozwijana **Presety** w aplikacji pokazuje nazwy polskie z angielską nazwą w nawiasie i prefiksem szerokości pasma, np. „Szeroki daleki szybki (Long Fast)” czy „Wąski szybki (Narrow Fast)”. Odkrywanie lokalnej sieci mesh pokazuje te same presety jako *Long Fast* i *Short Turbo*.

#### Wybór presetu modemu

Preset modemu określa podstawowy kompromis między **zasięgiem** a **szybkością transmisji danych**:

- **Wolniejsze presety** używają większego rozproszenia, dzięki czemu sygnały da się zdekodować przy słabszych poziomach sygnału (niższy limit SNR). Oznacza to większy zasięg, ale mniej bajtów na sekundę.
- **Szybsze presety** upakowują więcej danych w jednej transmisji, ale do zdekodowania wymagają silniejszego sygnału.

**Praktyczne wskazówki:**

- **Sieć miejska (wiele węzłów, małe odległości):** Użyj **Long Fast** (domyślny) lub **Short Fast**. Większa szybkość oznacza mniejsze zatłoczenie eteru, gdy wiele węzłów dzieli kanał.
- **Sieć wiejska/rzadka (mało węzłów, duże odległości):** Użyj **Long Moderate**. Zasięg jest ważniejszy niż szybkość, gdy węzły są daleko od siebie.
- **Zgodność z przepisami UE w paśmie 866/868 MHz:** Użyj **Lite Fast**, **Lite Slow**, **Narrow Fast** lub **Narrow Slow** — są zoptymalizowane pod pasma SRD/868 MHz w UE i mają węższe pasma.
- **Stałe łącza infrastrukturalne:** Użyj **Short Turbo** lub **Long Turbo** do dedykowanych łączy punkt-punkt z dobrymi antenami i widocznością bezpośrednią.
- **Środowiska mieszane:** Zostań przy **Long Fast** — to domyślny wybór społeczności i zapewnia zgodność z innymi w Twojej okolicy.

Wszystkie węzły na tym samym kanale muszą używać tego samego presetu modemu. Węzły z niezgodnymi presetami nie mogą się komunikować, nawet jeśli mają tę samą częstotliwość i klucz szyfrowania.

Szacunki zasięgu w tabeli [Presety modemu](#modem-presets) zakładają płaski teren i skromne anteny. Przewaga wysokości (szczyt wzgórza, dach) dramatycznie zwiększa efektywny zasięg. Dobrze ustawiony Router z Long Fast często może przewyższyć węzeł na poziomie gruntu z Long Slow.

### Konfiguracja wyświetlacza

W **Ustawienia → Konfiguracja urządzenia → Wyświetlacz**. Te ustawienia dotyczą **własnego ekranu węzła**, a nie aplikacji.

| Ustawienie | Opis |
|---------|-------------|
| Ekran włączony na | Jak długo wyświetlacz pozostaje podświetlony, zanim przejdzie w stan uśpienia |
| Interwał "karuzeli" | Jak często węzeł sam przełącza się między ekranami |
| Tryb wyświetlania | Układ/gęstość ekranu używana przez firmware |
| Wyświetlana jednostka | Metryczne lub imperialne na ekranie węzła |
| Użyj formatu 12-godzinnego | Pokazuje zegar węzła w formacie 12-godzinnym zamiast 24-godzinnego |
| Pogrubiony nagłówek | Rysuje tekst nagłówka ekranu pogrubioną czcionką |
| Odwróć ekran | Obraca wyświetlacz o 180° przy odwróconym montażu |
| Typ ekranu OLED | Sześć wartości, pokazanych jako surowe stałe: `OLED_AUTO`, `OLED_SSD1306`, `OLED_SH1106`, `OLED_SH1107`, `OLED_SH1107_128_128`, `OLED_SH1107_ROTATED` |
| Wybudź przy dotknięciu lub ruchu | Zapala ekran, gdy węzeł zostanie dotknięty lub poruszony |
| Orientacja kompasu | Przesunięcie obrotu różyny kompasu. Osiem wartości, pokazanych jako surowe stałe `DEGREES_0` do `DEGREES_270` oraz wariant `_INVERTED` każdej z nich |
| Zawsze wskazuj północ | Blokuje różę kompasu północą do góry zamiast obracać ją razem z Twoim kierunkiem. Niezależne od Orientacji kompasu — żadne z nich nie zastępuje drugiego |

### Konfiguracja pozycji

W **Ustawienia → Konfiguracja urządzenia → Pozycjonowanie**.

| Ustawienie | Opis |
|---------|-------------|
| Tryb GPS (sprzęt fizyczny) | Trzy stany: GPS włączony, wyłączony lub nieobecny. Nie jest to zwykłe włącz/wyłącz |
| Interwał odpytywania GPS | Jak często węzeł prosi GPS o ustalenie pozycji |
| Interwał transmisji | Jak często pozycja jest udostępniana w sieci mesh |
| Inteligentne Pozycjonowanie | Nadaje na podstawie ruchu, a nie wyłącznie według zegara |
| Inteligentny interwał | Przy włączonym Inteligentnym Pozycjonowaniu — najkrótszy odstęp między transmisjami |
| Inteligentny dystans | Przy włączonym Inteligentnym Pozycjonowaniu — jak daleko musisz się przemieścić, zanim nastąpi transmisja |
| Położenie stałe | Używa ręcznie wpisanej szerokości, długości i wysokości zamiast GPS |
| Flagi pozycji (Position Flags) | Grupa przełączników wybierających, które pola są dołączane do pozycji — wysokość, jej odniesienie i dokładność, widoczne satelity, znacznik czasu itd. |
| GPS EN / Rx / Tx GPIO | Zaawansowane: piny, do których podłączony jest moduł GPS |

### Konfiguracja zasilania

W **Ustawienia → Konfiguracja urządzenia → Zasilanie**.

| Ustawienie | Opis |
|---------|-------------|
| Włącz tryb oszczędzania energii | Pozwala węzłowi głęboko zasypiać między aktywnościami |
| Wyłącz przy utracie zasilania | Wyłącza urządzenie po zaniku zasilania zewnętrznego |
| Czas trwania głębokiego uśpienia | Jak długo trwa najgłębszy stan uśpienia |
| Minimalny czas czuwania | Najkrótszy czas, przez jaki węzeł pozostaje obudzony po wybudzeniu |
| Czas oczekiwania na Bluetooth (Wait for Bluetooth duration) | Jak długo czekać na połączenie telefonu przed uśpieniem |
| Nadpisanie mnożnika ADC | Włącza ręczną korektę odczytów napięcia baterii |
| Współczynnik nadpisania mnożnika ADC | Sam współczynnik korekty, używany tylko przy włączonym nadpisaniu |
| Adres I2C dla wartości baterii INA_2XX | Adres zewnętrznego czujnika zasilania serii INA, jeśli jest zamontowany |

### Konfiguracja sieci

W **Ustawienia → Konfiguracja urządzenia → Sieć**, w węzłach z Wi-Fi lub Ethernetem.

> ⚠️ **Ostrzeżenie:** Włączenie **WiFi włączone** lub **Ethernet enabled** kończy połączenie Bluetooth między telefonem a węzłem. Połącz się potem ponownie przez sieć z ekranu [Połączenia](connections) albo wyłącz Wi-Fi ponownie z ekranu samego węzła lub przez USB. Zapisanie tego ekranu zawsze też restartuje węzeł.

| Ustawienie | Opis |
|---------|-------------|
| WiFi włączone | Włącza Wi-Fi węzła (węzły ESP32) |
| SSID | Nazwa sieci, z którą ma się połączyć. Pojawia się dopiero po włączeniu **WiFi włączone**, razem z **Hasło**. **Skanuj kod QR Wi-Fi** wypełnia oba pola ze standardowego kodu QR Wi-Fi; na Androidzie przyłożenie telefonu do tagu NFC z danymi Wi-Fi przy otwartym tym ekranie wypełnia je w ten sam sposób, a aplikacja proponuje otwarcie ustawień systemowych, jeśli NFC jest wyłączone |
| Hasło | Hasło sieci |
| Ethernet włączony | Używa połączenia przewodowego na sprzęcie, który je ma |
| Tryb IPv4 | DHCP lub adres statyczny konfigurowany czterema następnymi polami |
| Wifi IP / Maska podsieci / Brama domyślna / DNS | Adres statyczny, używany tylko gdy tryb IPv4 jest statyczny |
| Nadawanie UDP | Udostępnia ruch sieci mesh innym węzłom przez sieć lokalną |
| Serwer NTP | Serwer synchronizacji czasu |
| Serwer rsyslog | Serwer zdalnego logowania |

![Konfiguracja sieci z wpisanym statycznym adresem IPv4](../../assets/screenshots/settings_ipv4_field.png)

### Konfiguracja Bluetooth

W **Ustawienia → Konfiguracja urządzenia → Bluetooth**, w węzłach z Bluetooth.

| Ustawienie | Opis |
|---------|-------------|
| Bluetooth włączony | Włącza/wyłącza radio BLE |
| Tryb parowania | Stały PIN, Losowy PIN lub Bez PIN |
| Stały PIN (Fixed PIN) | Kod PIN do parowania. Musi mieć **dokładnie sześć cyfr** — pole odrzuca wszystko inne |

### Konfiguracja bezpieczeństwa

W **Ustawienia → Bezpieczeństwo**. Ekran jest podzielony na karty: **Autentyczność pakietów**, **Klucz wiadomości bezpośredniej** (para kluczy Twojego węzła), **Klucze administratora**, **Rejestry zdarzeń (logs)** i **Zarządzanie**.

| Ustawienie | Opis |
|---------|-------------|
| Klucz publiczny | Klucz publiczny Twojego węzła (tylko do odczytu) |
| Klucz administratora | Klucze uprawnione do zdalnego zarządzania tym węzłem — do trzech |
| Klucz prywatny | Klucz prywatny Twojego węzła (obchodź się z nim bezpiecznie). Pokazany jako zaciemniony, gdy oglądasz inny węzeł przez zdalne zarządzanie — firmware go nie wysyła |
| Wygeneruj nowy klucz prywatny | Wydaje nową parę kluczy dla tego węzła, po potwierdzeniu. Każdy partner, który znał Twój stary klucz, musi poznać nowy |
| ~~Kanał administracyjny włączony (Admin Channel Enabled)~~ | ⚠️ Usunięte — konfigurowane teraz automatycznie po ustawieniu klucza administratora |
| Konsola szeregowa | Konsola szeregowa przez Stream API |
| API dziennika debugowania włączone | Wysyła na żywo logi debugowania przez port szeregowy oraz pozwala przeglądać i eksportować logi węzła z zaciemnioną pozycją przez Bluetooth |
| Tryb zarządzany | Blokuje całą listę Konfiguracji, a nie tylko kanały — każde ustawienie węzła staje się tylko do odczytu i cokolwiek zmienić może tylko administrator. Można go wybrać dopiero po ustawieniu Klucza administratora |
| Kopia kluczy | Zapisuje zaszyfrowaną kopię zapasową kluczy węzła na tym telefonie (tylko dla własnego węzła) |
| Przywróć klucze | Zapisuje kopie zapasowe kluczy z powrotem w węźle (dostępne, gdy istnieje kopia zapasowa) |
| Usuń kopię zapasową kluczy | Usuwa zapisaną kopię zapasową kluczy z tego telefonu |
| Poziom ochrony | Jak traktowane są pakiety niepodpisane lub przekazywane dalej: **Ścisły — Wymagaj uwierzytelnienia**, **Zrównoważony — Preferuj uwierzytelnione** lub **Kompatybilny — Akceptuj niepodpisane** (wymaga obsługującego to firmware; Ścisły prosi o potwierdzenie) |

#### Tryb blokady

Blokada szyfruje pamięć urządzenia i wymaga hasła przy każdym połączeniu. Wymaga obsługującego to firmware; w przeciwnym razie wiersz się nie pojawia.

Jej włączenie prosi o ustawienie i potwierdzenie hasła oraz o potwierdzenie, że **blokuje port debugowania (SWD) na sprzęcie, na którym blokada faktycznie działa**. Tego kroku nie da się cofnąć z poziomu aplikacji. Późniejsze wyłączenie blokady odszyfrowuje pamięć i restartuje urządzenie, ale port pozostaje zablokowany. Ponowne jego otwarcie wymaga pełnego wymazania układu sondą debugującą, co niszczy wszystko na urządzeniu.

Obok hasła ustawiasz limity, które automatycznie kończą sesję:

| Pole | Co robi |
|---|---|
| Pozostało restartów | Przez ile restartów urządzenia przetrwa stan odblokowany |
| Godziny do wygaśnięcia | Czas życia stanu odblokowanego według zegara ściennego |
| Limit sesji (minuty) | Limit czasu pracy na jeden restart w stanie odblokowanym. 0, wartość domyślna, oznacza brak limitu |

Gdy blokada jest aktywna, wiersz pokazuje *Aktywna — pamięć zaszyfrowana, to połączenie uwierzytelnione*, gdy jest odblokowana, lub *Aktywna — wpisz hasło, aby odblokować to połączenie*, gdy nie jest. **Zablokuj teraz** natychmiast kończy bieżącą sesję. Powtarzane błędne hasła są ograniczane czasowo, zanim będzie można spróbować ponownie.

> ⚠️ **Ostrzeżenie:** Nie ma odzyskiwania hasła. Jego utrata oznacza konieczność wymazania urządzenia, aby odzyskać do niego dostęp, co niszczy jego klucze, kanały i ustawienia.

## Powiązane tematy

- [Ustawienia — Moduły i administracja](settings-module-admin) — opcjonalne moduły funkcji i administrowanie urządzeniem
- [Miernik sygnału](signal-meter) — jak presety modemu wpływają na progi jakości sygnału
- [Konfiguracja LoRa](https://meshtastic.org/docs/configuration/radio/lora) — szczegółowy opis ustawień LoRa na meshtastic.org
- [Konfiguracja początkowa](https://meshtastic.org/docs/getting-started/initial-config) — przewodnik ustawiania regionu na meshtastic.org
