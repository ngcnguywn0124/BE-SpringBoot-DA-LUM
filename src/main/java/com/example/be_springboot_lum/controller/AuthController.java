package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.*;
import com.example.be_springboot_lum.dto.response.AuthResponse;
import com.example.be_springboot_lum.dto.response.UserResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.security.JwtTokenProvider;
import com.example.be_springboot_lum.service.AuthService;
import com.example.be_springboot_lum.service.GoogleOAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Thời gian sống của cookie khớp với JWT expiration.
     * Đơn vị ms từ application.properties → chuyển sang giây cho cookie maxAge.
     */
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Value("${jwt.remember-me-expiration:2592000000}")
    private long rememberMeExpirationMs;

    /** true trong môi trường production (HTTPS) */
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ─────────────────────────────────────────────────────────────────────────
    // Public endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/register
     * Trả về thông tin user, KHÔNG set cookie.
     * Người dùng phải xác thực email trước khi đăng nhập.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(201).body(
                ApiResponse.<UserResponse>builder()
                        .code(201)
                        .message("Đăng ký thành công. Vui lòng xác thực email để đăng nhập.")
                        .data(user)
                        .build()
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        UserResponse user = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Xác thực email thành công", user));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP mới đã được gửi đến email của bạn", null));
    }

    /**
     * POST /api/v1/auth/login
     * Trả về thông tin user, đồng thời set httpOnly cookie chứa token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        setAuthCookies(response, authResponse, request.isRememberMe());
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authResponse.getUser()));
    }

    /**
     * POST /api/v1/auth/logout
     * Đọc refreshToken từ httpOnly cookie, xóa session DB, clear cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    /**
     * POST /api/v1/auth/refresh-token
     * Đọc refreshToken từ httpOnly cookie, cấp access token mới, set cookie mới.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<UserResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken(refreshToken);

        AuthResponse authResponse = authService.refreshToken(refreshTokenRequest);
        
        // Khi refresh token, chúng ta kiểm tra thời hạn còn lại để xác định xem có nên tiếp tục rememberMe không
        // Ở đây đơn giản là giữ nguyên if it was long-lived
        boolean isRememberMe = false; 
        try {
            long expiry = jwtTokenProvider.extractAllClaims(authResponse.getRefreshToken()).getExpiration().getTime();
            if (expiry - System.currentTimeMillis() > refreshTokenExpirationMs + 1000000) {
                isRememberMe = true;
            }
        } catch(Exception e) {}

        setAuthCookies(response, authResponse, isRememberMe);
        return ResponseEntity.ok(ApiResponse.success(authResponse.getUser()));
    }

    /**
     * POST /api/v1/auth/forgot-password
     * Gửi email đặt lại mật khẩu.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Nếu email tồn tại, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu", null));
    }

    /**
     * POST /api/v1/auth/reset-password
     * Đặt lại mật khẩu bằng token từ email.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", null));
    }

    /**
     * POST /api/v1/auth/change-password
     * Đổi mật khẩu khi đã đăng nhập (yêu cầu xác thực).
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }

    /**
     * GET /api/v1/auth/me
     * Lấy thông tin user hiện tại (yêu cầu xác thực).
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse user = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Google OAuth2 endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/auth/google/authorize
     * <p>
     * Bước 1 của Google OAuth2 flow:
     * - Tạo state token ngẫu nhiên (chống CSRF).
     * - Lưu state vào httpOnly cookie ngắn hạn (5 phút).
     * - Redirect browser đến Google Authorization Endpoint.
     * <p>
     * Frontend gọi: window.location.href = "/api/v1/auth/google/authorize"
     */
    @GetMapping("/google/authorize")
    public void googleAuthorize(HttpServletResponse response) throws IOException {
        // Tạo state ngẫu nhiên để chống CSRF
        String state = UUID.randomUUID().toString();

        // Lưu state vào httpOnly cookie (5 phút)
        ResponseCookie stateCookie = ResponseCookie.from("oauth2_state", state)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth/google")
                .maxAge(300) // 5 phút
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString());

        // Redirect đến Google
        String authUrl = googleOAuthService.buildAuthorizationUrl(state);
        response.sendRedirect(authUrl);
    }

    /**
     * GET /api/v1/auth/google/callback?code=...&state=...
     * <p>
     * Bước 2 của Google OAuth2 flow (Google redirect về đây sau khi user đồng ý):
     * - Xác thực state chống CSRF.
     * - Đổi authorization code lấy Google token.
     * - Tìm / tạo user trong DB.
     * - Set JWT httpOnly cookies.
     * - Redirect về frontend.
     */
    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // Nếu user từ chối cấp quyền
        if (error != null) {
            response.sendRedirect(frontendUrl + "/?error=google_denied");
            return;
        }

        // Xác thực state chống CSRF
        String savedState = extractCookie(request, "oauth2_state");
        if (savedState == null || !savedState.equals(state)) {
            response.sendRedirect(frontendUrl + "/?error=invalid_state");
            return;
        }

        // Xóa state cookie
        ResponseCookie clearState = ResponseCookie.from("oauth2_state", "")
                .httpOnly(true).secure(cookieSecure)
                .path("/api/v1/auth/google").maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearState.toString());

        // Xử lý callback: đổi code → token → user → JWT
        AuthResponse authResponse;
        try {
            authResponse = googleOAuthService.handleCallback(code);
        } catch (Exception e) {
            response.sendRedirect(frontendUrl + "/?error=google_failed");
            return;
        }

        // Set JWT cookies
        setAuthCookies(response, authResponse, true); // Google login thường mặc định rememberMe

        // Redirect về frontend (trang chủ hoặc trang đã được chỉ định)
        response.sendRedirect(frontendUrl + "/");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Set cả hai httpOnly cookie: accessToken (path=/) và refreshToken (path=/api/v1/auth). */
    private void setAuthCookies(HttpServletResponse response, AuthResponse authResponse, boolean rememberMe) {
        long accessTokenAge = accessTokenExpirationMs / 1000;
        long refreshTokenAge = (rememberMe ? rememberMeExpirationMs : refreshTokenExpirationMs) / 1000;

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", authResponse.getAccessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(accessTokenAge)
                .sameSite("Lax") // Khi dùng ngrok (cross-origin), SameSite phải là None thay vì Lax hoặc Strict
                .build();

        // Refresh token chỉ được gửi đến /api/v1/auth (tăng bảo mật)
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(refreshTokenAge)
                .sameSite("Lax") // Khi dùng ngrok (cross-origin), SameSite phải là None thay vì Lax hoặc Strict
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    /** Xóa cả hai cookie (maxAge=0). */
    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie clearAccess = ResponseCookie.from("accessToken", "")
                .httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite("Lax").build();
        ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(cookieSecure).path("/api/v1/auth").maxAge(0).sameSite("Lax").build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());
    }

    /** Đọc giá trị cookie theo tên từ HttpServletRequest. */
    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
