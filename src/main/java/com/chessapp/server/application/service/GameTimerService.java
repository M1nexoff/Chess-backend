package com.chessapp.server.application.service;

import com.chessapp.server.domain.model.Game;

public interface GameTimerService {
    void scheduleTimeout(Game game);

    void cancelTimeout(Long gameId);

    void stopAll();
}
