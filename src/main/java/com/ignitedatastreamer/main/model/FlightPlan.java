package com.ignitedatastreamer.main.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.io.Serializable;
import java.util.Date;

@Document(collection = "flightPlans")
public class FlightPlan implements Serializable {
    @Id
    private String flightNumber;
    private String airline;
    private String origin;
    private String destination;
    private Date departureTime;
    private Date arrivalTime;

    public FlightPlan() {
    }

    public FlightPlan(String flightNumber, String airline, String origin,
                     String destination, Date departureTime, Date arrivalTime) {
        this.flightNumber = flightNumber;
        this.airline = airline;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    // Getters and Setters (same as before)
}