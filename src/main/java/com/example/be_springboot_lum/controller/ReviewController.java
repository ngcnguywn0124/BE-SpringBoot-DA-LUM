package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.dto.request.CreateReviewRequest;
import com.example.be_springboot_lum.dto.response.ReviewResponse;
import com.example.be_springboot_lum.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @RequestHeader("User-Id") UUID currentUserId,
            @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(currentUserId, request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReviewResponse>> getUserReviews(
            @PathVariable UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByUser(userId, pageable));
    }
}
