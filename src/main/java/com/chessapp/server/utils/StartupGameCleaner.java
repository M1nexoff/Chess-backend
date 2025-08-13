package com.chessapp.server.utils;

import com.chessapp.server.service.GameService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class StartupGameCleaner {

    private final GameService gameService;

    public StartupGameCleaner(GameService gameService) {
        this.gameService = gameService;
    }

    @PostConstruct
    public void cleanUnfinishedGames() {
        gameService.endAllUnfinishedGames();
        System.out.println("[StartupGameCleaner] All unfinished games are marked as ENDED.");
    }
}