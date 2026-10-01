package com.example.inquiry_management.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "name",
            nullable = false,
            length = 50
    )
    private String name;

    @Column(
            name = "login_id",
            nullable = false,
            unique = true,
            length = 100
    )
    private String loginId;

    @Column(
            name = "password_hash",
            nullable = false,
            length = 100
    )
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 20
    )
    private UserRole role;

    @Column(
            name = "is_active",
            nullable = false
    )
    private boolean active;

    /**
     * JPAが使用するコンストラクタ。
     */
    protected User() {
    }

    public User(
            String name,
            String loginId,
            String passwordHash,
            UserRole role,
            boolean active
    ) {
        this.name =
                Objects.requireNonNull(name);

        this.loginId =
                Objects.requireNonNull(loginId);

        this.passwordHash =
                Objects.requireNonNull(passwordHash);

        this.role =
                Objects.requireNonNull(role);

        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isMember() {
        return role == UserRole.MEMBER;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public void changePasswordHash(
            String newPasswordHash
    ) {
        passwordHash =
                Objects.requireNonNull(newPasswordHash);
    }
}
