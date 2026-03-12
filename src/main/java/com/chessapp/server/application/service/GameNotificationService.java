package com.chessapp.server.application.service;

import com.chessapp.server.domain.model.Game;
import com.chessapp.server.domain.model.User;

public interface GameNotificationService {
    void notifyGameFound(User user1, User user2, Game game);

    void notifyGameEnded(Game game);
}
