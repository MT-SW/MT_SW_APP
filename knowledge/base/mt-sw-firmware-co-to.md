# MT_SW_FIRMWARE: co to jest i czym różni się od oficjalnego

### Co to jest MT_SW_FIRMWARE?
MT_SW_FIRMWARE to nasz własny firmware do radia: fork oficjalnego firmware Meshtastic (obecnie na bazie wersji 2.8.2), prowadzony w organizacji MT-SW na GitHubie przez Spontaniczne Majsterkowanie i cheaterenatora. Jest zgodny z główną siecią Meshtastic, ale ma kilka własnych rozwiązań: statystyki radia wysyłane po sieci mesh (local_stats i rozszerzone local_stats_extended z użyciem procesora, pamięci flash i RAM), poprawki prywatności w MQTT, dodatkowe komendy czatu, diagnostykę radia, nowe warianty sprzętowe oraz poprawki stabilności. Domyślnie startuje z presetem Narrow Fast, ale region trzeba wybrać samemu. Firmware nie ma własnego presetu ani regionu: używa oficjalnych.
Słowa: MT_SW_FIRMWARE, firmware MT_SW, nasz firmware, fork firmware, local_stats, cheaterenator, Spontaniczne Majsterkowanie, co to jest firmware MT_SW, czym się różni
Źródło: https://github.com/MT-SW/MT_SW_FIRMWARE (sprawdzono 2026-10)

### Czy z MT_SW_FIRMWARE można rozmawiać z urządzeniami na oficjalnym firmware?
Tak. MT_SW_FIRMWARE nie omija głównej sieci Meshtastic: pakiety, szyfrowanie i presety są zgodne, więc radio z naszym firmware słyszy radia z oficjalnym firmware i odwrotnie (przy tym samym regionie, presecie i kluczu kanału). Własne dodatki, jak rozszerzona telemetria, widzi tylko aplikacja, która je rozumie, np. MT_SW_APP.
Słowa: zgodność, oficjalny firmware, kompatybilny, MT_SW_FIRMWARE, główna sieć, Meshtastic, mieszane sieci
Źródło: https://github.com/MT-SW/MT_SW_FIRMWARE (sprawdzono 2026-10)
