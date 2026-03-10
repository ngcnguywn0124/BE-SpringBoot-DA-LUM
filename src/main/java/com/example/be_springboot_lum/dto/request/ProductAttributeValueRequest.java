package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Một giá trị thuộc tính động trong ProductRequest.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeValueRequest {

    @NotNull(message = "attributeId không được để trống")
    private UUID attributeId;

    @NotBlank(message = "Giá trị thuộc tính không được để trống")
    private String value;
}
