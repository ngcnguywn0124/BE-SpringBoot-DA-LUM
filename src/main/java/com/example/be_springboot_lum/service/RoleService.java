package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.AssignPermissionsRequest;
import com.example.be_springboot_lum.dto.request.RoleRequest;
import com.example.be_springboot_lum.dto.response.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    List<RoleResponse> getAllRoles();

    RoleResponse getRoleById(UUID id);

    RoleResponse createRole(RoleRequest request);

    RoleResponse updateRole(UUID id, RoleRequest request);

    void deleteRole(UUID id);

    com.example.be_springboot_lum.dto.request.BulkIdsRequest bulkDeleteRoles(com.example.be_springboot_lum.dto.request.BulkIdsRequest request);

    RoleResponse assignPermissions(UUID roleId, AssignPermissionsRequest request);

    RoleResponse revokePermissions(UUID roleId, AssignPermissionsRequest request);

    org.springframework.data.domain.Page<com.example.be_springboot_lum.dto.response.UserResponse> getUsersByRole(UUID roleId, org.springframework.data.domain.Pageable pageable, String search);

    void bulkAssignRoleToUsers(UUID roleId, com.example.be_springboot_lum.dto.request.BulkUserIdsRequest request);

    void bulkAssignRoleByEmails(UUID roleId, List<String> emails);

    void bulkRevokeRoleFromUsers(UUID roleId, com.example.be_springboot_lum.dto.request.BulkUserIdsRequest request);

    List<RoleResponse> getUserRoles(UUID userId);

    void setUserRoles(UUID userId, com.example.be_springboot_lum.dto.request.BulkIdsRequest request);

    void addUserRole(UUID userId, UUID roleId);

    void removeUserRole(UUID userId, UUID roleId);

    List<com.example.be_springboot_lum.dto.response.PermissionResponse> getUserPermissions(UUID userId);

    boolean checkUserPermission(UUID userId, String resource, String action);
}
