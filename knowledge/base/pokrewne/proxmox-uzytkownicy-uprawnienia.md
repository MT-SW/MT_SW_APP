### Jakie realmy uwierzytelniania oferuje Proxmox VE?
Proxmox VE obsługuje kilka realmów: Linux PAM (konta systemowe hosta), wbudowany serwer uwierzytelniania Proxmox VE, LDAP, Microsoft Active Directory oraz OpenID Connect. Konfiguracja realmów leży w pliku `/etc/pve/domains.cfg`. Przy PAM użytkownik musi istnieć na każdym węźle, na który się loguje.

Słowa: proxmox, realm, PAM, LDAP, Active Directory, OpenID Connect, domains.cfg
Źródło: https://pve.proxmox.com/wiki/User_Management (sprawdzono 2026-10)

### Gdzie Proxmox VE przechowuje dane użytkowników i hasła?
Atrybuty użytkowników, np. imię, e-mail, grupy, datę wygaśnięcia i status, są w `/etc/pve/user.cfg`. Hasła nie trafiają do tego pliku. Użytkownicy z realmów zewnętrznych (LDAP, AD, OIDC) uwierzytelniają się w zewnętrznym systemie, a wyjątkiem jest serwer PVE, który trzyma zahashowane hasła w `/etc/pve/priv/shadow.cfg`.

Słowa: proxmox, user.cfg, shadow.cfg, hasła, użytkownik, LDAP
Źródło: https://pve.proxmox.com/wiki/User_Management (sprawdzono 2026-10)

### Z czego składa się reguła dostępu w Proxmox VE?
Reguła RBAC to trójka: ścieżka obiektu (np. `/vms`, `/storage/{storeid}`, `/pool/{poolid}`), podmiot (użytkownik, grupa lub token API) i rola, czyli nazwany zestaw uprawnień. Uprawnienia dziedziczą się w dół drzewa ścieżek. Głębsza ścieżka nadpisuje dziedziczenie, bezpośrednie uprawnienie użytkownika przebija grupowe, a `NoAccess` anuluje wszystkie inne role na danej ścieżce.

Słowa: proxmox, RBAC, acl, rola, ścieżka, NoAccess, dziedziczenie
Źródło: https://pve.proxmox.com/wiki/User_Management (sprawdzono 2026-10)

### Jak nadać użytkownikowi tylko odczyt wszystkich VM w Proxmox?
Przykład z dokumentacji to `pveum acl modify /vms -user joe@pve -role PVEAuditor`. Daje użytkownikowi `joe@pve` rolę tylko do odczytu na wszystkich maszynach wirtualnych. Flagi propagate nie trzeba podawać, bo dziedziczenie w dół jest domyślne.

Słowa: proxmox, pveum, acl, PVEAuditor, uprawnienia, odczyt, VM
Źródło: https://pve.proxmox.com/wiki/User_Management (sprawdzono 2026-10)
