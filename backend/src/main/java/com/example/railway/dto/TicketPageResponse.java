package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.example.railway.domain.TicketRecord;

public class TicketPageResponse {

    private List<TicketResponse> content = new ArrayList<TicketResponse>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static TicketPageResponse from(Page<TicketRecord> ticketPage) {
        TicketPageResponse response = new TicketPageResponse();
        List<TicketResponse> tickets = new ArrayList<TicketResponse>();
        for (TicketRecord ticket : ticketPage.getContent()) {
            tickets.add(TicketResponse.from(ticket));
        }
        response.setContent(tickets);
        response.setPage(ticketPage.getNumber());
        response.setSize(ticketPage.getSize());
        response.setTotalElements(ticketPage.getTotalElements());
        response.setTotalPages(ticketPage.getTotalPages());
        response.setFirst(ticketPage.isFirst());
        response.setLast(ticketPage.isLast());
        return response;
    }

    public List<TicketResponse> getContent() {
        return content;
    }

    public void setContent(List<TicketResponse> content) {
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
