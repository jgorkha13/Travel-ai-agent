package com.substring.agent.backend.tools;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.JsonNode;

@Component
public class FlightTools {
    private final RestClient restClient = RestClient.create("https://api.duffel.com");
    private final String accessToken;

    public FlightTools(@Value("${duffel.access-token:}") String accessToken) {
        this.accessToken = accessToken == null ? "" : accessToken.trim();
    }

    @Tool(description = "Search live airline flight offers for a one-way trip. Origin and destination can be city names or three-letter IATA codes. The departure date must be YYYY-MM-DD. This first version searches one adult in economy. Ask for any missing origin, destination, or date; do not make up availability or prices.")
    public String searchFlight(String source, String destination, String date) {
        if (accessToken.isBlank()) {
            return "Live flight search is not configured yet. Add a Duffel test or live access token as DUFFEL_ACCESS_TOKEN in the backend .env file.";
        }
        if (source == null || source.isBlank() || destination == null || destination.isBlank()) {
            return "Please provide both the departure city and destination city.";
        }

        final LocalDate departureDate;
        try {
            departureDate = LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException exception) {
            return "Please provide the departure date in YYYY-MM-DD format.";
        }
        if (departureDate.isBefore(LocalDate.now())) {
            return "That departure date has already passed. Ask the user for a future date.";
        }

        try {
            String origin = resolvePlace(source);
            if (origin.startsWith("NEEDS_CLARIFICATION:")) {
                return origin.substring("NEEDS_CLARIFICATION:".length());
            }
            String destinationCode = resolvePlace(destination);
            if (destinationCode.startsWith("NEEDS_CLARIFICATION:")) {
                return destinationCode.substring("NEEDS_CLARIFICATION:".length());
            }

            Map<String, Object> requestBody = Map.of("data", Map.of(
                    "slices", List.of(Map.of(
                            "origin", origin,
                            "destination", destinationCode,
                            "departure_date", departureDate.toString())),
                    "passengers", List.of(Map.of("type", "adult")),
                    "cabin_class", "economy"));

            JsonNode response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/air/offer_requests")
                            .queryParam("return_offers", true)
                            .queryParam("supplier_timeout", 10000)
                            .build())
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.set("Duffel-Version", "v2");
                        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode data = response == null ? null : response.path("data");
            JsonNode offers = data == null ? null : data.path("offers");
            if (offers == null || !offers.isArray() || offers.isEmpty()) {
                return "Duffel found no live flight offers for " + source + " to " + destination
                        + " on " + departureDate + ". Try another date or nearby airport.";
            }

            List<JsonNode> sortedOffers = new ArrayList<>();
            offers.forEach(sortedOffers::add);
            sortedOffers.sort(Comparator.comparing(offer ->
                    new BigDecimal(offer.path("total_amount").asText("999999999"))));

            StringBuilder result = new StringBuilder("Live flight offers from Duffel for ")
                    .append(source).append(" to ").append(destination).append(" on ")
                    .append(departureDate).append(" (one adult, economy). Prices and availability can change.\n");
            int count = Math.min(5, sortedOffers.size());
            for (int index = 0; index < count; index++) {
                JsonNode offer = sortedOffers.get(index);
                result.append("\nOffer ").append(index + 1)
                        .append(" (offer ID ").append(offer.path("id").asText("unavailable"))
                        .append("): ").append(offer.path("total_currency").asText(""))
                        .append(" ").append(offer.path("total_amount").asText("price unavailable"));

                JsonNode slices = offer.path("slices");
                if (slices.isArray()) {
                    for (JsonNode slice : slices) {
                        JsonNode segments = slice.path("segments");
                        if (!segments.isArray()) continue;
                        for (JsonNode segment : segments) {
                            String carrier = segment.path("operating_carrier").path("name")
                                    .asText(segment.path("marketing_carrier").path("name").asText("Airline unavailable"));
                            result.append("\n  ").append(carrier)
                                    .append(" flight ").append(segment.path("marketing_carrier_flight_number").asText(""))
                                    .append(": ").append(segment.path("origin").path("iata_code").asText("?"))
                                    .append(" to ").append(segment.path("destination").path("iata_code").asText("?"))
                                    .append(", departs ").append(segment.path("departing_at").asText("time unavailable"))
                                    .append(", arrives ").append(segment.path("arriving_at").asText("time unavailable"));
                        }
                    }
                }
            }
            return result.toString();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                return "Duffel rejected the access token. Check DUFFEL_ACCESS_TOKEN in the backend .env file and restart the app.";
            }
            return "Duffel flight search failed with HTTP " + exception.getStatusCode().value()
                    + ". Check the route, date, and Duffel account settings, then try again.";
        } catch (RestClientException | IllegalArgumentException exception) {
            return "Could not reach Duffel flight search right now. Please try again shortly.";
        }
    }

    private String resolvePlace(String placeName) {
        String trimmed = placeName.trim();
        if (trimmed.matches("(?i)^[A-Z]{3}$")) {
            return trimmed.toUpperCase(Locale.ROOT);
        }

        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/places/suggestions")
                        .queryParam("query", trimmed)
                        .build())
                .headers(headers -> {
                    headers.setBearerAuth(accessToken);
                    headers.set("Duffel-Version", "v2");
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .retrieve()
                .body(JsonNode.class);

        JsonNode places = response == null ? null : response.path("data");
        if (places == null || !places.isArray() || places.isEmpty()) {
            return "NEEDS_CLARIFICATION: I couldn't find an airport or city for '" + trimmed
                    + "'. Ask the user for a nearby airport or three-letter IATA code.";
        }

        String normalized = trimmed.toLowerCase(Locale.ROOT);
        Map<String, JsonNode> exactMatches = new LinkedHashMap<>();
        for (JsonNode place : places) {
            String name = place.path("name").asText("");
            String cityName = place.path("city_name").asText("");
            String type = place.path("type").asText("");
            if ((name.equalsIgnoreCase(trimmed) || cityName.equalsIgnoreCase(trimmed))
                    && ("city".equals(type) || name.equalsIgnoreCase(trimmed))) {
                String code = "city".equals(type)
                        ? place.path("iata_code").asText("")
                        : place.path("iata_city_code").asText(place.path("iata_code").asText(""));
                if (!code.isBlank()) {
                    exactMatches.putIfAbsent(code, place);
                }
            }
        }

        if (exactMatches.size() == 1) {
            return exactMatches.keySet().iterator().next();
        }
        if (exactMatches.size() > 1) {
            String choices = exactMatches.entrySet().stream().limit(4)
                    .map(entry -> entry.getValue().path("name").asText(trimmed) + " (" + entry.getKey() + ")")
                    .reduce((first, next) -> first + ", " + next).orElse("");
            return "NEEDS_CLARIFICATION: '" + trimmed + "' matches multiple airports or cities: " + choices
                    + ". Ask the user which one they mean.";
        }

        return "NEEDS_CLARIFICATION: I couldn't confidently match '" + trimmed
                + "' to one airport or city. Ask the user for the city code or a nearby airport.";
    }
}
