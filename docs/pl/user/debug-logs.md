---
title: Dzienniki debugowania
parent: Przewodnik użytkownika
nav_order: 22
last_updated: 2026-10-21
description: Przeglądaj i eksportuj własne dzienniki debugowania aplikacji z wnętrza aplikacji oraz dołącz zrzut do zgłoszenia na GitHubie, aby pomóc w diagnozie błędów — bez adb.
aliases:
  - debug-logs
  - logcat
  - app-logs
  - bug-report
---

# Dzienniki debugowania

Gdy coś działa nieprawidłowo, dzienniki debugowania aplikacji to najbardziej przydatna rzecz, jaką możesz dołączyć do zgłoszenia błędu. MT_SW_APP potrafi je zebrać **za Ciebie, z wnętrza aplikacji** — nie potrzebujesz `adb` ani żadnych narzędzi na komputerze, aby je zebrać.

Otwórz **Panel debugowania** w **Ustawienia → Zaawansowane → Panel debugowania**.

Jeśli zgłaszasz problem, wyeksportuj dzienniki (zobacz [Eksportowanie](#exporting)) i dołącz plik `.txt` do zgłoszenia w [systemie zgłoszeń Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android/issues). Zrzut dzienników obejmujący moment wystąpienia problemu zamienia „to nie działa” w coś, co programista faktycznie może wyśledzić.

## Dwie zakładki

Panel debugowania ma trzy zakładki (trzecia, **Logi urządzenia**, to dodatek MT_SW):

- **Pakiety** — zdekodowany ruch sieci mesh, który Twój węzeł wysłał i odebrał (wiadomości na poziomie protokołu). Przydatne do diagnozowania zachowania sieci mesh i routingu.
- **Dzienniki aplikacji** — własny dziennik diagnostyczny aplikacji (Android *logcat*), w tym ostrzeżenia, błędy i ślady stosu z samej aplikacji. Zwykle to właśnie tego potrzebuje zgłoszenie błędu.
- **Logi urządzenia** — logi wysyłane na żywo przez samo radio (te same, co na jego porcie szeregowym), z wyszukiwaniem, kolorami poziomów i eksportem. Najpierw włącz „Logi debugowania” w ustawieniach zabezpieczeń urządzenia. Przez Bluetooth działa z każdym firmware, a przez Wi-Fi i USB tylko z firmware MT_SW.

Każda zakładka ma własny przycisk **eksportu** i tworzy własny plik, więc możesz zabrać ten, który jest istotny — albo oba.

## Przeglądanie pakietów

Zakładka **Pakiety** pokazuje listę zapisanych pakietów; dotknij jednego, aby go rozwinąć — razem ze zdekodowaną zawartością, jeśli jest.

- **Szukaj** — wpisz tekst w polu **Szukaj w logach…**, aby podświetlić trafienia, i przechodź między nimi strzałkami.
- **Filtry** — dodaj własne filtry tekstowe albo wybierz z **Gotowych filtrów**: identyfikator Twojego węzła, adres rozgłoszeniowy `!ffffffff`, `decoded`, dzisiejsza data i nazwa każdego portu. Przełącznik dopasowania wybiera, czy pakiet ma pasować do dowolnego filtra, czy do wszystkich naraz.
- **Ustawienia** — ikona koła zębatego pokazuje **Zapisuj logi sieci mesh**, który włącza lub wyłącza zapisywanie pakietów do dziennika, oraz **Czas przechowywania logów**, od jednej godziny do roku, albo **Nigdy nie usuwaj logów**.
- **Wyczyść logi** — ikona kosza usuwa wszystkie zapisane pakiety, po Twoim potwierdzeniu.

## Przeglądanie dzienników aplikacji

Zakładka **Dzienniki aplikacji** pokazuje najnowsze linie dziennika **tylko z tej aplikacji** — nigdy z innych aplikacji w telefonie.

- **Szukaj** — wpisz tekst w polu wyszukiwania, aby przefiltrować do pasujących linii.
- **Filtr poziomu** — chipy **V / D / I / W / E** przełączają linie Verbose, Debug, Info, Warn i Error. Dotknij poziomu, aby go ukryć; dotknij ponownie, aby go przywrócić. Linie Fatal są zawsze pokazywane.
- **Odśwież** — ikona odświeżania odczytuje najnowsze dzienniki jeszcze raz.

Linie błędów i ostrzeżeń są podkolorowane, aby problemy rzucały się w oczy.

## Eksportowanie

Dotknij ikony **pobierania**, aby zapisać bieżące dzienniki do pliku. Aplikacja najpierw pokazuje ostrzeżenie o zawartości pliku — potwierdź je, a następnie wybierz miejsce zapisu w systemowym selektorze plików. Plik ma w nazwie znacznik czasu (na przykład `meshtastic_logcat_20260701_143312.txt`), więc kolejne eksporty nigdy się nie nadpisują. To samo ostrzeżenie chroni eksport z zakładki **Pakiety**.

Dołącz ten plik do zgłoszenia na GitHubie.

> 🔒 **Prywatność:** Eksporty automatycznie **zacierają** klucze prywatne, klucze administratora, hasła sesji i PSK kanałów oraz pomijają surowe bajty pakietów. Cała reszta zostaje — plik może zawierać treść Twoich wiadomości, dokładne lokalizacje i szczegóły węzłów. Przeczytaj go przed publicznym udostępnieniem, a w razie wątpliwości udostępnij go prywatnie.

## Komputer

Aplikacja na komputer nie ma systemowego logcata, więc zakładka **Dzienniki aplikacji** pokazuje zamiast tego własne zebrane wyjście dziennika aplikacji. Wyszukiwanie, filtrowanie i eksport działają tak samo. Wydania produkcyjne zbierają linie Info, Warn i Error; linie Verbose i Debug pojawiają się tylko w wersjach deweloperskich.

## Powiązane tematy

- [Pomoc i dokumentacja w aplikacji](help-and-docs) — czytanie tej dokumentacji offline w aplikacji
- [Połączenia](connections) — jeśli problem polega na samym połączeniu się z węzłem
