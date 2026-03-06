package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "cover_url", columnDefinition = "TEXT")
    private String coverUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // Thông tin sinh viên
    @Column(name = "student_id", length = 50)
    private String studentId;

    @Column(name = "university_id", columnDefinition = "uuid")
    private UUID universityId;

    @Column(name = "campus_id", columnDefinition = "uuid")
    private UUID campusId;

    @Column(name = "faculty", length = 255)
    private String faculty;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    // Hồ sơ cá nhân
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "location", length = 255)
    private String location;

    // Thống kê
    @Column(name = "reputation_score", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal reputationScore = BigDecimal.ZERO;

    @Column(name = "total_sales")
    @Builder.Default
    private Integer totalSales = 0;

    @Column(name = "total_purchases")
    @Builder.Default
    private Integer totalPurchases = 0;

    @Column(name = "followers_count")
    @Builder.Default
    private Integer followersCount = 0;

    @Column(name = "following_count")
    @Builder.Default
    private Integer followingCount = 0;

    @Column(name = "response_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal responseRate = BigDecimal.ZERO;

    @Column(name = "response_time", length = 50)
    private String responseTime;

    // Trạng thái tài khoản
    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "is_phone_verified")
    @Builder.Default
    private Boolean isPhoneVerified = false;

    @Column(name = "is_student_verified")
    @Builder.Default
    private Boolean isStudentVerified = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;
}
