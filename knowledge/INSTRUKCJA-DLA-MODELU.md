# Instrukcja: rozbudowa bazy wiedzy asystenta MT_SW

Ten plik jest dla modelu, który ma dalej budować bazę wiedzy. Czytaj go w całości przed pracą. Plik leży poza `base/`, więc asystent w aplikacji go nie wczytuje.

## Cel
Użytkownik (Cezary) chce, żeby w bazie było **naprawdę dużo informacji i danych**, tak by asystent offline odpowiadał sam i **nie musiał sięgać do internetu bez potrzeby**. Kompletność jest ważniejsza niż lekkość (rozmiar bazy nie jest problemem, nawet do ok. 100 MB tekstu). Pisz dużo, rzetelnie, w prostym polskim.

## Gdzie co leży (katalog `knowledge/`)
- `base/` – temat główny: nasza sieć MT_SW, Meshtastic, urządzenia, anteny, LoRa, zasilanie, solary, firmware.
- `base/pokrewne/` – tematy bliskie: informatyka, elektronika, Wi-Fi, Bluetooth, krótkofalarstwo, sieć elektryczna, zakupy z zagranicy, alternatywne sieci (MeshCore, Reticulum, APRS, LoRaWAN) – tylko informacyjnie, bez promowania.
- `base/offtop/` – tematy dalsze: zdrowie, pierwsza pomoc, turystyka, życie codzienne, dom, samochód, druk 3D, praca na wysokości, wiedza ogólna.
- `README.md` – opis (nie wczytywany). Pliki `firmware.md`, `integration*.md` odświeża skrypt `scripts/update-knowledge.sh` z internetu – **nie edytuj ich ręcznie**.
- Tylko pliki z `base/` trafiają do aplikacji (wraz ze spisem `index.txt`, tworzonym przy budowaniu).

Zanim zaczniesz nowy temat, sprawdź, co już jest:
`ls knowledge/base knowledge/base/pokrewne knowledge/base/offtop` oraz `grep -rh "^### " knowledge/base | sort`. Nie dubluj wpisów; jeśli temat już jest, rozbuduj go albo dodaj nowe, węższe pytania.

## Format wpisu
```
### Pytanie napisane tak, jak zapyta człowiek?
Treść własnymi słowami, do ok. 1750 znaków. Konkrety: liczby, jednostki, nazwy, kroki.
Słowa: synonimy, potoczne nazwy, skróty, angielskie odpowiedniki, literówki typowe dla tematu
Źródło: https://adres (sprawdzono RRRR-MM)
```
Zasady:
- Jeden wpis = jedno pytanie. Lepiej 5 krótkich wpisów niż 1 długi.
- Linia `Słowa:` jest ukryta i służy tylko wyszukiwaniu – wpisz tam wszystko, czym ludzie nazywają dany temat.
- Linia `Źródło:` jest obowiązkowa. Data to miesiąc sprawdzenia (dziś: 2026-10 lub późniejszy).
- Treść **własnymi słowami**, nigdy kopia artykułu. Artykuły osób trzecich przepisuj z zachowaniem sensu, innymi słowami, i dopasuj do MT_SW/Meshtastic.
- Czego źródło nie podaje – napisz wprost („źródło nie podaje…”). Wnioski z ogólnej wiedzy oznacz („z ogólnej wiedzy, nie ze źródła”).
- Wpisy, co do których masz wątpliwości, oznacz `do przeglądu` (sprawdzi je Rafał).
- **Nie wymyślaj faktów.** Korzystaj tylko z plików w repozytorium oraz ze stron, które naprawdę otworzyłeś (WebFetch/WebSearch). Bez sprawdzonego źródła nie dodawaj liczb, cen ani parametrów.
- Nowe pliki nazywaj małymi literami z myślnikami, na początku pliku daj nagłówek `# Tytuł` i komentarz HTML jak w `base/anteny.md`.

## Zasady dotyczące projektu MT_SW (obowiązkowe)
- Zapytany o „nasz projekt” – odpowiadaj o MT_SW, nie o główny Meshtastic (ten tylko krótko, chyba że ktoś pyta wprost).
- MT_SW **nie omija** głównej sieci Meshtastic: ma część własnych rozwiązań, ale pozostaje z nią zgodny.
- MT_SW **nie ma własnego presetu** radiowego – używa oficjalnych. Dla Świętokrzyskiego: region EU_N_868 (patrz istniejące wpisy). Gdy pytają o zalecany preset, nie odpowiadaj „nasz”: zależy od terenu, liczby urządzeń i warunków (tabela presetów jest w bazie).
- Hop limit: od Meshtastic 2.8.x zalecane **7** (limit jest dynamiczny). Stare „3 wystarczy” jest nieaktualne.
- Błędy w naszej wersji zgłasza się **tylko własnymi kanałami** (lokalne grupy, Discord/WhatsApp/Signal, sama sieć mesh) – nie kieruj na GitHub. Błędy oryginalnego Meshtastic – na GitHub oryginału.
- Firmware nazywaj **MT_SW_FIRMWARE vX**. Nigdy „FW+”.
- Nie wspominaj o odblokowywaniu ukrytych funkcji. O szerokich kanałach i uwagach regionalnych pisz ogólnie, bez szczegółów.
- Alternatywne sieci (MeshCore, Reticulum itd.) – tylko informacja, w `pokrewne/`.

## Co warto dopisać (pomysły, w kolejności ważności)
1. Wszystko o sieci MT_SW: konfiguracja, flashowanie dla każdego układu (ESP32, nRF52, RP2040), kompilacja, userPrefs, MQTT, Home Assistant, aplikacja.
2. Radio i anteny: dobór, kable, złącza, straty, filtry, LNA, propagacja, przeszkody, budżet łącza, pomiary.
3. Zasilanie: baterie, ogniwa, solary, MPPT, powerbanki, pobór prądu, zimno/upał.
4. Sprzęt: porównania urządzeń, obudowy, druk 3D, montaż na maszcie, piorunochrony, praca na wysokości (bezpieczeństwo!).
5. Informatyka i elektronika: sieci, Wi-Fi, Bluetooth, Linux, Windows, sterowniki USB, lutowanie, multimetr, Arduino/ESP.
6. Zakupy: gdzie kupować części, cła i VAT przy małych zamówieniach z zagranicy, podróbki.
7. Prawo i pasma w Polsce: 868 MHz, moce, PMR, licencje – tylko ze źródeł urzędowych.
8. Życie codzienne (offtop): pierwsza pomoc, turystyka, dom, auto, fotografia, przyroda, zdrowie – rzetelnie, z ostrożnością i źródłem. W zdrowiu zawsze dopisz, że to informacja ogólna, a nie porada lekarska.

## Sposób pracy
- Jedna rzecz po kolei; nie mieszaj z kodem aplikacji. **Nie ruszaj kodu**.
- Pracuj partiami (np. jeden plik tematyczny = jeden commit). Po każdej partii sprawdź: czy każdy wpis ma `Słowa:` i `Źródło:`, czy nie przekracza ~1750 znaków, czy nie ma kopii obcego tekstu.
- Commit: jedna linia, po polsku, bez dodatkowych linii i stopek, np. `Baza wiedzy: zasilanie solarne w terenie`. Przy tej bazie zmiana w `CHANGELOG.md` jest potrzebna tylko przy większej partii.
- Git w tym katalogu zostawia `index.lock`. Gdy git krzyczy o locku: `mv .git/index.lock .git/index.lock.old` i ponów. Zamiast `git revert` użyj `git diff X^ X | git apply -R`, potem `git add` i `git commit`.
- Nie rób `git push` – Cezary zbuduje i wypchnie sam.
- Do Cezarego pisz **po polsku, prostym językiem**, krótko. Na koniec partii podaj: ile wpisów dodano, w których plikach i co proponujesz dalej. Jeśli podajesz mu polecenia, daj je w jednym bloku do skopiowania jednym kliknięciem.
