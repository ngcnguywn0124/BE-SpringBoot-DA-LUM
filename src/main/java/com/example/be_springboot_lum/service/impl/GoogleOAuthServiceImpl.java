package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.config.GoogleOAuthProperties;
import com.example.be_springboot_lum.dto.response.AuthResponse;
import com.example.be_springboot_lum.dto.response.GoogleTokenResponse;
import com.example.be_springboot_lum.dto.response.GoogleUserInfoResponse;
import com.example.be_springboot_lum.dto.response.UserResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.OAuthAccount;
import com.example.be_springboot_lum.model.Role;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.model.UserSession;
import com.example.be_springboot_lum.repository.OAuthAccountRepository;
import com.example.be_springboot_lum.repository.RoleRepository;
import com.example.be_springboot_lum.repository.UserRepository;
import com.example.be_springboot_lum.repository.UserSessionRepository;
import com.example.be_springboot_lum.security.JwtTokenProvider;
import com.example.be_springboot_lum.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    private static final String PROVIDER = "google";
    private static final String SCOPE    = "openid email profile";

    private final GoogleOAuthProperties googleProps;
    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    // ── Build Authorization URL ───────────────────────────────────────────────

    @Override
    public String buildAuthorizationUrl(String state) {
        return googleProps.getAuthorizationUri()
                + "?client_id=" + googleProps.getClientId()
                + "&redirect_uri=" + encodeUrl(googleProps.getCallbackUrl())
                + "&response_type=code"
                + "&scope=" + encodeUrl(SCOPE)
                + "&access_type=offline"
                + "&state=" + state
                + "&prompt=consent";
    }

    // ── Handle Callback ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse handleCallback(String code) {
        // 1. Đổi authorization code lấy Google access token
        GoogleTokenResponse tokenResponse = exchangeCodeForToken(code);

        // 2. Lấy thông tin user từ Google Userinfo API
        GoogleUserInfoResponse userInfo = fetchGoogleUserInfo(tokenResponse.getAccessToken());

        if (userInfo.getEmail() == null || userInfo.getSub() == null) {
            throw new AppException(ErrorCode.OAUTH_GOOGLE_INVALID_TOKEN);
        }

        // 3. Tìm hoặc tạo người dùng trong DB
        User user = findOrCreateUser(userInfo);

        // 4. Cập nhật hoặc tạo oauth_account liên kết
        linkOAuthAccount(user, userInfo, tokenResponse);

        // 5. Cập nhật thời gian đăng nhập
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        // 6. Tạo JWT và trả về
        return buildAuthResponse(user);
    }

    // ── Private: Exchange Code ────────────────────────────────────────────────

    private GoogleTokenResponse exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code",          code);
        body.add("client_id",     googleProps.getClientId());
        body.add("client_secret", googleProps.getClientSecret());
        body.add("redirect_uri",  googleProps.getCallbackUrl());
        body.add("grant_type",    "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
                    googleProps.getTokenUri(), request, GoogleTokenResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new AppException(ErrorCode.OAUTH_CODE_EXCHANGE_FAILED);
            }
            return response.getBody();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi đổi Google authorization code: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.OAUTH_CODE_EXCHANGE_FAILED);
        }
    }

    // ── Private: Fetch User Info ──────────────────────────────────────────────

    private GoogleUserInfoResponse fetchGoogleUserInfo(String googleAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(googleAccessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleUserInfoResponse> response = restTemplate.exchange(
                    googleProps.getUserinfoUri(), HttpMethod.GET, request, GoogleUserInfoResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new AppException(ErrorCode.OAUTH_GOOGLE_INVALID_TOKEN);
            }
            return response.getBody();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin user từ Google: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.OAUTH_GOOGLE_INVALID_TOKEN);
        }
    }

    // ── Private: Find or Create User ──────────────────────────────────────────

    private User findOrCreateUser(GoogleUserInfoResponse googleUser) {
        // Kiểm tra xem oauth_account đã tồn tại chưa
        return oAuthAccountRepository
                .findByProviderAndProviderUserId(PROVIDER, googleUser.getSub())
                .map(OAuthAccount::getUser)
                .orElseGet(() ->
                        // Chưa có oauth_account → kiểm tra user theo email
                        userRepository.findByEmail(googleUser.getEmail())
                                .orElseGet(() -> createNewUserFromGoogle(googleUser))
                );
    }

    private User createNewUserFromGoogle(GoogleUserInfoResponse googleUser) {
        Role defaultRole = roleRepository.findByName(Role.ROLE_USER)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "Default role chưa được khởi tạo"));

        String fullName = googleUser.getName() != null
                ? googleUser.getName()
                : (googleUser.getGivenName() + " " + googleUser.getFamilyName()).trim();

        User newUser = User.builder()
                .email(googleUser.getEmail())
                .fullName(fullName)
                .avatarUrl(googleUser.getPicture())
                .isEmailVerified(Boolean.TRUE.equals(googleUser.getEmailVerified()))
                .roles(Set.of(defaultRole))
                .build();

        log.info("Tạo tài khoản mới từ Google OAuth cho email: {}", googleUser.getEmail());
        return userRepository.save(newUser);
    }

    // ── Private: Link OAuth Account ───────────────────────────────────────────

    private void linkOAuthAccount(User user, GoogleUserInfoResponse googleUser,
                                  GoogleTokenResponse tokenResponse) {
        oAuthAccountRepository
                .findByProviderAndProviderUserId(PROVIDER, googleUser.getSub())
                .ifPresentOrElse(
                        existing -> {
                            // Cập nhật token mới
                            existing.setAccessToken(tokenResponse.getAccessToken());
                            if (tokenResponse.getRefreshToken() != null) {
                                existing.setRefreshToken(tokenResponse.getRefreshToken());
                            }
                            oAuthAccountRepository.save(existing);
                        },
                        () -> {
                            // Tạo mới oauth_account
                            OAuthAccount account = OAuthAccount.builder()
                                    .user(user)
                                    .provider(PROVIDER)
                                    .providerUserId(googleUser.getSub())
                                    .providerEmail(googleUser.getEmail())
                                    .accessToken(tokenResponse.getAccessToken())
                                    .refreshToken(tokenResponse.getRefreshToken())
                                    .build();
                            oAuthAccountRepository.save(account);
                        }
                );
    }

    // ── Private: Build JWT AuthResponse ───────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getUserId(), user.getEmail(), roleNames);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        // Lưu refresh token vào DB
        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .expiresAt(OffsetDateTime.now().plusSeconds(
                        jwtTokenProvider.getRefreshTokenExpirationMs() / 1000))
                .build();
        userSessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(mapToUserResponse(user, roleNames))
                .build();
    }

    private UserResponse mapToUserResponse(User user, Set<String> roleNames) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roleNames)
                .isEmailVerified(user.getIsEmailVerified())
                .isActive(user.getIsActive())
                .reputationScore(user.getReputationScore())
                .totalSales(user.getTotalSales())
                .totalPurchases(user.getTotalPurchases())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static String encodeUrl(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
