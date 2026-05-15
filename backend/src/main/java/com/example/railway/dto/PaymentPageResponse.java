package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.example.railway.domain.PaymentRecord;

public class PaymentPageResponse {

    private List<PaymentResponse> content = new ArrayList<PaymentResponse>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static PaymentPageResponse from(Page<PaymentRecord> paymentPage) {
        PaymentPageResponse response = new PaymentPageResponse();
        List<PaymentResponse> payments = new ArrayList<PaymentResponse>();
        for (PaymentRecord record : paymentPage.getContent()) {
            payments.add(PaymentResponse.from(record));
        }
        response.setContent(payments);
        response.setPage(paymentPage.getNumber());
        response.setSize(paymentPage.getSize());
        response.setTotalElements(paymentPage.getTotalElements());
        response.setTotalPages(paymentPage.getTotalPages());
        response.setFirst(paymentPage.isFirst());
        response.setLast(paymentPage.isLast());
        return response;
    }

    public List<PaymentResponse> getContent() {
        return content;
    }

    public void setContent(List<PaymentResponse> content) {
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
