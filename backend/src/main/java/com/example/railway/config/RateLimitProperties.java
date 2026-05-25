package com.example.railway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "railway.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private String mode = "local";
    private boolean localFallbackEnabled = true;

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

    public boolean isLocalFallbackEnabled() {
        return localFallbackEnabled;
    }

    public void setLocalFallbackEnabled(boolean localFallbackEnabled) {
        this.localFallbackEnabled = localFallbackEnabled;
    }
}
