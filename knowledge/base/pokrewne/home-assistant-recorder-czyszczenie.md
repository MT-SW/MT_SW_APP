<!-- Baza wiedzy MT_SW: baza historii Home Assistant (recorder), czyszczenie i kompaktowanie, oszczędzanie karty SD. Tylko informacyjnie. -->
# Home Assistant: baza historii (recorder) i czyszczenie

### Jak Home Assistant czyści bazę historii: auto_purge i purge_keep_days?
Domyślnie auto_purge jest włączone i co noc o 04:12 czasu lokalnego baza jest czyszczona. Bez tego baza rosłaby bez końca, zajmując miejsce i spowalniając Home Assistant. Opcja purge_keep_days domyślnie wynosi 10 dni i określa, ile dni historii zostaje po czyszczeniu. Jeśli wyłączysz auto_purge, dokumentacja zaleca utworzyć automatyzację wywołującą recorder.purge okresowo. Usługa recorder.purge_entities czyści dane wybranych encji, domen lub wzorców. Dokumentacja nie podaje domyślnej wartości keep_days dla tej usługi, więc sprawdź ją na swojej wersji przed użyciem.
Słowa: home assistant baza, recorder, auto_purge, 04:12, purge_keep_days, 10 dni, czyszczenie historii, recorder.purge, purge_entities, baza rośnie, zapchany dysk, historia zdarzeń
Źródło: https://www.home-assistant.io/integrations/recorder (sprawdzono 2026-10; oficjalna dokumentacja Home Assistant)

### Co robi auto_repack i commit_interval w bazie Home Assistant i dlaczego to ważne dla karty SD?
Opcja auto_repack domyślnie jest włączona i przepakowuje bazę drugiej niedzieli każdego miesiąca, po automatycznym czyszczeniu. Bez tego plik bazy może nie zmaleć mimo czyszczenia, więc dalej zajmuje miejsce. Opcja commit_interval domyślnie wynosi 5 sekund i określa, jak często zdarzenia zapisują się do bazy. Wyższa wartość zmniejsza zapisy na dysku, co według dokumentacji może wydłużyć życie karty SD. Kosztem jest opóźnienie bazy, choć panele historii i aktywności aktualizują się od razu. Wartość 0 zapisuje tak szybko, jak to możliwe. Wybór konkretnej wartości dla Twojego serwera zależy od obciążenia.
Słowa: auto_repack, przepakowanie bazy, commit_interval, zapis co 5 sekund, karta sd home assistant, zużycie karty, zapisy na dysk, sqlite, home-assistant_v2.db, historia, opóźnienie bazy
Źródło: https://www.home-assistant.io/integrations/recorder (sprawdzono 2026-10; oficjalna dokumentacja Home Assistant; wpływ na kartę SD według opisu dokumentacji)
