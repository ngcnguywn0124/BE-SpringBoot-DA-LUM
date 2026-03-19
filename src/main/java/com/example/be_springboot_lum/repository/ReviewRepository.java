package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    
    boolean existsByTransactionTransactionIdAndReviewerUserId(UUID transactionId, UUID reviewerId);

    Page<Review> findByRevieweeUserIdOrderByCreatedAtDesc(UUID revieweeId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.userId = :revieweeId")
    Double getAverageRatingByRevieweeId(UUID revieweeId);
}
