package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.example.railway.domain.RiskEvent;

public class RiskEventPageResponse {

    private List<RiskEventResponse> content = new ArrayList<RiskEventResponse>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static RiskEventPageResponse from(Page<RiskEvent> riskPage) {
        RiskEventPageResponse response = new RiskEventPageResponse();
        List<RiskEventResponse> risks = new ArrayList<RiskEventResponse>();
        for (RiskEvent riskEvent : riskPage.getContent()) {
            risks.add(RiskEventResponse.from(riskEvent));
        }
        response.setContent(risks);
        response.setPage(riskPage.getNumber());
        response.setSize(riskPage.getSize());
        response.setTotalElements(riskPage.getTotalElements());
        response.setTotalPages(riskPage.getTotalPages());
        response.setFirst(riskPage.isFirst());
        response.setLast(riskPage.isLast());
        return response;
    }

    public List<RiskEventResponse> getContent() {
        return content;
    }

    public void setContent(List<RiskEventResponse> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
