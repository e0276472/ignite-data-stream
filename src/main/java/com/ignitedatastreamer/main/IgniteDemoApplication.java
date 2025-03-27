package com.ignitedatastreamer.main;

import com.ignitedatastreamer.main.metrics.PerformanceMetrics;
import com.ignitedatastreamer.main.model.FlightPlan;
import com.ignitedatastreamer.main.repository.FlightPlanRepository;
import org.apache.ignite.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
@EnableMongoRepositories(basePackageClasses = IgniteDemoApplication.class)
public class IgniteDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(IgniteDemoApplication.class, args);
	}

	@Bean
	public ApplicationRunner applicationRunner(Ignite ignite,
											   MongoTemplate mongoTemplate,
											   FlightPlanRepository flightPlanRepository) {
		return args -> {
			PerformanceMetrics metrics = new PerformanceMetrics();

			// Test Ignite with write-through
			testIgniteOperations(ignite, metrics);

			// Test Direct MongoDB
			testMongoDBOperations(mongoTemplate, flightPlanRepository, metrics);

			// Log results
			logMetrics(metrics, "performance.log");
		};
	}

	private void testIgniteOperations(Ignite ignite, PerformanceMetrics metrics) {
		clearCache(ignite, "flightPlans");
		loadFlightPlans(ignite, metrics);
		IgniteCache<String, FlightPlan> cache = ignite.cache("flightPlans");
		simulateFlightOperations(cache, metrics);
	}

	private void testMongoDBOperations(MongoTemplate mongoTemplate,
									   FlightPlanRepository repository,
									   PerformanceMetrics metrics) {
		clearMongoCollection(mongoTemplate);
		loadFlightPlansDirect(mongoTemplate, metrics);
		simulateDirectMongoOperations(repository, metrics);
	}

	private void clearCache(Ignite ignite, String cacheName) {
		IgniteCache<?, ?> cache = ignite.cache(cacheName);
		if (cache != null) {
			cache.clear();
			System.out.println("🧹 Cleared Ignite cache: " + cacheName);
		}
	}

	private void clearMongoCollection(MongoTemplate mongoTemplate) {
		mongoTemplate.dropCollection("flightPlans");
		System.out.println("🧹 Cleared MongoDB collection");
	}

	private void loadSampleFlights(IgniteDataStreamer<String, FlightPlan> streamer, long baseTime) {
		// Sample flight AA123
		FlightPlan aa123 = createFlightPlan(
				"AA123",
				"American Airlines",
				"JFK",
				"LAX",
				baseTime,
				3 // hours offset
		);
		streamer.addData(aa123.getFlightNumber(), aa123);

		// Sample flight DL456
		FlightPlan dl456 = createFlightPlan(
				"DL456",
				"Delta Airlines",
				"ATL",
				"SFO",
				baseTime + 30 * 60 * 1000L, // 30 minutes offset
				4 // hours offset
		);
		streamer.addData(dl456.getFlightNumber(), dl456);

		System.out.println("✈️ Added sample flights: AA123, DL456");
	}

	private void loadFlightPlans(Ignite ignite, PerformanceMetrics metrics) {
		long startTime = System.currentTimeMillis();

		try (IgniteDataStreamer<String, FlightPlan> streamer = ignite.dataStreamer("flightPlans")) {
			streamer.allowOverwrite(true);
			streamer.autoFlushFrequency(5000);

			long baseTime = System.currentTimeMillis();

			// Load sample flights
			loadSampleFlights(streamer, baseTime);

			// Generate 100,000 flights
			for (int i = 0; i < 100_000; i++) {
				FlightPlan fp = createFlightPlan(i, baseTime);
				streamer.addData(fp.getFlightNumber(), fp);

				if ((i + 1) % 1000 == 0) {
					System.out.println("🔄 Ignite: Added 1000 flights (Total: " + (i + 1) + ")");
				}
			}
			streamer.flush();
		}

		metrics.setIgniteLoadTime(System.currentTimeMillis() - startTime);
		System.out.println("✅ Ignite data load completed");
	}

	private void simulateFlightOperations(IgniteCache<String, FlightPlan> cache, PerformanceMetrics metrics) {
		System.out.println("✈️ Starting Ignite operations...");

		// Bulk reads
		long readStart = System.currentTimeMillis();
		performReadOperations(cache);
		metrics.setIgniteReadTime(System.currentTimeMillis() - readStart);

		// Bulk updates
		long updateStart = System.currentTimeMillis();
		performUpdateOperations(cache);
		metrics.setIgniteUpdateTime(System.currentTimeMillis() - updateStart);

		// Bulk deletions
		long deleteStart = System.currentTimeMillis();
		performDeleteOperations(cache);
		metrics.setIgniteDeleteTime(System.currentTimeMillis() - deleteStart);

		System.out.println("✅ Ignite operations completed");
	}

	private void loadFlightPlansDirect(MongoTemplate mongoTemplate, PerformanceMetrics metrics) {
		long startTime = System.currentTimeMillis();
		List<FlightPlan> batch = new ArrayList<>();
		long baseTime = System.currentTimeMillis();

		// Load sample flights
		batch.add(createFlightPlan("AA123", "American Airlines", "JFK", "LAX", baseTime, 3));
		batch.add(createFlightPlan("DL456", "Delta Airlines", "ATL", "SFO", baseTime + 1800000L, 4));

		// Generate 100,000 flights
		for (int i = 0; i < 100_000; i++) {
			batch.add(createFlightPlan(i, baseTime));

			if (batch.size() % 1000 == 0) {
				mongoTemplate.insert(batch, FlightPlan.class);
				batch.clear();
				System.out.println("🔄 MongoDB: Added 1000 flights (Total: " + (i + 1) + ")");
			}
		}

		if (!batch.isEmpty()) {
			mongoTemplate.insert(batch, FlightPlan.class);
		}

		metrics.setMongoLoadTime(System.currentTimeMillis() - startTime);
		System.out.println("✅ MongoDB data load completed");
	}

	private void simulateDirectMongoOperations(FlightPlanRepository repository, PerformanceMetrics metrics) {
		System.out.println("✈️ Starting MongoDB operations...");

		// Bulk reads
		long readStart = System.currentTimeMillis();
		performMongoReads(repository);
		metrics.setMongoReadTime(System.currentTimeMillis() - readStart);

		// Bulk updates
		long updateStart = System.currentTimeMillis();
		performMongoUpdates(repository);
		metrics.setMongoUpdateTime(System.currentTimeMillis() - updateStart);

		// Bulk deletions
		long deleteStart = System.currentTimeMillis();
		performMongoDeletions(repository);
		metrics.setMongoDeleteTime(System.currentTimeMillis() - deleteStart);

		System.out.println("✅ MongoDB operations completed");
	}

	// Helper methods
	private FlightPlan createFlightPlan(int index, long baseTime) {
		String flightNumber = "FL" + index;
		long departureOffset = index * 900000L; // 15 minutes in milliseconds
		return new FlightPlan(
				flightNumber,
				"Airline " + (index % 5),
				"APT" + (index % 10),
				"APT" + ((index % 10) + 1),
				new Date(baseTime + departureOffset),
				new Date(baseTime + departureOffset + 7200000L) // +2 hours
		);
	}

	private FlightPlan createFlightPlan(String number, String airline,
										String origin, String destination,
										long baseTime, int hoursOffset) {
		return new FlightPlan(
				number,
				airline,
				origin,
				destination,
				new Date(baseTime),
				new Date(baseTime + hoursOffset * 3600000L)
		);
	}

	private void performReadOperations(IgniteCache<String, FlightPlan> cache) {
		for (int i = 0; i < 50_000; i++) {
			String flightNumber = "FL" + ThreadLocalRandom.current().nextInt(100_000);
			FlightPlan flight = cache.get(flightNumber);
			if (i % 10_000 == 0 && flight != null) {
				System.out.println("🔍 Ignite Read: " + flight.getFlightNumber());
			}
		}
	}

	private void performUpdateOperations(IgniteCache<String, FlightPlan> cache) {
		for (int i = 0; i < 10_000; i++) {
			String flightNumber = "FL" + i;
			FlightPlan updated = new FlightPlan(
					flightNumber, "Updated Airline", "UPD_ORG", "UPD_DEST",
					new Date(), new Date(System.currentTimeMillis() + 10800000L)
			);
			cache.put(flightNumber, updated);
			if ((i + 1) % 1000 == 0) {
				System.out.println("🔄 Ignite Update: " + (i + 1));
			}
		}
	}

	private void performDeleteOperations(IgniteCache<String, FlightPlan> cache) {
		for (int i = 0; i < 5_000; i++) {
			String flightNumber = "FL" + (99_999 - i);
			cache.remove(flightNumber);
			if ((i + 1) % 1000 == 0) {
				System.out.println("🗑️ Ignite Delete: " + (i + 1));
			}
		}
	}

	private void performMongoReads(FlightPlanRepository repository) {
		for (int i = 0; i < 50_000; i++) {
			String flightNumber = "FL" + ThreadLocalRandom.current().nextInt(100_000);
			FlightPlan flight = repository.findById(flightNumber).orElse(null);
			if (i % 10_000 == 0 && flight != null) {
				System.out.println("🔍 MongoDB Read: " + flight.getFlightNumber());
			}
		}
	}

	private void performMongoUpdates(FlightPlanRepository repository) {
		for (int i = 0; i < 10_000; i++) {
			String flightNumber = "FL" + i;
			FlightPlan updated = new FlightPlan(
					flightNumber, "Updated Airline", "UPD_ORG", "UPD_DEST",
					new Date(), new Date(System.currentTimeMillis() + 10800000L)
			);
			repository.save(updated);
			if ((i + 1) % 1000 == 0) {
				System.out.println("🔄 MongoDB Update: " + (i + 1));
			}
		}
	}

	private void performMongoDeletions(FlightPlanRepository repository) {
		for (int i = 0; i < 5_000; i++) {
			String flightNumber = "FL" + (99_999 - i);
			repository.deleteById(flightNumber);
			if ((i + 1) % 1000 == 0) {
				System.out.println("🗑️ MongoDB Delete: " + (i + 1));
			}
		}
	}

	private void logMetrics(PerformanceMetrics metrics, String filename) {
		try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
			writer.println("\n===== Performance Report =====");
			writer.println("Generated at: " + new Date());

			writer.println("\n=== Ignite Performance ===");
			writer.println("Data Load Time: " + metrics.getIgniteLoadTime() + " ms");
			writer.println("Read Time (50k ops): " + metrics.getIgniteReadTime() + " ms");
			writer.println("Update Time (10k ops): " + metrics.getIgniteUpdateTime() + " ms");
			writer.println("Delete Time (5k ops): " + metrics.getIgniteDeleteTime() + " ms");

			writer.println("\n=== MongoDB Performance ===");
			writer.println("Data Load Time: " + metrics.getMongoLoadTime() + " ms");
			writer.println("Read Time (50k ops): " + metrics.getMongoReadTime() + " ms");
			writer.println("Update Time (10k ops): " + metrics.getMongoUpdateTime() + " ms");
			writer.println("Delete Time (5k ops): " + metrics.getMongoDeleteTime() + " ms");

			writer.println("\n" + "-".repeat(50));
		} catch (IOException e) {
			System.err.println("Error writing metrics: " + e.getMessage());
		}
	}
}