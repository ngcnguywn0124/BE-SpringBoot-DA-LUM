package com.example.be_springboot_lum.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response cho một ảnh sản phẩm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductImageResponse {

    private UUID imageId;
    private String imageUrl;
    private String imageCloudId;
    private Integer displayOrder;
    private Boolean isPrimary;
    private OffsetDateTime createdAt;
}
