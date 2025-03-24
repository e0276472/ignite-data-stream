package com.ignitedatastreamer.main.store;

import com.ignitedatastreamer.main.IgniteDemoApplication.FlightPlan;
import com.ignitedatastreamer.main.repository.FlightPlanRepository;
import com.ignitedatastreamer.main.util.ApplicationContextHolder;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import javax.cache.Cache;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;

public class MongoCacheStore extends CacheStoreAdapter<String, FlightPlan> {
    // Retrieve repository from Spring context
    private FlightPlanRepository getRepository() {
        return ApplicationContextHolder.getApplicationContext().getBean(FlightPlanRepository.class);
    }

    @Override
    public FlightPlan load(String key) throws CacheLoaderException {
        return getRepository().findById(key).orElse(null);
    }

    @Override
    public void write(Cache.Entry<? extends String, ? extends FlightPlan> entry) throws CacheWriterException {
        getRepository().save(entry.getValue());
    }

    @Override
    public void delete(Object key) throws CacheWriterException {
        getRepository().deleteById((String) key);
    }
}