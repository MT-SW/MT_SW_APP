# Baza wiedzy asystenta MT_SW / Assistant knowledge base

## Po polsku

Ten katalog to **baza wiedzy offline wbudowanego asystenta** aplikacji MT_SW. Asystent korzysta z tych plików, żeby odpowiadać na pytania o sieć, radio, sprzęt i pokrewne tematy bez internetu. To jedyny powód, dla którego baza powstała: nie jest to dokumentacja do czytania od deski do deski, tylko materiał, z którego asystent wyszukuje pasujące fragmenty i odpowiada własnymi słowami, podając źródło.

Układ:

- `base/` — temat główny: nasza sieć, Meshtastic, urządzenia, anteny, LoRa, zasilanie i solary, firmware.
- `base/pokrewne/` — tematy bliskie: informatyka, elektronika, Wi-Fi, Bluetooth, krótkofalarstwo, sieć elektryczna, zakupy z zagranicy oraz **alternatywne sieci** (MeshCore, Reticulum, APRS, LoRaWAN) podane wyłącznie informacyjnie.
- `base/offtop/` — tematy dalsze: zdrowie, pierwsza pomoc, turystyka, życie codzienne, dom, samochód.

Zasady pisania wpisów:

- Wpis zaczyna się od `### pytanie`, potem treść, linia `Słowa:` (ukryte słowa kluczowe) i linia `Źródło: adres (sprawdzono RRRR-MM)`.
- Wpis ma do ok. 1750 znaków. Treść jest własnymi słowami, nie kopią artykułu.
- Czego źródło nie podaje, wpis mówi wprost; wnioski z ogólnej wiedzy są oznaczone.
- Plik `README.md` leży poza `base/`, więc asystent go nie wczytuje.

Przy budowaniu aplikacji pliki z `base/`, `base/pokrewne/` i `base/offtop/` są kopiowane do zasobów razem ze spisem (`index.txt`).

## In English

This folder is the **offline knowledge base of the built-in assistant** in the MT_SW app. The assistant uses these files to answer questions about the network, radio, hardware and related topics without internet. It exists mainly to **help the assistant do its job**: it lets it answer factually with a source instead of guessing. It is not documentation to read cover to cover, but material the assistant searches for matching fragments and answers from in its own words.

Layout: `base/` (main topic: our network and Meshtastic), `base/pokrewne/` (related: IT, electronics, Wi-Fi, ham radio, mains power, imports, and alternative networks such as MeshCore and Reticulum, given for information only), `base/offtop/` (further afield: health, first aid, hiking, daily life).

Entry format: `### question`, text, a hidden `Słowa:` keyword line and `Źródło: URL (checked YYYY-MM)`, up to about 1750 characters, written in own words; gaps in the source are stated, and general-knowledge parts are marked.
