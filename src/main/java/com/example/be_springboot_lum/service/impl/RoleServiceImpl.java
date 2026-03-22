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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final com.example.be_springboot_lum.repository.UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID id) {
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
    public RoleResponse updateRole(UUID id, RoleRequest request) {
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
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        roleRepository.delete(role);
    }

    @Override
    @Transactional
    public RoleResponse assignPermissions(UUID roleId, AssignPermissionsRequest request) {
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
    public RoleResponse revokePermissions(UUID roleId, AssignPermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<UUID> idsToRevoke = new HashSet<>(request.getPermissionIds());
        role.getPermissions().removeIf(p -> idsToRevoke.contains(p.getId()));

        return toResponse(roleRepository.save(role));
    }

    // ─── NEW METHODS ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public com.example.be_springboot_lum.dto.request.BulkIdsRequest bulkDeleteRoles(com.example.be_springboot_lum.dto.request.BulkIdsRequest request) {
        List<Role> roles = roleRepository.findAllById(request.getIds());
        roleRepository.deleteAll(roles);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.example.be_springboot_lum.dto.response.UserResponse> getUsersByRole(UUID roleId, org.springframework.data.domain.Pageable pageable, String search) {
        roleRepository.findById(roleId).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return userRepository.findUsersByRoleIdAndSearch(roleId, search, pageable).map(this::toUserResponse);
    }

    @Override
    @Transactional
    public void bulkAssignRoleToUsers(UUID roleId, com.example.be_springboot_lum.dto.request.BulkUserIdsRequest request) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        List<com.example.be_springboot_lum.model.User> users = userRepository.findAllById(request.getUserIds());
        for (com.example.be_springboot_lum.model.User user : users) {
            user.getRoles().add(role);
        }
        userRepository.saveAll(users);
    }

    @Override
    @Transactional
    public void bulkAssignRoleByEmails(UUID roleId, List<String> emails) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        List<com.example.be_springboot_lum.model.User> users = userRepository.findAllByEmailIn(emails);
        for (com.example.be_springboot_lum.model.User user : users) {
             user.getRoles().add(role);
        }
        userRepository.saveAll(users);
    }

    @Override
    @Transactional
    public void bulkRevokeRoleFromUsers(UUID roleId, com.example.be_springboot_lum.dto.request.BulkUserIdsRequest request) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        List<com.example.be_springboot_lum.model.User> users = userRepository.findAllById(request.getUserIds());
        for (com.example.be_springboot_lum.model.User user : users) {
            // Không gỡ nếu user chỉ có duy nhất role này
            if (user.getRoles().size() <= 1 && user.getRoles().contains(role)) {
                 continue; // skip
            }
            user.getRoles().remove(role);
        }
        userRepository.saveAll(users);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(UUID userId) {
        com.example.be_springboot_lum.model.User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return user.getRoles().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setUserRoles(UUID userId, com.example.be_springboot_lum.dto.request.BulkIdsRequest request) {
        com.example.be_springboot_lum.model.User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<Role> roles = roleRepository.findAllById(request.getIds());
        if (roles.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR); // Assuming VALIDATION_ERROR exists
        }
        user.setRoles(new HashSet<>(roles));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void addUserRole(UUID userId, UUID roleId) {
        com.example.be_springboot_lum.model.User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void removeUserRole(UUID userId, UUID roleId) {
        com.example.be_springboot_lum.model.User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        
        if (user.getRoles().size() <= 1 && user.getRoles().contains(role)) {
            throw new AppException(ErrorCode.UNAUTHORIZED); // Cannot remove last role
        }
        user.getRoles().remove(role);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getUserPermissions(UUID userId) {
        com.example.be_springboot_lum.model.User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Set<Permission> permissions = new HashSet<>();
        for (Role role : user.getRoles()) {
            permissions.addAll(role.getPermissions());
        }
        return permissions.stream().map(p -> PermissionResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .resource(p.getResource())
                .action(p.getAction())
                .createdAt(p.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkUserPermission(UUID userId, String resource, String action) {
        com.example.be_springboot_lum.model.User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        for (Role role : user.getRoles()) {
            for (Permission p : role.getPermissions()) {
                if (p.getResource().equalsIgnoreCase(resource) && p.getAction().equalsIgnoreCase(action)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private com.example.be_springboot_lum.dto.response.UserResponse toUserResponse(com.example.be_springboot_lum.model.User user) {
        Set<String> rNames = user.getRoles() == null ? Set.of() : user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return com.example.be_springboot_lum.dto.response.UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .coverUrl(user.getCoverUrl())
                .roles(rNames)
                .isSocialAccount(user.getPasswordHash() == null)
                .studentId(user.getStudentId())
                .universityId(user.getUniversityId())
                .campusId(user.getCampusId())
                .faculty(user.getFaculty())
                .bio(user.getBio())
                .location(user.getLocation())
                .reputationScore(user.getReputationScore())
                .totalSales(user.getTotalSales())
                .totalPurchases(user.getTotalPurchases())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .responseRate(user.getResponseRate())
                .isEmailVerified(user.getIsEmailVerified())
                .isPhoneVerified(user.getIsPhoneVerified())
                .isStudentVerified(user.getIsStudentVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .lastSeenAt(user.getLastSeenAt())
                .build();
    }

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
