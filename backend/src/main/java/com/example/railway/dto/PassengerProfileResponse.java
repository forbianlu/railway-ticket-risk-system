package com.example.railway.dto;

public class PassengerProfileResponse {

    private Long userId;
    private String username;
    private String displayName;
    private String role;
    private long travelerCount;
    private String defaultTravelerName;
    private long orderCount;
    private long activeTicketCount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public long getTravelerCount() {
        return travelerCount;
    }

    public void setTravelerCount(long travelerCount) {
        this.travelerCount = travelerCount;
    }

    public String getDefaultTravelerName() {
        return defaultTravelerName;
    }

    public void setDefaultTravelerName(String defaultTravelerName) {
        this.defaultTravelerName = defaultTravelerName;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public long getActiveTicketCount() {
        return activeTicketCount;
    }

    public void setActiveTicketCount(long activeTicketCount) {
        this.activeTicketCount = activeTicketCount;
    }
}
