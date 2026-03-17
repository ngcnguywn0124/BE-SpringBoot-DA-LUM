package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.ReviewStudentVerificationRequest;
import com.example.be_springboot_lum.dto.request.SendVerificationCodeRequest;
import com.example.be_springboot_lum.dto.request.StudentVerificationRequest;
import com.example.be_springboot_lum.dto.request.VerifyCodeRequest;
import com.example.be_springboot_lum.dto.response.PendingStudentVerificationResponse;
import com.example.be_springboot_lum.dto.response.UserVerificationResponse;
import com.example.be_springboot_lum.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/send-code")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserVerificationResponse>> sendCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã gửi mã xác thực", verificationService.sendCode(request)));
    }

    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserVerificationResponse>> confirmCode(
            @Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Xác thực thành công", verificationService.verifyCode(request)));
    }

    @PostMapping("/student/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserVerificationResponse>> submitStudentVerification(
            @Valid @RequestBody StudentVerificationRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(verificationService.submitStudentVerification(request)));
    }

    @PostMapping("/student/{userId}/review")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserVerificationResponse>> reviewStudentVerification(
            @PathVariable UUID userId,
            @Valid @RequestBody ReviewStudentVerificationRequest request) {
        String message = Boolean.TRUE.equals(request.getApproved())
                ? "Duyệt xác thực sinh viên thành công"
                : "Từ chối xác thực sinh viên thành công";
        return ResponseEntity.ok(ApiResponse.success(message,
                verificationService.reviewStudentVerification(userId, request)));
    }

    @GetMapping("/student/pending")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<PendingStudentVerificationResponse>>> getPendingStudentVerifications() {
        return ResponseEntity.ok(ApiResponse.success(verificationService.getPendingStudentVerifications()));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserVerificationResponse>>> getMyVerifications() {
        return ResponseEntity.ok(ApiResponse.success(verificationService.getMyVerificationHistory()));
    }
}
