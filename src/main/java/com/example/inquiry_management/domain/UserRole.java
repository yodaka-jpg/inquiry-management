package com.example.inquiry_management.domain;

public enum UserRole {

    ADMIN("管理者"),
    MEMBER("一般担当者");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isMember() {
        return this == MEMBER;
    }
}
