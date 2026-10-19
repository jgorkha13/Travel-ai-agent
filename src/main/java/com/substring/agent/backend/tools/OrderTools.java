package com.substring.agent.backend.tools;

import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class OrderTools {
    //fake data
    private final Map<String,String> orders = Map.of(
        "1042",  "Shipped - arriving tomorrow",
         "1043",  "Shipped - arriving in 3 days",
        "1044",  "Shipped - arriving in 5 days",
         "1045",  "Shipped - arriving in 7 days"
    );

    @Tool (description="Get the status of an customer order by its order ID")
    public String getOrderStatus(String orderId) {
        System.out.println("calling getOrderStatus ");
        System.out.println("Fetching order status for Order ID: " + orderId);
        return orders.getOrDefault(orderId, "Order ID not found");
    }
    @Tool(description="Cancel a customer order by its order ID")
    public String cancelOrder(String orderId) {
        if (orders.containsKey(orderId)) {
            System.out.println("calling cancelOrder ");
            System.out.println("Attempting to cancel order with Order ID: " + orderId);
            // In a real application, you would perform the cancellation logic here
            return "Order " + orderId + " has been cancelled.";
        } else {
            return "Order ID not found";
        }
    }
}
