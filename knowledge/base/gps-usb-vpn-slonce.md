# GPS, zasilanie USB i nasłonecznienie

### Jak działa GPS w węźle i jak dokładny jest?
Systemy globalne: GPS (USA), GLONASS (Rosja), Galileo (UE) i BeiDou (Chiny); do tego wspomaganie SBAS, w Europie EGNOS. Do ustalenia pozycji potrzebne są sygnały z 3 satelitów (4, jeśli ma być też wysokość), a sygnał biegnie w linii prostej, więc odbiornik potrzebuje widoku nieba. Dokładność sygnałów publicznych według źródła: GPS bez wspomagania 0,3–5 m, GLONASS 2–4 m, BeiDou 3,6 m, Galileo ok. 0,2 m; EGNOS w praktyce na poziomie metra. Odbiorniki wielosystemowe widzą więcej satelitów i szybciej ustalają pozycję. GPS nie wymaga zasięgu telefonu ani internetu. Źródło nie podaje czasów zimnego i ciepłego startu.
Słowa: GPS, GNSS, Galileo, GLONASS, BeiDou, EGNOS, dokładność, satelity, widok nieba, brak fixa, pozycja, wysokość
Źródło: https://en.wikipedia.org/wiki/Satellite_navigation (sprawdzono 2026-10)

### Ile prądu da port USB i dlaczego kabel czasem nie działa?
USB 2.0: do 500 mA przy 5 V (2,5 W). USB 3.0: do 900 mA (4,5 W). USB-C: 900 mA bazowo, a po uzgodnieniu 1,5 A lub 3 A przy 5 V. USB Power Delivery: do 100 W (20 V, 5 A), w rozszerzonym zakresie 3.1 do 240 W (48 V, 5 A); powyżej 3 A potrzebny kabel na 5 A. Kable "tylko do ładowania" nie mają linii danych, więc urządzenie się ładuje, ale komputer go nie widzi i nie da się wgrać firmware ani połączyć przez port szeregowy. Dokumentacja Meshtastic radzi sprawdzić kabel, kopiując plik na telefon i z powrotem.
Słowa: USB, prąd USB, 500 mA, USB-C, Power Delivery, PD, kabel do ładowania, kabel bez danych, nie widzi urządzenia, zasilanie z USB, powerbank
Źródło: https://en.wikipedia.org/wiki/USB_hardware i https://meshtastic.org/docs/getting-started/ (sprawdzono 2026-10)

### Ile energii daje słońce i jak nachylić panel?
W południe przy czystym niebie na poziomie morza dociera ok. 1000 W/m² (to wartość, przy której podaje się moc paneli). Dzienne nasłonecznienie przy bezchmurnym niebie to ok. 6 kWh/m², czyli w przybliżeniu 6 "godzin pełnego słońca" (wniosek z tych liczb). Na szerokości 50° (Polska) odpowiednio nachylony panel dostaje ok. 1860 kWh/m² rocznie przy pomijaniu chmur; w Norwegii poziomo instalacje dają 800–950 kWh na kWp rocznie. Natężenie spada z cosinusem kąta padania: promień padający pod kątem 30° do powierzchni rozkłada się na dwa razy większej powierzchni. Obliczenia bez uwzględnienia nachylenia zaniżają zimę i zawyżają lato, a zimą w Polsce słońca jest najmniej, więc panel do węzła dobiera się pod zimę (wniosek ogólny).
Słowa: nasłonecznienie, 1000 W/m², godziny słońca, panel słoneczny, nachylenie panelu, kąt, Polska, zima, dobór panelu, solar
Źródło: https://en.wikipedia.org/wiki/Solar_irradiance (sprawdzono 2026-10; liczby dla Polski z chmurami nie są podane w źródle)
