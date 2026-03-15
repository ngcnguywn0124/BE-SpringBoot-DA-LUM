package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteResponse {

    private UUID favoriteId;
    private UUID productId;
    private OffsetDateTime savedAt;
    private ProductSummaryResponse product;
}
