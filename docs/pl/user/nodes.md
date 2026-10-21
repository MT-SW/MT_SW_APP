---
title: Węzły
parent: Przewodnik użytkownika
nav_order: 4
last_updated: 2026-10-21
description: Przeglądaj, filtruj i sortuj węzły sieci mesh — szczegóły, jakość sygnału, role i szybkie akcje.
aliases:
  - node-list
  - mesh-nodes
  - peers
  - hop-histogram
  - remote-shell
  - terminal
---

# Węzły

Ekran Węzły wyświetla listę wszystkich węzłów widocznych w Twojej sieci mesh.

## Lista węzłów

Lista węzłów pokazuje każdy węzeł, który Twój węzeł usłyszał, w tym:
- **Nazwa węzła** — długa nazwa ustawiona przez użytkownika
- **Nazwa skrócona** — 4-znakowy identyfikator
- **Jakość sygnału** — SNR, RSSI i określenie jakości, pokazywane tylko dla węzłów, które Twój węzeł usłyszał bezpośrednio. W układzie Pełnym węzeł osiągany przez przekaźnik pokazuje tutaj zamiast tego liczbę skoków; węzeł słyszany wyłącznie przez MQTT nie pokazuje żadnej z tych informacji
- **Ostatnio słyszany** — czas od ostatniej komunikacji
- **Odległość** — szacowana odległość (jeśli pozycje są udostępniane)
- **Bateria** — poziom baterii zdalnego węzła (jeśli telemetria jest włączona)

### Wybór tego, co pokazuje lista

Lista ma dwa poziomy szczegółowości, ustawiane w **Ustawienia → Układ węzła** (Node Layout). Układ **Pełny** pokazuje każde pole, które węzeł zgłosił, i ukrywa te, których nie zgłosił. Układ **Kompaktowy** mieści więcej węzłów na ekranie i pozwala samodzielnie wybrać pola — **Zasilanie**, **Czas ostatniej aktywności**, **Względny czas ostatniej aktywności**, **Odległość i kierunek**, **Skoków**, **Sygnał (tylko bezpośredni)**, **Kanał** oraz **Urządzenie i rola**. Przełącznik **Metryki środowiskowe** dotyczy obu układów. Podgląd nad przełącznikami pokazuje efekt, zanim opuścisz ekran.

### Dodatkowe dane przy węźle

W **Ustawienia → Układ węzła → Dodatkowe dane przy węźle** możesz włączyć dodatkową linię pod każdym węzłem: pogoda (temperatura, wilgotność, ciśnienie, wiatr, deszcz, światło, UV i inne czujniki), zasilanie, ruch (pakiety wysłane, odebrane, uszkodzone, zajętość kanału) oraz zasoby (procesor, pamięć, czas pracy). Pokazywane są tylko najnowsze odczyty, bez historii. Węzeł bez czujników pogody pokazuje ruch i zasoby.

Trzy tryby: **Wyłączone**, **Strony** (jedna strona naraz; strony zmieniają się same, a przesunięcie palcem w lewo lub w prawo zmienia je ręcznie i na chwilę wstrzymuje automat) oraz **Wszystko naraz**. Przełącznik **Same ikony (bez podpisów)** działa niezależnie od układu; przycisk (i) obok niego pokazuje znaczenie ikon. Gdy dodatkowa linia jest włączona, zastępuje starszą siatkę czujników.

### Wskaźniki stanu węzła

| Wskaźnik | Znaczenie |
|-------|---------|
| Zielony czas ostatniej aktywności | Węzeł usłyszany w ciągu ostatnich 2 godzin |
| Zwykły czas ostatniej aktywności | Węzeł nie był słyszany od ponad 2 godzin |
| Pomarańczowy czas ostatniej aktywności z przekreśloną ikoną sygnału | Nie był słyszany od zmiany ustawień LoRa Twojego węzła, więc nie można się z nim stąd połączyć |
| Przekreślona nazwa | Węzeł, który zignorowałeś |
| ⭐ Ulubiony | Węzeł oznaczony jako ulubiony. |

Nie ma osobnego poziomu „nieobecny”, ale pomarańczowy stan niedostępności ma pierwszeństwo przed
zielonym: węzeł może być online i mimo to niedostępny przy Twoich obecnych ustawieniach. Węzły znane
wyłącznie przez MQTT nigdy nie są pokazywane jako niedostępne.

### Kolory baterii na liście

Poziom baterii na liście węzłów ma kolor, który od razu mówi, jak jest z zasilaniem. Używamy tych samych kolorów co w całej aplikacji:

| Kolor | Poziom baterii | Znaczenie |
|-------|----------------|-----------|
| Złoty | 50% i więcej (oraz zasilanie zewnętrzne) | Dobrze |
| Czerwony | od 20% do 49% | Średnio — pora pomyśleć o ładowaniu |
| Fioletowy | poniżej 20% | Słabo — węzeł może wkrótce zniknąć z sieci |

Węzeł zasilany z zewnątrz (zasilacz, panel słoneczny) pokazuje się jako w pełni naładowany.

### Zdrowie sieci i kolory

Karta **Zdrowie sieci** pokazuje, jak radzą sobie wszystkie węzły naraz. Na górze jest pierścień z procentem węzłów w dobrym stanie, liczbą węzłów w każdym stanie i liczbą węzłów online. Każda zakładka (Zasilanie, Sygnał, Eter, Środowisko, Zasoby, Ruch, Sąsiedzi) ma ikonę w kolorze najgorszego węzła w tej zakładce.

| Kolor | Znaczenie |
|-------|-----------|
| Złoty | Dobrze |
| Czerwony | Średnio |
| Fioletowy | Źle |
| Biały (na jasnym motywie czarny) | Bardzo źle |
| Szary | Brak danych do oceny |

Limity w każdej zakładce:

- **Zasilanie** — dobrze: 50% lub więcej albo zasilanie zewnętrzne; średnio: 20–49%; źle: 5–19%; bardzo źle: poniżej 5%.
- **Sygnał** — kolor wynika z SNR ostatniego bezpośredniego odbioru: dobrze od 0 dB; średnio od −7,5 do 0 dB; źle od −15 do −7,5 dB; bardzo źle poniżej −15 dB. Węzeł bez żadnego SNR (na przykład Twój własny) jest oceniany po poziomie szumu: do −110 dBm dobrze, do −100 średnio, do −90 źle, powyżej bardzo źle. Duża liczba na liście to poziom szumu (noise floor), po korekcie LNA, jeśli jest ustawiona.
- **Eter** (zajęcie kanału) — dobrze: poniżej 15%; średnio: 15–25%; źle: 25–40%; bardzo źle: 40% i więcej.
- **Sąsiedzi** — dobrze: 3 lub więcej; średnio: 2; źle: 1; bardzo źle: 0.
- **Środowisko, Zasoby, Ruch** — bez oceny kolorem; karta pokazuje, ile węzłów zgłasza dane.

Przycisk **Tylko problemy** zostawia na liście węzły ocenione na źle lub bardzo źle. Przycisk (i) w górnym pasku przypomina te kolory i limity dla wybranej zakładki.

Po dotknięciu węzła otwierają się jego szczegóły. Te same dane możesz tam zobaczyć na cztery sposoby — **Linia** (z kolorowymi strefami), **Zegar**, **Słupki** i **Mapa cieplna** — a dla baterii, SNR i zajęcia kanału także pasek **Czas w każdym stanie**. Ikona **Podsumowanie** w górnym pasku otwiera zbiorcze podsumowanie sieci z wykresami; można je udostępnić albo zapisać jako PDF, stronę HTML, Markdown, tekst lub CSV.

### Role węzłów

Węzły można skonfigurować z różnymi rolami, które wpływają na ich zachowanie w sieci mesh:

| Rola | Opis |
|------|-------------|
| Klient (Client) | Standardowy węzeł użytkownika końcowego |
| Klient bazowy (Client Base) | Traktuje ruch ulubionych węzłów z priorytetem Routera pomocniczego; cały pozostały ruch jak Klient |
| Klient pasywny (Client Mute) | Odbiera, ale nie retransmituje |
| Klient ukryty (Client Hidden) | Jak Klient pasywny, a dodatkowo ukrywa się na liście węzłów |
| Router | Priorytetowo przekazuje wiadomości; pozostaje wybudzony, aby przekazywać |
| Router pomocniczy (Router Late) | Węzeł infrastruktury, który retransmituje raz, ale dopiero po wszystkich pozostałych trybach (zapewnia dodatkowy zasięg) |
| ~~Router Klienta (Router Client)~~ | ⚠️ **Przestarzała** (usunięta w firmware 2.3.15) — nie można już jej wybrać; użyj Routera lub Klienta |
| ~~Repeater~~ | ⚠️ **Przestarzała** (usunięta w firmware 2.7.11) — nie można już jej wybrać; użyj Routera |
| Tracker | Zoptymalizowany pod raportowanie pozycji w regularnych odstępach |
| Czujnik (Sensor) | Zoptymalizowany pod raportowanie telemetrii |
| TAK | Współpracuje z systemami TAK (wysyła/odbiera CoT) |
| TAK Tracker | Tylko raportowanie pozycji TAK |
| Zgubiony i znaleziony (Lost and Found) | Wysyła swoją pozycję na kanał domyślny jako wiadomość tekstową w regularnych odstępach, aby pomóc odzyskać zgubiony węzeł |

### Wybór roli

Większość użytkowników powinna zostawić domyślną rolę **Klient**. Rozważ inną rolę, gdy:

- **Router** — Masz węzeł w stałym, wyniesionym miejscu z niezawodnym zasilaniem (dach, wzgórze). Routery pozostają stale wybudzone, aby przekazywać wiadomości innym, i są kluczowe dla rozszerzania zasięgu sieci mesh. Nie używaj roli Router w przenośnych węzłach zasilanych z baterii.
- **Router pomocniczy (Router Late)** — Węzeł infrastruktury, który zawsze retransmituje pakiety raz, ale dopiero po tym, jak wszystkie inne tryby routingu miały swoją kolej. Zapewnia dodatkowy zasięg dla lokalnych skupisk bez rywalizacji z głównymi routerami.
- **Klient bazowy (Client Base)** — Traktuje ruch od/do Twoich ulubionych węzłów z priorytetem Routera pomocniczego (dzięki czemu te wiadomości dostają dodatkowy zasięg przekazywania), a wszystko inne obsługuje jak zwykły Klient.
- **Klient pasywny (Client Mute)** — Chcesz odbierać ruch sieci mesh, ale nie uczestniczyć w jego przekazywaniu. Przydatne dla węzłów tylko do obserwacji albo do zmniejszenia zatłoczenia w gęstych obszarach.
- **Tracker** — Bezobsługowy węzeł, którego jedynym zadaniem jest nadawanie pozycji GPS (np. pojazd, zwierzę lub mienie). Śpi między transmisjami, aby oszczędzać baterię.
- **Czujnik (Sensor)** — Bezobsługowy węzeł raportujący telemetrię środowiskową (temperatura, wilgotność, jakość powietrza). Podobny profil zasilania jak Tracker.
- **TAK / TAK Tracker** — Potrzebne tylko przy współpracy z systemami ATAK/WinTAK. Szczegóły w [Integracja TAK](tak).

> 💡 **Wskazówka:** Sieć mesh działa najlepiej, gdy większość węzłów to **Klient** lub **Router**. Zbyt wiele węzłów Klient pasywny zmniejsza odporność sieci; zbyt wiele Routerów w gęstym obszarze może powodować zatłoczenie. Dobra zasada: jeden Router na 5–10 Klientów w Twojej okolicy.

### Wskaźniki bezpieczeństwa

Każdy węzeł ma jedną ikonę bezpieczeństwa obok nazwy na liście węzłów. Dotknij jej, aby przeczytać, co oznacza, i wybierz **Pokaż wszystkie znaczenia** (Show All Meanings) w tym oknie, aby zobaczyć pełną legendę. Ekran szczegółów pokazuje ten sam stan słowami, jako wiersz **Bezpieczeństwo**, który otwiera to samo okno.

| Ikona | Znaczenie | Pokazywana dla |
|------|---------|-----------|
| Osoba z tarczą z ptaszkiem (zielona) | **Zweryfikowany kontakt** — zweryfikowałeś klucz tego węzła osobiście, wymieniając się kodami QR kontaktu, więc jego tożsamość jest potwierdzona. Najsilniejsze zaufanie, jakie pokazuje lista. Twój własny podłączony węzeł też ją ma | Dowolna wersja firmware |
| Ikona węzłów z tarczą z ptaszkiem (zielona) | **Podpisany węzeł** — ten węzeł podpisuje swoje transmisje kluczem tożsamości, więc jego tożsamość jest spójna w czasie, ale nie zweryfikowałeś go osobiście. W firmware 2.8 ikona pojawia się już na podstawie samej wersji, zanim usłyszysz jakąkolwiek podpisaną transmisję | Firmware 2.8 lub nowsze oraz każdy węzeł, którego podpisaną transmisję Twój węzeł zweryfikował |
| 🔒 Zamknięta kłódka | Klucz publiczny jest zapisany i zgadza się, więc wiadomości bezpośrednie do tego węzła są szyfrowane | Firmware starsze niż 2.8 lub brak zgłoszonej wersji |
| 🔓 Otwarta kłódka | Nie otrzymano klucza publicznego tego węzła, więc nie można wysyłać mu wiadomości bezpośrednich — użyj **Poproś o informacje użytkownika** na stronie szczegółów węzła, aby o niego poprosić | Firmware starsze niż 2.8 lub brak zgłoszonej wersji |
| ⚠️ Niezgodność | **Niezgodność klucza publicznego** — po zapisaniu klucza dla tego węzła nadszedł inny klucz. Zbadaj sprawę, zanim zaufasz | Dowolna wersja firmware |

Każdy węzeł z firmware 2.8 lub nowszym podpisuje swoje transmisje, więc w tym firmware stan podpisany jest stanem podstawowym, a kłódki nie są pokazywane.

Wiadomości bezpośrednie zawsze używają szyfrowania kluczem publicznym, więc Twój węzeł potrzebuje klucza publicznego drugiego węzła, zanim będzie mógł wysłać wiadomość. Odmawia wysłania zamiast przechodzić na szyfrowanie kanału. Klucze przychodzą wewnątrz informacji o węźle, dlatego otwarta kłódka zwykle znika sama, gdy ten węzeł zostanie poprawnie usłyszany.

Niezgodność nigdy nie zastępuje klucza, który już masz. Aplikacja zachowuje pierwszy zapisany klucz i odrzuca nowy, tak jak robi to firmware, aby przypadkowa lub wroga informacja o węźle nie mogła po cichu zepsuć szyfrowanych wiadomości do kontaktu. Aby usunąć niezgodność, najpierw potwierdź innym zaufanym kanałem, że zmiana klucza była zamierzona — powoduje ją na przykład reset do ustawień fabrycznych. Następnie dotknij i przytrzymaj węzeł, wybierz **Usuń** i pozwól obu węzłom ponownie wymienić się kluczami, gdy Twój go usłyszy.

## Szybkie akcje

Z listy węzłów możesz:
- **Dotknąć** węzła, aby zobaczyć jego stronę szczegółów
- **Dotknąć i przytrzymać**, aby wywołać szybkie akcje:
  - Oznacz/usuń z ulubionych
  - Wycisz/wyłącz wyciszenie powiadomień
  - Wyślij wiadomość bezpośrednią
  - Pokaż trasę
  - Otwórz kompas (dla węzła z pozycją)
  - Otwórz widok AR sieci (dla węzła z pozycją, na telefonie, który go obsługuje) — zobacz [widok AR sieci](map-and-waypoints#widok-ar-sieci)
  - Zignoruj/przestań ignorować
  - Usuń

Jeśli dotkniesz i przytrzymasz **własny węzeł**, dostaniesz jedną akcję, **Status aktualizacji**, która otwiera
ekran ustawień Użytkownika z kursorem już w polu Status Message. Pojawia się tylko wtedy, gdy
węzeł jest połączony i ma firmware 2.8 lub nowsze — samo pole opisano w
[Ustawienia — Radio i użytkownik](settings-radio-user.md).

## Udostępnianie kontaktu

Na ekranie szczegółów węzła dotknij **Udostępnij kontakt**, aby uzyskać link i kod QR tego węzła. W tym samym oknie **Udostępnij link** otwiera systemowe menu udostępniania Androida (na komputerze zamiast tego kopiuje link), **Zapisz na tagu NFC** zapisuje go na zapisywalnym tagu NFC, a **Kopiuj** umieszcza go w schowku. Gdy to okno jest otwarte i przed Tobą, telefon oferuje też ten sam link każdemu czytnikowi NFC, więc ktoś może przejąć kontakt, przykładając swój telefon do Twojego, bez żadnego tagu.

Udostępnienie w ten sposób własnego kontaktu oznacza go jako zweryfikowany osobiście, więc osoba, która go zaimportuje, zobaczy ikonę **Zweryfikowany kontakt**, a nie ikonę podpisanego węzła. Przekazanie cudzego kontaktu dalej przekazuje tylko to, co Twoja aplikacja już o nim zapisała.

Aby dodać cudzy kontakt, użyj przycisku importu na liście węzłów i wybierz **Skanuj kod QR udostępnionego kontaktu**, **Skanuj udostępniony kontakt (NFC)** albo **Wpisz URL udostępnionego kontaktu**. Aplikacja prosi o potwierdzenie przez **Zaimportować udostępniony kontakt?** i ostrzega, gdy kontakt jest już Twoim kontaktem.

## Filtrowanie i sortowanie

### Wyszukiwanie tekstowe

Wpisz tekst w polu wyszukiwania, aby filtrować węzły według nazwy lub nazwy skróconej. Filtr aktualizuje się na bieżąco w trakcie pisania.

### Przełączniki filtrów

| Filtr | Opis |
|--------|-------------|
| **Schowaj nieaktywne węzły** | Pokazuje tylko węzły usłyszane w ciągu ostatnich 2 godzin |
| **Pokaż tylko bezpośrednie węzły** | Pokazuje tylko węzły, które Twój węzeł usłyszał bezpośrednio, bez przekaźnika pomiędzy |
| **Pokaż nierozpoznane** | Pokazuje węzły, które nie wysłały jeszcze informacji o użytkowniku. **Domyślnie włączone**, więc węzeł usłyszany przed nadejściem jego informacji pozostaje widoczny; takie węzły mają plakietkę oznaczającą niekompletność i nie można wysyłać im wiadomości bezpośrednich, dopóki ich informacje o użytkowniku nie przyniosą klucza publicznego |
| **Wyklucz infrastrukturę** | Ukrywa węzły o rolach infrastrukturalnych (Router, Router pomocniczy, Klient bazowy i dawne węzły Repeater) oraz każdy węzeł, któremu nie można wysłać wiadomości, niezależnie od roli |
| **Wyklucz MQTT** | Ukrywa węzły słyszane wyłącznie przez most internetowy MQTT |
| **Wyklucz nieusłyszane węzły** | Ukrywa węzły, których Twój węzeł nie słyszał od zmiany swoich ustawień LoRa. Domyślnie wyłączone i nie robi nic w firmware, które nie zgłasza, czy węzeł został usłyszany przy obecnych ustawieniach |
| **Pokaż tylko podpisane węzły** | Pokazuje tylko węzły, których podpisane transmisje Twój węzeł faktycznie usłyszał i zweryfikował. Bardziej rygorystyczne niż ikona: węzeł z firmware 2.8 jest pokazywany jako podpisany na podstawie wersji firmware, zanim nadejdzie jakakolwiek podpisana transmisja, a kontakt zweryfikowany osobiście na starszym firmware nie jest w ogóle podpisany |
| **Pokaż tylko szyfrowane węzły** | Pokazuje węzły z pasującym kluczem publicznym, którego potrzebuje zaszyfrowana wiadomość bezpośrednia. Węzeł z niezgodnością klucza jest wykluczony |
| **Pokaż tylko ignorowane węzły** | Zastępuje listę węzłami, które zignorowałeś. Wszystkie pozostałe węzły są ukryte, gdy to jest włączone, a u góry listy pojawia się baner umożliwiający powrót |

### Węzły nie słyszane przy Twoich obecnych ustawieniach

Gdy ustawienia LoRa Twojego węzła się zmieniają — inny preset, region lub slot częstotliwości — węzły
słyszane przy starych ustawieniach nadal są na liście, ale nie można się już z nimi połączyć. Aplikacja oznacza je
pomarańczowym czasem ostatniej aktywności i przekreśloną ikoną sygnału oraz pokazuje u góry listy baner:
**N węzłów nie słyszanych od zmiany ustawień**.

Baner oferuje dwie akcje:

- **Zachowaj** zamyka baner i zostawia wszystkie węzły na miejscu.
- **Usuń** kasuje te węzły z listy. Twoje ulubione i Twój własny węzeł nigdy nie są usuwane.

**Usuń** to masowe usuwanie bez możliwości cofnięcia, więc użyj **Zachowaj**, jeśli spodziewasz się powrotu
do starych ustawień. Usunięte węzły pojawiają się ponownie, jeśli Twój węzeł znów je usłyszy.

Filtr **Wyklucz nieusłyszane węzły** ukrywa je w ten sam sposób, ale niczego nie usuwa.

### Opcje sortowania

| Sortowanie | Opis |
|------|-------------|
| **Aktywność** | Najpierw ostatnio słyszane węzły |
| **Nazwa** | Według długiej nazwy węzła |
| **Dystans** | Najpierw najbliższe węzły (wymaga udostępniania pozycji) |
| **Liczba skoków** | Najpierw najmniej skoków przez przekaźniki |
| **Kanał** | Pogrupowane według indeksu kanału |
| **Przez MQTT** | Pogrupowane: przez MQTT i słyszane przez węzeł |
| **Przez ulubione** (domyślne) | Najpierw ulubione węzły, potem reszta |

## Węzły na skok

Dotknij ikony histogramu skoków na pasku aplikacji listy węzłów, aby otworzyć wykres słupkowy pokazujący, ile węzłów znajduje się na każdej odległości w skokach (0 = bezpośrednio, 1 = jeden przekaźnik dalej itd.). Przefiltruj wykres do okna **ostatnio słyszane** — **Wszystkie**, **1 godzina**, **8 godzin** lub **24H** — aby zobaczyć, jak sieć mesh wygląda teraz, a jak w dłuższym okresie. To szybki sposób na ocenę, jak ruchliwa i rozległa jest Twoja lokalna sieć mesh.

## Szczegóły węzła

Dotknięcie węzła otwiera widok szczegółów z pełnymi informacjami. Pełne informacje o metrykach i telemetrii znajdziesz w [Metryki węzła](node-metrics).

Jakość sygnału jest oceniana względem Twojego presetu modemu. Ten sam SNR może być dobry na presecie dalekiego zasięgu, a słaby na szybszym. Kolory SNR w traceroute i informacjach o sąsiadach również używają tego presetu. Tekst RSSI ma własne kolory siły sygnału; wpływa na ocenę jakości tylko wtedy, gdy dostępny jest też odczyt poziomu szumu.

Karta Szczegóły zawiera nazwę skróconą węzła, rolę, identyfikatory, czas ostatniej aktywności, liczbę skoków, czas pracy oraz jego SNR i RSSI:

![Sekcja szczegółów węzła](../../assets/screenshots/nodes_detail_section.png)

Wbudowane wskaźniki stanu pokazują najważniejsze metryki na pierwszy rzut oka:

| Wskaźnik | Zrzut ekranu |
|-----------|------------|
| Jakość sygnału | ![Sygnał](../../assets/screenshots/nodes_signal_info.png) |
| Poziom baterii | ![Bateria](../../assets/screenshots/nodes_battery_info.png) |
| Liczba skoków | ![Skoki](../../assets/screenshots/nodes_hops_info.png) |
| Ostatnio słyszany | ![Ostatnio słyszany](../../assets/screenshots/nodes_last_heard.png) |
| Odległość | ![Odległość](../../assets/screenshots/nodes_distance_info.png) |

### Status wsparcia sprzętu

Gdy sprzęt węzła zostanie rozpoznany, widok szczegółów podaje nazwę urządzenia i oznacza jego status wsparcia, pobrany z rejestru urządzeń Meshtastic, a nie z aplikacji:

| Oznaczenie | Znaczenie |
| --- | --- |
| Rozeta (zielona) | **Wspierany** — sprzęt aktywnie wspierany przez projekt Meshtastic |
| Klucz francuski (błękitny) | **Sprzęt niezależnego producenta** — zbudowany i przetestowany przez niezależnego producenta, pokazywany jako osobny szczebel niezależnie od tego, czy projekt już go awansował |
| Niezweryfikowany (czerwony) | **Wspierany przez społeczność Meshtastic** — sprzęt, którego projekt aktywnie nie wspiera, w tym starsze płytki |

Etykieta zawsze towarzyszy oznaczeniu, więc status nigdy nie jest przekazywany samym kolorem.

### Linki do urządzeń („Chcę taki”)

Gdy sprzęt węzła zostanie rozpoznany, widok szczegółów pokazuje zwijaną sekcję **„Chcę taki”** z linkami do miejsc, gdzie można kupić to urządzenie lub dowiedzieć się o nim więcej: strona produktu producenta, warianty produktu i oferty regionalnych sklepów (takich jak AliExpress, Amazon i wspierani sprzedawcy), przefiltrowane do Twojego kraju. Każdy link otwiera się przez usługę przekierowań `msh.to`. Urządzenia bez pasujących linków nie pokazują tej sekcji.

Pełny, przeglądalny katalog wszystkich linków jest też dostępny w **Ustawienia → Linki urządzenia**. Pozycja jest ukryta, gdy ustawienia są otwarte dla zdalnego węzła.

Niektóre z tych linków są linkami afiliacyjnymi. Oba miejsca informują o tym nad linkami: linki do produktów mogą być afiliacyjne, a zakupy mogą przynosić Meshtastic prowizję.

### Zdalny terminal

**Otwórz terminal**, w karcie **Zarządzanie** węzła, otwiera powłokę na węźle z Linuksem uruchamiającym `meshtasticd` 2.8.2 lub nowszy, przesyłaną przez sieć mesh. Węzeł akceptuje ją tylko od radia, którego klucz publiczny jest na jego liście **Bezpieczeństwo → Klucz administratora**, tej samej, której używa zdalne zarządzanie; aplikacja najpierw rozpoczyna tę sesję administracyjną, jeśli jej nie ma, a tytuł karty pokazuje, czy jest aktywna. Terminal zajmuje całe okno. Jeśli węzeł nigdy nie odpowie, ekran informuje o tym po minucie.

Sieć mesh przenosi kilkaset bajtów na sekundę i terminal jest zbudowany z myślą o tym:

- Gdy węzeł odeśle echem pierwszy znak wiersza, to, co wpisujesz, pojawia się **podkreślone** przy kursorze, dopóki węzeł nie odeśle echa. Przy znaku zachęty, który nie robi echa, np. przy haśle, nic z tego, co wpisujesz, nie jest pokazywane; w trybie liniowym pole poleceń maskuje tekst i nie zapisuje go w historii.
- Podtytuł pokazuje zmierzony czas odpowiedzi. Gdy węzeł przestaje odpowiadać, wiersz u góry terminala podaje, jak dawno był ostatnio słyszany.
- Dwa rzędy dodatkowych klawiszy dają **ESC**, **TAB**, strzałki, **HOME**, **END**, **PGUP** i **PGDN**. **CTRL** i **ALT** są lepkie: dotknij raz, aby zadziałały na następny klawisz, dwa razy, aby zablokować, trzeci raz, aby zwolnić. **CTRL** a potem **c** przerywa uruchomione polecenie. Ctrl i Alt klawiatury sprzętowej działają jak zwykle.
- **Tryb liniowy** (Line mode), w menu **⋮**, daje pole poleceń: każde polecenie jest wysyłane jedną transmisją, **↑** i **↓** przywołują wcześniejsze polecenia bez pytania węzła, a podpowiedzi oferują typowe polecenia dla hosta `meshtasticd`. To najtańszy sposób użycia powłoki w zajętej sieci mesh.
- Menu **⋮** pozwala też wklejać ze schowka i zmieniać rozmiar tekstu.

Kolory, wiersze postępu i `clear` wyświetlają się tak jak w terminalu na komputerze. Programy pełnoekranowe, takie jak `vi` czy `top`, przerysowują cały ekran przy każdym klawiszu, czego sieć mesh nie uniesie; terminal pokazuje wtedy komunikat.

## Gdy nie widać żadnych węzłów

Lista pozostaje pusta, dopóki Twój węzeł nie usłyszy innego węzła.

- **Brak podłączonych urządzeń** — aplikacja nie jest połączona z węzłem. Zobacz [Połączenia](connections).
- **Wyszukiwanie węzłów** — węzeł jest połączony i nasłuchuje, ale nic jeszcze nie dotarło. Sprawdź, czy jego region i preset modemu zgadzają się z siecią mesh wokół Ciebie, i zostaw włączone **Pokaż nierozpoznane**, aby węzeł, który nie wysłał jeszcze swojej nazwy, nadal się pojawiał. Zobacz [Ustawienia — Radio i użytkownik](settings-radio-user).
- Brakuje oczekiwanego węzła — sprawdź przełączniki filtrów. **Pokaż tylko bezpośrednie węzły**, **Wyklucz MQTT** i **Wyklucz infrastrukturę** ukrywają po całej kategorii węzłów.

## Zobacz także

- [Metryki węzła](node-metrics) — szczegółowe panele telemetrii każdego węzła
- [Wiadomości i kanały](messages-and-channels) — wyślij wiadomość bezpośrednią do węzła
- [Mapa i punkty trasy](map-and-waypoints) — zobacz pozycje węzłów na mapie
- [Odkrywanie lokalnej sieci mesh](discovery) — traceroute i informacje o sąsiadach do badania topologii
- [Miernik sygnału](signal-meter) — zrozum, co oznaczają paski sygnału
