package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bảng: products
 * Tin đăng bán / trao đổi đồ cũ của sinh viên.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID productId;

    // ─── Quan hệ ──────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    // ─── Thông tin cơ bản ─────────────────────────────────────────────────────

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "slug", unique = true, length = 255)
    private String slug;

    /**
     * new | like_new | used | old | broken
     */
    @Column(name = "condition", nullable = false, length = 20)
    private String condition;

    // ─── Giá ─────────────────────────────────────────────────────────────────

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "is_free")
    @Builder.Default
    private Boolean isFree = false;

    @Column(name = "is_negotiable")
    @Builder.Default
    private Boolean isNegotiable = true;

    // ─── Hình thức đăng tin ──────────────────────────────────────────────────

    /**
     * sell | exchange | both
     */
    @Column(name = "listing_type", nullable = false, length = 20)
    @Builder.Default
    private String listingType = "sell";

    @Column(name = "exchange_preferences", columnDefinition = "TEXT")
    private String exchangePreferences;

    // ─── Hình thức giao nhận ─────────────────────────────────────────────────

    /**
     * meetup | delivery | both
     */
    @Column(name = "transaction_type", nullable = false, length = 20)
    @Builder.Default
    private String transactionType = "meetup";

    @Column(name = "meeting_point", columnDefinition = "TEXT")
    private String meetingPoint;

    // ─── Thông tin liên hệ bổ sung ────────────────────────────────────────────

    @Column(name = "contact_name", length = 255)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "zalo_link", columnDefinition = "TEXT")
    private String zaloLink;

    @Column(name = "facebook_link", columnDefinition = "TEXT")
    private String facebookLink;

    // ─── Trạng thái ──────────────────────────────────────────────────────────

    /**
     * available | pending | hidden | expired | sold | deleted | admin_hidden
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    // ─── Thời hạn ──────────────────────────────────────────────────────────

    @Column(name = "expiry_days")
    @Builder.Default
    private Integer expiryDays = 30;

    @Column(name = "renewal_count")
    @Builder.Default
    private Integer renewalCount = 0;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    // ─── Thống kê ────────────────────────────────────────────────────────────

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "favorite_count")
    @Builder.Default
    private Integer favoriteCount = 0;

    @Column(name = "message_count")
    @Builder.Default
    private Integer messageCount = 0;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    // ─── Timestamp ───────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "sold_at")
    private OffsetDateTime soldAt;

    // ─── Quan hệ con ─────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC, createdAt ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductAttributeValue> attributeValues = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductTag> productTags = new ArrayList<>();
}
