# 5 | Wersja v5 (2.8.5)
## new | Nowe funkcje
- **Tryb SOS.** Ekran „SOS” z dużym przyciskiem: alarm z Twoją pozycją na wybrane kanały (można kilka) i jako wiadomość bezpośrednia do wybranych węzłów, także niemonitorowanych. Wysyła się po przytrzymaniu przycisku, z powtarzaniem i potwierdzeniem. Wiadomość ma dzwonek alarmowy, więc u odbiorców przychodzi jako alarm.
- **SOS pod ręką.** Czerwony przycisk „SOS” z żółtą obwódką obok plakietki Twojego węzła na górnym pasku każdej karty (przytrzymanie co najmniej 2 s wysyła alarm; wyłączysz go albo wybierzesz karty w ekranie SOS), kafelek w szybkich ustawieniach Androida, skrót „SOS MT_SW” (Siri, Skróty), pozycja w menu ikony przy zegarze na komputerze oraz widżet SOS na Androidzie i iPhonie/iPadzie (też ekran blokady).
- **Alarm po odebraniu SOS.** SOS z innej kopii aplikacji (ma ukryty znacznik, więc zwykły tekst ze słowem SOS nic nie uruchamia) otwiera rozmowę, zakrywa cały ekran wielkim napisem SOS i dzwoni w kółko, dopóki go nie potwierdzisz. Na Androidzie i iPhonie/iPadzie także przy zablokowanym telefonie (syrena w pętli, wibracja, latarka). W oknie alarmu są gotowe odpowiedzi do nadawcy, „Pokaż na mapie” i „Otwórz rozmowę”, a potwierdzenie daje nadawcy znać, że SOS został zauważony. Przycisk „Sprawdź alarm” w ekranie SOS próbuje to na tym urządzeniu.
- **SOS: wybrane węzły, ciche godziny, historia.** Pełny alarm można ograniczyć do wybranych węzłów i ustawić ciche godziny. Odebrane SOS zapisują się w historii z przyciskami „Otwórz rozmowę” i „Pokaż na mapie”. Przycisk „Wyślij SOS ponownie” powtarza alarm, gdy nikt nie potwierdził. Ciche godziny mają własną listę wyjątków, dla każdego węzła z listy wybierzesz sygnał (syrena, dzwonek, cichy), historię zapiszesz do pliku, a powtarzanie można ustawić „Bez limitu”. Opcjonalnie alarm włącza się dopiero po drugim SOS od tego samego węzła. U nadawcy przycisk „Kto potwierdził SOS” pokazuje listę potwierdzeń. Dla kanału lub węzła można ustawić osobną treść SOS, a historia czyści się sama po wybranej liczbie dni. Gdy ktoś odpowie na Twój SOS, powtarzanie się zatrzymuje, dostajesz powiadomienie, a listę potwierdzeń zapiszesz do pliku. „Wzywam pomoc” ma dźwięk dzwonka, powiadomienie o SOS ma przyciski odpowiedzi, a na mapie węzły, które odpowiedziały, mają kółko.
- **Sąsiedzi na mapie.** W oknie sąsiadów węzła nowy przycisk „Pokaż na mapie”: sam węzeł i jego sąsiedzi z liniami i kolorem sygnału (SNR); gdy część nie ma pozycji, jest o tym informacja.
- **Zasięgi sieci na mapie.** Warstwa rysuje przybliżony zasięg każdego węzła z pozycją (z ukształtowaniem terenu), a nowa warstwa „Propagacja (prognoza pogody)” pokazuje, gdzie pogoda wydłuża zasięg; można dodać kilka obszarów.
- **Strażnik węzła i dostępność.** Powiadomienie, gdy wybrany węzeł zamilknie (z godzinami ciszy i wspólnym powiadomieniem dla wielu węzłów), oraz wykres historii dostępności węzła.
- **Archiwum wiadomości.** Automatyczna kopia wiadomości w wybranym folderze, przywracanie z plików CSV, tekstowych i Markdown, szyfrowanie hasłem.
- **Zasięg z pomiarów.** Trasa węzła kolorowana siłą sygnału, porównanie dwóch tras i zapis zasięgu całej sieci do KML.
- **Panorama terenu.** Budynki jako bryły, lasy, mgła, światło według prawdziwego słońca, ustawienie jakości panoramy.
- **Planer łącza.** Porównanie wyników, kierunek i pochylenie anteny, rodzaje anten, budynki i odbicia od ziemi na mapie.
- **Widżet statystyk.** Nowy widżet na Androidzie, a na iPhonie/iPadzie także małe widżety na ekran blokady; plakietka naszego węzła, wybór, które węzły pokazywać.
- **iPhone i iPad jak Android.** Skaner kodów QR, motyw wydarzenia, import warstw KML/KMZ/GeoJSON, widok terenu z aparatem, mapy offline, praca w tle, pełny asystent AI, sprawdzanie nowych wersji, lokalny serwer TAK, skrót Siri „Stan sieci MT_SW” i ustawienia powiadomień jak na Androidzie.
- **Komputer.** Opcja „Uruchamiaj z komputerem” z możliwością startu w zasobniku (okno się nie otwiera) i domyślnie jedna kopia programu.
- **Wygląd.** Motywy „Nasycony” i „Bardzo nasycony”, kategorie w wyborze motywu, opcja płaskiej ikony aplikacji.
- **Dane.** „Dołącz dane z kopii” oraz synchronizacja statystyk urządzeń.
- **MT_SW_FIRMWARE v3.** Zapytanie o statystyki sieci LAN w Diagnostyce na żądanie i odczyt siły sygnału Bluetooth telefonu.
## better | Ulepszenia
- Plakietka Twojego węzła jest teraz na górnym pasku wszystkich kart, także w Ustawieniach, Asystencie, Tłumaczu i Zdrowiu sieci.
- Mapa: filtr „Maks. liczba skoków” i „Maks. odległość od mojego węzła”, lista „wybierz węzeł” dla węzłów w tym samym miejscu, dokładniejsze dotykanie dymków i płynniejsze przesuwanie.
- Dolne zakładki otwierają stronę tam, gdzie ją zostawiłeś, a kliknięcie powiadomienia otwiera rozmowę, z której przyszło.
- Wykresy pomijają odczyty z błędnym zegarem węzła, baza zapisuje szybciej, a logi na iPhonie/iPadzie są dużo szybsze.
- Widżet: temperatura i wilgotność w jednej linii, rzadsze przebudowy danych.
- Panorama: gładsze grzbiety, nowe tekstury gór i lasów, szybsze rysowanie budynków; budynki pobierają się od małych kawałków i każdy zapisuje się od razu.
- Aktualizacja oprogramowania przez Bluetooth na iPhonie/iPadzie szuka urządzenia w trybie aktualizacji bez adresu MAC.
## fix | Naprawione błędy
- „Uruchamiaj z komputerem” działa na Windowsie i w AppImage na Linuksie.
- iPhone/iPad: przesunięte klikanie w dymki na mapie, brak dźwięku powiadomień przy zablokowanym telefonie, błąd kopii zapasowej, gaśnięcie ekranu podczas aktualizacji.
- Zdalna administracja pyta o wersję MT_SW_FIRMWARE także węzeł zdalny.
- Zapis GPIO czeka na potwierdzenie z węzła i pokazuje wynik.
- Bluetooth nie zapisuje już w logach osobnej linii dla każdego pakietu.
- Zasięgi z trasy rozpoznają pozycje usłyszane bezpośrednio także wtedy, gdy radio nie podaje rodzaju transportu.
- Okno sąsiadów miało dwa przyciski robiące to samo; teraz jest jeden „Zamknij”.
- Pobieranie budynków trwa dalej po wyjściu z ekranu, zamiast przerywać się po cichu.
## orig | Zmiany względem oryginału
- Scalone zmiany oryginału (wersja 2.8.5 i późniejsze): m.in. kreator konfiguracji radia, kody połączenia QR/NFC, aktualizacja bootloadera nRF52 przez Bluetooth i nowsze biblioteki.
- Usunięte widżety „Statystyki lokalne” i „Śledzenie węzła”: został jeden widżet MT_SW.
- iPhone/iPad: najstarszy obsługiwany system to iOS 17, a identyfikator aplikacji to pl.sw.mt, jak na Androidzie.
- Budowanie dostosowane do nowego Gradle.
# 0 | Od początku do wersji v4 (2.8.4)
## new | Nowe funkcje
- **Zdrowie sieci.** Zakładki zasilania, sygnału, eteru i środowiska, karta „Kondycja sieci”, podsumowanie z rankingami i eksport do PDF, HTML, Markdown, tekstu i CSV, wykresy w szczegółach węzła.
- **Asystent AI, tłumacz i dyktowanie offline.** Jeden silnik (llama.cpp) i jeden wybór modeli (Gemma, Qwen, Bielik, DeepSeek), z użyciem NPU i karty graficznej; asystent liczy sam, zna aplikację i sieć, ma pamięć między rozmowami, pokazuje przyciski do ekranów i ustawień; opcjonalny internet; czytanie na głos. Treści AI są oznaczone.
- **Planer łącza (Mesh Link Planer) i kalkulator anten.** Natywny planer z terenem, przeszkodami, pogodą na żywo i zasięgiem na mapie, kreator kabli i złączy, wymiary 13 typów anten.
- **Widok sieci (panorama terenu).** Sylwetka terenu dookoła z węzłami, obracana kompasem, teren offline, widok AR z aparatu.
- **Diagnostyka na żądanie i sniffer.** Zapytania do węzłów (port 354) oraz sniffer radiowy i MQTT na jednym ekranie z zapisem i grupowaniem duplikatów.
- **Funkcje MT_SW i dokumentacja.** Osobny ekran z funkcjami forka, baza wiedzy, cała dokumentacja w aplikacji po polsku i angielsku, ekran powitalny „Co dodaje MT_SW”.
- **Powiadomienia adaptacyjne** (Android 16+) i powiadomienia o nowych wersjach aplikacji.
- **Trasy i sąsiedzi.** Traceroute na mapie z siłą sygnału, linie jakości sygnału między węzłami, ikona sąsiadów na liście węzłów.
- **Węzły.** Nazwa węzła pośredniczącego (relay), ręczne dodawanie kontaktu, czyszczenie bazy węzłów (także automatyczne i przez Bluetooth), kolor węzła przy generowaniu klucza, dodatkowa telemetria MT_SW_FIRMWARE v3 i siła Wi-Fi.
- **Czat.** Długie wiadomości dzielone i składane, zdjęcia przez link, podgląd obrazków z linków, tłumaczenie przed wysłaniem, czytanie na głos.
- **Motywy i wygląd.** Czarno-białe motywy, „Skalowanie aplikacji”, wybór dowolnego koloru węzła, kolory statusów zgodne z aplikacją.
- **Wersje na komputer i iPhone/iPad.** Aplikacja na macOS, Windows i Linuksa z ekranem powitalnym oraz podstawa aplikacji na iPhone/iPad.
- **Wsparcie.** „Wesprzyj projekt”, zgłaszanie błędów i pomysłów, zbieranie logów.
## better | Ulepszenia
- Płynniejsza aplikacja: lżejsze listy węzłów i rozmów, mapa, szczegóły węzła i Zdrowie sieci przy dużej ilości danych, szybszy start na komputerze.
- Nazwy presetów modemu po polsku z angielską nazwą w nawiasie, polskie nazwy ról urządzeń, pełne tłumaczenie na polski.
- Jednolite kolory w całej aplikacji: złoty = dobrze, czerwony = średnio, fioletowy = źle.
- Szum jest oceniany po korekcie LNA, ocena SNR zależy od presetu.
- Ustawienia LoRa: przesunięcie częstotliwości, coding rate, domyślnie preset Narrow Fast i 869,44165 MHz.
- Mapy offline także dla domyślnej mapy OpenStreetMap, domyślna mapa OpenStreetMap i mniejsze plakietki z ogonkiem.
- Opisy „i” przy funkcjach, spokojniejsze okna informacyjne, pasek przewijania na komputerze.
## fix | Naprawione błędy
- Ponowne łączenie Bluetooth z już sparowanym urządzeniem (Xiaomi/MIUI).
- Puste listy rozwijane w spakowanej wersji na komputer.
- Odzyskiwanie urządzenia zablokowanego w trybie DFU oraz aktualizacja własnym plikiem dla nieznanych urządzeń.
- Zamykanie się aplikacji przy użyciu tłumacza i przy ogromnej ilości danych w Zdrowiu sieci.
- Czyszczenie węzłów przez Bluetooth działa do końca; naprawione wysyłanie zdjęć przez link i gubienie zapytań diagnostyki.
- Komputer: podwójne ekrany w nawigacji i nieprawidłowe kolumny po zapisanej sesji.
- Poprawne zapisy liczby skoków w historii tras i „Czas trasy” zamiast angielskiego tekstu.
## orig | Zmiany względem oryginału
- Przemianowanie na MT_SW_APP z własną ikoną i własnym identyfikatorem, dzięki czemu aplikacja instaluje się obok oryginału.
- Natywny planer łącza zamiast zewnętrznego Site Plannera.
- Usunięty model tłumacza NLLB-200 (licencja niezgodna z naszą).
- Ukryte przestarzałe role urządzeń (REPEATER, ROUTER_CLIENT); „Heap” nazywa się teraz RAM.
- Kolejne scalenia z oryginałem (protobufy, biblioteki, tłumaczenia, nowe funkcje), z pierwszeństwem naszych polskich tłumaczeń.
