package com.chessapp.server.infrastructure.config;

import com.chessapp.server.infrastructure.security.JwtHandshakeInterceptor;
import com.chessapp.server.presentation.websocket.ChessWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ChessWebSocketHandler chessWebSocketHandler;

    @Autowired
    private JwtHandshakeInterceptor jwtInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chessWebSocketHandler, "/ws/chess")
                .addInterceptors(jwtInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
