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
public class TransactionStatusHistoryResponse {

    private UUID historyId;
    private String status;
    private UUID changedById;
    private String changedByName;
    private String notes;
    private OffsetDateTime createdAt;
}
