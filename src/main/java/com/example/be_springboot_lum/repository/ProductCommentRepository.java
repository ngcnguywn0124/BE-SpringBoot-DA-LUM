package com.example.be_springboot_lum.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.be_springboot_lum.model.ProductComment;

public interface ProductCommentRepository extends JpaRepository<ProductComment, UUID> {
    Page<ProductComment> findByProduct_ProductIdAndParentCommentIsNullOrderByCreatedAtDesc(UUID productId, Pageable pageable);
    List<ProductComment> findByParentComment_CommentIdInOrderByCreatedAtAsc(List<UUID> parentIds);
    Optional<ProductComment> findByCommentIdAndProduct_ProductId(UUID commentId, UUID productId);
}
