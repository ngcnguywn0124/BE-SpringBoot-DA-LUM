package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.CreateProductCommentRequest;
import com.example.be_springboot_lum.dto.response.ProductCommentResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Product;
import com.example.be_springboot_lum.model.ProductComment;
import com.example.be_springboot_lum.model.ProductCommentLike;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.repository.ProductCommentLikeRepository;
import com.example.be_springboot_lum.repository.ProductCommentRepository;
import com.example.be_springboot_lum.repository.ProductRepository;
import com.example.be_springboot_lum.service.ProductCommentService;
import com.example.be_springboot_lum.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCommentServiceImpl implements ProductCommentService {

    private static final int MAX_COMMENT_LENGTH = 1000;

    private final ProductCommentRepository productCommentRepository;
    private final ProductCommentLikeRepository productCommentLikeRepository;
    private final ProductRepository productRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCommentResponse> getCommentsByProduct(UUID productId, Pageable pageable) {
        getProductOrThrow(productId);

        Page<ProductComment> page = productCommentRepository
                .findByProduct_ProductIdAndParentCommentIsNullOrderByCreatedAtDesc(productId, pageable);

        List<UUID> parentIds = page.getContent().stream()
                .map(ProductComment::getCommentId)
                .toList();

        List<ProductComment> replies = parentIds.isEmpty()
                ? List.of()
                : productCommentRepository.findByParentComment_CommentIdInOrderByCreatedAtAsc(parentIds);

        List<UUID> allCommentIds = new ArrayList<>(parentIds);
        replies.stream()
                .map(ProductComment::getCommentId)
                .forEach(allCommentIds::add);

        UUID currentUserId = getOptionalCurrentUserId();
        Set<UUID> likedCommentIds = getLikedCommentIds(allCommentIds, currentUserId);

        Map<UUID, List<ProductCommentResponse>> repliesByParentId = new LinkedHashMap<>();
        for (ProductComment reply : replies) {
            ProductComment parent = reply.getParentComment();
            if (parent == null) {
                continue;
            }

            repliesByParentId
                    .computeIfAbsent(parent.getCommentId(), ignored -> new ArrayList<>())
                    .add(mapToResponse(reply, likedCommentIds, List.of()));
        }

        List<ProductCommentResponse> content = page.getContent().stream()
                .map(comment -> mapToResponse(
                        comment,
                        likedCommentIds,
                        repliesByParentId.getOrDefault(comment.getCommentId(), List.of())
                ))
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    @Transactional
    public ProductCommentResponse createComment(UUID productId, CreateProductCommentRequest request) {
        Product product = getProductOrThrow(productId);
        User currentUser = securityUtils.getCurrentUser();
        String content = validateContent(request.getContent());

        ProductComment comment = ProductComment.builder()
                .product(product)
                .user(currentUser)
                .content(content)
                .parentComment(null)
                .likeCount(0)
                .build();

        return mapToResponse(productCommentRepository.save(comment), Set.of(), List.of());
    }

    @Override
    @Transactional
    public ProductCommentResponse replyToComment(UUID productId, UUID commentId, CreateProductCommentRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        ProductComment targetComment = getCommentOrThrow(productId, commentId);
        ProductComment parentComment = targetComment.getParentComment() != null
                ? targetComment.getParentComment()
                : targetComment;

        ProductComment comment = ProductComment.builder()
                .product(targetComment.getProduct())
                .user(currentUser)
                .parentComment(parentComment)
                .content(validateContent(request.getContent()))
                .likeCount(0)
                .build();

        return mapToResponse(productCommentRepository.save(comment), Set.of(), List.of());
    }

    @Override
    @Transactional
    public ProductCommentResponse toggleLike(UUID productId, UUID commentId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = securityUtils.getCurrentUser();
        ProductComment comment = getCommentOrThrow(productId, commentId);

        Optional<ProductCommentLike> existingLike = productCommentLikeRepository
                .findByComment_CommentIdAndUser_UserId(comment.getCommentId(), currentUserId);

        if (existingLike.isPresent()) {
            productCommentLikeRepository.delete(existingLike.get());
        } else {
            productCommentLikeRepository.save(ProductCommentLike.builder()
                    .comment(comment)
                    .user(currentUser)
                    .build());
        }

        int likeCount = (int) productCommentLikeRepository.countByComment_CommentId(comment.getCommentId());
        comment.setLikeCount(likeCount);
        ProductComment savedComment = productCommentRepository.save(comment);

        return mapToResponse(
                savedComment,
                existingLike.isPresent() ? Set.of() : Set.of(savedComment.getCommentId()),
                List.of()
        );
    }

    private Product getProductOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private ProductComment getCommentOrThrow(UUID productId, UUID commentId) {
        return productCommentRepository.findByCommentIdAndProduct_ProductId(commentId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_COMMENT_NOT_FOUND));
    }

    private String validateContent(String content) {
        String normalized = content != null ? content.trim() : "";
        if (normalized.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Nội dung bình luận không được để trống");
        }
        if (normalized.length() > MAX_COMMENT_LENGTH) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "Bình luận không được vượt quá " + MAX_COMMENT_LENGTH + " ký tự"
            );
        }
        return normalized;
    }

    private UUID getOptionalCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String principal = authentication.getName();
        if (principal == null || principal.isBlank() || "anonymousUser".equalsIgnoreCase(principal)) {
            return null;
        }

        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Set<UUID> getLikedCommentIds(Collection<UUID> commentIds, UUID currentUserId) {
        if (currentUserId == null || commentIds.isEmpty()) {
            return Set.of();
        }

        return productCommentLikeRepository
                .findByComment_CommentIdInAndUser_UserId(commentIds, currentUserId)
                .stream()
                .map(like -> like.getComment().getCommentId())
                .collect(Collectors.toSet());
    }

    private ProductCommentResponse mapToResponse(
            ProductComment comment,
            Set<UUID> likedCommentIds,
            List<ProductCommentResponse> replies
    ) {
        return ProductCommentResponse.builder()
                .commentId(comment.getCommentId())
                .productId(comment.getProduct().getProductId())
                .userId(comment.getUser().getUserId())
                .userName(comment.getUser().getFullName())
                .userAvatarUrl(comment.getUser().getAvatarUrl())
                .content(comment.getContent())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null)
                .likeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0)
                .likedByCurrentUser(likedCommentIds.contains(comment.getCommentId()))
                .replies(replies)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
