package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendVerificationCodeRequest {

    @NotBlank(message = "Loại xác thực không được để trống")
    private String verificationType;
}
