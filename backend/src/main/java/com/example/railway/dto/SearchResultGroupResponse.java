package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

public class SearchResultGroupResponse {

    private String type;
    private String typeName;
    private int count;
    private List<SearchResultItemResponse> items = new ArrayList<SearchResultItemResponse>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<SearchResultItemResponse> getItems() {
        return items;
    }

    public void setItems(List<SearchResultItemResponse> items) {
        this.items = items;
    }
}
