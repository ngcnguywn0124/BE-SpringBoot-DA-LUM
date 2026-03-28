package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Blog;
import com.example.be_springboot_lum.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlogRepository extends JpaRepository<Blog, UUID> {

    Optional<Blog> findBySlug(String slug);

    Page<Blog> findAllByStatus(String status, Pageable pageable);

    Page<Blog> findAllByAuthor(User author, Pageable pageable);

    Page<Blog> findAllByStatusAndCategory(String status, String category, Pageable pageable);

    Page<Blog> findAllByStatusAndIsFeatured(String status, Boolean isFeatured, Pageable pageable);
 
    Page<Blog> findAllByStatusAndTitleContainingIgnoreCaseOrExcerptContainingIgnoreCase(
            String status, String titleQuery, String excerptQuery, Pageable pageable);
}
