# Moduły: telemetria i czujniki, powiadomienia, gotowe wiadomości, Serial, Remote Hardware

### Telemetria środowiskowa: domyślne interwały i ustawienia
Interwał metryk środowiskowych i urządzenia to domyślnie 1800 s (30 min); aplikacje podłączone do węzła dostają metryki urządzenia co minutę niezależnie od tego. Jakość powietrza, zasilanie i zdrowie są domyślnie wyłączone (Air Quality co 1800 s). Pokazywanie temperatury w °F dotyczy tylko wyświetlacza, bo czujniki zawsze mierzą w °C. Metryki mocy (INA219 itd.) nie są potrzebne do samego napięcia baterii. Moduł zdrowia wymaga firmware skompilowanego z odpowiednimi makrami. Strona jest niespójna co do domyślnego interwału urządzenia (30 min lub 5 min w komentarzu CLI), więc sprawdź na własnym firmware. Czujniki na I2C są wykrywane automatycznie przy starcie, ale moduły środowiska, powietrza i zdrowia trzeba włączyć.
Słowa: telemetria, environment, interwał 1800, metryki urządzenia, temperatura, wilgotność, czujnik, I2C, air quality, telemetry
Źródło: https://meshtastic.org/docs/configuration/module/telemetry/ (sprawdzono 2026-10)

### Jakie czujniki obsługuje telemetria Meshtastic?
Środowisko (temperatura, wilgotność, ciśnienie): AHT10/AHT20, BMP085/180/280, BME280, BMP388/390, BME68x (także gaz), DPS310, SHTC3, SHT31, SHT4X, MCP9808, LPS22, PCT2075, DFROBOT_LARK (wiatr). Powietrze: PMSA003I, HM330X, SEN5X, SEN6X, SCD30, SCD4X (CO2), SFA30 (formaldehyd). Światło: OPT3001, VEML7700, TSL2591, TSL2561, BH1750, LTR390UV. Zasilanie: INA219, INA226, INA260, INA3221, MAX17048. Zdrowie: MAX30102, MLX90614, MLX90632. Inne: RCWL9620 (odległość ultradźwiękowa), DFROBOT_RAIN (deszczomierz), RadSens (promieniowanie), NAU7802, ADS1X15, AS3935 (burze; wymaga podpiętego pinu IRQ), DS18B20 przez DS2482. Nie każdy czujnik działa na każdej płytce, więc sprawdź obsługę dla swojego wariantu.
Słowa: czujniki, BME280, INA219, SCD4X, CO2, deszczomierz, AS3935, burza, DS18B20, stacja pogodowa, I2C adres
Źródło: https://meshtastic.org/docs/configuration/module/telemetry/ (sprawdzono 2026-10; ostatnie zdanie: ogólna ostrożność)

### Powiadomienia zewnętrzne: dioda, wibracja, buzzer
Moduł steruje do trzech pinów niezależnie: ogólnym (LED), wibracją i buzzerem. Active ustala, czy pin jest aktywny stanem wysokim, czy niskim. Alert Bell reaguje na znak dzwonka ASCII 0x07, Alert Message na wiadomość tekstową, osobno dla każdego wyjścia. Czas aktywności pinu (Output Milliseconds): 0 oznacza 1000 ms, w trybie powtarzania to długość tonu i przerwy. Use PWM używa wbudowanego buzzera PWM (pin buzzera jest wtedy ignorowany, a brany z device.buzzer_gpio) i odtwarza melodie RTTTL wgrywane z aplikacji. Nag Timeout powtarza alert do naciśnięcia przycisku lub upływu sekund. Na ESP32 piny GPIO 34–39 są tylko wejściami i nie nadają się na wyjścia.
Słowa: powiadomienia zewnętrzne, external notification, buzzer, LED, wibracja, RTTTL, melodyjka, alert bell, nag timeout, GPIO 34
Źródło: https://meshtastic.org/docs/configuration/module/external-notification/ (sprawdzono 2026-10)

### Gotowe wiadomości (Canned Message): format i źródła wejścia
Wiadomości oddziela się pionową kreską, np. "Potrzebuję pomocy|Zadzwoń|Jadę" (łącznie do 200 bajtów). Źródła wejścia: _any (domyślnie), rotEnc1 (enkoder obrotowy), upDownEnc1 (enkoder góra–dół, także RAK14006), scanAndSelect (pojedynczy przycisk, krótkie i długie naciśnięcie), cardkb (M5Stack CardKB, także RAK14004) oraz serialkb. Piny GPIO podaje się w zakresie 1–39; w klawiaturze matrycowej nie da się użyć co czwartego miejsca (4, 8, 12). Zdarzenia enkodera (zgodnie, przeciwnie, naciśnięcie) mapuje się na UP, DOWN, SELECT. Błędne ustawienia GPIO mogą fizycznie uszkodzić sprzęt, a moduł wymaga podłączonego urządzenia wejściowego.
Słowa: canned message, gotowe wiadomości, enkoder, CardKB, przycisk, wiadomości szybkie, pipe, rotary encoder, 200 bajtów
Źródło: https://meshtastic.org/docs/configuration/module/canned-message/ (sprawdzono 2026-10)

### Moduł Serial: tryby i połączenie przewodowe
Tryby: SIMPLE (domyślny, surowy przesył przez kanał o nazwie "serial", który trzeba najpierw utworzyć; przykłady do 237 bajtów na pakiet), PROTO (API Protobuf, obserwacja przez meshtastic --listen), TEXTMSG (tekst z portu szeregowego idzie jako wiadomość na kanał domyślny), NMEA (zdania $GNGGA i $GPWPL co 2 s) oraz CALTOPO ($GPWPL co 10 s dla CalTopo/SARTopo); są też WS85, VE_DIRECT, MS_CONFIG, LOG i LOGTEXT. Domyślna prędkość 38400 baud; Timeout w trybie SIMPLE 0 oznacza 250 ms. Zawsze krzyżuj linie: TX urządzenia do RX adaptera i odwrotnie. Do T1000-E użyj adaptera 3,3 V, bo 5 V może uszkodzić nRF52840.
Słowa: moduł Serial, UART, NMEA, CALTOPO, TEXTMSG, PROTO, baud, RXD, TXD, T1000-E, port szeregowy
Źródło: https://meshtastic.org/docs/configuration/module/serial/ (sprawdzono 2026-10)

### Remote Hardware: zdalne sterowanie pinami GPIO
Pozwala ustawiać, czytać i obserwować piny GPIO zdalnego węzła; moduł musi być włączony na obu węzłach, a do dostępu z uwierzytelnieniem tworzy się kanał o nazwie "gpio". Obserwacja nie wyłapuje szybkich zmian (przyciski), do tego służy moduł Detection Sensor. Każdy pin ma bit w masce (GPIO 1 to 0x2, GPIO 2 to 0x4). Odczyt i zapis działają dziś tylko z Python CLI (--gpio-wrb, --gpio-rd, --gpio-watch z --dest); wyniki mogą trafiać do MQTT jako JSON. Dla wersji powyżej 2.5.3 trzeba samemu skompilować firmware bez flagi -DMESHTASTIC_EXCLUDE_REMOTEHARDWARE=1. Dokumentacja nazywa to "zasadniczo niebezpiecznym": zły ruch może zniszczyć sprzęt, więc poznaj schemat płytki.
Słowa: Remote Hardware, GPIO, zdalne sterowanie, gpio-wrb, gpio-rd, kanał gpio, przekaźnik, bezpieczeństwo, maska pinów
Źródło: https://meshtastic.org/docs/configuration/module/remote-hardware/ (sprawdzono 2026-10)
