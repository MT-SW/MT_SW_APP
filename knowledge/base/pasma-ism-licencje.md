# Pasma ISM i SRD, licencja krótkofalarska

### Co znaczy ISM i dlaczego w tych pasmach trzeba znosić zakłócenia?
ISM to pasma przemysłowe, naukowe i medyczne: ITU przeznacza je dla urządzeń, które lokalnie wytwarzają energię radiową do celów przemysłowych, naukowych, medycznych czy domowych, nie dla telekomunikacji. Usługi łączności w tych pasmach muszą akceptować zakłócenia od urządzeń ISM i nie mają przed nimi ochrony prawnej. Urządzenia krótkiego zasięgu (SRD), m.in. LoRa, to urządzenia małej mocy bez licencji; wiele z nich korzysta z pasm ISM. W Europie przepisy dla SRD opierają się na zaleceniach CEPT i normach ETSI.
Słowa: ISM, SRD, urządzenia krótkiego zasięgu, bez licencji, zakłócenia, ochrona prawna, ETSI, CEPT, pasmo bezlicencyjne
Źródło: https://en.wikipedia.org/wiki/ISM_radio_band (sprawdzono 2026-10)

### Pasmo 433 MHz i 2,4 GHz: co jeszcze tam pracuje?
433,05–434,79 MHz (środek 433,92 MHz, szerokość 1,74 MHz) to pasmo ISM tylko w Regionie 1 ITU (m.in. Europa), w krajach, które je przyjęły. Korzystają z niego m.in. bezlicencyjne radiotelefony LPD433, piloty i czujniki oraz LoRa. Jednocześnie 430–450 MHz to pasmo krótkofalarskie 70 cm, więc w okolicy 433 MHz pracują też radioamatorzy. 2,4–2,5 GHz jest dostępne na całym świecie i bardzo zatłoczone: Wi-Fi, Bluetooth, Zigbee. Źródło nie opisuje pasma 863–870 MHz.
Słowa: 433 MHz, 433,92, LPD433, piloty, 70 cm, krótkofalowcy, 2,4 GHz, Wi-Fi, Bluetooth, Zigbee, zatłoczone pasmo
Źródło: https://en.wikipedia.org/wiki/ISM_radio_band i https://en.wikipedia.org/wiki/Amateur_radio_licensing (sprawdzono 2026-10)

### Czym jest licencja krótkofalarska i co daje?
To indywidualne pozwolenie ze znakiem wywoławczym, wydawane po egzaminie z teorii radia, elektroniki, propagacji, zasad pracy i przepisów (czasem też bezpieczeństwa RF i anten). Egzaminy mają poziomy; wyższy daje więcej pasm i większą moc. Licencja daje szerszy dostęp do widma i większą moc niż usługi bezlicencyjne (CB, PMR446). Ważna jest w kraju wydania i w krajach z umowami wzajemnymi. Przykładowe maksymalne moce najwyższych klas: Niemcy 750 W, Włochy 500 W, Wielka Brytania 1 kW. Szczegóły dla Polski (UKE, kategorie) nie są w tym źródle. Ważne dla Meshtastic: w regionach amatorskich szyfrowanie jest zabronione i trzeba nadawać znak.
Słowa: licencja, krótkofalowiec, radioamator, egzamin, znak wywoławczy, UKE, moc, pasma amatorskie, CB, PMR446, szyfrowanie zabronione
Źródło: https://en.wikipedia.org/wiki/Amateur_radio_licensing i https://meshtastic.org/docs/configuration/radio/lora/ (sprawdzono 2026-10)

### Na jakiej podstawie prawnej w Polsce używa się LoRa bez pozwolenia?
UKE wskazuje art. 145 Prawa komunikacji elektronicznej (wcześniej art. 144 Prawa telekomunikacyjnego): z obowiązku pozwolenia zwolnione są m.in. urządzenia tylko odbiorcze, urządzenia klasy 1 (bez ograniczeń wprowadzania w UE) oraz urządzenia wymienione w rozporządzeniu wykonawczym. Obecnie jest to rozporządzenie Ministra Cyfryzacji z 9 lutego 2022 (Dz.U. 2022 poz. 567) w sprawie urządzeń radiowych nadawczych i nadawczo-odbiorczych używanych bez pozwolenia, zmienione rozporządzeniem z 11 listopada 2023 (Dz.U. 2023 poz. 2567). Strona UKE nie podaje liczb dla 863–870 MHz ani 433 MHz, a treści rozporządzenia nie udało się pobrać, więc konkretne limity (moc, cykl pracy) sprawdzaj w samym akcie. Limity, które stosuje firmware Meshtastic dla EU_868, są w osobnym wpisie.
Słowa: prawo, UKE, bez pozwolenia, rozporządzenie, Dz.U. 2022 poz. 567, Dz.U. 2023 poz. 2567, art. 145, Prawo komunikacji elektronicznej, legalność LoRa, przepisy Polska
Źródło: https://bip.uke.gov.pl/pozwolenia-radiowe/urzadzenia-niewymagajace-pozwolenia-/ (sprawdzono 2026-10; treść rozporządzenia niepobrana)
