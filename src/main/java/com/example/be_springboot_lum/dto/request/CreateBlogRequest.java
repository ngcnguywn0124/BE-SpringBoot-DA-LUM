package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CreateBlogRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 10, max = 200, message = "Tiêu đề bài viết cần từ 10 đến 200 ký tự")
    private String title;

    @NotBlank(message = "Mô tả ngắn không được để trống")
    @Size(min = 20, max = 500, message = "Mô tả ngắn cần từ 20 đến 500 ký tự")
    private String excerpt;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(min = 100, message = "Nội dung bài viết cần tối thiểu 100 ký tự")
    private String content;

    @NotBlank(message = "Chuyên mục không được để trống")
    private String category;

    private MultipartFile thumbnail;
}
