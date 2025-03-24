package com.ignitedatastreamer.main;

import com.ignitedatastreamer.main.model.FlightPlan;
import com.ignitedatastreamer.main.repository.FlightPlanRepository;
import org.apache.ignite.*;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cluster.ClusterMetrics;
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
import com.ignitedatastreamer.main.metrics.DirectMetrics;
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
			// Clear previous data
			clearCache(ignite, "flightPlans");
			clearMongoCollection(mongoTemplate);

			// Initialize metrics container
			DirectMetrics metrics = new DirectMetrics();

			// Test 1: Ignite with write-through
			loadFlightPlans(ignite);
			IgniteCache<String, FlightPlan> cache = ignite.cache("flightPlans");
			simulateFlightOperations(cache);

			// Test 2: Direct MongoDB
			clearMongoCollection(mongoTemplate);
			loadFlightPlansDirect(mongoTemplate, metrics);
			simulateDirectMongoOperations(flightPlanRepository, metrics);

			// Unified logging
			logMetrics(ignite, "performance.log", cache, metrics);
		};
	}

	private void clearCache(Ignite ignite, String cacheName) {
		IgniteCache<?, ?> cache = ignite.cache(cacheName);
		if (cache != null) {
			cache.clear();
			System.out.println("🧹 Cleared cache: " + cacheName);
		} else {
			System.out.println("⚠️ Cache " + cacheName + " does not exist, skipping clear.");
		}
	}

	private void clearMongoCollection(MongoTemplate mongoTemplate) {
		mongoTemplate.dropCollection("flightPlans");
		System.out.println("🧹 Cleared MongoDB collection");
	}

	private void loadFlightPlans(Ignite ignite) {
		try (IgniteDataStreamer<String, FlightPlan> ids = ignite.dataStreamer("flightPlans")) {
			ids.allowOverwrite(true);
			ids.autoFlushFrequency(5000);

			long baseTime = System.currentTimeMillis();

			// Sample Flights
			FlightPlan aa123 = new FlightPlan(
					"AA123", "American Airlines", "JFK", "LAX",
					new Date(baseTime), new Date(baseTime + 3 * 60 * 60 * 1000)
			);
			ids.addData(aa123.getFlightNumber(), aa123);

			FlightPlan dl456 = new FlightPlan(
					"DL456", "Delta Airlines", "ATL", "SFO",
					new Date(baseTime + 30 * 60 * 1000), new Date(baseTime + 4 * 60 * 60 * 1000)
			);
			ids.addData(dl456.getFlightNumber(), dl456);

			// Generate 100,000 flights
			for (int i = 0; i < 100_000; i++) {
				String flightNumber = "FL" + i;
				long departureOffset = i * 15 * 60 * 1000L;
				FlightPlan fp = new FlightPlan(
						flightNumber,
						"Airline " + (i % 5),
						"APT" + (i % 10),
						"APT" + ((i % 10) + 1),
						new Date(baseTime + departureOffset),
						new Date(baseTime + departureOffset + 2 * 60 * 60 * 1000)
				);

				ids.addData(flightNumber, fp);

				if ((i + 1) % 1000 == 0) {
					System.out.println("🔄 Generated and added " + 1000 + " flights (Total: " + (i + 1) + ")");
				}
			}
			ids.flush();
		}
		System.out.println("✅ Successfully loaded flight plans into Ignite");
	}

	private void simulateFlightOperations(IgniteCache<String, FlightPlan> cache) {
		System.out.println("✈️ Starting flight operations simulation...");

		// Bulk reads
		for (int i = 0; i < 50_000; i++) {
			String flightNumber = "FL" + ThreadLocalRandom.current().nextInt(100_000);
			FlightPlan flight = cache.get(flightNumber);
			if (i % 10_000 == 0 && flight != null) {
				System.out.println("🔍 Retrieved flight: " + flight.getFlightNumber());
			}
		}

		// Bulk updates
		for (int i = 0; i < 10_000; i++) {
			String flightNumber = "FL" + i;
			FlightPlan updated = new FlightPlan(
					flightNumber, "Updated Airline", "UPD_ORG", "UPD_DEST",
					new Date(), new Date(System.currentTimeMillis() + 3 * 60 * 60 * 1000)
			);

			cache.put(flightNumber, updated);

			if ((i + 1) % 1000 == 0) {
				System.out.println("🔄 Updated " + 1000 + " flights (Total: " + (i + 1) + ")");
			}
		}

		// Bulk deletions
		for (int i = 0; i < 5_000; i++) {
			String flightNumber = "FL" + (100_000 - i - 1);
			cache.remove(flightNumber);

			if ((i + 1) % 1000 == 0) {
				System.out.println("🗑️ Deleted " + 1000 + " flights (Total: " + (i + 1) + ")");
			}
		}

		System.out.println("✅ Flight operations for Ignite Cache simulation complete");
	}

	private void logMetrics(Ignite ignite, String filename,
							IgniteCache<String, FlightPlan> cache,
							DirectMetrics directMetrics) {
		try {
			Thread.sleep(2000); // Allow final metric updates
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
			// Ignite Metrics
			CacheMetrics igniteMetrics = cache.metrics();
			writer.println("\n===== Ignite Metrics =====");
			writer.println("Timestamp: " + System.currentTimeMillis());
			writer.println("Entries: " + cache.size());
			writer.println("Reads: " + igniteMetrics.getCacheGets());
			writer.println("Writes: " + igniteMetrics.getCachePuts());
			writer.println("Hit Ratio: " + String.format("%.2f%%", igniteMetrics.getCacheHitPercentage()));

			// Direct MongoDB Metrics
			writer.println("\n===== Direct MongoDB Metrics =====");
			writer.println("Data Load Time: " + directMetrics.getLoadTime() + " ms");
			writer.println("Read Time (50k ops): " + directMetrics.getReadTime() + " ms");
			writer.println("Update Time (10k ops): " + directMetrics.getUpdateTime() + " ms");
			writer.println("Delete Time (5k ops): " + directMetrics.getDeleteTime() + " ms");

		} catch (IOException e) {
			System.err.println("Error writing metrics: " + e.getMessage());
		}
	}

	private void loadFlightPlansDirect(MongoTemplate mongoTemplate, DirectMetrics metrics) {
		long startTime = System.currentTimeMillis();
		List<FlightPlan> batch = new ArrayList<>();

		long baseTime = System.currentTimeMillis();

		// Sample flights
		FlightPlan aa123 = new FlightPlan(
				"AA123", "American Airlines", "JFK", "LAX",
				new Date(baseTime), new Date(baseTime + 3 * 60 * 60 * 1000)
		);
		batch.add(aa123);

		FlightPlan dl456 = new FlightPlan(
				"DL456", "Delta Airlines", "ATL", "SFO",
				new Date(baseTime + 30 * 60 * 1000), new Date(baseTime + 4 * 60 * 60 * 1000)
		);
		batch.add(dl456);

		// Generate 100,000 flights
		for (int i = 0; i < 100_000; i++) {
			String flightNumber = "FL" + i;
			long departureOffset = i * 15 * 60 * 1000L;
			FlightPlan fp = new FlightPlan(
					flightNumber,
					"Airline " + (i % 5),
					"APT" + (i % 10),
					"APT" + ((i % 10) + 1),
					new Date(baseTime + departureOffset),
					new Date(baseTime + departureOffset + 2 * 60 * 60 * 1000)
			);
			batch.add(fp);

			if (batch.size() % 1000 == 0) {
				mongoTemplate.insert(batch, FlightPlan.class);
				batch.clear();
			}
		}

		if (!batch.isEmpty()) {
			mongoTemplate.insert(batch, FlightPlan.class);
		}

		metrics.setLoadTime(System.currentTimeMillis() - startTime);
		System.out.println("✅ Direct MongoDB load complete");
	}

	private void simulateDirectMongoOperations(FlightPlanRepository repository, DirectMetrics metrics) {
		System.out.println("✈️ Starting direct MongoDB operations...");

		// Bulk Reads
		long readStart = System.currentTimeMillis();
		for (int i = 0; i < 50_000; i++) {
			String flightNumber = "FL" + ThreadLocalRandom.current().nextInt(100_000);
			FlightPlan flight = repository.findById(flightNumber).orElse(null);

			// Log every 10,000th read
			if (i % 10_000 == 0 && flight != null) {
				System.out.println("🔍 Retrieved flight: " + flight.getFlightNumber());
			}
		}
		metrics.setReadTime(System.currentTimeMillis() - readStart);

		// Bulk Updates
		long updateStart = System.currentTimeMillis();
		for (int i = 0; i < 10_000; i++) {
			String flightNumber = "FL" + i;
			FlightPlan updated = new FlightPlan(
					flightNumber, "Updated Airline", "UPD_ORG", "UPD_DEST",
					new Date(), new Date(System.currentTimeMillis() + 3 * 60 * 60 * 1000)
			);
			repository.save(updated);

			// Log every 1,000 updates
			if ((i + 1) % 1000 == 0) {
				System.out.println("🔄 Updated 1000 flights (Total: " + (i + 1) + ")");
			}
		}
		metrics.setUpdateTime(System.currentTimeMillis() - updateStart);

		// Bulk Deletions
		long deleteStart = System.currentTimeMillis();
		for (int i = 0; i < 5_000; i++) {
			String flightNumber = "FL" + (100_000 - i - 1);
			repository.deleteById(flightNumber);

			// Log every 1,000 deletions
			if ((i + 1) % 1000 == 0) {
				System.out.println("🗑️ Deleted 1000 flights (Total: " + (i + 1) + ")");
			}
		}
		metrics.setDeleteTime(System.currentTimeMillis() - deleteStart);
		System.out.println("✅ Flight operations for MongoDB simulation complete");
	}

	private void logDirectMetrics(DirectMetrics metrics, String filename) {
		try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
			writer.println("\n===== Direct MongoDB Metrics =====");
			writer.println("Data Load Time: " + metrics.getLoadTime() + " ms");
			writer.println("Read Time (50k ops): " + metrics.getReadTime() + " ms");
			writer.println("Update Time (10k ops): " + metrics.getUpdateTime() + " ms");
			writer.println("Delete Time (5k ops): " + metrics.getDeleteTime() + " ms");
			writer.println("===================================");
		} catch (IOException e) {
			System.err.println("Error writing direct metrics: " + e.getMessage());
		}
	}
}