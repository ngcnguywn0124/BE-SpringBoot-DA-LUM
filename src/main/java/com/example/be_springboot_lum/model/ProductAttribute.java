package com.example.be_springboot_lum.model;

import com.example.be_springboot_lum.model.converter.AttributeTypeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * Định nghĩa thuộc tính động của sản phẩm theo danh mục.
 * Admin cấu hình bộ thuộc tính cho từng danh mục (ví dụ: RAM, ROM, Màu sắc...).
 * Bảng: product_attributes
 */
@Entity
@Table(name = "product_attributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attribute_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID attributeId;

    // ─── Danh mục áp dụng ────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // ─── Thông tin thuộc tính ─────────────────────────────────────────────────

    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    /**
     * Kiểu dữ liệu: text | number | boolean | select.
     * Dùng converter để lưu chữ thường xuống DB.
     */
    @Convert(converter = AttributeTypeConverter.class)
    @Column(name = "attribute_type", length = 20)
    private AttributeType attributeType;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = false;

    /**
     * Danh sách lựa chọn (chỉ dùng khi attributeType = SELECT).
     * Lưu dạng JSON array, ví dụ: ["Đỏ","Xanh","Trắng"]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<String> options;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
}
