package com.chessapp.server.service;

import com.chessapp.server.data.model.Game;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class GameTimerService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<Long, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();

    private final GameService gameService;

    public GameTimerService(@Lazy GameService gameService) {
        this.gameService = gameService;
    }

    public void scheduleTimeout(Game game) {
        cancelTimeout(game.getId());

        int timeLeft = game.getIsWhiteTurn() ? game.getWhiteTimeLeft() : game.getBlackTimeLeft();

        if (timeLeft <= 0) {
            Long timedOutPlayerId = game.getCurrentPlayer().getId();
            scheduler.execute(() -> {
                System.out.println("[Timeout] Triggering immediate timeout for game " + game.getId());
                gameService.handleTimeOutAsync(game.getId(), timedOutPlayerId);
            });
            return;
        }

        Long timedOutPlayerId = game.getCurrentPlayer().getId(); // Capture correct ID now
        ScheduledFuture<?> task = scheduler.schedule(() -> {
            System.out.println("[Timeout] Scheduled timeout triggered for game " + game.getId());
            gameService.handleTimeOutAsync(game.getId(), timedOutPlayerId); // Use correct player
        }, timeLeft, TimeUnit.MILLISECONDS);

        timeoutTasks.put(game.getId(), task);
    }





    public void cancelTimeout(Long gameId) {
        ScheduledFuture<?> task = timeoutTasks.remove(gameId);
        if (task != null && !task.isDone()) {
            task.cancel(true);
        }
    }

    public void stopAll() {
        for (ScheduledFuture<?> task : timeoutTasks.values()) {
            task.cancel(true);
        }
        timeoutTasks.clear();
    }
}

