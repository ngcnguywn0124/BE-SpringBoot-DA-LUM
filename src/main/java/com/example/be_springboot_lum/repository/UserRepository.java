package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    java.util.List<User> findAllByEmailIn(java.util.List<String> emails);

    /**
     * Tìm user theo email HOẶC số điện thoại (dùng cho login)
     */
    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);

    /**
     * Tìm kiếm users theo Role ID
     */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.id = :roleId " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<User> findUsersByRoleIdAndSearch(
            @org.springframework.data.repository.query.Param("roleId") UUID roleId,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable);
}
