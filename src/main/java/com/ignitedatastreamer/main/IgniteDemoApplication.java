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

	// Core operations (unchanged)
	private void clearCache(Ignite ignite, String cacheName) {
		IgniteCache<?, ?> cache = ignite.cache(cacheName);
		if (cache != null) cache.clear();
	}

	private void clearMongoCollection(MongoTemplate mongoTemplate) {
		mongoTemplate.dropCollection("flightPlans");
	}

	// Ignite data loading with metrics
	private void loadFlightPlans(Ignite ignite, PerformanceMetrics metrics) {
		long start = System.currentTimeMillis();
		try (IgniteDataStreamer<String, FlightPlan> streamer = ignite.dataStreamer("flightPlans")) {
			streamer.allowOverwrite(true);
			streamer.autoFlushFrequency(5000);

			long baseTime = System.currentTimeMillis();
			for (int i = 0; i < 100_000; i++) {
				FlightPlan fp = createFlightPlan(i, baseTime);
				streamer.addData(fp.getFlightNumber(), fp);

				if ((i + 1) % 1000 == 0) {
					System.out.println("🔄 Ignite: Added 1000 flights (Total: " + (i + 1) + ")");
				}
			}
			streamer.flush();
		}
		metrics.setIgniteLoadTime(System.currentTimeMillis() - start);
	}

	// MongoDB data loading with clean batches
	private void loadFlightPlansDirect(MongoTemplate mongoTemplate, PerformanceMetrics metrics) {
		long start = System.currentTimeMillis();
		List<FlightPlan> batch = new ArrayList<>();
		long baseTime = System.currentTimeMillis();

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

		metrics.setMongoLoadTime(System.currentTimeMillis() - start);
	}

	// Flight operations with precise timing
	private void simulateFlightOperations(IgniteCache<String, FlightPlan> cache, PerformanceMetrics metrics) {
		// Reads
		long readStart = System.currentTimeMillis();
		performReadOperations(cache);
		metrics.setIgniteReadTime(System.currentTimeMillis() - readStart);

		// Updates
		long updateStart = System.currentTimeMillis();
		performUpdateOperations(cache);
		metrics.setIgniteUpdateTime(System.currentTimeMillis() - updateStart);

		// Deletes
		long deleteStart = System.currentTimeMillis();
		performDeleteOperations(cache);
		metrics.setIgniteDeleteTime(System.currentTimeMillis() - deleteStart);
	}

	private void simulateDirectMongoOperations(FlightPlanRepository repository, PerformanceMetrics metrics) {
		// Reads
		long readStart = System.currentTimeMillis();
		performMongoReads(repository);
		metrics.setMongoReadTime(System.currentTimeMillis() - readStart);

		// Updates
		long updateStart = System.currentTimeMillis();
		performMongoUpdates(repository);
		metrics.setMongoUpdateTime(System.currentTimeMillis() - updateStart);

		// Deletes
		long deleteStart = System.currentTimeMillis();
		performMongoDeletions(repository);
		metrics.setMongoDeleteTime(System.currentTimeMillis() - deleteStart);
	}

	// Helper methods
	private FlightPlan createFlightPlan(int index, long baseTime) {
		String flightNumber = "FL" + index;
		long departureOffset = index * 900_000L; // 15 minute intervals
		return new FlightPlan(
				flightNumber,
				"Airline " + (index % 5),
				"APT" + (index % 10),
				"APT" + ((index % 10) + 1),
				new Date(baseTime + departureOffset),
				new Date(baseTime + departureOffset + 7_200_000L) // +2 hours
		);
	}

	// Operation implementations
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
			cache.put(flightNumber, createUpdatedFlight(flightNumber));
			if ((i + 1) % 1000 == 0) {
				System.out.println("🔄 Ignite Updated: " + (i + 1));
			}
		}
	}

	private void performDeleteOperations(IgniteCache<String, FlightPlan> cache) {
		for (int i = 0; i < 5_000; i++) {
			String flightNumber = "FL" + (99_999 - i);
			cache.remove(flightNumber);
			if ((i + 1) % 1000 == 0) {
				System.out.println("🗑️ Ignite Deleted: " + (i + 1));
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
			repository.save(createUpdatedFlight(flightNumber));
			if ((i + 1) % 1000 == 0) {
				System.out.println("🔄 MongoDB Updated: " + (i + 1));
			}
		}
	}

	private void performMongoDeletions(FlightPlanRepository repository) {
		for (int i = 0; i < 5_000; i++) {
			String flightNumber = "FL" + (99_999 - i);
			repository.deleteById(flightNumber);
			if ((i + 1) % 1000 == 0) {
				System.out.println("🗑️ MongoDB Deleted: " + (i + 1));
			}
		}
	}

	private FlightPlan createUpdatedFlight(String flightNumber) {
		return new FlightPlan(
				flightNumber,
				"Updated Airline",
				"UPD_ORG",
				"UPD_DEST",
				new Date(),
				new Date(System.currentTimeMillis() + 10_800_000L) // +3 hours
		);
	}

	// Metrics logging
	private void logMetrics(PerformanceMetrics metrics, String filename) {
		try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
			// Header
			writer.println("===== Performance Report =====");
			writer.println("Generated at: " + new Date());

			// Ignite Section
			writer.println("\n=== Ignite Performance ===");
			writer.println("Data Load Time: " + metrics.getIgniteLoadTime() + " ms");
			writer.println("Read Time (50k ops): " + metrics.getIgniteReadTime() + " ms");
			writer.println("Update Time (10k ops): " + metrics.getIgniteUpdateTime() + " ms");
			writer.println("Delete Time (5k ops): " + metrics.getIgniteDeleteTime() + " ms");

			// MongoDB Section
			writer.println("\n=== MongoDB Performance ===");
			writer.println("Data Load Time: " + metrics.getMongoLoadTime() + " ms");
			writer.println("Read Time (50k ops): " + metrics.getMongoReadTime() + " ms");
			writer.println("Update Time (10k ops): " + metrics.getMongoUpdateTime() + " ms");
			writer.println("Delete Time (5k ops): " + metrics.getMongoDeleteTime() + " ms");

			// Footer
			writer.println("\n" + "=".repeat(50));
		} catch (IOException e) {
			System.err.println("Error writing metrics: " + e.getMessage());
		}
	}
}