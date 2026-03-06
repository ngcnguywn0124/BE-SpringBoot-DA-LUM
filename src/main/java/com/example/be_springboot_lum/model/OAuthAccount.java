package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lưu thông tin đăng nhập qua mạng xã hội (OAuth).
 * provider: google | facebook
 */
@Entity
@Table(
    name = "oauth_accounts",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_oauth_provider_provider_user",
        columnNames = {"provider", "provider_user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "oauth_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID oauthId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** "google" hoặc "facebook" */
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /** ID của user bên phía Google/Facebook */
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
