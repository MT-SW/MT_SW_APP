---
title: Kompilacje testowe i Obtainium
parent: Przewodnik dewelopera
nav_order: 10
last_updated: 2026-10-21
description: Instalacja i automatyczna aktualizacja kompilacji testowych Meshtastic z wydań GitHub za pomocą Obtainium — konfiguracje kanałów, wybór APK i format konfiguracji do udostępniania.
aliases:
  - test-builds
  - obtainium
  - beta
  - snapshot
---

# Kompilacje testowe i Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) instaluje i automatycznie aktualizuje aplikacje na Androida wprost z ich wydań na GitHubie — bez konta Play Store, bez zaproszenia do ścieżki testowej. Ta strona konfiguruje go dla Meshtastic i pokazuje, jak śledzić kanały **open beta**, **closed beta** lub **snapshot**.

Kompilacje z GitHuba są podpisane kluczem wydaniowym projektu (`CN=Kevin Hester, O=Geeksville Industries`), **a nie** kluczem Google Play — jeśli masz już zainstalowane Meshtastic ze Sklepu Play, Android odmówi aktualizacji na wierzchu. Przełączanie między wariantami `fdroid` i `google` jest w porządku: oba warianty w danym wydaniu mają *ten sam* klucz wydaniowy, więc Obtainium może przenieść cię z jednego na drugi w miejscu (sprawdzone przez zainstalowanie APK `google` na zainstalowanej kompilacji `fdroid`). Tylko *pochodzenie* kompilacji (Play czy GitHub) decyduje o tym, czy podpisy się kłócą.

> ⚠️ **Ostrzeżenie:** Odinstalowanie wersji Meshtastic ze Sklepu Play czyści dane aplikacji — historia wiadomości i ustawienia na tym telefonie przepadają. Odinstaluj ją przed instalacją przez Obtainium, a potem pozostań przy Obtainium w aktualizacjach.

## Do jakich kanałów sięga Obtainium?

Meshtastic dla Androida promuje kompilacje po drabince: `internal → closed → open → production`. Każdy stopień odpowiada ścieżce Google Play. Wydanie na GitHubie dla kompilacji internal jest wersją roboczą (draft), której Obtainium nie widzi; jest publikowane, gdy kompilacja jest promowana do closed lub wyżej.

| Kanał | Ścieżka Play | Na GitHubie | Czy Obtainium może to zainstalować? |
|---|---|---|---|
| **stable** | Production | opublikowane, oznaczone jako *Latest* | ✅ Tak |
| **open** beta | Beta (Open) | opublikowane wydanie wstępne, tag `vX.Y.Z-open.N` | ✅ Tak |
| **closed** beta | Alpha (Closed) | opublikowane wydanie wstępne, tag `vX.Y.Z-closed.N` | ✅ Tak |
| **snapshot** | — (nie ma na Play) | ciągle aktualizowane wydanie wstępne, tag `snapshot` | ✅ Tak |

Każda promocja przenosi wydanie kompilacji na GitHubie na **nowy** tag dla jej kanału (`vX.Y.Z-closed.N`, potem `vX.Y.Z-open.N`, potem `vX.Y.Z`) i zmienia jego tytuł, aby pasował. Kompilacja, która nie jest promowana dalej, zachowuje swoje wydanie, więc w trakcie cyklu wersji kanał może zawierać kilka wydań, a konfiguracje kanałów w [Konfiguracji jednym dotknięciem](#one-tap-setup) wybierają **najnowsze** wydanie, którego tytuł pasuje do kanału. Gdy wydanie produkcyjne trafi do użytkowników, a jego dokumentacja zostanie opublikowana, `post-release-cleanup.yml` usuwa każde wydanie i tag internal, closed i open o tej wersji lub niższej.

Do którego wydania rozwiązuje się każdy kanał, celowo **nie** jest tu wymienione: jest archiwizowane dla każdego tagu wydania i dołączane do aplikacji, więc numer wersji na żywo byłby tu zamrożony w chwili budowania i błędny w ciągu tygodni. [README projektu](https://github.com/meshtastic/Meshtastic-Android#get-meshtastic) zawiera aktualny stan, odświeżany automatycznie.

Kanał może być zasadnie pusty: `-open` i `-closed` są puste między wydaniem produkcyjnym a następną promocją, a `-closed` bywa pusty często na początku cyklu, zanim zostanie do niego wycięta jakakolwiek kompilacja. Liczba opublikowanych podziałów ABI `fdroid` zmieniała się między wydaniami, dlatego filtr dopasowuje każdy podział.

**Snapshot** jest inny: to zautomatyzowana kompilacja debug najnowszego commita na `main`, przebudowywana i ponownie publikowana pod jednym ruchomym tagiem `snapshot` przy każdym pushu. Nigdy nie trafia do Play. Ponieważ kompilacje debug używają sufiksu identyfikatora aplikacji `.debug` (`com.geeksville.mesh.fdroid.debug` / `com.geeksville.mesh.google.debug`) i klucza podpisu debug, snapshot instaluje się jako **własna, osobna aplikacja** — działa obok instalacji Play/stable/beta, więc ostrzeżenie o konieczności wcześniejszego odinstalowania z początku tej strony **nie** dotyczy go.

## Konfiguracja jednym dotknięciem

Dotknij linku w poniższej tabeli na telefonie z zainstalowanym Obtainium. Wszystkie ustawienia z reszty tej strony są już w nim zawarte — wybierz kanał i wariant, który chcesz ([który wariant?](#picking-the-apk)).

<!-- BEGIN GENERATED LINKS: obtainium/generate-links.py -->

| Kanał | wariant `google` | wariant `fdroid` |
|---|---|---|
| Stabilny | [Dodaj](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.geeksville.mesh%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fmeshtastic%2FMeshtastic-Android%22%2C%22author%22%3A%22meshtastic%22%2C%22name%22%3A%22Meshtastic%22%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22google-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22appName%5C%22%3A%5C%22Meshtastic%5C%22%7D%22%7D) | [Dodaj](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.geeksville.mesh%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fmeshtastic%2FMeshtastic-Android%22%2C%22author%22%3A%22meshtastic%22%2C%22name%22%3A%22Meshtastic%22%2C%22additionalSettings%22%3A%22%7B%5C%22apkFilterRegEx%5C%22%3A%5C%22fdroid-.%2A-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22Meshtastic%5C%22%7D%22%7D) |
| Open beta | [Dodaj](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.geeksville.mesh%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fmeshtastic%2FMeshtastic-Android%22%2C%22author%22%3A%22meshtastic%22%2C%22name%22%3A%22Meshtastic%20Beta%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22-open%5C%22%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22google-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22appName%5C%22%3A%5C%22Meshtastic%20Beta%5C%22%7D%22%7D) | [Dodaj](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.geeksville.mesh%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fmeshtastic%2FMeshtastic-Android%22%2C%22author%22%3A%22meshtastic%22%2C%22name%22%3A%22Meshtastic%20Beta%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22-open%5C%22%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22fdroid-.%2A-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22Meshtastic%20Beta%5C%22%7D%22%7D) |
| Closed beta | [Dodaj](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.geeksville.mesh%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fmeshtastic%2FMeshtastic-Android%22%2C%22author%22%3A%22meshtastic%22%2C%22name%22%3A%22Meshtastic%20Alpha%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22-closed%5C%22%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22google-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22appName%5C%22%3A%5C%22Meshtastic%20Alpha%5C%22%7D%22%7D) | [Dodaj](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.geeksville.mesh%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fmeshtastic%2FMeshtastic-Android%22%2C%22author%22%3A%22meshtastic%22%2C%22name%22%3A%22Meshtastic%20Alpha%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22-closed%5C%22%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22fdroid-.%2A-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22Meshtastic%20Alpha%5C%22%7D%22%7D) |
| Najnowsze (najnowsza wypromowana kompilacja testowa) | [Dodaj](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.geeksville.mesh%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fmeshtastic%2FMeshtastic-Android%22%2C%22author%22%3A%22meshtastic%22%2C%22name%22%3A%22Meshtastic%20Beta%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22-%28closed%7Copen%29%5C%22%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22google-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22appName%5C%22%3A%5C%22Meshtastic%20Beta%5C%22%7D%22%7D) | [Dodaj](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.geeksville.mesh%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fmeshtastic%2FMeshtastic-Android%22%2C%22author%22%3A%22meshtastic%22%2C%22name%22%3A%22Meshtastic%20Beta%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22-%28closed%7Copen%29%5C%22%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22fdroid-.%2A-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22Meshtastic%20Beta%5C%22%7D%22%7D) |
| Snapshot (najnowszy commit na `main`) | [Dodaj](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.geeksville.mesh.google.debug%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fmeshtastic%2FMeshtastic-Android%22%2C%22author%22%3A%22meshtastic%22%2C%22name%22%3A%22Meshtastic%20Snapshot%20%28google%29%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5ESnapshot%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Atrue%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Atrue%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22google-.%2A-debug-%5C%5C%5C%5Cd%2B%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22Meshtastic%20Snapshot%20%28google%29%5C%22%7D%22%7D) | [Dodaj](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/%7B%22id%22%3A%22com.geeksville.mesh.fdroid.debug%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fmeshtastic%2FMeshtastic-Android%22%2C%22author%22%3A%22meshtastic%22%2C%22name%22%3A%22Meshtastic%20Snapshot%20%28fdroid%29%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5ESnapshot%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Atrue%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Atrue%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22fdroid-.%2A-debug-%5C%5C%5C%5Cd%2B%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22Meshtastic%20Snapshot%20%28fdroid%29%5C%22%7D%22%7D) |

<!-- END GENERATED LINKS -->

Wolisz plik? Zaimportuj jeden z poniższych przez **Import/Export → Obtainium import**. Wybierz ten odpowiadający wariantowi, który chcesz dla kompilacji *wydaniowej*; każdy ustawia też **oba** warianty snapshot, które mogą współistnieć:

- [`meshtastic-obtainium-export-google.json`](https://github.com/meshtastic/Meshtastic-Android/blob/main/obtainium/meshtastic-obtainium-export-google.json) — `google` stable + oba snapshoty
- [`meshtastic-obtainium-export-fdroid.json`](https://github.com/meshtastic/Meshtastic-Android/blob/main/obtainium/meshtastic-obtainium-export-fdroid.json) — `fdroid` stable + oba snapshoty

### Ile wpisów można mieć naraz?

Obtainium identyfikuje aplikacje po identyfikatorze aplikacji (application ID), więc reguła wynika z naszego:

| | Identyfikator aplikacji | Mogą współistnieć? |
|---|---|---|
| Kompilacje wydaniowe — stable, open, closed, najnowsze, każdy wariant | `com.geeksville.mesh` | **Nie** — wybierz dokładnie jedną |
| Snapshot, `google` | `com.geeksville.mesh.google.debug` | Tak |
| Snapshot, `fdroid` | `com.geeksville.mesh.fdroid.debug` | Tak |

Co najwyżej jeden wpis wydaniowy plus dwa wpisy snapshot. Dotknięcie drugiego linku *wydaniowego* przełącza ten jedyny wpis na nowy kanał lub wariant zamiast dodawać kolejny, a import obu plików po kolei zostawia wpis wydaniowy tego wariantu, który był ostatni. Snapshoty nie są w żadnym wypadku naruszone.

Aby skonfigurować wszystko ręcznie, postępuj zgodnie z resztą tej strony.

## Konfiguracja ręczna

1. Zainstaluj Obtainium ([wydania na GitHubie](https://github.com/ImranR98/Obtainium/releases) lub [F-Droid / IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/dev.imranr.obtainium)).
2. Dotknij **Add App**.
3. **App Source URL:** `https://github.com/meshtastic/Meshtastic-Android`
4. Ustaw opcje dla wybranego kanału — zobacz [Stable](#stable), [Open beta](#open-beta), [Closed beta](#closed-beta), [Najnowsze](#bleeding-edge-newest-promoted-test-build-any-channel) lub [Snapshot](#snapshot-latest-commit-on-main).
5. Dotknij **Add**, a następnie **Install**.

### Stable

- **Include prereleases:** wyłączone
- *(opcjonalnie, ściślej)* **Verify the 'latest' tag:** włączone
- **Filter APKs by regular expression:** zobacz [Wybór APK](#picking-the-apk)

### Open beta

- **Include prereleases:** włączone
- **Filter release titles by regular expression:** `-open`
- **Filter APKs by regular expression:** zobacz [Wybór APK](#picking-the-apk)

### Closed beta

- **Include prereleases:** włączone
- **Filter release titles by regular expression:** `-closed`
- **Filter APKs by regular expression:** zobacz [Wybór APK](#picking-the-apk)

### Najnowsze (najnowsza wypromowana kompilacja testowa, dowolny kanał)

- **Include prereleases:** włączone
- **Filter release titles by regular expression:** `-(closed|open)`
- **Filter APKs by regular expression:** zobacz [Wybór APK](#picking-the-apk)

Obtainium instaluje najnowsze wypromowane wydanie wstępne, niezależnie od tego, czy jest to open, czy closed. Filtr tytułów jest wymagany, aby pominąć zawsze nowsze wydanie wstępne `snapshot`; bez niego Obtainium podążałoby za snapshotem.

### Snapshot (najnowszy commit na `main`)

- **Include prereleases:** włączone
- **Filter release titles by regular expression:** `^Snapshot`
- **Use the latest asset date as the release date:** włączone
- **Version detection:** wyłączone, z włączonym **Release date as version**
- **Filter APKs by regular expression:** nazwy podpisane kluczem debug, zobacz poniższą tabelę

Podąża bezpośrednio za `main` — aktualizuje się przy każdym pushu. To **kompilacje debug** (pakiet `.debug`, klucz debug), więc instalują się jako osobna aplikacja i nie zakłócą instalacji stable/beta. Tag `snapshot` nigdy się nie zmienia, więc nie ma ciągu wersji do porównania — wykrywalność aktualizacji zapewnia pseudo-wersjonowanie oparte na dacie.

APK nazywają się `…-debug-<versionCode>.apk` (nie `-release.apk`), więc użyj filtrów z sufiksem debug:

| Chcesz | Regex |
|---|---|
| wariant `google`, większość telefonów (arm64) | `google-arm64-v8a-debug-\d+\.apk` |
| wariant `fdroid`, większość telefonów (arm64) | `fdroid-arm64-v8a-debug-\d+\.apk` |
| wariant `fdroid`, uniwersalny | `fdroid-universal-debug-\d+\.apk` |

Wydania snapshot dołączają APK debug oraz, dla każdej platformy, której kompilacja się udała, niepodpisane instalatory desktopowe z tego samego commita, każdy oznaczony versionCode. Nie ma `.aab`.

Lokalne kompilacje debug i snapshoty z CI są ze sobą zgodne przy aktualizacji: repozytorium zawiera wspólny
keystore debug (`config/debug.keystore`), którym podpisują się oba, więc Obtainium może aktualizować na wierzchu twojej własnej
instalacji debug. `INSTALL_FAILED_UPDATE_INCOMPATIBLE` oznacza, że zainstalowana kompilacja powstała przed wspólnym keystore
(lub została podpisana domyślnym kluczem innej maszyny) — odinstaluj tę jedną kompilację i zainstaluj ponownie.

Jeszcze dwie rzeczy dotyczące wyboru wydania odnoszą się do każdego opisanego wcześniej kanału, nie tylko do snapshot:

> ⚠️ **Ważne:** Zostaw **Fallback to older releases** włączone. To nie jest ustawienie ścisłości kanału, a jego wyłączenie psuje konfiguracje beta i snapshot. Pętla wydań Obtainium przerywa po pierwszym kandydacie, gdy fallback jest wyłączony, więc filtr tytułów nigdy nie ma szansy pominąć niepasujących wydań. Konfiguracja przypięta do `-open` lub `^Snapshot` kończy się wtedy błędem "Could not find a suitable release", gdy na czele listy leży nowsze wydanie produkcyjne. To filtr tytułów przypina kanał; fallback pozwala Obtainium zejść w dół do najnowszego wydania, które do niego pasuje.

Jeśli filtr kanału nadal nic nie znajduje, ten kanał nie ma opublikowanego wydania, co jest częste na początku cyklu wersji oraz między wydaniem produkcyjnym a następną promocją. Sprawdź [README projektu](https://github.com/meshtastic/Meshtastic-Android#get-meshtastic), co jest aktualnie dostępne, lub użyj formy *Najnowsze*, aby śledzić to z open/closed, co jest nowsze.

## Wybór APK

Każde wydanie dołącza APK wariantu `google`, kilka APK wariantu `fdroid`, plus `.aab` (niemożliwy do zainstalowania) i instalatory desktopowe. Obtainium samo ignoruje zasoby niebędące APK, ale oba warianty mają tę samą nazwę pakietu `com.geeksville.mesh`, więc dokładnie jeden trzeba przypiąć przez **Filter APKs by regular expression**:

| Chcesz | Regex |
|---|---|
| **Zalecane do testów — wariant `google`** | `google-release\.apk` |
| wariant `fdroid`, większość telefonów (arm64) | `fdroid-arm64-v8a-release\.apk` |
| wariant `fdroid`, uniwersalny | `fdroid-universal-release\.apk` |
| wariant `fdroid`, Obtainium wybiera ABI | `fdroid-.*-release\.apk` z włączonym **Auto-select by architecture** |

**Podczas testów używaj wariantu `google`.** Zawiera Firebase Crashlytics i Datadog RUM, więc awarie i błędy, na które trafisz, są raportowane do zespołu — a to jest cały sens fazy testów. Ma też Google Maps. Wariant `fdroid` wybierz tylko wtedy, gdy wolisz nie wysyłać tej telemetrii. Możesz później zmienić zdanie: oba warianty mają ten sam klucz wydaniowy, więc Obtainium aktualizuje przez granicę wariantów bez odinstalowywania. Spodziewaj się, że wariant `google` przy pierwszym uruchomieniu pokaże ekran zgody na analitykę.

## Wykrywanie wersji wyłącza się samo — to oczekiwane

Tagi wydań mają postać `vX.Y.Z`, natomiast nasz zainstalowany `versionName` ma postać `X.Y.Z (versionCode) <flavor>`. Obtainium próbuje uzgodnić te dwa zapisy z listą standardowych formatów wersji, z których wszystkie są **w pełni zakotwiczone** — żaden nie toleruje spacji i nawiasów w naszym `versionName`. Przy pierwszym sprawdzeniu aktualizacji Obtainium loguje więc `Could not reconcile version formats for: com.geeksville.mesh`, ustawia **Version detection** na wyłączone dla aplikacji i zapisuje tag wydania jako zainstalowaną wersję.

**To jest nieszkodliwe i nie należy tego naprawiać.** Przy wyłączonym wykrywaniu Obtainium porównuje tag wydania, który ostatnio zainstalowało, z najnowszym tagiem, więc aktualizacje są nadal wykrywane i instalowane normalnie. Tracisz tylko możliwość zauważenia wersji zainstalowanej *poza* Obtainium.

W szczególności **nie ustawiaj "Trim the version string"** na `\d+\.\d+\.\d+` w nadziei, że to naprawi. To ustawienie przepisuje tylko wersję parsowaną z *wydania*; nieparsowalna strona to zainstalowany `versionName`, którego nigdy nie dotyka — więc uzgadnianie zawodzi dokładnie jak wcześniej. Dodaje też nowy sposób awarii: jeśli regex kiedykolwiek nic nie dopasuje (tag bez trzyczęściowej wersji), sprawdzanie aktualizacji kończy się wprost błędem "no version found" zamiast cicho się degradować.

Snapshot to osobny przypadek: jego tag to dosłowny ciąg `snapshot` i nigdy się nie zmienia, więc porównywanie tagów też nie może działać. Te konfiguracje używają zamiast tego pseudo-wersjonowania opartego na dacie (**Release date as version** plus **Use the latest asset date as the release date**).

## Format konfiguracji

Konfiguracja aplikacji Obtainium to płaski obiekt JSON. Wymagane są cztery klucze — `id` (nazwa pakietu Androida), `url` (repozytorium GitHub, które wybiera źródło), `author` i `name`. Wszystko inne trafia do `additionalSettings`, czyli **ciągu JSON zagnieżdżonego w JSON**, więc jego cudzysłowy i backslashe są escapowane o jeden poziom więcej. Każdy pominięty klucz przyjmuje wartość domyślną.

Ten sam obiekt występuje w trzech kształtach:

| Kształt | Format | Zastosowanie |
|---|---|---|
| Plik importu/eksportu | `{"apps":[…],"settings":null}` | **Import/Export → Obtainium import** |
| Deep link | `obtainium://app/<percent-encoded JSON>` | dodanie jednym dotknięciem z wbudowanymi ustawieniami |
| Wpis witryny konfiguracji | `{"configs":[…],"icon","categories","description"}` | PR-y do [apps.obtainium.imranr.dev](https://apps.obtainium.imranr.dev) |

`obtainium://add/<url>` tylko wstępnie wypełnia stronę Add-App; `obtainium://app/…` jest tym, co niesie ustawienia. Opakowanie linku jako `https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/…` utrzymuje go klikalnym tam, gdzie niestandardowy schemat nie zostałby zamieniony na link.

Obtainium identyfikuje aplikacje po nazwie pakietu, więc dwa warianty stable nie mogą współistnieć jako osobne wpisy — zgłoszenie do witryny konfiguracji wyraża je jako alternatywne `configs`, każdy z `altLabel`. Pełny opis kluczy i listę kontrolną zgłoszenia znajdziesz w [`obtainium/README.md`](https://github.com/meshtastic/Meshtastic-Android/blob/main/obtainium/README.md).

## Uwagi

- **Play Protect może przerwać instalację** komunikatem "App scan recommended — Play Protect hasn't seen this app before", oferując *Scan app* lub *Don't install app*. *Scan app* wysyła APK do Google. Odmowa anuluje instalację, więc na telefonie z Play Services trzeba zezwolić na skanowanie albo zainstalować przez `adb install`. Zdarzyło się to dla snapshotu podpisanego kluczem debug, ale nie dla kompilacji podpisanej kluczem wydaniowym.
- **Pierwsze uruchomienie Obtainium pokazuje dwa pełnoekranowe okna dialogowe** (powitanie i informację o certyfikacji Play). Połykają one przychodzący link `obtainium://`, więc zamknij je przed użyciem linku jednego dotknięcia.
- **Śledzenie bez instalacji:** włącz **Track-only**, aby dostawać powiadomienia o aktualizacjach, bez pobierania czegokolwiek przez Obtainium.
- **Nazwy plików snapshot zawierają versionCode**, aby ruchomy tag, który w nieskończoność używa tego samego wydania i adresów zasobów, mimo to tworzył rozróżnialny zasób przy każdej kompilacji.
