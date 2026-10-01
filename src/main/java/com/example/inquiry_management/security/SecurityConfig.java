package com.example.inquiry_management.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * BCrypt形式のパスワードハッシュを使用する。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 画面へのアクセス制御を設定する。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(
                        authorization ->
                                authorization
                                        .requestMatchers(
                                                "/login",
                                                "/error"
                                        )
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated()
                )
                .formLogin(
                        form ->
                                form
                                        .defaultSuccessUrl(
                                                "/",
                                                true
                                        )
                                        .permitAll()
                )
                .logout(
                        logout ->
                                logout
                                        .logoutSuccessUrl(
                                                "/login?logout"
                                        )
                                        .permitAll()
                );

        return http.build();
    }
}
