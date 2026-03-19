package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** Tìm conversation đã liên kết với một transaction cụ thể */
    Optional<Conversation> findFirstByTransactionId(UUID transactionId);

    /** Tìm conversation theo sản phẩm */
    List<Conversation> findByProductProductId(UUID productId);
}
