package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.CreateReviewRequest;
import com.example.be_springboot_lum.dto.response.ReviewResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Review;
import com.example.be_springboot_lum.model.Transaction;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.repository.ReviewRepository;
import com.example.be_springboot_lum.repository.TransactionRepository;
import com.example.be_springboot_lum.repository.UserRepository;
import com.example.be_springboot_lum.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(UUID reviewerId, CreateReviewRequest request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        // Chỉ người mua mới được quyền review người bán trong giao dịch mua bán
        if (!transaction.getBuyer().getUserId().equals(reviewerId)) {
            throw new AppException(ErrorCode.TRANSACTION_FORBIDDEN);
        }

        if (!"completed".equals(transaction.getStatus())) {
            throw new AppException(ErrorCode.TRANSACTION_INVALID_STATUS);
        }

        if (reviewRepository.existsByTransactionTransactionIdAndReviewerUserId(transaction.getTransactionId(), reviewerId)) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        User reviewer = transaction.getBuyer();
        User reviewee = transaction.getSeller();

        Review review = Review.builder()
                .transaction(transaction)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(request.getRating())
                .comment(request.getComment())
                .reviewType("as_buyer")
                .isVerifiedPurchase(true)
                .isVisible(true)
                .build();

        review = reviewRepository.save(review);
        reviewRepository.flush();

        // Cập nhật điểm uy tín cho người bán
        Double currentAvg = reviewRepository.getAverageRatingByRevieweeId(reviewee.getUserId());
        reviewee.setReputationScore(java.math.BigDecimal.valueOf(currentAvg != null ? currentAvg : 0.0));
        userRepository.save(reviewee);

        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByUser(UUID revieweeId, Pageable pageable) {
        return reviewRepository.findByRevieweeUserIdOrderByCreatedAtDesc(revieweeId, pageable)
                .map(this::mapToResponse);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .transactionId(review.getTransaction().getTransactionId())
                .productName(review.getTransaction().getProduct().getTitle())
                .reviewerId(review.getReviewer().getUserId())
                .reviewerName(review.getReviewer().getFullName())
                .reviewerAvatarUrl(review.getReviewer().getAvatarUrl())
                .revieweeId(review.getReviewee().getUserId())
                .rating(review.getRating())
                .comment(review.getComment())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
