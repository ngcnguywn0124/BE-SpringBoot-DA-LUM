package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    private Long id;
    private String name;
    private String description;
    private String resource;
    private String action;
    private LocalDateTime createdAt;
}
