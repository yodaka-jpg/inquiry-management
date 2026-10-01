package com.example.inquiry_management.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InquiryStatusTest {

    /**
     * 遷移表の全16組み合わせを確認する。
     */
    @ParameterizedTest
    @CsvSource({
            "NEW,         NEW,         false",
            "NEW,         IN_PROGRESS, true",
            "NEW,         PENDING,     false",
            "NEW,         DONE,        false",

            "IN_PROGRESS, NEW,         false",
            "IN_PROGRESS, IN_PROGRESS, false",
            "IN_PROGRESS, PENDING,     true",
            "IN_PROGRESS, DONE,        true",

            "PENDING,     NEW,         false",
            "PENDING,     IN_PROGRESS, true",
            "PENDING,     PENDING,     false",
            "PENDING,     DONE,        false",

            "DONE,        NEW,         false",
            "DONE,        IN_PROGRESS, true",
            "DONE,        PENDING,     false",
            "DONE,        DONE,        false"
    })
    void canTransitionToFollowsTransitionTable(
            InquiryStatus currentStatus,
            InquiryStatus nextStatus,
            boolean expected
    ) {
        assertEquals(
                expected,
                currentStatus.canTransitionTo(nextStatus)
        );
    }

    @Test
    void nullStatusIsNotAllowed() {
        assertFalse(
                InquiryStatus.NEW.canTransitionTo(null)
        );
    }

    @Test
    void invalidTransitionThrowsSpecifiedMessage() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                InquiryStatus.NEW
                                        .validateTransitionTo(
                                                InquiryStatus.DONE
                                        )
                );

        assertEquals(
                "このステータスへは変更できません。"
                + "画面を再読み込みしてください。",
                exception.getMessage()
        );
    }
}
