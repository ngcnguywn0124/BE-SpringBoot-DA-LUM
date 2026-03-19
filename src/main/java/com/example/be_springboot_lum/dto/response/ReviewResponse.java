package com.example.be_springboot_lum.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID reviewId;
    private UUID transactionId;
    private String productName;
    private UUID reviewerId;
    private String reviewerName;
    private String reviewerAvatarUrl;
    private UUID revieweeId;
    private Integer rating;
    private String comment;
    private Boolean isVerifiedPurchase;
    private OffsetDateTime createdAt;
}
