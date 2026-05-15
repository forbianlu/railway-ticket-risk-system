package com.example.railway.dto;

import javax.validation.constraints.Size;

public class RiskHandleRequest {

    @Size(max = 32)
    private String status;

    @Size(max = 500)
    private String remark;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
