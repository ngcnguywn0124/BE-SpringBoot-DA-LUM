package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "blog_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "blog_category_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID blogCategoryId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "post_count")
    private Integer postCount;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
