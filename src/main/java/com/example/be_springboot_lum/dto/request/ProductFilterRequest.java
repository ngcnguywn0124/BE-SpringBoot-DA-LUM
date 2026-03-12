package com.example.be_springboot_lum.dto.request;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tham số lọc sản phẩm (query params).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterRequest {

    private UUID categoryId;
    private UUID universityId;
    private UUID campusId;

    /** sell | exchange | both */
    private String listingType;

    /** new | like_new | used | old | broken */
    private String condition;

    private Boolean isFree;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    /** Từ khóa tìm kiếm */
    private String keyword;

    // ─── Phân trang & sắp xếp ─────────────────────────────────────────────────

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    /** Ví dụ: "createdAt,desc" | "price,asc" */
    @Builder.Default
    private String sort = "createdAt,desc";
}
