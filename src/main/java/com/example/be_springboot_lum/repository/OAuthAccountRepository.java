package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    /**
     * Tìm tài khoản OAuth theo provider và provider_user_id.
     * Ví dụ: provider="google", providerUserId="1234567890"
     */
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
