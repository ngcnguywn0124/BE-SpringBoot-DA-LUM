package com.example.be_springboot_lum.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bảng: transactions
 * Vòng đời trạng thái:
 * buyer_requested → seller_confirmed → meetup_confirmed → payment_pending →
 * completed
 * → cancelled (bất kỳ giai đoạn)
 * → disputed
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID transactionId;

    // ─── Quan hệ ──────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    // ─── Loại giao dịch ────────────────────────────────────────────────────

    /** sale | exchange */
    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    // ─── Trạng thái ────────────────────────────────────────────────────────

    /**
     * buyer_requested | seller_confirmed | meetup_confirmed |
     * payment_pending | completed | cancelled | disputed
     */
    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "buyer_requested";

    // ─── Thông tin giá ────────────────────────────────────────────────────

    @Column(name = "agreed_price", precision = 12, scale = 2)
    private BigDecimal agreedPrice;

    /** cash | transfer */
    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    // ─── Thông tin gặp mặt ────────────────────────────────────────────────

    @Column(name = "meetup_location", columnDefinition = "TEXT")
    private String meetupLocation;

    @Column(name = "meetup_time")
    private OffsetDateTime meetupTime;

    // ─── Xác nhận song phương – gặp mặt ─────────────────────────────────

    @Column(name = "buyer_confirmed_meetup")
    @Builder.Default
    private Boolean buyerConfirmedMeetup = false;

    @Column(name = "seller_confirmed_meetup")
    @Builder.Default
    private Boolean sellerConfirmedMeetup = false;

    // ─── Xác nhận song phương – thanh toán ───────────────────────────────

    @Column(name = "buyer_confirmed_payment")
    @Builder.Default
    private Boolean buyerConfirmedPayment = false;

    @Column(name = "seller_confirmed_payment")
    @Builder.Default
    private Boolean sellerConfirmedPayment = false;

    // ─── Hình thức giao nhận ─────────────────────────────────────────────

    /** meetup | delivery */
    @Column(name = "shipping_method", length = 30)
    private String shippingMethod;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee;

    // ─── Trao đổi ────────────────────────────────────────────────────────

    @Column(name = "exchange_product_id", columnDefinition = "uuid")
    private UUID exchangeProductId;

    @Column(name = "additional_payment", precision = 12, scale = 2)
    private BigDecimal additionalPayment;

    // ─── Huỷ / tranh chấp ────────────────────────────────────────────────

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_by", columnDefinition = "uuid")
    private UUID cancelledBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ─── Timeline ─────────────────────────────────────────────────────────

    @Column(name = "requested_at")
    private OffsetDateTime requestedAt;

    @Column(name = "seller_confirmed_at")
    private OffsetDateTime sellerConfirmedAt;

    @Column(name = "meetup_confirmed_at")
    private OffsetDateTime meetupConfirmedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    // ─── Timestamps ──────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
