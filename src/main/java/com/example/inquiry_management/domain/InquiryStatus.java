package com.example.inquiry_management.domain;

public enum InquiryStatus {

    NEW("未対応"),
    IN_PROGRESS("対応中"),
    PENDING("保留"),
    DONE("完了");

    private final String displayName;

    InquiryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 現在のステータスから、
     * 指定されたステータスへ遷移可能か判定する。
     */
    public boolean canTransitionTo(
            InquiryStatus nextStatus
    ) {
        if (nextStatus == null || this == nextStatus) {
            return false;
        }

        return switch (this) {
            case NEW ->
                    nextStatus == IN_PROGRESS;

            case IN_PROGRESS ->
                    nextStatus == PENDING
                    || nextStatus == DONE;

            case PENDING ->
                    nextStatus == IN_PROGRESS;

            case DONE ->
                    nextStatus == IN_PROGRESS;
        };
    }

    /**
     * 不正な遷移の場合は例外を発生させる。
     */
    public void validateTransitionTo(
            InquiryStatus nextStatus
    ) {
        if (!canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    "このステータスへは変更できません。"
                    + "画面を再読み込みしてください。"
            );
        }
    }
}
