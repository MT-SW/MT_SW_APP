# Druk 3D: obudowy i uchwyty do węzłów

### Jak działa druk FDM i z czego się go robi?
W druku FDM (FFF) filament z szpuli wpycha się do podgrzewanego hotendu, gdzie się topi i wychodzi dyszą jako cienka nitka; głowica kładzie warstwę po warstwie, a oprogramowanie (slicer) zamienia model 3D na G-code. Popularne materiały: PLA, ABS, PETG, ASA, HIPS, TPU i nylon, a także poliwęglan i kompozyty z włóknem węglowym lub szklanym. Dysze mają zwykle 0,3–1,0 mm; mosiężne wystarczą do miękkich tworzyw, a do ściernych (np. z włóknem węglowym) potrzebna jest stal hartowana. Dyszę wymienia się na gorąco, żeby nie przeciekała.
Słowa: druk 3D, FDM, FFF, filament, slicer, G-code, dysza, PLA, ABS, PETG, ASA, TPU, nylon, hotend
Źródło: https://en.wikipedia.org/wiki/Fused_filament_fabrication (sprawdzono 2026-10)

### Dlaczego PLA nie nadaje się na obudowę stojącą na słońcu?
PLA ma temperaturę zeszklenia ok. 60–65 °C (przy wyższej mięknie), a temperaturę topnienia ok. 130–180 °C. W aucie lub na nasłonecznionym maszcie obudowa z PLA może się odkształcić. Jest kruchy (wydłużenie przy zerwaniu poniżej 10 %) i rozkłada się głównie przez hydrolizę, więc wilgoć mu szkodzi. Wygrzewanie (annealing) poprawia odporność cieplną, a wersje "heat-resistant" wytrzymują ok. 110 °C. Źródło nie podaje konkretnych danych o odporności PLA na UV na zewnątrz.
Słowa: PLA, temperatura zeszklenia, 60 stopni, odkształcenie na słońcu, kruchy, hydroliza, wygrzewanie, annealing, obudowa zewnętrzna
Źródło: https://en.wikipedia.org/wiki/Polylactic_acid (sprawdzono 2026-10)

### Który materiał na obudowę zewnętrzną: ASA, PETG, ABS?
ASA ma, według źródła, około dziesięciokrotnie lepszą odporność na pogodę i UV niż ABS, zachowuje połysk, kolor i właściwości na zewnątrz; temperatura zeszklenia ok. 100 °C (ABS 105 °C). Używa się go na elewacje, części zewnętrzne aut i meble ogrodowe oraz do prototypów drukowanych z myślą o zewnątrz. Jest lekko higroskopijny, więc może wymagać suszenia przed drukiem. PETG to glikolem modyfikowany PET, amorficzny, o nieco niższej temperaturze topnienia niż zwykły PET; PET ogólnie ma temperaturę zeszklenia 67–81 °C i chłonie wilgoć (nadmiar wilgoci przy przetwarzaniu daje kruchość). Dla obudowy węzła solarnego ASA jest więc najlepiej uzasadnionym wyborem ze względu na UV i temperaturę.
Słowa: ASA, PETG, ABS, UV, odporność na pogodę, obudowa zewnętrzna, solar, suszenie filamentu, higroskopijny, węzeł na dworze
Źródło: https://en.wikipedia.org/wiki/Acrylonitrile_styrene_acrylate i https://en.wikipedia.org/wiki/Polyethylene_terephthalate (sprawdzono 2026-10; wniosek o wyborze ASA to wnioskowanie z tych danych)

### Warping, przyczepność, wentylacja i obudowa drukarki
Głównym problemem jest odkształcanie (warping); powodują je niestabilna temperatura i przeciągi. Pomaga zamknięta komora; ABS wymaga stałej temperatury komory ok. 60–70 °C, PLA drukuje się w temperaturze pokojowej. Drukarka najlepiej pracuje w 18–25 °C, przy niskiej wilgotności i bez przeciągów. FFF wydziela lotne związki organiczne i ultradrobne cząstki, więc zalecana jest dobra wentylacja i nie stawianie drukarki w sypialni ani salonie. PLA jest bezpieczniejszy niż ABS, ale też coś emituje. Komory ogrzewane wymagają odpowiedniej elektroniki, by ograniczyć ryzyko pożaru. Strome nawisy wymagają podpór.
Słowa: warping, przyczepność stołu, komora, wentylacja, opary, VOC, przeciąg, podpory, nawisy, bezpieczeństwo druku, pożar
Źródło: https://en.wikipedia.org/wiki/Fused_filament_fabrication (sprawdzono 2026-10)

### Na co zwrócić uwagę projektując obudowę radiową?
Z danych w naszej bazie wynika: obudowa nie może ekranować anteny (metalowe lub węglowe wypełnienia pogarszają odbiór, więc antenę wyprowadzaj poza obudowę lub używaj plastiku bez wypełnień przewodzących), złącze antenowe trzeba uszczelnić, a wnętrze zabezpieczyć przed wilgocią. Przy słońcu liczy się temperatura zeszklenia materiału. To są zasady ogólne wynikające z poprzednich wpisów (strefa Fresnela, UV/temperatura), a nie przepisy producenta.
Słowa: projekt obudowy, ekranowanie anteny, uszczelnienie, wilgoć, węgiel, kompozyt, IP, wyprowadzenie anteny, drukowana obudowa
Źródło: wnioski z wpisów o druku 3D i antenach (sprawdzono 2026-10); zasady ogólne, nie przepisy
