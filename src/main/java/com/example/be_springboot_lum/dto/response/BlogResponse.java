package com.example.be_springboot_lum.dto.response;

import com.example.be_springboot_lum.model.BlogStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogResponse {
    private UUID blogId;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String thumbnail;
    private String thumbnailCloudId;
    private BlogStatus status;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isFeatured;
    private OffsetDateTime publishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Nested info
    private BlogCategoryResponse blogCategory;
    private UserResponse author;
    private UserResponse approvedBy;
    private String rejectionReason;
}
