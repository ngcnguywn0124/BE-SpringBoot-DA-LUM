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

    /**
     * Xóa nhiều roles cùng lúc.
     * Chỉ SUPER_ADMIN.
     */
    @DeleteMapping("/bulk")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.example.be_springboot_lum.dto.request.BulkIdsRequest>> bulkDeleteRoles(
            @Valid @RequestBody com.example.be_springboot_lum.dto.request.BulkIdsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Xóa roles hàng loạt thành công",
                roleService.bulkDeleteRoles(request)));
    }

    /**
     * Lấy danh sách users có role này (phân trang & tìm kiếm).
     * Chỉ SUPER_ADMIN.
     */
    @GetMapping("/{id}/users")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getUsersByRole(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search) {

        if (page < 1) page = 1;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page - 1, limit);
        org.springframework.data.domain.Page<com.example.be_springboot_lum.dto.response.UserResponse> pagedResult =
                roleService.getUsersByRole(id, pageable, search);

        java.util.Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("data", pagedResult.getContent());

        java.util.Map<String, Object> pagination = new java.util.HashMap<>();
        pagination.put("total", pagedResult.getTotalElements());
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("totalPages", pagedResult.getTotalPages());

        responseData.put("pagination", pagination);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", responseData));
    }

    /**
     * Gán role cho nhiều users cùng lúc.
     * Chỉ SUPER_ADMIN.
     */
    @PostMapping("/{id}/users/bulk")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> bulkAssignRoleToUsers(
            @PathVariable UUID id,
            @Valid @RequestBody com.example.be_springboot_lum.dto.request.BulkUserIdsRequest request) {
        roleService.bulkAssignRoleToUsers(id, request);
        return ResponseEntity.ok(ApiResponse.success("Gán role cho người dùng thành công", null));
    }

    @PostMapping("/{id}/users/bulk-by-email")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> bulkAssignRoleByEmails(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, java.util.List<String>> request) {
        java.util.List<String> emails = request.get("emails");
        if (emails == null || emails.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.success("Vui lòng cung cấp danh sách email", null));
        }
        roleService.bulkAssignRoleByEmails(id, emails);
        return ResponseEntity.ok(ApiResponse.success("Gán role cho người dùng qua email thành công", null));
    }

    /**
     * Thu hồi role khỏi nhiều users.
     * Chỉ SUPER_ADMIN.
     */
    @DeleteMapping("/{id}/users/bulk")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> bulkRevokeRoleFromUsers(
            @PathVariable UUID id,
            @Valid @RequestBody com.example.be_springboot_lum.dto.request.BulkUserIdsRequest request) {
        roleService.bulkRevokeRoleFromUsers(id, request);
        return ResponseEntity.ok(ApiResponse.success("Thu hồi role của người dùng thành công", null));
    }
}
