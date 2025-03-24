package com.ignitedatastreamer.main.config;

import com.ignitedatastreamer.main.model.FlightPlan;
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

        CacheConfiguration<String, FlightPlan> flightCfg = new CacheConfiguration<>();
        flightCfg.setName("flightPlans");
        flightCfg.setCacheMode(CacheMode.PARTITIONED); // ✅ Works universally
        flightCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        flightCfg.setStatisticsEnabled(true); // Required for metrics
        cfg.setMetricsUpdateFrequency(1000); // Update metrics every 1 second
        cfg.setCacheConfiguration(flightCfg);
        return Ignition.start(cfg);
    }
}