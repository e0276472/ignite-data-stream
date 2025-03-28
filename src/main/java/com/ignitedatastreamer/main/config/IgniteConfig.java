package com.ignitedatastreamer.main.config;

import com.ignitedatastreamer.main.model.FlightPlan;
import com.ignitedatastreamer.main.store.MongoCacheStore;
import com.ignitedatastreamer.main.util.ApplicationContextHolder;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IgniteConfig {

    @Bean
    public Ignite ignite() {
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Configure cache for FlightPlan objects
        CacheConfiguration<String, FlightPlan> flightCfg = new CacheConfiguration<>();
        flightCfg.setName("flightPlans");
        flightCfg.setCacheMode(CacheMode.PARTITIONED);
        flightCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        flightCfg.setStatisticsEnabled(true); // For metrics collection

        // MongoDB integration
        flightCfg.setCacheStoreFactory(() ->
                ApplicationContextHolder.getApplicationContext().getBean(MongoCacheStore.class)
        );
        flightCfg.setReadThrough(true);  // Enable read-through
        flightCfg.setWriteThrough(true); // Enable write-through

        // Metrics collection interval
        cfg.setMetricsUpdateFrequency(1000); // 1 second

        cfg.setCacheConfiguration(flightCfg);

        return Ignition.start(cfg);
    }
}