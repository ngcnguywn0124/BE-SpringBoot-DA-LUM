package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** Lấy tất cả giao dịch mà user là buyer HOẶC seller, sắp xếp mới nhất trước */
    @Query("SELECT t FROM Transaction t WHERE t.buyer.userId = :userId OR t.seller.userId = :userId ORDER BY t.updatedAt DESC")
    Page<Transaction> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query(
        value = """
            SELECT t FROM Transaction t
            WHERE (
                (:role = 'buyer' AND t.buyer.userId = :userId)
                OR (:role = 'seller' AND t.seller.userId = :userId)
                OR (:role = 'all' AND (t.buyer.userId = :userId OR t.seller.userId = :userId))
            )
            AND (:status IS NULL OR t.status = :status)
            AND (:fromDate IS NULL OR t.createdAt >= :fromDate)
            AND (:toDate IS NULL OR t.createdAt <= :toDate)
            ORDER BY t.updatedAt DESC
            """,
        countQuery = """
            SELECT COUNT(t) FROM Transaction t
            WHERE (
                (:role = 'buyer' AND t.buyer.userId = :userId)
                OR (:role = 'seller' AND t.seller.userId = :userId)
                OR (:role = 'all' AND (t.buyer.userId = :userId OR t.seller.userId = :userId))
            )
            AND (:status IS NULL OR t.status = :status)
            AND (:fromDate IS NULL OR t.createdAt >= :fromDate)
            AND (:toDate IS NULL OR t.createdAt <= :toDate)
            """
    )
    Page<Transaction> findByUserIdWithFilters(
            @Param("userId") UUID userId,
            @Param("role") String role,
            @Param("status") String status,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            Pageable pageable);

    /** Lấy giao dịch theo sản phẩm */
    List<Transaction> findByProductProductId(UUID productId);

    /** Kiểm tra giao dịch đang active (chưa hoàn thành / huỷ) cho sản phẩm */
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.product.productId = :productId
          AND t.status NOT IN ('completed', 'cancelled')
        """)
    List<Transaction> findActiveByProductId(@Param("productId") UUID productId);

    /** Lấy giao dịch theo buyer và sản phẩm (để tránh tạo duplicate) */
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.buyer.userId = :buyerId
          AND t.product.productId = :productId
          AND t.status NOT IN ('completed', 'cancelled')
        """)
    Optional<Transaction> findActiveByBuyerAndProduct(
            @Param("buyerId") UUID buyerId,
            @Param("productId") UUID productId);
}
