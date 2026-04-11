package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "product_comment_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_comment_like_comment_user", columnNames = {"comment_id", "user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "like_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID likeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private ProductComment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private OffsetDateTime updatedAt;
}
