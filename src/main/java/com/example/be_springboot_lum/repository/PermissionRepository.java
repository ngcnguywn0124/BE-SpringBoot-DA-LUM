package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByName(String name);

    List<Permission> findByNameIn(List<String> names);

    boolean existsByName(String name);
}
