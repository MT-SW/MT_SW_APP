# Filamenty do druku 3D, kod IP, koncentryk, GPS i GNSS

### Filamenty PLA, PETG, ABS, ASA, TPU: który na zewnątrz?
Według tabeli: PLA drukuje się w 180 do 230 °C, jest łatwy w druku, stół zwykle 60 do 80 °C; PETG 220 do 235 °C, bardziej elastyczny i trwały niż PLA i ABS; ABS 210 do 250 °C, odporny na uderzenia, stół 50 do 100 °C i zalecana zamknięta komora około 40 do 60 °C; ASA 240 do 260 °C, stół 100 do 120 °C, sztywny i trwały, jedyny z tej piątki opisany jako odporny na warunki atmosferyczne i polecany na zewnątrz; TPU 225 do 235 °C, bardzo elastyczny, drukuj wolno. Gęstości: PLA 1240, PETG 1270, ABS 1010, ASA 1070 kg/m³. ABS ma wyższą temperaturę ugięcia cieplnego niż PLA. Odporności na UV dla PLA, PETG, ABS i TPU oraz odporności cieplnej PLA, PETG i TPU źródło nie podaje. Dla obudowy węzła na słońcu wybór ASA ma więc oparcie w źródle, a PETG wymaga własnej oceny.
Słowa: filament, PLA, PETG, ABS, ASA, TPU, druk 3D, obudowa zewnętrzna, UV, temperatura druku, stół
Źródło: https://en.wikipedia.org/wiki/3D_printing_filament (sprawdzono 2026-10)

### Kod IP: co znaczą cyfry
Pierwsza cyfra (0 do 6 lub X) to ochrona przed ciałami stałymi i pyłem, 6 oznacza pyłoszczelność. Druga cyfra (0 do 9 lub X) to ochrona przed wodą. X oznacza brak danych. Litery dodatkowe: H (wysokie napięcie), M i S (ruch lub bezruch podczas próby wodnej), W (warunki pogodowe). IP65: pyłoszczelny i odporny na strumień wody z dowolnego kierunku (wniosek z definicji cyfr). IP67: pyłoszczelny, przykład to zanurzenie w słodkiej wodzie na 1 m do 30 minut. IP68: zanurzenie na głębokość podaną przez producenta, zwykle do 3 m. Stopnie wodoodporności nie sumują się po IPX6: IPX7 nie gwarantuje IPX5 ani IPX6. Test robi się w słodkiej wodzie, słona woda, oleje i rozpuszczalniki wymagają osobnych prób. Zapis „IPX-8” jest błędny. IP nie odpowiada normom NEMA.
Słowa: IP65, IP67, IP68, stopień ochrony, szczelność obudowy, pyłoszczelny, wodoodporny, IPX7, obudowa zewnętrzna
Źródło: https://en.wikipedia.org/wiki/Ingress_Protection_code (sprawdzono 2026-10)

### Kabel koncentryczny: 50 czy 75 omów, straty, wilgoć
Najczęściej używane impedancje to 50 (52) i 75 Ω; 50 Ω to kompromis między mocą a tłumiennością, a 75 Ω typowo telewizja i anteny krótkofalowe małej mocy. Najmniejsze straty przy typowych dielektrykach dają 52 do 64 Ω. Tłumienność maksymalna przy 750 MHz na 100 stóp: RG-58 13,10 dB, RG-213 5,98 dB, LMR-400 3,5 dB (zawsze 50 Ω). Współczynnik skrócenia: RG-58 i RG-213 0,66, LMR-400 0,85; dielektryk piankowy 0,78 do 0,88. Pianka może chłonąć wilgoć, co mocno zwiększa straty, a podpory gwiazdowe są bardzo podatne na wnikanie wody. Kable „flooded” mają żel blokujący wodę. O złączach N i SMA źródło nic nie mówi. Przy 868 MHz krótki, grubszy kabel (jak LMR-400) mocno ogranicza stratę; to wniosek z podanych liczb (częstotliwość inna niż 750 MHz).
Słowa: koncentryk, RG-58, RG-213, LMR-400, 50 omów, impedancja, tłumienność, współczynnik skrócenia, wilgoć w kablu, pigtail
Źródło: https://en.wikipedia.org/wiki/Coaxial_cable (sprawdzono 2026-10)

### GPS: ile satelitów, jaka dokładność, jak szybko fix
Konstelacja nominalna to 24 satelity, a według źródła na 25 kwietnia 2026 działało 31. Do pozycji potrzeba co najmniej czterech widocznych; z dowolnego miejsca widać co najmniej sześć, a przy czystym horyzoncie zwykle dziewięć. Dokładność: lepsza niż 3,5 m dla dobrych odbiorników, około 5 m po wyłączeniu Selective Availability (maj 2000), około 30 cm z L5, do 2 cm w odbiornikach geodezyjnych, w smartfonie 4,9 m lub lepiej ze wspomaganiem Wi-Fi. Pierwsza pozycja zajmuje do 30 s (odczyt efemeryd), a pobranie pełnego almanachu 12,5 minuty. Na dokładność wpływają jakość odbiornika i anteny, układ satelitów (skupione w jednym miejscu pogarszają), opóźnienia atmosferyczne, zasłonięcie widoku i zagłuszanie. Terminów cold/warm/hot start ani systemów GLONASS, Galileo i BeiDou źródło nie opisuje.
Słowa: GPS, GNSS, satelity, dokładność GPS, fix, almanach, efemerydy, Selective Availability, L5, zimny start
Źródło: https://en.wikipedia.org/wiki/Global_Positioning_System (sprawdzono 2026-10)
