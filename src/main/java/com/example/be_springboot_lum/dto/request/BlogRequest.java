package com.example.be_springboot_lum.dto.request;

import com.example.be_springboot_lum.model.BlogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

    import org.springframework.web.multipart.MultipartFile;
    // ...

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class BlogRequest {
        @NotNull(message = "Danh mục blog là bắt buộc")
        private UUID categoryId;

        @NotBlank(message = "Tiêu đề bài viết là bắt buộc")
        @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
        private String title;

        @NotBlank(message = "Mô tả ngắn là bắt buộc")
        @Size(max = 500, message = "Mô tả ngắn không được vượt quá 500 ký tự")
        private String excerpt;

        @NotBlank(message = "Nội dung bài viết là bắt buộc")
        private String content;

        private MultipartFile thumbnail;
        private String thumbnailCloudId; // kept for legacy or internal usage if needed
        private BlogStatus status;
        private Boolean isFeatured;
    }
