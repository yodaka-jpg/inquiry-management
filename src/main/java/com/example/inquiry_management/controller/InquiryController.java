package com.example.inquiry_management.controller;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.inquiry_management.domain.InquiryStatus;
import com.example.inquiry_management.dto.InquiryDetailView;
import com.example.inquiry_management.service.InquiryOperationException;
import com.example.inquiry_management.service.InquiryQueryService;
import com.example.inquiry_management.service.InquiryService;

@Controller
public class InquiryController {

    private final InquiryQueryService queryService;
    private final InquiryService inquiryService;

    public InquiryController(
            InquiryQueryService queryService,
            InquiryService inquiryService
    ) {
        this.queryService = queryService;
        this.inquiryService = inquiryService;
    }

    /**
     * 問い合わせ詳細画面を表示する。
     */
    @GetMapping("/inquiries/{id}")
    public String detail(
            @PathVariable("id") Long inquiryId,
            Authentication authentication,
            Model model
    ) {
        InquiryDetailView view =
                queryService.getDetail(
                        inquiryId,
                        authentication.getName()
                );

        model.addAttribute("view", view);

        return "inquiry-detail";
    }

    /**
     * ステータスを変更する。
     */
    @PostMapping("/inquiries/{id}/status")
    public String changeStatus(
            @PathVariable("id") Long inquiryId,
            @RequestParam("nextStatus")
            InquiryStatus nextStatus,
            @RequestParam("updatedAt")
            String updatedAt,
            @RequestParam(
                    name = "comment",
                    required = false,
                    defaultValue = ""
            )
            String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            inquiryService.changeStatus(
                    inquiryId,
                    nextStatus,
                    LocalDateTime.parse(updatedAt),
                    authentication.getName(),
                    comment
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "ステータスを「"
                    + nextStatus.getDisplayName()
                    + "」に変更しました。"
            );
        } catch (
                InquiryOperationException
                | IllegalStateException
                | IllegalArgumentException exception
        ) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return redirectToDetail(inquiryId);
    }

    /**
     * 担当者を変更する。
     */
    @PostMapping("/inquiries/{id}/assignee")
    public String changeAssignee(
            @PathVariable("id") Long inquiryId,
            @RequestParam(
                    name = "newAssigneeId",
                    required = false
            )
            Long newAssigneeId,
            @RequestParam("updatedAt")
            String updatedAt,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            inquiryService.changeAssignee(
                    inquiryId,
                    newAssigneeId,
                    LocalDateTime.parse(updatedAt),
                    authentication.getName()
            );

            if (newAssigneeId == null) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "担当者を未割り当てに戻しました。"
                );
            } else {
                InquiryDetailView updatedView =
                        queryService.getDetail(
                                inquiryId,
                                authentication.getName()
                        );

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "担当者を「"
                        + updatedView.assigneeName()
                        + "」に変更しました。"
                );
            }
        } catch (
                InquiryOperationException
                | IllegalStateException
                | IllegalArgumentException exception
        ) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return redirectToDetail(inquiryId);
    }

    private String redirectToDetail(Long inquiryId) {
        return "redirect:/inquiries/" + inquiryId;
    }
}
