<!-- Baza wiedzy MT_SW: plik .stignore w Syncthing, czyli które pliki nie są synchronizowane. Tylko informacyjnie. -->
# Syncthing: plik .stignore

### Gdzie leży plik .stignore w Syncthing i jak działa kolejność wzorców?
Plik .stignore ma leżeć w katalogu głównym synchronizowanego folderu. Pliki w podfolderach nie mają na niego wpływu. Sam plik nigdy nie jest synchronizowany z innymi urządzeniami, ale może dołączać inne pliki poleceniem #include. Wzorce działają od góry do dołu, a pierwszy pasujący decyduje o losie pliku. Wzorzec z prefiksem ! oznacza, że pasujące pliki mają zostać uwzględnione. Działa tylko wtedy, gdy stoi przed szerszym wzorcem, który by je wykluczył. Linia zaczynająca się od // jest komentarzem. W innym miejscu // traktowane jest dosłownie, więc plik „file // notka” będzie szukany pod taką nazwą. Ścieżki w dołączanym pliku są liczone względem jego katalogu, a brak pliku albo podwójne dołączenie kończy się błędem.
Słowa: stignore, .stignore, ignorowanie plików, wykluczenia, syncthing ignore, wzorce, !, negacja, include, komentarz, katalog główny, kolejność, pierwszy pasujący, wykluczanie plików
Źródło: https://docs.syncthing.net/users/ignoring (sprawdzono 2026-10; oficjalna dokumentacja Syncthing)

### Jak zignorować śmieci systemowe w Syncthing i pozwolić je usuwać?
Prefiks (?d) przed wzorcem pozwala Syncthingowi usunąć pasujące ignorowane pliki, gdy blokują usunięcie katalogu nadrzędnego. Dokumentacja zaleca go dla plików generowanych przez system, np. .DS_Store na macOS, bo są bezpieczne do skasowania. Prefiks (?i) czyni wzorzec niewrażliwym na wielkość liter. Na macOS i Windows wzorce są zawsze niewrażliwe na wielkość liter. Przykład ze źródła: wzorzec foo pasuje do plików foo, subdir/foo i do katalogu o nazwie foo. Źródło nie podaje, jakie pliki systemowe warto wykluczyć na serwerze domowym, więc listę dobierz samodzielnie.
Słowa: stignore ds_store, (?d), (?i), usuwanie ignorowanych plików, pliki systemowe, macos, windows, wielkość liter, wzorzec foo, katalog nadrzędny, thumbs.db, pliki śmieci
Źródło: https://docs.syncthing.net/users/ignoring (sprawdzono 2026-10; oficjalna dokumentacja Syncthing)
