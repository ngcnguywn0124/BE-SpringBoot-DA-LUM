package com.example.be_springboot_lum.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response đầy đủ cho một tin đăng (dùng cho trang chi tiết).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private UUID productId;
    private String title;
    private String description;
    private String slug;
    private String condition;

    // ─── Giá ─────────────────────────────────────────────────────────────────
    private BigDecimal price;
    private Boolean isFree;
    private Boolean isNegotiable;

    // ─── Hình thức ────────────────────────────────────────────────────────────
    private String listingType;
    private String exchangePreferences;
    private String transactionType;
    private String meetingPoint;

    // ─── Liên hệ ─────────────────────────────────────────────────────────────
    private String contactName;
    private String contactPhone;
    private String zaloLink;
    private String facebookLink;

    // ─── Trạng thái & thống kê ───────────────────────────────────────────────
    private String status;
    private String previousStatus;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer messageCount;
    private Integer expiryDays;
    private Integer renewalCount;
    private Boolean isFeatured;
    private OffsetDateTime expiresAt;

    // ─── Timestamps ───────────────────────────────────────────────────────────
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime approvedAt;
    private OffsetDateTime soldAt;

    // ─── Danh mục ────────────────────────────────────────────────────────────
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;

    // ─── Trường & cơ sở ──────────────────────────────────────────────────────
    private UUID universityId;
    private String universityName;
    private String universityShortName;
    private UUID campusId;
    private String campusName;

    // ─── Người bán ───────────────────────────────────────────────────────────
    private UUID sellerId;
    private String sellerName;
    private String sellerAvatar;
    private Double sellerReputation;
    private Long sellerTotalSales;

    // ─── Quan hệ ─────────────────────────────────────────────────────────────
    private List<ProductImageResponse> images;
    private List<ProductAttributeValueResponse> attributeValues;
    private List<TagResponse> tags;
}
