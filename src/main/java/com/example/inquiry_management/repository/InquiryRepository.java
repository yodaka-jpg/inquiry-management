package com.example.inquiry_management.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.inquiry_management.domain.Inquiry;

public interface InquiryRepository
        extends JpaRepository<Inquiry, Long> {

    /**
     * 更新対象の問い合わせをDB上でロックして取得する。
     *
     * SELECT ... FOR UPDATE相当。
     * N-01、N-02で使用する。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT inquiry
            FROM Inquiry inquiry
            WHERE inquiry.id = :id
            """)
    Optional<Inquiry> findByIdForUpdate(
            @Param("id") Long id
    );
}
