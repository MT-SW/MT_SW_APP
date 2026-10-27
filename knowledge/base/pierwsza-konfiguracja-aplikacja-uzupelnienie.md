# Pierwsza konfiguracja i aplikacja Android: uzupełnienie

### Pierwsza konfiguracja: region obowiązkowy, preset może nie pasować
Zanim węzeł zacznie rozmawiać z siecią, musisz ustawić region (w CLI: pip3 install --upgrade meshtastic, potem meshtastic --set lora.region KOD). Połączyć się można przez USB (serial), Bluetooth lub sieć (sieć tylko na ESP32). Po ustawieniu regionu węzeł zostaje na domyślnym presecie, który często nie pasuje do lokalnych sieci, a niezgodny preset oznacza, że nie słyszysz nikogo. U nas trzeba więc ustawić preset wąskopasmowy i częstotliwość naszej sieci. Podana w tabeli regionów moc to pułap firmware, a nie prawny limit w Twoim kraju, więc sprawdź krajowy plan pasm. Regiony ITU1_*, ITU2_*, ITU3_* są tylko dla trybu krótkofalarskiego z licencją; bez szyfrowania i z podawaniem znaku.
Słowa: pierwsza konfiguracja, region, lora.region, preset domyślny, ITU1, tryb amatorski, USB, Bluetooth, sieć ESP32, pip meshtastic
Źródło: https://meshtastic.org/docs/getting-started/initial-config/ (sprawdzono 2026-10; zdanie o naszej sieci: wniosek z konfiguracji MT_SW)

### Aplikacja Android (wersja 2.8): ważniejsze funkcje list i mapy
Węzły niesłyszane od zmiany ustawień LoRa mają pomarańczowy znacznik; jest filtr „Ukryj niesłyszane” i zbiorcze usuwanie. Przy firmware 2.8 ikony podpisanej i zweryfikowanej tożsamości zastępują kłódkę PKI, są filtry „tylko podpisane” i „tylko szyfrowane”. Na mapie można filtrować węzły po roli i po sposobie usłyszenia, każda warstwa ma własny suwak przezroczystości, a offline teren (cieniowanie i warstwice) pobierzesz w wersji Google Play, F-Droid i na komputerze. Tryb Managed blokuje całą listę konfiguracji, a lista presetów jest ograniczona do dozwolonych w regionie. Blokady portu debugowania (Lockdown) nie da się cofnąć z aplikacji. Spis stron instrukcji: połączenia, wiadomości, węzły, mapa, ustawienia, telemetria, TAK, MQTT, aktualizacje, widżet, logi debugowania.
Słowa: aplikacja Android, Meshtastic 2.8, węzły niesłyszane, filtr, mapa, offline teren, Managed Mode, Lockdown, podpisana tożsamość, PKI, widżet
Źródło: https://meshtastic.org/docs/software/android/user/ (sprawdzono 2026-10)
