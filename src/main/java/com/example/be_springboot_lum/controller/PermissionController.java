package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.response.PermissionResponse;
import com.example.be_springboot_lum.model.Permission;
import com.example.be_springboot_lum.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * READ-ONLY listing of all permissions.
 * GET /api/v1/permissions   — ADMIN / SUPER_ADMIN only
 */
@RestController
@RequestMapping("${api.prefix}/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        List<PermissionResponse> list = permissionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    private PermissionResponse toResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .resource(p.getResource())
                .action(p.getAction())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
