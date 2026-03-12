package com.chessapp.server.application.service;

import com.chessapp.server.domain.model.User;
import com.chessapp.server.domain.enums.TimeControl;

public interface MatchmakingService {
    void setNotifier(GameNotificationService notifier);

    void enterSearchMode(User user, TimeControl timeControl);

    void exitSearchMode(User user);

    boolean isUserSearching(User user);
}