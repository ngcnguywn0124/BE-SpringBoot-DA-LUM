package com.example.be_springboot_lum.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class CreateReviewRequest {
    private UUID transactionId;
    private Integer rating;
    private String comment;
}
