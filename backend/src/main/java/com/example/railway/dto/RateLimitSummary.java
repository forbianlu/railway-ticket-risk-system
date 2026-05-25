package com.example.railway.dto;

public class RateLimitSummary {

    private boolean enabled;
    private String mode;
    private String configuredMode;
    private boolean redisAvailable;
    private boolean localFallback;
    private int localKeyCount;
    private long blockedCount;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getConfiguredMode() {
        return configuredMode;
    }

    public void setConfiguredMode(String configuredMode) {
        this.configuredMode = configuredMode;
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

    public int getLocalKeyCount() {
        return localKeyCount;
    }

    public void setLocalKeyCount(int localKeyCount) {
        this.localKeyCount = localKeyCount;
    }

    public long getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }
}
