package com.substring.agent.backend.tools;

import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class InventoryTools {
    private final Map<String,Integer> stock = Map.of(
        "Bluetooth Headphones",5,
        "Wireless Mouse",10,
        "Mechanical Keyboard",3,
        "USB-C Hub",7,
        "Webcam",4,
        "External Hard Drive",6
    );
    @Tool(description = "Check the available stock quantity for a product")
    public String checkStock(String productName) {
        System.out.println("calling checkStock Tool");
        System.out.println("Checking stock for product: " + productName);
        Integer quantity = stock.get(productName);
        if (quantity == null) {
            return "Product not found";
        } else if (quantity > 0) {
            return "In Stock: " + quantity + " units available";
        } else {
            return "Out of Stock";
        }
    }

    @Tool(description = "Get the number of product types tracked in inventory")
    public String getTotalProductsInStock() {
        System.out.println("calling getTotalProductsInStock Tool");
        int totalProducts = stock.size();
        return "Total Products in Stock: " + totalProducts;
    }

    @Tool(description="Get a list of all products currently in stock")
    public java.util.List<String> getAllProductsInStock() {
        System.out.println("calling getAllProductsInStock Tool");
        return stock.keySet().stream().filter(name -> stock.get(name) > 0).collect(java.util.stream.Collectors.toList());
    }
}
