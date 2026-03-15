package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.ReviewStudentVerificationRequest;
import com.example.be_springboot_lum.dto.request.SendVerificationCodeRequest;
import com.example.be_springboot_lum.dto.request.StudentVerificationRequest;
import com.example.be_springboot_lum.dto.request.VerifyCodeRequest;
import com.example.be_springboot_lum.dto.response.UserVerificationResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Campus;
import com.example.be_springboot_lum.model.University;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.model.UserVerification;
import com.example.be_springboot_lum.repository.CampusRepository;
import com.example.be_springboot_lum.repository.UniversityRepository;
import com.example.be_springboot_lum.repository.UserRepository;
import com.example.be_springboot_lum.repository.UserVerificationRepository;
import com.example.be_springboot_lum.service.EmailService;
import com.example.be_springboot_lum.service.VerificationService;
import com.example.be_springboot_lum.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private static final String TYPE_EMAIL = "email";
    private static final String TYPE_PHONE = "phone";
    private static final String TYPE_STUDENT = "student";

    private final UserVerificationRepository userVerificationRepository;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final CampusRepository campusRepository;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public UserVerificationResponse sendCode(SendVerificationCodeRequest request) {
        String type = normalizeType(request.getVerificationType());
        if (TYPE_STUDENT.equals(type)) {
            throw new AppException(ErrorCode.VERIFICATION_TYPE_INVALID,
                    "Xác thực sinh viên không dùng mã OTP, vui lòng gửi hồ sơ xác thực sinh viên");
        }

        User user = securityUtils.getCurrentUser();
        String targetContact = TYPE_EMAIL.equals(type) ? user.getEmail() : user.getPhoneNumber();
        if (!StringUtils.hasText(targetContact)) {
            throw new AppException(ErrorCode.VERIFICATION_CONTACT_MISSING);
        }

        String code = generate6DigitCode();
        UserVerification verification = UserVerification.builder()
                .user(user)
                .verificationType(type)
                .verificationCode(code)
                .isVerified(false)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
        UserVerification saved = userVerificationRepository.save(verification);

        if (TYPE_EMAIL.equals(type)) {
            emailService.sendVerificationCodeEmail(user.getEmail(), user.getFullName(), code);
        } else {
            log.info("[PHONE-VERIFY] userId={} phone={} code={}", user.getUserId(), user.getPhoneNumber(), code);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public UserVerificationResponse verifyCode(VerifyCodeRequest request) {
        String type = normalizeType(request.getVerificationType());
        if (TYPE_STUDENT.equals(type)) {
            throw new AppException(ErrorCode.VERIFICATION_TYPE_INVALID,
                    "Xác thực sinh viên cần được admin duyệt");
        }

        User user = securityUtils.getCurrentUser();
        UserVerification verification = userVerificationRepository
                .findTopByUserUserIdAndVerificationTypeAndVerificationCodeAndIsVerifiedFalseOrderByCreatedAtDesc(
                        user.getUserId(),
                        type,
                        request.getVerificationCode()
                )
                .orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_CODE_INVALID));

        if (verification.getExpiresAt() == null || verification.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AppException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        verification.setIsVerified(true);
        verification.setVerifiedAt(OffsetDateTime.now());
        UserVerification updatedVerification = userVerificationRepository.save(verification);

        if (TYPE_EMAIL.equals(type)) {
            user.setIsEmailVerified(true);
        } else if (TYPE_PHONE.equals(type)) {
            user.setIsPhoneVerified(true);
        }
        userRepository.save(user);

        return toResponse(updatedVerification);
    }

    @Override
    @Transactional
    public UserVerificationResponse submitStudentVerification(StudentVerificationRequest request) {
        User user = securityUtils.getCurrentUser();

        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new AppException(ErrorCode.UNIVERSITY_NOT_FOUND));
        Campus campus = campusRepository.findById(request.getCampusId())
                .orElseThrow(() -> new AppException(ErrorCode.CAMPUS_NOT_FOUND));
        if (!campus.getUniversity().getUniversityId().equals(university.getUniversityId())) {
            throw new AppException(ErrorCode.CAMPUS_UNIVERSITY_MISMATCH);
        }

        user.setStudentId(request.getStudentId());
        user.setUniversityId(request.getUniversityId());
        user.setCampusId(request.getCampusId());
        user.setFaculty(request.getFaculty());
        user.setGraduationYear(request.getGraduationYear());
        user.setIsStudentVerified(false);
        userRepository.save(user);

        UserVerification verification = UserVerification.builder()
                .user(user)
                .verificationType(TYPE_STUDENT)
                .verificationCode(null)
                .isVerified(false)
                .expiresAt(null)
                .verifiedAt(null)
                .build();

        return toResponse(userVerificationRepository.save(verification));
    }

    @Override
    @Transactional
    public UserVerificationResponse reviewStudentVerification(UUID userId, ReviewStudentVerificationRequest request) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UserVerification verification = userVerificationRepository
                .findTopByUserUserIdAndVerificationTypeAndIsVerifiedFalseOrderByCreatedAtDesc(
                        userId,
                        TYPE_STUDENT
                )
                .orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_REQUEST_NOT_FOUND));

        if (Boolean.TRUE.equals(request.getApproved())) {
            verification.setIsVerified(true);
            verification.setVerifiedAt(OffsetDateTime.now());
            targetUser.setIsStudentVerified(true);
        } else {
            verification.setIsVerified(false);
            verification.setVerifiedAt(null);
            targetUser.setIsStudentVerified(false);
        }

        userRepository.save(targetUser);
        UserVerification reviewed = userVerificationRepository.save(verification);
        return toResponse(reviewed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVerificationResponse> getMyVerificationHistory() {
        User user = securityUtils.getCurrentUser();
        return userVerificationRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserVerificationResponse toResponse(UserVerification verification) {
        return UserVerificationResponse.builder()
                .verificationId(verification.getVerificationId())
                .userId(verification.getUser().getUserId())
                .verificationType(verification.getVerificationType())
                .isVerified(verification.getIsVerified())
                .expiresAt(verification.getExpiresAt())
                .verifiedAt(verification.getVerifiedAt())
                .createdAt(verification.getCreatedAt())
                .build();
    }

    private String normalizeType(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AppException(ErrorCode.VERIFICATION_TYPE_INVALID);
        }
        String normalized = value.trim().toLowerCase();
        if (!TYPE_EMAIL.equals(normalized) && !TYPE_PHONE.equals(normalized) && !TYPE_STUDENT.equals(normalized)) {
            throw new AppException(ErrorCode.VERIFICATION_TYPE_INVALID);
        }
        return normalized;
    }

    private String generate6DigitCode() {
        int number = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return String.valueOf(number);
    }
}
