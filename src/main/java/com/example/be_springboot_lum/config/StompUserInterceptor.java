package com.example.be_springboot_lum.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.util.List;

/**
 * Gán Principal cho WebSocket session dựa trên STOMP CONNECT header.
 * Mục tiêu: enable /user/queue/** cho realtime (convertAndSendToUser).
 *
 * Lưu ý: Cách này ưu tiên tính ổn định khi deploy qua ngrok (cookie/JWT có thể không đi kèm handshake).
 * Nếu cần bảo mật, hãy thay bằng xác thực JWT trong handshake/inbound channel.
 */
public class StompUserInterceptor implements ChannelInterceptor {

    private static final List<String> USER_ID_HEADER_CANDIDATES = List.of("user-id", "User-Id", "userId", "UserId");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand()) && accessor.getUser() == null) {
            String userId = null;
            for (String key : USER_ID_HEADER_CANDIDATES) {
                List<String> values = accessor.getNativeHeader(key);
                if (values != null && !values.isEmpty() && values.get(0) != null && !values.get(0).isBlank()) {
                    userId = values.get(0).trim();
                    break;
                }
            }

            if (userId != null) {
                final String uid = userId;
                Principal principal = () -> uid;
                accessor.setUser(principal);
            }
        }

        return message;
    }
}

