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

    RoleResponse assignPermissions(UUID roleId, AssignPermissionsRequest request);

    RoleResponse revokePermissions(UUID roleId, AssignPermissionsRequest request);
}
