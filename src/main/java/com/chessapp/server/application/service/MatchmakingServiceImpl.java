package com.chessapp.server.application.service;

import com.chessapp.server.domain.model.*;
import com.chessapp.server.domain.enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class MatchmakingServiceImpl implements MatchmakingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchmakingServiceImpl.class);

    private final GameService gameService;
    private GameNotificationService notifier;

    public MatchmakingServiceImpl(GameService gameService) {
        this.gameService = gameService;
    }

    public void setNotifier(GameNotificationService notifier) {
        this.notifier = notifier;
    }

    // Store users looking for games
    private final ConcurrentMap<String, MatchmakingRequest> searchingUsers = new ConcurrentHashMap<>();

    @Transactional
    public void enterSearchMode(User user, TimeControl timeControl) {
        Optional<Game> activeGame = gameService.findActiveGameByPlayer(user);
        logger.info("[MM] User {} entering search. Active game present? {}", user.getLogin(), activeGame.isPresent());

        // if (activeGame.isPresent()) {
        // // Check if the game is actually in progress
        // Game game = activeGame.get();
        // if (game.getState() == GameState.IN_PROGRESS) {
        // throw new IllegalStateException("User already has an active game");
        // }
        // }

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
            if (other.getUser().equals(user))
                continue;
            if (other.getTimeControl() != timeControl)
                continue;

            int otherRating = other.getUser().getRatingForTimeControl(timeControl);
            if (Math.abs(userRating - otherRating) <= 200) {
                // Create the game
                Game game = gameService.createGame(user, other.getUser(), timeControl);
                logger.info("After game creation: {} - {}", game.getId(), game.getState());

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