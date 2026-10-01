package com.example.inquiry_management.controller;

import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.inquiry_management.domain.User;
import com.example.inquiry_management.repository.UserRepository;

@Controller
public class HomeController {

    private final UserRepository userRepository;

    public HomeController(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(
            Authentication authentication,
            Model model
    ) {
        String loginId = authentication.getName();

        User user = userRepository
                .findByLoginId(loginId)
                .orElseThrow();

        String authorities =
                authentication
                        .getAuthorities()
                        .stream()
                        .map(
                                authority ->
                                        authority.getAuthority()
                        )
                        .filter(
                                authority ->
                                        authority.startsWith("ROLE_")
                        )
                        .collect(
                                Collectors.joining(", ")
                        );

        model.addAttribute(
                "loginId",
                user.getLoginId()
        );

        model.addAttribute(
                "userName",
                user.getName()
        );

        model.addAttribute(
                "role",
                user.getRole().getDisplayName()
        );

        model.addAttribute(
                "authorities",
                authorities
        );

        return "home";
    }
}
