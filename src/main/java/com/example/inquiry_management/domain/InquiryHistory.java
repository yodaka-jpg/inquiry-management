package com.example.inquiry_management.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inquiry_histories")
public class InquiryHistory {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "inquiry_id",
            nullable = false
    )
    private Inquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "changed_by",
            nullable = false
    )
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "field_name",
            nullable = false,
            length = 20
    )
    private HistoryField fieldName;

    @Column(
            name = "old_value",
            length = 50
    )
    private String oldValue;

    @Column(
            name = "new_value",
            length = 50
    )
    private String newValue;

    @Column(
            name = "changed_at",
            nullable = false
    )
    private LocalDateTime changedAt;

    @Column(
            name = "comment",
            length = 200
    )
    private String comment;

    /**
     * JPAが使用するコンストラクタ。
     */
    protected InquiryHistory() {
    }

    private InquiryHistory(
            Inquiry inquiry,
            User changedBy,
            HistoryField fieldName,
            String oldValue,
            String newValue,
            LocalDateTime changedAt,
            String comment
    ) {
        this.inquiry =
                Objects.requireNonNull(inquiry);

        this.changedBy =
                Objects.requireNonNull(changedBy);

        this.fieldName =
                Objects.requireNonNull(fieldName);

        this.oldValue = oldValue;
        this.newValue = newValue;

        this.changedAt =
                Objects.requireNonNull(changedAt);

        this.comment = normalizeComment(comment);
    }

    /**
     * ステータス変更履歴を作る。
     */
    public static InquiryHistory statusChange(
            Inquiry inquiry,
            User changedBy,
            InquiryStatus oldStatus,
            InquiryStatus newStatus,
            LocalDateTime changedAt,
            String comment
    ) {
        Objects.requireNonNull(oldStatus);
        Objects.requireNonNull(newStatus);

        return new InquiryHistory(
                inquiry,
                changedBy,
                HistoryField.status,
                oldStatus.name(),
                newStatus.name(),
                changedAt,
                comment
        );
    }

    /**
     * 担当者変更履歴を作る。
     */
    public static InquiryHistory assigneeChange(
            Inquiry inquiry,
            User changedBy,
            Long oldAssigneeId,
            Long newAssigneeId,
            LocalDateTime changedAt
    ) {
        return new InquiryHistory(
                inquiry,
                changedBy,
                HistoryField.assignee,
                toStringOrNull(oldAssigneeId),
                toStringOrNull(newAssigneeId),
                changedAt,
                null
        );
    }

    private static String toStringOrNull(
            Long value
    ) {
        return value == null
                ? null
                : value.toString();
    }

    private static String normalizeComment(
            String comment
    ) {
        if (comment == null) {
            return null;
        }

        String normalized = comment.trim();

        if (normalized.length() > 200) {
            throw new IllegalArgumentException(
                    "コメントは 200 文字以内で入力してください。"
            );
        }

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    public Long getId() {
        return id;
    }

    public Inquiry getInquiry() {
        return inquiry;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public HistoryField getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public String getComment() {
        return comment;
    }
}
