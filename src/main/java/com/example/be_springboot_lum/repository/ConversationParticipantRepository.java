package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.ConversationParticipant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {
    
    Optional<ConversationParticipant> findByConversationConversationIdAndUserUserId(UUID conversationId, UUID userId);

    @Query("SELECT cp FROM ConversationParticipant cp JOIN FETCH cp.conversation c WHERE cp.user.userId = :userId AND cp.isActive = true ORDER BY c.updatedAt DESC")
    Page<ConversationParticipant> findActiveConversationsByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId <> :userId")
    List<ConversationParticipant> findOtherParticipants(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    List<ConversationParticipant> findByConversationConversationId(UUID conversationId);

    List<ConversationParticipant> findByUserUserId(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ConversationParticipant cp SET cp.lastReadAt = :readAt WHERE cp.conversation.conversationId = :conversationId AND cp.user.userId = :userId")
    int updateLastReadAt(@Param("conversationId") UUID conversationId,
                         @Param("userId") UUID userId,
                         @Param("readAt") OffsetDateTime readAt);
}
