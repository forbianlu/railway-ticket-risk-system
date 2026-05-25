package com.example.railway.service.cache;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.example.railway.config.TrainSearchCacheProperties;
import com.example.railway.dto.TrainSearchCacheStats;
import com.example.railway.dto.TrainSearchResponse;

@Component
public class LocalTrainSearchCacheStore implements TrainSearchCacheStore {

    private final TrainSearchCacheProperties properties;
    private final ConcurrentMap<TrainSearchCacheKey, CacheEntry> cache = new ConcurrentHashMap<TrainSearchCacheKey, CacheEntry>();
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong evictCount = new AtomicLong();

    public LocalTrainSearchCacheStore(TrainSearchCacheProperties properties) {
        this.properties = properties;
    }

    @Override
    public String mode() {
        return "local";
    }

    @Override
    public List<TrainSearchResponse> get(String departureCode, String arrivalCode, LocalDate travelDate) {
        if (!isUsable()) {
            return null;
        }

        TrainSearchCacheKey key = TrainSearchCacheKey.of(departureCode, arrivalCode, travelDate);
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            missCount.incrementAndGet();
            return null;
        }
        if (entry.isExpired()) {
            if (cache.remove(key, entry)) {
                evictCount.incrementAndGet();
            }
            missCount.incrementAndGet();
            return null;
        }

        hitCount.incrementAndGet();
        return copy(entry.getResponses());
    }

    @Override
    public void put(String departureCode, String arrivalCode, LocalDate travelDate, List<TrainSearchResponse> responses) {
        if (!isUsable()) {
            return;
        }
        if (cache.size() >= properties.getMaxEntries()) {
            evictOneEntry();
        }

        TrainSearchCacheKey key = TrainSearchCacheKey.of(departureCode, arrivalCode, travelDate);
        Instant expiresAt = Instant.now().plusSeconds(properties.getTtlSeconds());
        cache.put(key, new CacheEntry(copy(responses), expiresAt));
    }

    @Override
    public void evictRoute(String departureCode, String arrivalCode, LocalDate travelDate) {
        TrainSearchCacheKey key = TrainSearchCacheKey.of(departureCode, arrivalCode, travelDate);
        if (cache.remove(key) != null) {
            evictCount.incrementAndGet();
        }
    }

    @Override
    public TrainSearchCacheStats clear() {
        evictCount.addAndGet(cache.size());
        cache.clear();
        return stats();
    }

    @Override
    public TrainSearchCacheStats stats() {
        removeExpiredEntries();
        TrainSearchCacheStats stats = new TrainSearchCacheStats();
        stats.setEnabled(properties.isEnabled());
        stats.setCacheMode(mode());
        stats.setConfiguredMode(properties.getMode());
        stats.setTtlSeconds(properties.getTtlSeconds());
        stats.setMaxEntries(properties.getMaxEntries());
        stats.setEntryCount(cache.size());
        stats.setHitCount(hitCount.get());
        stats.setMissCount(missCount.get());
        stats.setEvictCount(evictCount.get());
        stats.setRedisAvailable(false);
        stats.setLocalFallback(false);
        return stats;
    }

    private boolean isUsable() {
        return properties.isEnabled() && properties.getTtlSeconds() > 0 && properties.getMaxEntries() > 0;
    }

    private void removeExpiredEntries() {
        Iterator<TrainSearchCacheKey> iterator = cache.keySet().iterator();
        while (iterator.hasNext()) {
            TrainSearchCacheKey key = iterator.next();
            CacheEntry entry = cache.get(key);
            if (entry != null && entry.isExpired() && cache.remove(key, entry)) {
                evictCount.incrementAndGet();
            }
        }
    }

    private void evictOneEntry() {
        removeExpiredEntries();
        if (cache.size() < properties.getMaxEntries()) {
            return;
        }
        Iterator<TrainSearchCacheKey> iterator = cache.keySet().iterator();
        if (iterator.hasNext()) {
            TrainSearchCacheKey key = iterator.next();
            if (cache.remove(key) != null) {
                evictCount.incrementAndGet();
            }
        }
    }

    private List<TrainSearchResponse> copy(List<TrainSearchResponse> responses) {
        List<TrainSearchResponse> copies = new ArrayList<TrainSearchResponse>();
        if (responses == null) {
            return copies;
        }
        for (TrainSearchResponse response : responses) {
            copies.add(response.copy());
        }
        return copies;
    }

    private static final class CacheEntry {
        private final List<TrainSearchResponse> responses;
        private final Instant expiresAt;

        private CacheEntry(List<TrainSearchResponse> responses, Instant expiresAt) {
            this.responses = responses;
            this.expiresAt = expiresAt;
        }

        private List<TrainSearchResponse> getResponses() {
            return responses;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
