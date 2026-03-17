package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private UUID userId;
    private String email;
    private String phoneNumber;
    private String fullName;
    private String avatarUrl;
    private String coverUrl;
    private LocalDate dateOfBirth;
    private String gender;
    private Set<String> roles;
    private Boolean isSocialAccount;
    private String studentId;
    private UUID universityId;
    private UUID campusId;
    private String faculty;
    private Integer graduationYear;
    private String bio;
    private String location;
    private BigDecimal reputationScore;
    private Integer totalSales;
    private Integer totalPurchases;
    private Integer followersCount;
    private Integer followingCount;
    private BigDecimal responseRate;
    private String responseTime;
    private Boolean isEmailVerified;
    private Boolean isPhoneVerified;
    private Boolean isStudentVerified;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLoginAt;

    // Statistics for UI
    private Integer totalListings;
    private Integer totalSold;
    private Double rating;
    private Integer reviewCount;
}
