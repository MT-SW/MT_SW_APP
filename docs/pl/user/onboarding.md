---
title: Pierwsze kroki
parent: Przewodnik użytkownika
nav_order: 1
last_updated: 2026-10-21
description: Konfiguracja przy pierwszym uruchomieniu — uprawnienia, przebieg wprowadzenia i dalsze kroki po połączeniu węzła.
aliases:
  - first-launch
  - setup
  - intro
---

# Pierwsze kroki

Ta strona opisuje przebieg pierwszego uruchomienia aplikacji MT_SW_APP na Androidzie, do czego służy każde uprawnienie i jak do nich wrócić później.

## Pierwsze uruchomienie

Przy pierwszym otwarciu aplikacja prowadzi Cię przez wstępny proces, który konfiguruje niezbędne uprawnienia i ustawienia. Wykonuj kolejne kroki po kolei albo je pomijaj — nic tu nie jest jednorazową ofertą. Każde uprawnienie możesz przejrzeć i przyznać później w sekcji **Uprawnienia** w **Ustawieniach** aplikacji.

### Ekran powitalny

Ekran powitalny przedstawia MT_SW_APP w trzech wierszach funkcji:

| | |
|---|---|
| **Bądź w kontakcie wszędzie** | Komunikuj się poza siecią komórkową z przyjaciółmi i społecznością, bez zasięgu telefonii. |
| **Twórz własne sieci** | Łatwo konfiguruj prywatne sieci mesh do bezpiecznej i niezawodnej komunikacji w odległych miejscach. |
| **Śledź i udostępniaj lokalizacje** | Udostępniaj swoją lokalizację w czasie rzeczywistym i koordynuj grupę dzięki zintegrowanemu GPS. |

Dotknij **Rozpocznij**, aby przejść przez proces konfiguracji.

![Ekran powitalny](../../assets/screenshots/onboarding_welcome.png)

## Uprawnienia

Podczas konfiguracji aplikacja prosi o kilka uprawnień. Każde ma konkretne zadanie, a niektóre są wymagane do podstawowego działania.

### Uprawnienie Bluetooth

Bluetooth to główna metoda połączenia między telefonem a węzłem Meshtastic. Ekran **Bluetooth** pokazuje, co daje to uprawnienie:
- **Wykrywanie** — znajdowanie i rozpoznawanie pobliskich urządzeń.
- **Konfiguracja** — bezprzewodowe zarządzanie ustawieniami urządzenia i kanałami.

W Androidzie 12 i nowszym system pyta raz, o **Urządzenia w pobliżu**, a ta jedna zgoda obejmuje zarówno skanowanie, jak i łączenie. Bez niej musisz użyć połączenia USB lub TCP. Na sprzęcie bez Bluetooth LE konfiguracja pomija ten ekran.

### Uprawnienie lokalizacji

> ℹ️ **Uwaga:** Lokalizacja nie jest wymagana dla Bluetooth w Androidzie 12 i nowszym. **Android 11 i starsze** pokazują jeden krok lokalizacji, na ekranie Bluetooth, zamiast dwóch — te wersje traktują skanowanie Bluetooth jako funkcję lokalizacji, więc aplikacja prosi o Lokalizację zamiast o „Urządzenia w pobliżu". Pytanie dwa razy przybliżałoby moment, w którym Android w ogóle przestaje wyświetlać okno (drugie odrzucenie w Androidzie 11; pole „Nie pytaj ponownie" w Androidzie 10 i starszych). W **Androidzie 12 i nowszym** te dwa uprawnienia są osobne: „Urządzenia w pobliżu" jest zadeklarowane jako `neverForLocation`, a odmowa Lokalizacji nie przeszkadza w znajdowaniu ani łączeniu się z węzłem.

Aplikacja używa Twojej lokalizacji także do:
- Pokazywania Twojej pozycji na mapie sieci mesh
- Obliczania odległości do innych węzłów
- Udostępniania współrzędnych GPS innym członkom sieci mesh (jeśli włączone)

W Androidzie 12 i nowszym wybierz **Dokładna**, aby udostępniać pozycję sieci mesh: lokalizacja przybliżona nadal pokaże Cię na mapie, ale udostępnianie pozycji wymaga lokalizacji dokładnej.

Przyznaj **„Podczas korzystania z aplikacji"**. Aplikacja nie prosi o lokalizację w tle — `ACCESS_BACKGROUND_LOCATION` nie ma w jej manifeście — więc Android nie zaoferuje opcji „Zawsze", a pozycja jest aktualizowana, gdy aplikacja jest na pierwszym planie lub działa jej usługa pierwszoplanowa.

Odmowa nie psuje reszty aplikacji: w Androidzie 12 i nowszym Bluetooth pozostaje bez zmian, a wyłączone są tylko pozycja na mapie i udostępnianie pozycji. W Androidzie 11 i starszych zatrzymuje się też skanowanie Bluetooth, bo właśnie za tym uprawnieniem Android je ukrywa — a systemowe **Usługi lokalizacji** także muszą być włączone, aby skan cokolwiek zwrócił.

### Uprawnienie powiadomień

Powiadomienia informują Cię o:
- Przychodzących wiadomościach z kanałów i wiadomościach bezpośrednich
- Nowych węzłach dołączających do sieci mesh
- Niskim poziomie baterii w zdalnym węźle

Preferencje powiadomień możesz dopracować później w ustawieniach systemu Android — aplikacja tworzy osobny kanał powiadomień dla każdej kategorii (oraz kilka wewnętrznych, np. usługę w tle), więc możesz je włączać lub wyciszać osobno.

### Uprawnienie alertów krytycznych

Alerty krytyczne to powiadomienia o wysokim priorytecie, które przebijają się przez tryb Nie przeszkadzać — dla alarmów awaryjnych z sieci mesh i pilnych wiadomości.

Ten krok nie jest zwykłym oknem z prośbą o uprawnienie. Nie ma okna zezwól/odmów: przycisk otwiera stronę ustawień systemu Android dla kanału **Powiadomienia alertowe** tej aplikacji, gdzie sam włączasz przebijanie się przez tryb Nie przeszkadzać. Dotknij **Skonfiguruj alerty krytyczne**, aby otworzyć tę stronę, lub **Pomiń**, aby iść dalej — do tej samej strony wrócisz później z ustawień powiadomień Androida dla aplikacji. Ten krok pojawia się tylko wtedy, gdy na poprzednim ekranie zezwolono na powiadomienia — jeśli je pominiesz lub odmówisz, konfiguracja kończy się na tym.

### Późniejsze przeglądanie uprawnień

Sekcja **Uprawnienia** w **Ustawieniach** podsumowuje stan każdego uprawnienia. W Androidzie 12 i nowszym wymienia pięć: **Uprawnienie do pobliskich urządzeń** (Bluetooth), **Uprawnienie do lokalizacji**, **Powiadomienia aplikacji**, **Pozwolenie na dostęp do kamery** (skanowanie kodów QR kanałów i kontaktów) oraz **Uprawnienie do sieci lokalnej** (znajdowanie węzłów w Twojej sieci Wi-Fi i łączenie się z nimi). Na sprzęcie bez Bluetooth LE wiersz **Uprawnienie do pobliskich urządzeń** jest pomijany. W Androidzie 11 i starszych jeden wiersz **Uprawnienie do lokalizacji** obejmuje Bluetooth i lokalizację, więc są cztery. Dwa ostatnie nigdy nie są wymagane podczas konfiguracji, tylko wtedy, gdy funkcja po raz pierwszy ich potrzebuje.

Sekcja pokazuje *Wszystkie dozwolone*, gdy każde uprawnienie jest przyznane, *Nic nie wymaga Twojej uwagi*, gdy o niektóre jeszcze nie pytano, i podaje liczbę, gdy któreś odrzucono — wtedy sama się rozwija. Dotknij wiersza, aby w każdej chwili rozwinąć lub zwinąć sekcję:

| Stan | Co robi dotknięcie wiersza |
|---|---|
| **Dozwolone** | Otwiera stronę systemową, abyś mógł je sprawdzić lub cofnąć |
| **Jeszcze nie zapytano** | Prosi o nie |
| **Odmówiono — dotknij, aby zezwolić** | Wyjaśnia, do czego służy uprawnienie, a potem pyta ponownie, jeśli się zgodzisz |
| **Zablokowane — dotknij, aby otworzyć ustawienia systemowe** | Android nie pokaże już swojego okna, więc otwiera stronę, na której możesz je włączyć z powrotem |
| **Niewymagane w tej wersji Androida** | Nic — to uprawnienie nie istnieje na Twoim urządzeniu |
| **Tylko przybliżona** | Pokazywane w wierszu lokalizacji, gdy udostępnianie pozycji jest włączone, a Android daje tylko przybliżoną lokalizację. Udostępnianie wymaga dokładnej lokalizacji |

Najbardziej liczy się to przy powiadomieniach. Jeśli odmówisz ich podczas konfiguracji, ten wiersz jest drogą powrotną: Android przestaje pokazywać okno, gdy odmówisz stanowczo (drugie odrzucenie), i wtedy ten wiersz przełącza się na **Zablokowane** i kieruje Cię do strony ustawień systemowych. Okno z prośbą o powiadomienia istnieje tylko w Androidzie 13 i nowszym — w starszych wersjach powiadomienia są domyślnie włączone i zarządza się nimi z ustawień Androida.

## Po konfiguracji

Po przyznaniu uprawnień aplikacja otwiera główny interfejs. Pierwszą rzeczą powinno być połączenie z węzłem Meshtastic — zobacz [Połączenia](connections), aby poznać szczegóły.

> 💡 **Wskazówka:** Jeśli podczas konfiguracji pominąłeś jakieś uprawnienia, otwórz sekcję **Uprawnienia** w **Ustawieniach** aplikacji. Każde uprawnienie jest tam wymienione ze stanem i drogą powrotną do niego — także powiadomienia, o które system nie zapyta drugi raz samodzielnie.

Funkcje proszą też o uprawnienia w kontekście. Na karcie **Połączenie** karta nad listą urządzeń wyjaśnia, do czego służy uprawnienie Bluetooth, i oferuje **Przyznaj uprawnienie**; gdy Android przestanie pytać, ten przycisk zmienia się na **Otwórz ustawienia**.

Nowy w Meshtastic? [Poradnik na początek](https://meshtastic.org/docs/getting-started) na meshtastic.org opisuje wybór sprzętu, wstępną konfigurację węzła i pierwszą sieć mesh.

## Powiązane tematy

- [Połączenia](connections) — sparuj pierwszy węzeł
- [Wiadomości i kanały](messages-and-channels) — wyślij pierwszą wiadomość
- [Węzły](nodes) — zobacz, kto jeszcze jest w Twojej sieci mesh
- [Mapa i punkty trasy](map-and-waypoints) — podgląd pozycji węzłów
- [Ustawienia — Radio i użytkownik](settings-radio-user) — konfiguracja węzła i profilu użytkownika
