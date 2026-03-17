package com.example.be_springboot_lum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private UUID messageId;
    private UUID conversationId;
    
    private UUID senderId;
    private String senderName;
    private String senderAvatarUrl;

    private String messageType;
    private String content;
    private String attachmentUrl;
    private BigDecimal offerAmount;
    private String transactionEventType;

    private String deliveryStatus;
    private Boolean isEdited;
    
    private OffsetDateTime createdAt;
}
