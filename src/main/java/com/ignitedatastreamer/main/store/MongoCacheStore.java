package com.ignitedatastreamer.main.store;

import com.ignitedatastreamer.main.model.FlightPlan;
import com.ignitedatastreamer.main.repository.FlightPlanRepository;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.cache.Cache;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;

@Component
public class MongoCacheStore extends CacheStoreAdapter<String, FlightPlan> implements ApplicationContextAware {

    private FlightPlanRepository repository;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.repository = applicationContext.getBean(FlightPlanRepository.class);
    }

    @Override
    public FlightPlan load(String key) throws CacheLoaderException {
        return repository.findById(key).orElse(null);
    }

    @Override
    public void write(Cache.Entry<? extends String, ? extends FlightPlan> entry) throws CacheWriterException {
        repository.save(entry.getValue());
    }

    @Override
    public void delete(Object key) throws CacheWriterException {
        repository.deleteById((String) key);
    }
}