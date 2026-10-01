package com.example.inquiry_management.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inquiry_management.domain.Inquiry;
import com.example.inquiry_management.domain.InquiryHistory;
import com.example.inquiry_management.domain.InquiryStatus;
import com.example.inquiry_management.domain.User;
import com.example.inquiry_management.repository.InquiryHistoryRepository;
import com.example.inquiry_management.repository.InquiryRepository;
import com.example.inquiry_management.repository.UserRepository;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final InquiryHistoryRepository historyRepository;

    public InquiryService(
            InquiryRepository inquiryRepository,
            UserRepository userRepository,
            InquiryHistoryRepository historyRepository
    ) {
        this.inquiryRepository = inquiryRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * ステータスを変更する。
     *
     * N-01：
     * 問い合わせ更新と履歴登録を
     * 1トランザクションで実行する。
     *
     * N-02：
     * updated_atが画面表示時と一致するか確認する。
     *
     * N-03：
     * 操作者をサーバー側で確認する。
     */
    @Transactional
    public void changeStatus(
            Long inquiryId,
            InquiryStatus nextStatus,
            LocalDateTime expectedUpdatedAt,
            String operatorLoginId,
            String comment
    ) {
        Inquiry inquiry =
                findInquiryForUpdate(inquiryId);

        validateUpdatedAt(
                inquiry,
                expectedUpdatedAt
        );

        User operator =
                findActiveOperator(operatorLoginId);

        InquiryStatus oldStatus =
                inquiry.getStatus();

        Long oldAssigneeId =
                getAssigneeId(inquiry);

        LocalDateTime changedAt =
                currentTime();

        /*
         * 遷移判定と付随処理は
         * Inquiry Entity内で実施する。
         */
        inquiry.changeStatus(
                nextStatus,
                operator,
                changedAt
        );

        /*
         * ステータス変更履歴。
         * コメントが201文字以上なら、
         * ここで例外となり全処理がロールバックされる。
         */
        InquiryHistory statusHistory =
                InquiryHistory.statusChange(
                        inquiry,
                        operator,
                        oldStatus,
                        nextStatus,
                        changedAt,
                        comment
                );

        historyRepository.save(statusHistory);

        /*
         * NEW → IN_PROGRESSで担当者が
         * 自動設定された場合の担当者履歴。
         */
        Long newAssigneeId =
                getAssigneeId(inquiry);

        if (!Objects.equals(
                oldAssigneeId,
                newAssigneeId
        )) {
            InquiryHistory assigneeHistory =
                    InquiryHistory.assigneeChange(
                            inquiry,
                            operator,
                            oldAssigneeId,
                            newAssigneeId,
                            changedAt
                    );

            historyRepository.save(
                    assigneeHistory
            );
        }
    }

    /**
     * 担当者を変更する。
     */
    @Transactional
    public void changeAssignee(
            Long inquiryId,
            Long newAssigneeId,
            LocalDateTime expectedUpdatedAt,
            String operatorLoginId
    ) {
        Inquiry inquiry =
                findInquiryForUpdate(inquiryId);

        validateUpdatedAt(
                inquiry,
                expectedUpdatedAt
        );

        User operator =
                findActiveOperator(operatorLoginId);

        /*
         * R-02
         */
        if (inquiry.getStatus() == InquiryStatus.DONE) {
            throw new InquiryOperationException(
                    "完了済みの問い合わせは"
                    + "担当者を変更できません。"
            );
        }

        Long oldAssigneeId =
                getAssigneeId(inquiry);

        LocalDateTime changedAt =
                currentTime();

        if (newAssigneeId == null) {
            /*
             * R-05
             */
            if (
                    !operator.isAdmin()
                    || inquiry.getStatus()
                    != InquiryStatus.NEW
            ) {
                throw new InquiryOperationException(
                        "この操作を行う権限がありません。"
                );
            }

            if (oldAssigneeId == null) {
                throw new InquiryOperationException(
                        "担当者が変更されていません。"
                );
            }

            inquiry.unassign(changedAt);
        } else {
            /*
             * R-01
             */
            User newAssignee =
                    userRepository
                            .findByIdAndActiveTrue(
                                    newAssigneeId
                            )
                            .orElseThrow(
                                    () ->
                                            new InquiryOperationException(
                                                    "指定されたユーザーは"
                                                    + "選択できません。"
                                            )
                            );

            /*
             * R-03
             */
            if (operator.isMember()) {
                boolean allowed =
                        oldAssigneeId == null
                        && Objects.equals(
                                operator.getId(),
                                newAssignee.getId()
                        );

                if (!allowed) {
                    throw new InquiryOperationException(
                            "この操作を行う権限がありません。"
                    );
                }
            }

            /*
             * R-04：
             * ADMINは任意の有効ユーザーへ変更可能。
             */
            if (
                    !operator.isAdmin()
                    && !operator.isMember()
            ) {
                throw new InquiryOperationException(
                        "この操作を行う権限がありません。"
                );
            }

            if (Objects.equals(
                    oldAssigneeId,
                    newAssignee.getId()
            )) {
                throw new InquiryOperationException(
                        "担当者が変更されていません。"
                );
            }

            inquiry.assignTo(
                    newAssignee,
                    changedAt
            );
        }

        Long actualNewAssigneeId =
                getAssigneeId(inquiry);

        InquiryHistory history =
                InquiryHistory.assigneeChange(
                        inquiry,
                        operator,
                        oldAssigneeId,
                        actualNewAssigneeId,
                        changedAt
                );

        historyRepository.save(history);
    }

    private Inquiry findInquiryForUpdate(
            Long inquiryId
    ) {
        return inquiryRepository
                .findByIdForUpdate(inquiryId)
                .orElseThrow(
                        () ->
                                new InquiryOperationException(
                                        "指定された問い合わせは"
                                        + "存在しません。"
                                )
                );
    }

    private User findActiveOperator(
        String operatorLoginId
) {
    if (
            operatorLoginId == null
            || operatorLoginId.isBlank()
    ) {
        throw new InquiryOperationException(
                "この操作を行う権限がありません。"
        );
    }

    return userRepository
            .findByLoginId(operatorLoginId)
            .filter(User::isActive)
            .orElseThrow(
                    () ->
                            new InquiryOperationException(
                                    "この操作を行う権限がありません。"
                            )
            );
}

    /**
     * N-02：楽観的排他制御。
     */
    private void validateUpdatedAt(
            Inquiry inquiry,
            LocalDateTime expectedUpdatedAt
    ) {
        if (
                expectedUpdatedAt == null
                || !inquiry.getUpdatedAt()
                        .equals(expectedUpdatedAt)
        ) {
            throw new InquiryOperationException(
                    "他のユーザーが更新しました。"
                    + "画面を再読み込みしてください。"
            );
        }
    }

    private Long getAssigneeId(Inquiry inquiry) {
        return inquiry.getAssignee() == null
                ? null
                : inquiry.getAssignee().getId();
    }

    /**
     * MySQLのDATETIME(6)に合わせて
     * マイクロ秒単位へ切り捨てる。
     */
    private LocalDateTime currentTime() {
        return LocalDateTime
                .now()
                .truncatedTo(ChronoUnit.MICROS);
    }
}
