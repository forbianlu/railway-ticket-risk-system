package com.example.railway.security;

import com.example.railway.domain.UserRole;

public class AuthPrincipal {

    private final String username;
    private final String displayName;
    private final UserRole role;
    private final Long expiresAt;

    public AuthPrincipal(String username, String displayName, UserRole role, Long expiresAt) {
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.expiresAt = expiresAt;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }
}
