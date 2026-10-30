<!-- Baza wiedzy MT_SW: listy kontroli dostępu (ACL) w Mosquitto, czyli kto może czytać i pisać na jakich tematach. Tylko informacyjnie. -->
# Mosquitto: ACL, czyli kto może czytać i pisać

### Jak zapisać listę ACL w Mosquitto: format tematów, użytkowników i komentarzy?
Plik ACL określa dostęp do tematów. Każda linia ma postać: topic [read|write|readwrite|deny] temat. Jeśli nie podasz typu, dostęp to read/write. Tematy mogą zawierać znaki + i #. Linie zaczynające się od # są komentarzem. Reguły na początku pliku dotyczą klientów anonimowych, o ile anonimowy dostęp jest dozwolony. Sekcja user nazwa zaczyna reguły dla konkretnego użytkownika. Nazwa musi pasować do loginu z pakietu CONNECT, a nie do identyfikatora klienta. Reguły deny sprawdza się przed regułami nadania, więc mogą wyciąć wyjątek z szerszej reguły.
Słowa: mosquitto acl, acl_file, plik acl, read write readwrite deny, user, komentarz, login, identyfikator klienta, prawa dostępu mqtt, kontrola tematów, deny, wildcard
Źródło: https://mosquitto.org/documentation/plugins/acl-file/ (sprawdzono 2026-10; oficjalna dokumentacja Mosquitto)

### Jak ograniczyć użytkownikowi dostęp do własnego tematu w Mosquitto i jak włączyć wtyczkę ACL?
Wzorzec pattern z podstawieniami %u (login) i %c (identyfikator klienta) pozwala dać każdemu użytkownikowi dostęp tylko do jego tematu. Przykład: pattern write sensor/%u/data pozwala każdemu zapisywać wyłącznie do sensor/<login>/data. Podstawienie musi być jedynym tekstem na danym poziomie tematu. Wzorce obowiązują wszystkich, nawet po linii user. Od Mosquitto 2.1 zalecany jest plik ACL przez wtyczkę mosquitto_acl_file: w mosquitto.conf dodajesz global_plugin ze ścieżką do pliku .so (na Windows .dll) i plugin_opt_acl_file ze ścieżką do listy. Opcja acl_file w samym pliku konfiguracji jest zastępowana przez wtyczkę. Dla bardziej rozbudowanej kontroli dokumentacja wskazuje wtyczkę dynamic-security.
Słowa: pattern, %u, %c, login w temacie, sensor/%u/data, mosquitto_acl_file, global_plugin, plugin_opt_acl_file, wtyczka acl, dynamic-security, zastąpienie acl_file, mosquitto 2.1, uprawnienia per użytkownik
Źródło: https://mosquitto.org/documentation/plugins/acl-file/ (sprawdzono 2026-10; oficjalna dokumentacja Mosquitto)
