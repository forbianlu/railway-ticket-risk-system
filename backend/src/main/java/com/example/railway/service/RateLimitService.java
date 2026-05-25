package com.example.railway.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.railway.common.TooManyRequestsException;
import com.example.railway.config.RateLimitProperties;
import com.example.railway.dto.RateLimitSummary;

@Service
public class RateLimitService {

    private static final String REDIS_PREFIX = "railway:rate-limit:";

    private final RateLimitProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ConcurrentMap<String, LocalWindow> localWindows = new ConcurrentHashMap<String, LocalWindow>();
    private final AtomicLong blockedCount = new AtomicLong();
    private volatile boolean localFallback;
    private volatile boolean redisAvailable;
    private volatile String currentMode = "local";

    public RateLimitService(RateLimitProperties properties,
                            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.properties = properties;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.redisAvailable = redisTemplate != null;
    }

    public void check(String ruleName, String key) {
        RateLimitProperties.Rule rule = properties.getRule(ruleName);
        check(key, rule.getLimit(), rule.getWindowSeconds());
    }

    public void check(String key, int limit, int windowSeconds) {
        if (!properties.isEnabled() || limit <= 0 || windowSeconds <= 0) {
            return;
        }
        String normalizedKey = normalize(key);
        boolean allowed;
        if (isRedisMode()) {
            try {
                allowed = checkRedis(normalizedKey, limit, windowSeconds);
                currentMode = "redis";
                redisAvailable = true;
                localFallback = false;
            } catch (RuntimeException exception) {
                redisAvailable = false;
                if (!properties.isLocalFallbackEnabled()) {
                    throw exception;
                }
                allowed = checkLocal(normalizedKey, limit, windowSeconds);
                currentMode = "local";
                localFallback = true;
            }
        } else {
            allowed = checkLocal(normalizedKey, limit, windowSeconds);
            currentMode = "local";
            localFallback = false;
        }
        if (!allowed) {
            blockedCount.incrementAndGet();
            throw new TooManyRequestsException("请求过于频繁，请稍后再试");
        }
    }

    public RateLimitSummary summary() {
        RateLimitSummary summary = new RateLimitSummary();
        summary.setEnabled(properties.isEnabled());
        summary.setConfiguredMode(properties.getMode());
        summary.setMode(currentMode);
        summary.setRedisAvailable(isRedisMode() && redisAvailable);
        summary.setLocalFallback(localFallback);
        summary.setLocalKeyCount(localWindows.size());
        summary.setBlockedCount(blockedCount.get());
        summary.setRules(properties.getRules());
        return summary;
    }

    private boolean checkRedis(String key, int limit, int windowSeconds) {
        Long count = redisTemplate.opsForValue().increment(REDIS_PREFIX + key);
        if (count != null && count == 1L) {
            redisTemplate.expire(REDIS_PREFIX + key, windowSeconds, TimeUnit.SECONDS);
        }
        return count != null && count <= limit;
    }

    private synchronized boolean checkLocal(String key, int limit, int windowSeconds) {
        long now = Instant.now().getEpochSecond();
        LocalWindow window = localWindows.get(key);
        if (window == null || now >= window.windowStartEpochSecond + windowSeconds) {
            localWindows.put(key, new LocalWindow(now, 1));
            return true;
        }
        window.count++;
        return window.count <= limit;
    }

    private boolean isRedisMode() {
        return "redis".equalsIgnoreCase(properties.getMode()) && redisTemplate != null;
    }

    private String normalize(String key) {
        return key == null ? "unknown" : key.replaceAll("[^A-Za-z0-9:_-]", "_");
    }

    private static final class LocalWindow {
        private final long windowStartEpochSecond;
        private int count;

        private LocalWindow(long windowStartEpochSecond, int count) {
            this.windowStartEpochSecond = windowStartEpochSecond;
            this.count = count;
        }
    }
}
