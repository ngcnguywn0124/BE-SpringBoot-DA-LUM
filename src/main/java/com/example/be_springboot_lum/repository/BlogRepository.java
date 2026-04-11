package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Blog;
import com.example.be_springboot_lum.model.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogRepository extends JpaRepository<Blog, UUID> {

    Optional<Blog> findBySlugAndStatus(String slug, BlogStatus status);

    @Query("SELECT b FROM Blog b " +
           "LEFT JOIN FETCH b.blogCategory " +
           "LEFT JOIN FETCH b.author " +
           "LEFT JOIN FETCH b.approvedBy " +
           "WHERE (:status IS NULL OR b.status = :status) " +
           "AND (:categoryId IS NULL OR b.blogCategory.blogCategoryId = :categoryId) " +
           "AND (:isFeatured IS NULL OR b.isFeatured = :isFeatured) " +
           "AND (:search = '' OR LOWER(CAST(b.title as string)) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "                  OR LOWER(CAST(b.excerpt as string)) LIKE LOWER(CONCAT('%', :search, '%'))) ")
    Page<Blog> searchBlogs(
            @Param("status") BlogStatus status,
            @Param("categoryId") UUID categoryId,
            @Param("isFeatured") Boolean isFeatured,
            @Param("search") String search,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Blog b SET b.viewCount = b.viewCount + 1 WHERE b.blogId = :id")
    void incrementViewCount(@Param("id") UUID id);
}
