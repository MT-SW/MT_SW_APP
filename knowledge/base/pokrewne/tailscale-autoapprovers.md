<!-- Baza wiedzy MT_SW: sekcja autoApprovers w pliku polityk Tailscale (automatyczne zatwierdzanie tras i węzłów wyjściowych). Tylko informacyjnie. -->
# Tailscale: autoApprovers

### Jak skonfigurować autoApprovers w Tailscale, żeby trasy podsieci zatwierdzały się same?
Sekcja autoApprovers w pliku polityk tailnetu pozwala, by wybrani użytkownicy, grupy albo tagi zatwierdzali określone działania bez ręcznego kroku w panelu administracyjnym. Ma dwa klucze. Klucz routes odwzorowuje każdą trasę CIDR na listę osób zatwierdzających, np. grupę, konto e-mail albo tag. Klucz exitNode to lista osób, które mogą ogłaszać węzeł wyjściowy. Przykład ze źródła: grupa inżynierów, konto alice i urządzenia z tagiem foo mogą ogłaszać trasę 192.0.2.0/24 bez zatwierdzenia. Przypisanie tagu zamiast konta jest wygodniejsze przy zmianach właściciela urządzenia.
Słowa: autoapprovers, auto approvers, zatwierdzanie tras, polityka tailscale, routes, exitNode, tag, grupa, zatwierdzanie automatyczne, panel administracyjny, ogłaszanie trasy, plik polityk
Źródło: https://tailscale.com/docs/reference/syntax/policy-file (sprawdzono 2026-10; oficjalna dokumentacja Tailscale; zalecenie o tagach z ogólnej wiedzy)

### Czy zmiana autoApprovers zatwierdza już istniejące trasy i co unieważnia automatyczne zatwierdzenie?
Automatyczne zatwierdzenie działa tylko wtedy, gdy Tailscale pierwszy raz dostaje ogłoszenie trasy. Dodanie lub zmiana autoApprovers nie zatwierdza trasy, która już czeka na zatwierdzenie. Aby to wywołać, usuń trasę z routera i ogłoś ją ponownie. Trasa przestaje być ogłaszana, gdy inny użytkownik ponownie uwierzytelni urządzenie albo gdy użytkownik, który ogłaszał trasę, zostanie zawieszony lub usunięty. Dlatego jako zatwierdzającego warto wskazać tag, a nie konto osobiste.
Słowa: autoapprovers zmiana, trasa czeka na zatwierdzenie, ponowne ogłoszenie trasy, usunięcie użytkownika, zawieszone konto, ponowne uwierzytelnienie, tag zamiast konta, trasa przestała działać
Źródło: https://tailscale.com/docs/reference/syntax/policy-file (sprawdzono 2026-10; oficjalna dokumentacja Tailscale; zalecenie o tagu zamiast konta z ogólnej wiedzy)
