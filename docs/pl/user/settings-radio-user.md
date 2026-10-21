---
title: Ustawienia — Radio i użytkownik
parent: Przewodnik użytkownika
nav_order: 7
last_updated: 2026-10-21
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

Wszystko znajduje się na ekranie **Ustawienia**. **Użytkownik**, **LoRa**, **Kanały** i **Bezpieczeństwo** są wymienione bezpośrednio na nim, w części **Konfiguracja**. **Urządzenie**, **Pozycjonowanie**, **Zasilanie**, **Sieć**, **Wyświetlacz** i **Bluetooth** znajdują się poziom niżej, w **Ustawienia → Konfiguracja urządzenia**. **Sieć** pojawia się tylko w węzłach z Wi-Fi lub Ethernetem, a **Bluetooth** tylko w węzłach z Bluetooth.

Pole **Szukaj w ustawieniach** na górze ekranu **Ustawienia** znajduje ustawienie po jego nazwie, opisie lub pokrewnym haśle, na przykład *ttl* dla **Liczby skoków**. Wybranie wyniku otwiera ekran z tym ustawieniem, przewija do niego i na chwilę je podświetla. Gdy zarządzasz zdalnym węzłem, wyniki pomijają ustawienia samej aplikacji na tym telefonie.

Gdy żaden węzeł nie jest połączony, ekrany nadal się otwierają i pokazują ustawienia ostatnio odczytane z Twojego węzła, pod napisem *Połącz się z radiem, aby zmienić te ustawienia.* Kontrolki są wyłączone, dopóki węzeł się nie połączy.

Ustawienia korzystają ze standardowych kontrolek — list rozwijanych, przełączników i suwaków:

| Kontrolka | Zrzut ekranu |
|---------|------------|
| Lista rozwijana | ![Lista rozwijana, rozwinięta i pokazująca listę opcji](../../assets/screenshots/settings_dropdown.png) |
| Przełącznik | ![Przełącznik w pozycji włączonej](../../assets/screenshots/settings_switch.png) |
| Suwak | ![Suwak z pokazaną bieżącą wartością liczbową](../../assets/screenshots/settings_slider.png) |

## Ustawienia użytkownika

### Profil użytkownika

W **Ustawienia → Użytkownik**. Ekran pokazuje też (tylko do odczytu) **ID węzła** i **Model sprzętu**.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Radioamator licencjonowany (Ham) | Włącz, jeśli posiadasz licencję radioamatorską. Włączenie wymaga potwierdzenia w oknie dialogowym, bo tryb licencjonowany usuwa klucze szyfrowania kanałów i wysyła cały ruch jawnym tekstem. We własnym węźle zmienia wtedy etykietę **Długa nazwa** na **Znak wywoławczy** (do 7 znaków) i dodaje osobną, nieobowiązkową **Długą nazwę**, która jest dopisywana do znaku wywoławczego. Przez zdalne zarządzanie pole pozostaje **Długa nazwa** | Wyłączone |
| Długa nazwa | Twoja wyświetlana nazwa. Do 24 bajtów w firmware 2.8 lub nowszym, a w starszym do 39 | Ustawia firmware |
| Krótka nazwa | Do czterech znaków | Ustawia firmware |
| Wiadomość statusu | Krótki, publiczny, dowolny status wyświetlany przez inne węzły obok Twojego węzła, do 80 bajtów, usuwany za pomocą **✕** w polu. Węzeł rozgłasza go w sieci mesh po zmianie i ponownie co 12 godzin. Wymaga firmware 2.7.20 lub nowszego, w innym przypadku jest niedostępny | Pusta |
| Nie przyjmuje wiadomości | Oznacza węzeł jako nienadzorowany lub infrastrukturalny, taki, do którego nikt nie powinien próbować pisać. Aplikacja pokazuje go ikoną na liście węzłów. Do zmiany potrzebne jest firmware 2.6.9 lub nowsze | Wyłączone |

### Stosowanie zmian

Stopka pojawia się, gdy tylko coś zmienisz. **Odrzuć** porzuca zmianę, a drugi przycisk zapisuje ją w węźle: nosi nazwę **Zapisz i uruchom ponownie** na ekranach, które firmware stosuje z restartem — Pozycjonowanie, Sieć, Bluetooth, Bezpieczeństwo i większość ekranów modułów — oraz **Zapisz** wszędzie indziej.

Wiadomość statusu jest zapisywana tym samym przyciskiem **Zapisz**, ale nigdy nie restartuje węzła — i, tak jak reszta tego ekranu, może być edytowana na zdalnym węźle, którym zarządzasz. Dla własnego węzła, gdy jest połączony, istnieje skrót: dotknij i przytrzymaj swój węzeł na [liście węzłów](nodes.md) i wybierz **Status aktualizacji**. Starsze firmware i rozłączony węzeł nie mają skrótu — nadal można skorzystać z samego pola.

## Konfiguracja urządzenia

W **Ustawienia → Konfiguracja urządzenia → Urządzenie**. Te ustawienia określają rolę węzła w sieci mesh i zachowanie jego sprzętu.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Przycisk GPIO / Buzzer GPIO | Zaawansowane: do jakich pinów podłączone są przycisk użytkownika i brzęczyk PWM | 0 (domyślne dla płytki) |
| Rola urządzenia | Zachowanie węzła: **Client**, **Client Mute**, **Client Hidden**, **Client Base**, **Router**, **Router Late**, **Tracker**, **Sensor**, **TAK**, **TAK Tracker** lub **Lost and Found**. Opis wybranej roli pojawia się pod polem. Wybór **Router** lub **Router Late** otwiera okno, w którym potwierdzasz, że przeczytano wskazówki dotyczące ról urządzeń, zanim **Akceptuj** stanie się aktywne | Client |
| LED bicia serca | Steruje migającą diodą stanu. Mimo nazwy przełącznik jest włączony, gdy dioda miga | Włączone (dioda miga) |
| Podwójne dotknięcie jako naciśnięcie przycisku | Traktuje podwójne dotknięcie jak naciśnięcie przycisku, na obsługiwanym akcelerometrze | Wyłączone |
| Interwał rozgłaszania informacji o węźle (Node Info Broadcast Interval) | Jak często węzeł ponownie się ogłasza. Lista rozwijana ze stałymi interwałami: **Nieustawiony**, potem od 3 do 72 godzin | 3 godziny |
| Tryb retransmisji (Rebroadcast Mode) | Które pakiety węzeł przekazuje dalej: **All**, **All Skip Decoding**, **Local Only**, **Known Only**, **None** lub **Core Portnums Only**. Opis wybranego trybu pojawia się pod polem. **None** nie jest dozwolony w rolach Router | All |
| Strefa czasowa (Time Zone) | Ciąg strefy czasowej POSIX dla dat na ekranie węzła i w jego dzienniku. Przycisk **Użyj strefy czasowej telefonu** wypełnia go z tego telefonu, a **✕** go czyści | Pusta |
| Potrójne kliknięcie: Ping adhoc | Wysyła pozycję na kanale głównym po potrójnym kliknięciu przycisku użytkownika | Włączone |

## Konfiguracja LoRa

W **Ustawienia → LoRa**. Te ustawienia określają, gdzie i jak węzeł nadaje. Region, preset i ręczne ustawienia modemu są na karcie **Opcje**. Reszta jest w **Zaawansowane**.

> ⚠️ **Ważne:** Praca bez prawidłowego regionu może naruszać lokalne przepisy radiowe. Szczegóły znajdziesz w [przewodniku konfiguracji regionu](https://meshtastic.org/docs/getting-started/initial-config) na meshtastic.org.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Pasmo (kHz) | Tylko tryb ręczny: szerokość pasma kanału w kHz, wpisywana bezpośrednio. W regionie 2.4 GHz aplikacja oferuje zamiast tego listę pasm obsługiwanych przez Twój węzeł, na której pierwsze jest **Domyślne (812.5 kHz)**. Zapisana wartość spoza tej listy jest pokazywana jako *Nieobsługiwane* i blokuje zapis, dopóki nie wybierzesz obsługiwanej | Z presetu |
| Szybkość kodowania | Tylko tryb ręczny: od 5 do 8, mianownik z 4/n. Większa redundancja kosztuje czas w eterze | Z presetu |
| Nadpisanie współczynnika kodowania (Coding Rate Override) | Tylko w trybie presetu, firmware 2.7.18 lub nowszy: podnosi szybkość kodowania presetu, aby uzyskać większą korekcję błędów, zachowując jego pasmo i współczynnik rozproszenia. Oferowane są tylko wartości wyższe od własnej wartości presetu, a preset, który już ma 4/8, nie ma żadnej. Węzły na tym samym presecie nadal się słyszą, ale każdy pakiet trwa dłużej w eterze i zużywa więcej duty cycle. Przełączenie na preset, który już spełnia nadpisanie, resetuje je do wartości domyślnej presetu | Domyślna presetu |
| Wymuszenie częstotliwości | Całkowicie zastępuje obliczoną częstotliwość pracy, w MHz. Nie jest to przesunięcie względem wartości obliczonej. Gdy wynosi 0, pole pokazuje obliczoną częstotliwość z napisem **Auto** obok | 0 (użyj obliczonej) |
| Slot częstotliwości | Którego slotu w paśmie regionu użyć. Przy 0 slot wybiera region, zwykle na podstawie nazwy kanału głównego, a pole pokazuje ten slot z napisem **Auto** obok | 0 (automatycznie) |
| Liczba skoków | Ile razy wiadomość może być powtórzona, zanim przestanie być przekazywana dalej, od 0 do 7 | 3 |
| Ignoruj MQTT | Odrzuca pakiety, które dotarły z MQTT, a nie drogą radiową. Firmware włącza to za Ciebie, ilekroć ustawisz region z limitem duty cycle — pasma UE, Tajlandia i Ukraina 433 | Wyłączone, dopóki nie ustawisz regionu z limitem duty cycle |
| Ok dla MQTT | Pozwala bramom przekazywać Twoje pakiety do MQTT | Wyłączone |
| Nadpisz cykl pracy | Ignoruje limit duty cycle regionu. UWAGA!!! W większości regionów to łamie przepisy — włączaj tylko tam, gdzie przepisy na to pozwalają (apka pokazuje to samo ostrzeżenie pod opcją) | Wyłączone |
| Wentylator PA wyłączony | Wyłącza wentylator wzmacniacza mocy. Widoczny tylko w sprzęcie, który go ma | Wyłączone |
| Presety | Kompromis między szybkością a zasięgiem, wymienione pełnymi nazwami, np. „Szeroki daleki szybki (Long Fast)”. Zobacz [Presety modemu](#modem-presets) | Long Fast. Gdy węzeł z firmware 2.8 lub nowszym po raz pierwszy dostaje region Stany Zjednoczone, aplikacja wybiera Long Turbo |
| Region | Region regulacyjny określający pasma częstotliwości. Musisz go ustawić przed nadawaniem. W firmware 2.8 lub nowszym pierwsze ustawienie regionu tworzy także klucz tożsamości węzła i nadaje mu nowy numer węzła | Nieustawiony (trzeba skonfigurować) |
| Wzmocniony zysk RX | Dodatkowe wzmocnienie odbioru w radiach SX126x; zużywa nieco więcej prądu | Włączone |
| Współczynnik rozproszenia | Tylko tryb ręczny: od 5 do 12. Wyższy daje większy zasięg, ale wolniej. W radiach SX127x (RF95) firmware nie przyjmuje 5 ani 6 i używa 11 | Z presetu |
| Nadawanie włączone (Transmit Enabled) | Wyłączenie sprawia, że węzeł tylko odbiera | Włączone |
| Moc nadawania | W dBm. 0 używa największej mocy dozwolonej w regionie, co pasuje do większości węzłów | 0 (maks. regionu) |
| Użyj presetu | Wyłącz, aby ustawić **Współczynnik rozproszenia**, **Szybkość kodowania** i **Pasmo** ręcznie, zamiast brać je z presetu modemu | Włączone |

Lista presetów jest również filtrowana do tego, na co pozwala prawo w Twoim regionie, więc preset dozwolony w jednym regionie nie pojawia się w innym. Zmiana regionu może więc sprawić, że Twój obecny preset stanie się niedozwolony — aplikacja naprawia to za Ciebie, przełączając na domyślny preset danego regionu, zamiast zostawiać nieużyteczne ustawienie. Sama lista regionów jest filtrowana według tego, co obsługuje Twój firmware, więc regiony dodane w wersji 2.8 pojawiają się dopiero w wersji 2.8 lub nowszej, obok regionu, który węzeł już ma ustawiony.

Niektóre regiony to przydziały dla radioamatorów, których presetów mogą używać tylko licencjonowani operatorzy. W firmware 2.8 lub nowszym aplikacja wie, które to regiony, i wyszarza całą listę **Presetów**, dopóki nie włączysz **Radioamator licencjonowany (Ham)** dla konfigurowanego węzła; tekst pod polem informuje o tym, gdy jest wyszarzona.

### Presety modemu

Presety Lite, Narrow, Medium Turbo i Tiny wymagają firmware 2.8 lub nowszego — aplikacja ukrywa je w starszych węzłach.

Ta tabela używa krótkich nazw. Lista rozwijana **Presety** w aplikacji pokazuje nazwy polskie z angielską nazwą w nawiasie i prefiksem szerokości pasma, np. „Szeroki daleki szybki (Long Fast)” czy „Wąski szybki (Narrow Fast)”.

Wartości **Limitu SNR** są ujemne celowo. LoRa potrafi dekodować sygnały *poniżej* poziomu szumu, więc bardziej ujemny limit oznacza, że preset toleruje słabszy, bardziej zaszumiony sygnał (większy zasięg). Pełne wyjaśnienie znajdziesz w [Jak działa miernik sygnału](signal-meter).

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
| Long Slow | ~30 km | 0,18 kbps | −20 dB | Przestarzały. Nadal można go wybrać, ale może zostać usunięty w przyszłym wydaniu firmware |
| Very Long Slow | ~40+ km | 0,09 kbps | −20 dB | Przestarzały. Firmware 2.6.5 i nowsze już go nie obsługują |

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

## Konfiguracja wyświetlacza

W **Ustawienia → Konfiguracja urządzenia → Wyświetlacz**. Te ustawienia dotyczą **własnego ekranu węzła**, a nie aplikacji.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Format 12-godzinny | Pokazuje zegar węzła w formacie 12-godzinnym zamiast 24-godzinnego | Wyłączone |
| Zawsze wskazuj północ | Blokuje różę kompasu północą do góry zamiast obracać ją razem z Twoim kierunkiem. Niezależne od **Orientacji kompasu** — żadne z nich nie zastępuje drugiego | Wyłączone |
| Pogrubiony nagłówek | Rysuje tekst nagłówka ekranu pogrubioną czcionką | Wyłączone |
| Interwał "karuzeli" | Jak często węzeł sam przechodzi do następnego ekranu: **Nieustawiony** (nigdy) albo od 15 sekund do 15 minut | Nieustawiony |
| Orientacja kompasu | Przesunięcie obrotu różyny kompasu: **0°**, **90°**, **180°** lub **270°**, każde także w wariancie **Odwrócone** | 0° |
| Tryb wyświetlania | Układ ekranu: **Default 128x64 screen layout**, **Optimized for 2 color displays**, **Inverted top bar for 2 color displays** lub **TFT Full Color Displays** | Domyślny, a w węzłach z kolorowym ekranem kolorowy |
| Wyświetlana jednostka | **Metryczne** lub **imperialne** na ekranie węzła | Metryczne |
| Odwróć ekran | Odwraca wyświetlacz przy odwróconym montażu | Wyłączone |
| Typ ekranu OLED | Zastępuje automatyczne wykrywanie OLED: **Detect Automatically**, **SSD1306**, **SH1106**, **SH1107**, **SH1107 128x128** lub **SH1107 Rotated** | Detect Automatically |
| Ekran włączony na | Jak długo wyświetlacz pozostaje podświetlony po naciśnięciu przycisku lub odebraniu wiadomości: od 15 sekund do 1 godziny albo **Zawsze włączony** | 10 minut |
| Wybudź przy dotknięciu lub ruchu | Zapala ekran, gdy węzeł zostanie dotknięty lub poruszony. Wymaga akcelerometru | Wyłączone |

## Konfiguracja pozycji

W **Ustawienia → Konfiguracja urządzenia → Pozycjonowanie**. Te ustawienia określają, skąd węzeł bierze swoją pozycję i jak często ją udostępnia.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Interwał transmisji | Najdłuższy czas, przez jaki węzeł nie nadaje swojej pozycji: **Nieustawiony** albo od 1 minuty do 72 godzin | 1 godzina |
| Położenie stałe | Nadaje ręcznie wpisaną **Szerokość geograficzną**, **Długość geograficzną** i **Wysokość** zamiast pozycji z GPS na żywo. Przycisk **Ustaw z bieżącej lokalizacji telefonu** wypełnia je | Wyłączone |
| GPS EN GPIO / GPS Rx GPIO / GPS Tx GPIO | Zaawansowane: piny, do których podłączony jest dodatkowy moduł GPS. Ukryte na płytkach, w których producent sam podłączył GPS, chyba że pin ma już wartość | 0 |
| Tryb GPS (sprzęt fizyczny) | **Włączony**, **wyłączony** lub **nieobecny**. Ukryty, gdy włączone jest **Położenie stałe** | Zależy od sprzętu |
| Inteligentny dystans | W metrach. Przy włączonym **Inteligentnym Pozycjonowaniu** — jak daleko musisz się przemieścić, zanim nadanie będzie brane pod uwagę | 100 m |
| Inteligentny interwał | Przy włączonym **Inteligentnym Pozycjonowaniu** — najkrótszy odstęp między transmisjami po spełnieniu warunku dystansu | 5 minut |
| Flagi pozycji (Position Flags) | Grupa pól wyboru wskazujących, które dane są dołączane do pozycji, takie jak wysokość, jej odniesienie i dokładność, widoczne satelity i znacznik czasu | **Wysokość**, **Wysokość nad poziomem morza**, **Prędkość pojazdu**, **Kierunek pojazdu**, **DOP**, **Liczba satelitów** |
| Inteligentne Pozycjonowanie | Nadaje na podstawie ruchu, a nie wyłącznie według zegara | Włączone |
| Interwał odpytywania GPS | Jak często węzeł prosi GPS o ustalenie pozycji. Ukryty, gdy włączone jest **Położenie stałe** | 2 minuty |

## Konfiguracja zasilania

W **Ustawienia → Konfiguracja urządzenia → Zasilanie**. Te ustawienia wymieniają czas pracy na baterii na dostępność węzła.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Nadpisanie mnożnika ADC | Włącza ręczną korektę odczytów napięcia baterii | Wyłączone |
| Współczynnik nadpisania mnożnika ADC | Sam współczynnik korekty, widoczny tylko przy włączonym nadpisaniu | — |
| Adres I2C dla wartości baterii INA_2XX | Adres zewnętrznego czujnika zasilania serii INA, jeśli jest zamontowany | 0 |
| Minimalny czas czuwania | Najkrótszy czas, przez jaki węzeł pozostaje obudzony po wybudzeniu | 10 sekund |
| Oszczędzanie energii | Śpi tak długo, jak to możliwe. W rolach Tracker i Sensor obejmuje to także radio LoRa. Nie dla węzła używanego z aplikacją ani dla węzła bez przycisku użytkownika | Wyłączone |
| Wyłącz przy utracie zasilania | Po jakim czasie od odłączenia zasilania zewnętrznego węzeł się wyłączy. **Nieustawiony** wyłącza tę funkcję, a wartości poniżej 30 sekund są podnoszone do 30 | Nieustawiony |
| Czas trwania głębokiego uśpienia | Jak długo trwa najgłębszy stan uśpienia | — |
| Czas oczekiwania na Bluetooth (Wait for Bluetooth duration) | Jak długo czekać na połączenie telefonu przed uśpieniem | 1 minuta |

## Konfiguracja sieci

W **Ustawienia → Konfiguracja urządzenia → Sieć**, w węzłach z Wi-Fi lub Ethernetem. Gdy węzeł jest w sieci, karta **Bieżące połączenia:** na górze pokazuje jego **Wifi IP:** lub **Ethernet IP:**.

> ⚠️ **Ostrzeżenie:** Włączenie **WiFi włączone** lub **Ethernet włączony** kończy połączenie Bluetooth między telefonem a węzłem. Połącz się potem ponownie przez sieć z ekranu [Połączenia](connections) albo wyłącz Wi-Fi ponownie z ekranu samego węzła lub przez USB. Zapisanie tego ekranu zawsze też restartuje węzeł.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Tryb IPv4 | **DHCP** albo **statyczny** z czterema następnymi polami adresu | DHCP |
| Ethernet włączony | Używa połączenia przewodowego. Widoczny tylko na sprzęcie z Ethernetem | Wyłączone |
| IP / Brama domyślna / Maska podsieci / DNS | Adres statyczny, widoczny tylko gdy **Tryb IPv4** jest **statyczny** | — |
| Serwer NTP | Serwer synchronizacji czasu | `meshtastic.pool.ntp.org` |
| Hasło | Hasło sieci. Pojawia się dopiero po włączeniu **WiFi włączone** | — |
| Serwer rsyslog | Serwer zdalnego logowania | — |
| SSID | Nazwa sieci, z którą ma się połączyć. Pojawia się dopiero po włączeniu **WiFi włączone**. **Wybierz sieć Wi-Fi z listy** pokazuje sieci 2,4 GHz w pobliżu telefonu lub komputera (obecna sieć jest na górze, przy sieciach z hasłem jest kłódka). Po wybraniu sieci z hasłem pojawia się osobne okienko na hasło, a po **Połącz** radio od razu dostaje sieć i hasło i łączy się z nią (jak po **Wyślij**); sieć bez hasła łączy się od razu. Aplikacja nigdy nie odczytuje zapisanych haseł telefonu. **Skanuj kod QR Wi-Fi** wypełnia to pole i **Hasło** ze standardowego kodu QR Wi-Fi; na Androidzie przyłożenie telefonu do tagu NFC z danymi Wi-Fi przy otwartym tym ekranie wypełnia je w ten sam sposób, a aplikacja proponuje otwarcie ustawień systemowych, jeśli NFC jest wyłączone | — |
| Nadawanie UDP | Udostępnia ruch sieci mesh innym węzłom przez sieć lokalną | Wyłączone |
| WiFi włączone | Łączy węzeł z Wi-Fi | Wyłączone |

![Konfiguracja sieci z wpisanym statycznym adresem IPv4](../../assets/screenshots/settings_ipv4_field.png)

## Konfiguracja Bluetooth

W **Ustawienia → Konfiguracja urządzenia → Bluetooth**, w węzłach z Bluetooth.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Bluetooth włączony | Włącza lub wyłącza Bluetooth węzła | Włączony, oprócz niektórych węzłów z kolorowym ekranem |
| Stały PIN (Fixed PIN) | Kod PIN do parowania, używany z `FIXED_PIN`. Musi mieć **dokładnie sześć cyfr** — pole odrzuca wszystko inne | 123456 |
| Tryb parowania | Wymieniony pod nazwami z firmware: `RANDOM_PIN`, `FIXED_PIN` lub `NO_PIN` | `RANDOM_PIN` w węzłach z ekranem, `FIXED_PIN` w węzłach bez ekranu |

## Konfiguracja bezpieczeństwa

W **Ustawienia → Bezpieczeństwo**. Ekran jest podzielony na karty: **Autentyczność pakietów**, **Klucz wiadomości bezpośredniej** (para kluczy Twojego węzła), **Klucze administratora**, **Rejestry zdarzeń (logs)** i **Zarządzanie**.

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| Klucz administratora | Klucze publiczne uprawnione do zdalnego zarządzania tym węzłem, do trzech | Brak |
| Kopia kluczy | Zapisuje zaszyfrowaną kopię zapasową kluczy węzła na tym telefonie. Tylko dla własnego węzła i nie w aplikacji na komputer | — |
| API dziennika debugowania włączone | Wysyła na żywo logi debugowania przez port szeregowy oraz pozwala przeglądać i eksportować logi węzła z zaciemnioną pozycją przez Bluetooth | Wyłączone |
| Usuń kopię zapasową kluczy | Usuwa zapisaną kopię zapasową kluczy z tego telefonu | — |
| Tryb zarządzany | Blokuje całą listę Konfiguracji, a nie tylko kanały — każde ustawienie węzła staje się tylko do odczytu i cokolwiek zmienić może tylko administrator. Można go wybrać dopiero po ustawieniu **Klucza administratora** | Wyłączone |
| Klucz prywatny | Klucz prywatny Twojego węzła (obchodź się z nim bezpiecznie). Pokazany jako zaciemniony, gdy oglądasz inny węzeł przez zdalne zarządzanie — firmware go nie wysyła | Tworzy go węzeł |
| Poziom ochrony | Jak traktowane są pakiety niepodpisane: **Ścisły — Wymagaj uwierzytelnienia**, **Zrównoważony — Preferuj uwierzytelnione** lub **Kompatybilny — Akceptuj niepodpisane**. Wymaga firmware obsługującego podpisywanie pakietów; **Ścisły** prosi o potwierdzenie | Kompatybilny |
| Klucz publiczny | Klucz publiczny Twojego węzła (tylko do odczytu) | Wyliczany z klucza prywatnego |
| Wygeneruj nowy klucz prywatny | Wydaje nową parę kluczy dla tego węzła, po potwierdzeniu. Każdy partner, który znał Twój stary klucz, musi poznać nowy | — |
| Przywróć klucze | Zapisuje kopie zapasowe kluczy z powrotem w węźle (dostępne, gdy istnieje kopia zapasowa) | — |
| Konsola szeregowa | Konsola szeregowa przez Stream API | Włączona |

### Tryb blokady

Blokada szyfruje pamięć urządzenia i wymaga hasła przy każdym połączeniu. Wymaga obsługującego to firmware; w przeciwnym razie wiersz się nie pojawia.

> ⚠️ **Ostrzeżenie:** Nie ma odzyskiwania hasła. Jego utrata oznacza konieczność wymazania urządzenia, aby odzyskać do niego dostęp, co niszczy jego klucze, kanały i ustawienia.

Jej włączenie prosi o ustawienie i potwierdzenie hasła oraz o potwierdzenie, że **blokuje port debugowania (SWD) na sprzęcie, na którym blokada faktycznie działa**. Tego kroku nie da się cofnąć z poziomu aplikacji. Późniejsze wyłączenie blokady odszyfrowuje pamięć i restartuje urządzenie, ale port pozostaje zablokowany. Ponowne jego otwarcie wymaga pełnego wymazania układu sondą debugującą, co niszczy wszystko na urządzeniu.

Obok hasła ustawiasz limity, które automatycznie kończą sesję:

| Pole | Co robi |
|---|---|
| Pozostało restartów | Przez ile restartów urządzenia przetrwa stan odblokowany. Zaczyna od 50 |
| Godziny do wygaśnięcia | Czas życia stanu odblokowanego według zegara ściennego |
| Limit sesji (minuty) | Limit czasu pracy na jeden restart w stanie odblokowanym. 0, wartość domyślna, oznacza brak limitu |

Gdy blokada jest aktywna, wiersz pokazuje *Aktywna — pamięć zaszyfrowana, to połączenie uwierzytelnione*, gdy jest odblokowana, lub *Aktywna — wpisz hasło, aby odblokować to połączenie*, gdy nie jest. **Zablokuj teraz** natychmiast kończy bieżącą sesję. Powtarzane błędne hasła są ograniczane czasowo, zanim będzie można spróbować ponownie.

## Powiązane tematy

- [Ustawienia — Moduły i administracja](settings-module-admin) — opcjonalne moduły funkcji i administrowanie urządzeniem
- [Miernik sygnału](signal-meter) — jak presety modemu wpływają na progi jakości sygnału
- [Konfiguracja LoRa](https://meshtastic.org/docs/configuration/radio/lora) — szczegółowy opis ustawień LoRa na meshtastic.org
- [Konfiguracja początkowa](https://meshtastic.org/docs/getting-started/initial-config) — przewodnik ustawiania regionu na meshtastic.org
