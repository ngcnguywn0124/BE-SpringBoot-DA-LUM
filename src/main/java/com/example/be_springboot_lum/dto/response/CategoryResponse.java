package com.example.be_springboot_lum.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {

    private UUID categoryId;
    private String categoryName;
    private String slug;
    private String description;
    private String imageUrl;
    private String imageCloudId;
    private String iconName;
    private Integer displayOrder;
    private Boolean isActive;
    private OffsetDateTime createdAt;

    // Thông tin cha (chỉ id + tên, tránh vòng lặp)
    private UUID parentCategoryId;
    private String parentCategoryName;

    // Danh sách danh mục con trực tiếp (lazy – chỉ trả khi cần)
    private List<CategoryResponse> children;
}
