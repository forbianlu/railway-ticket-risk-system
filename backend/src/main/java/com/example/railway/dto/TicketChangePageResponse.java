package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.example.railway.domain.TicketChangeRecord;

public class TicketChangePageResponse {

    private List<TicketChangeResponse> content = new ArrayList<TicketChangeResponse>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static TicketChangePageResponse from(Page<TicketChangeRecord> changePage) {
        TicketChangePageResponse response = new TicketChangePageResponse();
        List<TicketChangeResponse> changes = new ArrayList<TicketChangeResponse>();
        for (TicketChangeRecord record : changePage.getContent()) {
            changes.add(TicketChangeResponse.from(record));
        }
        response.setContent(changes);
        response.setPage(changePage.getNumber());
        response.setSize(changePage.getSize());
        response.setTotalElements(changePage.getTotalElements());
        response.setTotalPages(changePage.getTotalPages());
        response.setFirst(changePage.isFirst());
        response.setLast(changePage.isLast());
        return response;
    }

    public List<TicketChangeResponse> getContent() { return content; }
    public void setContent(List<TicketChangeResponse> content) { this.content = content; }
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
