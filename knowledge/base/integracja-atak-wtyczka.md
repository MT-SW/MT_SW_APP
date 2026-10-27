# Wtyczka ATAK dla Meshtastic

### Jak działa wtyczka ATAK i jakie ma wymagania?
Oficjalna wtyczka Meshtastic do ATAK (klient TAK na Androidzie) przekazuje ruch Cursor on Target (CoT) przez sieć mesh. Łączy się z interfejsem IMeshService aplikacji Meshtastic na Androida, przechwytuje wychodzące wiadomości CoT i nasłuchuje pakietów na porcie ATAK_PLUGIN. Wymagania: aplikacja Meshtastic na Androida u wszystkich uczestników (działa w tle, z działającą łącznością LoRa), rola urządzenia `TAK` w konfiguracji, wtyczka zgodna z wersją ATAK (pobierana z GitHub Releases), wszyscy na tych samych kanałach. Zielona ikona Meshtastic w prawym dolnym rogu oznacza połączenie z IMeshService, czerwona brak połączenia, brak ikony błędną instalację. Wtyczka nie konfiguruje Meshtastic (to robi się w aplikacji lub urządzeniu) i działa tylko na Androidzie.
Słowa: atak tak cot cursor on target wtyczka plugin imeshservice atak_plugin android rola tak wojsko
Źródło: https://meshtastic.org/docs/software/integrations/integrations-atak-plugin/ (sprawdzono 2026-10)

### Rola TAK_TRACKER: pozycja bez uruchomionego ATAK
Dla urządzeń z GPS można ustawić rolę `TAK_TRACKER`: urządzenie wysyła wtedy PLI (informację o pozycji) niezależnie od ATAK, a dane są widoczne w ATAK u osób z wtyczką, połączonych z Meshtastic na tych samych kanałach. Częstotliwość wysyłania zależy od standardowych ustawień Position. Znakiem wywoławczym w PLI jest Long Name użytkownika. Tryb wymaga modułu GPS w urządzeniu.
Słowa: tak_tracker pli pozycja atak gps rola long name znak wywoławczy tracker
Źródło: https://meshtastic.org/docs/software/integrations/integrations-atak-plugin/ (sprawdzono 2026-10)
