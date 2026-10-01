package com.example.inquiry_management.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.example.inquiry_management.service.InquiryOperationException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String INQUIRY_NOT_FOUND_MESSAGE =
            "指定された問い合わせは存在しません。";

    @ExceptionHandler(InquiryOperationException.class)
    public ModelAndView handleInquiryOperationException(
            InquiryOperationException exception
    ) {
        ModelAndView modelAndView =
                new ModelAndView("inquiry-error");

        modelAndView.addObject(
                "errorMessage",
                exception.getMessage()
        );

        /*
         * 問い合わせが存在しない場合は
         * HTTP 404 Not Foundとして返す。
         */
        if (INQUIRY_NOT_FOUND_MESSAGE.equals(
                exception.getMessage()
        )) {
            modelAndView.setStatus(
                    HttpStatus.NOT_FOUND
            );
        } else {
            modelAndView.setStatus(
                    HttpStatus.BAD_REQUEST
            );
        }

        return modelAndView;
    }
}
