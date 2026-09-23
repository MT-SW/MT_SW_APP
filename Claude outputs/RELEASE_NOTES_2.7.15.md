# v2.7.15

## Polski

### Poprawka

Włączenie sniffera MQTT (dodanego w 2.7.14) kończyło się błędem „nie udało
się połączyć z radiem", mimo że sam sniffer MQTT w ogóle nie dotyka radia.
Złożyły się na to dwie osobne przyczyny, obie teraz poprawione:

- Przełącznik zapisywał swój stan jako opcję integracji, co zawsze wywołuje
  pełny reload całej integracji — łącznie z rozłączeniem i ponownym
  łączeniem z radiem. Stan przełącznika jest teraz zapisywany tak jak inne
  ustawienia panelu (np. auto-czyszczenie bazy węzłów), bez wywoływania
  reloadu integracji.
- Panel kieruje polecenia ustawień przez jedną, jawną listę obsługiwanych
  poleceń (`panel.js`); nowe polecenie sniffera MQTT nie zostało tam
  dopisane, więc zawsze kończyło się bez efektu, a panel pokazywał ten sam
  zapasowy komunikat co przy braku łączności z radiem — stąd mylący opis
  błędu. Polecenie jest teraz poprawnie obsługiwane.

## English

### Fix

Enabling the MQTT sniffer (added in 2.7.14) always failed with a "failed to
connect to the radio" error, even though the MQTT sniffer itself never
touches the radio. Two separate causes were behind it, both fixed now:

- The toggle was persisted as an integration option, which always triggers
  a full integration reload — including disconnecting and reconnecting the
  radio. The toggle's state is now stored the same way other panel settings
  are (e.g. node database auto-cleanup), without triggering a reload.
- The panel routes settings commands through a single explicit whitelist
  (`panel.js`); the new MQTT sniffer command was never added to it, so it
  always silently did nothing, and the panel showed the same fallback
  message used for an unreachable radio — hence the misleading error text.
  The command is now handled correctly.
