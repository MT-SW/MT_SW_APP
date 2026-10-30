<!-- Baza wiedzy MT_SW: szyfrowanie TLS w brokerze Mosquitto (certfile, keyfile, cafile). Tylko informacyjnie. -->
# Mosquitto: TLS i certyfikaty

### Jakie opcje certfile, keyfile i cafile włączają szyfrowanie TLS w Mosquitto?
TLS w Mosquitto konfiguruje się per listener. Certfile wskazuje plik z certyfikatem serwera w formacie PEM, a keyfile plik z jego kluczem prywatnym. Opcja działa tylko wtedy, gdy podasz obie. Cafile wskazuje plik z certyfikatami urzędów (CA), którym broker ufa przy sprawdzaniu certyfikatów klientów. Alternatywą dla pliku jest capath, czyli katalog. Pliki certfile i keyfile są przeładowywane po sygnale SIGHUP. Źródło nie podaje domyślnych wartości tych opcji, więc nie wpisuję żadnej. Klucz prywatny trzymaj tylko na serwerze, bo jego ujawnienie pozwala podszyć się pod broker.
Słowa: mosquitto tls, certfile, keyfile, cafile, capath, certyfikat serwera, klucz prywatny, pem, sighup, listener, szyfrowanie mqtt, mqtts, 8883, ca, ssl
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html (sprawdzono 2026-10; oficjalna strona podręcznika mosquitto.conf)

### Jak wymusić certyfikat klienta i użyć nazwy z certyfikatu jako loginu w Mosquitto?
Opcja require_certificate true wymusza, by klient przedstawił ważny certyfikat, żeby się połączyć. Wtedy use_identity_as_username bierze wspólną nazwę (CN) z certyfikatu jako nazwę użytkownika do kontroli dostępu, a plik password_file na tym listenerze nie jest używany. Dokumentacja wskazuje też use_subject_as_username jako alternatywę, ale pierwszeństwo ma use_identity_as_username. Przy require_certificate false klient sprawdza serwer, a logować się może tylko hasłem MQTT. Opcja per_listener_settings domyślnie ma wartość false. Gdy ustawisz true, hasła, listy dostępu i allow_anonymous obowiązują tylko na danym listenerze, co pozwala mieć port bez TLS i port z TLS z innymi zasadami.
Słowa: require_certificate, use_identity_as_username, certyfikat klienta, mtls, cn, common name, per_listener_settings, dwa listenery, port 1883 i 8883, uwierzytelnianie certyfikatem, allow_anonymous, password_file
Źródło: https://mosquitto.org/man/mosquitto-conf-5.html (sprawdzono 2026-10; oficjalna strona podręcznika mosquitto.conf; wartości domyślne require_certificate i use_identity_as_username nie są podane)
