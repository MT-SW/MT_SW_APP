# Smart home: Zigbee, Thread, Matter

### Zigbee, Thread i Matter: czym się różnią?
Według bloga OpenELAB: Zigbee to bezprzewodowy stos IoT do czujników, przełączników i lamp; sieć ma koordynatora, routery i urządzenia końcowe (te mogą spać, oszczędzając baterię). Thread to energooszczędna sieć mesh oparta na IPv6; łączy się z siecią lokalną przez Thread Border Router. Zigbee i Thread korzystają z radia IEEE 802.15.4. Matter to standard interoperacyjności na poziomie aplikacji, nie radio ani sieć; działa przez Thread, Wi-Fi lub Ethernet. Urządzenia Zigbee nie stają się automatycznie urządzeniami Matter; wymagają mostu lub huba. W Home Assistant: dla Zigbee potrzebny koordynator, dla Thread router brzegowy, dla Matter kontroler i transport. Meshtastic (LoRa) to inna technologia: daleki zasięg i małe ilości danych, a nie sterowanie urządzeniami domowymi.
Słowa: zigbee thread matter smart home home assistant koordynator border router 802.15.4 ipv6 mesh
Źródło: https://openelab.io/pl/blogs/home-assistant/zigbee-thread-and-matter-differences-explained-simply (sprawdzono 2026-10, blog sklepu)
