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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "inquiries")
public class Inquiry {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "title",
            nullable = false,
            length = 100
    )
    private String title;

    @Column(
            name = "body",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String body;

    @Column(
            name = "requester_name",
            nullable = false,
            length = 50
    )
    private String requesterName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private InquiryStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "priority",
            nullable = false,
            length = 10
    )
    private Priority priority;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * JPAが使用するコンストラクタ。
     */
    protected Inquiry() {
    }

    /**
     * 新規問い合わせ作成用。
     */
    public Inquiry(
            String title,
            String body,
            String requesterName,
            Priority priority
    ) {
        this.title = Objects.requireNonNull(title);
        this.body = Objects.requireNonNull(body);
        this.requesterName =
                Objects.requireNonNull(requesterName);
        this.priority =
                Objects.requireNonNull(priority);
        this.status = InquiryStatus.NEW;
    }

    /**
     * 新規登録時に日時と初期値を補完する。
     */
    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (status == null) {
            status = InquiryStatus.NEW;
        }

        if (priority == null) {
            priority = Priority.MIDDLE;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * ステータスを変更し、付随処理を実行する。
     */
    public void changeStatus(
            InquiryStatus nextStatus,
            User operator,
            LocalDateTime changedAt
    ) {
        Objects.requireNonNull(nextStatus);
        Objects.requireNonNull(operator);
        Objects.requireNonNull(changedAt);

        InquiryStatus oldStatus = this.status;

        oldStatus.validateTransitionTo(nextStatus);

        /*
         * NEW → IN_PROGRESSで担当者が未設定の場合、
         * 操作者を担当者に設定する。
         */
        if (
                oldStatus == InquiryStatus.NEW
                && nextStatus == InquiryStatus.IN_PROGRESS
                && assignee == null
        ) {
            assignee = operator;
        }

        /*
         * → DONEの場合、完了日時を設定する。
         */
        if (nextStatus == InquiryStatus.DONE) {
            closedAt = changedAt;
        }

        /*
         * DONE → IN_PROGRESSの場合、
         * 完了日時をクリアする。
         */
        if (
                oldStatus == InquiryStatus.DONE
                && nextStatus == InquiryStatus.IN_PROGRESS
        ) {
            closedAt = null;
        }

        status = nextStatus;
        updatedAt = changedAt;
    }

    /**
     * 担当者を設定する。
     *
     * 権限などの業務ルールはServiceで確認する。
     */
    public void assignTo(
            User newAssignee,
            LocalDateTime changedAt
    ) {
        assignee =
                Objects.requireNonNull(newAssignee);
        updatedAt =
                Objects.requireNonNull(changedAt);
    }

    /**
     * 担当者を未割り当てに戻す。
     *
     * 権限などの業務ルールはServiceで確認する。
     */
    public void unassign(
            LocalDateTime changedAt
    ) {
        assignee = null;
        updatedAt =
                Objects.requireNonNull(changedAt);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public InquiryStatus getStatus() {
        return status;
    }

    public User getAssignee() {
        return assignee;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}
