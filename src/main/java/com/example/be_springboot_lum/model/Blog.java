package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "blogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "blog_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID blogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_category_id", nullable = false)
    private BlogCategory blogCategory;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "excerpt", nullable = false, length = 500)
    private String excerpt;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "thumbnail", columnDefinition = "TEXT")
    private String thumbnail;

    @Column(name = "thumbnail_cloud_id", length = 255)
    private String thumbnailCloudId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BlogStatus status = BlogStatus.draft;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
