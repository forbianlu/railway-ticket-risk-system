package com.example.railway.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.example.railway.config.TrainSearchCacheProperties;
import com.example.railway.dto.TrainSearchCacheStats;
import com.example.railway.dto.TrainSearchResponse;
import com.example.railway.service.cache.LocalTrainSearchCacheStore;
import com.example.railway.service.cache.RedisTrainSearchCacheStore;
import com.example.railway.service.cache.TrainSearchCacheStore;

@Service
public class TrainSearchCacheService {

    private final TrainSearchCacheProperties properties;
    private final LocalTrainSearchCacheStore localStore;
    private final RedisTrainSearchCacheStore redisStore;
    private volatile boolean localFallback;

    public TrainSearchCacheService(TrainSearchCacheProperties properties,
                                   LocalTrainSearchCacheStore localStore,
                                   ObjectProvider<RedisTrainSearchCacheStore> redisStoreProvider) {
        this.properties = properties;
        this.localStore = localStore;
        this.redisStore = redisStoreProvider.getIfAvailable();
    }

    public List<TrainSearchResponse> get(String departureCode, String arrivalCode, LocalDate travelDate) {
        if (!isUsable()) {
            return null;
        }
        try {
            return activeStore().get(departureCode, arrivalCode, travelDate);
        } catch (RuntimeException exception) {
            return fallbackStore().get(departureCode, arrivalCode, travelDate);
        }
    }

    public void put(String departureCode, String arrivalCode, LocalDate travelDate, List<TrainSearchResponse> responses) {
        if (!isUsable()) {
            return;
        }
        try {
            activeStore().put(departureCode, arrivalCode, travelDate, responses);
        } catch (RuntimeException exception) {
            fallbackStore().put(departureCode, arrivalCode, travelDate, responses);
        }
    }

    public void evictRoute(String departureCode, String arrivalCode, LocalDate travelDate) {
        try {
            activeStore().evictRoute(departureCode, arrivalCode, travelDate);
        } catch (RuntimeException exception) {
            fallbackStore().evictRoute(departureCode, arrivalCode, travelDate);
        }
    }

    public TrainSearchCacheStats clear() {
        try {
            return decorate(activeStore().clear(), activeStore(), true);
        } catch (RuntimeException exception) {
            return decorate(fallbackStore().clear(), localStore, false);
        }
    }

    public TrainSearchCacheStats stats() {
        try {
            return decorate(activeStore().stats(), activeStore(), true);
        } catch (RuntimeException exception) {
            return decorate(fallbackStore().stats(), localStore, false);
        }
    }

    private TrainSearchCacheStore activeStore() {
        if ("redis".equalsIgnoreCase(properties.getMode())) {
            if (redisStore != null) {
                return redisStore;
            }
            localFallback = true;
            return localStore;
        }
        localFallback = false;
        return localStore;
    }

    private TrainSearchCacheStore fallbackStore() {
        localFallback = true;
        return localStore;
    }

    private TrainSearchCacheStats decorate(TrainSearchCacheStats stats, TrainSearchCacheStore store, boolean operationSucceeded) {
        stats.setEnabled(properties.isEnabled());
        stats.setCacheMode(store.mode());
        stats.setConfiguredMode(properties.getMode());
        boolean redisMode = "redis".equalsIgnoreCase(properties.getMode());
        stats.setRedisAvailable(redisMode && operationSucceeded && redisStore != null && "redis".equals(store.mode()));
        stats.setLocalFallback(localFallback);
        return stats;
    }

    private boolean isUsable() {
        return properties.isEnabled() && properties.getTtlSeconds() > 0 && properties.getMaxEntries() > 0;
    }
}
