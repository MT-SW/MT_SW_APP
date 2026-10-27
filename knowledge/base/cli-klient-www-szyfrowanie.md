# CLI, klient www i szyfrowanie Meshtastic

### Python CLI: instalacja i podstawowe polecenia
Instalacja: pip3 install --upgrade "meshtastic[cli]". Jeśli polecenie meshtastic nie jest znalezione, dodaj katalog skryptów Pythona do PATH. Wybór urządzenia: --port (np. /dev/ttyUSB0 albo COM4) przy USB, --host (np. meshtastic.local) przez sieć. Polecenia: --info (odczyt konfiguracji), --get lora.region (odczyt), --set lora.region EU_868 (zapis), --ch-set id 1234 --ch-index 0 (parametr kanału), --sendtext 'Cześć' (wiadomość, opcjonalnie --ch-index lub --dest), --nodes (tabela węzłów), --export-config > plik.yaml (zapis konfiguracji), --configure plik.yaml (wczytanie), --reboot, --factory-reset. Przykład z dokumentacji: meshtastic --port /dev/ttyUSB0 --info. Eksport do YAML to wygodna kopia zapasowa przed eksperymentami.
Słowa: CLI, python, pip, meshtastic --info, --set, --get, --export-config, --configure, --sendtext, --nodes, kopia konfiguracji, yaml
Źródło: https://meshtastic.org/docs/software/python/cli/ (sprawdzono 2026-10)

### Klient webowy Meshtastic: przeglądarki, Bluetooth, USB, HTTPS
Działa w głównych przeglądarkach, ale najlepiej w opartych na Chromium (Chrome, Edge). Bluetooth wymaga Web Bluetooth API, które jest głównie w Chromium. USB (Web Serial) ma najmniejsze wsparcie platform, pozwala łączyć się z węzłem, zasilać go i flashować. Hostowany klient (client.meshtastic.org) wymaga HTTPS, a węzły mają certyfikaty samopodpisane, więc najpierw otwórz https://ADRES_IP_WĘZŁA/ i zaufaj certyfikatowi; adres IP znajdziesz na ekranie węzła, w tabeli DHCP routera albo w konsoli szeregowej. Zwykłe HTTP działa tylko z urządzeniami ESP32, po skonfigurowaniu połączenia sieciowego. Można też hostować klienta samemu, kod jest na GitHubie. O adresie meshtastic.local strona nie mówi.
Słowa: klient www, web client, Web Bluetooth, Web Serial, HTTPS, certyfikat, client.meshtastic.org, Chrome, Edge
Źródło: https://meshtastic.org/docs/software/web-client/ (sprawdzono 2026-10)

### Jak szyfruje Meshtastic: kanały, wiadomości prywatne, ograniczenia
Kanały: AES-CTR kluczem kanału (16 bajtów to AES-128, 32 bajty to AES-256). Domyślny klucz AQ== jest publiczny, więc każdy odczyta ruch na takim kanale. Od firmware 2.8.1 opcja „Use AEAD” przełącza kanał na AES-CCM z wykrywaniem zmian pakietu; wszystkie węzły kanału muszą mieć to samo ustawienie. Wiadomości prywatne (DM): para kluczy X25519 na węźle, AES-256-CCM; klucze publiczne przychodzą w NodeInfo, a bez klucza odbiorcy węzeł nie wyśle DM. Rozgłoszenia od 2.8 mogą być podpisane (XEdDSA, +66 bajtów). Wiadomości administracyjne przyjmowane są tylko od maks. trzech kluczy admina i z hasłem sesji ważnym 5 minut. Ograniczenia: brak utajnienia w przód (nagrany ruch odszyfruje się po wycieku klucza), X25519 nie jest odporny na komputery kwantowe, a bez AEAD wiadomości kanałowe nie są chronione przed modyfikacją. To nie jest poziom Signal ani TLS 1.3.
Słowa: szyfrowanie, AES, PKC, klucz domyślny AQ==, AEAD, admin key, DM, podpis, XEdDSA, prywatność, bezpieczeństwo
Źródło: https://meshtastic.org/docs/overview/encryption/ (sprawdzono 2026-10)

### Szyfrowanie w praktyce: o czym pamiętać
Według dokumentacji: nie konfiguruj kanałów prywatnych na węzłach bez nadzoru, bo fizyczny dostęp może ujawnić klucz. Usunięcie wiadomości nie chroni ich, jeśli klucz kiedyś wycieknie. Warto okresowo zmieniać klucze kanałów. Zrób kopię kluczy przed wymazaniem i ponownym flashowaniem, bo nowy klucz oznacza nowy numer węzła. Przed wysłaniem czegoś wrażliwego w DM potwierdź klucz publiczny odbiorcy innym kanałem. Numer węzła to CRC-32 klucza publicznego, a węzły ufają pierwszemu zobaczonemu kluczowi, bez centralnego urzędu. Nadawcę niepodpisanej wiadomości kanałowej traktuj jako twierdzenie, nie dowód, bo każdy z kluczem może ją sfałszować.
Słowa: bezpieczeństwo, klucz kanału, kopia klucza, numer węzła, CRC-32, fałszowanie, wyciek klucza, prywatny kanał
Źródło: https://meshtastic.org/docs/overview/encryption/ (sprawdzono 2026-10)
