package com.substring.agent.backend.tools;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class WeatherTools {
    private final RestClient restClient;

    public WeatherTools() {
        this.restClient = RestClient.create();
    }

    @Tool(description = "Get current weather conditions from Open-Meteo when the date is today, or a daily weather forecast for a future date. Provide a specific city (optionally with country, such as Zurich, Switzerland) and date in YYYY-MM-DD format. Forecasts are available up to 16 days ahead.")
    public String getForecast(String city, String date) {
        if (city == null || city.isBlank()) {
            return "Please provide a city for the weather forecast.";
        }

        final LocalDate forecastDate;
        try {
            forecastDate = LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException exception) {
            return "Please provide the forecast date in YYYY-MM-DD format.";
        }

        try {
            JsonNode geocoding = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("geocoding-api.open-meteo.com")
                            .path("/v1/search")
                            .queryParam("name", city.trim())
                            .queryParam("count", 10)
                            .queryParam("language", "en")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode results = geocoding == null ? null : geocoding.path("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                return "No matching city was found. Ask the user to check the city name and include its country.";
            }

            String normalizedCity = city.trim().toLowerCase(Locale.ROOT);
            String qualifier = normalizedCity.contains(",")
                    ? normalizedCity.substring(normalizedCity.indexOf(',') + 1).split(",", 2)[0].trim()
                    : "";
            boolean hasAdminArea = false;
            boolean hasCountry = false;
            for (JsonNode result : results) {
                String admin1 = result.path("admin1").asText("");
                String resultCountry = result.path("country").asText("");
                hasAdminArea |= !admin1.isBlank() && qualifier.contains(admin1.toLowerCase(Locale.ROOT));
                hasCountry |= !resultCountry.isBlank() && qualifier.contains(resultCountry.toLowerCase(Locale.ROOT));
            }

            List<JsonNode> candidates = new ArrayList<>();
            Map<String, JsonNode> regions = new LinkedHashMap<>();
            for (JsonNode result : results) {
                String admin1 = result.path("admin1").asText("");
                String resultCountry = result.path("country").asText("");
                if (hasAdminArea && !normalizedCity.contains(admin1.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (!hasAdminArea && hasCountry
                        && !normalizedCity.contains(resultCountry.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                candidates.add(result);
                regions.putIfAbsent(admin1 + ", " + resultCountry, result);
            }

            if (regions.size() > 1) {
                String choices = regions.keySet().stream().limit(4).reduce((first, next) -> first + "; " + next).orElse("");
                return "The place name '" + city + "' matches several locations: " + choices
                        + ". Ask the user which state/region and country they mean before getting weather.";
            }
            if (candidates.isEmpty()) {
                return "No matching city was found. Ask the user to check the city name and include its state/region and country.";
            }

            JsonNode location = candidates.get(0);
            double latitude = location.path("latitude").asDouble();
            double longitude = location.path("longitude").asDouble();
            String locationName = location.path("name").asText(city.trim());
            String country = location.path("country").asText("");
            ZoneId locationZone = ZoneId.of(location.path("timezone").asText("UTC"));
            LocalDate localToday = LocalDate.now(locationZone);
            if (forecastDate.isBefore(localToday) || forecastDate.isAfter(localToday.plusDays(15))) {
                return "Open-Meteo provides forecasts from today through 15 days ahead for this location. "
                        + "The requested date " + forecastDate + " is outside that forecast window.";
            }

            String place = country.isBlank() ? locationName : locationName + ", " + country;
            if (forecastDate.equals(localToday)) {
                JsonNode currentResponse = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("api.open-meteo.com")
                                .path("/v1/forecast")
                                .queryParam("latitude", latitude)
                                .queryParam("longitude", longitude)
                                .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m")
                                .queryParam("timezone", "auto")
                                .build())
                        .retrieve()
                        .body(JsonNode.class);

                JsonNode current = currentResponse == null ? null : currentResponse.path("current");
                if (current == null || current.path("time").isMissingNode()) {
                    return "Open-Meteo did not return current conditions for " + place + ".";
                }
                return "Current conditions from Open-Meteo for " + place + " at " + current.path("time").asText()
                        + " local time: " + weatherDescription(current.path("weather_code").asInt(-1))
                        + ", temperature " + current.path("temperature_2m").asText("unavailable") + "°C"
                        + ", feels like " + current.path("apparent_temperature").asText("unavailable") + "°C"
                        + ", humidity " + current.path("relative_humidity_2m").asText("unavailable") + "%"
                        + ", precipitation " + current.path("precipitation").asText("unavailable") + " mm"
                        + ", wind " + current.path("wind_speed_10m").asText("unavailable") + " km/h"
                        + ". Retrieved at " + Instant.now() + ".";
            }

            JsonNode forecast = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.open-meteo.com")
                            .path("/v1/forecast")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                            .queryParam("timezone", "auto")
                            .queryParam("start_date", forecastDate)
                            .queryParam("end_date", forecastDate)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode daily = forecast == null ? null : forecast.path("daily");
            JsonNode times = daily == null ? null : daily.path("time");
            if (times == null || !times.isArray() || times.isEmpty()) {
                return "Open-Meteo did not return a forecast for " + forecastDate
                        + ". Forecasts are generally available up to 16 days ahead.";
            }

            return "Open-Meteo forecast for " + place + " on " + times.get(0).asText()
                    + ": " + weatherDescription(daily.path("weather_code").path(0).asInt(-1))
                    + ", low " + daily.path("temperature_2m_min").path(0).asText("unavailable") + "°C"
                    + ", high " + daily.path("temperature_2m_max").path(0).asText("unavailable") + "°C"
                    + ", maximum precipitation probability "
                    + daily.path("precipitation_probability_max").path(0).asText("unavailable") + "%"
                    + ". Retrieved at " + Instant.now() + ". Forecasts can change.";
        } catch (RestClientException exception) {
            return "Could not retrieve a forecast from Open-Meteo right now. Do not guess the weather; let the user know and suggest trying again.";
        }
    }

    private String weatherDescription(int code) {
        return switch (code) {
            case 0 -> "clear sky";
            case 1 -> "mainly clear";
            case 2 -> "partly cloudy";
            case 3 -> "overcast";
            case 45, 48 -> "fog";
            case 51, 53, 55 -> "drizzle";
            case 56, 57 -> "freezing drizzle";
            case 61, 63, 65 -> "rain";
            case 66, 67 -> "freezing rain";
            case 71, 73, 75, 77 -> "snow";
            case 80, 81, 82 -> "rain showers";
            case 85, 86 -> "snow showers";
            case 95, 96, 99 -> "thunderstorm";
            default -> "weather conditions unavailable";
        };
    }
}
