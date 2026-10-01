package com.example.inquiry_management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.inquiry_management.domain.Inquiry;
import com.example.inquiry_management.domain.InquiryHistory;
import com.example.inquiry_management.domain.InquiryStatus;
import com.example.inquiry_management.domain.Priority;
import com.example.inquiry_management.domain.User;
import com.example.inquiry_management.domain.UserRole;
import com.example.inquiry_management.repository.InquiryHistoryRepository;
import com.example.inquiry_management.repository.InquiryRepository;
import com.example.inquiry_management.repository.UserRepository;

@SpringBootTest
@Transactional
class InquiryServiceIntegrationTest {

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private InquiryHistoryRepository historyRepository;

    @Test
    void newから対応中への変更で担当者と履歴が設定される() {
        /*
         * テスト専用の有効なMEMBERを登録する。
         * UUIDを付けることでlogin_idの重複を防止する。
         */
        String loginId =
                "test-member-" + UUID.randomUUID();

        User operator = new User(
                "テスト担当者",
                loginId,
                "test-password-hash",
                UserRole.MEMBER,
                true
        );

        operator = userRepository.saveAndFlush(operator);

        /*
         * 未割り当て・NEWの問い合わせを登録する。
         */
        Inquiry inquiry = new Inquiry(
                "結合テスト用問い合わせ",
                "結合テスト用の問い合わせ本文です。",
                "テスト依頼者",
                Priority.MIDDLE
        );

        inquiry = inquiryRepository.saveAndFlush(inquiry);

        Long inquiryId = inquiry.getId();
        Long operatorId = operator.getId();
        LocalDateTime expectedUpdatedAt =
                inquiry.getUpdatedAt();

        /*
         * 実際のサービスを使ってステータスを変更する。
         */
        inquiryService.changeStatus(
                inquiryId,
                InquiryStatus.IN_PROGRESS,
                expectedUpdatedAt,
                loginId,
                "対応を開始します。"
        );

        inquiryRepository.flush();
        historyRepository.flush();

        /*
         * 本体の更新結果を確認する。
         */
        Inquiry updatedInquiry =
                inquiryRepository.findById(inquiryId)
                        .orElseThrow();

        assertEquals(
                InquiryStatus.IN_PROGRESS,
                updatedInquiry.getStatus()
        );

        assertNotNull(updatedInquiry.getAssignee());

        assertEquals(
                operatorId,
                updatedInquiry.getAssignee().getId()
        );

        /*
         * ステータス履歴と担当者履歴の
         * 合計2件が登録されたことを確認する。
         */
        List<InquiryHistory> histories =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        );

        assertEquals(2, histories.size());
    }    @Test
    void 古いupdatedAtではステータスを変更できない() {
        String loginId =
                "test-member-" + UUID.randomUUID();

        User operator = new User(
                "排他制御テスト担当者",
                loginId,
                "test-password-hash",
                UserRole.MEMBER,
                true
        );

        userRepository.saveAndFlush(operator);

        Inquiry inquiry = new Inquiry(
                "排他制御テスト用問い合わせ",
                "古いupdated_atを送信するテストです。",
                "テスト依頼者",
                Priority.MIDDLE
        );

        inquiry = inquiryRepository.saveAndFlush(inquiry);

        Long inquiryId = inquiry.getId();

        /*
         * DBに保存されているupdated_atよりも
         * 1秒古い日時を作る。
         */
        LocalDateTime staleUpdatedAt =
                inquiry.getUpdatedAt().minusSeconds(1);

        InquiryOperationException exception =
                assertThrows(
                        InquiryOperationException.class,
                        () -> inquiryService.changeStatus(
                                inquiryId,
                                InquiryStatus.IN_PROGRESS,
                                staleUpdatedAt,
                                loginId,
                                "この更新は拒否されるはずです。"
                        )
                );

        /*
         * 指定されたエラーメッセージと
         * 完全に一致することを確認する。
         */
        assertEquals(
                "他のユーザーが更新しました。"
                + "画面を再読み込みしてください。",
                exception.getMessage()
        );

        /*
         * 更新が拒否されたので、
         * ステータスはNEWのままである。
         */
        Inquiry unchangedInquiry =
                inquiryRepository.findById(inquiryId)
                        .orElseThrow();

        assertEquals(
                InquiryStatus.NEW,
                unchangedInquiry.getStatus()
        );

        /*
         * 更新されていないので、
         * 履歴も登録されていない。
         */
        List<InquiryHistory> histories =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        );

        assertEquals(0, histories.size());
    }    @Test
    void memberは別のユーザーを担当者に設定できない() {
        /*
         * 操作を行うMEMBERを作る。
         */
        String operatorLoginId =
                "test-operator-" + UUID.randomUUID();

        User operator = new User(
                "操作担当者",
                operatorLoginId,
                "test-password-hash",
                UserRole.MEMBER,
                true
        );

        operator = userRepository.saveAndFlush(operator);

        /*
         * 担当者として指定しようとする
         * 別の有効ユーザーを作る。
         */
        String otherUserLoginId =
                "test-other-" + UUID.randomUUID();

        User otherUser = new User(
                "別の担当者",
                otherUserLoginId,
                "test-password-hash",
                UserRole.MEMBER,
                true
        );

        otherUser = userRepository.saveAndFlush(otherUser);

        /*
         * 未割り当ての問い合わせを作る。
         */
        Inquiry inquiry = new Inquiry(
                "権限チェック用問い合わせ",
                "MEMBERの担当者変更権限を確認します。",
                "テスト依頼者",
                Priority.MIDDLE
        );

        inquiry = inquiryRepository.saveAndFlush(inquiry);

        Long inquiryId = inquiry.getId();
        Long otherUserId = otherUser.getId();
        LocalDateTime expectedUpdatedAt =
                inquiry.getUpdatedAt();

        /*
         * MEMBERが別のユーザーを
         * 担当者に設定しようとする。
         */
        InquiryOperationException exception =
                assertThrows(
                        InquiryOperationException.class,
                        () -> inquiryService.changeAssignee(
                                inquiryId,
                                otherUserId,
                                expectedUpdatedAt,
                                operatorLoginId
                        )
                );

        /*
         * R-03用のメッセージと
         * 完全に一致することを確認する。
         */
        assertEquals(
                "この操作を行う権限がありません。",
                exception.getMessage()
        );

        /*
         * 担当者が設定されていないことを確認する。
         */
        Inquiry unchangedInquiry =
                inquiryRepository.findById(inquiryId)
                        .orElseThrow();

        assertEquals(
                null,
                unchangedInquiry.getAssignee()
        );

        /*
         * 変更に失敗したので、
         * 担当者変更履歴も登録されていない。
         */
        List<InquiryHistory> histories =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        );

        assertEquals(0, histories.size());
    }    @Test
    void done状態では担当者を変更できない() {
        /*
         * 操作を行う有効なADMINを作る。
         */
        String adminLoginId =
                "test-admin-" + UUID.randomUUID();

        User admin = new User(
                "テスト管理者",
                adminLoginId,
                "test-password-hash",
                UserRole.ADMIN,
                true
        );

        admin = userRepository.saveAndFlush(admin);

        /*
         * 変更先として指定する
         * 別の有効ユーザーを作る。
         */
        String otherUserLoginId =
                "test-done-user-" + UUID.randomUUID();

        User otherUser = new User(
                "変更先担当者",
                otherUserLoginId,
                "test-password-hash",
                UserRole.MEMBER,
                true
        );

        otherUser = userRepository.saveAndFlush(otherUser);

        /*
         * NEW状態の問い合わせを作る。
         */
        Inquiry inquiry = new Inquiry(
                "完了済み問い合わせのテスト",
                "DONE状態での担当者変更を確認します。",
                "テスト依頼者",
                Priority.MIDDLE
        );

        inquiry = inquiryRepository.saveAndFlush(inquiry);

        Long inquiryId = inquiry.getId();

        /*
         * NEWからIN_PROGRESSへ変更する。
         * 未割り当てなので、adminが担当者になる。
         */
        inquiryService.changeStatus(
                inquiryId,
                InquiryStatus.IN_PROGRESS,
                inquiry.getUpdatedAt(),
                adminLoginId,
                null
        );

        inquiryRepository.flush();
        historyRepository.flush();

        /*
         * IN_PROGRESSからDONEへ変更する。
         */
        inquiryService.changeStatus(
                inquiryId,
                InquiryStatus.DONE,
                inquiry.getUpdatedAt(),
                adminLoginId,
                null
        );

        inquiryRepository.flush();
        historyRepository.flush();

        assertEquals(
                InquiryStatus.DONE,
                inquiry.getStatus()
        );

        assertNotNull(inquiry.getClosedAt());

                Long originalAssigneeId =
                inquiry.getAssignee().getId();

        /*
         * ラムダ式の中で使用する値を、
         * 再代入されない変数に取り出す。
         */
        Long targetAssigneeId =
                otherUser.getId();

        LocalDateTime doneUpdatedAt =
                inquiry.getUpdatedAt();

        int historyCountBefore =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        )
                        .size();

        /*
         * DONE状態で担当者を変更しようとする。
         * ADMINであっても拒否される。
         */
        InquiryOperationException exception =
                assertThrows(
                        InquiryOperationException.class,
                        () -> inquiryService.changeAssignee(
                                inquiryId,
                                targetAssigneeId,
                                doneUpdatedAt,
                                adminLoginId
                        )
                );

        assertEquals(
                "完了済みの問い合わせは"
                + "担当者を変更できません。",
                exception.getMessage()
        );

        /*
         * 担当者が変更されていないことを確認する。
         */
        Inquiry unchangedInquiry =
                inquiryRepository.findById(inquiryId)
                        .orElseThrow();

        assertEquals(
                originalAssigneeId,
                unchangedInquiry.getAssignee().getId()
        );

        /*
         * 失敗した担当者変更について、
         * 新しい履歴が増えていないことを確認する。
         */
        int historyCountAfter =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        )
                        .size();

        assertEquals(
                historyCountBefore,
                historyCountAfter
        );
    }    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 履歴作成に失敗した場合は問い合わせ更新もロールバックされる() {
        Long inquiryId = null;
        Long operatorId = null;

        try {
            /*
             * テストデータをMySQLへ登録する。
             */
            String loginId =
                    "test-rollback-" + UUID.randomUUID();

            User operator = new User(
                    "ロールバックテスト担当者",
                    loginId,
                    "test-password-hash",
                    UserRole.MEMBER,
                    true
            );

            operator = userRepository.saveAndFlush(operator);
            operatorId = operator.getId();

            Inquiry inquiry = new Inquiry(
                    "ロールバックテスト用問い合わせ",
                    "履歴作成失敗時のロールバックを確認します。",
                    "テスト依頼者",
                    Priority.MIDDLE
            );

            inquiry = inquiryRepository.saveAndFlush(inquiry);
            inquiryId = inquiry.getId();

            LocalDateTime originalUpdatedAt =
                    inquiry.getUpdatedAt();

            /*
             * コメント上限は200文字なので、
             * 201文字のコメントを作る。
             */
            String tooLongComment =
                    "あ".repeat(201);

            Long targetInquiryId = inquiryId;

            /*
             * 履歴作成時に例外が発生することを確認する。
             */
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> inquiryService.changeStatus(
                                    targetInquiryId,
                                    InquiryStatus.IN_PROGRESS,
                                    originalUpdatedAt,
                                    loginId,
                                    tooLongComment
                            )
                    );

            assertEquals(
                    "コメントは 200 文字以内で入力してください。",
                    exception.getMessage()
            );

            /*
             * Serviceのトランザクション終了後に
             * MySQLから問い合わせを読み直す。
             */
            Inquiry rolledBackInquiry =
                    inquiryRepository.findById(inquiryId)
                            .orElseThrow();

            /*
             * ステータス変更が取り消され、
             * NEWのままである。
             */
            assertEquals(
                    InquiryStatus.NEW,
                    rolledBackInquiry.getStatus()
            );

            /*
             * NEW → IN_PROGRESS時の
             * 担当者自動設定も取り消されている。
             */
            assertEquals(
                    null,
                    rolledBackInquiry.getAssignee()
            );

            /*
             * updated_atも変更前のままである。
             */
            assertEquals(
                    originalUpdatedAt,
                    rolledBackInquiry.getUpdatedAt()
            );

            /*
             * 履歴も登録されていない。
             */
            List<InquiryHistory> histories =
                    historyRepository
                            .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                    inquiryId
                            );

            assertEquals(0, histories.size());

        } finally {
            /*
             * このテストだけはテストデータが
             * 自動ロールバックされないため、
             * 最後に削除する。
             */
            if (inquiryId != null) {
                inquiryRepository.deleteById(inquiryId);
            }

            if (operatorId != null) {
                userRepository.deleteById(operatorId);
            }
        }
    }    @Test
    void 無効ユーザーは担当者に指定できない() {
        /*
         * 操作を行う有効なADMINを作る。
         */
        String adminLoginId =
                "test-active-admin-" + UUID.randomUUID();

        User admin = new User(
                "有効なテスト管理者",
                adminLoginId,
                "test-password-hash",
                UserRole.ADMIN,
                true
        );

        userRepository.saveAndFlush(admin);

        /*
         * 担当者として指定しようとする
         * 無効ユーザーを作る。
         */
        String inactiveLoginId =
                "test-inactive-" + UUID.randomUUID();

        User inactiveUser = new User(
                "無効なテストユーザー",
                inactiveLoginId,
                "test-password-hash",
                UserRole.MEMBER,
                false
        );

        inactiveUser =
                userRepository.saveAndFlush(inactiveUser);

        /*
         * 未割り当ての問い合わせを作る。
         */
        Inquiry inquiry = new Inquiry(
                "無効ユーザー指定テスト",
                "無効ユーザーを担当者に指定できないことを確認します。",
                "テスト依頼者",
                Priority.MIDDLE
        );

        inquiry = inquiryRepository.saveAndFlush(inquiry);

        /*
         * ラムダ式で使用する値を
         * 別の変数へ取り出す。
         */
        Long inquiryId = inquiry.getId();
        Long inactiveUserId = inactiveUser.getId();
        LocalDateTime expectedUpdatedAt =
                inquiry.getUpdatedAt();

        /*
         * ADMINが無効ユーザーを
         * 担当者に指定しようとする。
         */
        InquiryOperationException exception =
                assertThrows(
                        InquiryOperationException.class,
                        () -> inquiryService.changeAssignee(
                                inquiryId,
                                inactiveUserId,
                                expectedUpdatedAt,
                                adminLoginId
                        )
                );

        assertEquals(
                "指定されたユーザーは"
                + "選択できません。",
                exception.getMessage()
        );

        /*
         * 担当者が未割り当てのままである。
         */
        Inquiry unchangedInquiry =
                inquiryRepository.findById(inquiryId)
                        .orElseThrow();

        assertEquals(
                null,
                unchangedInquiry.getAssignee()
        );

        /*
         * 失敗した変更について、
         * 履歴が登録されていない。
         */
        List<InquiryHistory> histories =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        );

        assertEquals(0, histories.size());
    }    @Test
    void adminは任意の有効ユーザーを担当者に設定できる() {
        /*
         * 操作を行う有効なADMINを作る。
         */
        String adminLoginId =
                "test-r04-admin-" + UUID.randomUUID();

        User admin = new User(
                "R04テスト管理者",
                adminLoginId,
                "test-password-hash",
                UserRole.ADMIN,
                true
        );

        userRepository.saveAndFlush(admin);

        /*
         * 担当者に設定する有効ユーザーを作る。
         */
        String assigneeLoginId =
                "test-r04-member-" + UUID.randomUUID();

        User newAssignee = new User(
                "R04テスト担当者",
                assigneeLoginId,
                "test-password-hash",
                UserRole.MEMBER,
                true
        );

        newAssignee =
                userRepository.saveAndFlush(newAssignee);

        /*
         * 未割り当ての問い合わせを作る。
         */
        Inquiry inquiry = new Inquiry(
                "ADMIN担当者変更テスト",
                "ADMINが有効ユーザーを割り当てるテストです。",
                "テスト依頼者",
                Priority.MIDDLE
        );

        inquiry = inquiryRepository.saveAndFlush(inquiry);

        Long inquiryId = inquiry.getId();
        Long newAssigneeId = newAssignee.getId();
        LocalDateTime expectedUpdatedAt =
                inquiry.getUpdatedAt();

        /*
         * ADMINとして担当者を変更する。
         */
        inquiryService.changeAssignee(
                inquiryId,
                newAssigneeId,
                expectedUpdatedAt,
                adminLoginId
        );

        inquiryRepository.flush();
        historyRepository.flush();

        /*
         * 担当者が指定したユーザーに
         * 変更されたことを確認する。
         */
        Inquiry updatedInquiry =
                inquiryRepository.findById(inquiryId)
                        .orElseThrow();

        assertNotNull(updatedInquiry.getAssignee());

        assertEquals(
                newAssigneeId,
                updatedInquiry.getAssignee().getId()
        );

        /*
         * 担当者変更履歴が1件登録されたことを確認する。
         */
        List<InquiryHistory> histories =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        );

        assertEquals(1, histories.size());
    }    @Test
    void adminはnew状態の担当者を未割り当てに戻せる() {
        /*
         * 操作を行う有効なADMINを作る。
         */
        String adminLoginId =
                "test-r05-admin-" + UUID.randomUUID();

        User admin = new User(
                "R05テスト管理者",
                adminLoginId,
                "test-password-hash",
                UserRole.ADMIN,
                true
        );

        userRepository.saveAndFlush(admin);

        /*
         * 最初に担当者として設定する
         * 有効ユーザーを作る。
         */
        String assigneeLoginId =
                "test-r05-member-" + UUID.randomUUID();

        User assignee = new User(
                "R05テスト担当者",
                assigneeLoginId,
                "test-password-hash",
                UserRole.MEMBER,
                true
        );

        assignee = userRepository.saveAndFlush(assignee);

        /*
         * NEW状態の未割り当て問い合わせを作る。
         */
        Inquiry inquiry = new Inquiry(
                "未割り当て変更テスト",
                "ADMINが担当者を未割り当てに戻すテストです。",
                "テスト依頼者",
                Priority.MIDDLE
        );

        inquiry = inquiryRepository.saveAndFlush(inquiry);

        Long inquiryId = inquiry.getId();
        Long assigneeId = assignee.getId();

        /*
         * まずADMINが担当者を設定する。
         */
        inquiryService.changeAssignee(
                inquiryId,
                assigneeId,
                inquiry.getUpdatedAt(),
                adminLoginId
        );

        inquiryRepository.flush();
        historyRepository.flush();

        assertNotNull(inquiry.getAssignee());

        /*
         * newAssigneeIdへnullを渡し、
         * 未割り当てへ戻す。
         */
        inquiryService.changeAssignee(
                inquiryId,
                null,
                inquiry.getUpdatedAt(),
                adminLoginId
        );

        inquiryRepository.flush();
        historyRepository.flush();

        Inquiry updatedInquiry =
                inquiryRepository.findById(inquiryId)
                        .orElseThrow();

        /*
         * 担当者がnull、つまり未割り当てに
         * 戻ったことを確認する。
         */
        assertEquals(
                null,
                updatedInquiry.getAssignee()
        );

        /*
         * ステータスはNEWのままである。
         */
        assertEquals(
                InquiryStatus.NEW,
                updatedInquiry.getStatus()
        );

        /*
         * 担当者設定と未割り当てへの変更で、
         * 履歴が合計2件登録される。
         */
        List<InquiryHistory> histories =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        );

        assertEquals(2, histories.size());
    }    @Test
    void memberは担当者を未割り当てに戻せない() {
        /*
         * 最初の担当者設定を行うADMINを作る。
         */
        String adminLoginId =
                "test-r05-setup-admin-" + UUID.randomUUID();

        User admin = new User(
                "R05設定用管理者",
                adminLoginId,
                "test-password-hash",
                UserRole.ADMIN,
                true
        );

        userRepository.saveAndFlush(admin);

        /*
         * 問い合わせの担当者となり、
         * 未割り当て操作も試みるMEMBERを作る。
         */
        String memberLoginId =
                "test-r05-operator-" + UUID.randomUUID();

        User member = new User(
                "R05一般担当者",
                memberLoginId,
                "test-password-hash",
                UserRole.MEMBER,
                true
        );

        member = userRepository.saveAndFlush(member);

        /*
         * NEW状態の問い合わせを作る。
         */
        Inquiry inquiry = new Inquiry(
                "MEMBER未割り当て禁止テスト",
                "MEMBERが未割り当てに戻せないことを確認します。",
                "テスト依頼者",
                Priority.MIDDLE
        );

        inquiry = inquiryRepository.saveAndFlush(inquiry);

        Long inquiryId = inquiry.getId();
        Long memberId = member.getId();

        /*
         * ADMINがMEMBERを担当者に設定する。
         */
        inquiryService.changeAssignee(
                inquiryId,
                memberId,
                inquiry.getUpdatedAt(),
                adminLoginId
        );

        inquiryRepository.flush();
        historyRepository.flush();

        LocalDateTime updatedAtAfterAssignment =
                inquiry.getUpdatedAt();

        /*
         * MEMBERが担当者を未割り当てへ
         * 戻そうとする。
         */
        InquiryOperationException exception =
                assertThrows(
                        InquiryOperationException.class,
                        () -> inquiryService.changeAssignee(
                                inquiryId,
                                null,
                                updatedAtAfterAssignment,
                                memberLoginId
                        )
                );

        assertEquals(
                "この操作を行う権限がありません。",
                exception.getMessage()
        );

        /*
         * 担当者が変更されていないことを確認する。
         */
        Inquiry unchangedInquiry =
                inquiryRepository.findById(inquiryId)
                        .orElseThrow();

        assertNotNull(unchangedInquiry.getAssignee());

        assertEquals(
                memberId,
                unchangedInquiry.getAssignee().getId()
        );

        /*
         * 最初の担当者設定履歴1件だけで、
         * 失敗した操作の履歴は増えていない。
         */
        List<InquiryHistory> histories =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        );

        assertEquals(1, histories.size());
    }    @Test
    void adminでもinProgress状態では未割り当てに戻せない() {
        /*
         * 操作を行う有効なADMINを作る。
         */
        String adminLoginId =
                "test-r05-progress-admin-" + UUID.randomUUID();

        User admin = new User(
                "R05進行中テスト管理者",
                adminLoginId,
                "test-password-hash",
                UserRole.ADMIN,
                true
        );

        admin = userRepository.saveAndFlush(admin);

        /*
         * NEW状態の未割り当て問い合わせを作る。
         */
        Inquiry inquiry = new Inquiry(
                "対応中の未割り当て禁止テスト",
                "IN_PROGRESSでは未割り当てに戻せないことを確認します。",
                "テスト依頼者",
                Priority.MIDDLE
        );

        inquiry = inquiryRepository.saveAndFlush(inquiry);

        Long inquiryId = inquiry.getId();
        Long adminId = admin.getId();

        /*
         * NEWからIN_PROGRESSへ変更する。
         * 担当者が未設定なのでADMINが自動設定される。
         */
        inquiryService.changeStatus(
                inquiryId,
                InquiryStatus.IN_PROGRESS,
                inquiry.getUpdatedAt(),
                adminLoginId,
                null
        );

        inquiryRepository.flush();
        historyRepository.flush();

        assertEquals(
                InquiryStatus.IN_PROGRESS,
                inquiry.getStatus()
        );

        assertNotNull(inquiry.getAssignee());

        assertEquals(
                adminId,
                inquiry.getAssignee().getId()
        );

        /*
         * ラムダ式で使用する更新日時を
         * 別の変数へ取り出す。
         */
        LocalDateTime inProgressUpdatedAt =
                inquiry.getUpdatedAt();

        /*
         * ADMINがIN_PROGRESS状態の問い合わせを
         * 未割り当てへ戻そうとする。
         */
        InquiryOperationException exception =
                assertThrows(
                        InquiryOperationException.class,
                        () -> inquiryService.changeAssignee(
                                inquiryId,
                                null,
                                inProgressUpdatedAt,
                                adminLoginId
                        )
                );

        assertEquals(
                "この操作を行う権限がありません。",
                exception.getMessage()
        );

        /*
         * ステータスと担当者が
         * 変更されていないことを確認する。
         */
        Inquiry unchangedInquiry =
                inquiryRepository.findById(inquiryId)
                        .orElseThrow();

        assertEquals(
                InquiryStatus.IN_PROGRESS,
                unchangedInquiry.getStatus()
        );

        assertNotNull(unchangedInquiry.getAssignee());

        assertEquals(
                adminId,
                unchangedInquiry.getAssignee().getId()
        );

        /*
         * NEWからIN_PROGRESSへの変更では、
         * ステータス履歴と担当者自動設定履歴の
         * 2件が登録される。
         *
         * 失敗した未割り当て操作による履歴は増えない。
         */
        List<InquiryHistory> histories =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        );

        assertEquals(2, histories.size());
    }    @Test
    void 存在しない問い合わせは変更できない() {
        /*
         * 通常は存在しない非常に大きなIDを指定する。
         */
        Long nonexistentInquiryId =
                Long.MAX_VALUE;

        InquiryOperationException exception =
                assertThrows(
                        InquiryOperationException.class,
                        () -> inquiryService.changeStatus(
                                nonexistentInquiryId,
                                InquiryStatus.IN_PROGRESS,
                                null,
                                "unknown-user",
                                null
                        )
                );

        assertEquals(
                "指定された問い合わせは"
                + "存在しません。",
                exception.getMessage()
        );
    }
}
