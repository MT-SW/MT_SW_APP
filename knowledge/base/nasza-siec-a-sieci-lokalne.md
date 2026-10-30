# Co z artykułu o sieciach lokalnych dotyczy naszej sieci mesh

<!-- Artykuł bikeman (SQ6B) pisał o MeshCore i Meshtastic ogólnie. Tu przełożone na naszą sieć (Meshtastic, Świętokrzyskie). Fakty o ustawieniach sieci pochodzą od administratora sieci; wnioski oznaczone jako takie. -->

### Jak wnioski z artykułu o sieciach lokalnych mają się do sieci Świętokrzyskiego?
Artykuł dotyczył MeshCore i Meshtastic razem, a my używamy Meshtastic, więc trzeba go czytać przez to, jak działa nasza sieć. Główna myśl pasuje do nas wprost: sieć radiowa LoRa jest lokalna z natury, bo eter jest wspólny i mały. Dlatego nasza sieć ma obejmować region świętokrzyski, a nie cały kraj. Żadnych sztuk "od morza po góry" nie obiecujemy.
Słowa: nasza sieć, Świętokrzyskie, MT_SW, MeshCore a Meshtastic, sieć regionalna, lokalność, wnioski dla nas
Źródło: opracowanie na podstawie artykułu bikeman (https://meshtastic.pop.pl/viewtopic.php?t=290, sprawdzono 2026-10) i ustaleń administratora sieci mesh

### Jakie są ustawienia naszej sieci i dlaczego pasują do idei sieci lokalnej?
Standardowo używamy presetu Narrow Fast na częstotliwości 869,44165 MHz (wąskie pasmo). Wąski kanał i własny preset sprawiają, że nasza sieć jest oddzielona od domyślnych ustawień innych sieci, więc ruch z dalekich regionów, który płynie na domyślnych presetach, w większości do nas nie dociera. To dokładnie ten kierunek, który artykuł uznaje za rozsądny: osobny, lokalny kanał zamiast wspólnego, zatłoczonego.
Słowa: Narrow Fast, 869.44165, 869,44165 MHz, wąskie pasmo, preset sieci, własny kanał, oddzielenie od innych sieci
Źródło: ustalenia administratora sieci mesh (sprawdzono 2026-10); wniosek o oddzieleniu ruchu to opracowanie na podstawie artykułu

### Czym różni się limit przeskoków w artykule od limitu w Meshtastic?
Artykuł przywołuje limit 64 przeskoków. W Meshtastic maksymalny limit to 7 przeskoków, a domyślny to 3 (od firmware 2.8.x w MT_SW zalecamy 7, bo limit jest automatycznie obniżany dla mniej ważnych pakietów). Dlatego burza rozgłoszeniowa w Meshtastic ma mniejszy zasięg niż w opisie z artykułu, ale idea jest ta sama: im większy hop limit, tym dalej sięga zalewanie i tym więcej eteru zajmuje. W gęstej sieci lokalnej zwykle opłaca się trzymać limit niski.
Słowa: hop limit, 64 a 7, domyślnie 3, burza, zasięg zalewania, MeshCore 64, przeskoki w Meshtastic
Źródło: https://meshtastic.org/docs/configuration/radio/lora/ (limit 7, domyślnie 3) i artykuł bikeman (limit 64); sprawdzono 2026-10

### Co może zrobić użytkownik, żeby nie przeciążać naszej sieci?
Według zasad z artykułu i dobrych praktyk w naszych wpisach: nie ustawiać wyższego hop limitu niż potrzeba, nie nadawać na pełnej mocy tam, gdzie wystarczy mniej, nie ustawiać roli routera bez potrzeby i nie włączać zbędnych modułów telemetrii o krótkim interwale. Każda taka zmiana zmniejsza zajętość eteru dla wszystkich sąsiadów. To zalecenia ogólne, nie przepisy.
Słowa: przeciążenie sieci, dobre praktyki, hop limit, rola router, telemetria interwał, moc nadawania, zajętość eteru
Źródło: wnioski z wpisów bazy o routingu i airtime (sprawdzono 2026-10); zalecenia ogólne, nie przepisy
