# Sieci kratowe, LoRa vs LoRaWAN, decybele i straty w wolnej przestrzeni

### Czym jest sieć kratowa (mesh) i jakie ma zalety?
W sieci kratowej węzły współpracują w przekazywaniu danych: wiadomość przeskakuje z węzła na węzeł, aż dotrze do celu. Żaden pojedynczy węzeł nie jest niezbędny, sieć sama się konfiguruje i potrafi się naprawiać, omijając uszkodzone ścieżki. Wady: przy w pełni połączonej sieci kosztowo rośnie liczba połączeń, a samonaprawa wymaga stałych połączeń; producenci nie uzgodnili wspólnych standardów, więc zgodność między sprzętem nie jest pewna. Artykuł nie wyjaśnia mechanizmu zalewania (flooding). Jak to wygląda w Meshtastic, opisują wpisy o routingu i skalowaniu.
Słowa: mesh, sieć kratowa, węzły, przeskok, samonaprawa, redundancja, routing, topologia, sieć ad hoc
Źródło: https://en.wikipedia.org/wiki/Mesh_networking (sprawdzono 2026-10; ostatnie zdanie: odesłanie do bazy)

### LoRa a LoRaWAN: warstwa fizyczna i sieć
LoRa to warstwa fizyczna: własnościowa modulacja oparta na rozpraszaniu widma "chirp" (CSS). Współczynnik rozproszenia SF wybiera się od 5 do 12; wyższy SF daje większą czułość, ale niższą szybkość, dłuższy czas nadawania i więcej energii. Szybkość to ok. 0,3–27 kbit/s, budżet łącza ok. 155–170 dB, moc do +22 dBm (przepisy krajowe mogą ją ograniczać). Podawany zasięg: do ok. 4,8 km w mieście, 16 km i więcej na wsi w widoczności, w warunkach idealnych rekord rzędu 330 km, a w praktyce ok. 10 km. LoRaWAN to protokół sieciowy nad LoRa: urządzenia końcowe, bramki i serwer sieci, rzadkie wysyłanie małych porcji danych (rolnictwo, monitoring). Meshtastic to osobny protokół kratowy na tej samej warstwie fizycznej, bez bramek i serwera.
Słowa: LoRa, LoRaWAN, chirp spread spectrum, spreading factor, SF, bramka, serwer sieci, LPWAN, czułość, zasięg LoRa
Źródło: https://en.wikipedia.org/wiki/LoRa i https://en.wikipedia.org/wiki/LoRaWAN (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### Decybele: dB, dBm, dBi i proste reguły
dB to logarytmiczna jednostka stosunku: dla mocy 10·log10(stosunek), dla napięcia 20·log10. dBm to moc względem 1 mW (0 dBm = 1 mW), dBW względem 1 W (1 dBW = 30 dBm). Zysk anteny: dBi względem anteny izotropowej, dBd względem dipola półfalowego, przy czym 0 dBd = 2,15 dBi. Reguły: +3 dB to około dwukrotność mocy, +10 dB to dziesięciokrotność, +6 dB to dwukrotność napięcia (czterokrotność mocy), 1 dB to około 26% więcej mocy. Wartości w dB dodaje się zamiast mnożyć, np. 10 + 8 + 7 dB = 25 dB, czyli ok. 316 razy. Poziomy dźwięku nie sumują się arytmetycznie: dwie maszyny po 90 dB dają ok. 93 dB.
Słowa: decybel, dB, dBm, dBi, dBd, logarytm, 3 dB, moc, zysk anteny, miliwat, jednostki radiowe
Źródło: https://en.wikipedia.org/wiki/Decibel (sprawdzono 2026-10)

### Tłumienie w wolnej przestrzeni (FSPL): ile traci sygnał?
Strata w wolnej przestrzeni rośnie o 20 dB na dziesięciokrotny wzrost odległości (podwojenie odległości to ok. +6 dB) i tak samo z częstotliwością (przy antenach izotropowych). Wzór: FSPL (dB) ≈ 32,45 + 20·log10(f w MHz) + 20·log10(d w km). Wyliczone z niego dla 869,4 MHz (nasza sieć): 1 km ≈ 91,2 dB, 5 km ≈ 105,2 dB, 10 km ≈ 111,2 dB, 20 km ≈ 117,2 dB, 50 km ≈ 125,2 dB. To najlepszy przypadek: bez przeszkód i bez zapasu na zaniki, więc w praktyce strat jest więcej. Dla porównania wg źródła: 1 km i 2,4 GHz to ok. 100 dB.
Słowa: FSPL, tłumienie, strata w wolnej przestrzeni, odległość, 869 MHz, wzór, 20 log, zasięg teoretyczny, straty łącza
Źródło: https://en.wikipedia.org/wiki/Free-space_path_loss (sprawdzono 2026-10; wartości dla 869,4 MHz: obliczenie własne ze wzoru)

### Z czego składa się bilans łącza?
Moc nadajnika (dBm) plus zyski anten po obu stronach i ewentualne wzmocnienia, minus straty: kabel i złącza, tłumienie ścieżki (zwykle największe i najbardziej niepewne), straty różne (zapas na zaniki, ciało, niezgodność polaryzacji). Wynik porównujesz z minimalną mocą potrzebną odbiornikowi do wykrycia sygnału; nadwyżka to zapas łącza, który dobiera się zależnie od tego, jak groźne jest zerwanie. Przykład ze źródła: ściana działowa z płyty gipsowej to ok. 6 dB straty na ścianę przy 2,4 GHz (dla 869 MHz źródło wartości nie podaje). Gotowe wpisy o zasięgu i kalkulator w aplikacji liczą tak samo.
Słowa: bilans łącza, link budget, zapas łącza, czułość odbiornika, straty, zysk anteny, moc nadajnika, fade margin, ściana
Źródło: https://en.wikipedia.org/wiki/Link_budget (sprawdzono 2026-10; ostatnie zdanie: odesłanie do bazy)
