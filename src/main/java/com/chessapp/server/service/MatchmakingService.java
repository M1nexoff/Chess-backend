package com.chessapp.server.service;

import com.chessapp.server.data.model.*;
import com.chessapp.server.data.model.enums.*;
import com.chessapp.server.websocket.ChessWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class MatchmakingService {

    @Autowired
    private UserService userService;

    @Autowired
    private GameService gameService;

    private GameNotificationService notifier;

    public void setNotifier(GameNotificationService notifier) {
        this.notifier = notifier;
    }

    // Store users looking for games
    private final ConcurrentMap<String, MatchmakingRequest> searchingUsers = new ConcurrentHashMap<>();

    @Transactional
    public void enterSearchMode(User user, TimeControl timeControl) {
        Optional<Game> activeGame = gameService.findActiveGameByPlayer(user);
        System.out.println("[MM] User " + user.getLogin() + " entering search. Active game present? " + activeGame.isPresent());

        if (activeGame.isPresent()) {
            // Check if the game is actually in progress
            Game game = activeGame.get();
            if (game.getState() == GameState.IN_PROGRESS) {
                throw new IllegalStateException("User already has an active game");
            }
        }

        MatchmakingRequest request = new MatchmakingRequest(user, timeControl);
        searchingUsers.put(user.getLogin(), request);
        findMatch(request);
    }

    public void exitSearchMode(User user) {
        searchingUsers.remove(user.getLogin());
    }

    public boolean isUserSearching(User user) {
        return searchingUsers.containsKey(user.getLogin());
    }

    @Transactional
    private void findMatch(MatchmakingRequest request) {
        User user = request.getUser();
        TimeControl timeControl = request.getTimeControl();
        int userRating = user.getRatingForTimeControl(timeControl);

        for (MatchmakingRequest other : searchingUsers.values()) {
            if (other.getUser().equals(user)) continue;
            if (other.getTimeControl() != timeControl) continue;

            int otherRating = other.getUser().getRatingForTimeControl(timeControl);
            if (Math.abs(userRating - otherRating) <= 200) {
                // Create the game
                Game game = gameService.createGame(user, other.getUser(), timeControl);
                System.out.println("After game creation: " + game.getId() + " - " + game.getState());

                // Remove both users from search
                searchingUsers.remove(user.getLogin());
                searchingUsers.remove(other.getUser().getLogin());

                // Notify both users about the game
                if (notifier != null) {
                    notifier.notifyGameFound(user, other.getUser(), game);

                }
                break;
            }
        }
    }

    private static class MatchmakingRequest {
        private final User user;
        private final TimeControl timeControl;
        private final long timestamp;

        public MatchmakingRequest(User user, TimeControl timeControl) {
            this.user = user;
            this.timeControl = timeControl;
            this.timestamp = System.currentTimeMillis();
        }

        public User getUser() {
            return user;
        }

        public TimeControl getTimeControl() {
            return timeControl;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}