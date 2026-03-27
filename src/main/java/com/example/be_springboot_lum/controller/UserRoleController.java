package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.BulkIdsRequest;
import com.example.be_springboot_lum.dto.response.PermissionResponse;
import com.example.be_springboot_lum.dto.response.RoleResponse;
import com.example.be_springboot_lum.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UserRole Controller
 *
 * GET    /api/v1/users/{userId}/roles           - Lấy danh sách role của user (SUPER_ADMIN)
 * PUT    /api/v1/users/{userId}/roles           - Đặt lại danh sách role cho user (SUPER_ADMIN)
 * POST   /api/v1/users/{userId}/roles           - Thêm 1 role cho user (SUPER_ADMIN)
 * DELETE /api/v1/users/{userId}/roles           - Xóa 1 role khỏi user (SUPER_ADMIN)
 * GET    /api/v1/users/{userId}/permissions     - Lấy danh sách permissions flatten của user (SUPER_ADMIN)
 * GET    /api/v1/users/{userId}/permissions/check - Kiểm tra permission cụ thể (SUPER_ADMIN)
 */
@RestController
@RequestMapping("${api.prefix}/users")
@RequiredArgsConstructor
public class UserRoleController {

    private final RoleService roleService;

    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getUserRoles(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getUserRoles(userId)));
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setUserRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody BulkIdsRequest request) {
        roleService.setUserRoles(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật roles cho người dùng thành công", null));
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addUserRole(
            @PathVariable UUID userId,
            @RequestBody Map<String, UUID> requestBody) {
        UUID roleId = requestBody.get("roleId");
        if (roleId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.success("Thiếu roleId", null)); // should be error response
        }
        roleService.addUserRole(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success("Thêm role thành công", null));
    }

    @DeleteMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeUserRole(
            @PathVariable UUID userId,
            @RequestBody Map<String, UUID> requestBody) {
        UUID roleId = requestBody.get("roleId");
        if (roleId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.success("Thiếu roleId", null));
        }
        roleService.removeUserRole(userId, roleId);
        return ResponseEntity.ok(ApiResponse.success("Thu hồi role thành công", null));
    }

    @GetMapping("/{userId}/permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getUserPermissions(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getUserPermissions(userId)));
    }

    @GetMapping("/{userId}/permissions/check")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkUserPermission(
            @PathVariable UUID userId,
            @RequestParam String resource,
            @RequestParam String action) {
        boolean hasPermission = roleService.checkUserPermission(userId, resource, action);
        Map<String, Object> data = Map.of(
            "hasPermission", hasPermission,
            "permission", resource.toUpperCase() + ":" + action.toUpperCase()
        );
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra quyền thành công", data));
    }
}
