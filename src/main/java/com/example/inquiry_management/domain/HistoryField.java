package com.example.inquiry_management.domain;

public enum HistoryField {

    status("ステータス"),
    assignee("担当者");

    private final String displayName;

    HistoryField(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
