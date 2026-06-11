package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminGlobalSearchResponse {

    private String keyword;
    private int totalCount;
    private List<SearchResultGroupResponse> groups = new ArrayList<SearchResultGroupResponse>();

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<SearchResultGroupResponse> getGroups() {
        return groups;
    }

    public void setGroups(List<SearchResultGroupResponse> groups) {
        this.groups = groups;
    }
}
