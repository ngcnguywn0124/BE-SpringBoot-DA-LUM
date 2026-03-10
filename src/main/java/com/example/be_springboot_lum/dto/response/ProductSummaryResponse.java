package com.example.be_springboot_lum.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response rút gọn cho tin đăng (dùng trong danh sách, trang chủ, trending).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSummaryResponse {

    private UUID productId;
    private String title;
    private String slug;
    private String condition;
    private BigDecimal price;
    private Boolean isFree;
    private Boolean isNegotiable;
    private String listingType;
    private String status;
    private Integer viewCount;
    private Integer favoriteCount;
    private Boolean isFeatured;
    private Integer imageCount;
    private Integer renewalCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;

    // ─── Ảnh thumbnail ───────────────────────────────────────────────────────
    private String thumbnailUrl;

    // ─── Danh mục ────────────────────────────────────────────────────────────
    private UUID categoryId;
    private String categoryName;

    // ─── Trường học ──────────────────────────────────────────────────────────
    private String universityShortName;
    private String campusName;

    // ─── Người bán ───────────────────────────────────────────────────────────
    private UUID sellerId;
    private String sellerName;
    private String sellerAvatar;
}
