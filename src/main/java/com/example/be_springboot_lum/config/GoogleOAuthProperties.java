package com.example.be_springboot_lum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Đọc cấu hình Google OAuth2 từ application.properties
 * Prefix: google.oauth2
 */
@Configuration
@ConfigurationProperties(prefix = "google.oauth2")
@Getter
@Setter
public class GoogleOAuthProperties {

    /** Google OAuth2 Client ID (từ Google Cloud Console) */
    private String clientId;

    /** Google OAuth2 Client Secret (từ Google Cloud Console) */
    private String clientSecret;

    /**
     * Callback URL đã đăng ký trong Google Cloud Console.
     * Ví dụ: http://localhost:8686/api/v1/auth/google/callback
     */
    private String callbackUrl;

    /** URL tạo request xin cấp quyền. Default: https://accounts.google.com/o/oauth2/v2/auth */
    private String authorizationUri;

    /** URL đổi authorization code lấy token. Default: https://oauth2.googleapis.com/token */
    private String tokenUri;

    /** URL lấy thông tin user. Default: https://www.googleapis.com/oauth2/v3/userinfo */
    private String userinfoUri;
}
