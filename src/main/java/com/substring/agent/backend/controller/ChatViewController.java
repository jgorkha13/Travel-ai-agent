package com.substring.agent.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ChatViewController {

    public record CapabilityItem(String title, String description, String icon, String sampleQuery) {}

    @GetMapping({"/", "/chat"})
    public String chatPage(Model model) {
        model.addAttribute("appName", "Grand Voyager");
        model.addAttribute("appSubtitle", "Private AI Travel & Concierge Desk");
        model.addAttribute("conciergeStatus", "Online & Ready");

        List<CapabilityItem> capabilities = List.of(
            new CapabilityItem(
                "Flight Search Demo",
                "Duffel test data may be synthetic",
                "✈️",
                "Find flights from New York to London for next Friday"
            ),
            new CapabilityItem(
                "Sample Hotels",
                "Local example listings; not live availability",
                "🏨",
                "Search top luxury hotels in Paris under $350 per night"
            ),
            new CapabilityItem(
                "Weather Intelligence",
                "Live conditions and short-range forecasts",
                "🌤️",
                "What is the weather forecast for Rome tomorrow?"
            ),
            new CapabilityItem(
                "Sample Order Lookup",
                "Local demo data; no ticket tracking",
                "📦",
                "Check the status of order #1002"
            )
        );

        model.addAttribute("capabilities", capabilities);

        List<String> starterPrompts = List.of(
            "Find flights from NYC to Paris for next Monday",
            "Recommend luxury hotels in Tokyo under $300 a night",
            "What will the weather be like in Zurich this weekend?",
            "Track booking status for order #1001"
        );

        model.addAttribute("starterPrompts", starterPrompts);

        return "chat";
    }
}
