package com.example.inquiry_management.dto;

import java.util.List;

public record InquiryDetailView(
        Long id,
        String title,
        String body,
        String requesterName,
        String statusCode,
        String statusDisplayName,
        Long assigneeId,
        String assigneeName,
        String priorityCode,
        String priorityDisplayName,
        String createdAt,
        String updatedAt,
        String updatedAtToken,
        String closedAt,
        List<StatusOption> availableStatuses,
        List<UserOption> assignableUsers,
        List<HistoryItem> histories,
        boolean canUnassign,
        boolean assigneeChangeDisabled,
        boolean showMemberRestrictionMessage
) {

    public record StatusOption(
            String code,
            String displayName
    ) {
    }

    public record UserOption(
            Long id,
            String name
    ) {
    }

    public record HistoryItem(
            Long id,
            String changedAt,
            String changedByName,
            String fieldDisplayName,
            String oldDisplayValue,
            String newDisplayValue,
            String comment
    ) {
    }
}
