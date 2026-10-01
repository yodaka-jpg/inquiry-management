package com.example.inquiry_management.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.inquiry_management.domain.User;
import com.example.inquiry_management.repository.UserRepository;

@Service
public class DatabaseUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    /**
     * login_idを使ってログインユーザーを取得する。
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(
            String loginId
    ) throws UsernameNotFoundException {

        User user = userRepository
                .findByLoginId(loginId)
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "ログインIDまたは"
                                        + "パスワードが正しくありません。"
                                )
                );

        return org.springframework.security
                .core.userdetails.User
                .withUsername(user.getLoginId())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isActive())
                .build();
    }
}
