package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Tìm user theo email HOẶC số điện thoại (dùng cho login)
     */
    Optional<User> findByEmailOrPhoneNumber(String email, String phoneNumber);
}
