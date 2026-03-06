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
import com.example.be_springboot_lum.repository.PasswordResetTokenRepository;
import com.example.be_springboot_lum.repository.RoleRepository;
import com.example.be_springboot_lum.repository.UserRepository;
import com.example.be_springboot_lum.repository.UserSessionRepository;
import com.example.be_springboot_lum.repository.OAuthAccountRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final RoleRepository roleRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;

    // ── Register ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.isAcceptTerms()) {
            throw new AppException(ErrorCode.TERMS_NOT_ACCEPTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
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
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(defaultRole))
                .build();

        user = userRepository.save(user);

        // Gửi email chào mừng bất đồng bộ
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        return buildAuthResponse(user, false);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Tìm theo email hoặc SĐT
        User user = userRepository
                .findByEmailOrPhoneNumber(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
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

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .coverUrl(user.getCoverUrl())
                .roles(roleNames)
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
                .build();
    }
}
