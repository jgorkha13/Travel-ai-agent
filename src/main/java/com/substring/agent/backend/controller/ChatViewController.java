package com.substring.agent.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ChatViewController {

    public record CapabilityItem(String title, String description, String icon, String sampleQuery) {}
    public record FaqItem(String category, String question, String prompt) {}

    @GetMapping({"/", "/chat"})
    public String chatPage(Model model) {
        model.addAttribute("appName", "Grand Voyager");
        model.addAttribute("appSubtitle", "Private AI Travel & Concierge Desk");
        model.addAttribute("conciergeStatus", "Online & Ready");

        List<CapabilityItem> capabilities = List.of(
            new CapabilityItem(
                "Trip Planning",
                "Ideas, itineraries, and travel tips",
                "🧭",
                "Plan a three-day itinerary for Rome, Italy"
            ),
            new CapabilityItem(
                "Sample Hotels",
                "Three local examples; not live availability",
                "🏨",
                "Show sample hotel listings in Goa priced at 5000 or less per night"
            ),
            new CapabilityItem(
                "Weather Intelligence",
                "Current conditions and forecasts up to 15 days",
                "🌤️",
                "What is the current weather in Rome, Italy?"
            ),
            new CapabilityItem(
                "Demo Order Lookup",
                "Try sample order 1042; no real booking data",
                "📦",
                "Look up demo order 1042 and tell me its current status"
            )
        );

        model.addAttribute("capabilities", capabilities);

        List<FaqItem> faqs = List.of(
            new FaqItem("Trip planning", "Plan a three-day trip to Rome", "Plan a three-day itinerary for Rome, Italy."),
            new FaqItem("Live weather", "What is today's weather in Rome?", "What is the current weather in Rome, Italy?"),
            new FaqItem("Sample hotels", "Show hotels in Goa under ₹5,000 a night", "Show sample hotel listings in Goa priced at 5000 or less per night."),
            new FaqItem("Demo order", "Where is demo order 1042?", "Look up demo order 1042 and tell me its current status.")
        );

        model.addAttribute("faqs", faqs);

        return "chat";
    }
}
