package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "approved|rejected", message = "Trạng thái chỉ có thể là approved hoặc rejected")
    private String status;

    private String rejectionReason;
}
