package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.*;
import com.example.be_springboot_lum.dto.response.AuthResponse;
import com.example.be_springboot_lum.dto.response.UserResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.PasswordResetToken;
import com.example.be_springboot_lum.model.Role;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.model.UserSession;
import com.example.be_springboot_lum.model.UserVerification;
import com.example.be_springboot_lum.repository.PasswordResetTokenRepository;
import com.example.be_springboot_lum.repository.RoleRepository;
import com.example.be_springboot_lum.repository.UserRepository;
import com.example.be_springboot_lum.repository.UserSessionRepository;
import com.example.be_springboot_lum.repository.OAuthAccountRepository;
import com.example.be_springboot_lum.repository.UserVerificationRepository;
import com.example.be_springboot_lum.security.JwtTokenProvider;
import com.example.be_springboot_lum.service.AuthService;
import com.example.be_springboot_lum.service.EmailService;
import com.example.be_springboot_lum.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private static final String TYPE_EMAIL = "email";
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int MAX_FAILED_OTP_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_MINUTES = 60;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int OTP_LOCK_MINUTES = 60;


    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final RoleRepository roleRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;

    // ── Register ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (!request.isAcceptTerms()) {
            throw new AppException(ErrorCode.TERMS_NOT_ACCEPTED);
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        // Lấy role mặc định ROLE_USER từ DB
        Role defaultRole = roleRepository.findByName(Role.ROLE_USER)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "Default role chưa được khởi tạo, vui lòng chạy DataInitializer"));

        User user = User.builder()
                .email(normalizedEmail)
                .phoneNumber(request.getPhoneNumber())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(defaultRole))
                .build();

        user = userRepository.save(user);

        issueEmailOtp(user);
        return mapToUserResponse(user);
    }

    @Override
    @Transactional(noRollbackFor = AppException.class)
    public UserResponse verifyEmail(VerifyEmailRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedOtp = request.getOtp().trim();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy tài khoản"));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Email đã được xác thực");
        }

        UserVerification verification = userVerificationRepository
                .findTopByUserUserIdAndVerificationTypeAndIsVerifiedFalseOrderByCreatedAtDesc(
                        user.getUserId(),
                        TYPE_EMAIL
                )
                .orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_CODE_INVALID));

        if (verification.getLockedUntil() != null && verification.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new AppException(ErrorCode.OTP_LOCKED);
        }

        if (verification.getExpiresAt() == null || verification.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AppException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!normalizedOtp.equals(verification.getVerificationCode())) {
            int failedAttempts = (verification.getFailedAttempts() == null ? 0 : verification.getFailedAttempts()) + 1;
            verification.setFailedAttempts(failedAttempts);
            if (failedAttempts >= MAX_FAILED_OTP_ATTEMPTS) {
                verification.setLockedUntil(OffsetDateTime.now().plusMinutes(OTP_LOCK_MINUTES));
                userVerificationRepository.save(verification);
                throw new AppException(ErrorCode.OTP_LOCKED);
            }
            userVerificationRepository.save(verification);
            throw new AppException(ErrorCode.VERIFICATION_CODE_INVALID);
        }

        verification.setIsVerified(true);
        verification.setVerifiedAt(OffsetDateTime.now());
        verification.setFailedAttempts(0);
        verification.setLockedUntil(null);
        userVerificationRepository.save(verification);

        user.setIsEmailVerified(true);
        userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy tài khoản"));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Email đã được xác thực");
        }

        issueEmailOtp(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier().trim();
        // Tìm theo email hoặc SĐT
        User user = userRepository
                .findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            OffsetDateTime lockUntil = user.getLockedUntil();
            if (lockUntil != null && lockUntil.isAfter(OffsetDateTime.now())) {
                throw new AppException(
                        ErrorCode.ACCOUNT_TEMPORARILY_LOCKED,
                        "Tài khoản đang tạm khóa đến " + lockUntil + ". Vui lòng thử lại sau."
                );
            }
            if (lockUntil != null && lockUntil.isBefore(OffsetDateTime.now())) {
                user.setIsActive(true);
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }
        }

        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failedAttempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                user.setIsActive(false);
                OffsetDateTime lockedUntil = OffsetDateTime.now().plusMinutes(ACCOUNT_LOCK_MINUTES);
                user.setLockedUntil(lockedUntil);
                userRepository.save(user);
                throw new AppException(
                        ErrorCode.ACCOUNT_TEMPORARILY_LOCKED,
                        "Tài khoản đã bị tạm khóa đến " + lockedUntil + " do nhập sai mật khẩu quá nhiều lần."
                );
            }
            userRepository.save(user);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        boolean isPrivileged = roleNames.contains(Role.ROLE_ADMIN) || roleNames.contains(Role.ROLE_SUPER_ADMIN);

        if (!Boolean.TRUE.equals(user.getIsEmailVerified()) && !isPrivileged) {
            try {
                issueEmailOtp(user);
            } catch (AppException ex) {
                if (ex.getErrorCode() != ErrorCode.OTP_RESEND_TOO_SOON) {
                    throw ex;
                }
            }
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // Cập nhật thời gian đăng nhập
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(user, request.isRememberMe());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String refreshToken) {
        userSessionRepository.deleteByRefreshToken(refreshToken);
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));

        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            userSessionRepository.delete(session);
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = session.getUser();

        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        // Xóa session cũ, tạo session mới (Refresh Token Rotation)
        userSessionRepository.delete(session);

        // Giữ nguyên trạng thái rememberMe nếu session cũ còn hiệu lực lâu
        boolean isRememberMe = session.getExpiresAt().isAfter(OffsetDateTime.now().plusWeeks(1));
        return buildAuthResponse(user, isRememberMe);
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Không tiết lộ email có tồn tại hay không
        if (user == null) {
            log.info("Forgot password: email {} không tồn tại trong hệ thống", request.getEmail());
            return;
        }

        // Kiểm tra xem người dùng này có đăng nhập bằng mạng xã hội không
        if (oAuthAccountRepository.existsByUserUserId(user.getUserId())) {
            throw new AppException(ErrorCode.SOCIAL_ACCOUNT_PASSWORD_RESET_NOT_ALLOWED);
        }

        // Xóa token cũ nếu có
        passwordResetTokenRepository.deleteByUser(user);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(OffsetDateTime.now().plusMinutes(30))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Gửi email bất đồng bộ
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));

        if (resetToken.getIsUsed()) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        if (resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Đánh dấu token đã dùng
        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Xóa tất cả session hiện tại để bắt buộc đăng nhập lại
        userSessionRepository.deleteByUser(user);
    }

    // ── Change Password ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        User user = securityUtils.getCurrentUser();

        // Chặn đổi mật khẩu nếu là tài khoản Google/Social
        if (oAuthAccountRepository.existsByUserUserId(user.getUserId())) {
            throw new AppException(ErrorCode.SOCIAL_ACCOUNT_PASSWORD_RESET_NOT_ALLOWED);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xóa tất cả session để bắt buộc đăng nhập lại trên các thiết bị khác
        userSessionRepository.deleteByUser(user);
    }

    // ── Get Current User ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        User user = securityUtils.getCurrentUser();
        return mapToUserResponse(user);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user, boolean rememberMe) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), roleNames);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), rememberMe);

        long refreshTokenExpiry = rememberMe 
                ? jwtTokenProvider.getRememberMeExpirationMs() 
                : jwtTokenProvider.getRefreshTokenExpirationMs();

        // Lưu refresh token vào DB (user_sessions)
        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(OffsetDateTime.now().plusSeconds(refreshTokenExpiry / 1000))
                .build();
        userSessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(mapToUserResponse(user))
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        boolean isSocialAccount = oAuthAccountRepository.existsByUserUserId(user.getUserId());

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .coverUrl(user.getCoverUrl())
                .roles(roleNames)
                .isSocialAccount(isSocialAccount)
                .studentId(user.getStudentId())
                .universityId(user.getUniversityId())
                .campusId(user.getCampusId())
                .faculty(user.getFaculty())
                .bio(user.getBio())
                .location(user.getLocation())
                .reputationScore(user.getReputationScore())
                .totalSales(user.getTotalSales())
                .totalPurchases(user.getTotalPurchases())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .responseRate(user.getResponseRate())
                .isEmailVerified(user.getIsEmailVerified())
                .isPhoneVerified(user.getIsPhoneVerified())
                .isStudentVerified(user.getIsStudentVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .lastSeenAt(user.getLastSeenAt())
                .build();
    }

    private void issueEmailOtp(User user) {
        UserVerification latestPending = userVerificationRepository
                .findTopByUserUserIdAndVerificationTypeAndIsVerifiedFalseOrderByCreatedAtDesc(
                        user.getUserId(),
                        TYPE_EMAIL
                )
                .orElse(null);

        if (latestPending != null) {
            OffsetDateTime cooldownUntil = latestPending.getCreatedAt().plusSeconds(OTP_RESEND_COOLDOWN_SECONDS);
            if (cooldownUntil.isAfter(OffsetDateTime.now())) {
                throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON);
            }
        }

        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1_000_000));

        UserVerification verification = UserVerification.builder()
                .user(user)
                .verificationType(TYPE_EMAIL)
                .verificationCode(code)
                .isVerified(false)
                .failedAttempts(0)
                .lockedUntil(null)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .build();
        userVerificationRepository.save(verification);

        emailService.sendVerificationCodeEmail(user.getEmail(), user.getFullName(), code);
    }
}
