# Pytania testowe asystenta (E2B i E4B)

**Automatycznie:** wpisz w asystencie `/test` i wyślij. Asystent sam zada sobie pytania po kolei (z prawdziwą nazwą węzła i kanału z Twojej sieci), a pytania i odpowiedzi pokaże w rozmowie, wypisze w terminalu i zapisze w pliku `assistant-test-results.md` (na komputerze, w folderze `desktopApp`). Przerwiesz przyciskiem „Zatrzymaj”. W trakcie testu nic nie otwiera się samo.

**Ręcznie:** zadaj każde pytanie w asystencie, najpierw na modelu E2B, potem na E4B. Przy każdym zapisz: OK / źle / uwagi.
Internet dla asystenta ma być WYŁĄCZONY (poza punktem 40).

## Dane z sieci (narzędzia danych)
1. Ile mam teraz węzłów w sieci?
2. Który węzeł jest najdalej ode mnie?
3. Jaki jest poziom baterii mojego węzła?
4. Które węzły słyszałem w ciągu ostatniej godziny?
5. Jaka jest średnia jakość sygnału (SNR) węzła [nazwa]?
6. Pokaż statystyki mojego radia (pakiety, pamięć).
7. Podsumuj mi całą sieć w 3 zdaniach.
8. Kto pisał do mnie ostatnio?

## Przewodnik po aplikacji
9. Jak zmienić region LoRa?
10. Gdzie ustawić limit skoków (hop limit)?
11. Jak zmienić język aplikacji?
12. Jak włączyć tłumacza offline?
13. Jak zmienić motyw na ciemny?
14. Jak powiększyć interfejs?
15. Jak wyłączyć automatyczne ładowanie obrazków w czacie?
16. Gdzie jest panel debugowania?
17. Co to jest „Funkcje MT_SW”?
18. Co nowego w ostatniej wersji?
19. Jak zaktualizować firmware?
20. Jak połączyć się z urządzeniem przez Bluetooth?

## Przejścia (aplikacja ma SAMA otworzyć miejsce; nic nie może się wysłać ani zmienić)
21. Pokaż mi ustawienie regionu. (otwiera się strona LoRa, region mruga na żółto)
22. Pokaż węzeł [nazwa] na mapie. (mapa z zaznaczonym węzłem)
23. Pokaż najbliższy router. (mapa z najbliższym routerem, w odpowiedzi odległość)
24. Czy mam zasięg do [nazwa]? (otwiera się planer łącza)
25. Napisz na kanale [nazwa kanału], że jestem na miejscu. (rozmowa z tekstem w polu, BEZ wysyłania)
26. Otwórz ustawienia wyświetlania.

## Liczby i wyszukiwanie (odpowiada program, liczby muszą się zgadzać z aplikacją)
27. Ile wiadomości było wczoraj na kanale [nazwa]?
28. Kto pisał najwięcej w ostatnim tygodniu?
29. Czy ktoś pisał o [słowo]?
30. Ile węzłów było aktywnych przez ostatnie 3 godziny?
31. Które węzły mają słabą baterię?
32. A wczoraj? (zadaj zaraz po pytaniu 27: ma policzyć wczoraj dla tego samego kanału)

## Swobodna rozmowa
33. Cześć, jak się masz?
34. Jak zrobić prostą antenę dipol na 868 MHz?
35. Opowiedz krótko, czym różni się LoRa od Wi-Fi.

## Mieszane i trudne
36. Mam słaby zasięg — co sprawdzić i pokaż mi odpowiednie ustawienie.
37. Ile węzłów jest w sieci i jak to zmienić w ustawieniach rolę mojego urządzenia?
38. Pytanie po angielsku: How do I change the LoRa region?
39. Pytanie spoza tematu: Jaka jest stolica Francji? (powinien powiedzieć, że offline nie sprawdza / odpowiedzieć z wiedzy)

## Internet (włącz przełącznik dopiero tutaj)
40. Co to jest pasmo 868 MHz i kto może na nim nadawać? (odpowiedź ma pokazać „Użyto internetu”, co wyszukano i źródła)

## Na co patrzeć
- Czy odpowiedź jest po polsku i krótka.
- Czy liczby się zgadzają z aplikacją.
- Czy przyciski prowadzą we właściwe miejsce.
- Czy w logach (Logcat / konsola) jest linia „Model … runs on <CPU/GPU>”.
- Przy złej odpowiedzi skopiuj z logów linię „Router: … -> {…}”: pokazuje, jak model zrozumiał pytanie. Z tych linii najłatwiej poprawić błąd.
