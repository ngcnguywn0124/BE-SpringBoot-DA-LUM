package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    /** Kiểm tra slug đã tồn tại chưa (để generate unique slug) */
    boolean existsBySlug(String slug);

    /** Tìm theo slug (public detail) */
    Optional<Product> findBySlugAndStatusNot(String slug, String status);

    /** Danh sách tin của một người bán, phân trang */
    Page<Product> findBySeller_UserIdAndStatusNotOrderByCreatedAtDesc(
            UUID sellerId, String excludeStatus, Pageable pageable);

    /** Danh sách tin của người bán theo trạng thái cụ thể */
    Page<Product> findBySeller_UserIdAndStatusOrderByCreatedAtDesc(
            UUID sellerId, String status, Pageable pageable);

    /** Lọc sản phẩm available, phân trang – dùng JPQL để hỗ trợ lọc nhiều tiêu chí */
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = 'available'
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
              AND (:universityId IS NULL OR p.university.universityId = :universityId)
              AND (:campusId IS NULL OR p.campus.campusId = :campusId)
              AND (:listingType IS NULL OR p.listingType = :listingType)
              AND (:condition IS NULL OR p.condition = :condition)
              AND (:isFree IS NULL OR p.isFree = :isFree)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            ORDER BY p.createdAt DESC
            """)
    Page<Product> findAvailableWithFilters(
            @Param("categoryId")   UUID categoryId,
            @Param("universityId") UUID universityId,
            @Param("campusId")     UUID campusId,
            @Param("listingType")  String listingType,
            @Param("condition")    String condition,
            @Param("isFree")       Boolean isFree,
            @Param("minPrice")     java.math.BigDecimal minPrice,
            @Param("maxPrice")     java.math.BigDecimal maxPrice,
            Pageable pageable);

    /** Full-text search đơn giản (ILIKE) trên title + description */
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = 'available'
              AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /** Trending: sản phẩm trong 30 ngày gần nhất, sắp xếp theo view + favorite */
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = 'available'
              AND p.createdAt >= :since
            ORDER BY (p.viewCount * 0.3 + p.favoriteCount * 0.7) DESC
            """)
    Page<Product> findTrending(@Param("since") java.time.OffsetDateTime since, Pageable pageable);

    /** Tăng view_count +1 (dùng @Modifying thay vì load entity) */
    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.productId = :id")
    void incrementViewCount(@Param("id") UUID id);

    /** Tìm danh sách tin đăng theo trạng thái và thời gian hết hạn trước một mốc */
    java.util.List<com.example.be_springboot_lum.model.Product> findAllByStatusAndExpiresAtBefore(String status, java.time.OffsetDateTime dateTime);

    /** Tự động cập nhật trạng thái hết hạn cho các tin available đã quá hạn */
    @Modifying
    @Query("UPDATE Product p SET p.status = 'expired' WHERE p.status = 'available' AND p.expiresAt < CURRENT_TIMESTAMP")
    int updateExpiredProducts();

    /** Admin – lọc tất cả trạng thái, hỗ trợ lọc theo status */
    @Query("""
            SELECT p FROM Product p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<Product> findAllForAdmin(
            @Param("status")  String status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
