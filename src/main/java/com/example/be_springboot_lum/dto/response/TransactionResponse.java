package com.example.be_springboot_lum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID transactionId;

    // ─── Thông tin sản phẩm ───────────────────────────────────────────────
    private UUID productId;
    private String productTitle;
    private String productSlug;
    private String productImageUrl;
    private BigDecimal productPrice;

    // ─── Buyer ───────────────────────────────────────────────────────────
    private UUID buyerId;
    private String buyerName;
    private String buyerAvatarUrl;

    // ─── Seller ──────────────────────────────────────────────────────────
    private UUID sellerId;
    private String sellerName;
    private String sellerAvatarUrl;

    // ─── Thông tin giao dịch ──────────────────────────────────────────────
    private String transactionType;
    private String status;
    private BigDecimal agreedPrice;
    private String paymentMethod;
    private String shippingMethod;
    private String meetupLocation;
    private OffsetDateTime meetupTime;

    // ─── Xác nhận ─────────────────────────────────────────────────────────
    private Boolean buyerConfirmedMeetup;
    private Boolean sellerConfirmedMeetup;
    private Boolean buyerConfirmedPayment;
    private Boolean sellerConfirmedPayment;

    // ─── Huỷ / tranh chấp ─────────────────────────────────────────────────
    private String cancellationReason;
    private UUID cancelledBy;

    private String notes;

    // ─── Timeline ─────────────────────────────────────────────────────────
    private OffsetDateTime requestedAt;
    private OffsetDateTime sellerConfirmedAt;
    private OffsetDateTime meetupConfirmedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime cancelledAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // ─── Lịch sử trạng thái ───────────────────────────────────────────────
    private List<TransactionStatusHistoryResponse> statusHistory;
}
