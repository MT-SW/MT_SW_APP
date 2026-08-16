<#
.SYNOPSIS
    Sprawdza, czy autorskie funkcje forka przetrwały ostatni `git merge origin/main`.

.DESCRIPTION
    Git czasem po cichu (bez konfliktu, bez ostrzeżenia) gubi hunki dodane lokalnie,
    gdy upstream mocno przebudował ten sam plik. Ten skrypt nie zapobiega temu
    zjawisku, ale wykrywa je natychmiast po mergu, zamiast dopiero przy błędzie
    kompilacji albo -- gorzej -- przy ciszy w działającej apce.

    Uruchamiaj po KAŻDYM `git merge origin/main`, PRZED commitem.

.USAGE
    cd C:\Kodowanie\AS\Meshtastic_Android
    .\verify-custom-features.ps1

    Dopisuj nowe wpisy do $checks za każdym razem, gdy dodajesz nową autorską
    funkcję -- to jedyna "konserwacja" jakiej ten skrypt wymaga.
#>

$ErrorActionPreference = "SilentlyContinue"

$checks = @(
    # --- Uptime icon w liście node'ów (sesja 08-10/08-15) ---
    @{ File = "core\ui\src\commonMain\kotlin\org\meshtastic\core\ui\component\NodeItem.kt"; Pattern = "rememberRefreshIcon"; Feature = "Ikona uptime (widok Complete)" }
    @{ File = "core\ui\src\commonMain\kotlin\org\meshtastic\core\ui\component\NodeItemCompact.kt"; Pattern = "rememberRefreshIcon"; Feature = "Ikona uptime (widok Compact)" }

    # --- Relay "via NodeX" na liście node'ów (sesja 08-10/08-15) ---
    @{ File = "core\database\src\commonMain\kotlin\org\meshtastic\core\database\dao\MeshLogDao.kt"; Pattern = "getLatestLogPerNode"; Feature = "Relay: zapytanie DAO" }
    @{ File = "feature\node\src\commonMain\kotlin\org\meshtastic\feature\node\list\NodeListViewModel.kt"; Pattern = "relayNodeIds"; Feature = "Relay: ViewModel listy" }

    # --- Relay w szczegółach node'a (sesja 08-14/08-15) ---
    @{ File = "feature\node\src\commonMain\kotlin\org\meshtastic\feature\node\domain\usecase\CommonGetNodeDetailsUseCase.kt"; Pattern = "relayNodeName"; Feature = "Relay: use case szczegółów" }

    # --- Wielokrotny relay w dialogu dostarczenia + zdalny routing ACK (sesja 07-30/08-04) ---
    @{ File = "core\data\src\commonMain\kotlin\org\meshtastic\core\data\manager\MeshDataHandlerImpl.kt"; Pattern = "relayNodes"; Feature = "Relay: akumulacja listy w handleAckNak" }
    @{ File = "core\data\src\commonMain\kotlin\org\meshtastic\core\data\manager\MeshDataHandlerImpl.kt"; Pattern = "completeRoutingAck"; Feature = "Zdalny routing-ACK: wybudzanie czekających" }
    @{ File = "core\data\src\commonMain\kotlin\org\meshtastic\core\data\manager\PacketHandlerImpl.kt"; Pattern = "routingAckResponse"; Feature = "Zdalny routing-ACK: infrastruktura PacketHandler" }
    @{ File = "feature\node\src\commonMain\kotlin\org\meshtastic\feature\node\detail\NodeManagementActions.kt"; Pattern = "destNum"; Feature = "Zdalne favorite/ignore (destNum)" }

    # --- Angielskie klucze zasobów specyficzne dla forka ---
    @{ File = "core\resources\src\commonMain\composeResources\values\strings.xml"; Pattern = "network_health"; Feature = "String EN: network_health" }
    @{ File = "core\resources\src\commonMain\composeResources\values\strings.xml"; Pattern = "add_contact"; Feature = "String EN: add_contact" }
    @{ File = "core\resources\src\commonMain\composeResources\values\strings.xml"; Pattern = "auto_load_chat_images"; Feature = "String EN: auto_load_chat_images" }

    # --- Inne znane autorskie funkcje (dopisz tu kolejne w miarę rozwoju forka) ---
        @{ File = "androidApp\build.gradle.kts"; Pattern = "pl.swietokrzyskie.meshtastic"; Feature = "applicationId side-by-side z oryginałem" }
        @{ File = "core\ui\src\commonMain\kotlin\org\meshtastic\core\ui\component\NarrowBandWarningDialog.kt"; Pattern = "NarrowBandWarningDialog"; Feature = "Ostrzeżenie o wąskim paśmie" }

        # --- Czas "ostatnia pozycja" dla fixed position (sesja 08-15) ---
        @{ File = "feature\node\src\commonMain\kotlin\org\meshtastic\feature\node\component\LinkedCoordinatesItem.kt"; Pattern = "LOC_MANUAL"; Feature = "Poprawka czasu pozycji: szczegóły node'a (LinkedCoordinatesItem)" }
        @{ File = "androidApp\src\fdroid\kotlin\org\meshtastic\app\map\traceroute\TracerouteOsmMap.kt"; Pattern = "LOC_MANUAL"; Feature = "Poprawka czasu pozycji: mapa Traceroute (fdroid)" }
        @{ File = "androidApp\src\google\kotlin\org\meshtastic\app\map\MapView.kt"; Pattern = "LOC_MANUAL"; Feature = "Poprawka czasu pozycji: mapa Google" }

        # --- Desktop: import/eksport configu + auto-load obrazków (sesja 08-15) ---
        @{ File = "feature\settings\src\jvmMain\kotlin\org\meshtastic\feature\settings\DesktopSettingsScreen.kt"; Pattern = "EditDeviceProfileDialog"; Feature = "Desktop: import/eksport configu urządzenia" }
        @{ File = "feature\settings\src\jvmMain\kotlin\org\meshtastic\feature\settings\DesktopSettingsScreen.kt"; Pattern = "auto_load_chat_images"; Feature = "Desktop: przełącznik auto-load obrazków (UI, nie tylko string)" }

        # --- Desktop: cała mapa, odzyskana po utracie 08-15 (sesje 08-10 do 08-15) ---
        @{ File = "desktopApp\src\main\kotlin\org\meshtastic\desktop\map\DesktopMapViewProvider.kt"; Pattern = "OsmCanvasMap"; Feature = "Desktop: renderer mapy (DesktopMapViewProvider)" }
        @{ File = "desktopApp\src\main\kotlin\org\meshtastic\desktop\map\DesktopMapScreen.kt"; Pattern = "DesktopMapFilterDropdown"; Feature = "Desktop: filtr mapy + dialog warstw" }
        @{ File = "desktopApp\src\main\kotlin\org\meshtastic\desktop\map\DesktopMapLayerManager.kt"; Pattern = "addGeoJsonLayer"; Feature = "Desktop: import warstw GeoJSON/KML" }
        @{ File = "desktopApp\src\main\kotlin\org\meshtastic\desktop\map\TileCache.kt"; Pattern = "TileCache"; Feature = "Desktop: cache kafelków offline" }
        @{ File = "desktopApp\src\main\kotlin\org\meshtastic\desktop\siteplanner\JcefRuntime.kt"; Pattern = "CefAppBuilder"; Feature = "Desktop:

Write-Host ""
Write-Host "=== Weryfikacja autorskich funkcji forka ===" -ForegroundColor Cyan
Write-Host ""

$missing = @()

foreach ($check in $checks) {
    if (-not (Test-Path $check.File)) {
        Write-Host "[?] $($check.Feature)" -ForegroundColor Yellow
        Write-Host "    Plik nie istnieje (mógł zmienić nazwę/lokalizację): $($check.File)" -ForegroundColor Yellow
        $missing += $check
        continue
    }

    $found = Select-String -Path $check.File -Pattern $check.Pattern -SimpleMatch -Quiet

    if ($found) {
        Write-Host "[OK] $($check.Feature)" -ForegroundColor Green
    } else {
        Write-Host "[BRAK] $($check.Feature)" -ForegroundColor Red
        Write-Host "    Nie znaleziono '$($check.Pattern)' w $($check.File)" -ForegroundColor Red
        $missing += $check
    }
}

Write-Host ""
if ($missing.Count -eq 0) {
    Write-Host "Wszystko na miejscu ($($checks.Count)/$($checks.Count))." -ForegroundColor Green
} else {
    Write-Host "UWAGA: $($missing.Count) z $($checks.Count) funkcji może być zgubionych w mergu." -ForegroundColor Red
    Write-Host "Zanim zrobisz commit, wróć do historii czatu / pamięci dla każdej z powyższych i odtwórz ją." -ForegroundColor Red
}
Write-Host ""
