package com.example.inquiry_management;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashGeneratorTest {

    @Test
    void generatePasswordHashes() {
        BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder();

        System.out.println(
                "HANAKO_HASH="
                + encoder.encode("member-pass")
        );

        System.out.println(
                "ADMIN_HASH="
                + encoder.encode("admin-pass")
        );

        System.out.println(
                "SABURO_HASH="
                + encoder.encode("inactive-pass")
        );
    }
}
