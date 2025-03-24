package com.ignitedatastreamer.main;

import org.apache.ignite.*;
import org.apache.ignite.cache.*;
import org.apache.ignite.cluster.ClusterMetrics;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.ignitedatastreamer.main.repository.FlightPlanRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootApplication
@EnableMongoRepositories(basePackageClasses = IgniteDemoApplication.class, considerNestedRepositories = true)
public class IgniteDemoApplication {

	@Document(collection = "flightPlans")
	public static class FlightPlan implements Serializable {
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

		public String getFlightNumber() { return flightNumber; }
		public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
		public String getAirline() { return airline; }
		public void setAirline(String airline) { this.airline = airline; }
		public String getOrigin() { return origin; }
		public void setOrigin(String origin) { this.origin = origin; }
		public String getDestination() { return destination; }
		public void setDestination(String destination) { this.destination = destination; }
		public Date getDepartureTime() { return departureTime; }
		public void setDepartureTime(Date departureTime) { this.departureTime = departureTime; }
		public Date getArrivalTime() { return arrivalTime; }
		public void setArrivalTime(Date arrivalTime) { this.arrivalTime = arrivalTime; }
	}

	public interface FlightPlanRepository extends MongoRepository<FlightPlan, String> {
		void deleteByFlightNumber(String flightNumber);
	}

	public static void main(String[] args) {
		SpringApplication.run(IgniteDemoApplication.class, args);
	}

	@Bean
	public ApplicationRunner applicationRunner(Ignite ignite, MongoTemplate mongoTemplate) {
		return args -> {
			clearCache(ignite, "flightPlans");
			clearMongoCollection(mongoTemplate);
			loadFlightPlans(ignite);
			IgniteCache<String, FlightPlan> flightPlanCache = ignite.cache("flightPlans");
			simulateFlightOperations(flightPlanCache);
			logMetrics(ignite, "flight_performance.log", flightPlanCache);
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
		System.out.println("✅ Successfully loaded flight plans into Ignite (write-through to MongoDB)");
	}

	private void simulateFlightOperations(IgniteCache<String, FlightPlan> cache) {
		System.out.println("✈️ Starting flight operations simulation...");

		// Bulk reads (automatically uses read-through)
		for (int i = 0; i < 50_000; i++) {
			String flightNumber = "FL" + ThreadLocalRandom.current().nextInt(100_000);
			FlightPlan flight = cache.get(flightNumber);
			if (i % 10_000 == 0 && flight != null) {
				System.out.println("🔍 Retrieved flight: " + flight.getFlightNumber());
			}
		}

		// Bulk updates (automatically uses write-through)
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

		// Bulk deletions (automatically uses write-through)
		for (int i = 0; i < 5_000; i++) {
			String flightNumber = "FL" + (100_000 - i - 1);
			cache.remove(flightNumber);

			if ((i + 1) % 1000 == 0) {
				System.out.println("🗑️ Deleted " + 1000 + " flights (Total: " + (i + 1) + ")");
			}
		}

		System.out.println("✅ Flight operations simulation complete");
	}

	private void logMetrics(Ignite ignite, String filename, IgniteCache<String, FlightPlan> cache) {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		CacheMetrics metrics = cache.metrics();
		try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
			writer.println("===== Flight Metrics Report =====");
			writer.println("Timestamp: " + System.currentTimeMillis());

			writer.println("\n=== Cluster Metrics ===");
			ClusterMetrics clusterMetrics = ignite.cluster().metrics();
			writer.println("CPU Load: " + String.format("%.2f%%", clusterMetrics.getCurrentCpuLoad() * 100));
			writer.println("Heap Used: " + (clusterMetrics.getHeapMemoryUsed() / (1024 * 1024)) + " MB");

			writer.println("\n=== Cache Metrics ===");
			writer.println("Entries: " + cache.size());
			writer.println("Reads: " + metrics.getCacheGets());
			writer.println("Writes: " + metrics.getCachePuts());
			writer.println("Hit Ratio: " + String.format("%.2f%%", metrics.getCacheHitPercentage()));

			writer.println("\n=====================================");
		} catch (IOException e) {
			System.err.println("Error writing metrics: " + e.getMessage());
		}
	}
}