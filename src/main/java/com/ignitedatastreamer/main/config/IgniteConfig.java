package com.ignitedatastreamer.main;

import com.ignitedatastreamer.main.store.MongoCacheStore;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cluster.ClusterState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.cache.configuration.Factory;
import java.io.Serializable;

@Configuration
public class IgniteConfig {

    // Serializable factory to create MongoCacheStore
    public static class MongoCacheStoreFactory implements Factory<MongoCacheStore>, Serializable {
        @Override
        public MongoCacheStore create() {
            return new MongoCacheStore(); // No dependencies in constructor
        }
    }

    @Bean
    public Ignite ignite() {
        IgniteConfiguration cfg = new IgniteConfiguration();

        CacheConfiguration<String, IgniteDemoApplication.FlightPlan> flightCfg = new CacheConfiguration<>();
        flightCfg.setName("flightPlans");
        flightCfg.setCacheMode(CacheMode.PARTITIONED);
        flightCfg.setBackups(1);
        flightCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        flightCfg.setReadThrough(true);
        flightCfg.setWriteThrough(true);
        flightCfg.setCacheStoreFactory(new MongoCacheStoreFactory()); // Use the serializable factory
        flightCfg.setStatisticsEnabled(true);

        cfg.setCacheConfiguration(flightCfg);
        cfg.setMetricsUpdateFrequency(1000);

        Ignite ignite = Ignition.start(cfg);
        ignite.cluster().state(ClusterState.ACTIVE);
        System.out.println("🚀 Ignite node started with read/write-through");
        return ignite;
    }
}