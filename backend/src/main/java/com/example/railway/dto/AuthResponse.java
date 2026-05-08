package com.example.railway.dto;

public class AuthResponse {

    private String token;
    private String username;
    private String displayName;
    private String role;
    private Long expiresAt;

    public AuthResponse() {
    }

    public AuthResponse(String token, String username, String displayName, String role, Long expiresAt) {
        this.token = token;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
