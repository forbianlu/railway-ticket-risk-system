package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.example.railway.domain.NotificationRecord;

public class NotificationPageResponse {

    private List<NotificationResponse> content = new ArrayList<NotificationResponse>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static NotificationPageResponse from(Page<NotificationRecord> notificationPage) {
        NotificationPageResponse response = new NotificationPageResponse();
        List<NotificationResponse> notifications = new ArrayList<NotificationResponse>();
        for (NotificationRecord record : notificationPage.getContent()) {
            notifications.add(NotificationResponse.from(record));
        }
        response.setContent(notifications);
        response.setPage(notificationPage.getNumber());
        response.setSize(notificationPage.getSize());
        response.setTotalElements(notificationPage.getTotalElements());
        response.setTotalPages(notificationPage.getTotalPages());
        response.setFirst(notificationPage.isFirst());
        response.setLast(notificationPage.isLast());
        return response;
    }

    public List<NotificationResponse> getContent() {
        return content;
    }

    public void setContent(List<NotificationResponse> content) {
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
