package com.example.be_springboot_lum.dto.request;

import com.example.be_springboot_lum.model.BlogStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogStatusRequest {
    @NotNull(message = "Trạng thái bài viết là bắt buộc")
    private BlogStatus status;
}
