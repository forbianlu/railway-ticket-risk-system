package com.example.railway.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "railway.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private String mode = "local";
    private boolean localFallbackEnabled = true;
    private Map<String, Rule> rules = new LinkedHashMap<String, Rule>();

    public RateLimitProperties() {
        rules.put("train-search", new Rule(60, 60));
        rules.put("order-create", new Rule(10, 60));
        rules.put("payment-callback", new Rule(30, 60));
        rules.put("risk-handle", new Rule(30, 60));
    }

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

    public Map<String, Rule> getRules() {
        return rules;
    }

    public void setRules(Map<String, Rule> rules) {
        this.rules = rules;
    }

    public Rule getRule(String name) {
        if (rules != null && rules.containsKey(name)) {
            Rule rule = rules.get(name);
            if (rule != null && rule.getLimit() > 0 && rule.getWindowSeconds() > 0) {
                return rule;
            }
        }
        return new Rule(60, 60);
    }

    public static class Rule {
        private int limit = 60;
        private int windowSeconds = 60;

        public Rule() {
        }

        public Rule(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
