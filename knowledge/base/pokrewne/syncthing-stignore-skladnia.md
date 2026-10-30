### Jak działają komentarze w pliku .stignore w Syncthing?
Linia zaczynająca się od `//` jest ignorowana. W innym miejscu `//` jest traktowane dosłownie jako część nazwy pliku.

Słowa: syncthing, stignore, komentarz, //, ignorowanie plików
Źródło: https://docs.syncthing.net/users/ignoring.html (sprawdzono 2026-10)

### Co oznacza wiodący ukośnik we wzorcu .stignore?
Wzorzec zaczynający się od `/` pasuje tylko do korzenia synchronizowanego folderu. Na przykład `/foo` pasuje do `foo`, ale nie do `podfolder/foo`.

Słowa: syncthing, stignore, wzorzec, korzeń folderu, ukośnik
Źródło: https://docs.syncthing.net/users/ignoring.html (sprawdzono 2026-10)

### Jak działa ** i wykluczenie ! w .stignore?
Wzorzec `**` przechodzi przez separatory katalogów, a pojedyncza gwiazdka nie. Prefiks `!` oznacza plik jako włączony, nadpisując bardziej ogólne reguły poniżej. Decyduje pierwszy pasujący wzorzec.

Słowa: syncthing, stignore, **, gwiazdka, negacja, wykluczenie, pierwszy pasujący wzorzec
Źródło: https://docs.syncthing.net/users/ignoring.html (sprawdzono 2026-10)

### Co robi #include w pliku .stignore Syncthing?
Dyrektywa `#include` wczytuje wzorce z innego pliku, który musi istnieć i może być włączony tylko raz. Ścieżki pozostają względne wobec korzenia folderu, ale `#include` wewnątrz dołączonego pliku używa ścieżek względem katalogu tego pliku.

Słowa: syncthing, stignore, #include, ścieżki względne, plik wzorców
Źródło: https://docs.syncthing.net/users/ignoring.html (sprawdzono 2026-10)

### Czy ignorowane pliki w Syncthing są usuwane?
Ignorowane pliki nie są synchronizowane z innymi urządzeniami. Mogą jednak blokować usunięcie katalogu, który inaczej byłby pusty. Prefiks `(?d)` pozwala usunąć pasujące ignorowane pliki, gdy blokują katalog nadrzędny. Strona zaleca go tylko dla plików, których utrata nie boli, np. wygenerowanych przez system.

Słowa: syncthing, stignore, (?d), usuwanie, ignorowane pliki, katalog
Źródło: https://docs.syncthing.net/users/ignoring.html (sprawdzono 2026-10)
