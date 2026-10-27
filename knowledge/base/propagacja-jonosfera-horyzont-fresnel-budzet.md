# Propagacja: jonosfera, horyzont radiowy, strefa Fresnela, bilans łącza

### Widoczność optyczna, wysokość anteny i współczynnik k
Powyżej około 30 MHz (UKF i wyżej) fala jonosferyczna i przyziemna nie mają znaczenia, więc przeszkody blokują sygnał, a zasięg zbliża się do widoczności optycznej. Odbicia od ziemi lub słonej wody mogą osłabiać albo wzmacniać sygnał bezpośredni; podniesienie anten zmniejsza ten efekt (zysk wysokościowy) i wydłuża zasięg, a horyzonty radiowe obu anten się sumują. Refrakcja atmosfery zagina fale ku Ziemi, jakby promień Ziemi był większy o 4/3 (k = 4/3), co zwiększa zasięg o około 15%. Przy burzowej pogodzie k spada, co daje zaniki. Wzory na odległość horyzontu źródło podaje, ale nie przeszły w pobraniu tekstu. Z ogólnej wiedzy: horyzont radiowy to około 4,12 razy pierwiastek z wysokości w metrach, w kilometrach (dla k = 4/3).
Słowa: horyzont radiowy, line of sight, współczynnik k, 4/3, wysokość anteny, zysk wysokościowy, refrakcja, zasięg
Źródło: https://en.wikipedia.org/wiki/Line-of-sight_propagation (sprawdzono 2026-10)

### Strefa Fresnela: ile musi być wolne
Strefy Fresnela to zagnieżdżone elipsoidy między nadajnikiem a odbiornikiem, wokół prostej linii widoczności. Fale odbite w pierwszej strefie zwykle pomagają, w drugiej zwykle szkodzą. Źródło: pierwsza strefa musi być wolna co najmniej w 60%, idealnie w 80% (przeszkód nie więcej niż 40%, zalecane do 20%). Promień zależy od odległości, częstotliwości i prędkości światła; wzoru w pobranym tekście nie było. Przed montażem policz pierwszą strefę, sprawdź, czy drzewa lub budynki do niej wchodzą, bo nawet nie przecinając linii widoczności osłabiają sygnał. Wysokość anteny bywa przy tym nieintuicyjna: obniżenie czasem poprawia stosunek sygnału do szumu przez odbicia.
Słowa: strefa Fresnela, Fresnel, 60%, przeszkody, drzewa, link, linia widoczności, planowanie łącza
Źródło: https://en.wikipedia.org/wiki/Fresnel_zone (sprawdzono 2026-10)

### Bilans łącza: wzór i marginesy
Odebrana moc (dBm) = moc nadawana + zyski anten − straty (kable, złącza, tłumienie trasy, inne). Straty inne to m.in. margines na zaniki, tłumienie ciała i niedopasowanie polaryzacji. Margines łącza to zapas między mocą odbieraną a minimalną wymaganą przez odbiornik, dobrany do spodziewanych zaników. Tłumienie w wolnej przestrzeni: 32,45 + 20 log(f w MHz) + 20 log(d w km). Dla 869,44 MHz: 1 km 91,2 dB, 5 km 105,2 dB, 10 km 111,2 dB, 20 km 117,3 dB, 50 km 125,2 dB (przeliczone z tego wzoru). Podwojenie odległości dodaje 6 dB. Przykłady ze źródła: ściana z płyt gipsowo-kartonowych na drewnianym stelażu około 6 dB na ścianę przy 2,4 GHz; wewnątrz budynku widoczność optyczna trwa tylko około 3 m, a dalej tłumienie może rosnąć do 30 dB na 30 m.
Słowa: bilans łącza, link budget, margin, zaniki, FSPL, tłumienie, dBm, czułość odbiornika, ściany tłumienie
Źródło: https://en.wikipedia.org/wiki/Link_budget (sprawdzono 2026-10)

