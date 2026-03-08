package com.example.be_springboot_lum.dto.request;

import com.example.be_springboot_lum.model.AttributeType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeRequest {

    @NotNull(message = "Danh mục không được để trống")
    private UUID categoryId;

    @NotBlank(message = "Tên thuộc tính không được để trống")
    @Size(max = 100, message = "Tên thuộc tính không vượt quá 100 ký tự")
    private String attributeName;

    @NotNull(message = "Kiểu dữ liệu không được để trống")
    private AttributeType attributeType;

    private Boolean isRequired;

    /**
     * Danh sách lựa chọn – bắt buộc khi attributeType = SELECT,
     * phải có ít nhất 2 lựa chọn.
     */
    private List<String> options;

    @Min(value = 0, message = "Thứ tự hiển thị không được âm")
    @Max(value = 9999, message = "Thứ tự hiển thị không được vượt quá 9999")
    private Integer displayOrder;
}
