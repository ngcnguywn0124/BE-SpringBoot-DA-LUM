package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByConversationConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m " +
           "JOIN ConversationParticipant cp ON cp.conversation.conversationId = m.conversation.conversationId " +
           "WHERE cp.user.userId = :userId AND m.sender.userId <> :userId " +
           "AND (cp.lastReadAt IS NULL OR m.createdAt > cp.lastReadAt)")
    long countUnreadMessagesForUser(@Param("userId") UUID userId);
}
