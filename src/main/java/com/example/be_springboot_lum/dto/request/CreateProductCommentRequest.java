package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProductCommentRequest {

    @NotBlank(message = "Nội dung bình luận không được để trống")
    @Size(max = 1000, message = "Bình luận không được vượt quá 1000 ký tự")
    private String content;
}
