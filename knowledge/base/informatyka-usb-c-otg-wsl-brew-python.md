# USB-C, OTG, Android, WSL, Homebrew i Python

### USB-C: kable tylko do ładowania i dane
USB-C to 24-pinowe odwracalne złącze, a nie protokół: przez nie mogą iść USB 2.0, USB 3.x, USB4, DisplayPort i Thunderbolt. Bazowa moc magistrali to 5 V (opcjonalnie 1,5 lub 3 A), a USB PD negocjuje przez linię CC do 20 V i 5 A (100 W), w rozszerzonym zakresie do 240 W. Kable "tylko do ładowania" mają zredukowane okablowanie: tylko USB 2.0 i zasilanie, więc do wgrywania firmware wybieraj kabel z danymi. Kable z większym prądem i prędkością mają układ E-Marker. Piny CC wykrywają urządzenie; stąd wiele kabli USB-A na USB-C działa tylko w jedną stronę (ładowanie urządzenia USB-C ze źródła USB-A). Spotyka się niezgodne kable, np. z błędnym rezystorem 10 kΩ zamiast 56 kΩ, które mogą uszkodzić ładowarki. Zasilanie z USB-C do USB-C bywa kłopotliwe dla niektórych płytek (patrz Heltec).
Słowa: USB-C, kabel danych, kabel do ładowania, USB PD, CC, E-Marker, rezystor 5,1 k, kabel USB, wgrywanie firmware kabel, Power Delivery
Źródło: https://en.wikipedia.org/wiki/USB-C (sprawdzono 2026-10; ostatnie zdanie: odesłanie do wpisu Heltec)

### USB OTG: telefon jako host
OTG (z 2001 r., dodatek do USB 2.0) pozwala urządzeniu przełączać się między rolą hosta a urządzenia podrzędnego; telefon z trybem hosta może czytać pendrive i łączyć się z klawiaturą, myszą czy aparatem. Rolę ustala pin ID w złączu (A-device zasila i startuje jako host, B-device pobiera zasilanie). Typowy kabel OTG ma wtyk micro-A i micro-B; USB-C to ułatwia. Wsparcie hosta zależy od modelu telefonu i trzeba je sprawdzić w specyfikacji. Host OTG zasila tylko urządzenia z listy docelowej, więc większe urządzenia mogą wymagać zasilanego huba. Połączenie telefonu z węzłem Meshtastic przez USB korzysta właśnie z tego trybu.
Słowa: OTG, USB host, telefon, adapter OTG, USB Android, pin ID, kabel OTG, połączenie USB z węzłem, hub USB
Źródło: https://en.wikipedia.org/wiki/USB_On-The-Go (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### Android: zawieszanie aplikacji, APK i sklepy
System zarządza procesami, by oszczędzać baterię: aplikację nieużywaną zawiesza, a przy braku pamięci zamyka najdłużej nieaktywne procesy. Raport z 2011 r. wskazywał, że aplikacje do zabijania zadań zwykle szkodzą bardziej, niż pomagają. Aplikacje mają format APK i można je instalować z pliku albo ze sklepów: Google Play (główny), Amazon Appstore, Samsung Galaxy Store, Huawei AppGallery i F-Droid (tylko wolne i otwarte oprogramowanie). Przy scoped storage aplikacja ma swobodny dostęp tylko do swojego katalogu i wybranych folderów, resztę przez Storage Access Framework. Źródło nie opisuje ograniczeń instalacji spoza sklepu ani szczegółów optymalizacji baterii.
Słowa: Android, APK, F-Droid, Google Play, optymalizacja baterii, zabijanie aplikacji, scoped storage, instalacja APK, w tle
Źródło: https://en.wikipedia.org/wiki/Android_(operating_system) (sprawdzono 2026-10)

### WSL: Linux w Windows
WSL pozwala używać środowiska Linux w Windows bez narzutu pełnej maszyny wirtualnej. WSL 2 uruchamia prawdziwe jądro Linuksa w lekkiej maszynie wirtualnej (Hyper-V) i obsługuje więcej programów niż WSL 1, który tłumaczył wywołania systemowe; wymaga Windows 11 lub Windows 10 w wersji 1903 (build 18362) albo nowszej na x64. W Windows 11 narzędzie jest preinstalowane, ale dystrybucję trzeba pobrać; dokładne polecenie instalacji jest w dokumentacji Microsoft (learn.microsoft.com/windows/wsl/install). Microsoft pozycjonuje WSL jako narzędzie dla programistów, nie na komputery desktopowe czy serwery produkcyjne; wydajność dysku bywa niska. Obsługi USB źródło nie opisuje, więc dostępność portów szeregowych w WSL sprawdź osobno.
Słowa: WSL, WSL2, Linux w Windows, Hyper-V, Ubuntu na Windows, instalacja WSL, Windows 11, programowanie, port COM w WSL
Źródło: https://en.wikipedia.org/wiki/Windows_Subsystem_for_Linux (sprawdzono 2026-10)

### Homebrew na macOS: czym jest
Homebrew to menedżer pakietów dla macOS i Linuksa (Max Howell, 2009), napisany głównie w Ruby, na licencji BSD. Instaluje oprogramowanie z formuł (skrypty Ruby), często gotowych "butelek"; zewnętrzne repozytoria to "taps". Domyślnie instaluje się w /usr/local na Intelu i /opt/homebrew na Apple silicon. Instalacja jest repozytorium Git, więc aktualizuje się z GitHuba. Od wersji 3.4.0 działa brew services także pod systemd w Linuksie. Źródło nie podaje pełnej listy poleceń; z wpisu o Mosquitto wiadomo, że brew install i brew services restart służą do instalacji i restartu usługi.
Słowa: Homebrew, brew, macOS, formuła, tap, brew services, brew install, menedżer pakietów, Apple silicon, /opt/homebrew
Źródło: https://en.wikipedia.org/wiki/Homebrew_(package_manager) (sprawdzono 2026-10; ostatnie zdanie: odesłanie do wpisu Mosquitto)

### Python dla hobbysty: wersje i skrypty
Python używa wcięć zamiast nawiasów do bloków; skrypty to zwykłe pliki .py, a większość implementacji ma interaktywną pętlę (REPL). Biblioteka standardowa jest obszerna, a pakiety zewnętrzne są na PyPI (ponad 614 000 w marcu 2025). Gałąź 3.x jest aktualna, Python 2.7 nie dostaje aktualizacji bezpieczeństwa. Najnowsza stabilna wersja to 3.14.7 (sierpień 2026), 3.15 miała wyjść w październiku 2026, a najstarsza wspierana gałąź to 3.10. Wydania są co roku, z dwuletnim pełnym wsparciem i trzyletnim dla bezpieczeństwa. Artykuł nie opisuje pip ani środowisk wirtualnych.
Słowa: Python, pip, PyPI, skrypt, wersja Pythona, REPL, 3.14, wcięcia, meshtastic python, środowisko wirtualne
Źródło: https://en.wikipedia.org/wiki/Python_(programming_language) (sprawdzono 2026-10)
