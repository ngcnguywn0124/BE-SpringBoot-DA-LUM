package com.example.be_springboot_lum.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.be_springboot_lum.dto.request.CreateProductCommentRequest;
import com.example.be_springboot_lum.dto.response.ProductCommentResponse;

public interface ProductCommentService {
    Page<ProductCommentResponse> getCommentsByProduct(UUID productId, Pageable pageable);
    ProductCommentResponse createComment(UUID productId, CreateProductCommentRequest request);
    ProductCommentResponse replyToComment(UUID productId, UUID commentId, CreateProductCommentRequest request);
    ProductCommentResponse toggleLike(UUID productId, UUID commentId);
}
