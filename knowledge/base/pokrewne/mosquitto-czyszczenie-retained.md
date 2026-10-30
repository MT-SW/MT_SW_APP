<!-- Baza wiedzy MT_SW: jak usunąć wiadomość zachowaną (retained) w Mosquitto poleceniem mosquitto_pub. Tylko informacyjnie. -->
# Mosquitto: usuwanie wiadomości retained

### Jak usunąć wiadomość retained z jednego tematu w Mosquitto?
Wiadomość retained to „ostatnia znana dobra wartość” na temacie, którą broker wysyła każdemu nowemu subskrybentowi. Aby ją usunąć, wyślij pusty payload z flagą retain na ten sam temat. Polecenie to mosquitto_pub -r -t temat -n. Flaga -n wysyła wiadomość o zerowej długości, a -r ustawia retain. Pusta wiadomość z retain czyści zachowaną wartość. Dokumentacja mosquitto_pub potwierdza, że wiadomości o zerowej długości nie są zachowywane, więc puste wysłanie z -r usuwa poprzednią.
Słowa: mosquitto retained, usunięcie retained, mosquitto_pub -r -n, pusta wiadomość, stara wartość, zachowana wiadomość, temat, last known good, wyczyszczenie tematu, ostatnia wartość
Źródło: https://mosquitto.org/man/mosquitto_pub-1.html (sprawdzono 2026-10; oficjalna strona podręcznika mosquitto_pub)

### Jak wysłać polecenie mosquitto_pub z loginem i hasłem do brokera z uwierzytelnianiem?
Gdy broker wymaga logowania, dodaj opcje -u nazwa_użytkownika i -P hasło do polecenia mosquitto_pub. Według dokumentacji przy protokole MQTT 3.1 lub 3.1.1 hasło bez nazwy użytkownika jest nieprawidłowe, więc podajesz oba. Pełne polecenie czyszczące to mosquitto_pub -r -t temat -n -u użytkownik -P hasło. Hasło w poleceniu zostaje w historii powłoki, więc przy prawdziwym serwerze rozważ podanie go interaktywnie lub przez plik konfiguracyjny. Ta uwaga pochodzi z ogólnej wiedzy, a nie ze źródła.
Słowa: mosquitto_pub -u -P, login mqtt, hasło mqtt, czyszczenie z uwierzytelnianiem, mqtt 3.1.1, historia poleceń, bezpieczeństwo hasła, retain z loginem
Źródło: https://mosquitto.org/man/mosquitto_pub-1.html (sprawdzono 2026-10; oficjalna dokumentacja mosquitto_pub; ostrzeżenie o historii z ogólnej wiedzy)
