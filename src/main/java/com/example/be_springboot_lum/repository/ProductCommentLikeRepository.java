package com.example.be_springboot_lum.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.be_springboot_lum.model.ProductCommentLike;

public interface ProductCommentLikeRepository extends JpaRepository<ProductCommentLike, UUID> {
    Optional<ProductCommentLike> findByComment_CommentIdAndUser_UserId(UUID commentId, UUID userId);
    List<ProductCommentLike> findByComment_CommentIdInAndUser_UserId(Collection<UUID> commentIds, UUID userId);
    long countByComment_CommentId(UUID commentId);
}
