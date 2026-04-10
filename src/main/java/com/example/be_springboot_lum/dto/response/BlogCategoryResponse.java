package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogCategoryResponse {
    private UUID blogCategoryId;
    private String name;
    private String slug;
    private String description;
    private String color;
    private String icon;
    private Integer displayOrder;
    private Integer postCount;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
