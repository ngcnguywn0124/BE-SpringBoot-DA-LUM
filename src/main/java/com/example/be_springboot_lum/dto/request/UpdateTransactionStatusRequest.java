package com.example.be_springboot_lum.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransactionStatusRequest {

    /**
     * Trạng thái mới muốn chuyển sang.
     * buyer_requested | seller_confirmed | meetup_confirmed |
     * payment_pending | completed | cancelled | disputed
     */
    private String status;

    /** Ghi chú kèm theo khi thay đổi trạng thái */
    private String notes;

    /** Cập nhật địa điểm gặp (tuỳ chọn khi confirm meetup) */
    private String meetupLocation;

    /** Cập nhật thời gian gặp (tuỳ chọn) */
    private OffsetDateTime meetupTime;

    /** Cập nhật giá thoả thuận (tuỳ chọn) */
    private BigDecimal agreedPrice;

    /** Lý do huỷ (bắt buộc khi status = 'cancelled') */
    private String cancellationReason;

    /** Cập nhật phương thức thanh toán (tuỳ chọn khi confirm meetup) */
    private String paymentMethod;
}
