package com.example.be_springboot_lum.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.UUID;

/**
 * Response cho một giá trị thuộc tính động của sản phẩm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductAttributeValueResponse {

    private UUID attributeId;
    private String attributeName;
    private String attributeType;
    private String value;
}
