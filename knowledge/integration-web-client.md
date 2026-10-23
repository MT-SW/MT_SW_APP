# Klient webowy MT_SW / MT_SW web client

## 🇵🇱 Polski

Folder `static/` to zbudowany klient [meshtastic/web](https://github.com/meshtastic/web)
(monorepo, `apps/web`) z commita `1c66c50` (20.09.2026), dostosowany do MT_SW.
Zmiany w źródłach są w pliku `mtsw-web-client.patch`:

- ścieżka bazowa `/meshtastic/web/` (Vite `base`) oraz router na adresach z `#`,
  bo Home Assistant serwuje klienta jako pliki statyczne,
- logo, ekran błędu, grafiki urządzeń i tłumaczenia wczytywane spod ścieżki bazowej,
- `@meshtastic/core` i `@meshtastic/protobufs` bez rejestru JSR (alias na
  `@meshtastic/sdk` i pakiet z workspace), żeby dało się zbudować bez dostępu do JSR,
- kolorystyka MT_SW (złoto #F5C839, granat #2C2D3C) dopisana na końcu `src/index.css`,
- logo i ikony MT_SW w `public/`, nazwa „MT_SW” w tłumaczeniach, pełne
  polskie tłumaczenie (`public/i18n/locales/pl-PL/`).

Mapa: w komponencie mapy styl `mapStyle` jest wpisany w kodzie (standardowa mapa OpenStreetMap,
kafle `tile.openstreetmap.org`, bez klucza), a kafle są pobierane z `referrerPolicy: "origin"`
(`transformRequest`) — tak samo jak w mapie panelu. Zmiana jest w gotowym
`static/index-*.js`; po ponownym zbudowaniu klienta trzeba ją powtórzyć w źródłach.

Budowanie: `pnpm install` (bez lockfile, `minimumReleaseAge: 0`), potem w `apps/web`
`vite build`; zawartość `apps/web/dist/` kopiuje się do `static/`.

Integracja wysyła pliki klienta z nagłówkami COOP/COEP (izolacja cross-origin),
bo klient trzyma bazę w SQLite na OPFS. Bez izolacji działa dalej, tylko bez
trwałej bazy.

## 🇬🇧 English

`static/` is a build of [meshtastic/web](https://github.com/meshtastic/web)
(monorepo, `apps/web`) at commit `1c66c50` (2026-09-20), adapted for MT_SW.
Source changes are in `mtsw-web-client.patch`:

- base path `/meshtastic/web/` (Vite `base`) and a hash-based router, because
  Home Assistant serves the client as static files,
- logo, error screen, device images and translations loaded from the base path,
- `@meshtastic/core` and `@meshtastic/protobufs` without the JSR registry (aliased
  to `@meshtastic/sdk` and the workspace package), so it builds without JSR access,
- MT_SW colours (gold #F5C839, navy #2C2D3C) appended to `src/index.css`,
- MT_SW logo and icons in `public/`, "MT_SW" in the translations, and a full
  Polish translation (`public/i18n/locales/pl-PL/`).

Map: in the map component the `mapStyle` is inlined (standard OpenStreetMap map, tiles from
`tile.openstreetmap.org`, no key) and tiles are fetched with `referrerPolicy: "origin"`
(`transformRequest`) — same as the panel map. The change is in the built
`static/index-*.js`; after rebuilding the client it has to be repeated in the sources.

Build: `pnpm install` (no lockfile, `minimumReleaseAge: 0`), then `vite build` in
`apps/web`; copy `apps/web/dist/` into `static/`.

The integration serves the client files with COOP/COEP headers (cross-origin
isolation) because the client keeps its database in SQLite on OPFS. Without
isolation it still works, just without a persistent database.
