package com.example.be_springboot_lum.dto.response;

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
    private String category;
    private String thumbnail;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isFeatured;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime approvedAt;
    private AuthorResponse author;
    private String rejectionReason;

    @Getter
    @Setter
    @Builder
    public static class AuthorResponse {
        private UUID userId;
        private String fullName;
        private String avatar;
    }
}
