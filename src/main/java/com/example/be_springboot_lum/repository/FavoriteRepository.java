package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    boolean existsByUser_UserIdAndProduct_ProductId(UUID userId, UUID productId);

    Optional<Favorite> findByUser_UserIdAndProduct_ProductId(UUID userId, UUID productId);

    @Query("""
            SELECT f FROM Favorite f
            WHERE f.user.userId = :userId
            ORDER BY f.createdAt DESC
            """)
    Page<Favorite> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT f FROM Favorite f
            WHERE f.user.userId = :userId
              AND f.product.status = :status
            ORDER BY f.createdAt DESC
            """)
    Page<Favorite> findByUserIdAndProductStatus(
            @Param("userId") UUID userId,
            @Param("status") String status,
            Pageable pageable);

    @Query("""
            SELECT f FROM Favorite f
            WHERE f.user.userId = :userId
              AND f.product.status <> 'sold'
              AND f.product.status <> 'deleted'
            ORDER BY f.createdAt DESC
            """)
    Page<Favorite> findActiveFavoritesByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.user.userId = :userId AND f.product.status = 'available'")
    long countActiveByUserId(@Param("userId") UUID userId);
}
