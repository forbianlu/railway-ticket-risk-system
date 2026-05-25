package com.example.railway.security;

import com.example.railway.domain.UserRole;

public class AuthPrincipal {

    private final Long userId;
    private final String username;
    private final String displayName;
    private final UserRole role;
    private final Long expiresAt;

    public AuthPrincipal(Long userId, String username, String displayName, UserRole role, Long expiresAt) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
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
