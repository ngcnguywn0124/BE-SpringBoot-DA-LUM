package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private Long id;
    private String name;
    private String description;
    private Set<PermissionResponse> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
