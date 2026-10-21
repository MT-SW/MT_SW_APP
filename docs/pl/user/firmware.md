---
title: Aktualizacje firmware
parent: Przewodnik użytkownika
nav_order: 13
last_updated: 2026-10-21
description: Aktualizuj firmware radia przez Bluetooth, Wi-Fi lub USB — proces OTA, kanały wersji, kontrole przed aktualizacją i odzyskiwanie.
aliases:
  - firmware
  - update
  - ota
  - flash
---

# Aktualizacje firmware

Utrzymuj firmware swojego radia w aktualnej wersji, aby mieć nowe funkcje, poprawki błędów i ulepszenia bezpieczeństwa.

## Sprawdzanie aktualizacji

1. Otwórz **Ustawienia** połączonego radia i w sekcji **Zaawansowane** dotknij **Aktualizacja oprogramowania**. Pozycja pojawia się tylko dla radiów obsługujących OTA.
2. Aplikacja sprawdza dostępne wersje firmware.
3. Dostępne aktualizacje pokazują numer wersji i informacje o wydaniu.

Aplikacja mówi też, gdy połączone radio ma starsze firmware niż najnowsze wydanie stabilne. Zakładka **Połącz** pokazuje wtedy kartę **Dostępna aktualizacja firmware** z zainstalowaną i stabilną wersją, a powiadomienie mówi to samo raz dla każdego radia i wydania. Jej przycisk to **Otwórz Aktualizacje firmware**, gdy aplikacja może zaktualizować radio przez obecne połączenie, albo **Otwórz MT_SW_APP Flasher**, gdy nie może.

## Metody aktualizacji

Ekran aktualizacji sam wybiera metodę na podstawie tego, jak radio jest połączone, a przycisk aktualizacji podaje jej nazwę: **Aktualizuj przez BLE OTA**, **Aktualizuj przez Wi-Fi OTA** lub **Aktualizuj przez transfer pliku USB**.

| Połączenie | nRF52 | ESP32 | RP2040 |
|---|---|---|---|
| Bluetooth | BLE OTA | BLE OTA | — |
| Sieć | — | Wi-Fi OTA | — |
| USB | Transfer pliku przez USB | — | Transfer pliku przez USB |

Tam, gdzie w tabeli nie ma metody, ekran mówi, że aktualizacja firmware nie jest obsługiwana przez to połączenie. Połącz się inaczej albo użyj [Web Flashera](https://flasher.meshtastic.org).

### OTA (Over-The-Air) przez Bluetooth

Najczęstsza metoda aktualizacji dla użytkowników Androida:

> ⚠️ **Ostrzeżenie:** Przerwanie aktualizacji firmware może sprawić, że radio nie uruchomi się. Trzymaj telefon w pobliżu, a oba urządzenia zasilane aż do zakończenia aktualizacji.

1. Upewnij się, że radio jest połączone przez Bluetooth.
2. Przejdź do ekranu Aktualizacja oprogramowania.
3. Wybierz żądaną wersję firmware.
4. Dotknij **Aktualizuj przez BLE OTA**. Okno **Ostrzeżenie o aktualizacji** wymienia kontrole przed aktualizacją — przeczytaj je, a następnie dotknij **Wiem, co robię.**, aby rozpocząć. To okno pojawia się dla każdej metody aktualizacji, w tym Wi-Fi OTA, USB i lokalnego pliku firmware.
5. Poczekaj na zakończenie aktualizacji. Nie rozłączaj w trakcie aktualizacji.

Gdy aktualizacja jest pobierana, wgrywana lub weryfikowana, możesz dotknąć **Zagraj w Chirpy Hop**, aby zagrać w krótką grę biegową: dotknij, aby skoczyć, przesuń w dół, aby się schylić. Postęp aktualizacji pozostaje widoczny nad grą, a przycisk wstecz wraca do ekranu aktualizacji. Gdy aktualizacja się zakończy lub nie powiedzie, gra się zatrzymuje i oferuje **Wróć do aktualizacji**. Twój najlepszy wynik jest zapisywany na tym urządzeniu.

![Firmware — sprawdzanie aktualizacji](../../assets/screenshots/firmware_checking.png)

Niektóre płyty mają bootloader, który nie przyjmie aktualizacji przez Bluetooth. Dla nich ekran pokazuje przed startem kartę z ostrzeżeniem, a dla niektórych płyt także podpowiedź. **Nie pokazuj ponownie dla tego urządzenia** ukrywa ją dla tego radia. Gdy zwykły bootloader nRF ogranicza szybkość wysyłania, ekran mówi o tym podczas wysyłania i jeszcze raz na ekranie sukcesu, z linkiem do bootloadera OTAFIX, dzięki któremu aktualizacje przez Bluetooth są dużo szybsze.

#### Wykasuj urządzenie podczas aktualizacji

Tam, gdzie aplikacja to oferuje, nad przyciskiem aktualizacji pojawia się przełącznik **Wykasuj urządzenie podczas aktualizacji**. Jest to zgoda dla pojedynczej aktualizacji i nigdy nie jest zapamiętywana.

| Metoda | Co robi kasowanie |
|---|---|
| BLE / Wi-Fi OTA | Przywraca ustawienia fabryczne urządzenia po zweryfikowaniu aktualizacji. Wszystkie ustawienia i parowanie Bluetooth zostają usunięte. |
| USB | Całkowicie kasuje pamięć flash urządzenia, a następnie instaluje wybrane firmware od nowa. |

Nie jest oferowane dla lokalnego pliku firmware, podczas aktualizacji odzyskiwania, ani przez USB w aplikacji desktopowej — kasowanie przez USB uruchamia opisaną niżej sekwencję serwisową tylko dla Androida. Przez USB jest oferowane tylko dla radiów nRF52 i RP2040, a gdy aplikacja nie może bezpiecznie wykasować któregoś z nich, przełącznik jest wyszarzony i podaje powód. Potem urządzenie wymaga ponownej konfiguracji — i sparowania.

### OTA przez Wi-Fi (ESP32 podłączony do sieci)

Gdy radio ESP32 jest połączone przez sieć, a nie przez Bluetooth, aplikacja oferuje **WiFi OTA**, które przesyła tę samą aktualizację przez TCP:

1. Połącz się z radiem przez sieć (zobacz [Połączenia](connections)).
2. Otwórz ekran Aktualizacja oprogramowania i wybierz wersję.
3. Dotknij **Aktualizuj przez Wi-Fi OTA**. Trzymaj radio i telefon w tej samej sieci przez cały czas transferu.

WiFi OTA instaluje obraz aplikacji ESP32, czyli plik `.bin`, a nie plik `.uf2` używany przy aktualizacji przez USB. Aplikacja sama wybiera właściwy plik z wydania.

#### Pierwszy raz na ESP32 z firmware MT_SW: pełny plik (`-full.bin`)

Aktualizacja przez OTA z tej aplikacji działa na ESP32 dopiero po jednorazowym wgraniu specjalnego pełnego pliku (`-full.bin`).

- Wgraj ten plik przez stronę internetową: oryginalny flasher Meshtastic albo inny. Wgranie go przez tę aplikację nie zadziała.
- **Uwaga:** ten plik kasuje wszystkie ustawienia urządzenia. Najpierw zrób kopię ustawień, potem wgraj plik, potem przywróć ustawienia.
- Po tym każda kolejna aktualizacja przez Wi-Fi działa z tej aplikacji.
- Jeśli wcześniej wgrywałeś oryginalne oprogramowanie przez stronę flashera i użyłeś wymazywania, to masz to już załatwione i możesz od razu aktualizować.
- Jeśli potrzebujesz pliku `-full.bin` dla swojego urządzenia, napisz do zespołu MT_SW i podaj, jaki masz sprzęt.

Ta sama informacja pojawia się na ekranie aktualizacji, gdy wybierzesz **Plik lokalny** dla ESP32.

### Aktualizacja USB w aplikacji

Gdy radio nRF52 lub RP2040 jest połączone przez **USB/port szeregowy** (a nie Bluetooth), ekran Aktualizacja oprogramowania oferuje **Transfer pliku przez USB**. Aplikacja restartuje urządzenie w tryb DFU, a następnie prosi o zapisanie pliku `.uf2` na dysku DFU urządzenia za pomocą systemowego selektora plików. Ta opcja pojawia się tylko przy połączeniu USB/szeregowym. Nie jest dostępna przez Bluetooth, a radia ESP32 nie mają w aplikacji aktualizacji przez USB.

Bootloader nRF od producenta dostarczany jako `.zip` (np. RAK WisBlock RAK4631) trzeba wgrać narzędziem szeregowego DFU, takim jak `adafruit-nrfutil` — skopiowanie tego `.zip` na dysk nie zadziała. Bootloader dostarczany jako `update-....uf2` **można** zainstalować, kopiując go na dysk; tak działa aktualizacja bootloadera w samej aplikacji. Gdy dotyczy to drogi tylko przez port szeregowy, aplikacja wyświetla podpowiedź.

### Kasowanie fabryczne i aktualizacja bootloadera

Przy połączeniu **USB/szeregowym** urządzenia nRF52 i RP2040 można wykasować w ramach aktualizacji — to zgoda **Wykasuj urządzenie podczas aktualizacji** opisana wcześniej na tej stronie. Urządzenia nRF52 dodatkowo oferują **Zaktualizuj bootloader**, gdy dla danej płyty opublikowano zaktualizowany bootloader; urządzenia RP2040 nie mają bootloadera Adafruit, więc nigdy go nie widzą.

Obie funkcje są tylko dla Androida. Zależą od kontroli dysku aktualizacji w Androidzie, więc aplikacja desktopowa ich nie pokazuje — tam użyj [Web Flashera](https://flasher.meshtastic.org).

Wybierz wersję firmware przed którąkolwiek z nich: aplikacja ukrywa obie, dopóki nie zostanie wybrane wydanie, ponieważ firmware jest instalowane ponownie po wykasowaniu urządzenia lub zapisaniu nowego bootloadera.

Zarówno kasowanie przez USB, jak i aktualizacja bootloadera zapisują po kolei dwa pliki, więc jesteś proszony o wskazanie dysku aktualizacji urządzenia dwa razy: raz dla obrazu kasowania lub bootloadera, a potem ponownie dla firmware.

Aplikacja odczytuje `INFO_UF2.TXT` z wybranego dysku, aby potwierdzić, że to naprawdę dysk aktualizacji urządzenia, i rozpoznać płytę przed zapisaniem czegokolwiek.

Przy aktualizacji bootloadera aplikacja odczytuje też zainstalowaną wersję bootloadera z `INFO_UF2.TXT` i pokazuje ją obok najnowszego wydania przed zapisaniem czegokolwiek. Działające firmware nie zgłasza swojego bootloadera, więc zainstalowana wersja pojawia się dopiero po restarcie urządzenia w tryb aktualizacji, nigdy na ekranie firmware podczas połączenia. Jeśli obie wersje są takie same, bootloader zostaje bez zmian, a aplikacja przechodzi od razu do ponownej instalacji firmware. W przeciwnym razie wybierz **Zaktualizuj bootloader**, aby go zapisać, lub **Pomiń**, aby ponownie zainstalować firmware bez zmiany bootloadera.

W nRF52 aplikacja musi już znać stos Bluetooth używany przez Twoje urządzenie, zanim zacznie, ponieważ nie może odczytać bootloadera, dopóki urządzenie się nie zrestartuje. Jeśli nie może potwierdzić stosu, odmawia kasowania i kieruje Cię do [Web Flashera](https://flasher.meshtastic.org). W Web Flasherze wybranie złego stosu Bluetooth może sprawić, że radio da się odzyskać tylko programatorem sprzętowym.

Gdy dysk jest już czytelny, sposób kasowania urządzenia nRF52 zależy od jego bootloadera:

- **Nowsze bootloadery kasują się same.** Bootloader, który w `INFO_UF2.TXT` zgłasza obsługę kasowania fabrycznego (OTAFIX 2.4 i nowsze), przyjmuje jeden, uniwersalny dla płyt plik kasujący. Bootloader sam wymazuje Twoje ustawienia, kanały, klucze i zapisaną listę węzłów, zostawia zainstalowane firmware na miejscu i po sekundzie lub dwóch wraca jako dysk aktualizacji, czekając na plik firmware. Na tej ścieżce nie ma wyboru stosu Bluetooth, w którym można się pomylić.
- **Starsze bootloadery wymagają kasowania właściwego dla stosu Bluetooth.** Na nich aplikacja zapisuje mały program kasujący dopasowany do stosu Bluetooth Twojego urządzenia, a następnie uruchamia go przez port szeregowy. Jeśli dysk zgłasza inny stos niż oczekiwany, aplikacja odmawia zamiast zgadywać.

Aplikacja wybiera ścieżkę na podstawie tego, co zgłasza dysk; nie wybierasz jej Ty.

### Inne opcje flashowania

Do odzyskiwania lub gdy nie jest dostępne ani OTA, ani USB w aplikacji:
- Użyj [Meshtastic Web Flasher](https://flasher.meshtastic.org)
- Albo [narzędzia CLI Meshtastic](https://meshtastic.org/docs/getting-started/flashing-firmware) na komputerze

## Kanały wersji

| Kanał | Opis |
|---------|-------------|
| Stabilna | Zalecana dla większości użytkowników; przetestowane wydania |
| Alpha | Wydania podglądowe; mogą zawierać błędy |
| Plik lokalny | Wgraj plik firmware, który wybierzesz sam, zamiast pobranego wydania. Przycisk nazywa się **Wybierz plik lokalny**, a aplikacja prosi o potwierdzenie nazwy pliku przed wgraniem |

## Lista kontrolna przed aktualizacją

Przed aktualizacją:
- [ ] Bateria powyżej 50% (zalecane). Sama aplikacja odmawia startu aktualizacji tylko wtedy, gdy radio zgłasza 10% lub mniej
- [ ] Stabilne połączenie metodą, którą aktualizujesz: Bluetooth, Wi-Fi lub USB
- [ ] Zanotuj swoje obecne ustawienia (mogą się zresetować przy zmianie wersji głównej)
- [ ] Sprawdź informacje o wydaniu pod kątem zmian łamiących zgodność
- [ ] Zaktualizuj samą aplikację przed aktualizacją firmware lub razem z nią, aby zapewnić zgodność

## Po aktualizacji

Po zapisaniu firmware aplikacja weryfikuje aktualizację i czeka, aż urządzenie wróci online:

![Weryfikowanie aktualizacji i oczekiwanie na ponowne połączenie urządzenia](../../assets/screenshots/firmware_verifying.png)

Gdy aktualizacja się powiedzie:
- Radio restartuje się automatycznie
- Połączenie Bluetooth zostaje nawiązane ponownie
- Sprawdź, czy Twoje ustawienia są nienaruszone
- Potwierdź nową wersję w **Aktualnie zainstalowano** na ekranie Aktualizacja oprogramowania — jest ona także pokazana na stronie szczegółów węzła i na ekranie Połączenia

![Aktualizacja firmware zakończona sukcesem](../../assets/screenshots/firmware_success.png)

## Rozwiązywanie problemów

### Aktualizacja się zawiesiła

Jeśli aktualizacja wygląda na zamarzniętą:
- Daj jej minutę. Po zapisaniu obrazu aplikacja czeka do **60 sekund**, aż radio wróci i zgłosi swoją nową wersję, więc pauza na etapie weryfikacji jest oczekiwana.
- Jeśli po tym czasie nadal się zawiesza, wyłącz i włącz zasilanie radia.
- Spróbuj aktualizacji ponownie.

Komunikat **Upłynął limit czasu weryfikacji. Urządzenie nie podłączyło się ponownie w czasie.** oznacza, że obraz został zapisany, ale radio nie wróciło w tym oknie czasowym — wyłącz i włącz jego zasilanie i sprawdź wersję w **Aktualnie zainstalowano**, zanim ponowisz aktualizację.

![Błąd aktualizacji firmware](../../assets/screenshots/firmware_error.png)

### Radio nie uruchamia się po aktualizacji

Aktualizacja przez Bluetooth, która urwie się w połowie, może zostawić radio czekające w trybie bootloadera. Gdy aplikacja jest rozłączona, zakładka **Połącz** pokazuje wtedy kartę **Dokończ aktualizację** z nazwą radia. Dotknij **Wznów aktualizację firmware** i trzymaj radio w pobliżu, aby aplikacja wgrała je ponownie, albo zamknij kartę, jeśli aktualizacji nie da się dokończyć z telefonu.

Jeśli aplikacja poinformowała, że aktualizacji przez Bluetooth nie udało się dokończyć, postępuj zgodnie z podaną instrukcją: podłącz radio do komputera przez USB i wgraj firmware ponownie narzędziem szeregowego DFU od producenta, takim jak `adafruit-nrfutil`. Standardowy bootloader nRF nie potrafi niezawodnie dokończyć przerwanej aktualizacji bezprzewodowej.

W przeciwnym razie, jeśli Twoje radio się nie uruchamia:
1. Spróbuj połączyć się z komputerem przez USB
2. Użyj web flashera w trybie odzyskiwania/DFU
3. Wgraj sprawdzoną wersję firmware
4. Sprawdź Discorda Meshtastic, aby znaleźć kroki odzyskiwania dla konkretnego urządzenia

### Ostrzeżenia o zgodności

Po połączeniu aplikacja porównuje firmware radia z dwoma progami i na każdy reaguje inaczej:

| Wersja firmware | Co widzisz | Co się dzieje |
|---|---|---|
| Poniżej **2.3.15** | **Wymagana aktualizacja firmware'u.** | Aplikacja rozłącza się z radiem. Nie działa z tak starym firmware. |
| **2.3.15** do, ale bez, **2.5.14** | **Wymagana aktualizacja firmware** | Radio zostaje połączone, ale okno blokuje resztę aplikacji, dopóki firmware nie zostanie zaktualizowane. Podaje zainstalowaną, minimalną i najnowszą stabilną wersję. **Aktualizuj firmware** otwiera ekran aktualizacji, a **Rozłącz** odłącza radio. |
| **2.5.14** lub nowsza | Nic | — |

Ciąg wersji, którego aplikacja nie potrafi sparsować, jest ignorowany, a nie traktowany jako zbyt stary, więc chwilowy błąd odczytu nigdy nie rozłączy działającego radia.

## Powiązane tematy

- [Połączenia](connections) — ponowne łączenie po aktualizacji firmware
- [Przewodnik flashowania firmware](https://meshtastic.org/docs/getting-started/flashing-firmware) — pełny opis flashowania firmware na meshtastic.org
- [Obsługiwane urządzenia](https://meshtastic.org/docs/hardware/devices) — sprawdź zgodność firmware według urządzenia
- [FAQ](https://meshtastic.org/docs/faq/) — częste pytania na meshtastic.org
