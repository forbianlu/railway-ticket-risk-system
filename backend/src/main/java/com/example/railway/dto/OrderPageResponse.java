package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.example.railway.domain.TicketOrder;

public class OrderPageResponse {

    private List<OrderResponse> content = new ArrayList<OrderResponse>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static OrderPageResponse from(Page<TicketOrder> orderPage) {
        OrderPageResponse response = new OrderPageResponse();
        List<OrderResponse> orders = new ArrayList<OrderResponse>();
        for (TicketOrder order : orderPage.getContent()) {
            orders.add(OrderResponse.from(order));
        }
        response.setContent(orders);
        response.setPage(orderPage.getNumber());
        response.setSize(orderPage.getSize());
        response.setTotalElements(orderPage.getTotalElements());
        response.setTotalPages(orderPage.getTotalPages());
        response.setFirst(orderPage.isFirst());
        response.setLast(orderPage.isLast());
        return response;
    }

    public List<OrderResponse> getContent() {
        return content;
    }

    public void setContent(List<OrderResponse> content) {
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
