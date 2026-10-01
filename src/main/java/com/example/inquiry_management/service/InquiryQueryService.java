package com.example.inquiry_management.service;

import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inquiry_management.domain.HistoryField;
import com.example.inquiry_management.domain.Inquiry;
import com.example.inquiry_management.domain.InquiryHistory;
import com.example.inquiry_management.domain.InquiryStatus;
import com.example.inquiry_management.domain.User;
import com.example.inquiry_management.dto.InquiryDetailView;
import com.example.inquiry_management.dto.InquiryDetailView.HistoryItem;
import com.example.inquiry_management.dto.InquiryDetailView.StatusOption;
import com.example.inquiry_management.dto.InquiryDetailView.UserOption;
import com.example.inquiry_management.repository.InquiryHistoryRepository;
import com.example.inquiry_management.repository.InquiryRepository;
import com.example.inquiry_management.repository.UserRepository;

@Service
public class InquiryQueryService {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyy/MM/dd HH:mm"
            );

    private static final DateTimeFormatter TOKEN_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final InquiryHistoryRepository historyRepository;

    public InquiryQueryService(
            InquiryRepository inquiryRepository,
            UserRepository userRepository,
            InquiryHistoryRepository historyRepository
    ) {
        this.inquiryRepository = inquiryRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * 問い合わせ詳細画面に必要な情報を取得する。
     */
    @Transactional(readOnly = true)
    public InquiryDetailView getDetail(
            Long inquiryId,
            String loginId
    ) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(
                        () ->
                                new InquiryOperationException(
                                        "指定された問い合わせは"
                                        + "存在しません。"
                                )
                );

        User currentUser = userRepository
                .findByLoginId(loginId)
                .filter(User::isActive)
                .orElseThrow(
                        () ->
                                new InquiryOperationException(
                                        "この操作を行う"
                                        + "権限がありません。"
                                )
                );

        List<User> activeUsers =
                sortUsersByJapaneseName(
                        userRepository
                                .findByActiveTrueOrderByNameAsc()
                );

        Map<Long, User> allUserMap =
                userRepository
                        .findAll()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        User::getId,
                                        Function.identity()
                                )
                        );

        List<StatusOption> availableStatuses =
                getAvailableStatuses(inquiry);

        List<UserOption> assignableUsers =
                getAssignableUsers(
                        inquiry,
                        currentUser,
                        activeUsers
                );

        List<HistoryItem> histories =
                historyRepository
                        .findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
                                inquiryId
                        )
                        .stream()
                        .map(
                                history ->
                                        toHistoryItem(
                                                history,
                                                allUserMap
                                        )
                        )
                        .toList();

        boolean memberWithAssignedInquiry =
                currentUser.isMember()
                && inquiry.getAssignee() != null;

        boolean canUnassign =
                currentUser.isAdmin()
                && inquiry.getStatus()
                == InquiryStatus.NEW
                && inquiry.getAssignee() != null;

        boolean assigneeChangeDisabled =
                inquiry.getStatus()
                == InquiryStatus.DONE
                || memberWithAssignedInquiry;

        return new InquiryDetailView(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getBody(),
                inquiry.getRequesterName(),
                inquiry.getStatus().name(),
                inquiry.getStatus().getDisplayName(),
                getAssigneeId(inquiry),
                getAssigneeName(inquiry),
                inquiry.getPriority().name(),
                inquiry.getPriority().getDisplayName(),
                formatDisplayDate(inquiry.getCreatedAt()),
                formatDisplayDate(inquiry.getUpdatedAt()),
                inquiry.getUpdatedAt().format(TOKEN_FORMAT),
                formatDisplayDate(inquiry.getClosedAt()),
                availableStatuses,
                assignableUsers,
                histories,
                canUnassign,
                assigneeChangeDisabled,
                memberWithAssignedInquiry
        );
    }

    private List<StatusOption> getAvailableStatuses(
            Inquiry inquiry
    ) {
        return Arrays
                .stream(InquiryStatus.values())
                .filter(
                        inquiry
                                .getStatus()
                                ::canTransitionTo
                )
                .map(
                        status ->
                                new StatusOption(
                                        status.name(),
                                        status.getDisplayName()
                                )
                )
                .toList();
    }

    private List<UserOption> getAssignableUsers(
            Inquiry inquiry,
            User currentUser,
            List<User> activeUsers
    ) {
        if (inquiry.getStatus() == InquiryStatus.DONE) {
            return List.of();
        }

        Long currentAssigneeId =
                getAssigneeId(inquiry);

        if (currentUser.isAdmin()) {
            return activeUsers
                    .stream()
                    .filter(
                            user ->
                                    !user.getId().equals(
                                            currentAssigneeId
                                    )
                    )
                    .map(
                            user ->
                                    new UserOption(
                                            user.getId(),
                                            user.getName()
                                    )
                    )
                    .toList();
        }

        if (
                currentUser.isMember()
                && currentAssigneeId == null
        ) {
            return List.of(
                    new UserOption(
                            currentUser.getId(),
                            currentUser.getName()
                    )
            );
        }

        return List.of();
    }

    private List<User> sortUsersByJapaneseName(
            List<User> users
    ) {
        Collator collator =
                Collator.getInstance(Locale.JAPANESE);

        return users
                .stream()
                .sorted(
                        Comparator.comparing(
                                User::getName,
                                collator
                        )
                )
                .toList();
    }

    private HistoryItem toHistoryItem(
            InquiryHistory history,
            Map<Long, User> userMap
    ) {
        String oldDisplayValue =
                getHistoryDisplayValue(
                        history.getFieldName(),
                        history.getOldValue(),
                        userMap
                );

        String newDisplayValue =
                getHistoryDisplayValue(
                        history.getFieldName(),
                        history.getNewValue(),
                        userMap
                );

        return new HistoryItem(
                history.getId(),
                formatDisplayDate(
                        history.getChangedAt()
                ),
                history.getChangedBy().getName(),
                history.getFieldName().getDisplayName(),
                oldDisplayValue,
                newDisplayValue,
                history.getComment()
        );
    }

    private String getHistoryDisplayValue(
            HistoryField field,
            String value,
            Map<Long, User> userMap
    ) {
        if (value == null || value.isBlank()) {
            return "未割り当て";
        }

        if (field == HistoryField.status) {
            try {
                return InquiryStatus
                        .valueOf(value)
                        .getDisplayName();
            } catch (IllegalArgumentException exception) {
                return value;
            }
        }

        if (field == HistoryField.assignee) {
            try {
                Long userId = Long.valueOf(value);

                User user = userMap.get(userId);

                return user == null
                        ? "不明なユーザー"
                        : user.getName();
            } catch (NumberFormatException exception) {
                return "不明なユーザー";
            }
        }

        return value;
    }

    private Long getAssigneeId(Inquiry inquiry) {
        return inquiry.getAssignee() == null
                ? null
                : inquiry.getAssignee().getId();
    }

    private String getAssigneeName(Inquiry inquiry) {
        return inquiry.getAssignee() == null
                ? "未割り当て"
                : inquiry.getAssignee().getName();
    }

    private String formatDisplayDate(
            LocalDateTime value
    ) {
        return value == null
                ? ""
                : value.format(DISPLAY_FORMAT);
    }
}
