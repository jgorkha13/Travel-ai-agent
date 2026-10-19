package com.substring.agent.backend.config;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.substring.agent.backend.tools.FlightTools;
import com.substring.agent.backend.tools.HotelTools;
import com.substring.agent.backend.tools.InventoryTools;
import com.substring.agent.backend.tools.OrderTools;
import com.substring.agent.backend.tools.WeatherTools;

@Configuration
public class AiConfig {

    private final OrderTools tools;
    private final InventoryTools inventoryTools;


    private final FlightTools flightTools;
    private final HotelTools hotelTools;
    private final WeatherTools weatherTools;
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
    }

    public AiConfig(OrderTools tools, InventoryTools inventoryTools, FlightTools flightTools,
            HotelTools hotelTools, WeatherTools weatherTools) {
        this.tools = tools;
        this.inventoryTools = inventoryTools;
        this.flightTools = flightTools;
        this.hotelTools = hotelTools;
        this.weatherTools = weatherTools;
    }

    // Configuration beans and settings can be defined here
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        String today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString();
        return builder.defaultSystem("""
                You are Grand Voyager, a friendly travel concierge. Today's date is %s in the Asia/Kolkata time zone. Interpret relative dates such as today, tomorrow, and next Friday from this date. Help with travel planning, destination ideas, itineraries, packing, transportation, and general travel questions. Answer general questions directly and naturally; the available tools do not limit the topics you can discuss.

                Use the flight search tool only when the user wants available flight options, and ask for any missing route or travel date instead of guessing. Use the hotel search tool when the user wants stays; ask for the city and budget if either is missing. Use the weather tool for a city and date, and the order tool for a booking or order status. Do not call unrelated inventory tools for travel questions.

                The flight tool searches live airline offers through Duffel when DUFFEL_ACCESS_TOKEN is configured. It currently searches one-way in economy for one adult only; it does not book flights. If the token is missing, explain that live search needs Duffel setup and do not use sample flights. Ask for missing origin, destination, or departure date. For a return journey, explain that this first search supports one-way only. Use ISO date format (YYYY-MM-DD), and never describe an offer price as guaranteed because offers can change.

                The hotel tool still contains sample data and is not connected to a live provider. For weather, call the Open-Meteo tool whenever the user asks about current or forecast weather, after confirming the specific city and date. Use ISO date format (YYYY-MM-DD). If the user gives only a country or region, ask which city they mean. If a city name matches multiple locations, ask the user to choose the state/region and country before reporting weather. Forecasts are available up to 16 days ahead; if the tool cannot provide data, say so and do not guess. Report the weather description and numeric values exactly as the tool returns them; never substitute remembered or invented weather. Distinguish current conditions from a future forecast, and include the local observation/forecast time when provided. Never describe sample hotel results as live availability or current prices. Do not invent bookings or claim that an action was completed unless a tool confirms it.

                Keep answers concise, useful, and warm. Ask one focused follow-up question when needed. For facts that can change, such as entry rules, prices, schedules, and local conditions, tell the user to verify with an official or live source.
                """.formatted(today))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(tools, inventoryTools, flightTools, hotelTools, weatherTools)
                .build();
    }
}
