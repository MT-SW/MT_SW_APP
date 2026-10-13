---
title: Funkcje aplikacji
parent: Przewodnik użytkownika
nav_order: 19
last_updated: 2026-08-30
description: Udostępnianie możliwości sieci mesh systemowi Android i asystentom AI działającym na urządzeniu (np. Gemini), aby mogły uruchamiać procesy sieci mesh bez otwierania aplikacji.
aliases:
  - app-functions
  - system-ai
  - gemini
  - assistant
---

# Funkcje aplikacji

Funkcje aplikacji (App Functions) udostępniają możliwości MT_SW systemowi Android i asystentom AI działającym na urządzeniu (takim jak Gemini) za pośrednictwem interfejsu Android App Functions API. Gdy są włączone, asystent może wykryć i uruchamiać za Ciebie procesy sieci mesh — na przykład wysłać wiadomość lub sprawdzić stan sieci — bez otwierania aplikacji. Funkcje aplikacji są dostępne **wyłącznie w kompilacjach Androida w wersji Google**.

> ℹ️ **Uwaga:** To coś innego niż asystent **Chirpy** wbudowany w aplikację. Funkcje aplikacji pozwalają *systemowemu* asystentowi AI działać w Twojej sieci mesh; Chirpy to asystent konwersacyjny wewnątrz samej aplikacji MT_SW_APP.

## Włączanie funkcji aplikacji

Funkcjami aplikacji zarządzasz w **Ustawienia → AI systemowe**. Ekran zawiera:

- **Główny przełącznik** o nazwie **„Zezwól na dostęp AI”**, z podpisem *„Pozwól systemowym asystentom AI (np. Gemini) wykrywać i korzystać z funkcji sieci mesh”*. Gdy jest wyłączony, żadne funkcje nie są udostępniane systemowi.
- **Osobny przełącznik dla każdej funkcji**, dzięki czemu możesz udostępnić tylko te możliwości, które chcesz.

> ⚠️ **Ważne:** Funkcje aplikacji są dostarczane włączone. W kompilacji w wersji Google główny przełącznik i każda pojedyncza funkcja, łącznie z **Wyślij wiadomość**, są domyślnie włączone — więc asystent może odczytywać dane Twojej sieci mesh i wysyłać wiadomości do Twojej sieci, dopóki nie wyłączysz **Zezwól na dostęp AI**.

Funkcje są pogrupowane w sekcję **Funkcje zapisu** (funkcje, które coś zmieniają lub wysyłają dane do Twojej sieci mesh) oraz sekcję **Funkcje odczytu** (funkcje, które tylko zwracają informacje).

![Ekran funkcji aplikacji z głównym przełącznikiem i przełącznikami poszczególnych funkcji](../../assets/screenshots/app-functions_settings.png)

Na zrzucie ekranu **Wyślij wiadomość** i **Pobierz ostatnie wiadomości** są wyłączone, aby pokazać sterowanie poszczególnymi funkcjami; świeża instalacja pokazuje wszystkie przełączniki włączone.

### Funkcje zapisu

| Funkcja | Co robi |
|----------|--------------|
| **Wyślij wiadomość** | Wysyła wiadomość tekstową do kontaktu (wiadomość bezpośrednia) lub na kanał. Sieć mesh przenosi najwyżej 233 bajty tekstu, więc wiadomości tworzone przez asystenta powinny być krótkie. |

### Funkcje odczytu

| Funkcja | Co zwraca |
|----------|-----------------|
| **Pobierz status sieci mesh** | Informację, czy jesteś połączony z radiem, i ile węzłów jest online. |
| **Pobierz listę węzłów** | Listę węzłów w Twojej sieci mesh. |
| **Pobierz informacje o kanale** | Informacje o Twoich kanałach. |
| **Pobierz status urządzenia** | Stan połączonego radia. |
| **Pobierz szczegóły węzła** | Szczegółowe informacje o wybranym węźle. |
| **Pobierz metryki sieci mesh** | Telemetrię i metryki z Twojej sieci mesh. |
| **Pobierz ostatnie wiadomości** | Ostatnie wiadomości z Twoich rozmów. |
| **Pobierz podsumowanie nieprzeczytanych** | Podsumowanie nieprzeczytanych wiadomości. |

## Prywatność

> 🔒 **Prywatność:** Funkcja **Wyślij wiadomość** pozwala asystentowi wysyłać wiadomości do Twojej sieci mesh w Twoim imieniu, a funkcje odczytu udostępniają mu dane o węzłach, wiadomościach i metrykach. Ponieważ wszystkie są domyślnie włączone, decyzja, którą tu podejmujesz, dotyczy tego, co wyłączyć, a nie tego, co włączyć. Każda funkcja ma własny przełącznik, a **Zezwól na dostęp AI** wyłącza je wszystkie naraz.

## Powiązane tematy

- [Wiadomości i kanały](messages-and-channels) — wysyłanie wiadomości bezpośrednio w aplikacji
- [Węzły](nodes) — lista węzłów, z której korzystają funkcje odczytu
- [Metryki węzła](node-metrics) — telemetria stojąca za funkcją Pobierz metryki sieci mesh
