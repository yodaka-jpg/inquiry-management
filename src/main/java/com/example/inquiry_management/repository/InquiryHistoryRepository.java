package com.example.inquiry_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.inquiry_management.domain.InquiryHistory;

public interface InquiryHistoryRepository
        extends JpaRepository<InquiryHistory, Long> {

    /**
     * 指定した問い合わせの履歴を、
     * 新しい順で最大20件取得する。
     */
    List<InquiryHistory>
    findTop20ByInquiry_IdOrderByChangedAtDescIdDesc(
            Long inquiryId
    );
}
