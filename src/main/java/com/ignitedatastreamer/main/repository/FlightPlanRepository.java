package com.ignitedatastreamer.main.repository;

import com.ignitedatastreamer.main.IgniteDemoApplication.FlightPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface FlightPlanRepository extends MongoRepository<FlightPlan, String> {
    @Query("{ 'flightNumber' : ?0 }")
    void deleteByFlightNumber(String flightNumber);
}