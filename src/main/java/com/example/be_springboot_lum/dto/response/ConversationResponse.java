package com.example.be_springboot_lum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private UUID conversationId;
    
    // Other participant details
    private UUID otherUserId;
    private String otherUserName;
    private String otherUserAvatarUrl;
    private Boolean otherUserOnline;
    private OffsetDateTime otherUserLastSeenAt;
    
    // Linked Product details (optional)
    private UUID productId;
    private String productTitle;
    private String productImageUrl;
    private String productSlug;
    private java.math.BigDecimal productPrice;
    
    // Seller of the product
    private UUID sellerId;
    private String sellerPhone;
    
    // Last Message overview
    private String lastMessagePreview;
    private OffsetDateTime lastMessageAt;
    private Boolean isUnread;

    // Self state
    private Boolean isPinned;
    private OffsetDateTime joinedAt;

    // Transaction info
    private UUID transactionId;
    private String transactionStatus;
}
