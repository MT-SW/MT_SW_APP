# Yagi, SWR, instalacje solarne, Wi-Fi i Bluetooth LE

### Jak zbudowana jest antena Yagi i co daje?
Yagi–Uda ma jeden element zasilany i elementy bierne na belce: reflektor (nieco dłuższy, z tyłu) i jeden lub więcej direktorów (nieco krótszych, z przodu). Odstępy to zwykle 1/10–1/4 długości fali. Zysk rośnie z liczbą elementów (do ok. 20 dBi), stosunek przód–tył sięga ok. 20 dB, polaryzacja jest liniowa (pozioma lub pionowa zależnie od montażu). Wada: wąskie pasmo, ok. 2–3% częstotliwości środkowej, a więcej elementów zwęża je jeszcze bardziej. Nie ma prostych wzorów, wymiary dobiera się iteracyjnie z istniejącego projektu, pomiarem lub symulacją. Element zasilany często jest dipolem pętlowym (impedancja ok. 50–75 Ω).
Słowa: Yagi, antena kierunkowa, reflektor, direktor, zysk dBi, przód-tył, antena Yagi LoRa, belka, polaryzacja, dipol pętlowy
Źródło: https://en.wikipedia.org/wiki/Yagi%E2%80%93Uda_antenna (sprawdzono 2026-10)

### Co to jest SWR i jaki jest dobry?
SWR (współczynnik fali stojącej) mierzy dopasowanie impedancji obciążenia do linii. 1:1 oznacza brak fali odbitej, wartość nieskończona całkowite odbicie. Artykuł nie podaje progu "dobrego" SWR, mówi tylko, że ma być jak najbliżej 1:1 i że ma większe znaczenie na wyższych częstotliwościach i długich liniach. SWR jest matematycznie powiązany ze współczynnikiem odbicia i tłumiennością odbiciową. Duży SWR to mniejsza moc oddana i, w skrajnych przypadkach, ryzyko uszkodzenia nadajnika. Dla LoRa: nie włączaj radia bez podłączonej anteny.
Słowa: SWR, VSWR, fala stojąca, dopasowanie impedancji, współczynnik odbicia, return loss, tłumienność odbiciowa, nadajnik, antena
Źródło: https://en.wikipedia.org/wiki/Standing_wave_ratio (sprawdzono 2026-10; ostatnie zdanie to wiedza ogólna)

### Dlaczego niedopasowanie i kabel tak bardzo szkodzą na UHF?
Odbita moc zwiększa prąd w linii, więc rosną straty, a te rosną z częstotliwością i długością kabla. Przykład z artykułu: SWR 6:1 na 75 m koncentryku RG-8A daje ok. 2,2 dB strat przy 3,5 MHz, ale ok. 10,8 dB przy 146 MHz. Dla naszych 869 MHz oznacza to, że lepiej krótki, dobry kabel i dobra antena niż długa linia o słabej jakości. Dokładnych strat dla pasma 868 MHz artykuł nie podaje.
Słowa: straty w kablu, koncentryk, RG-8A, długość kabla, tłumienie, 868 MHz, SWR a straty, pigtail
Źródło: https://en.wikipedia.org/wiki/Standing_wave_ratio (sprawdzono 2026-10; wniosek o 869 MHz to rozumowanie ogólne)

### Jak dobrać i zbudować solarny system off-grid?
Składniki: panel (moduły łączone szeregowo dla napięcia, a gałęzie równolegle dla prądu), akumulator, regulator ładowania, falownik tylko dla odbiorników AC oraz okablowanie i odłączniki. Zaczynasz od dziennego zużycia energii i uzysku z lokalnego nasłonecznienia, z uwzględnieniem strat (w przykładzie ok. 22%). Orientacyjnie 1 kW mocy w Europie południowej daje 3,5–5 kWh dziennie. Regulator PWM jest prostszy, MPPT zawiera śledzenie punktu mocy; MPPT bywa zbędny, gdy napięcie modułu zbliżone do napięcia akumulatora. Ołowiowy akumulator jest tańszy i odporny, ale krótkożyjący. Zakładaj wymianę akumulatora wcześniej niż paneli (w przykładzie 10 lat wobec 25–30).
Słowa: solar off-grid, panel słoneczny, regulator ładowania, MPPT, PWM, akumulator, falownik, uzysk kWh, instalacja fotowoltaiczna, dobór paneli
Źródło: https://en.wikipedia.org/wiki/Photovoltaic_system (sprawdzono 2026-10)

### Zacienienie, nachylenie i zabrudzenie paneli
Cień jest bardzo szkodliwy: jedno zacienione ogniwo może pochłonąć moc wielu innych, a diody bocznikujące tylko ograniczają straty. Unikaj cienia drzew i przeszkód. Stałe panele kieruje się na południe (półkula północna); w układach autonomicznych kąt wybiera się pod szczyt zapotrzebowania, nie zawsze pod maksimum roczne, i można go zmieniać sezonowo. Zabrudzenia dają średnio 3–4% strat globalnie, w miejscach zakurzonych czyszczenie się opłaca. Trackery jednoosiowe dodają ok. 20–25%, dwuosiowe ok. 30%, ale przy zachmurzeniu niewiele pomagają.
Słowa: zacienienie, nachylenie paneli, kąt panela, południe, zabrudzenie, tracker, dioda bocznikująca, zima, uzysk
Źródło: https://en.wikipedia.org/wiki/Photovoltaic_system (sprawdzono 2026-10)

### Wi-Fi: pasma, kanały i bezpieczeństwo
2,4 GHz ma największy zasięg, ale jest zatłoczone (mikrofalówki, Bluetooth); 5 GHz jest szybsze i ma więcej kanałów, ale gorzej przenika ściany; 6 GHz dotyczy Wi-Fi 6E i 7. Na 2,4 GHz kanały mają po ok. 20 MHz, więc nie nakładają się tylko 1, 6 i 11 (w Europie dostępne też 12 i 13). Zasięg w domu to ok. 20 m. Bezpieczeństwo: WEP jest złamany, WPA2 (AES) z mocnym hasłem uchodzi za bezpieczny, WPA3 jest następcą; wyłącz WPS, a ukrywanie SSID i filtr MAC dają niewiele. W miejscach publicznych używaj VPN i HTTPS.
Słowa: Wi-Fi, 2,4 GHz, 5 GHz, kanały 1 6 11, WPA2, WPA3, WPS, router, zasięg, interferencje, Wi-Fi 6
Źródło: https://en.wikipedia.org/wiki/Wi-Fi (sprawdzono 2026-10)

### Bluetooth Low Energy: zasięg, pobór prądu i GATT
BLE działa na 2,4 GHz, 40 kanałów po 2 MHz, 1 Mbit/s (2 Mbit/s opcjonalnie w BT 5). Nominalny zasięg do ok. 100 m; Bluetooth 5 może go czterokrotnie zwiększyć (tryb coded PHY). Pobiera ok. 0,01–0,5 W (klasyczny ok. 1 W). Beacon może pracować 1–2 lata na ogniwku 1000 mAh, ale ciągłe skanowanie wyczerpuje je w kilka godzin. Dane są ułożone w usługi, charakterystyki i deskryptory (GATT), klient czyta, zapisuje i dostaje powiadomienia od serwera; dla większych danych używa się odczytu długiego. Aplikacja Meshtastic łączy się z węzłem po BLE właśnie tak, ale szczegółów protokołu Meshtastic ten artykuł nie podaje.
Słowa: BLE, Bluetooth Low Energy, GATT, charakterystyka, zasięg Bluetooth, pobór energii, MTU, Bluetooth 5, parowanie, beacon
Źródło: https://en.wikipedia.org/wiki/Bluetooth_Low_Energy (sprawdzono 2026-10; ostatnie zdanie to wiedza ogólna)
