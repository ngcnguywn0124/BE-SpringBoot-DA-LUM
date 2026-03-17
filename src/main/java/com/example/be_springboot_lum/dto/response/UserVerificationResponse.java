package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVerificationResponse {

    private UUID verificationId;
    private UUID userId;
    private String verificationType;
    private Boolean isVerified;
    private OffsetDateTime expiresAt;
    private OffsetDateTime verifiedAt;
    private OffsetDateTime createdAt;
}
