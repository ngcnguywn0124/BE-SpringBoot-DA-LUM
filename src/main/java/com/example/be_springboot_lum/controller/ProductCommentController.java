package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.CreateProductCommentRequest;
import com.example.be_springboot_lum.dto.response.ProductCommentResponse;
import com.example.be_springboot_lum.service.ProductCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/products/{productId}/comments")
@RequiredArgsConstructor
public class ProductCommentController {

    private final ProductCommentService productCommentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductCommentResponse>>> getComments(
            @PathVariable UUID productId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                productCommentService.getCommentsByProduct(productId, pageable)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductCommentResponse>> createComment(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductCommentRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                productCommentService.createComment(productId, request)));
    }

    @PostMapping("/{commentId}/replies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductCommentResponse>> replyToComment(
            @PathVariable UUID productId,
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateProductCommentRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                productCommentService.replyToComment(productId, commentId, request)));
    }

    @PostMapping("/{commentId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductCommentResponse>> toggleLike(
            @PathVariable UUID productId,
            @PathVariable UUID commentId) {
        return ResponseEntity.ok(ApiResponse.success(
                productCommentService.toggleLike(productId, commentId)));
    }
}
