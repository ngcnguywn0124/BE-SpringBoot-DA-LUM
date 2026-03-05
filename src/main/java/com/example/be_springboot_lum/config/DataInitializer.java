package com.example.be_springboot_lum.config;

import com.example.be_springboot_lum.model.Permission;
import com.example.be_springboot_lum.model.Role;
import com.example.be_springboot_lum.repository.PermissionRepository;
import com.example.be_springboot_lum.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Khởi tạo dữ liệu Roles và Permissions khi ứng dụng khởi động.
 * Tương đương với các câu INSERT trong db.txt phần ROLE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPermissions();
        seedRoles();
        log.info("DataInitializer: Roles và Permissions đã được khởi tạo thành công.");
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private void seedPermissions() {
        List<Object[]> definitions = List.of(
                new Object[]{"USER_READ",        "Read user information",             "USER",       "READ"},
                new Object[]{"USER_WRITE",       "Create and update user information","USER",       "WRITE"},
                new Object[]{"USER_DELETE",      "Delete user accounts",              "USER",       "DELETE"},
                new Object[]{"ROLE_READ",        "Read role information",             "ROLE",       "READ"},
                new Object[]{"ROLE_WRITE",       "Create and update roles",           "ROLE",       "WRITE"},
                new Object[]{"ROLE_DELETE",      "Delete roles",                      "ROLE",       "DELETE"},
                new Object[]{"PERMISSION_READ",  "Read permission information",       "PERMISSION", "READ"},
                new Object[]{"PERMISSION_WRITE", "Create and update permissions",     "PERMISSION", "WRITE"},
                new Object[]{"PERMISSION_DELETE","Delete permissions",                "PERMISSION", "DELETE"}
        );

        for (Object[] def : definitions) {
            String name = (String) def[0];
            if (!permissionRepository.existsByName(name)) {
                permissionRepository.save(
                        Permission.builder()
                                .name(name)
                                .description((String) def[1])
                                .resource((String) def[2])
                                .action((String) def[3])
                                .build()
                );
                log.debug("DataInitializer: Permission '{}' đã được tạo.", name);
            }
        }
    }

    // ── Roles ─────────────────────────────────────────────────────────────────

    private void seedRoles() {
        // Map tên permission → entity (đã được lưu)
        Map<String, Permission> permMap = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getName, Function.identity()));

        // Định nghĩa role và danh sách permissions tương ứng
        Map<String, Object[]> roleDefs = Map.of(
                Role.ROLE_USER,        new Object[]{"Standard user role with basic permissions",
                        List.of("USER_READ")},

                Role.ROLE_MODERATOR,   new Object[]{"Moderator role with content management permissions",
                        List.of("USER_READ", "USER_WRITE")},

                Role.ROLE_ADMIN,       new Object[]{"Administrator role with elevated permissions",
                        List.of("USER_READ", "USER_WRITE", "USER_DELETE",
                                "ROLE_READ", "ROLE_WRITE")},

                Role.ROLE_SUPER_ADMIN, new Object[]{"Super administrator with full system access",
                        permMap.keySet().stream().toList()} // Tất cả permissions
        );

        for (Map.Entry<String, Object[]> entry : roleDefs.entrySet()) {
            String roleName = entry.getKey();
            String description = (String) entry.getValue()[0];
            @SuppressWarnings("unchecked")
            List<String> permNames = (List<String>) entry.getValue()[1];

            Role role = roleRepository.findByName(roleName).orElse(null);
            if (role == null) {
                role = Role.builder()
                        .name(roleName)
                        .description(description)
                        .build();
            }

            // Gán lại permissions (idempotent)
            Set<Permission> perms = new HashSet<>();
            for (String permName : permNames) {
                Permission p = permMap.get(permName);
                if (p != null) perms.add(p);
            }
            role.setPermissions(perms);
            roleRepository.save(role);
            log.debug("DataInitializer: Role '{}' đã được khởi tạo với {} permissions.", roleName, perms.size());
        }
    }
}
