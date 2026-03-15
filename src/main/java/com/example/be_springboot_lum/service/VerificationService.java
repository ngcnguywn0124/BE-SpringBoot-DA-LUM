package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.ReviewStudentVerificationRequest;
import com.example.be_springboot_lum.dto.request.SendVerificationCodeRequest;
import com.example.be_springboot_lum.dto.request.StudentVerificationRequest;
import com.example.be_springboot_lum.dto.request.VerifyCodeRequest;
import com.example.be_springboot_lum.dto.response.UserVerificationResponse;

import java.util.List;
import java.util.UUID;

public interface VerificationService {

    UserVerificationResponse sendCode(SendVerificationCodeRequest request);

    UserVerificationResponse verifyCode(VerifyCodeRequest request);

    UserVerificationResponse submitStudentVerification(StudentVerificationRequest request);

    UserVerificationResponse reviewStudentVerification(UUID userId, ReviewStudentVerificationRequest request);

    List<UserVerificationResponse> getMyVerificationHistory();
}
