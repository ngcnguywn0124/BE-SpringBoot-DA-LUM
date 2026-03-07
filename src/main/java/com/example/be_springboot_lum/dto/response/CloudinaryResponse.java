package com.example.be_springboot_lum.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudinaryResponse {
    private String publicId;
    private String url;
}
