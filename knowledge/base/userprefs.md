# userPrefs.jsonc: ustawienia domyślne w firmware

### Do czego służy userPrefs.jsonc?
To plik w głównym katalogu repozytorium firmware (JSON z komentarzami), w którym ustawia się domyślne wartości wbudowane w firmware przy jego budowaniu. Wszystkie klucze zaczynają się od USERPREFS_. Większość wpisów jest w pliku zakomentowana jako przykłady.
Słowa: userPrefs, jsonc, USERPREFS, domyślne ustawienia, kompilacja firmware, budowanie
Źródło: https://github.com/meshtastic/firmware/blob/master/userPrefs.jsonc (sprawdzono 2026-10)

### Jak włączyć klucz w userPrefs.jsonc?
Znajdź wiersz klucza, usuń początkowe // , wpisz wartość jako tekst w cudzysłowie, a po każdym wpisie poza ostatnim zostaw przecinek. Przykład: "USERPREFS_CONFIG_LORA_REGION": "meshtastic_Config_LoRaConfig_RegionCode_US",
Słowa: odkomentować, włączyć klucz, przecinek, cudzysłów, składnia
Źródło: https://github.com/meshtastic/firmware/blob/master/userPrefs.jsonc (sprawdzono 2026-10)

### Jakie są główne klucze userPrefs?
LoRa: USERPREFS_CONFIG_LORA_REGION, USERPREFS_LORACONFIG_MODEM_PRESET, USERPREFS_LORACONFIG_CHANNEL_NUM, USERPREFS_LORA_TX_DISABLED. Kanały: USERPREFS_CHANNELS_TO_WRITE oraz USERPREFS_CHANNEL_<n>_NAME / _PSK / _UPLINK_ENABLED / _DOWNLINK_ENABLED. Właściciel i urządzenie: USERPREFS_CONFIG_OWNER_LONG_NAME, _SHORT_NAME, USERPREFS_CONFIG_DEVICE_ROLE. GPS: USERPREFS_CONFIG_GPS_MODE, USERPREFS_FIXED_GPS. MQTT: USERPREFS_MQTT_ENABLED, _ADDRESS, _USERNAME, _PASSWORD, _ROOT_TOPIC. Wi-Fi: USERPREFS_NETWORK_WIFI_SSID, _PSK.
Słowa: klucze, region, preset, kanał, PSK, rola, GPS, MQTT, Wi-Fi, właściciel, nazwa węzła
Źródło: https://github.com/meshtastic/firmware/blob/master/userPrefs.jsonc (sprawdzono 2026-10)

### Na co uważać w userPrefs?
Przykładowy plik zawiera przykładowe klucze PSK, klucz administratora, login i hasło MQTT oraz pola Wi-Fi: traktuj je jako wzory i zamień przed użyciem, zwłaszcza w sieci prywatnej. Zastąp też wartość placeholder w USERPREFS_TZ_STRING prawdziwą strefą czasową. Nie wrzucaj swoich haseł do publicznego repozytorium.
Słowa: hasło, bezpieczeństwo, PSK, klucz admina, strefa czasowa, TZ, repozytorium publiczne
Źródło: https://github.com/meshtastic/firmware/blob/master/userPrefs.jsonc (sprawdzono 2026-10)
