package com.substring.agent.backend.ai_agent_backend.model;

public class Flight {
    private final String airline;
    private final String source;
    private final String destination;
    private final String date;
    private final int price;

    public Flight(String airline, String source, String destination, String date, int price) {
        this.airline = airline;
        this.source = source;
        this.destination = destination;
        this.date = date;
        this.price = price;
    }

    public String getAirline() { return airline; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public String getDate() { return date; }
    public int getPrice() { return price; }
}
