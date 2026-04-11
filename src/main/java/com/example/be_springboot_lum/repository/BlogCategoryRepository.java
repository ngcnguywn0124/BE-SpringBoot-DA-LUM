package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.BlogCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogCategoryRepository extends JpaRepository<BlogCategory, UUID> {
    
    Optional<BlogCategory> findByName(String name);
    
    Optional<BlogCategory> findBySlug(String slug);
    
    @Query("SELECT bc FROM BlogCategory bc WHERE " +
           "(:keyword = '' OR LOWER(CAST(bc.name as string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(CAST(bc.slug as string)) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:isActive IS NULL OR bc.isActive = :isActive) " +
           "ORDER BY bc.name ASC")
    List<BlogCategory> searchCategories(@Param("keyword") String keyword, @Param("isActive") Boolean isActive);
}
