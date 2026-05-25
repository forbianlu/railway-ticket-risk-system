package com.example.railway.dto;

public class TrainSearchCacheStats {

    private boolean enabled;
    private String cacheMode;
    private String configuredMode;
    private long ttlSeconds;
    private int maxEntries;
    private int entryCount;
    private long hitCount;
    private long missCount;
    private long evictCount;
    private boolean redisAvailable;
    private boolean localFallback;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(String cacheMode) {
        this.cacheMode = cacheMode;
    }

    public String getConfiguredMode() {
        return configuredMode;
    }

    public void setConfiguredMode(String configuredMode) {
        this.configuredMode = configuredMode;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(int entryCount) {
        this.entryCount = entryCount;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public void setMissCount(long missCount) {
        this.missCount = missCount;
    }

    public long getEvictCount() {
        return evictCount;
    }

    public void setEvictCount(long evictCount) {
        this.evictCount = evictCount;
    }

    public boolean isRedisAvailable() {
        return redisAvailable;
    }

    public void setRedisAvailable(boolean redisAvailable) {
        this.redisAvailable = redisAvailable;
    }

    public boolean isLocalFallback() {
        return localFallback;
    }

    public void setLocalFallback(boolean localFallback) {
        this.localFallback = localFallback;
    }
}
