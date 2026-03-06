package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.AssignPermissionsRequest;
import com.example.be_springboot_lum.dto.request.RoleRequest;
import com.example.be_springboot_lum.dto.response.PermissionResponse;
import com.example.be_springboot_lum.dto.response.RoleResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Permission;
import com.example.be_springboot_lum.model.Role;
import com.example.be_springboot_lum.repository.PermissionRepository;
import com.example.be_springboot_lum.repository.RoleRepository;
import com.example.be_springboot_lum.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.ROLE_ALREADY_EXISTS);
        }
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // Kiểm tra tên trùng (trừ chính nó)
        if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.ROLE_ALREADY_EXISTS);
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        roleRepository.delete(role);
    }

    @Override
    @Transactional
    public RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
        if (permissions.size() != request.getPermissionIds().size()) {
            throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
        }

        role.getPermissions().addAll(new HashSet<>(permissions));
        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse revokePermissions(Long roleId, AssignPermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<Long> idsToRevoke = new HashSet<>(request.getPermissionIds());
        role.getPermissions().removeIf(p -> idsToRevoke.contains(p.getId()));

        return toResponse(roleRepository.save(role));
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private RoleResponse toResponse(Role role) {
        Set<PermissionResponse> permissionResponses = role.getPermissions() == null ? Set.of() :
                role.getPermissions().stream().map(p -> PermissionResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .resource(p.getResource())
                        .action(p.getAction())
                        .createdAt(p.getCreatedAt())
                        .build()).collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissionResponses)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
