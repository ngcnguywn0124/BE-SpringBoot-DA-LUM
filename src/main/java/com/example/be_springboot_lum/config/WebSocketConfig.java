package com.example.be_springboot_lum.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kích hoạt broker đơn giản để gửi tin nhắn từ server tới client
        // /topic: cho các tin nhắn công khai (như thông báo trạng thái)
        // /queue: cho các tin nhắn riêng tư (như chat 1-1)
        config.enableSimpleBroker("/topic", "/queue");
        
        // Tiền tố cho các tin nhắn gửi từ client tới server (ví dụ: gửi chat)
        config.setApplicationDestinationPrefixes("/app");
        
        // Tiền tố cho tin nhắn riêng tư dùng User destination
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Điểm kết nối WebSocket chính
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Nên thay bằng domain cụ thể của frontend khi deploy
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompUserInterceptor());
    }
}
