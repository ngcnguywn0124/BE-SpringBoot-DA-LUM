package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.AssignPermissionsRequest;
import com.example.be_springboot_lum.dto.request.RoleRequest;
import com.example.be_springboot_lum.dto.response.RoleResponse;
import com.example.be_springboot_lum.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Role Controller
 *
 * GET    /api/v1/roles                              - Lấy danh sách role (ADMIN / SUPER_ADMIN)
 * GET    /api/v1/roles/{id}                         - Lấy chi tiết role (ADMIN / SUPER_ADMIN)
 * POST   /api/v1/roles                              - Tạo role mới (SUPER_ADMIN)
 * PUT    /api/v1/roles/{id}                         - Cập nhật role (SUPER_ADMIN)
 * DELETE /api/v1/roles/{id}                         - Xóa role (SUPER_ADMIN)
 * POST   /api/v1/roles/{id}/permissions             - Gán permissions vào role (SUPER_ADMIN)
 * DELETE /api/v1/roles/{id}/permissions             - Thu hồi permissions khỏi role (SUPER_ADMIN)
 */
@RestController
@RequestMapping("${api.prefix}/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * Lấy danh sách tất cả role kèm permissions.
     * Yêu cầu quyền ADMIN hoặc SUPER_ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllRoles()));
    }

    /**
     * Lấy chi tiết một role theo ID.
     * Yêu cầu quyền ADMIN hoặc SUPER_ADMIN.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getRoleById(id)));
    }

    /**
     * Tạo role mới.
     * Chỉ SUPER_ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(roleService.createRole(request)));
    }

    /**
     * Cập nhật role.
     * Chỉ SUPER_ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật role thành công",
                roleService.updateRole(id, request)));
    }

    /**
     * Xóa role.
     * Chỉ SUPER_ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa role thành công", null));
    }

    /**
     * Gán permissions vào role.
     * Chỉ SUPER_ADMIN.
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> assignPermissions(
            @PathVariable UUID id,
            @Valid @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Gán quyền thành công",
                roleService.assignPermissions(id, request)));
    }

    /**
     * Thu hồi permissions khỏi role.
     * Chỉ SUPER_ADMIN.
     */
    @DeleteMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> revokePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Thu hồi quyền thành công",
                roleService.revokePermissions(id, request)));
    }
}
