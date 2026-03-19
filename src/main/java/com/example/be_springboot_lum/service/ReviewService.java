package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.CreateReviewRequest;
import com.example.be_springboot_lum.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {
    ReviewResponse createReview(UUID reviewerId, CreateReviewRequest request);
    Page<ReviewResponse> getReviewsByUser(UUID revieweeId, Pageable pageable);
}
