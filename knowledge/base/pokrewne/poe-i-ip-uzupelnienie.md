# PoE i stopnie IP

### Czym jest PoE i jakie są standardy mocy?
PoE zasila urządzenie tym samym kablem sieciowym co dane. Standardy: 802.3af (Typ 1) do 15,4 W ze źródła i ok. 12,95 W u odbiornika; 802.3at (PoE+) 30 W / 25,5 W; 802.3bt Typ 3 do 60 W (51 W u odbiornika) i Typ 4 do 90 W (71,3 W). Zasilanie idzie parami 2 i 3 (tryb A), parami 1 i 4 (tryb B) albo wszystkimi czterema. W PoE aktywnym źródło wykrywa urządzenie po sygnaturze rezystancji i negocjuje moc. PoE pasywne podaje napięcie bez negocjacji (typowo 24 V lub 48 V, także 12, 18, 54 V), więc napięcie źródła i urządzenia musi się zgadzać; spotyka się je w sprzęcie Ubiquiti i MikroTik. Do większych mocy potrzebny jest kabel kategorii 5 lub lepszy. Maksymalnej długości źródło nie podaje.
Słowa: PoE, Power over Ethernet, 802.3af, 802.3at, 802.3bt, PoE pasywne, PoE aktywne, zasilanie przez Ethernet, injector, węzeł zewnętrzny
Źródło: https://en.wikipedia.org/wiki/Power_over_Ethernet (sprawdzono 2026-10)

### Co znaczą stopnie ochrony IP65, IP67, IP68?
Pierwsza cyfra to ochrona przed pyłem: 6 oznacza pyłoszczelność. Druga to woda: 5 odporność na strumień wody z dyszy, 7 zanurzenie (przykład: do 1 m przez 30 minut), 8 zanurzenie głębsze wg deklaracji producenta, zwykle do 3 m. Uwagi: oceny nie sumują się powyżej IPX6, więc IP67 nie gwarantuje odporności na strumień wody (urządzenie spełniające oba zapisuje się IPX5/IPX7). Testy dotyczą tylko słodkiej wody, nie słonej, olejów czy rozpuszczalników. Cyfra 5 przy pyle nie oznacza pełnej szczelności. IP nie jest równoważne klasom NEMA. Przy obudowach na zewnątrz liczy się też szczelność dławic i zamknięcia, a to zależy od wykonania.
Słowa: IP65, IP67, IP68, stopień ochrony, szczelność, pyłoszczelny, wodoodporny, obudowa zewnętrzna, deszcz, dławik
Źródło: https://en.wikipedia.org/wiki/Ingress_protection_rating (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

