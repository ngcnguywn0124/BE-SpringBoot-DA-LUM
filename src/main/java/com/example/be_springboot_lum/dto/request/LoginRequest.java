package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String identifier; // email hoặc số điện thoại

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
