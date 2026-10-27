# Kompilacja firmware i narzędzia wiersza poleceń

### Jak zbudować firmware Meshtastic (VS Code + PlatformIO)?
Potrzebne: Git, PlatformIO i Visual Studio Code. Polecenia: git clone https://github.com/meshtastic/firmware.git, cd firmware, git submodule update --init. Aktualizacja klona: git pull --recurse-submodules. Otwórz folder firmware w VS Code (pierwsze uruchomienie PlatformIO pobiera narzędzia i może trwać długo), w palecie poleceń (Ctrl+Shift+P) wybierz PlatformIO: Pick Project Environment (Twoje urządzenie), potem PlatformIO: Build i PlatformIO: Upload.
Słowa: kompilacja, build, PlatformIO, VS Code, git clone, submodule, upload, pio, środowisko, wariant
Źródło: https://meshtastic.org/docs/development/firmware/build/ (sprawdzono 2026-10)

### Jak dodać własną płytkę (DIY) do firmware?
W katalogu variants utwórz folder i skopiuj istniejącą konfigurację (np. cp heltec_v1/* m5stack_atom), w jego platformio.ini zmień definicję sprzętu na PRIVATE_HW i ustaw -I variants/twój_folder w build_flags, a variant.h dostosuj do płytki. Sprzęt PRIVATE_HW jest "Community Supported": nie ma go w Web Flasherze ani w wydaniach na GitHubie, a utrzymuje go autor.
Słowa: variant, variants, PRIVATE_HW, variant.h, własna płytka, DIY, nowy sprzęt, platformio.ini
Źródło: https://meshtastic.org/docs/development/firmware/build/ (sprawdzono 2026-10)

### Znany błąd przy budowaniu RP2040 na Windows
Błąd "Filename too long" przy budowaniu celów RP2040 na Windows: postępuj według dokumentacji PlatformIO dla arduino-pico dla użytkowników Windows, zanim zainstalujesz toolchain.
Słowa: RP2040, Windows, Filename too long, długa ścieżka, arduino-pico
Źródło: https://meshtastic.org/docs/development/firmware/build/ (sprawdzono 2026-10)

### Jak zainstalować Meshtastic CLI (Python) i połączyć się z węzłem?
Instalacja: pip3 install --upgrade "meshtastic[cli]"; jeśli powłoka nie zna polecenia, dodaj katalog scripts Pythona do PATH. Port: --port /dev/ttyUSB0 (Windows np. COM4; skrót -s), TCP: --host meshtastic.local (-t), Bluetooth: --ble "nazwa_lub_adres" (-b). Bez portu program próbuje wykryć urządzenie; przy kilku podaj port jawnie.
Słowa: CLI, python, pip, meshtastic cli, port, COM, ttyUSB0, host, ble, instalacja
Źródło: https://meshtastic.org/docs/software/python/cli/ (sprawdzono 2026-10)

### Jakie są najważniejsze polecenia CLI?
--info (informacje o urządzeniu); --get lora.region i --set lora.region EU_868 (odczyt i zmiana); --export-config > plik.yaml i --configure plik.yaml (kopia i przywrócenie konfiguracji); --ch-set id 1234 --ch-index 0 (parametr kanału); --set-owner "Nazwa" i --set-owner-short "MJ" (krótka nazwa do 4 znaków); --reboot; --factory-reset (konfiguracja domyślna); --noproto (zwykły terminal szeregowy, nie liczy się jako klient). Po każdym poleceniu --set urządzenie się restartuje, więc łącz kilka ustawień w jedno polecenie.
Słowa: --info, --set, --get, export-config, configure, factory-reset, reboot, set-owner, ch-set, noproto, kopia konfiguracji, yaml
Źródło: https://meshtastic.org/docs/software/python/cli/ (sprawdzono 2026-10)
