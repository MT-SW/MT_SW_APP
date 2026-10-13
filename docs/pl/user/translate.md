---
title: Przetłumacz aplikację
parent: Przewodnik użytkownika
nav_order: 17
last_updated: 2026-09-11
description: Jak aplikacja i jej dokumentacja są tłumaczone przez Crowdin oraz wskazówki dla osób dodających tłumaczenia.
aliases:
  - translate
  - crowdin
  - localization
---

# Przetłumacz aplikację

Aplikacja i jej dokumentacja w aplikacji są tłumaczone na Crowdin — ta strona pokazuje, jak się przyłączyć. Aplikacja używa [Crowdin](https://crowdin.com/) do zarządzania tłumaczeniami społeczności, zarówno interfejsu użytkownika, jak i dokumentacji w aplikacji.

## Co jest tłumaczone

| Zasób | Lokalizacja źródła | Uwagi |
|---|---|---|
| Teksty interfejsu | `core/resources/src/commonMain/composeResources/values/strings.xml` | Przyciski, etykiety, komunikaty i cały tekst widoczny dla użytkownika |
| Strony Przewodnika użytkownika | `docs/en/user/*.md` | Dokumentacja w aplikacji, pokazywana w Pomoc i dokumentacja |
| Metadane Fastlane | `fastlane/metadata/android/en-US/` | Tytuł, opis i dzienniki zmian w Google Play |

> ℹ️ **Uwaga:** Strony Przewodnika programisty są tylko po angielsku. Dokumentacja skierowana do programistów nie jest tłumaczona.

## Jak pomóc

1. **Wejdź do projektu Crowdin.** Otwórz [projekt Meshtastic Android na Crowdin](https://crowdin.com/project/meshtastic-android) i zaloguj się lub załóż darmowe konto.
2. **Wybierz język.** Wybierz istniejący język albo poproś o nowy, zakładając [zgłoszenie na GitHubie](https://github.com/meshtastic/Meshtastic-Android/issues/new).
3. **Tłumacz teksty.** Crowdin pokazuje angielski oryginał po lewej, a Twoje tłumaczenie po prawej. Przetłumacz każdy tekst i zapisz.
4. **Sprawdź kontekst.** Wiele tekstów ma zrzuty ekranu lub komentarze z kontekstem — zajrzyj do nich, aby zrozumieć, gdzie tekst pojawia się w aplikacji. Zaplanowane zadanie pobiera zatwierdzone tłumaczenia z Crowdin i otwiera pull request; trafiają do aplikacji, gdy opiekun go zatwierdzi i wyjdzie nowa wersja.

> 💡 **Wskazówka:** Pisz krótko. Teksty interfejsu często trafiają na przyciski, chipy lub wąskie kolumny. Jeśli tłumaczenie jest znacznie dłuższe niż angielski oryginał, rozważ skrócenie tam, gdzie sens pozostaje jasny.

## Dodawanie nowego języka

Jeśli Twojego języka nie ma jeszcze na Crowdin:

1. Otwórz zgłoszenie na [GitHubie](https://github.com/meshtastic/Meshtastic-Android/issues/new) z prośbą o nowy język.
2. Opiekun doda język do Crowdin i skonfiguruje `crowdin.yml`.
3. Po dodaniu możesz od razu zacząć tłumaczyć.

## Jak zorganizowane są tłumaczenia

Aplikacja na Androida używa **zasobów Compose Multiplatform** dla wszystkich tekstów widocznych dla użytkownika:

```text
core/resources/src/commonMain/composeResources/
├── values/              ← English (default)
│   └── strings.xml
├── values-de/           ← German
│   └── strings.xml
├── values-fr/           ← French
│   └── strings.xml
└── ...
```

Dokumentacja w aplikacji ma podobną strukturę w katalogu `docs/`:

```text
docs/
├── en/user/             ← English source (default)
│   ├── onboarding.md
│   └── ...
├── fr-rFR/user/         ← French (France)
│   ├── onboarding.md
│   └── ...
├── de-rDE/user/         ← German (Germany)
│   └── ...
└── ...
```

Foldery lokalizacji dokumentacji używają kwalifikatorów lokalizacji Androida, w postaci `{lang}` lub `{lang}-r{REGION}` (na przykład `fr`, `fr-rFR`, `de-rDE`, `ja-rJP`). Foldery `values-*` dla tekstów aplikacji używają zamiast tego samych kodów języka (`values-fr`, `values-de`, `values-ja`), ponieważ `crowdin.yml` zapisuje teksty z `%two_letters_code%`, a strony dokumentacji z `%android_code%`. Te dwa zestawy nie pokrywają się jeden do jednego.

Aplikacja automatycznie wybiera właściwy język na podstawie ustawień **Język i region** w telefonie.

Strona pochodząca z Crowdin jest oznaczona **Tłumaczenie społeczności** pod tytułem w aplikacji. Jeśli strona nie ma jeszcze tłumaczenia z Crowdin dla Twojego języka, wersja Android z Google tłumaczy maszynowo angielski oryginał na bieżąco i oznacza go jako **Tłumaczenie automatyczne**; wersje F-Droid i na komputer pokazują stronę po angielsku. Twoje tłumaczenie z Crowdin zastępuje maszynowe, gdy tylko się pojawi.

## Wskazówki dla tłumaczy

- **Nie tłumacz** terminów technicznych, takich jak „LoRa", „MQTT", „BLE", „TAK", „SNR" czy „RSSI" — są uniwersalne.
- **Zachowaj znaczniki zastępcze.** Teksty takie jak `%1$s` lub `%d` są wypełniane w czasie działania. Nie usuwaj ich ani nie zmieniaj kolejności, chyba że wymaga tego gramatyka Twojego języka.
- **Dopasuj ton.** Aplikacja mówi przyjaźnie i bezpośrednio. Unikaj zbyt oficjalnego języka.
- **Przetestuj, jeśli możesz.** Zmień język telefonu i otwórz aplikację, aby zobaczyć, jak tłumaczenia wyglądają w kontekście.

## Pytania?

Jeśli masz pytania o kontekst konkretnego tekstu albo potrzebujesz pomocy na start, otwórz dyskusję na stronie [Meshtastic GitHub Discussions](https://github.com/orgs/meshtastic/discussions).

Dziękujemy za pomoc w poszerzaniu zasięgu aplikacji.

## Powiązane tematy

- [Jednostki i lokalizacja](units-and-locale) — jak aplikacja dobiera formaty liczb, dat i jednostek dla Twojego regionu
- [Pomoc i dokumentacja](help-and-docs) — przeglądarka dokumentacji w aplikacji, w której publikowane są te strony
- [Pierwsze kroki](onboarding) — miejsce, w którym nowy użytkownik pierwszy raz spotyka przetłumaczone teksty
