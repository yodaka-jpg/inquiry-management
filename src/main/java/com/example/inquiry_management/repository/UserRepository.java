package com.example.inquiry_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.inquiry_management.domain.User;

public interface UserRepository
        extends JpaRepository<User, Long> {

    /**
     * ログインIDからユーザーを取得する。
     */
    Optional<User> findByLoginId(String loginId);

    /**
     * 有効なユーザーIDから取得する。
     */
    Optional<User> findByIdAndActiveTrue(Long id);

    /**
     * 有効なユーザーを氏名順で取得する。
     */
    List<User> findByActiveTrueOrderByNameAsc();
}
