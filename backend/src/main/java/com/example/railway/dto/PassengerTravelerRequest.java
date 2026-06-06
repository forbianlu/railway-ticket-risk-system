package com.example.railway.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class PassengerTravelerRequest {

    @NotBlank
    @Size(max = 64)
    private String passengerName;

    @Size(max = 32)
    private String idType;

    @Size(max = 64)
    private String idNo;

    @Size(max = 32)
    private String phone;

    private Boolean defaultTraveler;

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdNo() {
        return idNo;
    }

    public void setIdNo(String idNo) {
        this.idNo = idNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getDefaultTraveler() {
        return defaultTraveler;
    }

    public void setDefaultTraveler(Boolean defaultTraveler) {
        this.defaultTraveler = defaultTraveler;
    }
}
