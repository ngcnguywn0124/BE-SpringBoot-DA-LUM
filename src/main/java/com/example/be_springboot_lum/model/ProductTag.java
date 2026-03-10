package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Bảng: product_tags
 * Liên kết sản phẩm với các tag / từ khóa.
 */
@Entity
@Table(name = "product_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "tag_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_tag_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID productTagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
