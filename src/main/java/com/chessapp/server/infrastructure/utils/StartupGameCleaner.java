package com.chessapp.server.infrastructure.utils;

import com.chessapp.server.application.service.GameService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StartupGameCleaner {

    private static final Logger logger = LoggerFactory.getLogger(StartupGameCleaner.class);

    private final GameService gameService;

    public StartupGameCleaner(GameService gameService) {
        this.gameService = gameService;
    }

    @PostConstruct
    public void cleanUnfinishedGames() {
        gameService.endAllUnfinishedGames();
        logger.info("[StartupGameCleaner] All unfinished games are marked as ENDED.");
    }
}