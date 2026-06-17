package com.example.railway.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class UpdatePassengerProfileRequest {

    @NotBlank
    @Size(max = 64)
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
