package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.CreateConversationRequest;
import com.example.be_springboot_lum.dto.request.SendMessageRequest;
import com.example.be_springboot_lum.dto.response.ConversationResponse;
import com.example.be_springboot_lum.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChatService {
    
    ConversationResponse createOrGetConversation(UUID currentUserId, CreateConversationRequest request);
    
    Page<ConversationResponse> getUserConversations(UUID currentUserId, Pageable pageable);
    
    Page<MessageResponse> getMessages(UUID currentUserId, UUID conversationId, Pageable pageable);
    
    MessageResponse sendMessage(UUID currentUserId, SendMessageRequest request);
    
    void markConversationAsRead(UUID currentUserId, UUID conversationId);
}
