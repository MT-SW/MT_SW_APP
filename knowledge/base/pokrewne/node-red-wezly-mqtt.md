<!-- Baza wiedzy MT_SW: węzły MQTT in i MQTT out w Node-RED i wspólne połączenie z brokerem. Tylko informacyjnie. -->
# Node-RED: węzły MQTT in i MQTT out

### Jak skonfigurować węzły MQTT in i MQTT out w Node-RED i czy mogą współdzielić połączenie z brokerem?
Node-RED ma wbudowane dwa węzły MQTT. MQTT in subskrybuje temat, który może zawierać znaki wieloznaczne + i #, a każda odebrana wiadomość trafia do przepływu z payloadem i danymi, np. tematem i QoS. MQTT out wysyła wiadomości z przepływu do tematu. Oba węzły korzystają z osobnego węzła konfiguracyjnego, który trzyma dane połączenia z brokerem. Ustawia się go, klikając ikonkę ołówka przy polu Server. Jeden węzeł konfiguracyjny może obsługiwać wiele węzłów in i out, bo współdzielą jedno połączenie. Źródło nie opisuje szczegółowo MQTT out, więc przy jego ustawieniach QoS i retencji sprawdź dokumentację Node-RED. Na FlowFuse połączenie z brokerem konfiguruje się automatycznie, to inna usługa, nie ogólna zasada.
Słowa: node-red, mqtt in, mqtt out, węzeł, server, broker, połączenie, ołówek, konfiguracja, temat, wildcard, subskrypcja, publikacja, przepływ, flow
Źródło: https://flowfuse.com/node-red/core-nodes/mqtt (sprawdzono 2026-10; opis węzłów Node-RED na stronie FlowFuse)
