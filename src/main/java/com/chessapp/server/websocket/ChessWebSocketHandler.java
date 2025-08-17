package com.chessapp.server.websocket;

import com.chessapp.server.data.model.User;
import com.chessapp.server.data.model.Game;
import com.chessapp.server.data.model.Challenge;
import com.chessapp.server.data.model.enums.GameState;
import com.chessapp.server.data.model.enums.MoveResult;
import com.chessapp.server.data.model.enums.TimeControl;
import com.chessapp.server.security.JwtUtils;
import com.chessapp.server.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChessWebSocketHandler implements WebSocketHandler ,GameNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ChessWebSocketHandler.class);

    @Autowired
    private UserService userService;

    @Autowired
    private GameService gameService;

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private MatchmakingService matchmakingService;

    @Autowired
    private GameTimerService gameTimerService;

    @Autowired
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map to store active WebSocket sessions
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        matchmakingService.setNotifier(this);
    }
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());

        // Extract token from session attributes or query parameters
        String token = extractToken(session);
        if (token != null && jwtUtils.validateJwtToken(token)) {
            String username = jwtUtils.getUserNameFromJwtToken(token);
            userSessions.put(username, session);

            // Set user online
            userService.findByLogin(username).ifPresent(user -> {
                userService.setUserOnline(user, true);
                session.getAttributes().put("user", user);

                // Send welcome message
                sendMessage(session, "connected", Map.of("message", "Connected successfully"));

                // Send pending challenges
                sendPendingChallenges(user);
            });
        } else {
            session.close();
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            session.close();
            return;
        }

        String payload = message.getPayload().toString();
        logger.info("Received message from {}: {}", user.getLogin(), payload);

        try {
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String type = (String) messageData.get("type");

            switch (type) {
                case "searchGame":
                    handleSearchGame(user, messageData);
                    break;
                case "cancelSearch":
                    handleCancelSearch(user);
                    break;
                case "challenge":
                    handleDirectChallenge(user, messageData);
                    break;
                case "acceptChallenge":
                    handleAcceptChallenge(user, messageData);
                    break;
                case "declineChallenge":
                    handleDeclineChallenge(user, messageData);
                    break;
                case "move":
                    handleMove(user, messageData);
                    break;
                case "resign":
                    handleResign(user, messageData);
                    break;
                case "chat":
                    handleChat(user, messageData);
                    break;
                default:
                    logger.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Error handling message: ", e);
            sendMessage(session, "error", Map.of("message", "Invalid message format"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: ", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        User user = (User) session.getAttributes().get("user");
        if (user != null) {
            logger.info("WebSocket connection closed for user: {}", user.getLogin());

            // Remove from active sessions
            userSessions.remove(user.getLogin());

            // Set user offline
            userService.setUserOnline(user, false);

            // Cancel any active searches
            matchmakingService.exitSearchMode(user);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String extractToken(WebSocketSession session) {
        // Try to get token from query parameters
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
        }
        return null;
    }

    private void handleSearchGame(User user, Map<String, Object> messageData) {
        try {
            String timeControlStr = (String) messageData.get("timeControl");
            TimeControl timeControl;
            try {
                timeControl = TimeControl.valueOf(timeControlStr.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                sendToUser(user.getLogin(), "error", Map.of("message", "Invalid time control"));
                return;
            }
            matchmakingService.enterSearchMode(user, timeControl);

            // Send confirmation
            sendToUser(user.getLogin(), "searchStarted", Map.of("timeControl", timeControl.name()));

            // Remove the immediate match check - it's handled by the matchmaking service
            // checkForMatch(user, timeControl);

        } catch (Exception e) {
            logger.error("Error handling search game: ", e);
            sendToUser(user.getLogin(), "error", Map.of("message", "Failed to start search"));
        }
    }

    // Remove or update the checkForMatch method - it's not needed since
// the matchmaking service handles the matching logic


    private void handleCancelSearch(User user) {
        matchmakingService.exitSearchMode(user);
        sendToUser(user.getLogin(), "searchCancelled", Map.of("message", "Search cancelled"));
    }

    private void handleDirectChallenge(User user, Map<String, Object> messageData) {
        try {
            String targetLogin = (String) messageData.get("targetLogin");
            String timeControlStr = (String) messageData.get("timeControl");

            if (targetLogin == null || timeControlStr == null) {
                sendToUser(user.getLogin(), "error", Map.of("message", "Missing parameters for challenge"));
                return;
            }

            TimeControl timeControl;
            try {
                timeControl = TimeControl.valueOf(timeControlStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                sendToUser(user.getLogin(), "error", Map.of("message", "Invalid time control"));
                return;
            }

            Optional<User> targetUserOpt = userService.findByLogin(targetLogin);
            if (targetUserOpt.isEmpty()) {
                sendToUser(user.getLogin(), "error", Map.of("message", "User not found"));
                return;
            }

            User targetUser = targetUserOpt.get();
            if (!Boolean.TRUE.equals(targetUser.getIsOnline())) {
                sendToUser(user.getLogin(), "error", Map.of("message", "User is offline"));
                return;
            }

            if (user.equals(targetUser)) {
                sendToUser(user.getLogin(), "error", Map.of("message", "You cannot challenge yourself"));
                return;
            }

            Challenge challenge = challengeService.createChallenge(user, targetUser, timeControl);

            sendToUser(user.getLogin(), "challengeSent", Map.of(
                    "challengeId", challenge.getId(),
                    "targetUser", targetUser.getLogin(),
                    "timeControl", timeControl.name()
            ));

            sendToUser(targetUser.getLogin(), "incomingChallenge", Map.of(
                    "challengeId", challenge.getId(),
                    "challenger", user.getLogin(),
                    "challengerDisplayName", user.getDisplayName(),
                    "timeControl", timeControl.name()
            ));

        } catch (Exception e) {
            logger.error("Error handling direct challenge: ", e);
            sendToUser(user.getLogin(), "error", Map.of("message", "Failed to send challenge"));
        }
    }

    private void handleAcceptChallenge(User user, Map<String, Object> messageData) {
        try {
            Long challengeId;
            try {
                challengeId = Long.valueOf(messageData.get("challengeId").toString());
            } catch (Exception e) {
                sendToUser(user.getLogin(), "error", Map.of("message", "Invalid challengeId"));
                return;
            }

            Game game = challengeService.acceptChallenge(challengeId, user);
            Map<String, Object> gameData = gameService.createGameDataMap(game);

            sendToUser(game.getWhitePlayer().getLogin(), "gameStarted", gameData);
            sendToUser(game.getBlackPlayer().getLogin(), "gameStarted", gameData);

        } catch (IllegalArgumentException e) {
            logger.warn("Challenge accept failed: {}", e.getMessage());
            sendToUser(user.getLogin(), "error", Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error accepting challenge: ", e);
            sendToUser(user.getLogin(), "error", Map.of("message", "Failed to accept challenge"));
        }
    }

    private void handleDeclineChallenge(User user, Map<String, Object> messageData) {
        try {
            Long challengeId;
            try {
                challengeId = Long.valueOf(messageData.get("challengeId").toString());
            } catch (Exception e) {
                sendToUser(user.getLogin(), "error", Map.of("message", "Invalid challengeId"));
                return;
            }

            challengeService.declineChallenge(challengeId, user);

            Optional<Challenge> challengeOpt = challengeService.findById(challengeId);
            if (challengeOpt.isPresent()) {
                Challenge challenge = challengeOpt.get();
                sendToUser(challenge.getChallenger().getLogin(), "challengeDeclined", Map.of(
                        "challengeId", challengeId,
                        "message", user.getDisplayName() + " declined your challenge"
                ));
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Challenge decline failed: {}", e.getMessage());
            sendToUser(user.getLogin(), "error", Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error declining challenge: ", e);
            sendToUser(user.getLogin(), "error", Map.of("message", "Failed to decline challenge"));
        }
    }


    private void handleChat(User user, Map<String, Object> messageData) {
        try {
            Long gameId = Long.valueOf(messageData.get("gameId").toString());
            String message = (String) messageData.get("message");

            Optional<Game> gameOpt = gameService.findByIdWithMoves(gameId);
            if (gameOpt.isPresent()) {
                Game game = gameOpt.get();
Hibernate.initialize(game.getMoves());

                if (game.isPlayerInGame(user)) {
                    User opponent = game.getOpponent(user);

                    Map<String, Object> chatData = Map.of(
                            "gameId", gameId,
                            "sender", user.getDisplayName(),
                            "message", message,
                            "timestamp", System.currentTimeMillis()
                    );

                    sendToUser(opponent.getLogin(), "chatMessage", chatData);
                }
            }

        } catch (Exception e) {
            logger.error("Error handling chat: ", e);
        }
    }

    private void checkForMatch(User user, TimeControl timeControl) {
        // This method is causing the lazy initialization error
        // Remove it or rewrite it to not access the moves collection
        Optional<Game> optionalGame = gameService.findActiveGameByPlayer(user);

        if (optionalGame.isPresent()) {
            Game game = optionalGame.get();

            // Only check basic game properties, don't access moves
            if (game.getTimeControl() == timeControl && game.getState() == GameState.IN_PROGRESS) {
                User opponent = game.getOpponent(user);

                if (opponent != null) {
                    // Use the service method to create game data safely
                    Map<String, Object> gameData = gameService.createGameDataMap(game);
                    sendToUser(user.getLogin(), "gameStarted", gameData);
                    sendToUser(opponent.getLogin(), "gameStarted", gameData);
                }
            }
        }
    }



    private void sendPendingChallenges(User user) {
        List<Challenge> pendingChallenges = challengeService.findPendingChallengesForUser(user);

        for (Challenge challenge : pendingChallenges) {
            sendToUser(user.getLogin(), "incomingChallenge", Map.of(
                    "challengeId", challenge.getId(),
                    "challenger", challenge.getChallenger().getLogin(),
                    "challengerDisplayName", challenge.getChallenger().getDisplayName(),
                    "timeControl", challenge.getTimeControl().name()
            ));
        }
    }


    private void sendToUser(String username, String type, Map<String, Object> data) {
        WebSocketSession session = userSessions.get(username);
        if (session != null && session.isOpen()) {
            sendMessage(session, type, data);
        }
    }


    private void sendMessage(WebSocketSession session, String type, Map<String, Object> data) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", type);
            message.put("data", data);

            String jsonMessage = objectMapper.writeValueAsString(message);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(jsonMessage));
            } else {
                logger.warn("Attempted to send message to closed session: {}", session.getId());
            }

        } catch (IOException | IllegalStateException e) {
            logger.warn("Failed to send message to session {}: {}", session.getId(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending message", e);
        }
    }



    private void handleMove(User user, Map<String, Object> messageData) {
        try {
            Long gameId;
            try {
                gameId = Long.valueOf(String.valueOf(messageData.get("gameId")));
            } catch (Exception e) {
                sendToUser(user.getLogin(), "error", Map.of("message", "Invalid gameId"));
                return;
            }
            String move = (String) messageData.get("move");

            MoveResult result = gameService.makeMove(gameId, user, move);

            if (result == MoveResult.SUCCESS || result == MoveResult.GAME_ENDED) {
                Map<String, Object> gameData = gameService.createGameDataMap(gameId);
                if (gameData != null) {
                    Optional<Game> gameOpt = gameService.findById(gameId);
                    if (gameOpt.isPresent()) {
                        Game game = gameOpt.get();

                        sendToUser(game.getWhitePlayer().getLogin(), "gameUpdate", gameData);
                        sendToUser(game.getBlackPlayer().getLogin(), "gameUpdate", gameData);

                        if (result == MoveResult.GAME_ENDED) {
                            notifyGameEnded(game);
                        }
                    }
                }
            } else {
                sendToUser(user.getLogin(), result.name(), Map.of("message", "Invalid move"));
            }
        } catch (Exception e) {
            logger.error("Error handling move: ", e);
            sendToUser(user.getLogin(), "error", Map.of("message", "Failed to make move"));
        }
    }

    private void handleResign(User user, Map<String, Object> messageData) {
        try {
            Long gameId = Long.valueOf(messageData.get("gameId").toString());

            gameService.resignGame(gameId, user);

            // Get game data using the service method
            Map<String, Object> gameData = gameService.createGameDataMap(gameId);
            if (gameData != null) {
                Optional<Game> gameOpt = gameService.findById(gameId);
                if (gameOpt.isPresent()) {
                    Game game = gameOpt.get();
                    notifyGameEnded(game);
                }
            }

        } catch (Exception e) {
            logger.error("Error handling resign: ", e);
            sendToUser(user.getLogin(), "error", Map.of("message", "Failed to resign"));
        }
    }


    @Override
    public void notifyGameFound(User user1, User user2, Game game) {
        gameTimerService.scheduleTimeout(game);
        Map<String, Object> gameData = gameService.createGameDataMap(game);

        if (gameData != null) {
            sendToUser(user1.getLogin(), "gameStarted", gameData);
            sendToUser(user2.getLogin(), "gameStarted", gameData);
        } else {
            // Fallback - send error messages
            sendToUser(user1.getLogin(), "error", Map.of("message", "Failed to start game"));
            sendToUser(user2.getLogin(), "error", Map.of("message", "Failed to start game"));
        }
    }

    @Override
    public void notifyGameEnded(Game game) {
        Map<String, Object> resultData = Map.of(
                "gameId", game.getId(),
                "winner", game.getWinner() != null ? game.getWinner().getLogin() : "draw",
                "result", game.getResult().name(),
                "whiteRating", game.getWhitePlayer().getRatingForTimeControl(game.getTimeControl()),
                "blackRating", game.getBlackPlayer().getRatingForTimeControl(game.getTimeControl())
        );

        sendToUser(game.getWhitePlayer().getLogin(), "gameEnded", resultData);
        sendToUser(game.getBlackPlayer().getLogin(), "gameEnded", resultData);
    }
}