package com.substring.agent.backend.ai_agent_backend.model;

public class Hotel {
    private final String name;
    private final String city;
    private final int pricePerNight;
    private final double rating;

    public Hotel(String name, String city, int pricePerNight, double rating) {
        this.name = name;
        this.city = city;
        this.pricePerNight = pricePerNight;
        this.rating = rating;
    }

    public String getName() { return name; }
    public String getCity() { return city; }
    public int getPricePerNight() { return pricePerNight; }
    public double getRating() { return rating; }
}
