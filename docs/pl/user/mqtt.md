---
title: MQTT
parent: Przewodnik użytkownika
nav_order: 11
last_updated: 2026-09-19
description: Połącz swoją sieć mesh z internetem — konfiguracja brokera MQTT, warstwy szyfrowania i raportowanie na mapę.
aliases:
  - mqtt
  - internet-bridge
  - broker
---

# MQTT

MQTT łączy Twoją sieć mesh z internetem, umożliwiając komunikację na odległość większą niż zasięg radiowy.

## Przegląd

Moduł MQTT łączy węzeł z brokerem MQTT, co pozwala na:
- Docieranie wiadomości do węzłów w innych, fizycznie oddzielnych sieciach mesh przez internet
- Integrację z automatyką domową i systemami monitoringu
- Publikowanie pozycji węzłów na publicznej mapie Meshtastic
- Własne potoki danych do logowania i alarmów

## Jak to działa

```text
[Your Node] → Radio → [Gateway Node with Wi-Fi] → MQTT Broker → [Remote Gateway] → Radio → [Remote Node]
```

Węzeł-brama z dostępem do internetu (Wi-Fi lub Ethernet) publikuje wiadomości z sieci mesh w temacie MQTT. Zdalne bramy zapisane na ten sam temat wstrzykują te wiadomości do swojej lokalnej sieci mesh.

## Konfiguracja

### Włączanie MQTT

1. Przejdź do **Ustawienia → Ustawienia modułu → MQTT**.
2. Włącz moduł MQTT.
3. Skonfiguruj połączenie z brokerem:

| Ustawienie | Opis | Domyślnie |
|---------|-------------|---------|
| **Adres** | Nazwa hosta brokera MQTT | mqtt.meshtastic.org |
| **Nazwa użytkownika** | Uwierzytelnianie w brokerze | meshdev |
| **Hasło** | Uwierzytelnianie w brokerze | large4cats |
| **Główny temat** (Root topic) | Podstawowy temat wiadomości | `msh`, który radio zamienia na `msh/<REGION>` po ustawieniu regionu |
| **Szyfrowanie włączone** (Encryption enabled) | Szyfruj dane MQTT | Włączone |
| **Włącz wyjście JSON** | Publikuj i odbieraj także temat `/2/json/`. Oznaczone jako przestarzałe w schemacie protobuf, ale nadal jedyny przełącznik tego zachowania — i własne proxy aplikacji go respektuje | Wyłączone |
| **Włącz TLS** | Bezpieczne połączenie z brokerem | Wyłączone |
| **Raportowanie mapy** (Map reporting) | Raportuj pozycję na publiczną mapę | Wyłączone |
| **Proxy do klienta włączone** | Przekazuj MQTT przez połączoną aplikację | Wyłączone |

### Stan połączenia i test połączenia

Góra ekranu ustawień MQTT pokazuje stan przekaźnika, który uruchamia ta aplikacja:
**Połączony**, **Łączenie…**, **Ponowne łączenie…**, **Rozłączono** lub **Nieaktywny**. Pokazuje
**Nieaktywny** zawsze, gdy aplikacja niczego nie przekazuje, co obejmuje normalny przypadek radia
łączącego się z brokerem przez własne Wi-Fi lub Ethernet. Własne połączenie radia z brokerem
nie jest tu raportowane.

**Testuj połączenie** sprawdza broker, zanim zapiszesz ustawienia w radiu, i rozróżnia rodzaje
niepowodzeń: nierozpoznana nazwa hosta, odrzucone połączenie TCP, błąd TLS, przekroczenie czasu
oczekiwania albo odrzucenie Twoich danych logowania przez broker wraz z podaniem powodu.

### Proxy MQTT w tej aplikacji

Jeśli radio nie ma własnego dostępu do internetu, może użyć aplikacji jako bramy MQTT: włącz **MQTT** i **Proxy do klienta włączone** w konfiguracji modułu, a aplikacja będzie przekazywać ruch MQTT między radiem a brokerem przez internet Twojego telefonu lub komputera.

Przełącznik **Proxy MQTT w tej aplikacji** na górze ekranu ustawień MQTT pokazuje, czy to przekazywanie działa, i pozwala je natychmiast odciąć (lub uruchomić ponownie), bez edytowania i ponownego zapisywania konfiguracji MQTT radia.

### Domyślny broker Meshtastic

Społeczność utrzymuje publiczny broker pod adresem `mqtt.meshtastic.org`. Jest przeznaczony do ogólnego użytku i testów.

Gdy ta aplikacja przekazuje MQTT dla radia, połączenia z tym brokerem zawsze używają TLS na porcie 8883, nawet jeśli **Włącz TLS** jest wyłączone. To podniesienie zabezpieczeń robi sama aplikacja i przełącznik o tym informuje. Radio łączące się z brokerem przez własne Wi-Fi lub Ethernet niczego nie wymusza: używa **Włącz TLS** tak, jak zapisano, więc włącz je sam, bo inaczej radio połączy się jawnie na porcie 1883. Dla każdego innego brokera decyduje przełącznik w obu przypadkach (port 8883 z TLS, 1883 bez).

> 🔒 **Prywatność:** Wiadomości na publicznym brokerze może odczytać każdy, kto je subskrybuje. Do prywatnej komunikacji zawsze używaj szyfrowania kanału.

### Prywatny broker

Dla lepszej prywatności i kontroli możesz uruchomić własny broker MQTT:
- Mosquitto (lekki, open source)
- HiveMQ
- EMQX

Skonfiguruj węzeł tak, aby łączył się z Twoim prywatnym brokerem z odpowiednimi danymi logowania.

## Raportowanie na mapę

Gdy **Raportowanie mapy** jest włączone, Twój węzeł okresowo publikuje w brokerze raport dla mapy. Raport jest wysyłany bez szyfrowania, niezależnie od kluczy Twoich kanałów, i zawiera identyfikator węzła, długą i krótką nazwę, przybliżoną lokalizację, model sprzętu, rolę, wersję firmware, region LoRa, preset modemu i nazwę kanału głównego.

Włączenie otwiera kartę zgody. Włącz **Zgadzam się.** i wybierz **Interwał raportowania map** wynoszący godzinę lub więcej — ekran nie zapisze się, dopóki tego nie zrobisz. Suwak ustawia dokładność pozycji, a aplikacja pokazuje wynikową dokładność jako odległość ±, dzięki czemu możesz opublikować lokalizację przybliżoną zamiast dokładnej.

Raporty pojawiają się na [meshmap.net](https://meshmap.net) i podobnych społecznościowych serwisach map.

> 🔒 **Prywatność:** Raport dla mapy może odczytać każdy, kto subskrybuje broker. Jeśli nie chcesz, aby Twoja przybliżona lokalizacja była publikowana, zostaw **Raportowanie mapy** wyłączone.

## Uplink a downlink

| Kierunek | Opis |
|-----------|-------------|
| **Uplink** | Wiadomości z sieci mesh → broker MQTT |
| **Downlink** | Wiadomości z brokera MQTT → sieć mesh |

Uplink i downlink to ustawienia poszczególnych kanałów, a nie ustawienia modułu MQTT. Otwórz **Ustawienia → Kanały**, dotknij kanału i użyj **Wysył włączony** (MQTT Uplink Enabled) oraz **Odbiór włączony** (MQTT Downlink Enabled). Każdy kanał, który chcesz wysyłać na zewnątrz, musi mieć włączony uplink, a każdy kanał, do którego chcesz wstrzykiwać ruch MQTT, musi mieć włączony downlink.

## Formaty wiadomości

MQTT przenosi dwa formaty danych:

| Format | Opis | Zastosowanie |
|--------|-------------|----------|
| **Protobuf** | Binarne kodowanie protobuf Meshtastic | Mostkowanie sieci mesh między węzłami |
| **JSON** | Czytelny dla człowieka JSON w temacie `/2/json/` | Odbiorcy spoza sieci mesh (pulpity, automatyka domowa) |

> ℹ️ **Uwaga:** `json_enabled` jest oznaczone jako przestarzałe w schemacie protobuf, ale nie zostało
> zastąpione i nie jest ignorowane. Gdy jest włączone, własne proxy MQTT aplikacji subskrybuje
> temat `/2/json/` i dekoduje te dane.

## Szyfrowanie i prywatność

Zrozumienie warstwowego modelu szyfrowania:

1. **Szyfrowanie kanału** odbywa się w sieci mesh *przed* MQTT. Jeśli kanał ma PSK, dane MQTT są już zaszyfrowane — broker i wszyscy subskrybenci widzą tylko szyfrogram.
2. **Szyfrowanie włączone** (ustawienie modułu) decyduje, którą kopię pakietu publikuje brama — to nie jest dodatkowa warstwa. Zostaw je włączone, a broker dostanie pakiet nadal zaszyfrowany kluczem Twojego kanału. Wyłącz je, a brama opublikuje pakiet odszyfrowany, więc każdy subskrybujący temat przeczyta Twoje wiadomości jawnie. Wyłączaj je tylko wtedy, gdy broker jest Twój i chcesz mieć jawne dane np. dla pulpitu.
3. **TLS** szyfruje samo połączenie TCP z brokerem, chroniąc przed podsłuchem na poziomie sieci.

> 🔒 **Bezpieczeństwo:** Domyślny kanał publiczny ma powszechnie znany klucz. Wiadomości z kanału domyślnego wysyłane przez MQTT są w praktyce **niezaszyfrowane** — każdy może je odczytać. Do prywatnej komunikacji zawsze używaj własnego PSK.

## Dobre praktyki

- Używaj szyfrowania na poziomie kanału (PSK) na kanałach mostkowanych do MQTT
- Nie włączaj MQTT na węzłach bez dostępu do internetu (radio buforuje wiadomości, których nie może wysłać, i marnuje pamięć)
- Do wrażliwych wdrożeń używaj prywatnego brokera
- Pamiętaj o czasie antenowym przy downlinku z ruchliwych tematów MQTT — każda wiadomość odebrana z downlinku zużywa czas antenowy w Twojej lokalnej sieci mesh
- Rozważ włączenie samego uplinku, jeśli chcesz tylko zdalnie monitorować sieć, bez wstrzykiwania wiadomości z powrotem

## Rozwiązywanie problemów

### MQTT się nie łączy

- **Sprawdź Wi-Fi** — węzeł-brama musi mieć aktywne połączenie z internetem (Wi-Fi lub Ethernet). MQTT nie działa przez samo łącze radiowe LoRa.
- **Zweryfikuj dane logowania** — przy błędnych danych większość brokerów zawodzi po cichu — sprawdź dokładnie, czy nie ma spacji na końcu.
- **Zapora sieciowa** — port 1883 (MQTT) lub 8883 (MQTT przez TLS) musi być osiągalny. Niektóre sieci przepuszczają tylko ruch webowy (porty 80 i 443).
- **Rozwiązywanie nazw DNS** — jeśli używasz własnej nazwy hosta brokera, sprawdź, czy węzeł potrafi ją rozwiązać. Spróbuj użyć bezpośrednio adresu IP brokera.

### Wiadomości nie są mostkowane

- **Sprawdź ustawienia uplink/downlink** — jeśli włączony jest tylko uplink, wiadomości płyną z sieci mesh do MQTT, ale nie z powrotem. Włącz downlink na bramie odbiorczej.
- **Niezgodność kanałów** — obie bramy muszą mieć ten sam kanał z tym samym PSK. Niezgodność oznacza, że wiadomości są szyfrowane różnymi kluczami i wyglądają jak śmieci.
- **Niezgodność tematów** — obie bramy muszą używać dokładnie tego samego głównego tematu. Ustawienie regionu zamienia domyślny temat główny na `msh/<REGION>` (na przykład `msh/US`), więc bramy w różnych regionach nie spotkają się, dopóki obu nie nadasz tego samego jawnego tematu głównego.
- **Włączone Ignoruj MQTT** — w regionie z limitem czasu nadawania (duty cycle) radio włącza **Ignoruj MQTT** (Ignore MQTT) (konfiguracja LoRa, **Zaawansowane**) po ustawieniu regionu i wtedy odrzuca każdy pakiet, który dotarł do niego przez MQTT. Wyłącz to na węzłach odbierających, nie tylko na bramie.
- **Wyłączone Ok dla MQTT** — na publicznym brokerze brama wysyła pakiety innych węzłów tylko wtedy, gdy węzeł nadający ma włączone **Ok dla MQTT** (konfiguracja LoRa, **Zaawansowane**). Twój własny ruch jest mostkowany tak czy inaczej; ruch sąsiadów nie, dopóki się nie zgodzą.

## Powiązane tematy

- [Ustawienia — Moduły i administracja](settings-module-admin) — opis konfiguracji modułu MQTT
- [Wiadomości i kanały](messages-and-channels) — szyfrowanie kanału i ustawianie PSK
- [Poradnik integracji MQTT](https://meshtastic.org/docs/software/integrations/mqtt) — szczegółowa dokumentacja MQTT na meshtastic.org
