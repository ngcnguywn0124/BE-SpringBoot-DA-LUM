package com.example.be_springboot_lum.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCommentResponse {
    private UUID commentId;
    private UUID productId;
    private UUID userId;
    private String userName;
    private String userAvatarUrl;
    private String content;
    private UUID parentCommentId;
    private Integer likeCount;
    private Boolean likedByCurrentUser;
    private List<ProductCommentResponse> replies;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
