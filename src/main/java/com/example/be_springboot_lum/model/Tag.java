package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tags / từ khóa cho sản phẩm.
 * Bảng: tags
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tag_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID tagId;

    @Column(name = "tag_name", nullable = false, unique = true, length = 100)
    private String tagName;

    @Column(name = "slug", unique = true, length = 150)
    private String slug;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
