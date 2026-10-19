# Grand Voyager

Grand Voyager is a Java travel assistant with a browser chat interface. It uses Spring Boot, Spring AI, Google Gemini, conversation memory, and tool calls for travel-related requests.

## What works

- Chat with Gemini through the web interface.
- Get current weather and short-range forecasts from Open-Meteo.
- Ask for sample hotel listings and sample booking/order lookups.
- Search flights through Duffel only when a token is configured. Duffel test mode returns synthetic results, so those results are not real fares or availability. Flight search is not enabled by default.

Hotel and booking/order data are local demo data. This app does not make hotel or flight reservations.

## Requirements

- Java 21
- A Google AI Studio API key for Gemini
- Internet access for Gemini and Open-Meteo requests

## Run locally

1. Copy `.env.example` to `.env` in this directory.
2. Put your Google API key in `.env` as `GOOGLE_API_KEY=...`.
3. From this directory, run:

   ```sh
   ./mvnw spring-boot:run
   ```

4. Open [http://localhost:8081](http://localhost:8081).

Never commit `.env` or share API keys. `.env.example` is safe to commit because it contains placeholders only.

## Example prompts

- `What's the current weather in Zurich, Switzerland?`
- `Plan a three-day itinerary for Rome.`
- `Show sample hotels in Goa under 6000 per night.`
- `Find flights from JFK to LHR on 2026-12-01.` (requires Duffel setup; use test results only as a technical demo)

## Stack

Java 21, Spring Boot, Spring AI, Google Gemini, Open-Meteo, Thymeleaf, HTML, CSS, JavaScript, Maven.
