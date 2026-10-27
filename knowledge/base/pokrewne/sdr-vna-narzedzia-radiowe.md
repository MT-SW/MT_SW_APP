# Narzędzia radiowe: SDR i VNA

### SDR i tani odbiornik RTL-SDR
Radio programowe (SDR) wykonuje mieszanie, filtrowanie i demodulację w oprogramowaniu; jedno urządzenie obsłuży wiele protokołów. RTL-SDR to tanie odbiorniki na układzie Realtek RTL2832U, pierwotnie do telewizji DVB-T (w 2010 odkryto surowe dane); sterowniki rozwijał projekt Osmocom. Wersje dedykowane z lepszym ekranowaniem kosztują ok. 30 USD (2025). Zakres to w przybliżeniu 24–1766 MHz przy paśmie do 3,2 MHz. Zastosowania: radio FM, ADS-B i ACARS (samoloty), sondy meteorologiczne, satelity pogodowe, radioastronomia amatorska, tryby krótkofalarskie. Do odbioru 868 MHz nadaje się zakresem, więc można obejrzeć zajętość pasma lub sprawdzić, czy węzeł nadaje; to jednak odbiornik, nie narzędzie do dekodowania szyfrowanych wiadomości Meshtastic.
Słowa: SDR, RTL-SDR, RTL2832U, odbiornik, widmo, 868 MHz podgląd, ADS-B, Osmocom, dongle USB, analiza pasma
Źródło: https://en.wikipedia.org/wiki/Software-defined_radio (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

### Analizator wektorowy (VNA, NanoVNA): do czego służy?
VNA mierzy parametry S urządzeń radiowych (amplitudę i fazę); S11 to odbicie na porcie, z którego wynika SWR i impedancja. Kalibracja SOLT (short, open, load, through) z wzorcami 50 Ω koryguje błędy układu pomiarowego. Tanie, amatorskie VNA poniżej 100 USD mają mniej funkcji, ale wystarczą do nauki i hobby do kilku GHz. Źródło nie opisuje wprost strojenia anten ani nazwy NanoVNA w tekście; w praktyce hobbyści używają takiego urządzenia do sprawdzania rezonansu i SWR własnych anten na 868/869 MHz (wiedza ogólna).
Słowa: VNA, NanoVNA, S11, SWR pomiar, kalibracja SOLT, strojenie anteny, analizator anten, rezonans, impedancja
Źródło: https://en.wikipedia.org/wiki/Vector_network_analyzer (sprawdzono 2026-10; ostatnie zdanie: wiedza ogólna)

