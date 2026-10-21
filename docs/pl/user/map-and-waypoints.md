---
title: Mapa i punkty trasy
parent: Przewodnik użytkownika
nav_order: 6
last_updated: 2026-10-21
description: Podgląd pozycji węzłów na mapie, tworzenie i udostępnianie punktów trasy, zarządzanie warstwami mapy i Site Planner oraz kontrola udostępniania pozycji i prywatności.
aliases:
  - map
  - waypoints
  - gps
  - location
  - site-planner
  - map-layers
  - geojson
  - kml
---

# Mapa i punkty trasy

Ekran mapy pokazuje położenie geograficzne węzłów w Twojej sieci mesh oraz udostępnione punkty trasy.

## Widok mapy

Mapa wyświetla:
- **Pozycje węzłów** — kolorowe znaczniki dla każdego węzła, który podaje swoją lokalizację
- **Punkty trasy** — udostępnione miejsca warte uwagi
- **Twoja pozycja** — Twoja bieżąca lokalizacja GPS

### Znaczniki węzłów

Każdy węzeł, który podaje pozycję, jest pokazany jako znacznik **chip węzła** z krótką nazwą węzła. Chip ma kolor tożsamości węzła (stały kolor wyliczany z numeru węzła) — ten sam chip jest używany na liście węzłów, więc węzeł wygląda wszędzie tak samo. Kolor znacznika **nie** oznacza statusu online/offline. Gdy pozycja węzła aktualizuje się na żywo, jego znacznik na chwilę pulsuje. Znaczniki blisko siebie są grupowane po oddaleniu widoku. Dotknięcie grupy przybliża ją, a grupa, w której wszystkie węzły są w tym samym miejscu, pokazuje ich listę.

### Sterowanie mapą

- **Powiększanie** — szczypnięcie dwoma palcami lub przyciski +/-
- **Przesuwanie** — przeciągnij, aby przeglądać mapę
- **Wyśrodkowanie** — dotknij przycisku lokalizacji, aby mapa podążała za Twoją pozycją
- **Dotknięcie węzła** — dotknij znacznika węzła, aby zobaczyć szczegóły

Pływający pasek narzędzi daje szybki dostęp do kompasu, wyboru typu mapy i warstw, filtrów węzłów, Site Planner i śledzenia lokalizacji. Dotknij kompasu, aby ustawić mapę północą do góry, lub przycisku lokalizacji, aby mapa podążała za Twoją bieżącą pozycją. Gdy mapa za Tobą podąża, kompas zamiast tego włącza i wyłącza blokadę kierunku, czyli mapa obraca się razem z telefonem. W wersjach **Google Play** obok nich pojawia się przycisk odświeżania, gdy widoczna jest warstwa sieciowa; w wersjach **F-Droid** i **Desktop** odświeżaj warstwę sieciową z jej własnego wiersza w arkuszu warstw.

![Pływający pasek narzędzi mapy z kompasem, filtrem, odświeżaniem i przyciskami lokalizacji](../../assets/screenshots/map_controls_overlay.png)

### Filtrowanie mapy

Dotknij przycisku filtra na pływającym pasku narzędzi, aby otworzyć **Filtruj mapę**. Sekcja **Wyświetlacz** określa, co jest rysowane: **Tylko ulubione**, **Pokaż punkty trasy**, **Pokaż okręgi precyzji** oraz suwak, który ukrywa węzły niesłyszane od dłuższego czasu. **Role węzłów** to chip dla każdej roli urządzenia oraz **Wszystkie**, aby pokazać każdą rolę; zaznaczony chip oznacza, że ta rola jest widoczna. Sekcja **Węzeł** zawęża zestaw dalej opcjami **Schowaj nieaktywne węzły**, **Pokaż tylko bezpośrednie węzły**, **Pokaż tylko podpisane węzły**, **Pokaż tylko szyfrowane węzły**, **Wyklucz MQTT**, **Pokaż ignorowane węzły** i **Pokaż nierozpoznane** — to te same zasady co w [przełącznikach filtrów](nodes) listy węzłów.

Kropka na przycisku filtra oznacza, że co najmniej jeden filtr ukrywa węzły — sprawdź to, zanim uznasz, że w sieci jest cicho. Wyłączenie **Pokaż punkty trasy** ukrywa wszystkie punkty trasy, także Twoje własne. **Pokaż ignorowane węzły** dodaje je do mapy, a nie pokazuje wyłącznie ich — inaczej niż **Pokaż tylko ignorowane węzły** na liście węzłów.

## Widok AR sieci

Widok AR rysuje węzły i punkty trasy na obrazie z aparatu telefonu — każdy w kierunku i na wysokości, na której się znajduje, z odległością i wiekiem jego pozycji. Potrzebuje tylnego aparatu i czujnika orientacji, więc pojawia się tylko na telefonach z Androidem, które mają oba, zarówno w wersji Google Play, jak i F-Droid. Nie ma go na Desktopie. Po otwarciu prosi o dostęp do aparatu, a potem do lokalizacji.

Dotknij przycisku widoku sieci (**Otwórz widok sieci**) na górnym pasku mapy, aby zobaczyć całą sieć, filtrowaną tymi samymi ustawieniami **Filtruj mapę** co mapa. Widok otwierają też: menu po dotknięciu i przytrzymaniu na liście węzłów, strona szczegółów węzła, jego arkusz kompasu, dziennik pozycji i mapa traceroute. Otwarty z węzła pokazuje tylko ten węzeł oraz ślad lub trasę, z której przyszedłeś.

- **Dotknij** węzła, aby otworzyć jego szczegóły, albo punktu trasy, aby otworzyć jego informacje.
- **Przytrzymaj** węzeł lub punkt trasy, aby go znaleźć. Widok podpowiada, o ile obrócić i pochylić telefon, telefon tyka coraz szybciej w miarę zbliżania się, a gdy trafisz w cel, raz pulsuje.
- Węzeł, którego niepewność pozycji obejmuje Twoje miejsce, nie ma sensownego kierunku, więc trafia na radar na dole. Skieruj telefon w dół, aby powiększyć radar.
- Strefy punktów trasy są obrysowane, a widok informuje, gdy jesteś w jednej z nich.

Gdy pozycja telefonu nie ma wysokości, cele są na Twoim poziomie, a widok informuje, że wysokość jest przybliżona. Bez dostępu do aparatu lub gdy telefon jest zbyt gorący, widok działa dalej jako kompas.

## Punkty trasy

Punkty trasy to udostępnione miejsca warte uwagi, widoczne dla wszystkich w Twojej sieci mesh.

### Tworzenie punktu trasy

Twój węzeł musi być połączony — mapa ignoruje dotknięcie i przytrzymanie, gdy nie jest, ponieważ zapisanie punktu trasy oznacza jego rozesłanie.

1. Dotknij i przytrzymaj mapę w wybranym miejscu.
2. Wpisz nazwę i opcjonalny opis.
3. Wybierz ikonę/emoji punktu trasy.
4. Dotknij **Wyślij**, aby udostępnić go w sieci mesh.

Punkty trasy są zawsze rozsyłane do całej sieci mesh na kanale podstawowym. W przeciwieństwie do wiadomości punktu trasy nie można skierować do jednego kanału ani wysłać jako wiadomości bezpośredniej.

### Właściwości punktu trasy

| Właściwość | Opis |
|----------|-------------|
| Nazwa | Krótki identyfikator (maks. 29 znaków) |
| Opis | Opcjonalny dłuższy opis |
| Ikona | Emoji będące znacznikiem na mapie |
| Zablokowany | Jeśli zablokowany, tylko twórca może go edytować lub usunąć |
| Wygasa | Opcjonalna data i godzina automatycznego usunięcia |
| Geofence | Opcjonalny obszar alertu wejścia/wyjścia — zobacz [Geofence punktów trasy](#waypoint-geofences) |

### Wygasanie punktu trasy

Punkt trasy pozostaje, dopóki go nie usuniesz, chyba że w edytorze punktu trasy włączysz **Wygasa**. Przełącznik ustawia wygaśnięcie na osiem godzin od teraz; zmienisz to selektorami **Data** i **Godzina**. Po upływie tego czasu punkt trasy jest usuwany, co pasuje do tymczasowych znaczników, takich jak punkty zbiórki, zagrożenia lub miejsca spotkań.

Wygasłe punkty trasy są automatycznie ukrywane na mapie, aby jej nie zaśmiecały. Odliczanie do wygaśnięcia opiera się na bezwzględnym czasie, który wybrałeś, a nie na czasie liczonym od utworzenia lub otrzymania punktu trasy.

### Geofence punktów trasy

Każdy punkt trasy może też określać **geofence** — obszar alertu — aby Ty lub inni otrzymywali powiadomienie, gdy węzeł wejdzie do niego lub go opuści:

1. Ustaw **promień geofence** za pomocą gotowych chipów (lub **Wyłączony**, aby wyłączyć) albo dotknij **Ustaw obszar na mapie**, aby zamiast tego narysować własny prostokątny obszar.
2. Po ustawieniu obszaru włącz **Powiadom przy wejściu** i/lub **Powiadom przy wyjściu**.
3. Opcjonalnie włącz **Tylko ulubione**, aby ograniczyć alerty do węzłów dodanych do ulubionych.

Ponieważ punkty trasy (i ich geofence) są rozsyłane do całej sieci mesh, domyślnie alerty dostaje tylko **twórca**. Jeśli ktoś inny udostępni Ci punkt trasy z geofence, jego widok szczegółów oferuje opcję **Powiadamiaj mnie o przekroczeniach**, dzięki której możesz też otrzymywać alerty wejścia/wyjścia.

### Zarządzanie punktami trasy

- Dotknij punktu trasy, aby zobaczyć jego nazwę, opis i promień geofence. W wersjach **Google Play** pierwsze dotknięcie otwiera dymek informacyjny znacznika — dotknij dymka, aby otworzyć sam punkt trasy
- **Zablokowane punkty trasy** może zmieniać w sieci mesh tylko węzeł, który je zablokował
- Odblokowane punkty trasy może edytować każdy członek sieci mesh, gdy jest połączony z radiem — zapisanie ponownie rozsyła punkt trasy
- Potwierdzenie usunięcia usuwa tylko Twoją kopię. Aby usunąć go także z map wszystkich innych, wybierz **Usuń dla wszystkich** w oknie usuwania; to pole pojawia się tylko dla punktu trasy, który możesz zmieniać (odblokowanego lub zablokowanego przez Ciebie) i tylko gdy jesteś połączony

## Warstwy mapy

Dotknij ikony warstw na mapie, aby otworzyć **Zarządzaj warstwami map**. Pozwala ona zaimportować własne nakładki w formacie `.kml`, `.kmz` lub GeoJSON, w tym nakładki naziemne KMZ (obrazy z georeferencją, na przykład wyeksportowane kafelki topograficzne lub lotnicze), które są nakładane w podanych granicach. Dodaj nakładkę, wybierając plik przez **Dodaj warstwę**, otwierając plik w MT_SW_APP lub udostępniając go do aplikacji z innej aplikacji. Plik otwarty lub udostępniony w ten sposób jest odrzucany, jeśli ma ponad 50 MB. **Dodaj warstwę sieciową** zamiast tego przyjmuje nazwę oraz adres URL `https://` wskazujący plik KML lub GeoJSON (`http://` też działa na Desktopie, a na Androidzie tylko dla `localhost`); taka warstwa ma wtedy w arkuszu własny przycisk odświeżania. W wersjach **Google Play** przycisk odświeżania na pasku narzędzi pobiera ponownie wszystkie widoczne warstwy sieciowe naraz.

Zaimportowane warstwy są wymienione z przełącznikiem pokazywania/ukrywania każdej z nich i opcją usunięcia. Każda warstwa — zaimportowana lub wbudowana nakładka — ma własny suwak przezroczystości, gdy jest włączona, więc nakładkę można przyciemnić, a nie tylko wyłączyć. Działa to w wersji Google Play, wersji F-Droid i na **Desktopie**, który korzysta z tego samego magazynu warstw i selektora plików.

### Site Planner

**Site Planner** szacuje zasięg radiowy nadajnika i rysuje go na mapie jako kolorową nakładkę. Otwórz go przyciskiem Site Planner na pasku narzędzi mapy lub ze strony szczegółów węzła przez **Oszacuj zasięg** (pokazywane tylko dla węzłów ze znaną pozycją). Formularz ma cztery sekcje:

- **Miejsce / nadajnik**: nazwa, szerokość i długość geograficzna, **Moc TX (W)**, **Częstotliwość (MHz)**, **Wysokość anteny (m)** i **Zysk anteny (dBi)**. Chipy, takie jak **Użyj bieżącej lokalizacji węzła** i **Użyj środka mapy**, wypełniają współrzędne.
- **Odbiornik**: **Czułość odbiornika (dBm)** i **Wysokość odbiornika (m)**.
- **Opcje symulacji**: **Maksymalny zasięg (km)**, od 1 do 150.
- **Wyświetlanie**: **Paleta kolorów**.

Dotknij **Oszacuj zasięg**, aby uruchomić obliczenia. Aplikacja liczy oszacowanie sama, modelem propagacji ITU-R P.1812 na terenie z Mapterhorn — tak samo w Google Play, F-Droid i na Desktopie. Teren pobiera się przy pierwszym szacowaniu danego obszaru, więc ten krok wymaga połączenia z siecią; jeśli się nie uda, formularz wraca z uwagą, żeby sprawdzić połączenie. Gotowe oszacowanie jest dodawane w **Zarządzaj warstwami map** jako warstwa oszacowania zasięgu, a mapa przesuwa się do niego.

## Udostępnianie pozycji

### Włączanie udostępniania pozycji

Twój węzeł udostępnia pozycję GPS na podstawie:
- **Interwał transmisji** — udostępnianie pozycji według stałego zegara
- **Inteligentne Pozycjonowanie** — udostępnianie dopiero po przemieszczeniu się wystarczająco daleko; **Minimalny interwał** określa najkrótszy odstęp między transmisjami, a **Minimalny dystans** to, jak daleko musisz się przemieścić
- **Położenie stałe** — publikowanie wpisanej ręcznie szerokości, długości geograficznej i wysokości zamiast odczytu z GPS
- **Tryb GPS** — **Włączony**, **Wyłączony** lub **Nieobecny** w tym sprzęcie; dostępne tylko, gdy **Położenie stałe** jest wyłączone

Zachowanie pozycji skonfigurujesz w **Ustawienia → Konfiguracja urządzenia → Pozycjonowanie**, a zapisanie restartuje węzeł. Bez połączenia ekran otwiera się tylko do odczytu, z prośbą o połączenie przed zmianą czegokolwiek. Pełną listę pól znajdziesz w [Ustawienia — Radio i użytkownik](settings-radio-user).

### Kwestie prywatności

> 🔒 **Prywatność:** Dane o pozycji są rozsyłane do wszystkich węzłów na Twoim kanale. Jeśli nie chcesz udostępniać swojej lokalizacji, ustaw **Tryb GPS** na **Wyłączony** albo użyj **Położenia stałego** w innym miejscu. Aby nadal udostępniać pozycję, nie wskazując dokładnie swojego miejsca, edytuj kanał w **Ustawienia → Kanały**, wyłącz **Precyzyjna lokalizacja** i ustaw suwak pod nią — kanał będzie wtedy publikował przybliżony obszar, pokazany jako ± odległość, zamiast dokładnego punktu.

## Źródła map

Każda wersja oferuje wybór mapy bazowej na pasku narzędzi mapy. Wersje **Google Play** otwierają się na własnych typach map Google; wersje **F-Droid** i **Desktop** otwierają się na stylach wektorowych MapLibre. Niżej w wyborze map bazowych wszystkie trzy oferują te same rastrowe mapy bazowe:

| Mapa bazowa | Uwagi |
| --- | --- |
| Normalna / Satelita / Terenowa / Hybrydowy | Tylko Google Play — własne typy map Google |
| Liberty | Domyślna w F-Droid i Desktop. Wektorowa mapa ulic |
| Positron | Tylko F-Droid i Desktop. Mapa wektorowa o niskim kontraście; znaczniki węzłów pozostają na niej czytelne |
| Dark | Tylko F-Droid i Desktop. Mapa wektorowa dopasowana do ciemnych motywów |
| OpenStreetMap | Klasyczne rastrowe kafelki ulic |
| OpenTopoMap | Rastrowa mapa topograficzna |
| USGS Topo / USGS Imagery | Tylko obszar USA |
| Esri Topo / Esri Imagery | Obrazy topograficzne i satelitarne |

Nakładki można włączać na wierzchu dowolnej mapy bazowej, z arkusza warstw:

- **Radar pogodowy** — odbiciowość NOAA NEXRAD (obszar USA)
- **Cieniowanie rzeźby terenu** — rzeźba terenu z internetowego źródła kafelków, tylko w **F-Droid** i **Desktop**; Google
  Play rysuje cieniowanie z pobranego terenu offline. Przydatne do zrozumienia, dlaczego łącze
  nie działa, bo zasięg LoRa jest ograniczony przez teren

### Dodawanie własnego źródła kafelków

Dowolny punkt końcowy kafelków XYZ można dodać jako mapę bazową, w każdej wersji i na desktopie. Otwórz **Zarządzaj niestandardowymi źródłami kafelków** na dole wyboru mapy bazowej i wklej szablon adresu URL używający `{z}`, `{x}` i `{y}`
— oraz `{s}`, jeśli dostawca używa rotujących subdomen. Na przykład krajowa służba mapowa:

```text
https://wmts.geo.admin.ch/1.0.0/ch.swisstopo.pixelkarte-farbe/default/current/3857/{z}/{x}/{y}.jpeg
```

Na **Androidzie** szablon musi używać `https://`; zwykłe `http://` działa tylko dla `localhost`.

Kafelki są buforowane na dysku, więc przesuwanie mapy nie pobiera ponownie tego, co przed chwilą oglądałeś.

Na **Androidzie** ten sam ekran importuje też lokalne archiwum `.mbtiles` do pełnego użytku offline.

W **F-Droid** najpierw wybierz wektorową mapę bazową — Liberty, Positron lub Dark — ponieważ pobieranie jest zdefiniowane względem stylu wektorowego, a **Rozpocznij pobieranie** pozostaje wyłączone na mapie rastrowej. Ustaw na ekranie obszar, który chcesz mieć, a następnie dotknij **Rozpocznij pobieranie** w sekcji **Menedżer pamięci podręcznej** arkusza warstw: powstanie wstrzymana paczka obejmująca bieżący poziom przybliżenia plus dwa poziomy głębiej. Naciśnij odtwarzanie w wierszu paczki, aby ją pobrać. Gdy baza danych offline się otwiera, sekcja pokazuje pasek postępu. Jeśli nie można jej otworzyć, sekcja pokazuje **Mapy offline są niedostępne na tym urządzeniu**, a **Rozpocznij pobieranie** pozostaje wyłączone. Jeśli nie da się zapisać obszaru, sekcja pokazuje **Nie udało się zapisać tego obszaru do użytku offline**.

W **Google Play** sekcja **Menedżer map offline** w arkuszu warstw pobiera zamiast tego własne regiony offline
— wodę, drogi i granice administracyjne wyodrębnione z publicznego zbioru danych Protomaps, rysowane
bezpośrednio jako kształty mapy, a nie kafelki rastrowe. Ustaw obszar, który chcesz mieć, dotknij **Rozpocznij pobieranie**, a po
zakończeniu włącz **Pokaż na mapie**, aby go narysować. Przełącznik jest domyślnie wyłączony, żeby pobranie nie zasłaniało
mapy na żywo bez Twojej wiedzy. **Desktop** nie ma pobierania map bazowych offline, choć
opisany niżej teren offline działa tam bez problemu.

### Teren offline

Cieniowanie rzeźby terenu i warstwice można również pobrać do użytku offline, aby pobrany obszar nadal
pokazywał, dlaczego łącze nie działa, gdy nie ma sieci. Dane terenu pochodzą z Mapterhorn i są wskazane jako źródło na
mapie. W **Google Play** każdy pobrany region na liście **Menedżer map offline** ma przycisk **Pobierz teren**. Teren jest dołączany do tego regionu i dzieli jego limit miejsca, a obszar
zbyt duży dla terenu prosi o przybliżenie lub wybranie mniejszego regionu. Gdy teren jest pobrany, przycisk ustępuje miejsca przełącznikom **Cieniowanie rzeźby terenu** i **Warstwice**, obu wyłączonym, dopóki ich nie włączysz. W **F-Droid** i **Desktop**
arkusz warstw ma własną sekcję **Teren offline**: ustaw obszar, dotknij **Rozpocznij pobieranie**, a
cieniowanie rzeźby terenu i warstwice będą od tej pory rysowane automatycznie z pobranych kafelków, gdy pobrany obszar jest w widoku.

### Praca offline

Nad mapą pojawia się plakietka **Offline — pokazuję dane mapy z pamięci podręcznej**, gdy urządzenie nie ma połączenia z siecią. W wersjach **Google Play**,
jeśli zaimportowałeś lokalne archiwum `.mbtiles`, mapa przełącza się na nie automatycznie w chwili,
gdy sieć znika — bez przełącznika, o którym trzeba pamiętać — i wraca po odzyskaniu połączenia, o ile
w międzyczasie sam nie wybrałeś innej mapy bazowej. W **F-Droid** pobrany obszar offline nadal się
wyświetla sam; plakietka ma tam wyłącznie charakter informacyjny. **Desktop** pobiera teren offline, ale nie paczki map bazowych,
więc plakietka jest tam także tylko informacyjna.

## Powiązane tematy

- [Węzły](nodes) — podgląd i filtrowanie listy węzłów
- [Metryki węzła](node-metrics) — jakość sygnału i historia pozycji pojedynczych węzłów
- [Lokalne odkrywanie sieci mesh](discovery) — traceroute i informacje o sąsiadach do zrozumienia topologii sieci mesh
- [Jednostki i lokalizacja](units-and-locale) — formaty wyświetlania odległości i współrzędnych
