---
title: Odkrywanie lokalnej sieci mesh
parent: Przewodnik użytkownika
nav_order: 12
last_updated: 2026-10-12
description: Poznaj swoją sieć mesh — skaner Odkrywania lokalnej sieci mesh, trasy traceroute, mapy sąsiadów i narzędzia do odkrywania węzłów.
aliases:
  - discovery
  - local-mesh-discovery
  - mesh-discovery
  - local-discovery
  - network-scan
  - traceroute
  - neighbor-info
---

# Odkrywanie lokalnej sieci mesh

Narzędzia odkrywania pomagają zrozumieć, **jak** połączona jest Twoja sieć mesh — które węzły słyszą się nawzajem, jakimi ścieżkami podróżują wiadomości oraz gdzie są wąskie gardła lub słabe łącza.

Aplikacja oferuje dwa uzupełniające się podejścia:

- **Odkrywanie lokalnej sieci mesh (skaner)** — automatyczny tryb, który przełącza Twój podłączony węzeł między różnymi presetami LoRa, nasłuchuje na każdym i ustala ranking, który preset działa najlepiej w Twojej lokalizacji.
- **Ręczne badanie** — traceroute, Informacje o sąsiadach i lista węzłów, z których możesz korzystać w dowolnej chwili, aby badać konkretne ścieżki i topologię.

## Odkrywanie lokalnej sieci mesh (skaner)

Odkrywanie lokalnej sieci mesh to dedykowany tryb skanowania, który pomaga znaleźć najlepszy preset modemu LoRa dla Twojej lokalizacji i zobaczyć, które węzły są aktywne na każdym presecie. Przełącza Twój podłączony węzeł przez jeden lub więcej wybranych presetów, zatrzymuje się na każdym — nasłuchuje przez ustalony czas — aby zebrać pakiety, a następnie analizuje i porządkuje wyniki.

Podłącz węzeł, a następnie otwórz **Ustawienia → Zaawansowane → Odkrywanie lokalnej sieci mesh**. W Androidzie sekcja **Zaawansowane** pojawia się tylko dla lokalnie połączonego węzła, nigdy przez zdalne zarządzanie, i pozostaje wyszarzona, dopóki aplikacja nie skończy odczytywać konfiguracji węzła. Na urządzeniu zarządzanym jej pozycje są wyłączone, z wyjątkiem **Panelu debugowania**, który czyta lokalne dzienniki aplikacji i pozostaje dostępny. Na komputerze Odkrywanie lokalnej sieci mesh ma własną pozycję na ekranie Ustawień, bez takiego ograniczenia.

> ℹ️ **Uwaga:** Odkrywanie tymczasowo zmienia ustawienia LoRa Twojego węzła na czas skanowania, a po zakończeniu przywraca Twoją oryginalną konfigurację.

Każdy preset jest skanowany w sieci publicznej: domyślny slot częstotliwości dla tego presetu, dekodowany domyślnym kanałem publicznym. Jeśli Twój węzeł używa własnego slotu częstotliwości, skan używa slotu domyślnego i przywraca Twój slot po zakończeniu. Jeśli Twój kanał główny ma własną nazwę lub klucz, skan przełącza go na domyślny kanał publiczny przed pierwszą zmianą presetu i przywraca Twój kanał po zakończeniu skanu — niezależnie od tego, czy skan się zakończy, zatrzymasz go, czy się nie powiedzie. Zmiany kanału nie restartują węzła, więc dzieje się to od razu na początku i na końcu skanu. Nie musisz już ustawiać na kanale głównym domyślnego klucza przed skanowaniem.

### Przygotowanie skanu

Przed rozpoczęciem skonfiguruj te elementy:

| Element | Opis |
|---------|-------------|
| **Wybór presetów LoRa** | Wybierz jeden lub więcej presetów do przeskanowania. Odkrywanie zatrzymuje się po kolei na każdym wybranym presecie. |
| **Czas nasłuchu** | Czas nasłuchu na każdym presecie. Wybierz spośród 1, 5, 15, 30, 45, 60, 90, 120 lub 180 minut. Dłuższy czas nasłuchu zbiera więcej pakietów i daje wyraźniejszy obraz, ale trwa dłużej. |
| **Nie wygaszaj ekranu** | Utrzymuje włączony wyświetlacz podczas skanu. Sam skan trzyma blokadę wybudzenia procesora przez cały czas działania i publikuje powiadomienie **Skanowanie presetów LoRa…**, więc zbiera dane dalej przy wyłączonym ekranie lub aplikacji w tle. |

Przycisk **Rozpocznij skan** pozostaje wyłączony — z wyjaśnieniem, dlaczego — dopóki skan nie może się uruchomić. Typowe powody wyłączenia:

- Węzeł jest **niepołączony**.
- **Nie wybrano presetów** do skanowania.
- Wybrany preset używa pasma **2,4 GHz**, którego Twój sprzęt nie obsługuje.

### Postęp na żywo

Podczas skanu Odkrywanie pokazuje bieżący etap:

| Etap | Co się dzieje |
|-------|------------------|
| **Przygotowywanie skanu** | Zapisywanie Twojej bieżącej konfiguracji LoRa i kanału głównego oraz przygotowanie do skanu. |
| **Przełączanie na \<preset\>** | Przełączanie węzła na kolejny testowany preset. |
| **Ponowne łączenie na \<preset\>** | Ponowne nawiązywanie połączenia po zmianie presetu. |
| **Nasłuch na \<preset\>** | Nasłuchiwanie na bieżącym presecie w celu zebrania pakietów, z odliczaniem do następnego kroku. |
| **Analizowanie wyników** | Przetwarzanie zebranych pakietów i porządkowanie presetów w ranking. |
| **Przywracanie presetu domowego** | Przywracanie Twojej oryginalnej konfiguracji LoRa. |
| **Anulowanie skanowania** | Dotknąłeś **Zatrzymaj skan**; wyniki częściowe są zapisywane, zanim oryginalny preset zostanie przywrócony. |
| **Skan nieudany: \<przyczyna\>** | Skan nie mógł być kontynuowany — najczęściej dlatego, że węzeł nie wrócił w ciągu minuty od zmiany presetu. Dotychczas zebrane wyniki są zapisane, a oryginalny preset zostaje przywrócony automatycznie. |

![Odliczanie nasłuchu pokazujące czas pozostały na bieżącym presecie](../../assets/screenshots/discovery_dwell_progress.png)

Jeśli skan zostanie przerwany — aplikacja zostanie zamknięta lub węzeł zniknie — aplikacja przywróci Twój oryginalny preset i kanał główny przy następnym połączeniu z tym węzłem i poinformuje Cię o tym. Połącz ponownie ten sam węzeł, aby to nastąpiło; dopóki tego nie zrobisz, węzeł pozostaje na tym presecie i kanale, na którym zostawił go skan.

### Odczytywanie wyników

Gdy skan się zakończy, Odkrywanie przedstawia kartę wyników dla każdego przetestowanego presetu oraz ogólne podsumowanie.

![Karta wyników presetu z rankingiem i zebranymi metrykami](../../assets/screenshots/discovery_preset_result.png)

Metryki obejmują:

| Metryka | Co mówi |
|--------|-------------------|
| Kondycja RF | Ogólna jakość środowiska radiowego na tym presecie. |
| Wykorzystanie kanału | Jak zajęty był eter podczas nasłuchu. |
| Czas eteru | Zaobserwowany czas nadawania. |
| Węzły bezpośrednie a przekazywane | Ile węzłów sieci mesh usłyszano bezpośrednio, a ile przez przekaźnik. |
| Błędne / zduplikowane pakiety | Liczba uszkodzonych i powtórzonych pakietów, wskazująca na zatłoczenie lub zakłócenia. |

Dodatkowe funkcje dostępne z poziomu wyników:

- **Historia skanów** — zapisane sesje, do których możesz wrócić; przeglądaj lub usuwaj dawne skany.
- **Mapa odkrywania** — mapa węzłów znalezionych podczas skanu.
- **Eksport raportu** — wyeksportuj raport jako PDF w Androidzie lub jako tekst na innych platformach.

> 💡 **Wskazówka:** W wersjach z **Google Play** Odkrywanie może wygenerować podsumowanie wyników przez AI na urządzeniu (Gemini Nano). Wersje z F-Droid zawsze używają podsumowania algorytmicznego — zastrzeżona zależność ML Kit jest z tej wersji celowo wyłączona — więc w obu przypadkach dostajesz czytelną interpretację skanu.

## Meshbeacon

Meshbeacon pozwala węzłom zapraszać innych do dołączenia do ich sieci mesh. Węzeł nadający beacon okresowo rozgłasza zaproszenie — opcjonalnie ogłaszając kanał, region i preset modemu — które pobliskie węzły mogą usłyszeć, nawet zanim dzielą wspólną konfigurację.

Skonfiguruj go w **Ustawienia → Ustawienia modułu → Meshbeacon**. Pozycja pojawia się tylko na węzłach z firmware 2.8.0 lub nowszym. Wiersz **Region** tylko do odczytu na górze ekranu pokazuje region ogłaszany przez beacon: ten region i preset są zawsze tymi, których używa sam węzeł, więc beacon nie może zapraszać nikogo do ustawień, na których Twój węzeł nie działa.

- **Nasłuchuj pakietów meshbeacon** — odbieraj zaproszenia rozgłaszane przez inne węzły.
- **Nadawaj pakiety meshbeacon** — okresowo ogłaszaj tę sieć mesh pobliskim węzłom, z opcjonalną **Wiadomością meshbeacon** do 100 bajtów, **Interwałem nadawania** wybieranym ze stałych interwałów od 1 godziny do 72 godzin oraz **Oferowanym kanałem** wybranym spośród kanałów Twojego węzła. Oferowany kanał jest wymagany i domyślnie jest nim Twój kanał główny. Przez zdalne zarządzanie selektor oferuje tylko kanał główny.
- **Cele nadawania** — gdzie beacon faktycznie nadaje. Lista zawsze zawiera co najmniej jeden wiersz: pierwszy to własna transmisja beacona, a nie dodatkowa. Każdy wiersz wybiera **Kanał** i **Preset nadawania**. **Dodaj cel** dopisuje wiersz, a **Usuń cel** usuwa jeden — usunięcie ostatniego wiersza zastępuje go nowym domyślnym zamiast opróżniać listę.

Dwa warunki blokują konfigurację beacona:

- **Węzeł nie ma ustawionego regionu.** Ekran nie pokazuje nic poza *Ustaw region radia przed skonfigurowaniem beacona.* Najpierw ustaw region w **Ustawienia → LoRa**.
- **Węzeł używa własnych ustawień LoRa.** Beacon ogłasza preset modemu, do którego inni mogą dołączyć, więc węzeł z wyłączonym **Użyj predefiniowanych ustawień** nie ma standardowego presetu do zaoferowania. W takim stanie **Nadawaj pakiety meshbeacon** można wyłączyć, ale nie włączyć, a ustawienia nadawania są tylko do odczytu. Nasłuchiwanie beaconów pozostaje bez zmian.

Odebrane zaproszenia pojawiają się na ekranie Odkrywania jako karty **Zaproszenia do sieci mesh**. Każda karta pokazuje wiadomość nadawcy oraz oferowany kanał, region, preset i jakość sygnału, z następującymi akcjami:

- **Dołącz** — przełącz się na oferowany kanał i preset (przestraja węzeł i restartuje go). Gdy oferta zgadza się z Twoim bieżącym slotem częstotliwości, akcja **Dodaj kanał** dodaje go bez restartu.
- **Odkryj** — zasiej skan Odkrywania oferowanym presetem, aby zbadać tę sieć mesh przed dołączeniem (pokazywane tylko wtedy, gdy beacon oferuje preset).
- **Zamknij** — zignoruj zaproszenie.

Kanały ogłaszane przez beacony pojawiają się też w przygotowaniu skanu jako **Kanały meshbeacon** — wybierz jeden, aby dołączyć go jako cel skanu.

Zaproszenie do sieci mesh, w której Twój węzeł już jest, jest wstrzymywane: bez karty, bez powiadomienia i bez pozycji w **Kanały meshbeacon**. Kanał liczy się jako już posiadany tylko wtedy, gdy zarówno jego nazwa, jak i klucz zgadzają się z kanałem na Twoim węźle — ta sama nazwa z innym kluczem to inna sieć mesh, więc takie zaproszenie nadal do Ciebie dotrze.

## Ręczne badanie

Poniższe narzędzia są dostępne w dowolnej chwili z listy węzłów i ekranów szczegółów węzła. Używaj ich do badania konkretnych ścieżek i budowania obrazu topologii, razem z pełnym skanem lub zamiast niego.

### Traceroute

Traceroute pokazuje dokładną ścieżkę, jaką wiadomość pokonuje od Twojego węzła do dowolnego innego węzła w sieci mesh. To najprzydatniejsze narzędzie do diagnozowania problemów z łącznością.

#### Uruchamianie traceroute

1. Przejdź do **Węzłów** i dotknij węzła, do którego chcesz prześledzić trasę.
2. Na ekranie szczegółów węzła znajdź **Pokaż trasę** w sekcji **Telemetria** i dotknij jej przycisku żądania. Gdy nadejdzie wynik, drugi przycisk w tym samym wierszu otwiera dziennik traceroute, w którym każdy skok jest wymieniony wraz z jakością sygnału.

#### Odczytywanie wyników

Wynik traceroute wygląda tak:

```text
Route traced toward destination:

■ Your Node (YOUR)
⇊ 8.5 dB
■ Relay Node (RLAY)
⇊ -8.75 dB
■ Target Node (TGT1)
```

Każdy wiersz `⇊` między dwoma węzłami to jeden skok przez przekaźnik, a SNR w tym wierszu to jakość tylko tego odcinka. Aplikacja koloruje go względem progu demodulacji używanego presetu, a nie stałej liczby: zielony powyżej progu, żółty do 5,5 dB poniżej, pomarańczowy do 7,5 dB, a czerwony dalej. Próg wynosi −7,5 dB dla Short Fast i poprawia się o 2,5 dB na każdy stopień współczynnika rozpraszania, więc dla Long Fast jest to −17,5 dB — ten sam SNR jest odczytywany inaczej na różnych presetach. Zobacz [Miernik sygnału](signal-meter). Żądanie, które dostaje też odpowiedź, dodaje drugi blok pod **Route traced back to us:**.

| Na co zwrócić uwagę | Co to znaczy |
|------------------|---------------|
| Wszystkie skoki mają dobry SNR (zielony) | Zdrowa ścieżka — wiadomości płyną niezawodnie |
| Jeden skok ma słaby SNR (pomarańczowy lub czerwony) | Słabe łącze — ten odcinek przez przekaźnik jest wrażliwy |
| Wiele skoków (4+) | Długa ścieżka — rozważ przestawienie węzła, aby ją skrócić |
| Inna ścieżka przy ponownej próbie | Sieć mesh dostosowuje się — istnieje wiele tras (to dobrze!) |

> 💡 **Wskazówka:** Uruchom traceroute kilka razy w ciągu kilku minut. Jeśli ścieżka się zmienia, Twoja sieć mesh ma nadmiarowe trasy — to znak dobrze połączonej sieci.

#### Rozwiązywanie problemów za pomocą traceroute

- **Brak odpowiedzi** — Traceroute nic nie odebrał. Węzeł docelowy może być offline, poza zasięgiem lub na innym kanale. Sprawdź, czy oba węzły mają co najmniej jeden wspólny kanał z tym samym kluczem szyfrowania.
- **Traceroute przekracza limit czasu** — Ścieżka może być zbyt długa (przekracza limit skoków) albo węzeł przekaźnikowy jest zatłoczony. Spróbuj zwiększyć limit skoków w **Ustawienia → LoRa**.
- **Nie można wyświetlić trasy na mapie, ponieważ węzeł początkowy lub końcowy nie posiada aktualnej lokalizacji** — Ścieżka została prześledzona, ale jeden z końców nigdy nie udostępnił pozycji.
- **Ścieżki asymetryczne** — Traceroute z A→B może iść inną ścieżką niż B→A. To normalne — propagacja fal radiowych nie zawsze jest symetryczna.

### Informacje o sąsiadach

Moduł Informacje o sąsiadach pozwala każdemu węzłowi rozgłaszać listę węzłów, które **słyszy bezpośrednio** (jeden skok). Gdy wiele węzłów udostępnia swoje listy sąsiadów, możesz poskładać mapę topologii całej sieci mesh.

#### Włączanie Informacji o sąsiadach

1. Przejdź do **Ustawienia → Ustawienia modułu → Informacje o sąsiadach**.
2. Włącz moduł.
3. Ustaw **Częstotliwość aktualizacji**. Aplikacja przyjmuje wszystko, co wpiszesz; firmware wymusza własne minimum i resetuje wartość poniżej niego.
4. Włącz **Nadaj przez LoRa**. Bez tego Twoja lista sąsiadów trafia tylko do MQTT i do tej aplikacji, nigdy drogą radiową.

Po włączeniu i nadawaniu przez LoRa Twój węzeł okresowo rozgłasza listę sąsiadów. Inne węzły z włączonymi Informacjami o sąsiadach robią to samo.

#### Przeglądanie danych o sąsiadach

- Otwórz ekran szczegółów węzła i znajdź **Informacje o sąsiadach** w sekcji **Telemetria**. Przycisk żądania prosi węzeł o jego bieżącą listę sąsiadów; gdy aplikacja ją otrzyma, drugi przycisk w tym samym wierszu otwiera dziennik wszystkiego, co ten węzeł zgłosił. Wiersz pojawia się tylko na węzłach, które mogą odpowiedzieć na żądanie o sąsiadów albo które już zgłosiły sąsiadów.
- Każdy wpis sąsiada pokazuje węzeł, który został usłyszany bezpośrednio, i jakość jego sygnału.
- Połącz dane o sąsiadach z wielu węzłów, aby zrozumieć pełną topologię sieci mesh.

> ℹ️ **Uwaga:** Informacje o sąsiadach zwiększają zużycie czasu antenowego, ponieważ każdy włączony węzeł okresowo rozgłasza swoją listę sąsiadów. Z tego powodu firmware nie przyjmuje interwału krótszego niż 14400 sekund (4 godziny); w zajętych sieciach mesh zostaw domyślne 21600 sekund lub zwiększ jeszcze bardziej.

### Lista węzłów jako narzędzie odkrywania

Sama lista węzłów jest potężnym narzędziem odkrywania, gdy skutecznie używasz jej filtrowania i sortowania.

#### Znajdowanie nowych węzłów

- Posortuj według **Aktywność**, aby zobaczyć na górze ostatnio aktywne węzły.
- Włącz **Pokaż nierozpoznane**, aby zobaczyć węzły, które pojawiły się w sieci mesh, ale nie wysłały jeszcze informacji o użytkowniku — często są to świeżo włączone węzły.

#### Ocena łączności

- Posortuj według **Liczba skoków**, aby zobaczyć, które węzły są osiągalne bezpośrednio (0 skoków), a które przez przekaźniki.
- Posortuj według **Dystans**, aby znaleźć pobliskie węzły i sprawdzić, czy są osiągalne.
- Użyj **Wyklucz MQTT**, aby skupić się na węzłach osiągalnych przez LoRa (nie przez most internetowy).

#### Przegląd infrastruktury

- Wyłącz **Wyklucz infrastrukturę**, aby zobaczyć węzły Router, Router pomocniczy i Klient bazowy.
- Sprawdź ich jakość sygnału i czasy ostatniej aktywności, aby upewnić się, że Twoje węzły infrastruktury są w dobrej kondycji.

Pełne informacje o opcjach filtrowania i sortowania znajdziesz w [Węzły](nodes).

## Wskazówki do badania sieci mesh

- **Zacznij od traceroute** — daje natychmiastowe, praktyczne informacje o konkretnej ścieżce.
- **Włącz Informacje o sąsiadach na kluczowych węzłach** — zwłaszcza routerach i repeaterach, aby zbudować obraz szkieletu sieci.
- **Sprawdź mapę** — pozycje węzłów na [Mapie](map-and-waypoints) w połączeniu z danymi o sygnale pomagają zrozumieć, dlaczego niektóre łącza są silne, a inne słabe.
- **Porównuj sygnał w czasie** — użyj przewodnika [Miernik sygnału](signal-meter), aby poprawnie interpretować wartości SNR i RSSI.

## Zobacz także

- [Węzły](nodes) — lista węzłów, którą wypełniają te skany
- [Mapa i punkty trasy](map-and-waypoints) — zobacz odkryte węzły na mapie
- [Miernik sygnału](signal-meter) — interpretuj SNR i RSSI zgłaszane przez skan
- [Ustawienia — Moduły i administracja](settings-module-admin) — konfiguracja modułów Meshbeacon i Informacje o sąsiadach
- [Wiadomości i kanały](messages-and-channels) — dołącz do znalezionej sieci mesh i zacznij rozmawiać
