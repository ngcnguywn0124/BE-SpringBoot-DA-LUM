package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.CreateConversationRequest;
import com.example.be_springboot_lum.dto.request.SendMessageRequest;
import com.example.be_springboot_lum.dto.response.ConversationResponse;
import com.example.be_springboot_lum.dto.response.MessageResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Conversation;
import com.example.be_springboot_lum.model.ConversationParticipant;
import com.example.be_springboot_lum.model.Message;
import com.example.be_springboot_lum.model.Product;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.repository.ConversationParticipantRepository;
import com.example.be_springboot_lum.repository.ConversationRepository;
import com.example.be_springboot_lum.repository.MessageRepository;
import com.example.be_springboot_lum.repository.ProductRepository;
import com.example.be_springboot_lum.repository.TransactionRepository;
import com.example.be_springboot_lum.repository.UserRepository;
import com.example.be_springboot_lum.service.ChatService;
import com.example.be_springboot_lum.service.PresenceService;
import com.example.be_springboot_lum.dto.response.PresenceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final TransactionRepository transactionRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;

    @Override
    @Transactional
    public ConversationResponse createOrGetConversation(UUID currentUserId, CreateConversationRequest request) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("Current user id is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("CreateConversationRequest is required");
        }
        if (request.getTargetUserId() == null) {
            throw new IllegalArgumentException("targetUserId is required");
        }

        if (currentUserId.equals(request.getTargetUserId())) {
            throw new RuntimeException("Cannot create conversation with yourself");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + currentUserId));
        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new RuntimeException("Target user not found: " + request.getTargetUserId()));

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));
        }

        ConversationParticipant existingParticipant = findExistingConversationParticipant(
                currentUserId,
                request.getTargetUserId(),
                request.getProductId());
        if (existingParticipant != null) {
            activateAllParticipants(existingParticipant.getConversation());
            return mapToConversationResponse(existingParticipant);
        }

        try {
            Conversation conversation = new Conversation();
            conversation.setProduct(product);
            conversation.setUpdatedAt(OffsetDateTime.now());
            Conversation savedConversation = conversationRepository.save(conversation);

            ConversationParticipant participant1 = ConversationParticipant.builder()
                    .conversation(savedConversation)
                    .user(currentUser)
                    .joinedAt(OffsetDateTime.now())
                    .isActive(true)
                    .isPinned(false)
                    .build();

            ConversationParticipant participant2 = ConversationParticipant.builder()
                    .conversation(savedConversation)
                    .user(targetUser)
                    .joinedAt(OffsetDateTime.now())
                    .isActive(true)
                    .isPinned(false)
                    .build();

            participantRepository.save(participant1);
            participantRepository.save(participant2);

            return mapToConversationResponse(participant1);
        } catch (DataIntegrityViolationException ex) {
            ConversationParticipant participantAfterConflict = findExistingConversationParticipant(
                    currentUserId,
                    request.getTargetUserId(),
                    request.getProductId());
            if (participantAfterConflict != null) {
                activateAllParticipants(participantAfterConflict.getConversation());
                return mapToConversationResponse(participantAfterConflict);
            }
            throw ex;
        }
    }

    private ConversationParticipant findExistingConversationParticipant(UUID currentUserId, UUID targetUserId, UUID productId) {
        List<ConversationParticipant> existingParticipants = participantRepository.findByUserUserId(currentUserId);
        for (ConversationParticipant myParticipant : existingParticipants) {
            Conversation conversation = myParticipant.getConversation();
            List<ConversationParticipant> participants = participantRepository.findByConversationConversationId(
                    conversation.getConversationId());

            boolean hasTarget = participants.stream()
                    .anyMatch(p -> p.getUser().getUserId().equals(targetUserId));
            boolean onlyTwoParticipants = participants.size() == 2;
            boolean productMatches = isProductMatch(conversation, productId);

            if (hasTarget && onlyTwoParticipants && productMatches) {
                return myParticipant;
            }
        }
        return null;
    }

    private boolean isProductMatch(Conversation conversation, UUID productId) {
        if (productId == null) {
            return conversation.getProduct() == null;
        }
        return conversation.getProduct() != null
                && productId.equals(conversation.getProduct().getProductId());
    }

    private void activateAllParticipants(Conversation conversation) {
        List<ConversationParticipant> participants = participantRepository.findByConversationConversationId(
                conversation.getConversationId());
        for (ConversationParticipant participant : participants) {
            if (!Boolean.TRUE.equals(participant.getIsActive())) {
                participant.setIsActive(true);
                participantRepository.save(participant);
            }
        }
        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getUserConversations(UUID currentUserId, Pageable pageable) {
        Page<ConversationParticipant> participants = participantRepository.findActiveConversationsByUserId(currentUserId, pageable);
        return participants.map(this::mapToConversationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(UUID currentUserId, UUID conversationId, Pageable pageable) {
        // Verify user is part of the conversation
        participantRepository.findByConversationConversationIdAndUserUserId(conversationId, currentUserId)
                .orElseThrow(() -> new RuntimeException("User is not part of this conversation"));

        // Get other participant's lastReadAt to calculate 'seen' status
        List<ConversationParticipant> others = participantRepository.findOtherParticipants(conversationId, currentUserId);
        OffsetDateTime otherLastReadAt = others.stream()
                .map(ConversationParticipant::getLastReadAt)
                .filter(java.util.Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);

        Page<Message> messages = messageRepository.findByConversationConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        return messages.map(m -> mapToMessageResponse(m, otherLastReadAt));
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID currentUserId, SendMessageRequest request) {
        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        // Ensure sender is a participant
        participantRepository.findByConversationConversationIdAndUserUserId(request.getConversationId(), currentUserId)
                .orElseThrow(() -> new RuntimeException("User is not part of this conversation"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : "text");
        message.setContent(request.getContent());
        message.setAttachmentUrl(request.getAttachmentUrl());
        message.setOfferAmount(request.getOfferAmount());

        message = messageRepository.save(message);
        // Flush to ensure createdAt and messageId are populated before mapping
        messageRepository.flush();

        // Update conversation updated_at implicitly
        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        // For a new message, it can't be 'seen' yet by others
        MessageResponse response = mapToMessageResponse(message, null);

        // Gửi realtime qua WebSocket tới tất cả người tham gia
        List<ConversationParticipant> participants = participantRepository.findByConversationConversationId(request.getConversationId());
        for (ConversationParticipant p : participants) {
            // 1) User-destination (/user/queue/...) – chuẩn STOMP, nhưng có thể fail khi đi qua proxy/ngrok
            messagingTemplate.convertAndSendToUser(
                    p.getUser().getUserId().toString(),
                    "/queue/messages",
                    response
            );

            // 2) Fallback broadcast theo topic user-specific – ổn định hơn trong môi trường proxy
            messagingTemplate.convertAndSend(
                    "/topic/user-" + p.getUser().getUserId(),
                    response
            );
        }

        return response;
    }

    @Override
    @Transactional
    public void markConversationAsRead(UUID currentUserId, UUID conversationId) {
        int updatedRows = participantRepository.updateLastReadAt(
                conversationId,
                currentUserId,
                OffsetDateTime.now());

        if (updatedRows == 0) {
            throw new AppException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }

        // Broadcast "seen" status to other participants
        List<ConversationParticipant> others = participantRepository.findOtherParticipants(conversationId, currentUserId);
        for (ConversationParticipant other : others) {
            // Send a status update indicating this conversation is seen
            // The FE expects {conversationId, status: 'seen'} or similar
            // Since multiple messages might be seen, we can send a wrap event
            java.util.Map<String, Object> statusUpdate = new java.util.HashMap<>();
            statusUpdate.put("conversationId", conversationId);
            statusUpdate.put("userId", currentUserId);
            statusUpdate.put("status", "seen");
            statusUpdate.put("timestamp", OffsetDateTime.now());

            messagingTemplate.convertAndSendToUser(
                    other.getUser().getUserId().toString(),
                    "/queue/status",
                    statusUpdate
            );
            
            // Fallback for proxy/ngrok
            messagingTemplate.convertAndSend(
                    "/topic/user-" + other.getUser().getUserId() + "/status",
                    (Object) statusUpdate
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID currentUserId) {
        return messageRepository.countUnreadMessagesForUser(currentUserId);
    }

    // Helpers
    private ConversationResponse mapToConversationResponse(ConversationParticipant myParticipant) {
        Conversation conversation = myParticipant.getConversation();
        
        List<ConversationParticipant> others = participantRepository.findOtherParticipants(
                conversation.getConversationId(), myParticipant.getUser().getUserId());
        
        User otherUser = null;
        if (!others.isEmpty()) {
            ConversationParticipant otherPart = others.get(0);
            otherUser = otherPart.getUser();
        }

        ConversationResponse response = ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .isPinned(myParticipant.getIsPinned())
                .joinedAt(myParticipant.getJoinedAt())
                .build();
        
        if (otherUser != null) {
            response.setOtherUserId(otherUser.getUserId());
            response.setOtherUserName(otherUser.getFullName());
            response.setOtherUserAvatarUrl(otherUser.getAvatarUrl());
            
            // Check real-time presence instead of hardcoded false
            PresenceEvent presence = presenceService.getPresence(otherUser.getUserId()).orElse(null);
            response.setOtherUserOnline(presence != null && presence.isOnline());
            response.setOtherUserLastSeenAt(otherUser.getLastSeenAt());
        }

        if (conversation.getProduct() != null) {
            Product product = conversation.getProduct();
            response.setProductId(product.getProductId());
            response.setProductTitle(product.getTitle());
            response.setProductSlug(product.getSlug());
            response.setProductPrice(product.getPrice());
            response.setSellerId(product.getSeller().getUserId());
            response.setSellerPhone(product.getSeller().getPhoneNumber());
            // response.setProductImageUrl(product.getImageUrl());
        }

        if (conversation.getTransactionId() != null) {
            response.setTransactionId(conversation.getTransactionId());
            transactionRepository.findById(conversation.getTransactionId())
                    .ifPresent(t -> response.setTransactionStatus(t.getStatus()));
        }

        // Fetch last message
        Page<Message> lastMessages = messageRepository.findByConversationConversationIdOrderByCreatedAtDesc(
                conversation.getConversationId(), Pageable.ofSize(1));
        
        if (lastMessages.hasContent()) {
            Message lastMessage = lastMessages.getContent().get(0);
            response.setLastMessagePreview(lastMessage.getContent());
            response.setLastMessageAt(lastMessage.getCreatedAt());
            response.setIsUnread(myParticipant.getLastReadAt() == null || lastMessage.getCreatedAt().isAfter(myParticipant.getLastReadAt()));
        } else {
            response.setIsUnread(false);
        }
        
        return response;
    }

    private MessageResponse mapToMessageResponse(Message message, OffsetDateTime otherLastReadAt) {
        String content = message.getContent();
        String attachmentUrl = message.getAttachmentUrl();
        
        // Handle "images" type conversion to match frontend logic
        if ("images".equals(message.getMessageType()) && (content == null || content.isEmpty()) && attachmentUrl != null) {
            content = attachmentUrl;
        }

        String deliveryStatus = message.getDeliveryStatus();
        // Dynamic "seen" calculation: Nếu tin nhắn được tạo TRƯỚC HOẶC BẰNG lúc người kia đọc lần cuối cùng
        if (otherLastReadAt != null && !message.getCreatedAt().isAfter(otherLastReadAt)) {
            deliveryStatus = "seen";
        }

        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversation().getConversationId())
                .senderId(message.getSender().getUserId())
                .senderName(message.getSender().getFullName())
                .senderAvatarUrl(message.getSender().getAvatarUrl())
                .messageType(message.getMessageType())
                .content(content)
                .attachmentUrl(attachmentUrl)
                .offerAmount(message.getOfferAmount())
                .transactionEventType(message.getTransactionEventType())
                .deliveryStatus(deliveryStatus)
                .isEdited(message.getIsEdited())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
