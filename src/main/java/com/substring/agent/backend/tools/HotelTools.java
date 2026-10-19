package com.substring.agent.backend.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.substring.agent.backend.ai_agent_backend.model.Hotel;

@Component
public class HotelTools {
    private final List<Hotel> hotels = List.of(
            new Hotel("Hotel Sunshine", "Goa", 5000, 4.5),
            new Hotel("Hotel Paradise", "Indore", 4000, 4.0),
            new Hotel("Hotel Ocean View", "Rajasthan", 6000, 4.8));

    @Tool(description = "Search for hotels in a city at or below a maximum price per night.")
    public String searchHotel(String city, int maxPricePerNight) {
        StringBuilder result = new StringBuilder();
        for (Hotel hotel : hotels) {
            if (hotel.getCity().equalsIgnoreCase(city) && hotel.getPricePerNight() <= maxPricePerNight) {
                result.append("Hotel: ").append(hotel.getName())
                        .append(", City: ").append(hotel.getCity())
                        .append(", Price per Night: ").append(hotel.getPricePerNight())
                        .append(", Rating: ").append(hotel.getRating()).append("\n");
            }
        }
        return !result.isEmpty() ? result.toString() : "No hotels found for the given criteria.";
    }
}
