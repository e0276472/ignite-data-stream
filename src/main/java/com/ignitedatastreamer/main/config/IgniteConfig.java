package com.ignitedatastreamer.main.config;

import com.ignitedatastreamer.main.model.FlightPlan;
import com.ignitedatastreamer.main.store.MongoCacheStore;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.FactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IgniteConfig {

    @Bean
    public Ignite ignite() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        
        // Configure FlightPlan cache
        CacheConfiguration<String, FlightPlan> flightCfg = new CacheConfiguration<>();
        flightCfg.setName("flightPlans");
        flightCfg.setCacheMode(CacheMode.PARTITIONED);
        flightCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        flightCfg.setStatisticsEnabled(true);
        
        // MongoDB integration (critical additions)
        flightCfg.setCacheStoreFactory(FactoryBuilder.factoryOf(MongoCacheStore.class));
        flightCfg.setReadThrough(true);    // Enable read-through
        flightCfg.setWriteThrough(true);   // Enable write-through
        
        cfg.setCacheConfiguration(flightCfg);
        cfg.setMetricsUpdateFrequency(1000);
        
        return Ignition.start(cfg);
    }
}