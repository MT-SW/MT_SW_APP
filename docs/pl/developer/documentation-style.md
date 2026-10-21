---
title: Styl dokumentacji
parent: Przewodnik dewelopera
nav_order: 11
last_updated: 2026-10-21
description: Jak działa dokumentacja tego repozytorium oraz jedna zasada prozy, którą standardy projektowe Meshtastic pozostawiają nam.
aliases:
  - style
  - style-guide
  - docs-style
  - writing
---

# Styl dokumentacji

Sekcja 11 [Meshtastic Client Design Standards](https://github.com/meshtastic/design/tree/master/standards) jest przewodnikiem stylu dla wszystkiego w `docs/en/`. Przeczytaj ją najpierw. Rozstrzyga ona o głosie, prostym języku, terminologii, strukturze stron, instrukcjach, pokryciu wielu platform, przykładach kodu i CLI, mediach, dostępności, użyciu ramek ostrzeżeń, jednostkach, tłumaczeniu, wersjonowaniu i tekstach w produkcie, a kończy się listą kontrolną reguł, które recenzent może sprawdzić bez oceny subiektywnej.

Ta strona zawiera to, czego Sekcja 11 nie obejmuje: jak dokumentacja działa w tym repozytorium, dwie formy, które tu się różnią, oraz jedną zasadę prozy, którą standardy pozostawiają otwartą. Gdy obie kiedykolwiek się rozchodzą w sprawie, która tu nie jest wymieniona, wygrywa Sekcja 11, a ta strona jest błędna.

Strony w `docs/en/` trafiają w trzy miejsca — do przeglądarki dokumentacji w aplikacji, na witrynę GitHub Pages i na meshtastic.org — więc jedna strona musi dobrze się czytać we wszystkich, a jej angielski źródłowy tekst jest bazą tłumaczeń na ponad 40 języków w Crowdin. Sekcja 11.1 opisuje synchronizację: strony użytkownika i dewelopera trafiają co tydzień do `docs/software/android/` w witrynie dokumentacji, pobrane z najnowszego wydania. Strażnik CI w tamtym repozytorium odrzuca pull requesty, które dotykają tych ścieżek, a następna synchronizacja wycofuje każdą zmianę, która mimo to przejdzie.

Reguły zachowują dotychczasowe identyfikatory, aby uwagi z recenzji i ustalenia audytów nadal mogły się do nich odwoływać. Reguły, które Sekcja 11 obejmuje teraz, są wycofane, a ich identyfikatory nie są używane ponownie.

## W okresie przejściowym

Dwie reguły zmieniły się, gdy pojawiła się Sekcja 11. Żadna nie wymaga masowej zmiany.

- **Nagłówki piszemy z wielkiej litery tylko na początku (sentence case)** (11.5, i szybka kontrola w 11.16). Istniejące nagłówki w Title Case zostają, dopóki strona nie będzie edytowana z innego powodu. Zmiana wielkości liter w nagłówku unieważnia pamięć tłumaczeń w ponad 40 językach, więc dzieje się to przy okazji edycji stron, a nigdy jako osobny przebieg. Wartości `title` we frontmatter to etykiety nawigacji współdzielone przez przeglądarkę w aplikacji i witrynę, więc zmieniają się razem z resztą nawigacji, a nie pojedynczo dla każdej strony.
- **Sprzęt to węzeł** (11.4). To repozytorium używało słowa *radio*, aby odróżnić sprzęt od telefonu, i pozostało wiele jego użyć. W nowych tekstach preferuj *węzeł* (node), a *radio* usuwaj przy edycji strony. *Telefon* (phone) nadal oznacza uchwyt z Androidem, na którym działa aplikacja, na co Sekcja 11 nie ma słowa.

## Mechanika repozytorium

- **STRUCT-1** — Kompletny frontmatter na każdej stronie, użytkownika *i* dewelopera: `title`, `parent`, `nav_order`, `last_updated` (aktualizuj przy każdej zmianie treści; CI zgłasza stronę starszą niż 180 dni jako ostrzeżenie, które nigdy nie psuje budowania), `description` (jedno zdanie, ok. 160 znaków, używane przez wyszukiwanie i podglądy linków) oraz `aliases` (frazy wyszukiwania, które przeglądarka w aplikacji rozwiązuje).
- **HEAD-2** — Jeden `#` H1 na stronę, dokładnie zgodny z `title` we frontmatter. Indeks w aplikacji jest budowany z frontmatter, więc niezgodność pokazuje czytelnikowi dwa różne tytuły jednej strony.
- **STRUCT-3** — Strony użytkownika kończą się sekcją `## Related Topics`: lista linków, każdy z członem po półpauzie wyjaśniającym, po co tam przejść. Nagłówek jest stałą nazwą sekcji współdzieloną przez każdą stronę użytkownika, więc podobnie jak `title` we frontmatter pozostaje w Title Case, dopóki wszystkie nie zmienią się razem.
- **STRUCT-4** — Żadnych poziomych linii (`---`) w treści stron, a zwłaszcza na końcu pliku — układ witryny dodaje własną linię stopki, więc końcowe `---` renderuje się jako podwójna linia.
- **STRUCT-5** — Nowe strony muszą być zarejestrowane w `DocBundleLoader.kt` (indeks w aplikacji) — CI zawodzi w obu kierunkach, jeśli strona i indeks się nie zgadzają.
- **STRUCT-6** — Istotne zmiany stron dostają wpis *What's New* na górze `user.md` lub `developer.md`, w formacie określonym przez komentarz HTML w tych plikach.
- **IMG-1** — Odwołuj się do zrzutów ekranu ścieżką względną od `docs/assets/screenshots/`: `../../assets/screenshots/<page>_<subject>.png`. Zarówno synchronizacja Docusaurus, jak i renderer w aplikacji kotwiczą się na segmencie `assets/`, więc ta jedna forma działa we wszystkich trzech odbiorcach.
- **LINK-2** — Strony siostrzane linkujemy samym slugiem (`connections`), między sekcjami ścieżką względną (`../developer/testing`). `scripts/validate-doc-links.js` wymusza rozwiązywalność.
- **LINK-3** — Pojęcia po stronie węzła (presety LoRa, regiony firmware, tematy MQTT) linkujemy do [dokumentacji meshtastic.org](https://meshtastic.org/docs/), zamiast tłumaczyć je tutaj ponownie. Jedno źródło rozjeżdża się rzadziej niż dwa.
- **CODE-1** — Czcionka kodu dla ścieżek modułów (`core:ble`), zadań Gradle, nazw klas i funkcji oraz kluczy ustawień, obok przypadków, które obejmuje 11.8.
- **DEV-1** — Strony dla deweloperów zakładają biegłość w Kotlinie, Gradle i Androidzie i pomijają uspokajanie, ale każda reguła z tej strony i z Sekcji 11 nadal obowiązuje. Zwięźle to dobrze; zagadkowo to źle.
- **DEV-3** — Zmiany strukturalne lub proceduralne dostają wpis *What's New for Developers* w `developer.md`.

## Formy lokalne

Dwie formy różnią się od Sekcji 11. Pierwsza wynika z wymagań renderera. Druga to utrwalona forma domowa, zachowana, ponieważ jej zmiana dotknęłaby każdej strony bez korzyści dla czytelnika.

- **ADMON-1** — Ramki ostrzeżeń to blockquote zaczynający się emoji i pogrubioną etykietą, z tego zamkniętego zestawu. Renderer w aplikacji nie ma komponentu ramki, więc `:::note` dotarłoby do czytelnika jako dosłowny tekst. Sekcja 11.11 zwalnia synchronizowaną dokumentację klienta właśnie z tego powodu i pozostawia formę temu przewodnikowi; jej reguły dotyczące tego, *kiedy* użyć ramki, nadal obowiązują w pełni, w tym najwyżej jedna na sekcję H2.

  | Ramka | Do czego |
  |---|---|
  | `> 💡 **Tip:**` | Opcjonalny skrót lub nieoczywista alternatywa. Czytelnik nic nie traci, pomijając ją. |
  | `> ℹ️ **Note:**` | Informacja uzupełniająca, warta poznania, ale niewymagana do zadania. |
  | `> ⚠️ **Important:**` | Informacja niezbędna do poprawnego wykonania zadania, bez ryzyka utraty danych. |
  | `> ⚠️ **Warning:**` | Ryzyko utraty danych, zablokowania dostępu lub uszkodzenia sprzętu. Musi stać *przed* czynnością, której dotyczy. |
  | `> 🔒 **Privacy:**` | Jakie dane opuszczają telefon, kto w sieci mesh może je zobaczyć i jak to ograniczyć. |
  | `> 🔒 **Security:**` | Zastrzeżenia kryptograficzne — obsługa kluczy, nieszyfrowane kanały, dostęp administracyjny. |

  Privacy i Security to etykiety domowe bez odpowiednika w źródle nadrzędnym. Istnieją, ponieważ konstytucja aplikacji czyni prywatność główną zasadą i nie należy ich spłaszczać do Note.

- **PROC-2** — Ścieżki menu zapisujemy ze spacjowaną strzałką między pogrubionymi etykietami: **Settings → Permissions**. Nigdy `>` ani `›`. Sekcja 11.6 zapisuje separator w przykładzie jako `>`; strzałka jest ustaloną formą tego zbioru, jednoznaczną w Markdown i naturalnie wyglądającą w rendererze w aplikacji.

## Zasady prozy, które Sekcja 11 pozostawia otwarte

Jedna zasada. [design#154](https://github.com/meshtastic/design/pull/154) przeniósł resztę do Sekcji 11, gdzie brzmią tak samo, więc nic w sposobie pisania się nie zmienia — reguły mają jeden dom zamiast dwóch, które mogłyby się rozjechać.

- **LANG-7** — Półpauzy otaczamy spacjami — tak jak tu — zgodnie z całym dotychczasowym zbiorem. Sekcja 11 ich nie wspomina.

Te są wycofane, wraz z sekcją, która każdą z nich obejmuje. Ich identyfikatory nie są używane ponownie.

| Wycofana reguła | Sekcja |
|---|---|
| VOICE-2 — skróty (contractions) i *do not* w ostrzeżeniach | 11.2 |
| LANG-3 — słowa określające wymagania | 11.3 |
| LANG-4 — *e.g.* i *i.e.* | 11.3 |
| LANG-6 — język inkluzywny, dosłowny | 11.3 |
| LANG-7 — część o przecinku oksfordzkim; półpauza zostaje | 11.3 |
| LANG-1 — pisownia amerykańska | 11.4 |
| LANG-9 — *above* i *below* | 11.5 |
| LANG-8 — styl liczb | 11.12 |

## Lista słów

Terminy potrzebne temu repozytorium, których tabele Sekcji 11 nie obejmują. Resztę rozstrzyga Sekcja 11.4.

| Termin | Reguła |
|---|---|
| email | Bez myślnika. |
| internet | Małą literą. |
| phone | Uchwyt z Androidem, na którym działa aplikacja. Używaj *phone* nawet gdy działa też tablet, chyba że rozróżnienie ma znaczenie. |
| set up / setup | Czasownik w dwóch słowach, rzeczownik/przymiotnik w jednym. |
| sign in | Czasownik; *sign-in* jako przymiotnik. Nie *log in* ani *login*. |
| tap | Czasownik interakcji dla dotykowego UI aplikacji. Te strony są mobilne, więc *tap* stoi tam, gdzie 11.7 preferowałoby *select*. Nigdy *tap on*. |
| touch & hold | Dokładnie ta forma (konwencja Google dla Androida). Nie *long press*, nie *tap and hold*. |

## Lista kontrolna nowej strony

1. Utwórz `docs/en/user/<slug>.md` lub `docs/en/developer/<slug>.md` z kompletnym frontmatter (STRUCT-1).
2. Zarejestruj stronę w `feature/docs/.../data/DocBundleLoader.kt` ze słowami kluczowymi i aliasami.
3. Umieść zrzuty ekranu w `docs/assets/screenshots/`, z odwołaniami zgodnie z IMG-1.
4. Dodaj wpis *What's New* (STRUCT-6).
5. Zwaliduj lokalnie: `node scripts/validate-doc-links.js docs/en`, `node scripts/check-doc-coverage.js .`, `node scripts/check-doc-aliases.js .` (każdy alias z frontmatter z kroku 1 musi też być we wpisie loadera z kroku 2 — tylko lista loadera trafia do wyszukiwania w aplikacji) oraz sprawdzenia Gradle paczki dokumentacji w [Współtworzenie](contributing).
6. Przed otwarciem pull requesta wykonaj na stronie szybkie kontrole z Sekcji 11.16.

## Related Topics

- [Współtworzenie](contributing) — nazewnictwo gałęzi, przebieg PR i bramki weryfikacyjne, przez które przechodzą zmiany dokumentacji
- [Kompilacje testowe i Obtainium](test-builds) — gdzie publikowane są migawki dokumentacji przedwydaniowej
