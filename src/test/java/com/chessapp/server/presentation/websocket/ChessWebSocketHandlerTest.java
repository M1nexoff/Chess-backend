package com.chessapp.server.presentation.websocket;

import com.chessapp.server.application.service.GameService;
import com.chessapp.server.application.service.MatchmakingService;
import com.chessapp.server.application.service.UserService;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

import static org.mockito.Mockito.*;

class ChessWebSocketHandlerTest {

    @Mock
    private GameService gameService;

    @Mock
    private UserService userService;

    @Mock
    private MatchmakingService matchmakingService;

    @Mock
    private com.chessapp.server.application.service.ChallengeService challengeService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private ChessWebSocketHandler chessWebSocketHandler;

    @Mock
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(session.getId()).thenReturn("session123");
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    void testHandleTextMessage_InvalidJson() throws Exception {
        TextMessage message = new TextMessage("invalid json");
        chessWebSocketHandler.handleMessage(session, message);

        // Since it throws an exception internally caught, it shouldn't crash
        // and probably respond with an error message
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void testAfterConnectionEstablished_Success() throws Exception {
        java.net.URI uri = new java.net.URI("ws://localhost/chess?token=my-jwt");
        when(session.getUri()).thenReturn(uri);
        when(session.getAttributes()).thenReturn(new java.util.HashMap<>());
        when(jwtUtils.validateJwtToken("my-jwt")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("my-jwt")).thenReturn("testuser");

        User user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        when(userService.findByLogin("testuser")).thenReturn(Optional.of(user));
        when(challengeService.findPendingChallengesForUser(user)).thenReturn(new java.util.ArrayList<>());

        chessWebSocketHandler.afterConnectionEstablished(session);

        verify(session, atLeastOnce()).sendMessage(any(TextMessage.class));
        verify(userService, times(1)).setUserOnline(user, true);
    }

    @Test
    void testAfterConnectionClosed() throws Exception {
        chessWebSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL);
        verify(matchmakingService, never()).exitSearchMode(any()); // User not authenticated yet
    }
}
