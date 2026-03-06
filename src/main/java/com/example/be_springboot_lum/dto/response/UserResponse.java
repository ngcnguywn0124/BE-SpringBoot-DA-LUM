package com.example.be_springboot_lum.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID userId;
    private String email;
    private String phoneNumber;
    private String fullName;
    private String avatarUrl;
    private String coverUrl;
    /** Tập hợp các role của user, ví dụ: ["ROLE_USER", "ROLE_ADMIN"] */
    private Set<String> roles;
    private boolean isSocialAccount;
    private String studentId;
    private UUID universityId;
    private UUID campusId;
    private String faculty;
    private String bio;
    private String location;
    private BigDecimal reputationScore;
    private Integer totalSales;
    private Integer totalPurchases;
    private Integer followersCount;
    private Integer followingCount;
    private BigDecimal responseRate;
    private Boolean isEmailVerified;
    private Boolean isPhoneVerified;
    private Boolean isStudentVerified;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLoginAt;
}
