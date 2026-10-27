# Filtry, wzmacniacze LNA i zakłócenia na 868 MHz

### Do czego służy filtr SAW 868 MHz?
Filtr pasmowo-przepustowy SAW poprawia odbiór węzłów 868 MHz (Meshtastic, MeshCore, LoRa), tłumiąc sąsiednie zakłócenia, np. z pasma GSM 900. Przykładowy filtr (F4NLV): środek 869 MHz, pasmo ±4 MHz (865–873 MHz), tłumienie wtrąceniowe 1,5–1,8 dB, tłumienie poza pasmem 30–40 dB, moc maks. 27 dBm (ok. 500 mW), złącza u.FL/IPEX. Wpina się go między moduł LoRa a antenę. Jest pasywny: nie wzmacnia, a jego tłumienie wtrąceniowe trzeba wliczyć w bilans łącza. Nasza częstotliwość 869,44165 MHz leży wewnątrz takiego pasma.
Słowa: filtr SAW, filtr pasmowy, bandpass, 868, GSM, zakłócenia, tłumienie wtrąceniowe, insertion loss, u.FL, F4NLV, filtr
Źródło: https://www.passion-radio.com/mesh-accessories/f4nlv-868-saw-3217.html (sprawdzono 2026-10; parametry z karty sklepu)

### Na co uważać przy montażu filtra SAW?
Nie przekraczaj 27 dBm mocy wejściowej filtra. Sprawdź zgodność złączy, nie zaginaj ostro kabli koncentrycznych, obchodź się delikatnie z u.FL (nie narażaj ich na naprężenia przy mocowaniu). W SenseCAP Solar P1/P1-Pro wyjmij wszystkie ogniwa 18650 przed pracami.
Słowa: montaż filtra, u.FL, moc maksymalna, 27 dBm, SenseCAP Solar, ogniwa 18650, zginanie kabla
Źródło: https://www.passion-radio.com/mesh-accessories/f4nlv-868-saw-3217.html (sprawdzono 2026-10)

### Czy wzmacniacz LNA z filtrem (868 MHz) nadaje się do węzła Meshtastic?
Typowy filtrowany przedwzmacniacz 868 MHz (The Pi Hut): wzmocnienie co najmniej 16 dB (MiniCircuits PSA4-5043+), współczynnik szumów 0,75 dB, filtr SAW 15 MHz, zasilanie przez bias-tee do 5 V lub USB-C, złącza SMA. Jest przeznaczony do odbiorników SDR (RTL-SDR, Airspy) i NIE ma obejścia toru nadawania: nadawanie przez niego nie zadziała i może go uszkodzić. Do węzła, który nadaje, potrzebny jest LNA z obejściem nadawania (TX bypass), inaczej nie wolno go wpinać między radio a antenę.
Słowa: LNA, przedwzmacniacz, preamp, wzmacniacz odbioru, bypass, TX bypass, bias-tee, SDR, PSA4-5043, współczynnik szumów, noise figure
Źródło: https://thepihut.com/products/868mhz-filtered-preamplifier (sprawdzono 2026-10)

### Co zrobić z silnym zakłóceniem na 869,525 MHz?
Z wątku na forum Meshtastic: ciągły sygnał ok. −20 do −35 dB na 869,525 MHz zagłusza odbiór, urządzenia w jego zasięgu "są prawie głuche", wysyłają, ale prawie nie odbierają. Podejrzewano harmoniczną innego sygnału lub wadliwe złącze, ruch LoRa o innych ustawieniach, kierunkową antenę, a w Wielkiej Brytanii system bezpieczeństwa PilotAware na tej samej częstotliwości (np. z lotniska). Działania: zlokalizować źródło prostą anteną kierunkową, zanalizować przez SDR (SDRSharp, wolne wodospadowe odświeżanie, zmierzyć szerokość chirpów), zgłosić do regulatora (Ofcom, Bundesnetzagentur): Ofcom podobno rzadko bada zakłócenia nieszkodliwe. W wątku nie proponowano filtrów ani LNA.
Słowa: zakłócenia, 869.525, interferencja, głuchy odbiornik, SDR, PilotAware, szum, jamming, źródło zakłóceń
Źródło: https://meshtastic.discourse.group/t/lora-band-interference-868mhz-869-525/12415 (sprawdzono 2026-10; dyskusja forum, nie dokumentacja)
