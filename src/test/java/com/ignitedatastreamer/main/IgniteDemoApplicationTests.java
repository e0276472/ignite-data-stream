package com.ignitedatastreamer.main;

import com.ignitedatastreamer.main.repository.FlightPlanRepository;
import org.apache.ignite.Ignite;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IgniteDemoApplicationTests {

	@MockBean
	private Ignite ignite;

	@Autowired
	private FlightPlanRepository flightPlanRepository;

	@Test
	void contextLoads() {
		// Test context loading without initializing the real Ignite node.
	}

	@Test
	void testFlightPlanRepository() {
		// Example repository test
		IgniteDemoApplication.FlightPlan flightPlan = new IgniteDemoApplication.FlightPlan(
				"TEST123", "Test Airline", "AAA", "BBB",
				new java.util.Date(), new java.util.Date(System.currentTimeMillis() + 7200000)
		);
		flightPlanRepository.save(flightPlan);
		IgniteDemoApplication.FlightPlan retrieved = flightPlanRepository.findById("TEST123").orElse(null);
		assertThat(retrieved).isNotNull();
		assertThat(retrieved.getAirline()).isEqualTo("Test Airline");
		flightPlanRepository.deleteById("TEST123");
	}
}
