package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 255, message = "Tên danh mục không được vượt quá 255 ký tự")
    private String categoryName;

    /** null = danh mục gốc */
    private UUID parentCategoryId;

    private String description;

    @Min(value = 0, message = "Thứ tự hiển thị không được âm")
    @Max(value = 9999, message = "Thứ tự hiển thị không được vượt quá 9999")
    private Integer displayOrder;

    private Boolean isActive;
}
