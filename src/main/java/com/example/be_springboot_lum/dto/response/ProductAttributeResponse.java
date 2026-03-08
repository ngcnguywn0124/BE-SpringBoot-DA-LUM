package com.example.be_springboot_lum.dto.response;

import com.example.be_springboot_lum.model.AttributeType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductAttributeResponse {

    private UUID attributeId;

    // Thông tin danh mục cha
    private UUID categoryId;
    private String categoryName;

    private String attributeName;

    /** Kiểu dữ liệu: "text" | "number" | "boolean" | "select" */
    private AttributeType attributeType;

    private Boolean isRequired;

    /** Danh sách lựa chọn (chỉ có khi attributeType = SELECT) */
    private List<String> options;

    private Integer displayOrder;
}
