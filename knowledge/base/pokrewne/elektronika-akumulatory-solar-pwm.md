# Elektronika: akumulatory litowe, ogniwa słoneczne, PWM i dzielnik napięcia

### Li-ion: ładowanie, temperatura i zasady bezpieczeństwa
Bezpieczny zakres to zwykle 3–4,2 V na ogniwo (niektóre ogniwa 2,5–4,35 V). Ładowanie metodą CC/CV: stały prąd do 4,2 V, potem stałe napięcie aż prąd spadnie do ok. 3 % początkowego. Ładowanie powyżej 4,2 V powoduje osadzanie litu i trwałą utratę pojemności. Ładuj w zakresie ok. 5–45 °C, przy 0–5 °C zmniejsz prąd; poniżej 0 °C wiele ogniw nie wolno ładować, bo grozi zwarciem wewnętrznym. Niska temperatura zwiększa opór wewnętrzny. Najdłużej żyją w temperaturze pokojowej; ładowanie powyżej ok. 80 % i wysoka temperatura skracają życie. Typowa trwałość to kilkaset do ok. 2000 cykli do spadku pojemności do 80 %. Samorozładowanie 1,5–2 % miesięcznie. Zabezpieczenie rozłączy ogniwo przy zbyt niskim napięciu, a w długim składowaniu własny pobór układu może rozładować je poniżej progu i ładowarka go odrzuci.
Słowa: Li-ion, ładowanie zimą, poniżej zera, CC/CV, 4,2 V, cykle, trwałość, składowanie, samorozładowanie, temperatura ładowania, zabezpieczenie ogniwa
Źródło: https://en.wikipedia.org/wiki/Lithium-ion_battery (sprawdzono 2026-10)

### Ucieczka termiczna: co grozi akumulatorom litowym?
Temperatura ogniw przy ucieczce termicznej może przekraczać 500 °C i zapalić otoczenie; elektrolit jest łatwopalny, a dym palny i toksyczny. Przyczyny: przegrzanie, przeładowanie lub zwarcie, uszkodzenie mechaniczne i zwarcia wewnętrzne wynikające z wad lub starzenia. Ogniwa 18650 i większe mogą mieć zabezpieczenia CID i PTC. Chemia LFP jest bezpieczniejsza i trwalsza, ale ma mniejszą pojemność. Uszkodzonego, zgniecionego lub spuchniętego ogniwa nie ładuj i nie wyrzucaj do zwykłych śmieci. Przepisy ograniczają wysyłkę akumulatorów litowych, np. lotniczą.
Słowa: ucieczka termiczna, pożar akumulatora, spuchnięty akumulator, 18650, CID, PTC, LFP, utylizacja, wysyłka akumulatorów, bezpieczeństwo
Źródło: https://en.wikipedia.org/wiki/Lithium-ion_battery (sprawdzono 2026-10)

### Ogniwa słoneczne: mono czy poli, cień i diody bypass
Panele monokrystaliczne (jednolity kryształ, ścięte rogi ogniw) są bardziej wydajne niż polikrystaliczne (tańsze); mono wróciło do szerokiego użycia dzięki poprawie sprawności. Typowe ogniwo krzemowe daje ok. 0,5–0,6 V. Granica teoretyczna dla pojedynczego złącza krzemowego to ok. 33 %, rekord dla krzemu 25,6 % (2014). Współczynnik wypełnienia to stosunek rzeczywistej maksymalnej mocy do iloczynu napięcia jałowego i prądu zwarcia. Cień na części ogniw może wyłączyć słabszą gałąź i grozi uszkodzeniem; diody bypass omijają zacienione ogniwa, ograniczając straty. Źródło nie podaje współczynnika temperaturowego ani rocznej degradacji.
Słowa: panel słoneczny, monokrystaliczny, polikrystaliczny, sprawność, cień, dioda bypass, napięcie jałowe, prąd zwarcia, fill factor, ogniwo krzemowe
Źródło: https://en.wikipedia.org/wiki/Solar_cell (sprawdzono 2026-10)

### PWM: sterowanie wypełnieniem
PWM (modulacja szerokości impulsu) to przełączanie zasilania szybciej, niż obciążenie zdąży zareagować; średnia moc zależy od wypełnienia (duty cycle). Przy wypełnieniu 50 % z zasilania 10 V średnio wychodzi ok. 5 V, przy 25 % ok. 2,5 V (przykład ilustracyjny). Typowe częstotliwości: ściemniacz lamp 100–120 Hz, silniki od kilku do kilkudziesięciu kHz, zasilacze impulsowe dziesiątki do setek kHz. Zbyt niska częstotliwość powoduje migotanie diod LED. PWM jest też stosowany w regulatorach ładowania paneli i do ściemniania diod (ambient lighting) oraz buzzerów PWM w Meshtastic.
Słowa: PWM, wypełnienie, duty cycle, ściemnianie LED, silnik, regulator ładowania, buzzer, częstotliwość przełączania
Źródło: https://en.wikipedia.org/wiki/Pulse-width_modulation (sprawdzono 2026-10; przykłady liczbowe ilustracyjne)

### Dzielnik napięcia: jak zmierzyć napięcie akumulatora
Vout = Vin × R2 / (R1 + R2), czyli wyjście jest zawsze ułamkiem wejścia. Dwa równe rezystory dają połowę. Do pomiaru napięcia akumulatora przez przetwornik ADC odwróć wzór: Vin = Vout × (R1 + R2) / R2. Przykład ilustracyjny: R1 = 20 kΩ i R2 = 10 kΩ, przy 9 V na akumulatorze daje 3 V na ADC. Dzielnik zmienia wynik, jeśli obciążenie (wejście ADC) pobiera dużo prądu; niższe rezystory poprawiają dokładność, ale zużywają więcej energii z baterii. Do zmniejszenia sygnału 5 V do 3,3 V można użyć dzielnika o stosunku 3,3/5.
Słowa: dzielnik napięcia, rezystory, pomiar baterii, ADC, adc_multiplier, 5 V na 3,3 V, obciążenie dzielnika, pobór prądu
Źródło: https://en.wikipedia.org/wiki/Voltage_divider (sprawdzono 2026-10; przykłady 9 V i rezystorów ilustracyjne)
