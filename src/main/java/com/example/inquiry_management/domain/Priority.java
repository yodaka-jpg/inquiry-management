package com.example.inquiry_management.domain;

public enum Priority {

    HIGH("高"),
    MIDDLE("中"),
    LOW("低");

    private final String displayName;

    Priority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
