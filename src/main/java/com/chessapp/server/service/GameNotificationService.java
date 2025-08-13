package com.chessapp.server.service;

import com.chessapp.server.data.model.Game;
import com.chessapp.server.data.model.User;

public interface GameNotificationService {
    void notifyGameFound(User user1, User user2, Game game);

    void notifyGameEnded(Game game);
}
