# Polityka prywatności / Privacy Policy

[Polski](#polski) · [English](#english)

---

# Polski

**MT_SW_APP** to zmodyfikowana wersja aplikacji Meshtastic® (nieoficjalny fork, niezwiązany z Meshtastic LLC). Aplikacja łączy się z twoim radiem LoRa (mesh) i nie wymaga konta.

## Czego aplikacja NIE robi

- Nie ma kont, logowania ani własnego serwera MT_SW.
- Nie zbiera statystyk użycia ani raportów o awariach i niczego takiego nie wysyła (w żadnej wersji, także tej w sklepach).
- Nie ma reklam i nie sprzedaje ani nie udostępnia danych.
- Asystent AI, tłumacz i dyktowanie działają w całości na twoim urządzeniu. Treść wiadomości, rozmowy z asystentem i nagrania głosu nie są wysyłane do żadnych usług AI.

## Jakie dane przetwarza i po co

Wszystko jest zapisane lokalnie na twoim urządzeniu (wiadomości, węzły, ustawienia, logi), dopóki sam tego nie usuniesz.

- **Wiadomości, pozycje i dane węzłów** — idą przez twoje radio do sieci mesh. Inni użytkownicy sieci mogą je odebrać. Jeśli radio ma włączone szyfrowanie kanału, treść jest szyfrowana kluczem kanału. Aplikacja nie ma wpływu na to, co zrobią z nimi odbiorcy.
- **Lokalizacja telefonu** — tylko jeśli ją włączysz: do pokazania twojej pozycji na mapie i (opcjonalnie) do wysłania jej do sieci mesh.
- **Bluetooth, Wi-Fi, sieć lokalna, USB** — do połączenia z radiem.
- **Aparat** — do skanowania kodów QR kanałów i do wybierania zdjęć. **Mikrofon** — tylko do dyktowania, na urządzeniu.
- **Powiadomienia** — o wiadomościach i stanie połączenia.

## Połączenia z internetem (tylko gdy z nich korzystasz)

Przy każdym takim połączeniu zewnętrzna usługa widzi twój adres IP:

- **Mapy** — kafelki i style mapy z serwisu OpenFreeMap / OpenStreetMap (tiles.openfreemap.org).
- **Pobieranie modeli AI** — z serwisu Hugging Face (huggingface.co), tylko gdy sam pobierzesz model.
- **Aktualizacja firmware** — z serwisu GitHub, tylko gdy ją uruchomisz.
- **Zdjęcia w czacie** — jeśli wybierzesz „Wyślij”, zdjęcie jest wysyłane na publiczny serwer zdjęć (catbox.moe, a gdy jest niedostępny: litterbox.catbox.moe lub uguu.se), niezwiązany z MT_SW. Przed wysłaniem pytamy o zgodę i opisujemy, dokąd trafia zdjęcie. Każdy, kto zna link, może je zobaczyć, a aplikacja nie może go usunąć.
- **MQTT** — jeśli skonfigurujesz własny serwer MQTT, dane idą na ten serwer.

## Usuwanie danych

Odinstalowanie aplikacji usuwa jej dane z urządzenia. W ustawieniach można też usunąć wiadomości, węzły, logi i pobrane modele. Dane wysłane do sieci mesh lub na zewnętrzne serwery zdjęć nie podlegają usunięciu przez aplikację.

## Dzieci

Aplikacja nie jest skierowana do dzieci.

## Kontakt

Pytania i zgłoszenia (także dotyczące treści od innych użytkowników): zakładka **Issues** w repozytorium https://github.com/MT-SW/MT_SW_APP. Nadawcę niechcianych wiadomości możesz zignorować w szczegółach węzła lub wyciszyć powiadomienia z jego wiadomości.

## Zmiany

Zmiany tej polityki będą w historii repozytorium. Data ostatniej zmiany: 2026-11-05.

---

# English

**MT_SW_APP** is a modified version of the Meshtastic® app (an unofficial fork, not affiliated with Meshtastic LLC). It connects to your LoRa (mesh) radio and needs no account.

## What the app does NOT do

- It has no accounts, no sign-in and no MT_SW server of its own.
- It collects no usage statistics or crash reports and sends nothing like that (in any build, including the store builds).
- It has no ads and does not sell or share data.
- The AI assistant, translator and dictation run entirely on your device. Message text, assistant conversations and voice recordings are not sent to any AI service.

## What data it handles and why

Everything is stored locally on your device (messages, nodes, settings, logs) until you delete it.

- **Messages, positions and node data** — go through your radio into the mesh network. Other users of the network can receive them. If the radio has channel encryption on, the content is encrypted with the channel key. The app has no control over what recipients do with them.
- **Phone location** — only if you turn it on: to show your position on the map and (optionally) to send it to the mesh.
- **Bluetooth, Wi-Fi, local network, USB** — to connect to the radio.
- **Camera** — to scan channel QR codes and to pick pictures. **Microphone** — only for dictation, on the device.
- **Notifications** — about messages and the connection state.

## Internet connections (only when you use them)

With each such connection the outside service sees your IP address:

- **Maps** — map tiles and styles from OpenFreeMap / OpenStreetMap (tiles.openfreemap.org).
- **AI model downloads** — from Hugging Face (huggingface.co), only when you download a model yourself.
- **Firmware update** — from GitHub, only when you start it.
- **Chat pictures** — if you choose "Send", the picture is uploaded to a public picture host (catbox.moe, or, when unavailable, litterbox.catbox.moe or uguu.se), which is not connected to MT_SW. We ask for your permission first and explain where the picture goes. Anyone with the link can see it, and the app cannot delete it.
- **MQTT** — if you configure your own MQTT server, data goes to that server.

## Deleting data

Uninstalling the app removes its data from the device. In the settings you can also delete messages, nodes, logs and downloaded models. Data sent into the mesh or to external picture hosts cannot be deleted by the app.

## Children

The app is not aimed at children.

## Contact

Questions and reports (including about content from other users): the **Issues** tab of https://github.com/MT-SW/MT_SW_APP. You can ignore a sender of unwanted messages in the node details, or mute notifications from their messages.

## Changes

Changes to this policy are kept in the repository history. Last changed: 2026-11-05.
