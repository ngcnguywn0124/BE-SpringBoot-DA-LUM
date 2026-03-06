package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.AssignPermissionsRequest;
import com.example.be_springboot_lum.dto.request.RoleRequest;
import com.example.be_springboot_lum.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {

    List<RoleResponse> getAllRoles();

    RoleResponse getRoleById(Long id);

    RoleResponse createRole(RoleRequest request);

    RoleResponse updateRole(Long id, RoleRequest request);

    void deleteRole(Long id);

    RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request);

    RoleResponse revokePermissions(Long roleId, AssignPermissionsRequest request);
}
