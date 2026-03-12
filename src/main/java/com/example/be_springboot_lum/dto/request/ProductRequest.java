package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request body khi tạo mới hoặc cập nhật tin đăng.
 * Gửi dưới dạng multipart/form-data:
 *   - part "data"   : JSON của ProductRequest
 *   - part "images" : danh sách file ảnh (tối đa 10)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    // ─── Thông tin cơ bản ─────────────────────────────────────────────────────

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 120, message = "Tiêu đề không được vượt quá 120 ký tự")
    private String title;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotNull(message = "Danh mục không được để trống")
    private UUID categoryId;

    /**
     * new | like_new | used | old | broken
     */
    @NotBlank(message = "Tình trạng sản phẩm không được để trống")
    @Pattern(regexp = "new|like_new|used|old|broken",
             message = "Tình trạng không hợp lệ (new, like_new, used, old, broken)")
    private String condition;

    // ─── Giá ─────────────────────────────────────────────────────────────────

    @DecimalMin(value = "0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    private Boolean isFree;

    private Boolean isNegotiable;

    // ─── Hình thức đăng tin ──────────────────────────────────────────────────

    /**
     * sell | exchange | both
     */
    @Pattern(regexp = "sell|exchange|both",
             message = "Hình thức đăng không hợp lệ (sell, exchange, both)")
    private String listingType;

    private String exchangePreferences;

    // ─── Hình thức giao nhận ─────────────────────────────────────────────────

    /**
     * meetup | delivery | both
     */
    @Pattern(regexp = "meetup|delivery|both",
             message = "Hình thức giao nhận không hợp lệ (meetup, delivery, both)")
    private String transactionType;

    private String meetingPoint;

    // ─── Vị trí ──────────────────────────────────────────────────────────────

    private UUID universityId;

    private UUID campusId;

    // ─── Liên hệ ─────────────────────────────────────────────────────────────

    @Size(max = 255)
    private String contactName;

    @Size(max = 20)
    private String contactPhone;

    private String zaloLink;

    private String facebookLink;
    private Integer expireDays;
    // ─── Thuộc tính động ─────────────────────────────────────────────────────

    /** Danh sách giá trị thuộc tính động theo danh mục */
    private List<ProductAttributeValueRequest> attributeValues;

    // ─── Tags ────────────────────────────────────────────────────────────────

    /** Danh sách tag_id đã có sẵn trong hệ thống */
    private List<UUID> tagIds;

    /** Tên tag mới cần tạo (nếu chưa tồn tại) */
    private List<String> newTagNames;
}
