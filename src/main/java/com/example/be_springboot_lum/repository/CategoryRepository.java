package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /** Tất cả danh mục gốc (không có cha) */
    List<Category> findByParentIsNullOrderByDisplayOrderAscCategoryNameAsc();

    /** Danh mục con trực tiếp của một cha */
    List<Category> findByParent_CategoryIdOrderByDisplayOrderAscCategoryNameAsc(UUID parentId);

    /** Tìm kiếm theo tên */
    List<Category> findByCategoryNameContainingIgnoreCaseOrderByCategoryNameAsc(String keyword);

    Optional<Category> findBySlug(String slug);

    boolean existsByCategoryNameAndParent_CategoryId(String categoryName, UUID parentId);

    boolean existsByCategoryNameAndParentIsNull(String categoryName);

    /** Kiểm tra tên trùng ở cùng cấp – bỏ qua chính entity đang cập nhật */
    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c
            WHERE c.categoryName = :name
              AND ((:parentId IS NULL AND c.parent IS NULL)
                   OR c.parent.categoryId = :parentId)
              AND c.categoryId <> :excludeId
            """)
    boolean existsByNameAtSameLevelExcluding(@Param("name") String name,
                                             @Param("parentId") UUID parentId,
                                             @Param("excludeId") UUID excludeId);

    /** Đếm số sản phẩm đang dùng danh mục (để chặn xóa nếu có sản phẩm) */
    @Query(value = "SELECT COUNT(*) FROM products WHERE category_id = :categoryId", nativeQuery = true)
    long countProductsByCategory(@Param("categoryId") UUID categoryId);
}
