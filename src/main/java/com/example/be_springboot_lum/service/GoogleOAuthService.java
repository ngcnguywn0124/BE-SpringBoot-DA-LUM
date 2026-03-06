package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.response.AuthResponse;

public interface GoogleOAuthService {

    /**
     * Tạo Google Authorization URL.
     * Frontend sẽ redirect người dùng đến URL này để đăng nhập Google.
     *
     * @param state CSRF-protection state token (do backend tạo & lưu vào cookie ngắn hạn)
     * @return URL redirect đến Google OAuth2
     */
    String buildAuthorizationUrl(String state);

    /**
     * Xử lý callback từ Google:
     * 1. Đổi authorization code lấy access token
     * 2. Lấy thông tin user từ Google Userinfo API
     * 3. Tìm hoặc tạo user trong DB
     * 4. Liên kết oauth_account
     * 5. Trả về AuthResponse (JWT tokens)
     *
     * @param code  Authorization code từ Google
     * @return AuthResponse chứa accessToken + refreshToken + userInfo
     */
    AuthResponse handleCallback(String code);
}
