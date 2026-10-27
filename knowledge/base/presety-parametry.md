# Presety modemu: parametry, szybkość, zasięg

### Jakie parametry mają presety ogólne (Long Fast, Medium...)?
SF / szerokość pasma / CR / szybkość teoretyczna / budżet łącza: Short Turbo 7/500 kHz/4:5/21,88 kb/s/140 dB. Short Fast 7/250/4:5/10,94/143. Short Slow 8/250/4:5/6,25/145,5. Medium Turbo 9/500/4:5/7,03/145. Medium Fast 9/250/4:5/3,52/148. Medium Slow 10/250/4:5/1,95/150,5. Long Turbo 11/500/4:8/1,34/150. Long Fast 11/250/4:5/1,07/153. Long Moderate 11/125/4:8/0,34/156. Budżet łącza liczony dla mocy 22 dBm i anteny 0 dBi; szybkości to maksimum teoretyczne bez nagłówków, przeskoków i retransmisji.
Słowa: Long Fast, Medium Fast, Short Fast, SF, spreading factor, budżet łącza, link budget, szybkość, data rate, 153 dB
Źródło: https://meshtastic.org/docs/overview/radio-settings/ (sprawdzono 2026-10)

### Jakie parametry mają presety wąskopasmowe (Narrow Fast naszej sieci)?
Narrow Fast: SF7, CR 4:6, 62,5 kHz, 2,28 kb/s, budżet łącza 149 dB. Narrow Slow: SF8, CR 4:6, 62,5 kHz, 1,30 kb/s, 151,5 dB. Lite Fast: SF9, 4:5, 125 kHz, 1,76 kb/s, 151 dB. Lite Slow: SF10, 4:5, 125 kHz, 0,98 kb/s, 153,5 dB. Tiny Fast: SF7, 4:5, 15,6 kHz, 0,68 kb/s, 155 dB. Tiny Slow: SF8, 4:6, 15,6 kHz, 0,33 kb/s, 157,5 dB. Czyli Narrow Fast (sieć świętokrzyska) ma budżet łącza o ok. 4 dB niższy niż Long Fast (149 vs 153 dB), za to szybkość około dwa razy wyższą.
Słowa: Narrow Fast, Narrow Slow, Lite, Tiny, wąskopasmowy, 62.5 kHz, sieć świętokrzyska, 149 dB, EU_N_868
Źródło: https://meshtastic.org/docs/overview/radio-settings/ (sprawdzono 2026-10)

### Jakie są sloty częstotliwości w Europie?
433 MHz: pasmo 433–434 MHz, cztery sloty z LongFast, fabryczny slot 4 (433,875 MHz), maksymalnie +10 dBm ERP. 868 MHz: pasmo 869,40–869,65 MHz, jeden slot z LongFast, fabryczny slot 1 (869,525 MHz), maksymalnie +27 dBm ERP, duty cycle 10 %. Dla porównania 915 MHz (Ameryka Płn.): 902–928 MHz, +30 dBm, 104 sloty LongFast.
Słowa: slot, częstotliwość, 869.525, 433.875, 906.875, pasmo, kanał, ERP, dBm
Źródło: https://meshtastic.org/docs/overview/radio-settings/ (sprawdzono 2026-10)

### Gdzie wolno używać presetów Turbo, Lite, Narrow, Tiny?
Turbo (500 kHz) nie jest legalne wszędzie: niedostępne w EU_868 i wszystkich regionach wąskopasmowych. Lite tylko w EU_866. Narrow w EU_N_868 oraz w pasmach amatorskich 70 cm i 1,25 m. Tiny w pasmach amatorskich 2 m. Limity mocy zwykle są zdejmowane programowo, gdy is_licensed ma wartość true (tylko dla licencjonowanych krótkofalowców). Meshtastic to nie LoRaWAN, Helium ani TTN.
Słowa: legalność, przepisy, turbo, licencja, is_licensed, krótkofalarski, LoRaWAN, TTN, Helium
Źródło: https://meshtastic.org/docs/overview/radio-settings/ (sprawdzono 2026-10)
