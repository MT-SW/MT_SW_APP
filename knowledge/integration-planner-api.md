# Meshtastic radio link planner - JS API

Port of the Kotlin Android planner calculation engine (ES modules, no dependencies, browser + Node 20+).
Import everything from `./index.js`. Units: metres, dB, dBm, MHz, kHz, degrees (WGS84) unless stated otherwise.
Everything is plain data (JSON-friendly); errors in results are codes, never texts.

## High-level API

### `planLink(input, {nodes?, signal?, computer?}) -> Promise<PlannerResults>`
Computes the A-B link (terrain from Mapterhorn, optional Open-Meteo weather, optional OSM clutter).
Does not throw (except `AbortError`); problems land in `result.error`.

`input` (all fields optional, normalised and clamped by `normalizePlannerInput`; see `createDefaultInput()`):
- `a`, `b` (end): `{lat, lon, name, pointSource:{type:'none'|'map'|'manual'|'station'|'node', num?}, groundAltM, groundAltManual, antennaHeightM (0..2000), txPowerDbm (-10..50), antennaGainDbi (-10..60), feederManualDb (0..100), feederPrecise, feederConfig}`.
- radio: `frequencyMHz, bandId, modemPreset (id|null), radioOverride, bandwidthKhz, spreadingFactor (5..12), noiseFigureDb`.
- environment: `useWeather, extraLossDb (0..100), clutterPreset, preciseTerrain (OSM clutter), clutterRadiusKm, forestHeightM, buildingHeightM`.
- coverage: `coverageSide 'A'|'B', coverageMaxRangeKm (1..300), coverageRadials (8..360), coverageOpacity, coverageRxHeightM, coverageRxGainDbi`.
- limits are in `PLANNER_LIMITS`.

`PlannerResults`: `{link, series, groundAltA, groundAltB, weather, kFactor, surfaceRefractivity, atmosphericLossDb, comparison, error, clutter, effectiveExtraLossDb}`
- `link` (null when not computable): `{distanceM, bearingAToBDeg, bearingBToADeg, elevationAAngleDeg, elevationBAngleDeg, freeSpaceLossDb, itmLossDb, totalPathLossDb, sensitivityDbm, aToB:{rxPowerDbm, marginDb, verdict}, bToA, itmMode, itmWarnings, lineOfSightClear, worstFresnelClearanceM, worstFresnelRatio, profile}`; `verdict` is a `LinkVerdict` (EXCELLENT >=20 dB, GOOD >=10, MARGINAL >=3, WEAK >=0, NO_LINK).
- `series`: per-sample arrays for the profile chart (ground, LOS, Fresnel, bulge).
- `weather`: `{kind:'idle'}` | `{kind:'ready', conditions}` | `{kind:'failed', error:'OFFLINE'|'BAD_RESPONSE'}`. Failure falls back to k=4/3, N0=301.
- `clutter`: `{kind:'idle'}` | `{kind:'ready', stats}` | `{kind:'failed', failure:'NETWORK'|'BAD_RESPONSE'|'TOO_LARGE', detail}` (failure -> the preset loss is used again). `detail` (additive, JSON-friendly, may be `{}`): `{status?, errorName?, errorMessage?, remark?, serverSide?, timedOut?, attempts:[{mirror, status?, errorName?, errorMessage?, remark?, timedOut?, viaProxy?, ms}], radiusKm?, requestedRadiusKm?, firstFailure?}`; the panel shows it under "Szczegóły techniczne" and `PlannerOverpass` also `console.warn`s it.
- `error`: `null` | `PlannerError` (`ELEVATION_OFFLINE`, `ELEVATION_DECODE`, `POINTS_TOO_CLOSE`, `COMPUTE_FAILED`).
- `comparison`: prediction vs last measurement of a mesh node (needs `nodes: [{num, displayName, rssiDbm, snrDb, hopsAway, isOurs}]`), else null.

### `planCoverage(input, {onProgress?, signal?, onClutterFailure?, computer?}) -> Promise<CoverageResult>`
Polar coverage prediction around `coverageSide`. Runs in a Web Worker (module worker `coverage-worker.js`, tiles/clutter structured-cloned), falls back to the main thread when Worker is unavailable or fails to start.
- `onProgress(fraction 0..1)`; `signal.abort()` rejects with `AbortError` and terminates the worker.
- Rejects with `PlannerElevationError` (terrain), or `Error{code: PlannerError.COVERAGE_NEEDS_POINT}`.
- OSM failure does not reject: `onClutterFailure(failure, detail)` is called and the run continues. When the area was too large for the server and was retried with half the radius, `onClutterRadius({requestedKm, usedKm, failure})` is called (the panel shows which radius was used).
- `CoverageResult`: `{center, radials, ringsM: number[], marginDb: number[radials][rings], maxReachM: number[radials], ...}`; margin -200 = no data/error.
- `marginAt(result, lat, lon) -> number|null`.

### Computer
`PlannerComputer({elevation, weather, clutter, clockMs, useWorker, workerUrl})` with `compute`, `computeCoverage`, `invalidateWeather`. `getSharedComputer()/setSharedComputer(c)` replace the instance used by `planLink/planCoverage` (tests: inject mocks).

## Defaults and state helpers
`PLANNER_DEFAULTS` (869.44165 MHz, Narrow Fast 62.5 kHz SF7, 50 km, 180 radials), `PLANNER_LIMITS`, `END_DEFAULTS`, `createDefaultInput()`, `createDefaultEnd()`, `normalizePlannerInput(input)`, `DEFAULT_K`, `DEFAULT_N0`, `noiseFloorDbm(bwKhz, nf)`, `dbmToWatts`, `wattsToDbm`.
Pure state transitions (return a new state): `applyBand(state, bandId)`, `applyModemPreset`, `applyFrequency(state, MHz)`, `applyBandwidth` / `applySpreadingFactor` (set `radioOverride`), `applyClutterPreset`, `applyExtraLoss`. `CLUTTER_PRESETS` (NONE 0, RURAL_OPEN 0, FOREST_LIGHT 3, FOREST_DENSE 10, SUBURBAN 6, URBAN 12, CUSTOM), `clutterPresetExtraDb(id)`.

## Lists
- `PLANNER_BANDS` (8 bands, default `DEFAULT_BAND_ID` 868), `MODEM_PRESETS` (17), `DEFAULT_PRESET_ID` ('LONG_FAST'; the planner default is NARROW_FAST), `bandById`, `bandIdFor(MHz)`, `regionFor`, `defaultFrequencyMHz(band, preset)`, `bandwidthScale`, `clampMHz`, `radioFromPreset`, `FREE_ID/FREE_MIN_MHZ/FREE_MAX_MHZ`, `NARROW_868_MHZ`.
- `CABLES` (22), `CONNECTORS` (11), `cableById`, `connectorById`, `cableLossDbPerM(cable, MHz)`, `connectorLossDb(connector, MHz)`.
- `FEEDER_PRESETS`, `computeFeeder(config, MHz) -> {totalDb, warnings[codes], ...}`, `resizeFeeder`, `MAX_CONNECTORS`, `FEEDER_CUSTOM_ID`, `FEEDER_NONE_ID`.

## Quality scale / legend / raster
- `COVERAGE_MAX_DB` (30), `COVERAGE_CLASS_COUNT` (24), `COVERAGE_FILL_OPACITY` (0.62).
- `coverageClassOf(marginDb) -> -1..23`, `coverageColorOf(marginDb) -> '#rrggbb'` (0 dB '#7a3cb5' ... 30 dB '#e9f03b'), `coverageRgbOf`, `coverageQuality(marginDb) -> 'NONE'|'WEAK'|'FAIR'|'GOOD'|'EXCELLENT'`.
- `coverageLegend() -> [{fromDb, toDb, color}]` (24 items), `coverageLegendGradientCss()`.
- `coverageRaster(result, {gridCells=256, opacity, projection:'mercator'|'equirectangular'}) -> {width, height, rgba:Uint8ClampedArray, classes:Int8Array, bounds:{south,west,north,east}}|null`; pixel (0,0) is north-west. Use as an image overlay (`new ImageData(rgba, width, height)` -> canvas -> dataURL) with `bounds`.
- `renderCoverageGeoJson(result, name) -> string` (FeatureCollection of class polygons + marker).

## Low-level modules
- Geodesy: `distanceM(a,b)`, `bearingDeg(a,b)`, `destination(p, bearingDeg, distM)`, `interpolate(a,b,fraction)`, `elevationAngleDeg`, `EARTH_RADIUS_M`.
- `itmPointToPoint(elevM[], stepM, txHeightM, rxHeightM, fMHz, opts) -> {lossDb, mode, warnings, errorCode, ...}`, `ItmMode`, `ItmPolarization`, `ITM_WARNINGS`. Numerically identical to the Kotlin port.
- Link: `analyzeLink(input, profile)`, `profileSeries`, `freeSpaceLossDb(m, MHz)`, `verdictFromMargin`, `LinkVerdict`, `ITM_ERROR_LOSS_DB` (999). Profile: `makeProfile(stepM, groundM)`, `profileFromSampler`, `bulgeM`, `losHeightM`, `fresnelRadiusM`. `sensitivityDbm(bwKhz, nf, sf)`, `snrLimitDb(sf)`.
- Atmosphere (ITU P.453/676/838): `refractivityN`, `kFactorFromGradient`, `surfaceRefractivityForItm`, `analyzeProfile(levels)`, `gasAttenuationDbPerKm`, `rainAttenuationDbPerKm`, `classifyDucting`, `DuctingLevel`.
- Coverage engine: `planCoverageGrid`, `computeCoverageSync(input, elevationAt, clutterAt?, {onProgress})`, `computeCoverageAsync(..., {signal, shouldCancel, yieldMs})`.
- Terrain: `PlannerElevation({fetch, fetcherFactory, tileDecoder})` with `profile(a,b,{signal})`, `prepareArea(center, radiusKm,{signal}) -> sampler(lat,lon)`, `altitudeAt(lat,lon)`. Global PMTiles archive `https://download.mapterhorn.com/planet.pmtiles` (z<=12), regional `{x}-{y}.pmtiles` for z13-18, HTTP Range. `PlannerElevationError.failure`: `NETWORK` | `DECODE`. Also `PmTilesReader`, `createPmTilesFetcher`, `createXyzTileFetcher`, `decodeTerrariumTile`, `TERRAIN_ATTRIBUTION(_URL)` (show the attribution in the UI).
- Weather: `PlannerWeather({fetch,timeoutMs}).fetch(lat, lon) -> {ok:true, conditions}|{ok:false, error}`, `parseOpenMeteo`, `buildWeatherUrl`.
- Clutter (OSM/Overpass): `PlannerOverpass({fetch, clockMs, timeoutMs, baseUrl?, mirrors?, retryDelayMs, sleep, log})` `.forLink(a,b)`, `.forArea(center, radiusKm, {signal, onRadiusReduced})`, `.invalidate()`; one request at a time, 30 min cache of 6 entries, server `[maxsize:16777216]`, response aborted past 12,000,000 bytes. The query is sent as `POST` with an `application/x-www-form-urlencoded` body `data=<query>` (CORS-safelisted, no preflight). Mirrors are tried in order (`PlannerOverpass.MIRRORS`: overpass-api.de, overpass.private.coffee, overpass.kumi.systems): the next one is used after a network error/timeout, HTTP 403/408/429/5xx or a "timed out/busy" remark; never after TOO_LARGE or HTTP 400. HTTP 429/504 wait `retryDelayMs` (2 s) and retry once on the same mirror first. `forArea` retries ONCE with half the radius (min 0.5 km) after TOO_LARGE or a server-side failure and reports it through `onRadiusReduced`; the reduced radius is remembered for 30 min. `PlannerClutter.AREA_RADIUS_OPTIONS_KM = [5,10,15,30,50,100]` (MAX 100, DEFAULT 30). Errors: `PlannerClutterError.failure` in `PlannerClutterFailure` = NETWORK / BAD_RESPONSE / TOO_LARGE. Also `ClutterMap`, `ClutterPolygon`, `ClutterKind`, `OsmQueries`, `parseOverpass`, `withClutter`, `NoClutterSource`, `CLUTTER_HEIGHT_DEFAULTS`.

## Network layer (`net.js`) and backend proxy
All three data sources go through `plannerFetch(fetchImpl, url, init)`: CORS mode, `credentials: 'omit'`, `referrerPolicy: 'no-referrer'`, no custom headers (only `Range` for PMTiles, `Content-Type: application/x-www-form-urlencoded` for Overpass). If the direct request throws a `TypeError` (CORS, CSP `connect-src`, mixed content, offline) and a proxy is installed (`setPlannerProxy(fn)`; the panel installs `createHassProxy(() => hass)` automatically), the same request is sent through the integration: `POST /api/meshtastic/planner/proxy` with JSON `{url, method, headers:{Range?}, body?}` (HA auth, administrators only, `hass.fetchWithAuth`). The host is then "sticky" for the session. The backend (`planner_proxy.py` + `planner_proxy_rules.py`) allows only `https://{overpass-api.de,overpass.private.coffee,overpass.kumi.systems}/api/interpreter` (POST `data=...`), `https://api.open-meteo.com/v1/forecast` (GET) and `https://download.mapterhorn.com/*.pmtiles` (GET, `Range: bytes=a-b`), no redirects, responses capped at 16 MB, 60 s timeout. It always answers HTTP 200 with the real upstream code in `X-Upstream-Status`; `createHassProxy` rebuilds a normal `Response` from it. Exports: `setPlannerProxy`, `hasPlannerProxy`, `createHassProxy`, `plannerFetch`, `PLANNER_PROXY_PATH`, `PLANNER_PROXY_HOSTS`.

## Notes
- Open-Meteo, Overpass and download.mapterhorn.com should allow CORS from the HA origin (Overpass and Open-Meteo do; Mapterhorn could not be verified from the build sandbox - the proxy above covers the case where it does not, or where a reverse proxy/CSP blocks the direct request).
- Show attribution: Mapterhorn, OpenStreetMap contributors, Open-Meteo.
