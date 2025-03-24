package com.ignitedatastreamer.main;

import com.ignitedatastreamer.main.model.FlightPlan; // Correct import
import com.ignitedatastreamer.main.repository.FlightPlanRepository;
import org.apache.ignite.Ignite;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureDataMongo
class IgniteDemoApplicationTests {

	@MockBean
	private Ignite ignite;

	@Autowired
	private FlightPlanRepository flightPlanRepository;

	@Test
	void contextLoads() {
		// Test context loading
	}

	@Test
	void testFlightPlanRepository() {
		// Use the FlightPlan from model package
		FlightPlan flightPlan = new FlightPlan(
				"TEST123",
				"Test Airline",
				"AAA",
				"BBB",
				new Date(),
				new Date(System.currentTimeMillis() + 7200000)
		);

		flightPlanRepository.save(flightPlan);
		FlightPlan retrieved = flightPlanRepository.findById("TEST123").orElse(null);
		assertThat(retrieved).isNotNull();
		assertThat(retrieved.getAirline()).isEqualTo("Test Airline");
		flightPlanRepository.deleteById("TEST123");
	}
}