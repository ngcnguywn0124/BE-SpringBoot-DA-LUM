package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.dto.request.CreateConversationRequest;
import com.example.be_springboot_lum.dto.request.SendMessageRequest;
import com.example.be_springboot_lum.dto.response.ConversationResponse;
import com.example.be_springboot_lum.dto.response.MessageResponse;
import com.example.be_springboot_lum.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// For demonstration, assume a way to get the current authenticated user's ID
// In a real app with Spring Security, you'd use @AuthenticationPrincipal 
// or extract from SecurityContext. 
// For this controller, we'll accept it as a @RequestHeader("User-Id") for simplicity,
// but it should be replaced with actual auth logic.

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationResponse>> getConversations(
            @RequestHeader("User-Id") UUID currentUserId,
            Pageable pageable) {
        return ResponseEntity.ok(chatService.getUserConversations(currentUserId, pageable));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> createOrGetConversation(
            @RequestHeader("User-Id") UUID currentUserId,
            @RequestBody CreateConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createOrGetConversation(currentUserId, request));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @RequestHeader("User-Id") UUID currentUserId,
            @PathVariable UUID conversationId,
            Pageable pageable) {
        return ResponseEntity.ok(chatService.getMessages(currentUserId, conversationId, pageable));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestHeader("User-Id") UUID currentUserId,
            @PathVariable UUID conversationId,
            @RequestBody SendMessageRequest request) {
        // Ensure the path variable matches the payload if needed
        request.setConversationId(conversationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(currentUserId, request));
    }

    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("User-Id") UUID currentUserId,
            @PathVariable UUID conversationId) {
        chatService.markConversationAsRead(currentUserId, conversationId);
        return ResponseEntity.noContent().build();
    }
}
