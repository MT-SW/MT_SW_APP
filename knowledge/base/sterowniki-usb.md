# Sterowniki USB i połączenie z komputerem

### Jakie sterowniki USB-serial potrzebne są dla płytek ESP32?
Niektóre płytki wymagają sterownika mostka USB–UART Silicon Labs CP210x, nowsze płytki sterownika CH9102 (rodzina CH340/CH341) firmy WCH. Dokumentacja podaje na Windows linki do sterownika CP210x, pakietu WCH CH9102 oraz wersji CH9102 dla Windows 7; po instalacji zrestartuj komputer. Poprawność instalacji sprawdza strona "Test Serial Driver Installation". Urządzenia nRF52 i RP2040 w trybie UF2 nie wymagają sterowników ani żadnego oprogramowania na żadnym systemie.
Słowa: sterownik, driver, CP210x, CH340, CH9102, COM, port szeregowy, Windows, USB, nie widzi urządzenia, restart komputera
Źródło: https://meshtastic.org/docs/getting-started/serial-drivers/esp32/ (sprawdzono 2026-10)
