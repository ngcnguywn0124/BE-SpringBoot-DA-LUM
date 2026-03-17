package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.UserVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVerificationRepository extends JpaRepository<UserVerification, UUID> {

    List<UserVerification> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<UserVerification> findTopByUserUserIdAndVerificationTypeOrderByCreatedAtDesc(
            UUID userId,
            String verificationType
    );

    Optional<UserVerification> findTopByUserUserIdAndVerificationTypeAndVerificationCodeAndIsVerifiedFalseOrderByCreatedAtDesc(
            UUID userId,
            String verificationType,
            String verificationCode
    );

    Optional<UserVerification> findTopByUserUserIdAndVerificationTypeAndIsVerifiedFalseOrderByCreatedAtDesc(
            UUID userId,
            String verificationType
    );

    List<UserVerification> findByVerificationTypeAndIsVerifiedFalseAndVerifiedAtIsNullOrderByCreatedAtDesc(
            String verificationType
    );
}
