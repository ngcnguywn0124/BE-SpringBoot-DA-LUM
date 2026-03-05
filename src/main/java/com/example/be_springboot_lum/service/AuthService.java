package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.*;
import com.example.be_springboot_lum.dto.response.AuthResponse;
import com.example.be_springboot_lum.dto.response.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String refreshToken);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request);

    UserResponse getCurrentUserProfile();
}
