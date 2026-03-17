package com.example.be_springboot_lum.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private UUID conversationId;
    private String messageType; // text, image, images, offer
    private String content;
    private String attachmentUrl;
    private BigDecimal offerAmount;
}
