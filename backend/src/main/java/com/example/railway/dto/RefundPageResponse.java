package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.example.railway.domain.RefundRecord;

public class RefundPageResponse {

    private List<RefundResponse> content = new ArrayList<RefundResponse>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static RefundPageResponse from(Page<RefundRecord> refundPage) {
        RefundPageResponse response = new RefundPageResponse();
        List<RefundResponse> refunds = new ArrayList<RefundResponse>();
        for (RefundRecord record : refundPage.getContent()) {
            refunds.add(RefundResponse.from(record));
        }
        response.setContent(refunds);
        response.setPage(refundPage.getNumber());
        response.setSize(refundPage.getSize());
        response.setTotalElements(refundPage.getTotalElements());
        response.setTotalPages(refundPage.getTotalPages());
        response.setFirst(refundPage.isFirst());
        response.setLast(refundPage.isLast());
        return response;
    }

    public List<RefundResponse> getContent() { return content; }
    public void setContent(List<RefundResponse> content) { this.content = content; }
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
