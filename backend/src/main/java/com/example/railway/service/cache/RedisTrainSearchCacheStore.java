package com.example.railway.service.cache;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.railway.config.TrainSearchCacheProperties;
import com.example.railway.dto.TrainSearchCacheStats;
import com.example.railway.dto.TrainSearchResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisTrainSearchCacheStore implements TrainSearchCacheStore {

    private static final String KEY_PATTERN = "railway:cache:train-search:*";

    private final TrainSearchCacheProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong evictCount = new AtomicLong();

    public RedisTrainSearchCacheStore(TrainSearchCacheProperties properties,
                                      StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String mode() {
        return "redis";
    }

    @Override
    public List<TrainSearchResponse> get(String departureCode, String arrivalCode, LocalDate travelDate) {
        String value = redisTemplate.opsForValue().get(key(departureCode, arrivalCode, travelDate));
        if (value == null) {
            missCount.incrementAndGet();
            return null;
        }
        hitCount.incrementAndGet();
        return copy(readResponses(value));
    }

    @Override
    public void put(String departureCode, String arrivalCode, LocalDate travelDate, List<TrainSearchResponse> responses) {
        redisTemplate.opsForValue().set(
                key(departureCode, arrivalCode, travelDate),
                writeResponses(responses),
                properties.getTtlSeconds(),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void evictRoute(String departureCode, String arrivalCode, LocalDate travelDate) {
        Boolean removed = redisTemplate.delete(key(departureCode, arrivalCode, travelDate));
        if (Boolean.TRUE.equals(removed)) {
            evictCount.incrementAndGet();
        }
    }

    @Override
    public TrainSearchCacheStats clear() {
        Set<String> keys = redisTemplate.keys(KEY_PATTERN);
        if (keys != null && !keys.isEmpty()) {
            Long removed = redisTemplate.delete(keys);
            if (removed != null) {
                evictCount.addAndGet(removed);
            }
        }
        return stats();
    }

    @Override
    public TrainSearchCacheStats stats() {
        TrainSearchCacheStats stats = new TrainSearchCacheStats();
        stats.setEnabled(properties.isEnabled());
        stats.setCacheMode(mode());
        stats.setConfiguredMode(properties.getMode());
        stats.setTtlSeconds(properties.getTtlSeconds());
        stats.setMaxEntries(properties.getMaxEntries());
        Set<String> keys = redisTemplate.keys(KEY_PATTERN);
        stats.setEntryCount(keys == null ? 0 : keys.size());
        stats.setHitCount(hitCount.get());
        stats.setMissCount(missCount.get());
        stats.setEvictCount(evictCount.get());
        stats.setRedisAvailable(true);
        stats.setLocalFallback(false);
        return stats;
    }

    private String key(String departureCode, String arrivalCode, LocalDate travelDate) {
        return TrainSearchCacheKey.of(departureCode, arrivalCode, travelDate).redisKey();
    }

    private String writeResponses(List<TrainSearchResponse> responses) {
        try {
            return objectMapper.writeValueAsString(copy(responses));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write train search cache", exception);
        }
    }

    private List<TrainSearchResponse> readResponses(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<List<TrainSearchResponse>>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read train search cache", exception);
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
}
