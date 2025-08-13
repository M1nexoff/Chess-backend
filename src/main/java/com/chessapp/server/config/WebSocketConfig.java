package com.chessapp.server.config;

import com.chessapp.server.security.JwtHandshakeInterceptor;
import com.chessapp.server.websocket.ChessWebSocketHandler;
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
                .setAllowedOrigins("*");
    }
}

