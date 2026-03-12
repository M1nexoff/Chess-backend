package com.chessapp.server.presentation.websocket;

import com.chessapp.server.application.dto.UserResponseDto;
import com.chessapp.server.application.service.UserService;
import com.chessapp.server.domain.enums.TimeControl;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.security.JwtUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class ChessWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketSession session1;
    private WebSocketSession session2;
    private TestWebSocketHandler handler1;
    private TestWebSocketHandler handler2;

    private User user1;
    private User user2;
    private String token1;
    private String token2;

    @BeforeEach
    public void setup() throws Exception {
        user1 = userService.registerUser("testuser1_" + System.currentTimeMillis(), "password", "Test User 1");
        user2 = userService.registerUser("testuser2_" + System.currentTimeMillis(), "password", "Test User 2");

        token1 = jwtUtils.generateToken(user1.getLogin());
        token2 = jwtUtils.generateToken(user2.getLogin());

        handler1 = new TestWebSocketHandler();
        handler2 = new TestWebSocketHandler();
    }

    @AfterEach
    public void teardown() throws Exception {
        if (session1 != null && session1.isOpen())
            session1.close();
        if (session2 != null && session2.isOpen())
            session2.close();
    }

    private WebSocketSession connect(String token, TestWebSocketHandler handler) throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        String url = "ws://localhost:" + port + "/ws/chess?token=" + token;
        return client.execute(handler, new WebSocketHttpHeaders(), URI.create(url)).get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testConnectionAndAuthentication() throws Exception {
        session1 = connect(token1, handler1);

        // Wait for connected message
        String response = handler1.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(response);
        Map<String, Object> data = objectMapper.readValue(response, Map.class);
        assertEquals("connected", data.get("type"));
    }

    @Test
    public void testMatchmakingAndGameStart() throws Exception {
        session1 = connect(token1, handler1);
        session2 = connect(token2, handler2);

        // Wait for connection messages
        handler1.messages.poll(5, TimeUnit.SECONDS);
        handler2.messages.poll(5, TimeUnit.SECONDS);

        handler1.messages.clear();
        handler2.messages.clear();

        // User 1 searches for game
        sendMessage(session1, Map.of("type", "searchGame", "timeControl", "BLITZ"));

        String res1 = handler1.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(res1);
        assertEquals("searchStarted", getType(res1));

        // User 2 searches for game
        sendMessage(session2, Map.of("type", "searchGame", "timeControl", "BLITZ"));

        // Both should receive gameStarted
        String p1GameMsg = handler1.messages.poll(5, TimeUnit.SECONDS);
        String p2GameMsg = handler2.messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(p1GameMsg, "Player 1 did not receive gameStarted");
        assertNotNull(p2GameMsg, "Player 2 did not receive gameStarted");

        assertEquals("gameStarted", getType(p1GameMsg));
        assertEquals("gameStarted", getType(p2GameMsg));
    }

    @Test
    public void testDirectChallenge() throws Exception {
        session1 = connect(token1, handler1);
        session2 = connect(token2, handler2);

        // Wait for connection messages
        handler1.messages.poll(5, TimeUnit.SECONDS);
        handler2.messages.poll(5, TimeUnit.SECONDS);

        handler1.messages.clear();
        handler2.messages.clear();

        // User 1 challenges User 2
        sendMessage(session1, Map.of(
                "type", "challenge",
                "targetLogin", user2.getLogin(),
                "timeControl", "RAPID"));

        // User 1 gets challengeSent confirmation
        String res1 = handler1.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(res1);
        assertEquals("challengeSent", getType(res1));

        Map<String, Object> challengeData = (Map<String, Object>) parseJson(res1).get("data");
        Integer challengeId = (Integer) challengeData.get("challengeId");

        // User 2 gets incomingChallenge
        String res2 = handler2.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(res2);
        assertEquals("incomingChallenge", getType(res2));

        // User 2 accepts
        sendMessage(session2, Map.of(
                "type", "acceptChallenge",
                "challengeId", challengeId));

        // Both get gameStarted
        String p1GameMsg = handler1.messages.poll(5, TimeUnit.SECONDS);
        String p2GameMsg = handler2.messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(p1GameMsg);
        assertNotNull(p2GameMsg);
        assertEquals("gameStarted", getType(p1GameMsg));
        assertEquals("gameStarted", getType(p2GameMsg));
    }

    @Test
    public void testInGameActions() throws Exception {
        session1 = connect(token1, handler1);
        session2 = connect(token2, handler2);

        // consume connected messages
        handler1.messages.poll(5, TimeUnit.SECONDS);
        handler2.messages.poll(5, TimeUnit.SECONDS);

        handler1.messages.clear();
        handler2.messages.clear();

        // 1. Setup a game via direct challenge
        sendMessage(session1, Map.of("type", "challenge", "targetLogin", user2.getLogin(), "timeControl", "BLITZ"));

        String res1 = handler1.messages.poll(5, TimeUnit.SECONDS);
        Map<String, Object> challengeData = (Map<String, Object>) parseJson(res1).get("data");
        Integer challengeId = (Integer) challengeData.get("challengeId");

        // consume incomingChallenge on user 2
        handler2.messages.poll(5, TimeUnit.SECONDS);

        // User 2 accepts
        sendMessage(session2, Map.of("type", "acceptChallenge", "challengeId", challengeId));

        // Wait for gameStarted on both
        String p1GameMsg = handler1.messages.poll(5, TimeUnit.SECONDS);
        String p2GameMsg = handler2.messages.poll(5, TimeUnit.SECONDS);

        Long gameId = Long
                .valueOf(String.valueOf(((Map<String, Object>) parseJson(p1GameMsg).get("data")).get("gameId")));

        handler1.messages.clear();
        handler2.messages.clear();

        // 2. Test Chat
        sendMessage(session1, Map.of("type", "chat", "gameId", gameId, "message", "Hello opponent"));

        // User 2 should receive chat
        String chatMsg = handler2.messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(chatMsg, "Opponent did not receive chat message");
        assertEquals("chatMessage", getType(chatMsg));

        // 3. Test Move
        sendMessage(session1, Map.of("type", "move", "gameId", gameId, "move", "e2e4"));

        String p1MoveMsg = handler1.messages.poll(5, TimeUnit.SECONDS);
        String p2MoveMsg = handler2.messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(p1MoveMsg, "Player 1 did not receive move update");
        assertNotNull(p2MoveMsg, "Player 2 did not receive move update");

        assertEquals("gameUpdate", getType(p1MoveMsg));
        assertEquals("gameUpdate", getType(p2MoveMsg));

        handler1.messages.clear();
        handler2.messages.clear();

        // 4. Test Resign
        sendMessage(session2, Map.of("type", "resign", "gameId", gameId));

        String p1EndMsg = handler1.messages.poll(5, TimeUnit.SECONDS);
        String p2EndMsg = handler2.messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(p1EndMsg, "Player 1 did not receive gameEnded");
        assertNotNull(p2EndMsg, "Player 2 did not receive gameEnded");

        assertEquals("gameEnded", getType(p1EndMsg));
        assertEquals("gameEnded", getType(p2EndMsg));
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> payload) throws Exception {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private String getType(String json) throws JsonProcessingException {
        return (String) parseJson(json).get("type");
    }

    private Map<String, Object> parseJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, Map.class);
    }

    private class TestWebSocketHandler extends TextWebSocketHandler {
        public final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) {
            System.out.println("Received: " + message.getPayload());
            messages.add(message.getPayload());
        }
    }
}
