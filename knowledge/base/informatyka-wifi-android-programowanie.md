# Informatyka: Wi-Fi, Android, WSL, Kotlin Multiplatform, Gradle, SQLite

### Wi-Fi 2,4 GHz: kanały, zasięg i zakłócenia
Na 2,4 GHz kanały są co 5 MHz, ale każdy zajmuje ok. 20 MHz, więc sąsiednie nakładają się; trzy niezachodzące na siebie to 1, 6 i 11 (w Europie i Japonii także 1, 5, 9, 13). Pasmo 5 GHz ma co najmniej 23 kanały po 20 MHz, które można łączyć w 40, 80 lub 160 MHz; ma krótszy zasięg, bo budynki mocniej je tłumią. Standardy: Wi-Fi 4 (802.11n) 2,4 i 5 GHz, Wi-Fi 5 tylko 5 GHz, Wi-Fi 6 2,4 i 5 GHz, Wi-Fi 6E dodaje 6 GHz, Wi-Fi 7 (2024). Wewnątrz zasięg to ok. 20 m. Pasmo 2,4 GHz dzielą z Wi-Fi kuchenki mikrofalowe, telefony bezprzewodowe, Bluetooth, Zigbee i kamery. WEP jest złamany, WPA przestarzałe, WPA2 bezpieczne przy silnym haśle, WPA3 nowsze; WPS ma lukę, więc wyłącz; ukrycie nazwy sieci niczego nie chroni. Węzły ESP32 łączą się tylko z siecią 2,4 GHz.
Słowa: Wi-Fi, 2,4 GHz, 5 GHz, kanał 1 6 11, WPA2, WPA3, WPS, zakłócenia, mikrofalówka, router, zasięg Wi-Fi, ESP32 Wi-Fi
Źródło: https://en.wikipedia.org/wiki/Wi-Fi (sprawdzono 2026-10; ostatnie zdanie z dokumentacji Meshtastic)

### Android: Doze i oszczędzanie baterii a działanie w tle
Doze (od Androida 6): gdy telefon nie jest używany ani ruszany, trafia w stan niskiego poboru; ograniczony jest wtedy dostęp do sieci i praca w tle, a dostarczane są tylko powiadomienia o wysokim priorytecie. App Standby opóźnia sieć aplikacjom, z których użytkownik dawno nie korzystał. Aplikacja może poprosić o wyłączenie się z tych ograniczeń, ale Google Play traktuje takie prośby jako naruszenie, jeśli podstawowa funkcja nie cierpi z powodu ograniczeń. To tłumaczy, czemu aplikacja łącząca się z węzłem przez Bluetooth w tle może się rozłączać, jeśli system ją uśpi (wniosek ogólny). Od Androida 11 aplikacje mają automatyczny dostęp tylko do mediów i własnych katalogów, resztę przez Storage Access Framework. Najnowsze wersje: Android 14 (2023), 15 (2024), 16 (czerwiec 2025), 17 (czerwiec 2026).
Słowa: Doze, App Standby, oszczędzanie baterii, praca w tle, rozłączanie, Android 14, Android 15, Android 16, SAF, uprawnienia pamięci
Źródło: https://en.wikipedia.org/wiki/Doze_(Android) i https://en.wikipedia.org/wiki/Android_(operating_system) (sprawdzono 2026-10; wniosek o rozłączaniu Bluetooth jest ogólny)

### WSL: Linux w Windows
Windows Subsystem for Linux pozwala uruchamiać Linuksa w Windows bez pełnej maszyny wirtualnej. WSL 1 tłumaczy wywołania systemowe Linuksa na jądro Windows (bez prawdziwego jądra Linuksa, bez modułów jądra). WSL 2 uruchamia prawdziwe jądro Linuksa w lekkiej maszynie Hyper-V, jest zgodny z większą liczbą programów i ma znacznie szybsze operacje dyskowe; wymaga Windows 11 lub Windows 10 w wersji 1903 i nowszej. Użycie: powłoka i narzędzia GNU, Python, programy GUI przez WSLg. Źródło nie opisuje dostępu do urządzeń USB (np. do wgrywania firmware); WSL 1 nie obsługuje sterowników.
Słowa: WSL, WSL2, Linux w Windows, Hyper-V, Windows 11, narzędzia GNU, WSLg, USB w WSL
Źródło: https://en.wikipedia.org/wiki/Windows_Subsystem_for_Linux (sprawdzono 2026-10)

### Kotlin Multiplatform i Compose Multiplatform
Kotlin Multiplatform (KMP) od JetBrains pozwala dzielić kod między platformami (Android, iOS, macOS, Windows, Linux, web), zwykle logikę biznesową, sieć i dane, a kod specyficzny dla platformy pisać natywnie. Compose Multiplatform to powiązany deklaratywny framework interfejsu na bazie Jetpack Compose. KMP uznano za stabilny w listopadzie 2023, Google ogłosił oficjalne wsparcie w maju 2024, Compose Multiplatform dla iOS stał się stabilny w maju 2025. Nasza aplikacja MT_SW jest budowana w ten sposób (jeden kod dla Androida i komputerów).
Słowa: KMP, Kotlin Multiplatform, Compose Multiplatform, JetBrains, wieloplatformowy, jeden kod, Android i desktop
Źródło: https://en.wikipedia.org/wiki/Kotlin_Multiplatform (sprawdzono 2026-10; ostatnie zdanie o aplikacji MT_SW z ustaleń projektu)

### Gradle: czym jest i jak działa wrapper?
Gradle to narzędzie do automatyzacji budowania (kompilacja, pakowanie, testy, publikacja) dla Javy, Kotlina i innych; skrypty pisze się w języku opartym na Groovy lub Kotlinie. Zalecany sposób uruchamiania to wrapper (gradlew), który sam pobiera wersję Gradle zadeklarowaną w projekcie. Zadania tworzą graf zależności, wykonywany szeregowo lub równolegle; budowanie przyrostowe pomija to, co jest aktualne, a pamięć podręczna (build cache) może być współdzielona. Wtyczki dodają zadania. Źródło nie opisuje poleceń ani przyczyn wolnego budowania.
Słowa: Gradle, gradlew, wrapper, build, zadanie, wtyczka, cache budowania, build.gradle.kts, kompilacja aplikacji
Źródło: https://en.wikipedia.org/wiki/Gradle (sprawdzono 2026-10)

### SQLite: baza w jednym pliku
SQLite to otwarty silnik relacyjnej bazy danych, biblioteka wbudowywana w aplikacje, bez osobnego procesu serwera; cała baza (tabele, indeksy, dane) leży w jednym pliku. Jest domyślnie w Androidzie. Tryb WAL (od wersji 3.7) pozwala czytać i pisać równocześnie; bez niego blokada na poziomie pliku pozwala na zapis po jednym. Maksymalny rozmiar bazy to 281 TB; można też mieć bazę w pamięci. Ma ograniczone ALTER TABLE, co komplikuje migracje schematu.
Słowa: SQLite, baza danych, WAL, jeden plik, Android baza, migracja, ALTER TABLE, Room
Źródło: https://en.wikipedia.org/wiki/SQLite (sprawdzono 2026-10; źródło nie wspomina biblioteki Room)
