package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshToken(String refreshToken);

    void deleteByUser(User user);

    void deleteByRefreshToken(String refreshToken);
}
