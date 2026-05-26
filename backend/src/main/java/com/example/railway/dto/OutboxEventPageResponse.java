package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.example.railway.domain.OutboxEvent;

public class OutboxEventPageResponse {

    private List<OutboxEventResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static OutboxEventPageResponse from(Page<OutboxEvent> eventPage) {
        OutboxEventPageResponse response = new OutboxEventPageResponse();
        List<OutboxEventResponse> content = new ArrayList<OutboxEventResponse>();
        for (OutboxEvent event : eventPage.getContent()) {
            content.add(OutboxEventResponse.from(event));
        }
        response.setContent(content);
        response.setPage(eventPage.getNumber());
        response.setSize(eventPage.getSize());
        response.setTotalElements(eventPage.getTotalElements());
        response.setTotalPages(eventPage.getTotalPages());
        response.setFirst(eventPage.isFirst());
        response.setLast(eventPage.isLast());
        return response;
    }

    public List<OutboxEventResponse> getContent() { return content; }
    public void setContent(List<OutboxEventResponse> content) { this.content = content; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }
    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
}
