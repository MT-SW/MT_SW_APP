<!-- Baza wiedzy MT_SW: zasady grants (uprawnień) w pliku polityk tailnetu Tailscale. Tylko informacyjnie. -->
# Tailscale: grants (uprawnienia w pliku polityk)

### Jak działają grants w Tailscale: czy domyślnie wszystko jest dozwolone?
Grants to nowszy sposób zarządzania dostępem w pliku polityk tailnetu. Działają w modelu deny-by-default, czyli dostęp jest dozwolony tylko wtedy, gdy grant wprost na to pozwala. Jeśli pasuje kilka grants do jednego połączenia, silnik polityk stosuje sumę wszystkich przyznanych możliwości. Bardziej szczegółowy grant nie nadpisuje ogólnego, tylko do niego dokłada. Grants mają domyślną akcję accept, więc w swoim zakresie są z natury przepuszczające. Źródło dodaje, że grants i stare ACL mogą być w jednym pliku, ale zalecane jest używanie grants. Wniosek, że w domowym tailnecie warto zacząć od jednego grantu dla siebie, to z ogólnej wiedzy.
Słowa: tailscale grants, grants, deny by default, domyślnie zablokowane, plik polityk, tailnet policy file, acl, suma uprawnień, accept, przepuszczanie, dostęp, kontrola dostępu
Źródło: https://tailscale.com/docs/reference/syntax/grants.md (sprawdzono 2026-10; oficjalna dokumentacja Tailscale)

### Z czego składa się grant w Tailscale: src, dst, ip i app?
Każdy grant wymaga źródła (src) i celu (dst) oraz co najmniej jednego z pól: ip dla możliwości sieciowych albo app dla możliwości aplikacyjnych. Pola srcPosture i via są opcjonalne. Wszystkie właściwości grantu to tablice, nawet jeśli mają jeden element, oprócz app, które jest mapą. Gdy w tablicy src lub dst wypiszesz kilka selektorów, silnik bierze sumę wszystkich pasujących podmiotów. Źródło nie opisuje tu konkretnych selektorów grup i tagów, więc tego nie rozwijam. Tyle wystarczy, by zrozumieć strukturę przy przeglądaniu pliku.
Słowa: grant src dst, ip, app, srcPosture, via, selektory, tablica, mapa, tagi, grupy, pola grantu, składnia, json, hujson
Źródło: https://tailscale.com/docs/reference/syntax/grants.md (sprawdzono 2026-10; oficjalna dokumentacja składni grants Tailscale)
