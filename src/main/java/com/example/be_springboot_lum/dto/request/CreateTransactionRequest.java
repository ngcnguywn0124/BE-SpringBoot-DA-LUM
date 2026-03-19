package com.example.be_springboot_lum.dto.request;

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
public class CreateTransactionRequest {

    /** UUID của sản phẩm muốn mua/trao đổi */
    private UUID productId;

    /** sale | exchange */
    private String transactionType;

    /** Giá đã thoả thuận */
    private BigDecimal agreedPrice;

    /** cash | transfer */
    private String paymentMethod;

    /** meetup | delivery */
    private String shippingMethod;

    private String meetupLocation;
    private OffsetDateTime meetupTime;

    private String notes;
}
