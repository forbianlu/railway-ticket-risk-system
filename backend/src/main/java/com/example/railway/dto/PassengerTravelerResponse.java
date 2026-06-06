package com.example.railway.dto;

import java.time.LocalDateTime;

import com.example.railway.common.MaskingUtils;
import com.example.railway.domain.PassengerTraveler;

public class PassengerTravelerResponse {

    private Long id;
    private String passengerName;
    private String idType;
    private String idNoMasked;
    private String phoneMasked;
    private boolean defaultTraveler;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PassengerTravelerResponse from(PassengerTraveler traveler) {
        PassengerTravelerResponse response = new PassengerTravelerResponse();
        response.setId(traveler.getId());
        response.setPassengerName(traveler.getPassengerName());
        response.setIdType(traveler.getIdType() == null ? null : traveler.getIdType().name());
        response.setIdNoMasked(MaskingUtils.maskIdNo(traveler.getIdNo()));
        response.setPhoneMasked(MaskingUtils.maskPhone(traveler.getPhone()));
        response.setDefaultTraveler(traveler.isDefaultTraveler());
        response.setCreatedAt(traveler.getCreatedAt());
        response.setUpdatedAt(traveler.getUpdatedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }
    public String getIdNoMasked() { return idNoMasked; }
    public void setIdNoMasked(String idNoMasked) { this.idNoMasked = idNoMasked; }
    public String getPhoneMasked() { return phoneMasked; }
    public void setPhoneMasked(String phoneMasked) { this.phoneMasked = phoneMasked; }
    public boolean isDefaultTraveler() { return defaultTraveler; }
    public void setDefaultTraveler(boolean defaultTraveler) { this.defaultTraveler = defaultTraveler; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
