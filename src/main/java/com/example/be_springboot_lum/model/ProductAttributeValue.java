package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Bảng: product_attribute_values
 * Giá trị thuộc tính động của sản phẩm (vd: RAM=8GB, ROM=256GB).
 */
@Entity
@Table(name = "product_attribute_values",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "attribute_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "value_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID valueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private ProductAttribute attribute;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;
}
